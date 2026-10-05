package arc.scene.flabel;

import arc.func.*;
import arc.graphics.*;
import arc.scene.flabel.effects.*;
import arc.struct.*;

/**
 * Configuration class that easily allows the user to fine tune the library's functionality.
 * 配置类,便于用户对本库的功能进行微调。
 */
public class FConfig{

    /**
     * Whether or not <a href="https://github.com/libgdx/libgdx/wiki/Color-Markup-Language">LibGDX's Color Markup
     * Language</a> should be enabled when parsing a {@link FLabel}. Note that this library doesn't truly handle
     * colors, but simply convert them to the markup format. If markup is disabled, color tokens will be ignored.
     * <p>
     * 解析 {@link FLabel} 时是否启用 <a href="https://github.com/libgdx/libgdx/wiki/Color-Markup-Language">LibGDX's Color Markup Language</a>(LibGDX 颜色标记语言)。注意,本库并不真正处理颜色,只是将其转换为标记格式。如果禁用了标记,颜色令牌将被忽略。
     */
    public static boolean forceColorMarkupByDefault = true;

    /**
     * Default time in seconds that an empty {@code WAIT} token should wait for. Default value is {@code 0.250}.
     * 空的 {@code WAIT} 令牌默认等待的时间(秒)。默认值为 {@code 0.250}。
     */
    public static float defaultWaitValue = 0.250f;

    /**
     * Time in seconds that takes for each char to appear in the default speed. Default value is {@code 0.035}.
     * 默认速度下每个字符出现所需的时间(秒)。默认值为 {@code 0.035}。
     */
    public static float defaultSpeedPerChar = 0.035f;

    /**
     * Defines how many chars can appear per frame. Use a value less than {@code 1} to disable this limit. Default value
     * is {@code -1}.
     * <p>
     * 定义每帧最多可以出现多少个字符。使用小于 {@code 1} 的值可禁用此限制。默认值为 {@code -1}。
     */
    public static int charLimitPerFrame = -1;

    /**
     * Default color for the {@code CLEARCOLOR} token. Can be overriden by {@link FLabel#getClearColor()}.
     * {@code CLEARCOLOR} 令牌的默认颜色。可被 {@link FLabel#getClearColor()} 覆盖。
     */
    public static Color defaultClearColor = new Color(Color.white);

    /**
     * Returns a map of characters and their respective interval multipliers, of which the interval to the next char
     * should be multiplied for.
     * <p>
     * 返回一个字符与其对应间隔倍数的映射,到下一个字符的间隔应乘以该倍数。
     */
    public static ObjectFloatMap<Character> intervalMultipliersByChar = new ObjectFloatMap<>();

    /**
     * Map of global variables that affect all {@link FLabel} instances at once.
     * 全局变量映射,会同时影响所有 {@link FLabel} 实例。
     */
    public static final ObjectMap<String, String> globalVars = new ObjectMap<>();

    /**
     * Map of start tokens and their effect classes. Internal use only.
     * 起始令牌与其效果类的映射。仅供内部使用。
     */
    static final ObjectMap<String, Prov<FEffect>> effects = new ObjectMap<>();

    /**
     * Whether or not effect tokens are dirty and need to be recalculated.
     * 效果令牌是否已失效而需要重新计算。
     */
    static boolean dirtyEffectMaps = true;

    /**
     * Registers a new effect to FLabel.
     * <p>
     * 向 FLabel 注册一个新效果。
     *
     * @param tokenName Name of the token that starts the effect, such as WAVE. 启动该效果的令牌名称,例如 WAVE。
     */
    public static void registerEffect(String tokenName, Prov<FEffect> effect){
        effects.put(tokenName, effect);
        dirtyEffectMaps = true;
    }

    /**
     * Unregisters an effect from FLabel.
     * <p>
     * 从 FLabel 注销一个效果。
     *
     * @param tokenName Name of the token that starts the effect, such as WAVE. 启动该效果的令牌名称,例如 WAVE。
     */
    public static void unregisterEffect(String tokenName){
        effects.remove(tokenName);
        dirtyEffectMaps = true;
    }

    static{
        // Generate default char intervals
        // 生成默认的字符间隔
        intervalMultipliersByChar.put(' ', 0.0f);
        intervalMultipliersByChar.put(':', 1.5f);
        intervalMultipliersByChar.put(',', 2.5f);
        intervalMultipliersByChar.put('.', 2.5f);
        intervalMultipliersByChar.put('!', 5.0f);
        intervalMultipliersByChar.put('?', 5.0f);
        intervalMultipliersByChar.put('\n', 20f);

        // Register default tokens
        // 注册默认令牌
        registerEffect("ease", EaseEffect::new);
        registerEffect("jump", JumpEffect::new);
        registerEffect("shake", ShakeEffect::new);
        registerEffect("sick", SickEffect::new);
        registerEffect("wave", WaveEffect::new);
        registerEffect("wind", WindEffect::new);
        registerEffect("rainbow", RainbowEffect::new);
        registerEffect("gradient", GradientEffect::new);
        registerEffect("fade", FadeEffect::new);
        registerEffect("blink", BlinkEffect::new);
    }

}
