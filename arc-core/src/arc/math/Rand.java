package arc.math;

import java.util.Random;

/**
 * This class implements the xorshift128+ algorithm that is a very fast, top-quality 64-bit pseudo-random number generator. The
 * quality of this PRNG is much higher than {@link Random}'s, and its cycle length is 2<sup>128</sup>&nbsp;&minus;&nbsp;1, which
 * is more than enough for any single-thread application. More details and algorithms can be found <a
 * href="http://xorshift.di.unimi.it/">here</a>.
 * <p>
 * Instances of RandomXS128 are not thread-safe.
 * <p>
 * 此类实现 xorshift128+ 算法,这是一种非常快速、高质量的 64 位伪随机数生成器。该 PRNG 的质量远高于 {@link Random},周期长度为 2<sup>128</sup>&nbsp;&minus;&nbsp;1,对任何单线程应用都绰绰有余。更多细节和算法见 <a href="http://xorshift.di.unimi.it/">此处</a>。 <p> RandomXS128 的实例不是线程安全的。
 * @author Inferno
 * @author davebaol
 */
public class Rand extends Random{
    /**
     * Normalization constant for double.
     * double 的归一化常数。
     */
    private static final double NORM_DOUBLE = 1.0 / (1L << 53);
    /**
     * Normalization constant for float.
     * float 的归一化常数。
     */
    private static final double NORM_FLOAT = 1.0 / (1L << 24);

    /**
     * The first half of the internal state of this pseudo-random number generator.
     * 此伪随机数生成器内部状态的前半部分。
     */
    public long seed0;
    /**
     * The second half of the internal state of this pseudo-random number generator.
     * 此伪随机数生成器内部状态的后半部分。
     */
    public long seed1;

    /**
     * Creates a new random number generator. This constructor sets the seed of the random number generator to a value very likely
     * to be distinct from any other invocation of this constructor.
     * <p>
     * This implementation creates a {@link Random} instance to generate the initial seed.
     * <p>
     * 创建新的随机数生成器。此构造函数将随机数生成器的种子设为一个极不可能与其他构造调用相同的值。 <p> 此实现使用 {@link Random} 实例生成初始种子。
     */
    public Rand(){
        setSeed(new Random().nextLong());
    }

    /**
     * Creates a new random number generator using a single {@code long} seed.
     * <p>
     * 使用单个 {@code long} 种子创建新的随机数生成器。
     * @param seed the initial seed 初始种子
     */
    public Rand(long seed){
        setSeed(seed);
    }

    /**
     * Creates a new random number generator using two {@code long} seeds.
     * <p>
     * 使用两个 {@code long} 种子创建新的随机数生成器。
     * @param seed0 the first part of the initial seed 初始种子的第一部分
     * @param seed1 the second part of the initial seed 初始种子的第二部分
     */
    public Rand(long seed0, long seed1){
        setState(seed0, seed1);
    }

    private static long murmurHash3(long x){
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;

        return x;
    }

    /**
     * Returns the next pseudo-random, uniformly distributed {@code long} value from this random number generator's sequence.
     * <p>
     * Subclasses should override this, as this is used by all other methods.
     * <p>
     * 从此随机数生成器的序列返回下一个均匀分布的伪随机 {@code long} 值。 <p> 子类应重写此方法,因为所有其他方法都要用到它。
     */
    @Override
    public long nextLong(){
        long s1 = this.seed0;
        final long s0 = this.seed1;
        this.seed0 = s0;
        s1 ^= s1 << 23;
        return (this.seed1 = (s1 ^ s0 ^ (s1 >>> 17) ^ (s0 >>> 26))) + s0;
    }

    /**
     * This protected method is final because, contrary to the superclass, it's not used anymore by the other methods.
     * 此受保护方法为 final,因为与父类不同,其他方法已不再使用它。
     */
    @Override
    protected final int next(int bits){
        return (int)(nextLong() & ((1L << bits) - 1));
    }

    /**
     * Returns the next pseudo-random, uniformly distributed {@code int} value from this random number generator's sequence.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     * <p>
     * 从此随机数生成器的序列返回下一个均匀分布的伪随机 {@code int} 值。 <p> 此实现在内部使用 {@link #nextLong()}。
     */
    @Override
    public int nextInt(){
        return (int)nextLong();
    }

    /**
     * Returns a pseudo-random, uniformly distributed {@code int} value between 0 (inclusive) and the specified value (exclusive),
     * drawn from this random number generator's sequence.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     * <p>
     * 返回 0(含)到指定值(不含)之间均匀分布的伪随机 {@code int} 值,取自此随机数生成器的序列。 <p> 此实现在内部使用 {@link #nextLong()}。
     * @param n the positive bound on the random number to be returned. 返回的随机数的正上界。
     * @return the next pseudo-random {@code int} value between {@code 0} (inclusive) and {@code n} (exclusive). 序列中介于 {@code 0}(含)和 {@code n}(不含)之间的下一个伪随机 {@code int} 值。
     */
    @Override
    public int nextInt(final int n){
        return (int)nextLong(n);
    }

    /**
     * Returns a pseudo-random, uniformly distributed {@code long} value between 0 (inclusive) and the specified value (exclusive),
     * drawn from this random number generator's sequence. The algorithm used to generate the value guarantees that the result is
     * uniform, provided that the sequence of 64-bit values produced by this generator is.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     * <p>
     * 返回 0(含)到指定值(不含)之间均匀分布的伪随机 {@code long} 值,取自此随机数生成器的序列。生成该值的算法保证结果均匀,前提是此生成器产生的 64 位值序列是均匀的。 <p> 此实现在内部使用 {@link #nextLong()}。
     * @param n the positive bound on the random number to be returned. 返回的随机数的正上界。
     * @return the next pseudo-random {@code long} value between {@code 0} (inclusive) and {@code n} (exclusive). 序列中介于 {@code 0}(含)和 {@code n}(不含)之间的下一个伪随机 {@code long} 值。
     */
    public long nextLong(final long n){
        if(n <= 0) throw new IllegalArgumentException("n must be positive");
        for(;;){
            final long bits = nextLong() >>> 1;
            final long value = bits % n;
            if(bits - value + (n - 1) >= 0) return value;
        }
    }

    /**
     * Returns a pseudo-random, uniformly distributed {@code double} value between 0.0 and 1.0 from this random number generator's
     * sequence.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     * <p>
     * 返回 0.0 到 1.0 之间均匀分布的伪随机 {@code double} 值,取自此随机数生成器的序列。 <p> 此实现在内部使用 {@link #nextLong()}。
     */
    @Override
    public double nextDouble(){
        return (nextLong() >>> 11) * NORM_DOUBLE;
    }

    /**
     * Returns a pseudo-random, uniformly distributed {@code float} value between 0.0 and 1.0 from this random number generator's
     * sequence.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     * <p>
     * 返回 0.0 到 1.0 之间均匀分布的伪随机 {@code float} 值,取自此随机数生成器的序列。 <p> 此实现在内部使用 {@link #nextLong()}。
     */
    @Override
    public float nextFloat(){
        return (float)((nextLong() >>> 40) * NORM_FLOAT);
    }

    /**
     * Returns a pseudo-random, uniformly distributed {@code boolean } value from this random number generator's sequence.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     * <p>
     * 从此随机数生成器的序列返回均匀分布的伪随机 {@code boolean } 值。 <p> 此实现在内部使用 {@link #nextLong()}。
     */
    @Override
    public boolean nextBoolean(){
        return (nextLong() & 1) != 0;
    }

    /**
     * Generates random bytes and places them into a user-supplied byte array. The number of random bytes produced is equal to the
     * length of the byte array.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     * <p>
     * 生成随机字节并放入用户提供的字节数组。生成的随机字节数等于字节数组的长度。 <p> 此实现在内部使用 {@link #nextLong()}。
     */
    @Override
    public void nextBytes(final byte[] bytes){
        int n;
        int i = bytes.length;
        while(i != 0){
            n = i < 8 ? i : 8; // min(i, 8);
            // 限制 i 不超过 8
            for(long bits = nextLong(); n-- != 0; bits >>= 8)
                bytes[--i] = (byte)bits;
        }
    }

    /**
     * Sets the internal seed of this generator based on the given {@code long} value.
     * <p>
     * The given seed is passed twice through a hash function. This way, if the user passes a small value we avoid the short
     * irregular transient associated with states having a very small number of bits set.
     * <p>
     * 根据给定的 {@code long} 值设置此生成器的内部种子。 <p> 给定的种子会两次经过同一个哈希函数。这样,即使用户传入很小的值,也能避免因状态中设置的位数过少而产生的短暂不规则性。
     * @param seed a nonzero seed for this generator (if zero, the generator will be seeded with {@link Long#MIN_VALUE}). 此生成器的非零种子(若为 0,则将使用 {@link Long#MIN_VALUE} 作为种子)。
     */
    @Override
    public void setSeed(final long seed){
        long seed0 = murmurHash3(seed == 0 ? Long.MIN_VALUE : seed);
        setState(seed0, murmurHash3(seed0));
    }

    public boolean chance(double chance){
        return nextDouble() < chance;
    }

    public float range(float amount){
        return nextFloat() * amount * 2 - amount;
    }

    public float random(float max){
        return nextFloat() * max;
    }

    /**
     * Inclusive.
     * 包含边界。
     */
    public int random(int max){
        return nextInt(max + 1);
    }

    public float random(float min, float max){
        return min + (max - min) * nextFloat();
    }

    public int range(int amount){
        return nextInt(amount * 2 + 1) - amount;
    }

    public int random(int min, int max){
        if(min >= max) return min;
        return min + nextInt(max - min + 1);
    }

    /**
     * Sets the internal state of this generator.
     * <p>
     * 设置此生成器的内部状态。
     * @param seed0 the first part of the internal state 内部状态的第一部分
     * @param seed1 the second part of the internal state 内部状态的第二部分
     */
    public void setState(final long seed0, final long seed1){
        this.seed0 = seed0;
        this.seed1 = seed1;
    }

    /**
     * Returns the internal seeds to allow state saving.
     * <p>
     * 返回允许保存状态的内部种子。
     * @param seed must be 0 or 1, designating which of the 2 long seeds to return 必须为 0 或 1,指定返回两个 long 种子中的哪一个
     * @return the internal seed that can be used in setState 可用于 setState 的内部种子
     */
    public long getState(int seed){
        return seed == 0 ? seed0 : seed1;
    }

}
