package arc.assets.loaders;

import arc.assets.AssetDescriptor;
import arc.assets.AssetLoaderParameters;
import arc.assets.AssetManager;
import arc.struct.Ar;
import arc.files.Fi;
import arc.util.I18NBundle;

import java.util.Locale;

/**
 * {@link AssetLoader} for {@link I18NBundle} instances. The I18NBundle is loaded asynchronously.
 * <p>
 * Notice that you can't load two bundles with the same base name and different locale or encoding using the same {@link AssetManager}.
 * For example, if you try to load the 2 bundles below
 *
 * <pre>
 * manager.load(&quot;i18n/message&quot;, I18NBundle.class, new I18NBundleParameter(Locale.ITALIAN));
 * manager.load(&quot;i18n/message&quot;, I18NBundle.class, new I18NBundleParameter(Locale.ENGLISH));
 * </pre>
 * <p>
 * the English bundle won't be loaded because the asset manager thinks they are the same bundle since they have the same name.
 * There are 2 use cases:
 * <ul>
 * <li>If you want to load the English bundle so to replace the Italian bundle you have to unload the Italian bundle first.
 * <li>If you want to load the English bundle without replacing the Italian bundle you should use another asset manager.
 * </ul>
 * <p>
 * 用于 {@link I18NBundle} 实例的 {@link AssetLoader}。I18NBundle 是异步加载的。
 * <p>
 * 注意:不能使用同一个 {@link AssetManager} 加载两个基名相同但区域设置或编码不同的 bundle。
 * 例如,如果尝试加载这样的两个 bundle,由于名称相同,资产管理器会认为它们是同一个 bundle,后加载的英文 bundle 将不会被加载。
 * 此时有两种处理方式:
 * <ul>
 * <li>如果想用英文 bundle 替换意大利语 bundle,必须先卸载意大利语 bundle。
 * <li>如果想加载英文 bundle 且不替换意大利语 bundle,则应使用另一个资产管理器。
 * </ul>
 * @author davebaol
 */
public class I18NBundleLoader extends AsynchronousAssetLoader<I18NBundle, I18NBundleLoader.I18NBundleParameter>{

    I18NBundle bundle;

    public I18NBundleLoader(FileHandleResolver resolver){
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, Fi file, I18NBundleParameter parameter){
        this.bundle = null;
        Locale locale;
        String encoding;
        if(parameter == null){
            locale = Locale.getDefault();
            encoding = null;
        }else{
            locale = parameter.locale == null ? Locale.getDefault() : parameter.locale;
            encoding = parameter.encoding;
        }
        if(encoding == null){
            this.bundle = I18NBundle.createBundle(file, locale);
        }else{
            this.bundle = I18NBundle.createBundle(file, locale, encoding);
        }
    }

    @Override
    public I18NBundle loadSync(AssetManager manager, String fileName, Fi file, I18NBundleParameter parameter){
        I18NBundle bundle = this.bundle;
        this.bundle = null;
        return bundle;
    }

    @Override
    public Ar<AssetDescriptor> getDependencies(String fileName, Fi file, I18NBundleParameter parameter){
        return null;
    }

    public static class I18NBundleParameter extends AssetLoaderParameters<I18NBundle>{
        public final Locale locale;
        public final String encoding;

        public I18NBundleParameter(){
            this(null, null);
        }

        public I18NBundleParameter(Locale locale){
            this(locale, null);
        }

        public I18NBundleParameter(Locale locale, String encoding){
            this.locale = locale;
            this.encoding = encoding;
        }
    }

}
