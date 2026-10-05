package arc.math;

import arc.math.geom.*;
import arc.util.*;

public final class Mathf{
    public static final int[] signs = {-1, 1};
    public static final int[] zeroOne = {0, 1};
    public static final boolean[] booleans = {true, false};
    public static final float FLOAT_ROUNDING_ERROR = 0.000001f; // 32 bits
    // 32 位
    public static final float PI = 3.1415927f, pi = PI, halfPi = PI/2;
    public static final float PI2 = PI * 2;
    public static final float E = 2.7182818f;
    public static final float sqrt2 = Mathf.sqrt(2f);
    public static final float sqrt3 = Mathf.sqrt(3f);
    /**
     * multiply by this to convert from radians to degrees
     * 乘以该值可将弧度转换为度
     */
    public static final float radiansToDegrees = 180f / PI;
    public static final float radDeg = radiansToDegrees;
    /**
     * multiply by this to convert from degrees to radians
     * 乘以该值可将度转换为弧度
     */
    public static final float degreesToRadians = PI / 180;
    public static final float degRad = degreesToRadians;
    public static final double doubleDegRad = 0.017453292519943295;
    public static final double doubleRadDeg = 57.29577951308232;

    private static final int sinBits = 14; // 16KB. Adjust for accuracy.
    // 16KB。可根据精度要求调整。
    private static final int sinMask = ~(-1 << sinBits);
    private static final int sinCount = sinMask + 1;
    private static final float[] sinTable = new float[sinCount];
    private static final float radFull = PI * 2;
    private static final float degFull = 360;
    private static final float radToIndex = sinCount / radFull;
    private static final float degToIndex = sinCount / degFull;
    private static final int BIG_ENOUGH_INT = 16 * 1024;
    private static final double BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT;
    private static final double CEIL = 0.9999999;
    private static final double BIG_ENOUGH_ROUND = BIG_ENOUGH_INT + 0.5f;
    private static final Rand seedr = new Rand();
    private static final Vec2 v1 = new Vec2(), v2 = new Vec2(), v3 = new Vec2();

    static{
        for(int i = 0; i < sinCount; i++)
            sinTable[i] = (float)Math.sin((i + 0.5f) / sinCount * radFull);
        for(int i = 0; i < 360; i += 90)
            sinTable[(int)(i * degToIndex) & sinMask] = (float)Math.sin(i * degreesToRadians);

        sinTable[0] = 0f;
        sinTable[(int)(90 * degToIndex) & sinMask] = 1f;
        sinTable[(int)(180 * degToIndex) & sinMask] = 0f;
        sinTable[(int)(270 * degToIndex) & sinMask] = -1f;
    }

    public static Rand rand = new Rand();

    /**
     * Returns the sine in radians from a lookup table.
     * 从查找表返回以弧度表示的正弦值。
     */
    public static float sin(float radians){
        return sinTable[(int)(radians * radToIndex) & sinMask];
    }

    /**
     * Returns the cosine in radians from a lookup table.
     * 从查找表返回以弧度表示的余弦值。
     */
    public static float cos(float radians){
        return sinTable[(int)((radians + PI / 2) * radToIndex) & sinMask];
    }

    /**
     * Returns the sine in radians from a lookup table.
     * 从查找表返回以弧度表示的正弦值。
     */
    public static float sinDeg(float degrees){
        return sinTable[(int)(degrees * degToIndex) & sinMask];
    }

    /**
     * Returns the cosine in radians from a lookup table.
     * 从查找表返回以弧度表示的余弦值。
     */
    public static float cosDeg(float degrees){
        return sinTable[(int)((degrees + 90) * degToIndex) & sinMask];
    }

    public static float absin(float in, float scl, float mag){
        return (sin(in, scl * 2f, mag) + mag) / 2f;
    }

    public static float tan(float radians, float scl, float mag){
        return (sin(radians / scl)) / (cos(radians / scl)) * mag;
    }

    public static float sin(float radians, float scl, float mag){
        return sin(radians / scl) * mag;
    }

    public static float cos(float radians, float scl, float mag){
        return cos(radians / scl) * mag;
    }

    public static float angle(float x, float y){
        float result = atan2(x, y) * radDeg;
        if(result < 0) result += 360f;
        return result;
    }

    public static float angleExact(float x, float y){
        float result = (float)Math.atan2(y, x) * radDeg;
        if(result < 0) result += 360f;
        return result;
    }

    /** Wraps the given angle to the range [-PI, PI]
     * <p>
     * 将给定角度换到 [-PI, PI] 范围
     * @param a the angle in radians 以弧度表示的角度
     * @return the given angle wrapped to the range [-PI, PI] 被换到 [-PI, PI] 范围内的给定角度 */
    public static float wrapAngleAroundZero(float a) {
        if(a >= 0) {
            float rotation = a % Mathf.PI2;
            if(rotation > Mathf.PI) rotation -= Mathf.PI2;
            return rotation;
        }else{
            float rotation = -a % Mathf.PI2;
            if(rotation > Mathf.PI) rotation -= Mathf.PI2;
            return -rotation;
        }
    }

    /** A variant on atan that does not tolerate infinite inputs for speed reasons, and because infinite inputs
     * should never occur where this is used (only in {@link #atan2(float, float)}).
     * <p>
     * atan 的一种变体,出于速度原因不接受无穷大输入,而且在使用它的地方(仅在 {@link #atan2(float, float)} 中)本就不应出现无穷大输入。
     * @param i any finite float 任意有限浮点数
     * @return an output from the inverse tangent function, from PI/-2.0 to PI/2.0 inclusive 反正切函数的输出,范围为 -PI/2.0 到 PI/2.0(含) */
    private static float atn(final double i){
        // We use double precision internally, because some constants need double precision.
        // 我们内部使用双精度,因为一些常数需要双精度。
        final double n = Math.abs(i);
        // c uses the "equally-good" formulation that permits n to be from 0 to almost infinity.
        // c 采用 “equally-good” 公式,允许 n 从 0 到接近无穷大。
        final double c = (n - 1.0) / (n + 1.0);
        // The approximation needs 6 odd powers of c.
        // 该近似需要 c 的 6 个奇数次幂。
        final double c2 = c * c, c3 = c * c2, c5 = c3 * c2, c7 = c5 * c2, c9 = c7 * c2, c11 = c9 * c2;
        return (float)Math.copySign((Math.PI * 0.25)
        + (0.99997726 * c - 0.33262347 * c3 + 0.19354346 * c5 - 0.11643287 * c7 + 0.05265332 * c9 - 0.0117212 * c11), i);
    }

    /** Close approximation of the frequently-used trigonometric method atan2, with higher precision than libGDX's atan2
     * approximation. Average error is 1.057E-6 radians; maximum error is 1.922E-6. Takes y and x (in that unusual order) as
     * floats, and returns the angle from the origin to that point in radians. It is about 4 times faster than
     * {@link Math#atan2(double, double)} (roughly 15 ns instead of roughly 60 ns for Math, on Java 8 HotSpot). <br>
     * Credit for this goes to the 1955 research study "Approximations for Digital Computers," by RAND Corporation. This is sheet
     * 11's algorithm, which is the fourth-fastest and fourth-least precise. The algorithms on sheets 8-10 are faster, but only by
     * a very small degree, and are considerably less precise. That study provides an atan method, and that cleanly
     * translates to atan2().
     * <p>
     * 常用三角函数 atan2 的快速近似,精度高于 libGDX 的 atan2 近似。平均误差为 1.057E-6 弧度;最大误差为 1.922E-6。以 y 和 x(顺序特殊)作为 float 参数,返回原点到该点角度的弧度值。速度约为 {@link Math#atan2(double, double)} 的 4 倍(在 Java 8 HotSpot 上约 15 纳秒,而 Math 约 60 纳秒)。 <br> 这一算法出自 1955 年 RAND 公司的研究报告 “Approximations for Digital Computers”。此处采用第 11 页的算法,是第四快、第四不精确的。第 8-10 页的算法更快,但差距很小,且精度明显更差。该研究提供了一个 atan 方法,可干净地转换为 atan2()。
     * @param y y-component of the point to find the angle towards; note the parameter order is unusual by convention 目标点的 y 分量;注意按惯例参数顺序不常见
     * @param x x-component of the point to find the angle towards; note the parameter order is unusual by convention 目标点的 x 分量;注意按惯例参数顺序不常见
     * @return the angle to the given point, in radians as a float; ranges from -PI to PI 到给定点以弧度表示的角度,以 float 返回;范围为 -PI 到 PI */
    public static float atan2(float x, final float y){
        float n = y / x;
        if(n != n){
            n = (y == x ? 1f : -1f); // if both y and x are infinite, n would be NaN
            // 若 y 和 x 都为无穷大,n 将是 NaN
        }else if(n - n != n - n){
            x = 0f; // if n is infinite, y is infinitely larger than x.
            // 如果 n 为无穷大,说明 y 比 x 大无穷多倍。
        }

        if(x > 0){
            return atn(n);
        }else if(x < 0){
            return y >= 0 ? atn(n) + PI : atn(n) - PI;
        }else if(y > 0){
            return x + halfPi;
        }else if(y < 0){
            return x - halfPi;
        }else{
            return x + y; // returns 0 for 0,0 or NaN if either y or x is NaN
            // 输入 (0,0) 时返回 0,若 y 或 x 为 NaN 则返回 NaN
        }
    }

    public static int digits(int n){
        return n < 100000 ? n < 100 ? n < 10 ? 1 : 2 : n < 1000 ? 3 : n < 10000 ? 4 : 5 : n < 10000000 ? n < 1000000 ? 6 : 7 : n < 100000000 ? 8 : n < 1000000000 ? 9 : 10;
    }

    public static int digits(long n){
        return n == 0 ? 1 : (int)(Math.log10(n)+1);
    }

    public static float sqrt(float x){
        return (float) Math.sqrt(x);
    }

    public static float sqr(float x){
        return x * x;
    }

    public static float map(float value, float froma, float toa, float fromb, float tob){
        return fromb + (value - froma) * (tob - fromb) / (toa - froma);
    }

    /**
     * Map value from [0, 1].
     * 从 [0, 1] 映射值。
     */
    public static float map(float value, float from, float to){
        return map(value, 0, 1, from, to);
    }

    /**
     * Returns -1 if f<0, 1 otherwise.
     * f<0 时返回 -1,否则返回 1。
     */
    public static int sign(float f){
        return (f < 0 ? -1 : 1);
    }

    /**
     * Returns 1 if true, -1 if false.
     * true 返回 1,false 返回 -1。
     */
    public static int sign(boolean b){
        return b ? 1 : -1;
    }

    /**
     * Converts a boolean to an integer: 1 if true, 0, if false.
     * 将布尔值转换为整数:true 为 1,false 为 0。
     */
    public static int num(boolean b){
        return b ? 1 : 0;
    }

    public static float pow(float a, float b){
        return (float)Math.pow(a, b);
    }

    public static int pow(int a, int b){
        return (int)Math.ceil(Math.pow(a, b));
    }

    public static float range(float range){
        return random(-range, range);
    }

    public static int range(int range){
        return random(-range, range);
    }

    public static float range(float min, float max){
        if(chance(0.5)){
            return random(min, max);
        }else{
            return -random(min, max);
        }
    }

    public static boolean chanceDelta(double d){
        return rand.nextFloat() < d * Time.delta;
    }

    public static boolean chance(double d){
        return d >= 1f || rand.nextFloat() < d;
    }

    /**
     * Returns a random number between 0 (inclusive) and the specified value (inclusive).
     * 返回 0(含)到指定值(含)之间的随机数。
     */
    public static int random(int range){
        return rand.nextInt(range + 1);
    }

    /**
     * Returns a random number between start (inclusive) and end (inclusive).
     * 返回 start(含)到 end(含)之间的随机数。
     */
    public static int random(int start, int end){
        return start + rand.nextInt(end - start + 1);
    }

    /**
     * Returns a random number between 0 (inclusive) and the specified value (inclusive).
     * 返回 0(含)到指定值(含)之间的随机数。
     */
    public static long random(long range){
        return (long)(rand.nextDouble() * range);
    }

    /**
     * Returns a random number between start (inclusive) and end (inclusive).
     * 返回 start(含)到 end(含)之间的随机数。
     */
    public static long random(long start, long end){
        return start + (long)(rand.nextDouble() * (end - start));
    }

    /**
     * Returns a random boolean value.
     * 返回随机的布尔值。
     */
    public static boolean randomBoolean(){
        return rand.nextBoolean();
    }

    /**
     * Returns true if a random value between 0 and 1 is less than the specified value.
     * 若 0 到 1 之间的随机值小于指定值则返回 true。
     */
    public static boolean randomBoolean(float chance){
        return Mathf.random() < chance;
    }

    /**
     * Returns random number between 0.0 (inclusive) and 1.0 (exclusive).
     * 返回 0.0(含)到 1.0(不含)之间的随机数。
     */
    public static float random(){
        return rand.nextFloat();
    }

    /**
     * Returns a random number between 0 (inclusive) and the specified value (exclusive).
     * 返回 0(含)到指定值(不含)之间的随机数。
     */
    public static float random(float range){
        return rand.nextFloat() * range;
    }

    /**
     * Returns a random number between start (inclusive) and end (exclusive).
     * 返回 start(含)到 end(不含)之间的随机数。
     */
    public static float random(float start, float end){
        return start + rand.nextFloat() * (end - start);
    }

    /**
     * Returns -1 or 1, randomly.
     * 随机返回 -1 或 1。
     */
    public static int randomSign(){
        return 1 | (rand.nextInt() >> 31);
    }

    //TODO these can be optimized to a single function, setting the seed and getting a result may be expensive
    // TODO 这些可以优化为单个函数;设置种子和获取结果的开销可能较大

    /**
     * Inclusive.
     * 包含边界。
     */
    public static int randomSeed(long seed, int min, int max){
        seedr.setSeed(seed);
        if(isPowerOfTwo(max)){
            seedr.nextInt();
        }
        return seedr.nextInt(max - min + 1) + min;
    }

    /**
     * Inclusive.
     * 包含边界。
     */
    public static float randomSeed(long seed, float min, float max){
        seedr.setSeed(seed);
        return (min + seedr.nextFloat() * (max - min));
    }

    public static float randomSeed(long seed){
        seedr.setSeed(seed * 99999);
        return seedr.nextFloat();
    }

    public static float randomSeed(long seed, float max){
        seedr.setSeed(seed * 99999);
        return seedr.nextFloat() * max;
    }

    public static float randomSeedRange(long seed, float range){
        seedr.setSeed(seed * 99999);
        return range * (seedr.nextFloat() - 0.5f) * 2f;
    }

    /**
     * Returns a triangularly distributed random number between -1.0 (exclusive) and 1.0 (exclusive), where values around zero are
     * more likely.
     * <p>
     * This is an optimized version of {@link #randomTriangular(float, float, float) randomTriangular(-1, 1, 0)}
     * <p>
     * 返回 -1.0(不含)到 1.0(不含)之间的三角分布随机数,0 附近的值更可能出现。 <p> 这是 {@link #randomTriangular(float, float, float) randomTriangular(-1, 1, 0)} 的优化版本
     */
    public static float randomTriangular(){
        return rand.nextFloat() - rand.nextFloat();
    }

    /**
     * Returns a triangularly distributed random number between {@code -max} (exclusive) and {@code max} (exclusive), where values
     * around zero are more likely.
     * <p>
     * This is an optimized version of {@link #randomTriangular(float, float, float) randomTriangular(-max, max, 0)}
     * <p>
     * 返回 {@code -max}(不含)到 {@code max}(不含)之间的三角分布随机数,0 附近的值更可能出现。 <p> 这是 {@link #randomTriangular(float, float, float) randomTriangular(-max, max, 0)} 的优化版本
     * @param max the upper limit 上限
     */
    public static float randomTriangular(float max){
        return (rand.nextFloat() - rand.nextFloat()) * max;
    }

    /**
     * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where the
     * {@code mode} argument defaults to the midpoint between the bounds, giving a symmetric distribution.
     * <p>
     * This method is equivalent of {@link #randomTriangular(float, float, float) randomTriangular(min, max, (min + max) * .5f)}
     * <p>
     * 返回 {@code min}(含)到 {@code max}(不含)之间的三角分布随机数,{@code mode} 参数默认为区间的中点,给出对称分布。 <p> 此方法等价于 {@link #randomTriangular(float, float, float) randomTriangular(min, max, (min + max) * .5f)}
     * @param min the lower limit 下限
     * @param max the upper limit 上限
     */
    public static float randomTriangular(float min, float max){
        return randomTriangular(min, max, (min + max) * 0.5f);
    }

    /**
     * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where values
     * around {@code mode} are more likely.
     * <p>
     * 返回 {@code min}(含)到 {@code max}(不含)之间的三角分布随机数,{@code mode} 附近的值更可能出现。
     * @param min the lower limit 下限
     * @param max the upper limit 上限
     * @param mode the point around which the values are more likely 值更可能集中在其附近的点
     */
    public static float randomTriangular(float min, float max, float mode){
        float u = rand.nextFloat();
        float d = max - min;
        if(u <= (mode - min) / d) return min + (float)Math.sqrt(u * d * (mode - min));
        return max - (float)Math.sqrt((1 - u) * d * (max - mode));
    }

    /**
     * Returns the next power of two. Returns the specified value if the value is already a power of two.
     * 返回下一个 2 的幂。若值已是 2 的幂则原样返回。
     */
    public static int nextPowerOfTwo(int value){
        if(value == 0) return 1;
        value--;
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;
        return value + 1;
    }

    public static boolean isPowerOfTwo(int value){
        return value != 0 && (value & value - 1) == 0;
    }

    public static int clamp(int value, int min, int max){
        return Math.max(Math.min(value, max), min);
    }

    public static long clamp(long value, long min, long max){
        return Math.max(Math.min(value, max), min);
    }

    public static float clamp(float value, float min, float max){
        return Math.max(Math.min(value, max), min);
    }

    /**
     * Clamps to [0, 1].
     * 限制到 [0, 1]。
     */
    public static float clamp(float value){
        return Math.max(Math.min(value, 1f), 0f);
    }

    public static double clamp(double value, double min, double max){
        return Math.max(Math.min(value, max), min);
    }

    public static float maxZero(float val){
        return Math.max(val, 0);
    }

    /**
     * Approaches a value at linear speed.
     * 以线性速度逼近某个值。
     */
    public static float approach(float from, float to, float speed){
        return from + Mathf.clamp(to - from, -speed, speed);
    }

    /**
     * Approaches a value at linear speed. Multiplied by the delta.
     * 以线性速度逼近某个值。乘以 delta。
     */
    public static float approachDelta(float from, float to, float speed){
        return approach(from, to, Time.delta * speed);
    }

    /**
     * Linearly interpolates between fromValue to toValue on progress position.
     * 在 progress 位置处从 fromValue 到 toValue 进行线性插值。
     */
    public static float lerp(float fromValue, float toValue, float progress){
        return fromValue + (toValue - fromValue) * progress;
    }

    /**
     * Linearly interpolates between fromValue to toValue on progress position. Multiplied by Time.delta().
     * 在 progress 位置处从 fromValue 到 toValue 进行线性插值。乘以 Time.delta()。
     */
    public static float lerpDelta(float fromValue, float toValue, float progress){
        return lerp(fromValue, toValue, clamp(progress * Time.delta));
    }

    /**
     * Linearly interpolates between two angles in radians. Takes into account that angles wrap at two pi and always takes the
     * direction with the smallest delta angle.
     * <p>
     * 在两个以弧度表示的角度之间进行线性插值。考虑角度在 2π 处回绕,并总是取角度差较小的方向。
     * @param fromRadians start angle in radians 起始角,单位为弧度
     * @param toRadians target angle in radians 目标角,单位为弧度
     * @param progress interpolation value in the range [0, 1] 范围 [0, 1] 内的插值
     * @return the interpolated angle in the range [0, PI2[ 范围 [0, PI2[ 内的插值角度
     */
    public static float slerpRad(float fromRadians, float toRadians, float progress){
        float delta = ((toRadians - fromRadians + PI2 + PI) % PI2) - PI;
        return (fromRadians + delta * progress + PI2) % PI2;
    }

    /**
     * Linearly interpolates between two angles in degrees. Takes into account that angles wrap at 360 degrees and always takes
     * the direction with the smallest delta angle.
     * <p>
     * 在两个以度表示的角度之间进行线性插值。考虑角度在 360 度处回绕,并总是取角度差较小的方向。
     * @param fromDegrees start angle in degrees 起始角,单位为度
     * @param toDegrees target angle in degrees 目标角,单位为度
     * @param progress interpolation value in the range [0, 1] 范围 [0, 1] 内的插值
     * @return the interpolated angle in the range [0, 360[ 范围 [0, 360[ 内的插值角度
     */
    public static float slerp(float fromDegrees, float toDegrees, float progress){
        float delta = ((toDegrees - fromDegrees + 360 + 180) % 360) - 180;
        return (fromDegrees + delta * progress + 360) % 360;
    }

    public static float slerpDelta(float fromDegrees, float toDegrees, float progress){
        return slerp(fromDegrees, toDegrees, clamp(progress * Time.delta));
    }

    /**
     * Returns the largest integer less than or equal to the specified float. This method will only properly floor floats from
     * -(2^14) to (Float.MAX_VALUE - 2^14).
     * <p>
     * 返回小于等于指定浮点数的最大整数。此方法只能正确处理 -(2^14) 到 (Float.MAX_VALUE - 2^14) 范围内的浮点数。
     */
    public static int floor(float value){
        return (int)(value + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT;
    }

    /**
     * Returns the largest integer less than or equal to the specified float. This method will only properly floor floats that are
     * positive. Note this method simply casts the float to int.
     * <p>
     * 返回小于等于指定浮点数的最大整数。此方法只能正确处理正的浮点数。注意此方法只是简单地将 float 强制转换为 int。
     */
    public static int floorPositive(float value){
        return (int)value;
    }

    /**
     * Returns the smallest integer greater than or equal to the specified float. This method will only properly ceil floats from
     * -(2^14) to (Float.MAX_VALUE - 2^14).
     * <p>
     * 返回大于等于指定浮点数的最小整数。此方法只能正确处理 -(2^14) 到 (Float.MAX_VALUE - 2^14) 范围内的浮点数。
     */
    public static int ceil(float value){
        return BIG_ENOUGH_INT - (int)(BIG_ENOUGH_FLOOR - value);
    }

    /**
     * Returns the smallest integer greater than or equal to the specified float. This method will only properly ceil floats that
     * are positive.
     * <p>
     * 返回大于等于指定浮点数的最小整数。此方法只能正确处理正的浮点数。
     */
    public static int ceilPositive(float value){
        return (int)(value + CEIL);
    }

    /**
     * Returns the closest integer to the specified float. This method will only properly round floats from -(2^14) to
     * (Float.MAX_VALUE - 2^14).
     * <p>
     * 返回最接近指定浮点数的整数。此方法只能正确舍入 -(2^14) 到 (Float.MAX_VALUE - 2^14) 范围内的浮点数。
     */
    public static int round(float value){
        return (int)(value + BIG_ENOUGH_ROUND) - BIG_ENOUGH_INT;
    }

    public static int round(int value, int step){
        return (value / step) * step;
    }

    public static float round(float value, float step){
        return (int)(value / step) * step;
    }

    public static int round(float value, int step){
        return (int)(value / step) * step;
    }

    /**
     * Returns the closest integer to the specified float. This method will only properly round floats that are positive.
     * 返回最接近指定浮点数的整数。此方法只能正确舍入正的浮点数。
     */
    public static int roundPositive(float value){
        return (int)(value + 0.5f);
    }

    /**
     * Returns true if the value is zero (using the default tolerance as upper bound)
     * 若值为零(使用默认容差作为上界)则返回 true
     */
    public static boolean zero(float value){
        return Math.abs(value) <= FLOAT_ROUNDING_ERROR;
    }

    /**
     * Returns true if the value is zero (using the default tolerance as upper bound)
     * 若值为零(使用默认容差作为上界)则返回 true
     */
    public static boolean zero(double value){
        return Math.abs(value) <= FLOAT_ROUNDING_ERROR;
    }

    /**
     * Returns true if the value is zero.
     * <p>
     * 若值为零则返回 true。
     * @param tolerance represent an upper bound below which the value is considered zero. 表示一个上界,值低于该值即视为零。
     */
    public static boolean zero(float value, float tolerance){
        return Math.abs(value) <= tolerance;
    }

    /**
     * Returns true if a is nearly equal to b. The function uses the default floating error tolerance.
     * <p>
     * 若 a 与 b 近似相等则返回 true。该函数使用默认的浮点误差容差。
     * @param a the first value. 第一个值。
     * @param b the second value. 第二个值。
     */
    public static boolean equal(float a, float b){
        return Math.abs(a - b) <= FLOAT_ROUNDING_ERROR;
    }

    /**
     * Returns true if a is nearly equal to b.
     * <p>
     * 若 a 与 b 近似相等则返回 true。
     * @param a the first value. 第一个值。
     * @param b the second value. 第二个值。
     * @param tolerance represent an upper bound below which the two values are considered equal. 表示一个上界,两值之差低于该值即视为相等。
     */
    public static boolean equal(float a, float b, float tolerance){
        return Math.abs(a - b) <= tolerance;
    }

    /**
     * @return the logarithm of value with base a
     * value 以 a 为底的对数
     */
    public static float log(float a, float value){
        return (float)(Math.log(value) / Math.log(a));
    }

    /**
     * @return the logarithm of value with base 2
     * value 以 2 为底的对数
     */
    public static float log2(float value){
        return log(2, value);
    }

    /**
     * @return base-2 logarithm of the specified integer
     * 指定整数的以 2 为底的对数
     */
    public static int log2(int value){
        return value == 0 ? 0 : 31 - Integer.numberOfLeadingZeros(value);
    }

    /**
     * Mod function that works properly for negative numbers.
     * 对负数也能正确工作的取模函数。
     */
    public static float mod(float f, float n){
        return ((f % n) + n) % n;
    }

    /**
     * Mod function that works properly for negative numbers.
     * 对负数也能正确工作的取模函数。
     */
    public static int mod(int x, int n){
        return ((x % n) + n) % n;
    }

    /**
     * @return a sampled value based on position in an array of float values.
     * 根据浮点数组中的位置采样得到的值。
     */
    public static float sample(float[] values, float time){
        time = Mathf.clamp(time);
        float pos = time * (values.length - 1);
        int cur = Math.min((int)(time * (values.length - 1)), values.length - 1);
        int next = Math.min(cur + 1, values.length - 1);
        float mod = (pos - cur);
        return lerp(values[cur], values[next], mod);
    }

    /**
     * @return the input 0-1 value scaled to 0-1-0.
     * 输入的 0-1 值被缩放为 0-1-0。
     */
    public static float slope(float fin){
        return 1f - Math.abs(fin - 0.5f) * 2f;
    }

    /**
     * Converts a 0-1 value to 0-1 when it is in [offset, 1].
     * 将 [offset, 1] 范围内的 0-1 值转换为 0-1。
     */
    public static float curve(float f, float offset){
        if(f < offset){
            return 0f;
        }else{
            return (f - offset) / (1f - offset);
        }
    }

    /**
     * Converts a 0-1 value to 0-1 when it is in [offset, to].
     * 将 [offset, to] 范围内的 0-1 值转换为 0-1。
     */
    public static float curve(float f, float from, float to){
        if(f < from){
            return 0f;
        }else if(f > to){
            return 1f;
        }else{
            return (f - from) / (to - from);
        }
    }

    /**
     * Transforms a 0-1 value to a value with a 0.5 plateau in the middle. When margin = 0.5, this method doesn't do anything.
     * 将 0-1 值变换为中间带 0.5 平台的值。当 margin = 0.5 时,此方法不做任何事。
     */
    public static float curveMargin(float f, float margin){
        return curveMargin(f, margin, margin);
    }

    /**
     * Transforms a 0-1 value to a value with a 0.5 plateau in the middle. When margin = 0.5, this method doesn't do anything.
     * 将 0-1 值变换为中间带 0.5 平台的值。当 margin = 0.5 时,此方法不做任何事。
     */
    public static float curveMargin(float f, float marginLeft, float marginRight){
        if(f < marginLeft) return f/marginLeft * 0.5f;
        if(f > 1f-marginRight) return (f - 1f + marginRight) / marginRight * 0.5f + 0.5f;
        return 0.5f;
    }

    public static float len(float x, float y){
        return (float)Math.sqrt(x * x + y * y);
    }

    public static float len2(float x, float y){
        return x * x + y * y;
    }

    public static float dot(float x1, float y1, float x2, float y2){
        return x1 * x2 + y1 * y2;
    }

    public static float dst(float x1, float y1){
        return (float)Math.sqrt(x1 * x1 + y1*y1);
    }

    public static float dst2(float x1, float y1){
        return (x1 * x1 + y1*y1);
    }

    public static float dst(float x1, float y1, float x2, float y2){
        final float xd = x2 - x1;
        final float yd = y2 - y1;
        return (float)Math.sqrt(xd * xd + yd * yd);
    }

    public static float dst2(float x1, float y1, float x2, float y2){
        final float xd = x2 - x1;
        final float yd = y2 - y1;
        return xd * xd + yd * yd;
    }

    /**
     * Manhattan distance.
     * 曼哈顿距离。
     */
    public static float dstm(float x1, float y1, float x2, float y2){
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    public static Vec2 arrive(Position pos, Position target, Vec2 curVel, float radius, float tolerance, float speed, float smoothTime){
        return arrive(pos.getX(), pos.getY(), target.getX(), target.getY(), curVel, radius, tolerance, speed, smoothTime);
    }

    //TODO kind of a mess
    // TODO 有点乱
    public static Vec2 arrive(float x, float y, float destX, float destY, Vec2 curVel, float radius, float tolerance, float speed, float accel){
        Vec2 toTarget = v1.set(destX, destY).sub(x, y);
        float distance = toTarget.len();

        if(distance <= tolerance) return v3.setZero();
        float targetSpeed = speed;
        if(distance <= radius) targetSpeed *= distance / radius;

        return toTarget.sub(curVel.x / accel, curVel.y / accel).limit(targetSpeed);
    }

    /**
     * @return whether dst(x1, y1, x2, y2) < dst
     * 是否 dst(x1, y1, x2, y2) < dst
     */
    public static boolean within(float x1, float y1, float x2, float y2, float dst){
        return dst2(x1, y1, x2, y2) < dst*dst;
    }

    /**
     * @return whether dst(x, y, 0, 0) < dst
     * 是否 dst(x, y, 0, 0) < dst
     */
    public static boolean within(float x1, float y1, float dst){
        return (x1 * x1 + y1 * y1) < dst*dst;
    }
}
