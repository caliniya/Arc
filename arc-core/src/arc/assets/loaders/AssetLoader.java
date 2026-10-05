package arc.assets.loaders;

import arc.assets.AssetDescriptor;
import arc.assets.AssetLoaderParameters;
import arc.struct.Ar;
import arc.files.Fi;

/**
 * Abstract base class for asset loaders.
 * <p>
 * 资产加载器的抽象基类。
 * @param <T> the class of the asset the loader supports 加载器支持的资产的类
 * @param <P> the class of the loading parameters the loader supports. 加载器支持的加载参数的类
 * @author mzechner
 */
public abstract class AssetLoader<T, P extends AssetLoaderParameters<T>>{
    /**
     * {@link FileHandleResolver} used to map from plain asset names to {@link Fi} instances
     * 用于将普通资产名称映射为 {@link Fi} 实例的 {@link FileHandleResolver}
     */
    private FileHandleResolver resolver;

    /**
     * Constructor, sets the {@link FileHandleResolver} to use to resolve the file associated with the asset name.
     * <p>
     * 构造函数,设置用于解析与资产名关联的文件的 {@link FileHandleResolver}。
     */
    public AssetLoader(FileHandleResolver resolver){
        this.resolver = resolver;
    }

    /**
     * @param fileName file name to resolve 要解析的文件名
     * @return handle to the file, as resolved by the {@link FileHandleResolver} set on the loader 文件的句柄,由加载器上设置的 {@link FileHandleResolver} 解析得到
     */
    public Fi resolve(String fileName){
        return resolver.resolve(fileName);
    }

    /**
     * Returns the assets this asset requires to be loaded first. This method may be called on a thread other than the GL thread.
     * <p>
     * 返回此资产在加载前所需要加载的资产。此方法可能在 GL 线程以外的线程上调用。
     * @param fileName name of the asset to load 要加载的资产名称
     * @param file the resolved file to load 已解析的要加载的文件
     * @param parameter parameters for loading the asset 加载资产的参数
     * @return other assets that the asset depends on and need to be loaded first or null if there are no dependencies. 该资产依赖且需要先加载的其他资产,如果没有依赖项则为 null
     */
    public Ar<AssetDescriptor> getDependencies(String fileName, Fi file, P parameter){
        return null;
    }
}
