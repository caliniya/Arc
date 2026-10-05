package arc.graphics;

import arc.math.*;
import arc.math.geom.*;

/**
 * A color class, holding the r, g, b and alpha component as floats in the range [0,1]. All methods perform clamping on the
 * internal values after execution.
 * <p>
 * 颜色类,以 [0,1] 范围内的浮点数保存 r、g、b 和 alpha 分量。所有方法在执行后都会对内部值进行钳制。
 * @author mzechner
 */
public class Color{
    public static final Color white = new Color(1, 1, 1, 1);
    public static final Color lightGray = new Color(0xbfbfbfff);
    public static final Color gray = new Color(0x7f7f7fff);
    public static final Color darkGray = new Color(0x3f3f3fff);
    public static final Color black = new Color(0, 0, 0, 1);
    public static final Color clear = new Color(0, 0, 0, 0);

    /**
     * Convenience for frequently used <code>WHITE.toFloatBits()</code>
     * 便捷方法,等价于常用的 <code>WHITE.toFloatBits()</code>
     */
    public static final float whiteFloatBits = white.toFloatBits();
    public static final float clearFloatBits = clear.toFloatBits();
    public static final float blackFloatBits = black.toFloatBits();

    public static final int whiteRgba = white.rgba();
    public static final int clearRgba = clear.rgba();
    public static final int blackRgba = black.rgba();

    public static final Color blue = new Color(0, 0, 1, 1);
    public static final Color navy = new Color(0, 0, 0.5f, 1);
    public static final Color royal = new Color(0x4169e1ff);
    public static final Color slate = new Color(0x708090ff);
    public static final Color sky = new Color(0x87ceebff);
    public static final Color cyan = new Color(0, 1, 1, 1);
    public static final Color teal = new Color(0, 0.5f, 0.5f, 1);

    public static final Color green = new Color(0x00ff00ff);
    public static final Color acid = new Color(0x7fff00ff);
    public static final Color lime = new Color(0x32cd32ff);
    public static final Color forest = new Color(0x228b22ff);
    public static final Color olive = new Color(0x6b8e23ff);

    public static final Color yellow = new Color(0xffff00ff);
    public static final Color gold = new Color(0xffd700ff);
    public static final Color goldenrod = new Color(0xdaa520ff);
    public static final Color orange = new Color(0xffa500ff);

    public static final Color brown = new Color(0x8b4513ff);
    public static final Color tan = new Color(0xd2b48cff);
    public static final Color brick = new Color(0xb22222ff);

    public static final Color red = new Color(0xff0000ff);
    public static final Color scarlet = new Color(0xff341cff);
    public static final Color crimson = new Color(0xdc143cff);
    public static final Color coral = new Color(0xff7f50ff);
    public static final Color salmon = new Color(0xfa8072ff);
    public static final Color pink = new Color(0xff69b4ff);
    public static final Color magenta = new Color(1, 0, 1, 1);

    public static final Color purple = new Color(0xa020f0ff);
    public static final Color violet = new Color(0xee82eeff);
    public static final Color maroon = new Color(0xb03060ff);

    private static final float[] tmpHSV = new float[3];

    /**
     * the red, green, blue and alpha components *
     * 红、绿、蓝和 alpha 分量 *
     */
    public float r, g, b, a;

    /**
     * Constructs a new Color with all components set to 0.
     * 构造一个所有分量都为 0 的新 Color。
     */
    public Color(){
    }

    /** @see #rgba8888(int) */
    public Color(int rgba8888){
        rgba8888(rgba8888);
    }

    /**
     * Constructor, sets the components of the color
     * <p>
     * 构造函数,设置颜色的各分量
     * @param r the red component 红色分量
     * @param g the green component 绿色分量
     * @param b the blue component 蓝色分量
     * @param a the alpha component alpha 分量
     */
    public Color(float r, float g, float b, float a){
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        clamp();
    }

    /**
     * Constructor, sets the components of the color
     * <p>
     * 构造函数,设置颜色的各分量
     * @param r the red component 红色分量
     * @param g the green component 绿色分量
     * @param b the blue component 蓝色分量
     */
    public Color(float r, float g, float b){
        this(r, g, b, 1f);
    }

    /**
     * Constructs a new color using the given color
     * <p>
     * 使用给定颜色构造新颜色
     * @param color the color 颜色
     */
    public Color(Color color){
        set(color);
    }

    /**
     * Returns a new color from a hex string with the format RRGGBBAA.
     * <p>
     * 从 RRGGBBAA 格式的十六进制字符串创建新颜色。
     * @see #toString()
     */
    public static Color valueOf(String hex){
        return valueOf(new Color(), hex);
    }

    /**
     * Returns a new color from a hex string with the format RRGGBBAA.
     * <p>
     * 从 RRGGBBAA 格式的十六进制字符串创建新颜色。
     * @see #toString()
     */
    public static Color valueOf(Color color, String hex){
        int offset = hex.charAt(0) == '#' ? 1 : 0;

        int r = parseHex(hex, offset, offset + 2);
        int g = parseHex(hex, offset + 2, offset + 4);
        int b = parseHex(hex, offset + 4, offset + 6);
        int a = hex.length() - offset != 8 ? 255 : parseHex(hex, offset + 6, offset + 8);
        return color.set(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    private static int parseHex(String string, int from, int to){
        int total = 0;
        for(int i = from; i < to; i++){
            char c = string.charAt(i);
            total += Character.digit(c, 16) * (i == from ? 16 : 1);
        }
        return total;
    }

    /**
     * Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Note that no range
     * checking is performed for higher performance.
     * <p>
     * 将颜色分量以 ABGR 格式打包成 32 位整数,再转换为浮点数。注意,为提高性能不做范围检查。
     * @param r the red component, 0 - 255 红色分量,0 - 255
     * @param g the green component, 0 - 255 绿色分量,0 - 255
     * @param b the blue component, 0 - 255 蓝色分量,0 - 255
     * @param a the alpha component, 0 - 255 alpha 分量,0 - 255
     * @return the packed color as a float 打包为浮点数的颜色
     * @see Color#intToFloatColor(int)
     */
    public static float toFloatBits(int r, int g, int b, int a){
        int color = (a << 24) | (b << 16) | (g << 8) | r;
        return intToFloatColor(color);
    }

    /**
     * Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float.
     * <p>
     * 将颜色分量以 ABGR 格式打包成 32 位整数,再转换为浮点数。
     * @return the packed color as a 32-bit float 打包为 32 位浮点数的颜色
     * @see Color#intToFloatColor(int)
     */
    public static float toFloatBits(float r, float g, float b, float a){
        int color = ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
        return intToFloatColor(color);
    }

    public static double toDoubleBits(float r, float g, float b, float a){
        return Double.longBitsToDouble(Color.rgba8888(r, g, b, a) & 0x00000000_ffffffffL);
    }

    public static double toDoubleBits(int r, int g, int b, int a){
        return toDoubleBits(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    public Color fromDouble(double value){
        return rgba8888((int)(Double.doubleToRawLongBits(value)));
    }

    /**
     * Packs the color components into a 32-bit integer with the format ABGR. Note that no range checking is performed for higher
     * performance.
     * <p>
     * 将颜色分量以 ABGR 格式打包成 32 位整数。注意,为提高性能不做范围检查。
     * @param r the red component, 0 - 255 红色分量,0 - 255
     * @param g the green component, 0 - 255 绿色分量,0 - 255
     * @param b the blue component, 0 - 255 蓝色分量,0 - 255
     * @param a the alpha component, 0 - 255 alpha 分量,0 - 255
     * @return the packed color as a 32-bit int 打包为 32 位整数的颜色
     */
    public static int abgr(int r, int g, int b, int a){
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    public static int alpha(float alpha){
        return (int)(alpha * 255.0f);
    }

    public static int luminanceAlpha(float luminance, float alpha){
        return ((int)(luminance * 255.0f) << 8) | (int)(alpha * 255);
    }

    public static int rgb565(float r, float g, float b){
        return ((int)(r * 31) << 11) | ((int)(g * 63) << 5) | (int)(b * 31);
    }

    public static int rgba4444(float r, float g, float b, float a){
        return ((int)(r * 15) << 12) | ((int)(g * 15) << 8) | ((int)(b * 15) << 4) | (int)(a * 15);
    }

    public static int rgb888(float r, float g, float b){
        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    public static int rgba8888(float r, float g, float b, float a){
        return ((int)(r * 255) << 24) | ((int)(g * 255) << 16) | ((int)(b * 255) << 8) | (int)(a * 255);
    }

    public static int argb8888(float a, float r, float g, float b){
        return ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    /**
     * @return 4 0-255 RGBA components packed into an int. 打包为一个 int 的 4 个 0-255 RGBA 分量。
     */
    public static int packRgba(int r, int g, int b, int a){
        return (r << 24) | (g << 16) | (b << 8) | (a);
    }

    public int rgb565(){
        return ((int)(r * 31) << 11) | ((int)(g * 63) << 5) | (int)(b * 31);
    }

    public int rgba4444(){
        return ((int)(r * 15) << 12) | ((int)(g * 15) << 8) | ((int)(b * 15) << 4) | (int)(a * 15);
    }

    public int rgb888(){
        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    public int rgba8888(){
        return ((int)(r * 255) << 24) | ((int)(g * 255) << 16) | ((int)(b * 255) << 8) | (int)(a * 255);
    }

    public int argb8888(){
        return ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    /**
     * Sets the Color components using the specified integer value in the format RGB565. This is inverse to the rgb565(r, g, b)
     * method.
     * <p>
     * 使用 RGB565 格式的给定整数值设置 Color 分量。与 rgb565(r, g, b) 方法互为逆操作。
     * @param value An integer color value in RGB565 format. RGB565 格式的整型颜色值。
     */
    public Color rgb565(int value){
        r = ((value & 0x0000F800) >>> 11) / 31f;
        g = ((value & 0x000007E0) >>> 5) / 63f;
        b = ((value & 0x0000001F)) / 31f;
        return this;
    }

    /**
     * Sets the Color components using the specified integer value in the format RGBA4444. This is inverse to the rgba4444(r, g,
     * b, a) method.
     * <p>
     * 使用 RGBA4444 格式的给定整数值设置 Color 分量。与 rgba4444(r, g, b, a) 方法互为逆操作。
     * @param value An integer color value in RGBA4444 format. RGBA4444 格式的整型颜色值。
     */
    public Color rgba4444(int value){
        r = ((value & 0x0000f000) >>> 12) / 15f;
        g = ((value & 0x00000f00) >>> 8) / 15f;
        b = ((value & 0x000000f0) >>> 4) / 15f;
        a = ((value & 0x0000000f)) / 15f;
        return this;
    }

    /**
     * Sets the Color components using the specified integer value in the format RGB888. This is inverse to the rgb888(r, g, b)
     * method.
     * <p>
     * 使用 RGB888 格式的给定整数值设置 Color 分量。与 rgb888(r, g, b) 方法互为逆操作。
     * @param value An integer color value in RGB888 format. RGB888 格式的整型颜色值。
     */
    public Color rgb888(int value){
        r = ((value & 0x00ff0000) >>> 16) / 255f;
        g = ((value & 0x0000ff00) >>> 8) / 255f;
        b = ((value & 0x000000ff)) / 255f;
        return this;
    }

    /**
     * Sets the Color components using the specified integer value in the format RGBA8888. This is inverse to the rgba8888(r, g,
     * b, a) method.
     * <p>
     * 使用 RGBA8888 格式的给定整数值设置 Color 分量。与 rgba8888(r, g, b, a) 方法互为逆操作。
     * @param value An integer color value in RGBA8888 format. RGBA8888 格式的整型颜色值。
     */
    public Color rgba8888(int value){
        r = ((value & 0xff000000) >>> 24) / 255f;
        g = ((value & 0x00ff0000) >>> 16) / 255f;
        b = ((value & 0x0000ff00) >>> 8) / 255f;
        a = ((value & 0x000000ff)) / 255f;
        return this;
    }

    /**
     * Sets the Color components using the specified integer value in the format ARGB8888. This is the inverse to the argb8888(a,
     * r, g, b) method
     * <p>
     * 使用 ARGB8888 格式的给定整数值设置 Color 分量。与 argb8888(a, r, g, b) 方法互为逆操作
     * @param value An integer color value in ARGB8888 format. ARGB8888 格式的整型颜色值。
     */
    public Color argb8888(int value){
        a = ((value & 0xff000000) >>> 24) / 255f;
        r = ((value & 0x00ff0000) >>> 16) / 255f;
        g = ((value & 0x0000ff00) >>> 8) / 255f;
        b = ((value & 0x000000ff)) / 255f;
        return this;
    }

    /**
     * Sets the Color components using the specified float value in the format ABGB8888.
     * <p>
     * 使用 ABGB8888 格式的给定浮点值设置 Color 分量。
     */
    public Color abgr8888(float value){
        int c = floatToIntColor(value);
        a = ((c & 0xff000000) >>> 24) / 255f;
        b = ((c & 0x00ff0000) >>> 16) / 255f;
        g = ((c & 0x0000ff00) >>> 8) / 255f;
        r = ((c & 0x000000ff)) / 255f;
        return this;
    }

    /**
     * Creates a grayscale color.
     * 创建一个灰度颜色。
     */
    public static Color grays(float value){
        return new Color(value, value, value);
    }

    /**
     * Creates a color from 0-255 scaled RGB values.
     * 根据 0-255 缩放的 RGB 值创建颜色。
     */
    public static Color rgb(int r, int g, int b){
        return new Color(r / 255f, g / 255f, b / 255f);
    }

    /**
     * Converts the color from a float ABGR encoding to an int ABGR encoding. The alpha is expanded from 0-254 in the float
     * encoding (see {@link #intToFloatColor(int)}) to 0-255, which means converting from int to float and back to int can be
     * lossy.
     * <p>
     * 将颜色从浮点 ABGR 编码转换为整型 ABGR 编码。alpha 会从浮点编码中的 0-254(见 {@link #intToFloatColor(int)})扩展为 0-255,这意味着从 int 转到 float 再转回 int 可能是有损的。
     */
    public static int floatToIntColor(float value){
        int intBits = Float.floatToRawIntBits(value);
        intBits |= (int)((intBits >>> 24) * (255f / 254f)) << 24;
        return intBits;
    }

    /**
     * Encodes the ABGR int color as a float. The alpha is compressed to 0-254 to avoid using bits in the NaN range (see
     * {@link Float#intBitsToFloat(int)} javadocs). Rendering which uses colors encoded as floats should expand the 0-254 back to
     * 0-255.
     * <p>
     * 将 ABGR 整型颜色编码为浮点数。alpha 被压缩到 0-254,以避免使用 NaN 范围内的浮点位(见 {@link Float#intBitsToFloat(int)} 的 javadoc)。使用浮点颜色编码的渲染应将 0-254 重新扩展回 0-255。
     */
    public static float intToFloatColor(int value){
        return Float.intBitsToFloat(value & 0xfeffffff);
    }

    public Color rand(){
        return set(Mathf.random(), Mathf.random(), Mathf.random(), 1f);
    }

    public Color randHue(){
        fromHsv(Mathf.random(360f), 1f, 1f);
        a = 1f;
        return this;
    }

    /**
     * Returns the difference of all the HSV components combined.
     * 返回所有 HSV 分量的差值之和。
     */
    public float diff(Color other){
        return Math.abs(hue() - other.hue()) / 360f + Math.abs(value() - other.value()) + Math.abs(saturation() - other.saturation());
    }

    /**
     * Shorthand for {@link #rgba8888()}.
     * {@link #rgba8888()} 的简写。
     */
    public int rgba(){
        return rgba8888();
    }

    /**
     * Sets this color to the given color.
     * <p>
     * 将此颜色设置为给定颜色。
     * @param color the Color Color 对象
     */
    public Color set(Color color){
        this.r = color.r;
        this.g = color.g;
        this.b = color.b;
        this.a = color.a;
        return this;
    }

    public Color set(Vec3 vec){
        return set(vec.x, vec.y, vec.z);
    }

    /**
     * Multiplies the this color and the given color
     * <p>
     * 将此颜色与给定颜色相乘
     * @param color the color 颜色
     * @return this color. 此颜色。
     */
    public Color mul(Color color){
        this.r *= color.r;
        this.g *= color.g;
        this.b *= color.b;
        this.a *= color.a;
        return clamp();
    }

    /**
     * Multiplies RGB components of this Color with the given value.
     * <p>
     * 将此 Color 的 RGB 分量乘以给定值。
     * @param value the value 值
     * @return this color 此颜色
     */
    public Color mul(float value){
        this.r *= value;
        this.g *= value;
        this.b *= value;
        return clamp();
    }

    /**
     * Multiplies RGBA components of this Color with the given value.
     * <p>
     * 将此 Color 的 RGBA 分量乘以给定值。
     * @param value the value 值
     * @return this color 此颜色
     */
    public Color mula(float value){
        this.r *= value;
        this.g *= value;
        this.b *= value;
        this.a *= value;
        return clamp();
    }

    /**
     * Adds the given color to this color.
     * <p>
     * 将给定颜色加到此颜色上。
     * @param color the color 颜色
     * @return this color 此颜色
     */
    public Color add(Color color){
        this.r += color.r;
        this.g += color.g;
        this.b += color.b;
        return clamp();
    }

    /**
     * Subtracts the given color from this color
     * <p>
     * 从此颜色中减去给定颜色
     * @param color the color 颜色
     * @return this color 此颜色
     */
    public Color sub(Color color){
        this.r -= color.r;
        this.g -= color.g;
        this.b -= color.b;
        return clamp();
    }

    /**
     * Clamps this Color's components to a valid range [0 - 1]
     * <p>
     * 将此 Color 的分量钳制到有效范围 [0 - 1]
     * @return this Color for chaining 此 Color,便于链式调用
     */
    public Color clamp(){
        if(r < 0)
            r = 0;
        else if(r > 1) r = 1;

        if(g < 0)
            g = 0;
        else if(g > 1) g = 1;

        if(b < 0)
            b = 0;
        else if(b > 1) b = 1;

        if(a < 0)
            a = 0;
        else if(a > 1) a = 1;
        return this;
    }

    /**
     * Sets this Color's component values.
     * <p>
     * 设置此 Color 的分量值。
     * @param r Red component 红色分量
     * @param g Green component 绿色分量
     * @param b Blue component 蓝色分量
     * @param a Alpha component alpha 分量
     * @return this Color for chaining 此 Color,便于链式调用
     */
    public Color set(float r, float g, float b, float a){
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return clamp();
    }

    /**
     * Sets this Color's component values.
     * <p>
     * 设置此 Color 的分量值。
     * @param r Red component 红色分量
     * @param g Green component 绿色分量
     * @param b Blue component 蓝色分量
     * @return this Color for chaining 此 Color,便于链式调用
     */
    public Color set(float r, float g, float b){
        this.r = r;
        this.g = g;
        this.b = b;
        return clamp();
    }

    /**
     * Sets this color's component values through an integer representation.
     * <p>
     * 通过整型表示设置此颜色的分量值。
     * @return this Color for chaining 此 Color,便于链式调用
     */
    public Color set(int rgba){
        return rgba8888(rgba);
    }

    /**
     * Returns the sum of the RGB values of this color.
     * 返回此颜色 RGB 值之和。
     */
    public float sum(){
        return r + g + b;
    }

    /**
     * Adds the given color component values to this Color's values.
     * <p>
     * 将给定的颜色分量值加到此 Color 的值上。
     * @param r Red component 红色分量
     * @param g Green component 绿色分量
     * @param b Blue component 蓝色分量
     * @param a Alpha component alpha 分量
     * @return this Color for chaining 此 Color,便于链式调用
     */
    public Color add(float r, float g, float b, float a){
        this.r += r;
        this.g += g;
        this.b += b;
        this.a += a;
        return clamp();
    }

    /**
     * Adds the given color component values to this Color's values.
     * <p>
     * 将给定的颜色分量值加到此 Color 的值上。
     * @param r Red component 红色分量
     * @param g Green component 绿色分量
     * @param b Blue component 蓝色分量
     * @return this Color for chaining 此 Color,便于链式调用
     */
    public Color add(float r, float g, float b){
        this.r += r;
        this.g += g;
        this.b += b;
        return clamp();
    }

    /**
     * Subtracts the given values from this Color's component values.
     * <p>
     * 从此 Color 的分量值中减去给定值。
     * @param r Red component 红色分量
     * @param g Green component 绿色分量
     * @param b Blue component 蓝色分量
     * @param a Alpha component alpha 分量
     * @return this Color for chaining 此 Color,便于链式调用
     */
    public Color sub(float r, float g, float b, float a){
        this.r -= r;
        this.g -= g;
        this.b -= b;
        this.a -= a;
        return clamp();
    }

    /**
     * Subtracts the given values from this Color's component values.
     * <p>
     * 从此 Color 的分量值中减去给定值。
     * @param r Red component 红色分量
     * @param g Green component 绿色分量
     * @param b Blue component 蓝色分量
     * @return this Color for chaining 此 Color,便于链式调用
     */
    public Color sub(float r, float g, float b){
        this.r -= r;
        this.g -= g;
        this.b -= b;
        return clamp();
    }

    /**
     * Inverts this color's RGB.
     * 反转此颜色的 RGB。
     */
    public Color inv(){
        r = 1f - r;
        g = 1f - g;
        b = 1f - b;
        return this;
    }

    public Color r(float r){
        this.r = r;
        return this;
    }

    public Color g(float g){
        this.g = g;
        return this;
    }

    public Color b(float b){
        this.b = b;
        return this;
    }

    public Color a(float a){
        this.a = a;
        return this;
    }

    public Color mulA(float a){
        this.a *= a;
        return this;
    }

    /**
     * Multiplies this Color's color components by the given ones.
     * <p>
     * 将此 Color 的颜色分量乘以给定的分量值。
     * @param r Red component 红色分量
     * @param g Green component 绿色分量
     * @param b Blue component 蓝色分量
     * @param a Alpha component alpha 分量
     * @return this Color for chaining 此 Color,便于链式调用
     */
    public Color mul(float r, float g, float b, float a){
        this.r *= r;
        this.g *= g;
        this.b *= b;
        this.a *= a;
        return clamp();
    }

    /**
     * Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in
     * this color.
     * <p>
     * 在此颜色与目标颜色之间按 [0,1] 范围内的 t 进行线性插值。结果保存在此颜色中。
     * @param target The target color 目标颜色
     * @param t The interpolation coefficient 插值系数
     * @return This color for chaining. 此颜色,便于链式调用。
     */
    public Color lerp(final Color target, final float t){
        this.r += t * (target.r - this.r);
        this.g += t * (target.g - this.g);
        this.b += t * (target.b - this.b);
        this.a += t * (target.a - this.a);
        return clamp();
    }

    /**
     * Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in
     * this color.
     * <p>
     * 在此颜色与目标颜色之间按 [0,1] 范围内的 t 进行线性插值。结果保存在此颜色中。
     * @param r The red component of the target color 目标颜色的红色分量
     * @param g The green component of the target color 目标颜色的绿色分量
     * @param b The blue component of the target color 目标颜色的蓝色分量
     * @param a The alpha component of the target color 目标颜色的 alpha 分量
     * @param t The interpolation coefficient 插值系数
     * @return This color for chaining. 此颜色,便于链式调用。
     */
    public Color lerp(final float r, final float g, final float b, final float a, final float t){
        this.r += t * (r - this.r);
        this.g += t * (g - this.g);
        this.b += t * (b - this.b);
        this.a += t * (a - this.a);
        return clamp();
    }

    /**
     * Multiplies the RGB values by the alpha.
     * 将 RGB 值乘以 alpha。
     */
    public Color premultiplyAlpha(){
        r *= a;
        g *= a;
        b *= a;
        return this;
    }

    /**
     * @return Euclidean distance from the other color, based on RGB components. 基于 RGB 分量与其他颜色的欧氏距离。
     */
    public float dst(Color other){
        return Vec3.dst(r, g, b, other.r, other.g, other.b);
    }

    public Color write(Color to){
        return to.set(this);
    }

    public float hue(){
        toHsv(tmpHSV);
        return tmpHSV[0];
    }

    public float saturation(){
        toHsv(tmpHSV);
        return tmpHSV[1];
    }

    public float value(){
        toHsv(tmpHSV);
        return tmpHSV[2];
    }

    public Color hue(float amount){
        toHsv(tmpHSV);
        tmpHSV[0] = amount;
        fromHsv(tmpHSV);
        return this;
    }

    public Color saturation(float amount){
        toHsv(tmpHSV);
        tmpHSV[1] = amount;
        fromHsv(tmpHSV);
        return this;
    }

    public Color value(float amount){
        toHsv(tmpHSV);
        tmpHSV[2] = amount;
        fromHsv(tmpHSV);
        return this;
    }

    public Color shiftHue(float amount){
        toHsv(tmpHSV);
        tmpHSV[0] += amount;
        fromHsv(tmpHSV);
        return this;
    }

    public Color shiftSaturation(float amount){
        toHsv(tmpHSV);
        tmpHSV[1] += amount;
        fromHsv(tmpHSV);
        return this;
    }

    public Color shiftValue(float amount){
        toHsv(tmpHSV);
        tmpHSV[2] += amount;
        fromHsv(tmpHSV);
        return this;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Color color = (Color)o;
        return abgr() == color.abgr();
    }

    @Override
    public int hashCode(){
        int result = (r != +0.0f ? Float.floatToIntBits(r) : 0);
        result = 31 * result + (g != +0.0f ? Float.floatToIntBits(g) : 0);
        result = 31 * result + (b != +0.0f ? Float.floatToIntBits(b) : 0);
        result = 31 * result + (a != +0.0f ? Float.floatToIntBits(a) : 0);
        return result;
    }

    /**
     * Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Alpha is compressed
     * from 0-255 to 0-254 to avoid using float bits in the NaN range (see {@link Color#intToFloatColor(int)}).
     * <p>
     * 将颜色分量以 ABGR 格式打包成 32 位整数,再转换为浮点数。alpha 从 0-255 压缩到 0-254,以避免使用 NaN 范围内的浮点位(见 {@link Color#intToFloatColor(int)})。
     * @return the packed color as a 32-bit float 打包为 32 位浮点数的颜色
     */
    public float toFloatBits(){
        int color = ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
        return intToFloatColor(color);
    }

    public double toDoubleBits(){
        return toDoubleBits(r, g, b, a);
    }

    /**
     * Packs the color components into a 32-bit integer with the format ABGR.
     * <p>
     * 将颜色分量以 ABGR 格式打包成 32 位整数。
     * @return the packed color as a 32-bit int. 打包为 32 位整数的颜色。
     */
    public int abgr(){
        return ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
    }

    /**
     * Returns the color encoded as hex string with the format RRGGBBAA.
     * 返回以 RRGGBBAA 格式十六进制字符串编码的颜色。
     */
    public String toString(){
        StringBuilder value = new StringBuilder();
        toString(value);
        return value.toString();
    }

    public void toString(StringBuilder builder){
        builder.append(Integer.toHexString(((int)(255 * r) << 24) | ((int)(255 * g) << 16) | ((int)(255 * b) << 8) | ((int)(255 * a))));
        while(builder.length() < 8)
            builder.insert(0, "0");
    }

    /**
     * Sets the RGB Color components using the specified Hue-Saturation-Value. Note that HSV components are voluntary not clamped
     * to preserve high range color and can range beyond typical values.
     * <p>
     * 使用给定的色相-饱和度-明度设置 RGB 颜色分量。注意,HSV 分量特意不做钳制,以保留高动态范围的颜色,可以超出常规取值。
     * @param h The Hue in degree from 0 to 360 色相,0 到 360
     * @param s The Saturation from 0 to 1 饱和度,0 到 1
     * @param v The Value (brightness) from 0 to 1 明度(亮度),0 到 1
     * @return The modified Color for chaining. 修改后的 Color,便于链式调用。
     */
    public Color fromHsv(float h, float s, float v){
        float x = (h / 60f + 6) % 6;
        int i = (int)x;
        float f = x - i;
        float p = v * (1 - s);
        float q = v * (1 - s * f);
        float t = v * (1 - s * (1 - f));
        switch(i){
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            default:
                r = v;
                g = p;
                b = q;
        }

        return clamp();
    }

    /**
     * Sets RGB components using the specified Hue-Saturation-Value. This is a convenient method for
     * {@link #fromHsv(float, float, float)}. This is the inverse of {@link #toHsv(float[])}.
     * <p>
     * 使用给定的色相-饱和度-明度设置 RGB 分量。这是 {@link #fromHsv(float, float, float)} 的便捷方法,与 {@link #toHsv(float[])} 互为逆操作。
     * @param hsv The Hue, Saturation and Value components in that order. 色相、饱和度和明度分量,依此顺序。
     * @return The modified Color for chaining. 修改后的 Color,便于链式调用。
     */
    public Color fromHsv(float[] hsv){
        return fromHsv(hsv[0], hsv[1], hsv[2]);
    }

    /**
     * Extract Hue-Saturation-Value. This is the inverse of {@link #fromHsv(float[])}.
     * <p>
     * 提取色相-饱和度-明度。与 {@link #fromHsv(float[])} 互为逆操作。
     * @param hsv The HSV array to be modified. 待修改的 HSV 数组。
     * @return HSV components for chaining. HSV 分量,便于链式调用。
     */
    public float[] toHsv(float[] hsv){
        float max = Math.max(Math.max(r, g), b);
        float min = Math.min(Math.min(r, g), b);
        float range = max - min;
        if(range == 0){
            hsv[0] = 0;
        }else if(max == r){
            hsv[0] = (60 * (g - b) / range + 360) % 360;
        }else if(max == g){
            hsv[0] = 60 * (b - r) / range + 120;
        }else{
            hsv[0] = 60 * (r - g) / range + 240;
        }

        if(max > 0){
            hsv[1] = 1 - min / max;
        }else{
            hsv[1] = 0;
        }

        hsv[2] = max;

        return hsv;
    }

    /**
     * Converts HSV to RGB
     *
     * <p>
     * 将 HSV 转换为 RGB
     * @param h     hue 0-360 色相 0-360
     * @param s     saturation 0-100 饱和度 0-100
     * @param v     value 0-100 明度 0-100
     * @param alpha 0-1 0-1
     */
    public static Color HSVtoRGB(float h, float s, float v, float alpha) {
        Color c = HSVtoRGB(h, s, v);
        c.a = alpha;
        return c;
    }

    /**
     * Converts HSV color system to RGB
     *
     * <p>
     * 将 HSV 颜色系统转换为 RGB
     * @param h hue 0-360 色相 0-360
     * @param s saturation 0-100 饱和度 0-100
     * @param v value 0-100 明度 0-100
     */
    public static Color HSVtoRGB(float h, float s, float v) {
        Color c = new Color(1, 1, 1, 1);
        HSVtoRGB(h, s, v, c);
        return c;
    }

    /**
     * Converts HSV color system to RGB
     *
     * <p>
     * 将 HSV 颜色系统转换为 RGB
     * @param h           hue 0-360 色相 0-360
     * @param s           saturation 0-100 饱和度 0-100
     * @param v           value 0-100 明度 0-100
     * @param targetColor color that result will be stored in 存储结果的颜色对象
     * @return targetColor
     */
    public static Color HSVtoRGB(float h, float s, float v, Color targetColor) {
        if(h == 360) h = 359;
        float r, g, b;
        int i;
        float f, p, q, t;
        h = (float) Math.max(0.0, Math.min(360.0, h));
        s = (float) Math.max(0.0, Math.min(100.0, s));
        v = (float) Math.max(0.0, Math.min(100.0, v));
        s /= 100;
        v /= 100;
        h /= 60;
        i = Mathf.floor(h);
        f = h - i;
        p = v * (1 - s);
        q = v * (1 - s * f);
        t = v * (1 - s * (1 - f));
        switch(i) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            default:
                r = v;
                g = p;
                b = q;
        }

        targetColor.set(r, g, b, targetColor.a);
        return targetColor;
    }

    /**
     * Converts {@link Color} to HSV color system
     *
     * <p>
     * 将 {@link Color} 转换为 HSV 颜色系统
     * @return 3 element int array with hue (0-360), saturation (0-100) and value (0-100) 包含色相 (0-360)、饱和度 (0-100) 和明度 (0-100) 的 3 元素整型数组
     */
    public static int[] RGBtoHSV(Color c) {
        return RGBtoHSV(c.r, c.g, c.b);
    }

    /**
     * Converts RGB to HSV color system
     *
     * <p>
     * 将 RGB 转换为 HSV 颜色系统
     * @param r red 0-1 红 0-1
     * @param g green 0-1 绿 0-1
     * @param b blue 0-1 蓝 0-1
     * @return 3 element int array with hue (0-360), saturation (0-100) and value (0-100) 包含色相 (0-360)、饱和度 (0-100) 和明度 (0-100) 的 3 元素整型数组
     */
    public static int[] RGBtoHSV(float r, float g, float b) {
        float h, s, v;
        float min, max, delta;

        min = Math.min(Math.min(r, g), b);
        max = Math.max(Math.max(r, g), b);
        v = max;

        delta = max - min;

        if(max != 0)
            s = delta / max;
        else {
            s = 0;
            h = 0;
            return new int[]{Mathf.round(h), Mathf.round(s), Mathf.round(v)};
        }

        if(delta == 0)
            h = 0;
        else {

            if(r == max)
                h = (g - b) / delta;
            else if(g == max)
                h = 2 + (b - r) / delta;
            else
                h = 4 + (r - g) / delta;
        }

        h *= 60;
        if(h < 0)
            h += 360;

        s *= 100;
        v *= 100;

        return new int[]{Mathf.round(h), Mathf.round(s), Mathf.round(v)};
    }

    /**
     * @return a copy of this color 此颜色的副本
     */
    public Color cpy(){
        return new Color(this);
    }

    public Color lerp(Color[] colors, float s){
        int l = colors.length;
        Color a = colors[Mathf.clamp((int)(s * (l - 1)), 0, colors.length - 1)];
        Color b = colors[Mathf.clamp((int)(s * (l - 1) + 1), 0, l - 1)];

        float n = s * (l - 1) - (int)(s * (l - 1));
        float i = 1f - n;
        return set(a.r * i + b.r * n, a.g * i + b.g * n, a.b * i + b.b * n, 1f);
    }

    private static int clampf(float value){
        return Math.min(Math.max((int)value, 0), 255);
    }

    /**
     * @return R value of a RGBA packed color. RGBA 打包颜色的 R 值。
     */
    public static int ri(int rgba){
        return (rgba & 0xff000000) >>> 24;
    }

    /**
     * @return G value of a RGBA packed color. RGBA 打包颜色的 G 值。
     */
    public static int gi(int rgba){
        return (rgba & 0x00ff0000) >>> 16;
    }

    /**
     * @return B value of a RGBA packed color. RGBA 打包颜色的 B 值。
     */
    public static int bi(int rgba){
        return (rgba & 0x0000ff00) >>> 8;
    }

    /**
     * @return A value of a RGBA packed color. RGBA 打包颜色的 A 值。
     */
    public static int ai(int rgba){
        return (rgba & 0x000000ff);
    }

    /**
     * Multiplies 2 RGBA colors together.
     * 将两个 RGBA 颜色相乘。
     */
    public static int muli(int ca, int cb){
        int
        r = ((ca & 0xff000000) >>> 24),
        g = ((ca & 0x00ff0000) >>> 16),
        b = ((ca & 0x0000ff00) >>> 8),
        a = ((ca & 0x000000ff)),
        r2 = ((cb & 0xff000000) >>> 24),
        g2 = ((cb & 0x00ff0000) >>> 16),
        b2 = ((cb & 0x0000ff00) >>> 8),
        a2 = ((cb & 0x000000ff));
        return (clampf(r * r2 / 255f) << 24) | (clampf(g * g2 / 255f) << 16) | (clampf(b * b2 / 255f) << 8) | (clampf(a * a2 / 255f));
    }

    /**
     * Multiplies a RGBA color by a float. Alpha channels are not multiplied.
     * 将 RGBA 颜色乘以一个浮点数。alpha 通道不参与相乘。
     */
    public static int muli(int rgba, float value){
        int
        r = ((rgba & 0xff000000) >>> 24),
        g = ((rgba & 0x00ff0000) >>> 16),
        b = ((rgba & 0x0000ff00) >>> 8),
        a = ((rgba & 0x000000ff));
        return (clampf(r * value) << 24) | (clampf(g * value) << 16) | (clampf(b * value) << 8) | (a);
    }
}
