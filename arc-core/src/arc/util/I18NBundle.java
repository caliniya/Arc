package arc.util;

import arc.struct.*;
import arc.files.*;
import arc.util.io.*;

import java.io.*;
import java.util.*;

/**
 * A {@code I18NBundle} provides {@code Locale}-specific resources loaded from property files. A bundle contains a number of named
 * resources, whose names and values are {@code Strings}. A bundle may have a parent bundle, and when a resource is not found in a
 * bundle, the parent bundle is searched for the resource. If the fallback mechanism reaches the base bundle and still can't find
 * the resource it throws a {@code MissingResourceException}.
 *
 * <ul>
 * <li>All bundles for the same group of resources share a common base bundle. This base bundle acts as the root and is the last
 * fallback in case none of its children was able to respond to a request.</li>
 * <li>The first level contains changes between different languages. Only the differences between a language and the language of
 * the base bundle need to be handled by a language-specific {@code I18NBundle}.</li>
 * <li>The second level contains changes between different countries that use the same language. Only the differences between a
 * country and the country of the language bundle need to be handled by a country-specific {@code I18NBundle}.</li>
 * <li>The third level contains changes that don't have a geographic reason (e.g. changes that where made at some point in time
 * like {@code PREEURO} where the currency of come countries changed. The country bundle would return the current currency (Euro)
 * and the {@code PREEURO} variant bundle would return the old currency (e.g. DM for Germany).</li>
 * </ul>
 *
 * <strong>Examples</strong>
 * <ul>
 * <li>BaseName (base bundle)
 * <li>BaseName_de (german language bundle)
 * <li>BaseName_fr (french language bundle)
 * <li>BaseName_de_DE (bundle with Germany specific resources in german)
 * <li>BaseName_de_CH (bundle with Switzerland specific resources in german)
 * <li>BaseName_fr_CH (bundle with Switzerland specific resources in french)
 * <li>BaseName_de_DE_PREEURO (bundle with Germany specific resources in german of the time before the Euro)
 * <li>BaseName_fr_FR_PREEURO (bundle with France specific resources in french of the time before the Euro)
 * </ul>
 * <p>
 * It's also possible to create variants for languages or countries. This can be done by just skipping the country or language
 * abbreviation: BaseName_us__POSIX or BaseName__DE_PREEURO. But it's not allowed to circumvent both language and country:
 * BaseName___VARIANT is illegal.
 * <p>
 * 一个 {@code I18NBundle} 提供从属性文件加载的、特定于 {@code Locale} 的资源。bundle 包含多个命名资源,其名称和值都是 {@code String}。bundle 可以有父 bundle,当在某个 bundle 中找不到资源时,会在其父 bundle 中查找该资源。若回退机制到达基础 bundle 后仍找不到资源,则抛出 {@code MissingResourceException}。
 *
 * <ul>
 * <li>同一组资源的所有 bundle 共享一个共同的基础 bundle。该基础 bundle 作为根,是所有子 bundle 都无法响应请求时的最后回退。</li>
 * <li>第一层包含不同语言之间的差异。某种语言与基础 bundle 的语言之间的差异,只需由特定于该语言的 {@code I18NBundle} 处理。</li>
 * <li>第二层包含使用同一语言的不同国家之间的差异。某个国家与该语言 bundle 的国家之间的差异,只需由特定于该国家的 {@code I18NBundle} 处理。</li>
 * <li>第三层包含没有地理原因的差异(例如在某个时间点做出的更改,如 {@code PREEURO}:某些国家的货币发生了变化。国家 bundle 会返回当前货币(欧元),而 {@code PREEURO} 变体 bundle 会返回旧货币(如德国的 DM))。</li>
 * </ul>
 *
 * <strong>示例</strong>
 * <ul>
 * <li>BaseName(基础 bundle)
 * <li>BaseName_de(德语语言 bundle)
 * <li>BaseName_fr(法语语言 bundle)
 * <li>BaseName_de_DE(用德语表示的德国特定资源 bundle)
 * <li>BaseName_de_CH(用德语表示的瑞士特定资源 bundle)
 * <li>BaseName_fr_CH(用法语表示的瑞士特定资源 bundle)
 * <li>BaseName_de_DE_PREEURO(用德语表示的欧元时代之前德国特定资源的 bundle)
 * <li>BaseName_fr_FR_PREEURO(用法语表示的欧元时代之前法国特定资源的 bundle)
 * </ul>
 * <p>
 * 也可以为语言或国家创建变体,只需省略国家或语言缩写即可:BaseName_us__POSIX 或 BaseName__DE_PREEURO。但不允许同时省略语言和国家:BaseName___VARIANT 是非法的。
 * @author davebaol
 * @see PropertiesUtils
 */
public class I18NBundle{
    private static final String DEFAULT_ENCODING = "UTF-8";
    // Locale.ROOT does not exist in Android API level 8
    // Android API level 8 中不存在 Locale.ROOT
    private static final Locale ROOT_LOCALE = new Locale("", "", "");
    private static boolean simpleFormatter = false;
    /**
     * The parent of this {@code I18NBundle} that is used if this bundle doesn't include the requested resource.
     * 此 {@code I18NBundle} 的父 bundle,当此 bundle 不包含所请求的资源时使用。
     */
    private I18NBundle parent;
    /**
     * The locale for this bundle.
     * 此 bundle 的 locale。
     */
    private Locale locale;
    /**
     * The properties for this bundle.
     * 此 bundle 的属性。
     */
    private ObjectMap<String, String> properties;
    /**
     * The formatter used for argument replacement.
     * 用于参数替换的格式化器。
     */
    private TextFormatter formatter;

    /**
     * Returns the flag indicating whether to use the simplified message pattern syntax (default is false).
     * <p>
     * 返回指示是否使用简化消息模式语法的标志(默认为 false)。
     */
    public static boolean getSimpleFormatter(){
        return simpleFormatter;
    }

    /**
     * Sets the flag indicating whether to use the simplified message pattern. The flag must be set before calling the factory
     * methods {@code createBundle}.
     * <p>
     * 设置指示是否使用简化消息模式的标志。该标志必须在调用工厂方法 {@code createBundle} 之前设置。
     */
    public static void setSimpleFormatter(boolean enabled){
        simpleFormatter = enabled;
    }

    /**
     * Returns an empty bundle with no keys.
     * 返回一个没有任何键的空 bundle。
     */
    public static I18NBundle createEmptyBundle(){
        I18NBundle bundle = new I18NBundle();
        bundle.locale = ROOT_LOCALE;
        bundle.formatter = new TextFormatter(Locale.ROOT, false);
        bundle.properties = new ObjectMap<>();
        return bundle;
    }

    /**
     * Creates a new bundle using the specified <code>baseFileHandle</code>, the default locale and the default encoding "UTF-8".
     * <p>
     * 使用指定的 <code>baseFileHandle</code>、默认 locale 和默认编码 "UTF-8" 创建新 bundle。
     * @param baseFileHandle the file handle to the base of the bundle bundle 基础的文件句柄
     * @return a bundle for the given base file handle and the default locale 给定基础文件句柄和默认 locale 对应的 bundle
     * @throws NullPointerException if <code>baseFileHandle</code> is <code>null</code> 若 <code>baseFileHandle</code> 为 <code>null</code>
     * @throws MissingResourceException if no bundle for the specified base file handle can be found 若找不到指定基础文件句柄对应的 bundle
     */
    public static I18NBundle createBundle(Fi baseFileHandle){
        return createBundleImpl(baseFileHandle, Locale.getDefault(), DEFAULT_ENCODING);
    }

    /**
     * Creates a new bundle using the specified <code>baseFileHandle</code> and <code>locale</code>; the default encoding "UTF-8"
     * is used.
     * <p>
     * 使用指定的 <code>baseFileHandle</code> 和 <code>locale</code> 创建新 bundle;使用默认编码 "UTF-8"。
     * @param baseFileHandle the file handle to the base of the bundle bundle 基础的文件句柄
     * @param locale the locale for which a bundle is desired 需要为其获取 bundle 的 locale
     * @return a bundle for the given base file handle and locale 给定基础文件句柄和 locale 对应的 bundle
     * @throws NullPointerException if <code>baseFileHandle</code> or <code>locale</code> is <code>null</code> 若 <code>baseFileHandle</code> 或 <code>locale</code> 为 <code>null</code>
     * @throws MissingResourceException if no bundle for the specified base file handle can be found 若找不到指定基础文件句柄对应的 bundle
     */
    public static I18NBundle createBundle(Fi baseFileHandle, Locale locale){
        return createBundleImpl(baseFileHandle, locale, DEFAULT_ENCODING);
    }

    /**
     * Creates a new bundle using the specified <code>baseFileHandle</code> and <code>encoding</code>; the default locale is used.
     * <p>
     * 使用指定的 <code>baseFileHandle</code> 和 <code>encoding</code> 创建新 bundle;使用默认 locale。
     * @param baseFileHandle the file handle to the base of the bundle bundle 基础的文件句柄
     * @param encoding the charter encoding 字符编码
     * @return a bundle for the given base file handle and locale 给定基础文件句柄和 locale 对应的 bundle
     * @throws NullPointerException if <code>baseFileHandle</code> or <code>encoding</code> is <code>null</code> 若 <code>baseFileHandle</code> 或 <code>encoding</code> 为 <code>null</code>
     * @throws MissingResourceException if no bundle for the specified base file handle can be found 若找不到指定基础文件句柄对应的 bundle
     */
    public static I18NBundle createBundle(Fi baseFileHandle, String encoding){
        return createBundleImpl(baseFileHandle, Locale.getDefault(), encoding);
    }

    /**
     * Creates a new bundle using the specified <code>baseFileHandle</code>, <code>locale</code> and <code>encoding</code>.
     * <p>
     * 使用指定的 <code>baseFileHandle</code>、<code>locale</code> 和 <code>encoding</code> 创建新 bundle。
     * @param baseFileHandle the file handle to the base of the bundle bundle 基础的文件句柄
     * @param locale the locale for which a bundle is desired 需要为其获取 bundle 的 locale
     * @param encoding the charter encoding 字符编码
     * @return a bundle for the given base file handle and locale 给定基础文件句柄和 locale 对应的 bundle
     * @throws NullPointerException if <code>baseFileHandle</code>, <code>locale</code> or <code>encoding</code> is
     * <code>null</code> 若 <code>baseFileHandle</code>、<code>locale</code> 或 <code>encoding</code> 为 <code>null</code>
     * @throws MissingResourceException if no bundle for the specified base file handle can be found 若找不到指定基础文件句柄对应的 bundle
     */
    public static I18NBundle createBundle(Fi baseFileHandle, Locale locale, String encoding){
        return createBundleImpl(baseFileHandle, locale, encoding);
    }

    private static I18NBundle createBundleImpl(Fi baseFileHandle, Locale locale, String encoding){
        if(baseFileHandle == null || locale == null || encoding == null) throw new NullPointerException();

        I18NBundle bundle;
        I18NBundle baseBundle = null;
        Locale targetLocale = locale;
        do{
            // Create the candidate locales
            // 创建候选 locale
            Ar<Locale> candidateLocales = getCandidateLocales(targetLocale);

            // Load the bundle and its parents recursively
            // 递归加载该 bundle 及其父 bundle
            bundle = loadBundleChain(baseFileHandle, encoding, candidateLocales, 0, baseBundle);

            // Check the loaded bundle (if any)
            // 检查已加载的 bundle(如有)
            if(bundle != null){
                Locale bundleLocale = bundle.locale;
                boolean isBaseBundle = bundleLocale.equals(ROOT_LOCALE);

                if(!isBaseBundle || bundleLocale.equals(locale)){
                    // Found the bundle for the requested locale
                    // 找到了所请求 locale 对应的 bundle
                    break;
                }
                if(candidateLocales.size == 1 && bundleLocale.equals(candidateLocales.get(0))){
                    // Found the bundle for the only candidate locale
                    // 为唯一的候选 locale 找到了 bundle
                    break;
                }
                if(baseBundle == null){
                    // Store the base bundle and keep on processing the remaining fallback locales
                    // 保存基础 bundle,并继续处理剩余的回退 locale
                    baseBundle = bundle;
                }
            }

            // Set next fallback locale
            // 设置下一个回退 locale
            targetLocale = getFallbackLocale(targetLocale);

        }while(targetLocale != null);

        if(bundle == null){
            if(baseBundle == null){
                // No bundle found
                // 未找到 bundle
                throw new MissingResourceException("Can't find bundle for base file handle " + baseFileHandle.path() + ", locale "
                + locale, baseFileHandle + "_" + locale, "");
            }
            // Set the base bundle to be returned
            // 设置要返回的基础 bundle
            bundle = baseBundle;
        }

        return bundle;
    }

    /**
     * Returns a <code>List</code> of <code>Locale</code>s as candidate locales for the given <code>locale</code>. This method is
     * called by the <code>createBundle</code> factory method each time the factory method tries finding a resource bundle for a
     * target <code>Locale</code>.
     *
     * <p>
     * The sequence of the candidate locales also corresponds to the runtime resource lookup path (also known as the <I>parent
     * chain</I>), if the corresponding resource bundles for the candidate locales exist and their parents are not defined by
     * loaded resource bundles themselves. The last element of the list is always the {@linkplain Locale#ROOT root locale}, meaning
     * that the base bundle is the terminal of the parent chain.
     *
     * <p>
     * If the given locale is equal to <code>Locale.ROOT</code> (the root locale), a <code>List</code> containing only the root
     * <code>Locale</code> is returned. In this case, the <code>createBundle</code> factory method loads only the base bundle as
     * the resulting resource bundle.
     *
     * <p>
     * This implementation returns a <code>List</code> containing <code>Locale</code>s in the following sequence:
     *
     * <pre>
     *     Locale(language, country, variant)
     *     Locale(language, country)
     *     Locale(language)
     *     Locale.ROOT
     * </pre>
     * <p>
     * where <code>language</code>, <code>country</code> and <code>variant</code> are the language, country and variant values of
     * the given <code>locale</code>, respectively. Locales where the final component values are empty strings are omitted.
     *
     * <p>
     * For example, if the given base name is "Messages" and the given <code>locale</code> is
     * <code>Locale("ja",&nbsp;"",&nbsp;"XX")</code>, then a <code>List</code> of <code>Locale</code>s:
     *
     * <pre>
     *     Locale("ja", "", "XX")
     *     Locale("ja")
     *     Locale.ROOT
     * </pre>
     * <p>
     * is returned. And if the resource bundles for the "ja" and "" <code>Locale</code>s are found, then the runtime resource
     * lookup path (parent chain) is:
     *
     * <pre>
     *     Messages_ja -> Messages
     * </pre>
     * <p>
     * 返回一个 <code>List</code>,其中包含给定 <code>locale</code> 的候选 locale。<code>createBundle</code> 工厂方法每次尝试为目标 <code>Locale</code> 查找资源 bundle 时都会调用此方法。
     *
     * <p>
     * 候选 locale 的序列也对应于运行时资源查找路径(也称为<I>父链</I>),前提是候选 locale 对应的资源 bundle 存在,且其父级并非由已加载的资源 bundle 自身定义。列表的最后一个元素始终是 {@linkplain Locale#ROOT 根 locale},即基础 bundle 是父链的终点。
     *
     * <p>
     * 如果给定的 locale 等于 <code>Locale.ROOT</code>(根 locale),则返回仅包含根 <code>Locale</code> 的 <code>List</code>。此时 <code>createBundle</code> 工厂方法只加载基础 bundle 作为结果资源 bundle。
     *
     * <p>
     * 此实现返回按以下序列包含 <code>Locale</code> 的 <code>List</code>:
     *
     * <pre>
     *     Locale(language, country, variant)
     *     Locale(language, country)
     *     Locale(language)
     *     Locale.ROOT
     * </pre>
     * <p>
     * 其中 <code>language</code>、<code>country</code> 和 <code>variant</code> 分别是给定 <code>locale</code> 的语言、国家和变体值。末尾组成部分为空字符串的 locale 会被省略。
     *
     * <p>
     * 例如,若给定基础名称为 "Messages",给定 <code>locale</code> 为 <code>Locale("ja",&nbsp;"",&nbsp;"XX")</code>,则返回的 <code>Locale</code> 列表为:
     *
     * <pre>
     *     Locale("ja", "", "XX")
     *     Locale("ja")
     *     Locale.ROOT
     * </pre>
     * <p>
     * 若找到了 "ja" 和 "" <code>Locale</code> 对应的资源 bundle,则运行时资源查找路径(父链)为:
     *
     * <pre>
     *     Messages_ja -> Messages
     * </pre>
     * @param locale the locale for which a resource bundle is desired 需要为其获取资源 bundle 的 locale
     * @return a <code>List</code> of candidate <code>Locale</code>s for the given <code>locale</code> 给定 <code>locale</code> 的候选 <code>Locale</code> 列表
     * @throws NullPointerException if <code>locale</code> is <code>null</code> 若 <code>locale</code> 为 <code>null</code>
     */
    private static Ar<Locale> getCandidateLocales(Locale locale){
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();

        Ar<Locale> locales = new Ar<>(4);
        if(variant.length() > 0){
            locales.add(locale);
        }
        if(country.length() > 0){
            locales.add(locales.isEmpty() ? locale : new Locale(language, country));
        }
        if(language.length() > 0){
            locales.add(locales.isEmpty() ? locale : new Locale(language));
        }
        locales.add(ROOT_LOCALE);
        return locales;
    }

    /**
     * Returns a <code>Locale</code> to be used as a fallback locale for further bundle searches by the <code>createBundle</code>
     * factory method. This method is called from the factory method every time when no resulting bundle has been found for
     * <code>baseFileHandler</code> and <code>locale</code>, where locale is either the parameter for <code>createBundle</code> or
     * the previous fallback locale returned by this method.
     *
     * <p>
     * This method returns the {@linkplain Locale#getDefault() default <code>Locale</code>} if the given <code>locale</code> isn't
     * the default one. Otherwise, <code>null</code> is returned.
     * <p>
     * 返回一个 <code>Locale</code>,用作 <code>createBundle</code> 工厂方法继续查找 bundle 时的回退 locale。当工厂方法未能为 <code>baseFileHandler</code> 和 <code>locale</code> 找到结果 bundle 时会调用此方法,其中 locale 是 <code>createBundle</code> 的参数,或此方法上次返回的回退 locale。
     *
     * <p>
     * 若给定的 <code>locale</code> 不是默认 locale,此方法返回 {@linkplain Locale#getDefault() 默认 <code>Locale</code>};否则返回 <code>null</code>。
     * @param locale the <code>Locale</code> for which <code>createBundle</code> has been unable to find any resource bundles
     * (except for the base bundle) <code>createBundle</code> 未能为其找到任何资源 bundle(基础 bundle 除外)的 <code>Locale</code>
     * @return a <code>Locale</code> for the fallback search, or <code>null</code> if no further fallback search is needed. 用于回退查找的 <code>Locale</code>,若无需进一步回退查找则为 <code>null</code>。
     * @throws NullPointerException if <code>locale</code> is <code>null</code> 若 <code>locale</code> 为 <code>null</code>
     */
    private static Locale getFallbackLocale(Locale locale){
        Locale defaultLocale = Locale.getDefault();
        return locale.equals(defaultLocale) ? null : defaultLocale;
    }

    private static I18NBundle loadBundleChain(Fi baseFileHandle, String encoding, Ar<Locale> candidateLocales,
                                              int candidateIndex, I18NBundle baseBundle){
        Locale targetLocale = candidateLocales.get(candidateIndex);
        I18NBundle parent = null;
        if(candidateIndex != candidateLocales.size - 1){
            // Load recursively the parent having the next candidate locale
            // 递归加载具有下一个候选 locale 的父 bundle
            parent = loadBundleChain(baseFileHandle, encoding, candidateLocales, candidateIndex + 1, baseBundle);
        }else if(baseBundle != null && targetLocale.equals(ROOT_LOCALE)){
            return baseBundle;
        }

        // Load the bundle
        // 加载该 bundle
        I18NBundle bundle = loadBundle(baseFileHandle, encoding, targetLocale);
        if(bundle != null){
            bundle.parent = parent;
            return bundle;
        }

        return parent;
    }

    // Tries to load the bundle for the given locale.
    // 尝试加载给定 locale 对应的 bundle。
    private static I18NBundle loadBundle(Fi baseFileHandle, String encoding, Locale targetLocale){
        I18NBundle bundle = null;
        Reader reader = null;
        try{
            Fi fileHandle = toFileHandle(baseFileHandle, targetLocale);
            if(checkFileExistence(fileHandle)){
                // Instantiate the bundle
                // 实例化该 bundle
                bundle = new I18NBundle();

                // Load bundle properties from the stream with the specified encoding
                // 以指定编码从流中加载 bundle 属性
                reader = fileHandle.reader(encoding);
                bundle.load(reader);
            }
        }finally{
            Streams.close(reader);
        }
        if(bundle != null){
            bundle.setLocale(targetLocale);
        }

        return bundle;
    }

    //Fixes some problems with fh.exists(), see #2342 / #2345
    // 修复 fh.exists() 的一些问题,参见 #2342 / #2345
    private static boolean checkFileExistence(Fi fh){
        try{
            fh.read().close();
            return true;
        }catch(Exception e){
            return false;
        }
    }

    /**
     * Converts the given <code>baseFileHandle</code> and <code>locale</code> to the corresponding file handle.
     *
     * <p>
     * This implementation returns the <code>baseFileHandle</code>'s sibling with following value:
     *
     * <pre>
     * baseFileHandle.name() + &quot;_&quot; + language + &quot;_&quot; + country + &quot;_&quot; + variant + &quot;.properties&quot;
     * </pre>
     * <p>
     * where <code>language</code>, <code>country</code> and <code>variant</code> are the language, country and variant values of
     * <code>locale</code>, respectively. Final component values that are empty Strings are omitted along with the preceding '_'.
     * If all of the values are empty strings, then <code>baseFileHandle.name()</code> is returned with ".properties" appended.
     * <p>
     * 将给定的 <code>baseFileHandle</code> 和 <code>locale</code> 转换为对应的文件句柄。
     *
     * <p>
     * 此实现返回 <code>baseFileHandle</code> 的同级文件句柄,其值为:
     *
     * <pre>
     * baseFileHandle.name() + &quot;_&quot; + language + &quot;_&quot; + country + &quot;_&quot; + variant + &quot;.properties&quot;
     * </pre>
     * <p>
     * 其中 <code>language</code>、<code>country</code> 和 <code>variant</code> 分别是 <code>locale</code> 的语言、国家和变体值。为空字符串的末尾组成部分会连同其前面的 '_' 一起省略。若所有值都是空字符串,则返回 <code>baseFileHandle.name()</code> 并追加 ".properties"。
     * @param baseFileHandle the file handle to the base of the bundle bundle 基础的文件句柄
     * @param locale the locale for which a resource bundle should be loaded 需要为其加载资源 bundle 的 locale
     * @return the file handle for the bundle 该 bundle 的文件句柄
     * @throws NullPointerException if <code>baseFileHandle</code> or <code>locale</code> is <code>null</code> 若 <code>baseFileHandle</code> 或 <code>locale</code> 为 <code>null</code>
     */
    private static Fi toFileHandle(Fi baseFileHandle, Locale locale){
        StringBuilder sb = new StringBuilder(baseFileHandle.name());
        if(!locale.equals(ROOT_LOCALE)){
            String language = locale.getLanguage().replace("in", "id");
            String country = locale.getCountry();
            String variant = locale.getVariant();
            boolean emptyLanguage = "".equals(language);
            boolean emptyCountry = "".equals(country);
            boolean emptyVariant = "".equals(variant);

            if(!(emptyLanguage && emptyCountry && emptyVariant)){
                sb.append('_');
                if(!emptyVariant){
                    sb.append(language).append('_').append(country).append('_').append(variant);
                }else if(!emptyCountry){
                    sb.append(language).append('_').append(country);
                }else{
                    sb.append(language);
                }
            }
        }
        return baseFileHandle.sibling(sb.append(".properties").toString());
    }

    /**
     * Load the properties from the specified reader.
     * <p>
     * 从指定的 reader 加载属性。
     * @param reader the reader 读取器
     */
    private void load(Reader reader){
        properties = new ObjectMap<>();
        PropertiesUtils.load(properties, reader);
    }

    /**
     * Returns the locale of this bundle. This method can be used after a call to <code>createBundle()</code> to determine whether
     * the resource bundle returned really corresponds to the requested locale or is a fallback.
     * <p>
     * 返回此 bundle 的 locale。可在调用 <code>createBundle()</code> 之后使用此方法,以确定返回的资源 bundle 是真正对应所请求的 locale,还是回退结果。
     * @return the locale of this bundle 此 bundle 的 locale
     */
    public Locale getLocale(){
        return locale;
    }

    /**
     * Sets the bundle locale. This method is private because a bundle can't change the locale during its life.
     * <p>
     * 设置 bundle 的 locale。此方法是私有的,因为 bundle 在其生命周期内不能更改 locale。
     */
    private void setLocale(Locale locale){
        this.locale = locale;
        this.formatter = new TextFormatter(locale, !simpleFormatter);
    }

    /**
     * Gets a string for the given key from this bundle or one of its parents.
     * <p>
     * 从此 bundle 或其某个父 bundle 中获取给定键对应的字符串。
     * @param key the key for the desired string 所需字符串的键
     * @return the string for the given key or the key surrounded by {@code ???} if it cannot be found 给定键对应的字符串;若找不到,则返回被 {@code ???} 包围的键
     * @throws NullPointerException if <code>key</code> is <code>null</code> 若 <code>key</code> 为 <code>null</code>
     */
    public final String get(String key){
        String result = properties.get(key);
        if(result == null){
            if(parent != null) result = parent.get(key);
            if(result == null){
                return "???" + key + "???";
            }
        }
        return result;
    }

    /**
     * Returns the string for this given key, or def.
     * 返回给定键对应的字符串,若无则返回 def。
     */
    public String get(String key, String def){
        return has(key) ? get(key) : def;
    }

    public String getOrNull(String key){
        return has(key) ? get(key) : null;
    }

    public String getNotNull(String key){
        String s = getOrNull(key);
        if(s == null){
            throw new MissingResourceException("No key with name \"" + key + "\" found!", this.getClass().getName(), key);
        }
        return s;
    }

    /**
     * Returns all keys in this bundle. Does not check parent bundles.
     * 返回此 bundle 中的所有键。不检查父 bundle。
     */
    public Iterable<String> getKeys(){
        return properties.keys();
    }

    /** @return the internal property map. Can be modified. 内部属性 Map,可以被修改。 */
    public ObjectMap<String, String> getProperties(){
        return properties;
    }

    public void setProperties(ObjectMap<String, String> properties){
        this.properties = properties;
    }

    /**
     * Checks whether a specified key is present in this bundle.
     * 检查此 bundle 中是否存在指定的键。
     */
    public boolean has(String key){
        if(properties.containsKey(key)){
            return true;
        }

        if(parent != null){
            return parent.has(key);
        }
        return false;
    }

    /**
     * Gets the string with the specified key from this bundle or one of its parent after replacing the given arguments if they
     * occur.
     * <p>
     * 从此 bundle 或其某个父 bundle 中获取指定键对应的字符串,并在出现给定参数时进行替换。
     * @param key the key for the desired string 所需字符串的键
     * @param args the arguments to be replaced in the string associated to the given key. 要在给定键关联的字符串中替换的参数。
     * @return the string for the given key formatted with the given arguments 给定键对应的字符串,并使用给定参数完成格式化
     * @throws NullPointerException if <code>key</code> is <code>null</code> 若 <code>key</code> 为 <code>null</code>
     * @throws MissingResourceException if no string for the given key can be found 若找不到给定键对应的字符串
     */
    public String format(String key, Object... args){
        return formatter.format(get(key), args);
    }

    public String formatString(String string, Object... args){
        return formatter.format(string, args);
    }

    /**
     * Format, but with a number with fixed decimal places.
     * 格式化,但数字使用固定的小数位数。
     */
    public String formatFloat(String key, float value, int places){
        return formatter.format(get(key), Strings.fixed(value, places));
    }

    /**
     * Sets the value of all localized strings to String placeholder so hardcoded, unlocalized values can be easily spotted.
     * The I18NBundle won't be able to reset values after calling debug and should only be using during testing.
     * <p>
     * 将所有本地化字符串的值设置为占位字符串,以便轻松发现硬编码的、未本地化的值。调用 debug 后 I18NBundle 将无法重置这些值,因此只应在测试期间使用。
     */
    public void debug(String placeholder){
        ObjectMap.Keys<String> keys = properties.keys();
        if(keys == null) return;

        for(String s : keys){
            properties.put(s, placeholder);
        }
    }

    /** @return the parent bundle. 父 bundle。 */
    public I18NBundle getParent(){
        return parent;
    }
}
