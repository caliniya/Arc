package arc.packer;

import arc.files.*;
import arc.packer.TexturePacker.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** @author Nathan Sweet */
public class TexturePackerFileProcessor extends FileProcessor{
    private final Settings defaultSettings;
    private ObjectMap<File, Settings> dirToSettings = new ObjectMap<>();
    private Json json = new Json();
    private String packFileName;
    private File root;
    Ar<File> ignoreDirs = new Ar<>();
    boolean countOnly;
    int packCount;
    /**
     * Runs the packing of directories in the background. Only exists while the actual processing pass is running.
     * 在后台运行目录的打包。仅在实际处理阶段运行期间存在。
     */
    private PackQueue queue;

    public TexturePackerFileProcessor(){
        this(new Settings(), "pack.aatls");
    }

    public TexturePackerFileProcessor(Settings defaultSettings, String packFileName){
        this.defaultSettings = defaultSettings;

        if(packFileName.toLowerCase().endsWith(defaultSettings.atlasExtension.toLowerCase()))
            packFileName = packFileName.substring(0, packFileName.length() - defaultSettings.atlasExtension.length());
        this.packFileName = packFileName;

        setFlattenOutput(true);
        addInputSuffix(".png", ".jpg", ".jpeg");

        // Sort input files by name to avoid platform-dependent atlas output changes.
        // 按名称对输入文件排序,以避免图集输出因平台而异。
        setComparator(Structs.comparing(File::getName));
    }

    @Override
    public Ar<Entry> process(File inputFile, File outputRoot) throws Exception{
        root = inputFile;

        // Collect pack.json setting files.
        // 收集 pack.json 设置文件。
        final Ar<File> settingsFiles = new Ar<>();
        FileProcessor settingsProcessor = new FileProcessor(){
            @Override
            protected void processFile(Entry inputFile){
                settingsFiles.add(inputFile.inputFile);
            }
        };
        settingsProcessor.addInputRegex("pack\\.h?json");
        settingsProcessor.process(inputFile, null);
        // Sort parent first.
        // 父目录排在前面。
        settingsFiles.sort(Structs.comparingInt(file -> file.toString().length()));
        for(File settingsFile : settingsFiles){
            // Find first parent with settings, or use defaults.
            // 找到第一个有设置的父目录,或使用默认设置。
            Settings settings = null;
            File parent = settingsFile.getParentFile();
            while(true){
                if(parent.equals(root)) break;
                parent = parent.getParentFile();
                settings = dirToSettings.get(parent);
                if(settings != null){
                    settings = settings.copy();
                    break;
                }
            }
            if(settings == null) settings = defaultSettings.copy();
            // Merge settings from current directory.
            // 合并当前目录的设置。
            merge(settings, settingsFile);
            dirToSettings.put(settingsFile.getParentFile(), settings);
        }

        // Count the number of texture packer invocations.
        // 统计纹理打包器的调用次数。
        countOnly = true;
        super.process(inputFile, outputRoot);
        countOnly = false;

        // Do actual processing.
        // 执行实际处理。
        return super.process(inputFile, outputRoot);
    }

    void merge(Settings settings, File settingsFile){
        try{
            json.readFields(settings, Jval.read(new Fi(settingsFile)));
        }catch(Exception ex){
            throw new ArcRuntimeException("Error reading settings file: " + settingsFile, ex);
        }
    }

    @Override
    public Ar<Entry> process(File[] files, File outputRoot) throws Exception{
        // Delete pack file and images.
        // 删除打包文件和图片。
        if(countOnly && outputRoot.exists()) deleteOutput(outputRoot);
        if(countOnly) return super.process(files, outputRoot);

        //directories are packed in the background as they are found, and the results are written out in order once they're ready
        // 目录在被发现时即在后台打包,结果就绪后按顺序写出
        PackQueue queue = this.queue = new PackQueue();
        try{
            Ar<Entry> result = super.process(files, outputRoot);
            queue.finish();
            return result;
        }finally{
            this.queue = null;
            queue.close();
        }
    }

    protected void deleteOutput(File outputRoot) throws Exception{
        // Load root settings to get scale.
        // 加载根设置以获取缩放比例。
        File settingsFile = new File(root, "pack.hjson");
        //use JSON as fallback
        // 使用 JSON 作为后备
        if(!settingsFile.exists()) settingsFile = new File(root, "pack.json");
        Settings rootSettings = defaultSettings;
        if(settingsFile.exists()){
            rootSettings = rootSettings.copy();
            merge(rootSettings, settingsFile);
        }

        String atlasExtension = rootSettings.atlasExtension == null ? "" : rootSettings.atlasExtension;
        atlasExtension = Pattern.quote(atlasExtension);

        for(int i = 0, n = rootSettings.scale.length; i < n; i++){
            FileProcessor deleteProcessor = new FileProcessor(){
                @Override
                protected void processFile(Entry inputFile) throws Exception{
                    inputFile.inputFile.delete();
                }
            };
            deleteProcessor.setRecursive(false);

            File packFile = new File(rootSettings.getScaledPackFileName(packFileName, i));

            String prefix = packFile.getName();
            int dotIndex = prefix.lastIndexOf('.');
            if(dotIndex != -1) prefix = prefix.substring(0, dotIndex);
            deleteProcessor.addInputRegex("(?i)" + prefix + "\\d*\\.(png|jpg|jpeg)");
            deleteProcessor.addInputRegex("(?i)" + prefix + atlasExtension);

            String dir = packFile.getParent();
            if(dir == null)
                deleteProcessor.process(outputRoot, null);
            else if(new File(outputRoot + "/" + dir).exists()) //
                deleteProcessor.process(outputRoot + "/" + dir, null);
        }
    }

    @Override
    protected void processDir(final Entry inputDir, Ar<Entry> files) throws Exception{
        if(ignoreDirs.contains(inputDir.inputFile)) return;

        // Find first parent with settings, or use defaults.
        // 找到第一个有设置的父目录,或使用默认设置。
        Settings settings = null;
        File parent = inputDir.inputFile;
        while(true){
            settings = dirToSettings.get(parent);
            if(settings != null) break;
            if(parent == null || parent.equals(root)) break;
            parent = parent.getParentFile();
        }
        if(settings == null) settings = defaultSettings;

        if(settings.ignore) return;

        if(settings.combineSubdirectories){
            // Collect all files under subdirectories and ignore subdirectories without pack.json files.
            // 收集子目录下的所有文件,并忽略没有 pack.json 文件的子目录。
            files = new FileProcessor(this){
                @Override
                protected void processDir(Entry entryDir, Ar<Entry> files){
                    if(!entryDir.inputFile.equals(inputDir.inputFile) && (new File(entryDir.inputFile, "pack.json").exists() || new File(entryDir.inputFile, "pack.hjson").exists())){
                        files.clear();
                        return;
                    }
                    if(!countOnly) ignoreDirs.add(entryDir.inputFile);
                }

                @Override
                protected void processFile(Entry entry){
                    addProcessedFile(entry);
                }
            }.process(inputDir.inputFile, null);
        }

        if(files.isEmpty()) return;

        if(countOnly){
            packCount++;
            return;
        }

        final Pattern digitSuffix = Pattern.compile("(.*?)(\\d+)$");

        // Sort by name using numeric suffix, then alpha.
        // 按名称使用数字后缀排序,然后按字母排序。
        // The name and number are worked out once per file, rather than on every comparison.
        // 名称和编号每个文件只计算一次,而不是在每次比较时计算。
        Ar<SortKey> keys = new Ar<>(files.size);
        for(Entry entry : files){
            keys.add(new SortKey(entry, digitSuffix));
        }
        keys.sort((key1, key2) -> {
            int compare = key1.name.compareTo(key2.name);
            if(compare != 0 || key1.number == key2.number) return compare;
            return key1.number - key2.number;
        });
        files.clear();
        for(SortKey key : keys){
            files.add(key.entry);
        }

        // Pack.
        // 打包。
        TexturePacker packer = new TexturePacker(root, settings);
        //messages are held back until it's this directory's turn, so output from directories packed at the same time stays readable
        // 消息会被暂存,直到轮到该目录,这样同时打包的目录输出仍保持可读
        packer.bufferLog();
        if(!settings.silent){
            try{
                packer.log().println(inputDir.inputFile.getCanonicalPath());
            }catch(IOException ignored){
                packer.log().println(inputDir.inputFile.getAbsolutePath());
            }
        }

        for(Entry file : files){
            packer.addImage(file.inputFile);
        }

        queue.add(inputDir, packer, inputDir.outputDir, packFileName);
    }

    /**
     * Sort key for an input file, see {@link #processDir(Entry, Ar)}.
     * 输入文件的排序键,参见 {@link #processDir(Entry, Ar)}。
     */
    private static class SortKey{
        final Entry entry;
        final String name;
        int number;

        SortKey(Entry entry, Pattern digitSuffix){
            this.entry = entry;

            String full = entry.inputFile.getName();
            int dotIndex = full.lastIndexOf('.');
            if(dotIndex != -1) full = full.substring(0, dotIndex);

            String name = full;
            Matcher matcher = digitSuffix.matcher(full);
            if(matcher.matches()){
                try{
                    number = Integer.parseInt(matcher.group(2));
                    name = matcher.group(1);
                }catch(Exception ignored){
                }
            }
            this.name = name;
        }
    }

    /**
     * Packs directories concurrently, but hands out page names and writes atlases in the order the directories were added. Output
     * is therefore identical to processing them one after another.
     * <p>
     * 并发打包各目录,但按目录添加的顺序分配页面名称并写入图集。因此输出与逐个处理它们完全相同。
     */
    private class PackQueue{
        //Mostly waits on Core.executor, which does the real work, so this doesn't need many threads. It's kept small as every
        // 主要在等待 Core.executor(它做真正的工作),因此不需要很多线程。线程池保持较小,因为每个
        //directory in progress keeps all of its images in memory.
        // 进行中的目录都会将其所有图片保留在内存中。
        final ExecutorService pool = Threads.executor("Packer Directories", Math.max(1, Math.min(OS.cores, 4)));
        final Ar<Job> jobs = new Ar<>();
        /**
         * Page images claimed so far, shared by all jobs, as rendering doesn't create the files immediately.
         * 到目前为止已占用的页面图片,由所有作业共享,因为渲染不会立即创建文件。
         */
        final Set<File> claimed = Collections.synchronizedSet(new HashSet<File>());
        volatile boolean failed;
        Job last;

        void add(Entry dir, TexturePacker packer, File outputDir, String packFileName){
            final Job job = new Job(dir, packer, outputDir, packFileName);
            final Job previous = last;
            last = job;
            packer.setClaimedFiles(claimed);
            jobs.add(job);
            //jobs are started in the order they're submitted, so the one being waited on has always started already
            // 作业按提交顺序启动,因此正在等待的作业肯定已经启动
            job.result = pool.submit(() -> run(job, previous));
        }

        void run(Job job, Job previous){
            Ar<FutureTask<Void>> renders = null;
            try{
                if(failed) return;

                //the slow part: loading and packing, independent of anything else
                // 耗时部分:加载和打包,与其他任何东西无关
                Ar<Ar<Page>> pages = job.packer.packScales();

                //wait for our turn to touch the output directory
                // 等待轮到我们访问输出目录
                if(previous != null) previous.named.join();
                if(failed) return;

                job.packer.flushLog();
                renders = job.packer.write(job.outputDir, job.packFileName, pages);
            }catch(RuntimeException | Error t){
                failed = true;
                throw t;
            }finally{
                job.named.complete(null);
            }

            try{
                Tasks.joinAll(renders);
            }catch(RuntimeException | Error t){
                failed = true;
                throw t;
            }
        }

        /**
         * Waits for every directory to be completely written.
         * 等待每个目录完全写入。
         */
        void finish() throws Exception{
            for(Job job : jobs){
                try{
                    job.result.get();
                }catch(ExecutionException e){
                    failed = true;
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    throw new Exception("Error processing directory: " + job.dir.inputFile.getAbsolutePath(), cause);
                }
            }
        }

        /**
         * Stops accepting work and waits for anything in progress, so nothing is being written once processing ends.
         * 停止接受工作并等待进行中的任务,这样处理结束后就不会再有内容被写入。
         */
        void close(){
            //if we get here due to an error, make queued directories bail out instead of packing pointlessly
            // 如果因错误而走到这里,让排队的目录直接退出,而不是无意义地打包
            for(Job job : jobs){
                if(!job.result.isDone()) failed = true;
            }
            Threads.await(pool);
        }
    }

    private static class Job{
        final Entry dir;
        final TexturePacker packer;
        final File outputDir;
        final String packFileName;
        /**
         * Completed once page names and atlas contents have been decided.
         * 在页面名称和图集内容确定后即完成。
         */
        final CompletableFuture<Void> named = new CompletableFuture<>();
        Future<?> result;

        Job(Entry dir, TexturePacker packer, File outputDir, String packFileName){
            this.dir = dir;
            this.packer = packer;
            this.outputDir = outputDir;
            this.packFileName = packFileName;
        }
    }

}
