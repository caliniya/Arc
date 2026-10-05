package arc.assets.loaders;

import arc.assets.AssetDescriptor;
import arc.assets.AssetLoaderParameters;
import arc.assets.AssetManager;
import arc.assets.loaders.FontLoader.*;
import arc.struct.Ar;
import arc.files.Fi;
import arc.graphics.Texture;
import arc.graphics.TextureFilter;
import arc.graphics.font.Font;
import arc.graphics.font.Font.FontData;
import arc.graphics.g2d.TextureAtlas;
import arc.graphics.g2d.TextureAtlas.AtlasRegion;
import arc.graphics.g2d.TextureRegion;
import arc.util.ArcRuntimeException;

/**
 * {@link AssetLoader} for {@link Font} instances. Loads the font description file (.fnt) asynchronously, loads the
 * {@link Texture} containing the glyphs as a dependency. The {@link FontParameter} allows you to set things like texture
 * filters or whether to flip the glyphs vertically.
 * <p>
 * 用于 {@link Font} 实例的 {@link AssetLoader}。异步加载字体描述文件(.fnt),并将包含字形的 {@link Texture} 作为依赖项加载。{@link FontParameter} 允许你设置纹理过滤方式或是否垂直翻转字形等选项。
 * @author mzechner
 */
public class FontLoader extends AsynchronousAssetLoader<Font, FontParameter>{
    FontData data;

    public FontLoader(FileHandleResolver resolver){
        super(resolver);
    }

    @Override
    public Ar<AssetDescriptor> getDependencies(String fileName, Fi file, FontParameter parameter){
        Ar<AssetDescriptor> deps = new Ar();
        if(parameter != null && parameter.fontData != null){
            data = parameter.fontData;
            return deps;
        }

        data = new FontData(file, parameter != null && parameter.flip);
        if(parameter != null && parameter.atlasName != null){
            deps.add(new AssetDescriptor(parameter.atlasName, TextureAtlas.class));
        }else{
            for(int i = 0; i < data.getImagePaths().length; i++){
                String path = data.getImagePath(i);
                Fi resolved = resolve(path);

                TextureLoader.TextureParameter textureParams = new TextureLoader.TextureParameter();

                if(parameter != null){
                    textureParams.genMipMaps = parameter.genMipMaps;
                    textureParams.minFilter = parameter.minFilter;
                    textureParams.magFilter = parameter.magFilter;
                }

                AssetDescriptor descriptor = new AssetDescriptor(resolved, Texture.class, textureParams);
                deps.add(descriptor);
            }
        }

        return deps;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, Fi file, FontParameter parameter){
    }

    @Override
    public Font loadSync(AssetManager manager, String fileName, Fi file, FontParameter parameter){
        if(parameter != null && parameter.atlasName != null){
            TextureAtlas atlas = manager.get(parameter.atlasName, TextureAtlas.class);
            String name = file.sibling(data.imagePaths[0]).nameWithoutExtension();
            AtlasRegion region = atlas.find(name);

            if(region == null)
                throw new ArcRuntimeException("Could not find font region " + name + " in atlas " + parameter.atlasName);
            return new Font(file, region);
        }else{
            int n = data.getImagePaths().length;
            Ar<TextureRegion> regs = new Ar(n);
            for(int i = 0; i < n; i++){
                regs.add(new TextureRegion(manager.get(data.getImagePath(i), Texture.class)));
            }
            return new Font(data, regs, true);
        }
    }

    /**
     * Parameter to be passed to {@link AssetManager#load(String, Class, AssetLoaderParameters)} if additional configuration is
     * necessary for the {@link Font}.
     * <p>
     * 当 {@link Font} 需要额外配置时,传递给 {@link AssetManager#load(String, Class, AssetLoaderParameters)} 的参数。
     * @author mzechner
     */
    public static class FontParameter extends AssetLoaderParameters<Font>{
        /**
         * Flips the font vertically if {@code true}. Defaults to {@code false}.
         * 如果为 {@code true},则垂直翻转字体。默认为 {@code false}。
         */
        public boolean flip = false;

        /**
         * Generates mipmaps for the font if {@code true}. Defaults to {@code false}.
         * 如果为 {@code true},则为字体生成 mipmap。默认为 {@code false}。
         */
        public boolean genMipMaps = false;

        /**
         * The {@link TextureFilter} to use when scaling down the {@link Font}. Defaults to {@link TextureFilter#nearest}.
         * 缩小 {@link Font} 时使用的 {@link TextureFilter}。默认为 {@link TextureFilter#nearest}。
         */
        public TextureFilter minFilter = TextureFilter.nearest;

        /**
         * The {@link TextureFilter} to use when scaling up the {@link Font}. Defaults to {@link TextureFilter#nearest}.
         * 放大 {@link Font} 时使用的 {@link TextureFilter}。默认为 {@link TextureFilter#nearest}。
         */
        public TextureFilter magFilter = TextureFilter.nearest;

        /**
         * optional {@link FontData} to be used instead of loading the {@link Texture} directly.
         * 可选的 {@link FontData},用于代替直接加载 {@link Texture}。
         */
        public FontData fontData = null;

        /**
         * The name of the {@link TextureAtlas} to load the {@link Font} itself from. Optional; if {@code null}, will look for
         * a separate image
         * <p>
         * 要从中加载 {@link Font} 本身的 {@link TextureAtlas} 的名称。可选;如果为 {@code null},则查找单独的图片
         */
        public String atlasName = null;
    }
}
