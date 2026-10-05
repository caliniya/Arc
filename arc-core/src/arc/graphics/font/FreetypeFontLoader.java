package arc.graphics.font;

import arc.assets.*;
import arc.assets.loaders.*;
import arc.files.*;
import arc.graphics.font.FreeTypeFontGenerator.*;
import arc.struct.*;

/**
 * Creates {@link Font} instances from FreeType font files. Requires a {@link FreeTypeFontLoaderParameter} to be
 * passed to {@link AssetManager#load(String, Class, AssetLoaderParameters)} which specifies the name of the TTF
 * file as well the parameters used to generate the BitmapFont (size, characters, etc.)
 * <p>
 * 从 FreeType 字体文件创建 {@link Font} 实例。需要向 {@link AssetManager#load(String, Class, AssetLoaderParameters)} 传入 {@link FreeTypeFontLoaderParameter},其中指定 TTF 文件名以及生成 BitmapFont 所用的参数(尺寸、字符等)。
 */
public class FreetypeFontLoader extends AsynchronousAssetLoader<Font, FreetypeFontLoader.FreeTypeFontLoaderParameter>{
    public FreetypeFontLoader(FileHandleResolver resolver){
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, Fi file, FreeTypeFontLoaderParameter parameter){
        if(parameter == null)
            throw new RuntimeException("FreetypeFontParameter must be set in AssetManager#load to point at a TTF file!");
    }

    @Override
    public Font loadSync(AssetManager manager, String fileName, Fi file, FreeTypeFontLoaderParameter parameter){
        if(parameter == null)
            throw new RuntimeException("FreetypeFontParameter must be set in AssetManager#load to point at a TTF file!");
        FreeTypeFontGenerator generator = manager.get(parameter.fontFileName + ".gen", FreeTypeFontGenerator.class);
        return generator.generateFont(parameter.fontParameters);
    }

    @Override
    public Ar<AssetDescriptor> getDependencies(String fileName, Fi file, FreeTypeFontLoaderParameter parameter){
        Ar<AssetDescriptor> deps = new Ar<>();
        deps.add(new AssetDescriptor<>(parameter.fontFileName + ".gen", FreeTypeFontGenerator.class));
        return deps;
    }

    public static class FreeTypeFontLoaderParameter extends AssetLoaderParameters<Font>{
        /**
         * the name of the TTF file to be used to load the font
         * 用于加载字体的 TTF 文件名
         */
        public String fontFileName;
        /**
         * the parameters used to generate the font, e.g. size, characters, etc.
         * 生成字体所用的参数,如尺寸、字符等
         */
        public FreeTypeFontParameter fontParameters = new FreeTypeFontParameter();

        public FreeTypeFontLoaderParameter(){
        }

        public FreeTypeFontLoaderParameter(String fontFileName, FreeTypeFontParameter fontParameters){
            this.fontFileName = fontFileName;
            this.fontParameters = fontParameters;
        }
    }
}
