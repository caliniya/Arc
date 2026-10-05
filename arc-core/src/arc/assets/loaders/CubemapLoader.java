package arc.assets.loaders;

import arc.assets.*;
import arc.files.*;
import arc.graphics.*;
import arc.util.*;

/**
 * {@link AssetLoader} for {@link Cubemap} instances. The pixel data is loaded asynchronously. The texture is then created on the
 * rendering thread, synchronously. Passing a {@link CubemapParameter} to
 * {@link AssetManager#load(String, Class, AssetLoaderParameters)} allows one to specify parameters as can be passed to the
 * various Cubemap constructors, e.g. filtering and so on.
 * <p>
 * 用于 {@link Cubemap} 实例的 {@link AssetLoader}。像素数据是异步加载的,而纹理则在渲染线程上同步创建。向 {@link AssetManager#load(String, Class, AssetLoaderParameters)} 传入 {@link CubemapParameter} 可以指定参数,与传递给各种 Cubemap 构造函数的参数相同,例如过滤方式等。
 * @author mzechner, Vincent Bousquet
 */
public class CubemapLoader extends AsynchronousAssetLoader<Cubemap, CubemapLoader.CubemapParameter>{
    private static final String[] names = {"right.png", "left.png", "top.png", "bottom.png", "front.png", "back.png"};
    final CubemapLoaderInfo info = new CubemapLoaderInfo();

    public CubemapLoader(FileHandleResolver resolver){
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, Fi file, CubemapParameter parameter){
        info.pixmaps = new Pixmap[6];
        for(int i = 0; i < 6; i++){
            info.pixmaps[i] = new Pixmap(file.sibling(file.nameWithoutExtension() + names[i]));
        }
    }

    @Override
    public Cubemap loadSync(AssetManager manager, String fileName, Fi file, CubemapParameter parameter){
        if(info.pixmaps == null) return null;

        Cubemap cubemap = parameter == null ? null : parameter.cubemap;
        if(cubemap == null) cubemap = new Cubemap();
        cubemap.load(info.pixmaps, parameter != null && parameter.mipmaps, true);

        if(parameter != null){
            cubemap.setFilter(parameter.minFilter, parameter.magFilter);
            cubemap.setWrap(parameter.wrapU, parameter.wrapV);
        }
        return cubemap;
    }

    public static class CubemapLoaderInfo{
        Pixmap[] pixmaps;
    }

    public static class CubemapParameter extends AssetLoaderParameters<Cubemap>{
        /**
         * The texture to put the data in, optional.
         * 要放入数据的纹理,可选。
         */
        public @Nullable Cubemap cubemap = null;
        public boolean mipmaps;
        public TextureFilter minFilter = TextureFilter.nearest;
        public TextureFilter magFilter = TextureFilter.nearest;
        public TextureWrap wrapU = TextureWrap.clampToEdge;
        public TextureWrap wrapV = TextureWrap.clampToEdge;
    }
}
