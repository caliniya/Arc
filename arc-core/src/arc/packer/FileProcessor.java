package arc.packer;

import arc.struct.*;
import arc.util.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Collects files recursively, filtering by file name. Callbacks are provided to process files and the results are collected,
 * either {@link #processFile(Entry)} or {@link #processDir(Entry, Ar)} can be overridden, or both. The entries provided to
 * the callbacks have the original file, the output directory, and the output file. If {@link #setFlattenOutput(boolean)} is
 * false, the output will match the directory structure of the input.
 * <p>
 * 递归收集文件并按文件名过滤。提供回调来处理文件并收集结果,{@link #processFile(Entry)} 或 {@link #processDir(Entry, Ar)} 可以被覆盖,或两者同时覆盖。传递给回调的条目包含原始文件、输出目录和输出文件。如果 {@link #setFlattenOutput(boolean)} 为 false,输出将与输入的目录结构保持一致。
 * @author Nathan Sweet
 */
public class FileProcessor{
    FilenameFilter inputFilter;
    Comparator<File> comparator = Structs.comparing(File::getName);
    Ar<Pattern> inputRegex = new Ar<>();
    String outputSuffix;
    Ar<Entry> outputFiles = new Ar<>();
    boolean recursive = true;
    boolean flattenOutput;

    Comparator<Entry> entryComparator = (o1, o2) -> comparator.compare(o1.inputFile, o2.inputFile);

    public FileProcessor(){
    }

    /**
     * Copy constructor.
     * 复制构造函数。
     */
    public FileProcessor(FileProcessor processor){
        inputFilter = processor.inputFilter;
        comparator = processor.comparator;
        inputRegex.addAll(processor.inputRegex);
        outputSuffix = processor.outputSuffix;
        recursive = processor.recursive;
        flattenOutput = processor.flattenOutput;
    }

    public FileProcessor setInputFilter(FilenameFilter inputFilter){
        this.inputFilter = inputFilter;
        return this;
    }

    /**
     * Sets the comparator for {@link #processDir(Entry, Ar)}. By default the files are sorted by alpha.
     * 设置 {@link #processDir(Entry, Ar)} 使用的比较器。默认按字母顺序排序文件。
     */
    public FileProcessor setComparator(Comparator<File> comparator){
        this.comparator = comparator;
        return this;
    }

    /**
     * Adds a case insensitive suffix for matching input files.
     * 添加一个用于匹配输入文件的不区分大小写的后缀。
     */
    public FileProcessor addInputSuffix(String... suffixes){
        for(String suffix : suffixes)
            addInputRegex("(?i).*" + Pattern.quote(suffix));
        return this;
    }

    public FileProcessor addInputRegex(String... regexes){
        for(String regex : regexes)
            inputRegex.add(Pattern.compile(regex));
        return this;
    }

    /**
     * Sets the suffix for output files, replacing the extension of the input file.
     * 设置输出文件的后缀,替换输入文件的扩展名。
     */
    public FileProcessor setOutputSuffix(String outputSuffix){
        this.outputSuffix = outputSuffix;
        return this;
    }

    public FileProcessor setFlattenOutput(boolean flattenOutput){
        this.flattenOutput = flattenOutput;
        return this;
    }

    /**
     * Default is true.
     * 默认为 true。
     */
    public FileProcessor setRecursive(boolean recursive){
        this.recursive = recursive;
        return this;
    }

    /**
     * @param outputRoot May be null. 可以为 null。
     * @see #process(File, File)
     */
    public Ar<Entry> process(String inputFileOrDir, String outputRoot) throws Exception{
        return process(new File(inputFileOrDir), outputRoot == null ? null : new File(outputRoot));
    }

    /**
     * Processes the specified input file or directory.
     * <p>
     * 处理指定的输入文件或目录。
     * @param outputRoot May be null if there is no output from processing the files. 如果处理文件没有输出,可以为 null。
     * @return the processed files added with {@link #addProcessedFile(Entry)}. 通过 {@link #addProcessedFile(Entry)} 添加的已处理文件
     */
    public Ar<Entry> process(File inputFileOrDir, File outputRoot) throws Exception{
        if(!inputFileOrDir.exists())
            throw new IllegalArgumentException("Input file does not exist: " + inputFileOrDir.getAbsolutePath());
        if(inputFileOrDir.isFile())
            return process(new File[]{inputFileOrDir}, outputRoot);
        else
            return process(inputFileOrDir.listFiles(), outputRoot);
    }

    /**
     * Processes the specified input files.
     * <p>
     * 处理指定的输入文件。
     * @param outputRoot May be null if there is no output from processing the files. 如果处理文件没有输出,可以为 null。
     * @return the processed files added with {@link #addProcessedFile(Entry)}. 通过 {@link #addProcessedFile(Entry)} 添加的已处理文件
     */
    public Ar<Entry> process(File[] files, File outputRoot) throws Exception{
        if(outputRoot == null) outputRoot = new File("");
        outputFiles.clear();

        LinkedHashMap<File, Ar<Entry>> dirToEntries = new LinkedHashMap();
        process(files, outputRoot, outputRoot, dirToEntries, 0);

        Ar<Entry> allEntries = new Ar();
        for(java.util.Map.Entry<File, Ar<Entry>> mapEntry : dirToEntries.entrySet()){
            Ar<Entry> dirEntries = mapEntry.getValue();
            if(comparator != null) dirEntries.sort(entryComparator);

            File inputDir = mapEntry.getKey();
            File newOutputDir = null;
            if(flattenOutput)
                newOutputDir = outputRoot;
            else if(!dirEntries.isEmpty()) //
                newOutputDir = dirEntries.get(0).outputDir;
            String outputName = inputDir.getName();
            if(outputSuffix != null) outputName = outputName.replaceAll("(.*)\\..*", "$1") + outputSuffix;

            Entry entry = new Entry();
            entry.inputFile = mapEntry.getKey();
            entry.outputDir = newOutputDir;
            if(newOutputDir != null)
                entry.outputFile = newOutputDir.length() == 0 ? new File(outputName) : new File(newOutputDir, outputName);

            try{
                processDir(entry, dirEntries);
            }catch(Exception ex){
                throw new Exception("Error processing directory: " + entry.inputFile.getAbsolutePath(), ex);
            }
            allEntries.addAll(dirEntries);
        }

        if(comparator != null) allEntries.sort(entryComparator);
        for(Entry entry : allEntries){
            try{
                processFile(entry);
            }catch(Exception ex){
                throw new Exception("Error processing file: " + entry.inputFile.getAbsolutePath(), ex);
            }
        }

        return outputFiles;
    }

    private void process(File[] files, File outputRoot, File outputDir, LinkedHashMap<File, Ar<Entry>> dirToEntries,
                         int depth){
        // Store empty entries for every directory.
        // 为每个目录存储空条目。
        for(File file : files){
            File dir = file.getParentFile();
            Ar<Entry> entries = dirToEntries.get(dir);
            if(entries == null){
                entries = new Ar<>();
                dirToEntries.put(dir, entries);
            }
        }

        for(File file : files){
            boolean isFile = file.isFile();
            if(isFile){
                if(inputRegex.size > 0){
                    boolean found = false;
                    for(Pattern pattern : inputRegex){
                        if(pattern.matcher(file.getName()).matches()){
                            found = true;
                            break;
                        }
                    }
                    if(!found) continue;
                }

                File dir = file.getParentFile();
                if(inputFilter != null && !inputFilter.accept(dir, file.getName())) continue;

                String outputName = file.getName();
                if(outputSuffix != null) outputName = outputName.replaceAll("(.*)\\..*", "$1") + outputSuffix;

                Entry entry = new Entry();
                entry.depth = depth;
                entry.inputFile = file;
                entry.outputDir = outputDir;

                if(flattenOutput){
                    entry.outputFile = new File(outputRoot, outputName);
                }else{
                    entry.outputFile = new File(outputDir, outputName);
                }

                dirToEntries.get(dir).add(entry);
            }
            if(!isFile && recursive && file.isDirectory()){
                File subdir = outputDir.getPath().length() == 0 ? new File(file.getName()) : new File(outputDir, file.getName());
                process(file.listFiles(inputFilter), outputRoot, subdir, dirToEntries, depth + 1);
            }
        }
    }

    /**
     * Called with each input file.
     * 对每个输入文件调用。
     */
    protected void processFile(Entry entry) throws Exception{
    }

    /**
     * Called for each input directory. The files will be {@link #setComparator(Comparator) sorted}. The specified files list can
     * be modified to change which files are processed.
     * <p>
     * 对每个输入目录调用。文件将按 {@link #setComparator(Comparator) 排序}。可以修改指定的文件列表来更改要处理的文件。
     */
    protected void processDir(Entry entryDir, Ar<Entry> files) throws Exception{
    }

    /**
     * This method should be called by {@link #processFile(Entry)} or {@link #processDir(Entry, Ar)} if the return value of
     * {@link #process(File, File)} or {@link #process(File[], File)} should return all the processed files.
     * <p>
     * 如果 {@link #process(File, File)} 或 {@link #process(File[], File)} 的返回值应包含所有已处理的文件,则 {@link #processFile(Entry)} 或 {@link #processDir(Entry, Ar)} 应调用此方法。
     */
    protected void addProcessedFile(Entry entry){
        outputFiles.add(entry);
    }

    /** @author Nathan Sweet */
    public static class Entry{
        public File inputFile;
        /**
         * May be null.
         * 可以为 null。
         */
        public File outputDir;
        public File outputFile;
        public int depth;

        public Entry(){
        }

        public Entry(File inputFile, File outputFile){
            this.inputFile = inputFile;
            this.outputFile = outputFile;
        }

        public String toString(){
            return inputFile.toString();
        }
    }
}