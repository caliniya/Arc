package arc.assets.loaders;

import arc.assets.AssetLoaderParameters;
import arc.assets.AssetManager;
import arc.files.Fi;

/**
 * Base class for asynchronous {@link AssetLoader} instances. Such loaders try to load parts of an OpenGL resource, like the
 * Pixmap, on a separate thread to then load the actual resource on the thread the OpenGL context is active on.
 * <p>
 * 异步 {@link AssetLoader} 实例的基类。此类加载器会尝试在单独的线程上加载 OpenGL 资源的一部分(例如 Pixmap),然后在 OpenGL 上下文所在的线程上加载实际的资源。
 * @author mzechner
 */
public abstract class AsynchronousAssetLoader<T, P extends AssetLoaderParameters<T>> extends AssetLoader<T, P>{

    public AsynchronousAssetLoader(FileHandleResolver resolver){
        super(resolver);
    }

    /**
     * Loads the non-OpenGL part of the asset and injects any dependencies of the asset into the AssetManager.
     * <p>
     * 加载资产的非 OpenGL 部分,并将资产的任何依赖项注入 AssetManager。
     * @param fileName the name of the asset to load 要加载的资产名称
     * @param file the resolved file to load 已解析的要加载的文件
     * @param parameter the parameters to use for loading the asset 加载资产所用的参数
     */
    public abstract void loadAsync(AssetManager manager, String fileName, Fi file, P parameter);

    /**
     * Loads the OpenGL part of the asset.
     * <p>
     * 加载资产的 OpenGL 部分。
     * @param file the resolved file to load 已解析的要加载的文件
     */
    public abstract T loadSync(AssetManager manager, String fileName, Fi file, P parameter);
}
