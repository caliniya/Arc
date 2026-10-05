package arc.assets;

import arc.*;
import arc.assets.loaders.*;
import arc.audio.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.font.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;

import java.util.concurrent.*;

/**
 * Loads and stores assets like textures, bitmapfonts, tile maps, sounds, music and so on.
 * <p>
 * 加载并存储纹理、位图字体、瓦片地图、音效、音乐等资产。
 * @author mzechner
 */
@SuppressWarnings("unchecked")
public class AssetManager implements Disposable{
    final ObjectMap<Class, ObjectMap<String, RefCountedContainer>> assets = new ObjectMap<>();
    final ObjectMap<String, Class> assetTypes = new ObjectMap<>();
    final ObjectMap<String, Ar<String>> assetDependencies = new ObjectMap<>();
    final ObjectSet<String> injected = new ObjectSet<>();

    final ObjectMap<Class, ObjectMap<String, AssetLoader>> loaders = new ObjectMap<>();
    final Ar<AssetDescriptor> loadQueue = new Ar<>();
    final ExecutorService executor;

    final Ar<AssetLoadingTask> tasks = new Ar<>();
    final FileHandleResolver resolver;
    AssetErrorListener listener = null;
    int loaded = 0;
    int toLoad = 0;
    int peakTasks = 0;

    /**
     * Creates a new AssetManager with all default loaders.
     * 使用所有默认加载器创建一个新的 AssetManager。
     */
    public AssetManager(){
        this(Core.files::internal);
    }

    /**
     * Creates a new AssetManager with all default loaders.
     * 使用所有默认加载器创建一个新的 AssetManager。
     */
    public AssetManager(FileHandleResolver resolver){
        this(resolver, true);
    }

    /**
     * Creates a new AssetManager with optionally all default loaders. If you don't add the default loaders then you do have to
     * manually add the loaders you need, including any loaders they might depend on.
     * <p>
     * 创建一个新的 AssetManager,可选择添加所有默认加载器。如果不添加默认加载器,则必须手动添加所需的加载器,包括它们可能依赖的加载器。
     * @param defaultLoaders whether to add the default loaders 是否添加默认加载器
     */
    public AssetManager(FileHandleResolver resolver, boolean defaultLoaders){
        this.resolver = resolver;
        if(defaultLoaders){
            setLoader(Font.class, new FontLoader(resolver));
            setLoader(Music.class, new MusicLoader(resolver));
            setLoader(Pixmap.class, new PixmapLoader(resolver));
            setLoader(Sound.class, new SoundLoader(resolver));
            setLoader(TextureAtlas.class, new TextureAtlasLoader(resolver));
            setLoader(Texture.class, new TextureLoader(resolver));
            setLoader(I18NBundle.class, new I18NBundleLoader(resolver));
            setLoader(Shader.class, new ShaderProgramLoader(resolver));
            setLoader(Cubemap.class, new CubemapLoader(resolver));
        }
        executor = Threads.executor("Assets", 1);
    }

    /**
     * Returns the {@link FileHandleResolver} for which this AssetManager was loaded with.
     * <p>
     * 返回此 AssetManager 加载时所使用的 {@link FileHandleResolver}。
     * @return the file handle resolver which this AssetManager uses 此 AssetManager 使用的文件句柄解析器
     */
    public FileHandleResolver getFileHandleResolver(){
        return resolver;
    }

    /**
     * @param fileName the asset file name 资产文件名
     * @return the asset 资产
     */
    public synchronized <T> T get(String fileName){
        Class<T> type = assetTypes.get(fileName);
        if(type == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
        if(assetsByType == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        RefCountedContainer assetContainer = assetsByType.get(fileName);
        if(assetContainer == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        T asset = (T)assetContainer.object;
        if(asset == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        return asset;
    }

    /**
     * @param fileName the asset file name 资产文件名
     * @param type the asset type 资产类型
     * @return the asset 资产
     */
    public synchronized <T> T get(String fileName, Class<T> type){
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
        if(assetsByType == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        RefCountedContainer assetContainer = assetsByType.get(fileName);
        if(assetContainer == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        T asset = (T)assetContainer.object;
        if(asset == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        return asset;
    }

    /**
     * @param fileName the asset file name 资产文件名
     * @param type the asset type 资产类型
     * @return the asset, or null if not found 资产,如果未找到则为 null
     */
    public synchronized <T> T getOrNull(String fileName, Class<T> type){
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
        if(assetsByType == null) return null;
        RefCountedContainer assetContainer = assetsByType.get(fileName);
        if(assetContainer == null) return null;
        return (T)assetContainer.object;
    }

    /**
     * @param type the asset type 资产类型
     * @return all the assets matching the specified type 匹配指定类型的所有资产
     */
    public synchronized <T> Ar<T> getAll(Class<T> type, Ar<T> out){
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
        if(assetsByType != null){
            for(ObjectMap.Entry<String, RefCountedContainer> asset : assetsByType.entries()){
                out.add((T)asset.value.object);
            }
        }
        return out;
    }

    /**
     * @param type the asset type 资产类型
     * @return all the assets matching the specified type as entries by file name 以文件名条目形式返回匹配指定类型的所有资产
     */
    public synchronized <T> Ar<Entry<String, T>> getAllEntries(Class<T> type, Ar<Entry<String, T>> out){
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
        if(assetsByType != null){
            for(ObjectMap.Entry<String, RefCountedContainer> asset : assetsByType.entries()){
                Entry<String, T> entry = new Entry<>();
                entry.key = asset.key;
                entry.value = (T)asset.value.object;
                out.add(entry);
            }
        }
        return out;
    }

    /**
     * @param assetDescriptor the asset descriptor 资产描述符
     * @return the asset 资产
     */
    public synchronized <T> T get(AssetDescriptor<T> assetDescriptor){
        return get(assetDescriptor.fileName, assetDescriptor.type);
    }

    /**
     * Returns true if an asset with the specified name is loading, queued to be loaded, or has been loaded.
     * 如果具有指定名称的资产正在加载、已排队等待加载或已加载,则返回 true。
     */
    public synchronized boolean contains(String fileName){
        if(tasks.size > 0 && tasks.first().assetDesc.fileName.equals(fileName)) return true;

        for(int i = 0; i < loadQueue.size; i++)
            if(loadQueue.get(i).fileName.equals(fileName)) return true;

        return isLoaded(fileName);
    }

    /**
     * Returns true if an asset with the specified name and type is loading, queued to be loaded, or has been loaded.
     * 如果具有指定名称和类型的资产正在加载、已排队等待加载或已加载,则返回 true。
     */
    public synchronized boolean contains(String fileName, Class type){
        if(tasks.size > 0){
            AssetDescriptor assetDesc = tasks.first().assetDesc;
            if(assetDesc.type == type && assetDesc.fileName.equals(fileName)) return true;
        }

        for(int i = 0; i < loadQueue.size; i++){
            AssetDescriptor assetDesc = loadQueue.get(i);
            if(assetDesc.type == type && assetDesc.fileName.equals(fileName)) return true;
        }

        return isLoaded(fileName, type);
    }

    /**
     * Removes the asset and all its dependencies, if they are not used by other assets.
     * <p>
     * 移除该资产及其所有依赖项(前提是它们没有被其他资产使用)。
     * @param fileName the file name 文件名
     */
    public synchronized void unload(String fileName){
        // check if it's currently processed (and the first element in the stack, thus not a dependency) and cancel if necessary
        // 检查它当前是否正在被处理(并且是栈中的第一个元素,即并非依赖项),必要时取消
        if(tasks.size > 0){
            AssetLoadingTask currAsset = tasks.first();
            if(currAsset.assetDesc.fileName.equals(fileName)){
                currAsset.cancel = true;
                return;
            }
        }

        // check if it's in the queue
        // 检查它是否在队列中
        int foundIndex = -1;
        for(int i = 0; i < loadQueue.size; i++){
            if(loadQueue.get(i).fileName.equals(fileName)){
                foundIndex = i;
                break;
            }
        }
        if(foundIndex != -1){
            toLoad--;
            loadQueue.remove(foundIndex);
            return;
        }

        // get the asset and its type
        // 获取该资产及其类型
        Class type = assetTypes.get(fileName);
        if(type == null) return;

        RefCountedContainer assetRef = assets.get(type).get(fileName);

        if(assetRef == null) return;

        // if it is reference counted, decrement ref count and check if we can really get rid of it.
        // 如果启用了引用计数,则减少引用计数并检查是否真的可以将其移除。
        assetRef.count--;
        if(assetRef.count <= 0){

            // if it is disposable dispose it
            // 如果可销毁则销毁它
            if(assetRef.object instanceof Disposable)
                ((Disposable)assetRef.object).dispose();

            // remove the asset from the manager.
            // 从管理器中移除该资产。
            assetTypes.remove(fileName);
            assets.get(type).remove(fileName);
        }

        // remove any dependencies (or just decrement their ref count).
        // 移除所有依赖项(或仅减少它们的引用计数)。
        Ar<String> dependencies = assetDependencies.get(fileName);
        if(dependencies != null){
            for(String dependency : dependencies){
                if(isLoaded(dependency)) unload(dependency);
            }
        }
        // remove dependencies if ref count < 0
        // 引用计数 < 0 时移除依赖项
        if(assetRef.count <= 0){
            assetDependencies.remove(fileName);
        }
    }

    /**
     * @param asset the asset 资产
     * @return whether the asset is contained in this manager 此管理器是否包含该资产
     */
    public synchronized <T> boolean containsAsset(T asset){
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(asset.getClass());
        if(assetsByType == null) return false;
        for(String fileName : assetsByType.keys()){
            T otherAsset = (T)assetsByType.get(fileName).object;
            if(otherAsset == asset || asset.equals(otherAsset)) return true;
        }
        return false;
    }

    /**
     * @param asset the asset 资产
     * @return the filename of the asset or null 该资产的文件名,否则为 null
     */
    public synchronized <T> String getAssetFileName(T asset){
        for(Class assetType : assets.keys()){
            ObjectMap<String, RefCountedContainer> assetsByType = assets.get(assetType);
            for(String fileName : assetsByType.keys()){
                T otherAsset = (T)assetsByType.get(fileName).object;
                if(otherAsset == asset || asset.equals(otherAsset)) return fileName;
            }
        }
        return null;
    }

    /**
     * @param assetDesc the AssetDescriptor of the asset 该资产的 AssetDescriptor 描述符
     * @return whether the asset is loaded 该资产是否已加载
     */
    public synchronized boolean isLoaded(AssetDescriptor assetDesc){
        return isLoaded(assetDesc.fileName);
    }

    /**
     * @param fileName the file name of the asset 资产的文件名
     * @return whether the asset is loaded 该资产是否已加载
     */
    public synchronized boolean isLoaded(String fileName){
        if(fileName == null) return false;
        return assetTypes.containsKey(fileName);
    }

    /**
     * @param fileName the file name of the asset 资产的文件名
     * @return whether the asset is loaded 该资产是否已加载
     */
    public synchronized boolean isLoaded(String fileName, Class type){
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
        if(assetsByType == null) return false;
        RefCountedContainer assetContainer = assetsByType.get(fileName);
        if(assetContainer == null) return false;
        return assetContainer.object != null;
    }

    /**
     * Returns the default loader for the given type
     * <p>
     * 返回给定类型的默认加载器
     * @param type The type of the loader to get 要获取的加载器类型
     * @return The loader capable of loading the type, or null if none exists 能够加载该类型的加载器,如果不存在则为 null
     */
    public <T> AssetLoader getLoader(final Class<T> type){
        return getLoader(type, null);
    }

    /**
     * Returns the loader for the given type and the specified filename. If no loader exists for the specific filename, the
     * default loader for that type is returned.
     * <p>
     * 返回给定类型和指定文件名对应的加载器。如果没有针对该具体文件名的加载器,则返回该类型的默认加载器。
     * @param type The type of the loader to get 要获取的加载器类型
     * @param fileName The filename of the asset to get a loader for, or null to get the default loader 要为其获取加载器的资产文件名,为 null 时获取默认加载器
     * @return The loader capable of loading the type and filename, or null if none exists 能够加载该类型和文件名的加载器,如果不存在则为 null
     */
    public <T> AssetLoader getLoader(final Class<T> type, final String fileName){
        final ObjectMap<String, AssetLoader> loaders = this.loaders.get(type);
        if(loaders == null || loaders.size < 1) return null;
        if(fileName == null) return loaders.get("");
        AssetLoader result = null;
        int l = -1;
        for(ObjectMap.Entry<String, AssetLoader> entry : loaders.entries()){
            if(entry.key.length() > l && fileName.endsWith(entry.key)){
                result = entry.value;
                l = entry.key.length();
            }
        }
        return result;
    }

    /**
     * Adds the given asset to the loading queue of the AssetManager.
     * <p>
     * 将给定资产加入 AssetManager 的加载队列。
     * @param fileName the file name (interpretation depends on {@link AssetLoader}) 文件名(解释方式取决于 {@link AssetLoader})
     * @param type the type of the asset. 资产的类型。
     */
    public synchronized <T> AssetDescriptor<T> load(String fileName, Class<T> type){
        return load(fileName, type, null);
    }

    /**
     * Loads a custom one-time 'asset' that knows how to load itself.
     * <p>
     * 加载一个知道如何加载自身的自定义一次性“资产”。
     */
    public synchronized AssetDescriptor loadRun(String name, Class<?> type, Runnable loadasync){
        return loadRun(name, type, loadasync, () -> {});
    }

    /**
     * Loads a custom one-time 'asset' that knows how to load itself.
     * <p>
     * 加载一个知道如何加载自身的自定义一次性“资产”。
     */
    public synchronized AssetDescriptor loadRun(String name, Class<?> type, Runnable loadasync, Runnable loadsync){
        if(getLoader(type) == null){
            setLoader(type, new CustomLoader(){
                @Override
                public void loadAsync(AssetManager manager, String fileName, Fi file, AssetLoaderParameters parameter){
                    loadasync.run();
                }

                @Override
                public Object loadSync(AssetManager manager, String fileName, Fi file, AssetLoaderParameters parameter){
                    loadsync.run();
                    return super.loadSync(manager, fileName, file, parameter);
                }
            });
        }else{
            throw new IllegalArgumentException("Class already registered or loaded: " + type);
        }
        return load(name, type, null);
    }

    /**
     * Loads a custom one-time 'asset' that knows how to load itself.
     * <p>
     * 加载一个知道如何加载自身的自定义一次性“资产”。
     * @param load the asset 要加载的资产
     */
    public synchronized AssetDescriptor load(Loadable load){
        if(getLoader(load.getClass()) == null){
            setLoader(load.getClass(), new AsynchronousAssetLoader(Core.files::internal){
                @Override
                public void loadAsync(AssetManager manager, String fileName, Fi file, AssetLoaderParameters parameter){
                    load.loadAsync();
                }

                @Override
                public Object loadSync(AssetManager manager, String fileName, Fi file, AssetLoaderParameters parameter){
                    load.loadSync();
                    return load;
                }

                @Override
                public Ar<AssetDescriptor> getDependencies(String fileName, Fi file, AssetLoaderParameters parameter){
                    return load.getDependencies();
                }
            });
        }
        return load(load.getName(), load.getClass(), null);
    }

    /**
     * Adds the given asset to the loading queue of the AssetManager.
     * <p>
     * 将给定资产加入 AssetManager 的加载队列。
     * @param fileName the file name (interpretation depends on {@link AssetLoader}) 文件名(解释方式取决于 {@link AssetLoader})
     * @param type the type of the asset. 资产的类型。
     * @param parameter parameters for the AssetLoader. AssetLoader 的参数。
     */
    public synchronized <T> AssetDescriptor<T> load(String fileName, Class<T> type, AssetLoaderParameters<T> parameter){
        AssetLoader loader = getLoader(type, fileName);
        if(loader == null) throw new ArcRuntimeException("No loader for type: " + type.getSimpleName());

        // reset stats
        // 重置统计信息
        if(loadQueue.size == 0){
            loaded = 0;
            toLoad = 0;
            peakTasks = 0;
        }

        // check if an asset with the same name but a different type has already been added.
        // 检查是否已添加同名但类型不同的资产。

        // check preload queue
        // 检查预加载队列
        for(int i = 0; i < loadQueue.size; i++){
            AssetDescriptor desc = loadQueue.get(i);
            if(desc.fileName.equals(fileName) && !desc.type.equals(type)) throw new ArcRuntimeException(
            "Asset with name '" + fileName + "' already in preload queue, but has different type (expected: "
            + type.getSimpleName() + ", found: " + desc.type.getSimpleName() + ")");
        }

        // check task list
        // 检查任务列表
        for(int i = 0; i < tasks.size; i++){
            AssetDescriptor desc = tasks.get(i).assetDesc;
            if(desc.fileName.equals(fileName) && !desc.type.equals(type)) throw new ArcRuntimeException(
            "Asset with name '" + fileName + "' already in task list, but has different type (expected: "
            + type.getSimpleName() + ", found: " + desc.type.getSimpleName() + ")");
        }

        // check loaded assets
        // 检查已加载的资产
        Class otherType = assetTypes.get(fileName);
        if(otherType != null && !otherType.equals(type))
            throw new ArcRuntimeException("Asset with name '" + fileName + "' already loaded, but has different type (expected: "
            + type.getSimpleName() + ", found: " + otherType.getSimpleName() + ")");

        toLoad++;
        AssetDescriptor assetDesc = new AssetDescriptor<>(fileName, type, parameter);
        loadQueue.add(assetDesc);
        return assetDesc;
    }

    /**
     * Adds the given asset to the loading queue of the AssetManager.
     * <p>
     * 将给定资产加入 AssetManager 的加载队列。
     * @param desc the {@link AssetDescriptor} 该 {@link AssetDescriptor}
     */
    public synchronized <T> AssetDescriptor<T> load(AssetDescriptor<T> desc){
        return load(desc.fileName, desc.type, desc.params);
    }

    /**
     * Updates the AssetManager, keeping it loading any assets in the preload queue.
     * <p>
     * 更新 AssetManager,使其持续加载预加载队列中的资产。
     * @return true if all loading is finished. 如果所有加载已完成则为 true
     */
    public synchronized boolean update(){
        try{
            if(tasks.size == 0){
                // loop until we have a new task ready to be processed
                // 循环直到有新任务可以处理
                while(loadQueue.size != 0 && tasks.size == 0){
                    nextTask();
                }
                // have we not found a task? We are done!
                // 没有找到任务?完成了!
                if(tasks.size == 0) return true;
            }
            return updateTask() && loadQueue.size == 0 && tasks.size == 0;
        }catch(Throwable t){
            handleTaskError(t);
            return loadQueue.size == 0;
        }
    }

    /** @return the asset loading task that is currently being processed.
     * May return null if nothing is being loaded. 当前正在处理的资产加载任务。如果当前没有加载任何内容,可能返回 null。 */
    public synchronized AssetDescriptor getCurrentLoading(){
        if(tasks.size > 0){
            return tasks.first().assetDesc;
        }
        return null;
    }

    /**
     * Updates the AssetManager continuously for the specified number of milliseconds, yielding the CPU to the loading thread
     * between updates. This may block for less time if all loading tasks are complete. This may block for more time if the portion
     * of a single task that happens in the GL thread takes a long time.
     * <p>
     * 在指定的毫秒数内持续更新 AssetManager,并在两次更新之间让出 CPU 给加载线程。如果所有加载任务都已完成,阻塞时间可能更短;如果单个任务在 GL 线程中执行的部分耗时较长,阻塞时间可能更长。
     * @return true if all loading is finished. 如果所有加载已完成则为 true
     */
    public boolean update(int millis){
        long endTime = Time.millis() + millis;
        while(true){
            boolean done = update();
            if(done || Time.millis() > endTime) return done;
            Thread.yield();
        }
    }

    /**
     * Returns true when all assets are loaded. Can be called from any thread.
     * 当所有资产都加载完成时返回 true。可以在任意线程中调用。
     */
    public synchronized boolean isFinished(){
        return loadQueue.size == 0 && tasks.size == 0;
    }

    /**
     * Blocks until all assets are loaded.
     * 阻塞直到所有资产加载完成。
     */
    public void finishLoading(){
        while(!update())
            Thread.yield();
    }

    /**
     * Blocks until the specified asset is loaded.
     * <p>
     * 阻塞直到指定资产加载完成。
     * @param assetDesc the AssetDescriptor of the asset 该资产的 AssetDescriptor 描述符
     */
    public void finishLoadingAsset(AssetDescriptor assetDesc){
        finishLoadingAsset(assetDesc.fileName);
    }

    /**
     * Blocks until the specified asset is loaded.
     * <p>
     * 阻塞直到指定资产加载完成。
     * @param fileName the file name (interpretation depends on {@link AssetLoader}) 文件名(解释方式取决于 {@link AssetLoader})
     */
    public void finishLoadingAsset(String fileName){
        while(!isLoaded(fileName)){
            update();
            Thread.yield();
        }
    }

    synchronized void injectDependencies(String parentAssetFilename, Ar<AssetDescriptor> dependendAssetDescs){
        ObjectSet<String> injected = this.injected;
        for(AssetDescriptor desc : dependendAssetDescs){
            if(injected.contains(desc.fileName)) continue; // Ignore subsequent dependencies if there are duplicates.
            // 如果存在重复,忽略后续的依赖项。
            injected.add(desc.fileName);
            injectDependency(parentAssetFilename, desc);
        }
        injected.clear();
    }

    private synchronized void injectDependency(String parentAssetFilename, AssetDescriptor dependendAssetDesc){
        // add the asset as a dependency of the parent asset
        // 将该资产添加为父资产的依赖项
        Ar<String> dependencies = assetDependencies.get(parentAssetFilename);
        if(dependencies == null){
            dependencies = new Ar();
            assetDependencies.put(parentAssetFilename, dependencies);
        }
        dependencies.add(dependendAssetDesc.fileName);

        // if the asset is already loaded, increase its reference count.
        // 如果资产已加载,增加其引用计数。
        if(isLoaded(dependendAssetDesc.fileName)){
            Class type = assetTypes.get(dependendAssetDesc.fileName);
            RefCountedContainer assetRef = assets.get(type).get(dependendAssetDesc.fileName);
            assetRef.count++;
            incrementRefCountedDependencies(dependendAssetDesc.fileName);
        }
        // else add a new task for the asset.
        // 否则为该资产添加一个新任务。
        else{
            addTask(dependendAssetDesc);
        }
    }

    /**
     * Removes a task from the loadQueue and adds it to the task stack. If the asset is already loaded (which can happen if it was
     * a dependency of a previously loaded asset) its reference count will be increased.
     * <p>
     * 从加载队列中移除一个任务并将其压入任务栈。如果资产已加载(可能是之前加载的资产的依赖项),则会增加其引用计数。
     */
    private void nextTask(){
        AssetDescriptor assetDesc = loadQueue.remove(0);
        //Log.info("Loading asset task: {0}", assetDesc.fileName);

        // if the asset not meant to be reloaded and is already loaded, increase its reference count
        // 如果资产不打算重新加载且已加载,则增加其引用计数
        if(isLoaded(assetDesc.fileName)){
            Class type = assetTypes.get(assetDesc.fileName);
            RefCountedContainer assetRef = assets.get(type).get(assetDesc.fileName);
            assetRef.count++;
            incrementRefCountedDependencies(assetDesc.fileName);
            if(assetDesc.params != null && assetDesc.params.loadedCallback != null){
                assetDesc.params.loadedCallback.finishedLoading(this, assetDesc.fileName, assetDesc.type);
            }
            loaded++;
        }else{
            // else add a new task for the asset.
            // 否则为该资产添加一个新任务。
            addTask(assetDesc);
        }
    }

    /**
     * Adds a {@link AssetLoadingTask} to the task stack for the given asset.
     * <p>
     * 为给定资产向任务栈中添加一个 {@link AssetLoadingTask}。
     */
    private void addTask(AssetDescriptor assetDesc){
        AssetLoader loader = getLoader(assetDesc.type, assetDesc.fileName);
        if(loader == null)
            throw new ArcRuntimeException("No loader for type: " + assetDesc.type.getSimpleName());
        tasks.add(new AssetLoadingTask(this, assetDesc, loader, executor));
        peakTasks++;
    }

    /**
     * Adds an asset to this AssetManager
     * 向此 AssetManager 添加一个资产
     */
    public <T> void addAsset(final String fileName, Class<T> type, T asset){
        // add the asset to the filename lookup
        // 将资产添加到文件名查找表中
        assetTypes.put(fileName, type);

        // add the asset to the type lookup
        // 将资产添加到类型查找表中
        ObjectMap<String, RefCountedContainer> typeToAssets = assets.get(type);
        if(typeToAssets == null){
            typeToAssets = new ObjectMap<>();
            assets.put(type, typeToAssets);
        }
        typeToAssets.put(fileName, new RefCountedContainer(asset));
    }

    /**
     * Updates the current task on the top of the task stack.
     * <p>
     * 更新任务栈顶部的当前任务。
     * @return true if the asset is loaded or the task was cancelled. 如果资产已加载或任务已取消则为 true
     */
    private boolean updateTask(){
        AssetLoadingTask task = tasks.peek();

        boolean complete = true;
        try{
            complete = task.cancel || task.update();
        }catch(RuntimeException ex){
            task.cancel = true;
            taskFailed(task.assetDesc, ex);
        }

        // if the task has been cancelled or has finished loading
        // 如果任务已被取消或已完成加载
        if(complete){
            // increase the number of loaded assets and pop the task from the stack
            // 增加已加载资产的数量,并将任务从栈中弹出
            if(tasks.size == 1){
                loaded++;
                peakTasks = 0;
            }
            tasks.pop();

            if(task.cancel) return true;

            addAsset(task.assetDesc.fileName, task.assetDesc.type, task.getAsset());

            // otherwise, if a listener was found in the parameter invoke it
            // 否则,如果在参数中找到了监听器,则调用它
            if(task.assetDesc.params != null && task.assetDesc.params.loadedCallback != null){
                task.assetDesc.params.loadedCallback.finishedLoading(this, task.assetDesc.fileName, task.assetDesc.type);
            }

            task.assetDesc.loaded.get(task.getAsset());

            return true;
        }
        return false;
    }

    /**
     * Called when a task throws an exception during loading. The default implementation rethrows the exception. A subclass may
     * supress the default implementation when loading assets where loading failure is recoverable.
     * <p>
     * 当任务在加载过程中抛出异常时调用。默认实现会重新抛出该异常。在加载失败可以恢复的资产时,子类可以抑制默认实现。
     */
    protected void taskFailed(AssetDescriptor assetDesc, RuntimeException ex){
        throw ex;
    }

    private void incrementRefCountedDependencies(String parent){
        Ar<String> dependencies = assetDependencies.get(parent);
        if(dependencies == null) return;

        for(String dependency : dependencies){
            Class type = assetTypes.get(dependency);
            RefCountedContainer assetRef = assets.get(type).get(dependency);
            assetRef.count++;
            incrementRefCountedDependencies(dependency);
        }
    }

    /**
     * Handles a runtime/loading error in {@link #update()} by optionally invoking the {@link AssetErrorListener}.
     * <p>
     * 处理 {@link #update()} 中的运行时/加载错误,可选地调用 {@link AssetErrorListener}。
     */
    private void handleTaskError(Throwable t){
        if(tasks.isEmpty()) throw new ArcRuntimeException(t);

        // pop the faulty task from the stack
        // 将出错的任务从栈中弹出
        AssetLoadingTask task = tasks.pop();
        AssetDescriptor assetDesc = task.assetDesc;

        // remove all dependencies
        // 移除所有依赖项
        if(task.dependenciesLoaded && task.dependencies != null){
            for(AssetDescriptor desc : task.dependencies){
                unload(desc.fileName);
            }
        }

        // clear the rest of the stack
        // 清空栈的其余部分
        tasks.clear();

        // inform the listener that something bad happened
        // 通知监听器发生了错误
        if(listener != null){
            listener.error(assetDesc, t);
        }

        if(assetDesc.errored != null){
            assetDesc.errored.get(t);
        }else{
            throw new ArcRuntimeException(t);
        }
    }

    /**
     * Sets a new {@link AssetLoader} for the given type.
     * <p>
     * 为给定类型设置新的 {@link AssetLoader}。
     * @param type the type of the asset 资产的类型
     * @param loader the loader 加载器
     */
    public synchronized <T, P extends AssetLoaderParameters<T>> void setLoader(Class<T> type, AssetLoader<T, P> loader){
        setLoader(type, null, loader);
    }

    /**
     * Sets a new {@link AssetLoader} for the given type.
     * <p>
     * 为给定类型设置新的 {@link AssetLoader}。
     * @param type the type of the asset 资产的类型
     * @param suffix the suffix the filename must have for this loader to be used or null to specify the default loader. 文件名必须具有的后缀才会使用此加载器,为 null 时指定为默认加载器。
     * @param loader the loader 加载器
     */
    public synchronized <T, P extends AssetLoaderParameters<T>> void setLoader(Class<T> type, String suffix,
                                                                               AssetLoader<T, P> loader){
        if(type == null) throw new IllegalArgumentException("type cannot be null.");
        if(loader == null) throw new IllegalArgumentException("loader cannot be null.");
        ObjectMap<String, AssetLoader> loaders = this.loaders.get(type);
        if(loaders == null) this.loaders.put(type, loaders = new ObjectMap<>());
        loaders.put(suffix == null ? "" : suffix, loader);
    }

    /**
     * @return the number of loaded assets 已加载资产的数量
     */
    public synchronized int getLoadedAssets(){
        return assetTypes.size;
    }

    /**
     * @return the number of currently queued assets 当前排队中的资产数量
     */
    public synchronized int getQueuedAssets(){
        return loadQueue.size + tasks.size;
    }

    /**
     * @return the progress in percent of completion. 完成进度百分比
     */
    public synchronized float getProgress(){
        if(toLoad == 0) return 1;
        float fractionalLoaded = (float)loaded;
        if(peakTasks > 0){
            fractionalLoaded += ((peakTasks - tasks.size) / (float)peakTasks);
        }
        return Math.min(1, fractionalLoaded / (float)toLoad);
    }

    /**
     * Sets an {@link AssetErrorListener} to be invoked in case loading an asset failed.
     * <p>
     * 设置一个 {@link AssetErrorListener},在加载资产失败时调用。
     * @param listener the listener or null 监听器,或为 null
     */
    public synchronized void setErrorListener(AssetErrorListener listener){
        this.listener = listener;
    }

    /**
     * Disposes all assets in the manager and stops all asynchronous loading.
     * 销毁管理器中的所有资产并停止所有异步加载。
     */
    @Override
    public synchronized void dispose(){
        clear();
        Threads.await(executor);
    }

    /**
     * Clears and disposes all assets and the preloading queue.
     * 清空并销毁所有资产以及预加载队列。
     */
    public synchronized void clear(){
        loadQueue.clear();
        while(!update());

        ObjectIntMap<String> dependencyCount = new ObjectIntMap<>();
        while(assetTypes.size > 0){
            // for each asset, figure out how often it was referenced
            // 统计每个资产被引用的次数
            dependencyCount.clear();
            Ar<String> assets = assetTypes.keys().toSeq();
            for(String asset : assets){
                dependencyCount.put(asset, 0);
            }

            for(String asset : assets){
                Ar<String> dependencies = assetDependencies.get(asset);
                if(dependencies == null) continue;
                for(String dependency : dependencies){
                    int count = dependencyCount.get(dependency, 0);
                    count++;
                    dependencyCount.put(dependency, count);
                }
            }

            // only dispose of assets that are root assets (not referenced)
            // 只销毁根资产(未被引用的资产)
            for(String asset : assets){
                if(dependencyCount.get(asset, 0) == 0){
                    unload(asset);
                }
            }
        }

        this.assets.clear();
        this.assetTypes.clear();
        this.assetDependencies.clear();
        this.loaded = 0;
        this.toLoad = 0;
        this.peakTasks = 0;
        this.loadQueue.clear();
        this.tasks.clear();
    }

    /**
     * Returns the reference count of an asset.
     * <p>
     * 返回资产的引用计数。
     */
    public synchronized int getReferenceCount(String fileName){
        Class type = assetTypes.get(fileName);
        if(type == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        return assets.get(type).get(fileName).count;
    }

    /**
     * Sets the reference count of an asset.
     * <p>
     * 设置资产的引用计数。
     */
    public synchronized void setReferenceCount(String fileName, int refCount){
        Class type = assetTypes.get(fileName);
        if(type == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        assets.get(type).get(fileName).count = refCount;
    }

    /**
     * @return a string containing ref count and dependency information for all assets. 包含所有资产引用计数和依赖信息的字符串
     */
    public synchronized String getDiagnostics(){
        StringBuilder sb = new StringBuilder();
        for(String fileName : assetTypes.keys()){
            sb.append(fileName);
            sb.append(", ");

            Class type = assetTypes.get(fileName);
            RefCountedContainer assetRef = assets.get(type).get(fileName);
            Ar<String> dependencies = assetDependencies.get(fileName);

            sb.append(type.getSimpleName());

            sb.append(", refs: ");
            sb.append(assetRef.count);

            if(dependencies != null){
                sb.append(", deps: [");
                for(String dep : dependencies){
                    sb.append(dep);
                    sb.append(",");
                }
                sb.append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * @return the file names of all loaded assets. 所有已加载资产的文件名
     */
    public synchronized Ar<String> getAssetNames(){
        return assetTypes.keys().toSeq();
    }

    /**
     * @return the dependencies of an asset or null if the asset has no dependencies. 资产的依赖项,如果资产没有依赖项则为 null
     */
    public synchronized Ar<String> getDependencies(String fileName){
        return assetDependencies.get(fileName);
    }

    /**
     * @return the type of a loaded asset. 已加载资产的类型
     */
    public synchronized Class getAssetType(String fileName){
        return assetTypes.get(fileName);
    }

    static class RefCountedContainer{
        Object object;
        int count = 1;

        public RefCountedContainer(Object object){
            if(object == null) throw new IllegalArgumentException("Object must not be null");
            this.object = object;
        }
    }
}
