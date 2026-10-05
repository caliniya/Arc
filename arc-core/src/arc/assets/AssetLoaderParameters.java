package arc.assets;

public class AssetLoaderParameters<T>{
    public LoadedCallback loadedCallback;

    public AssetLoaderParameters(){

    }

    public AssetLoaderParameters(LoadedCallback loadedCallback){
        this.loadedCallback = loadedCallback;
    }

    /**
     * Callback interface that will be invoked when the {@link AssetManager} loaded an asset.
     * <p>
     * 当 {@link AssetManager} 加载完某个资产时会调用的回调接口。
     * @author mzechner
     */
    public interface LoadedCallback{
        void finishedLoading(AssetManager assetManager, String fileName, Class type);
    }
}
