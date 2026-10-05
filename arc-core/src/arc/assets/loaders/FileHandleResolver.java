package arc.assets.loaders;

import arc.assets.AssetManager;
import arc.files.Fi;

/**
 * Interface for classes the can map a file name to a {@link Fi}. Used to allow the {@link AssetManager} to load resources
 * from anywhere or implement caching strategies.
 * <p>
 * 用于将文件名映射为 {@link Fi} 的类的接口。用于让 {@link AssetManager} 能够从任意位置加载资源或实现缓存策略。
 * @author mzechner
 */
public interface FileHandleResolver{
    Fi resolve(String fileName);
}
