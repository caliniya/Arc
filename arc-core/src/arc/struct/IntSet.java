package arc.struct;

import arc.func.*;
import arc.util.*;

import java.util.*;

import static arc.struct.ObjectSet.*;

/**
 * An unordered set that uses int keys. This implementation uses cuckoo hashing using 3 hashes, random walking, and a small stash
 * for problematic keys. No allocation is done except when growing the table size. <br>
 * <br>
 * This set performs very fast contains and remove (typically O(1), worst case O(log(n))). Add may be a bit slower, depending on
 * hash collisions. Load factors greater than 0.91 greatly increase the chances the set will have to rehash to the next higher POT
 * size.
 * <p>
 * 使用 int 键的无序集合。此实现使用布谷鸟哈希,采用 3 个哈希函数、随机游走以及一个存放问题键的小 stash。除扩容表大小外不进行任何分配。<br> <br> 此集合的 contains 和 remove 非常快(通常 O(1),最坏 O(log(n)))。add 可能稍慢,取决于哈希冲突。负载因子大于 0.91 会大大增加集合被迫重新哈希到下一个更高 2 的幂(POT)大小的机会。
 * @author Nathan Sweet
 */
public class IntSet{
    public int size;

    int[] keyTable;
    boolean hasZeroValue;

    private final float loadFactor;
    private int threshold;

    /**
     * Used by {@link #place(int)} to bit shift the upper bits of a {@code long} into a usable range (&gt;= 0 and &lt;=
     * {@link #mask}). The shift can be negative, which is convenient to match the number of bits in mask: if mask is a 7-bit
     * number, a shift of -7 shifts the upper 7 bits into the lowest 7 positions. This class sets the shift &gt; 32 and &lt; 64,
     * which if used with an int will still move the upper bits of an int to the lower bits due to Java's implicit modulus on
     * shifts.
     * <p>
     * {@link #mask} can also be used to mask the low bits of a number, which may be faster for some hashcodes, if
     * {@link #place(int)} is overridden.
     * <p>
     * 供 {@link #place(int)} 使用,将 {@code long} 的高位移入可用范围(&gt;= 0 且 &lt;= {@link #mask})。shift 可以为负,这便于匹配 mask 的位数:如果 mask 是一个 7 位数,-7 的 shift 会把高 7 位移入最低 7 位。此类将 shift 设为 &gt; 32 且 &lt; 64,若与 int 一起使用,由于 Java 对移位的隐式取模,int 的高位仍会移到低位。<p> {@link #mask} 也可用于屏蔽数字的低位,如果重写了 {@link #place(int)},这对某些哈希码可能更快。
     */
    protected int shift;

    /**
     * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
     * minus 1. If {@link #place(int)} is overridden, this can be used instead of {@link #shift} to isolate usable bits of a
     * hash.
     * <p>
     * 用于将哈希码限制在表大小范围内的位掩码。其低位必须全为 1,即 2 的幂减 1。如果重写了 {@link #place(int)},可以用它代替 {@link #shift} 来隔离哈希的可用位。
     */
    protected int mask;

    private transient IntSetIterator iterator1, iterator2;

    /**
     * Creates a new set with an initial capacity of 51 and a load factor of 0.8.
     * 创建一个初始容量为 51、负载因子为 0.8 的新集合。
     */
    public IntSet(){
        this(51, 0.8f);
    }

    /**
     * Creates a new set with a load factor of 0.8.
     * <p>
     * 创建一个负载因子为 0.8 的新集合。
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two. 底层数组大小为 initialCapacity / loadFactor,并增加到下一个 2 的幂。
     */
    public IntSet(int initialCapacity){
        this(initialCapacity, 0.8f);
    }

    /**
     * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
     * growing the backing table.
     * <p>
     * 创建一个具有指定初始容量和负载因子的新集合。在底层数组扩容之前,此集合可容纳 initialCapacity 个条目。
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two. 底层数组大小为 initialCapacity / loadFactor,并增加到下一个 2 的幂。
     */
    public IntSet(int initialCapacity, float loadFactor){
        if(loadFactor <= 0f || loadFactor >= 1f)
            throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
        this.loadFactor = loadFactor;

        int tableSize = tableSize(initialCapacity, loadFactor);
        threshold = (int)(tableSize * loadFactor);
        mask = tableSize - 1;
        shift = Long.numberOfLeadingZeros(mask);

        keyTable = new int[tableSize];
    }

    /**
     * Creates a new set identical to the specified set.
     * 创建一个与指定集合完全相同的新集合。
     */
    public IntSet(IntSet set){
        this((int)(set.keyTable.length * set.loadFactor), set.loadFactor);
        System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.length);
        size = set.size;
        hasZeroValue = set.hasZeroValue;
    }

    public void each(Intc cons){
        IntSetIterator iter = iterator();
        while(iter.hasNext){
            cons.get(iter.next());
        }
    }

    /**
     * Returns an index >= 0 and <= {@link #mask} for the specified {@code item}.
     * <p>
     * The default implementation uses Fibonacci hashing on the item's {@link Object#hashCode()}: the hashcode is multiplied by a
     * long constant (2 to the 64th, divided by the golden ratio) then the uppermost bits are shifted into the lowest positions to
     * obtain an index in the desired range. Multiplication by a long may be slower than int (eg on GWT) but greatly improves
     * rehashing, allowing even very poor hashcodes, such as those that only differ in their upper bits, to be used without high
     * collision rates. Fibonacci hashing has increased collision rates when all or most hashcodes are multiples of larger
     * Fibonacci numbers (see <a href=
     * "https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/">Malte
     * Skarupke's blog post</a>).
     * <p>
     * This method can be overridden to customizing hashing. This may be useful eg in the unlikely event that most hashcodes are
     * Fibonacci numbers, if keys provide poor or incorrect hashcodes, or to simplify hashing if keys provide high quality
     * hashcodes and don't need Fibonacci hashing.
     * <p>
     * 为指定的 {@code item} 返回一个 >= 0 且 <= {@link #mask} 的索引。<p> 默认实现对 item 的 {@link Object#hashCode()} 使用斐波那契哈希:哈希码乘以一个 long 常量(2 的 64 次方除以黄金分割比),然后将最高位移入最低位,以获得所需范围内的索引。用 long 相乘可能比 int 慢(例如在 GWT 上),但能大大改进重新哈希,使得即使非常差的哈希码(例如只有高位不同的哈希码)也能在不会产生高冲突率的情况下使用。当全部或大多数哈希码是较大斐波那契数的倍数时,斐波那契哈希的冲突率会升高(见 <a href= "https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/">Malte Skarupke 的博客文章</a>)。<p> 可以重写此方法来自定义哈希。这在以下情况下可能有用:例如绝大多数哈希码恰为斐波那契数这种罕见情况、键提供的哈希码质量差或不正确时,或当键提供的哈希码质量很高、不需要斐波那契哈希从而简化哈希时。
     */
    protected int place(int item){
        return (int)(item * 0x9E3779B97F4A7C15L >>> shift);
    }

    /**
     * Returns the index of the key if already present, else -(index + 1) for the next empty index.
     * 如果键已存在,返回其索引;否则返回 -(index + 1),即下一个空索引。
     */
    private int locateKey(int key){
        int[] keyTable = this.keyTable;
        for(int i = place(key); ; i = i + 1 & mask){
            int other = keyTable[i];
            if(other == 0) return -(i + 1); // Empty space is available.
            // 有空位可用。
            if(other == key) return i; // Same key was found.
            // 已找到相同的键。
        }
    }

    /**
     * Returns true if the key was added to the set or false if it was already in the set.
     * 如果键被添加到集合中则返回 true,如果它已在集合中则返回 false。
     */
    public boolean add(int key){
        if(key == 0){
            if(hasZeroValue) return false;
            hasZeroValue = true;
            size++;
            return true;
        }
        int i = locateKey(key);
        if(i >= 0) return false; // Existing key was found.
        // 已找到现有键。
        i = -(i + 1); // Empty space was found.
        // 已找到空位。
        keyTable[i] = key;
        if(++size >= threshold) resize(keyTable.length << 1);
        return true;
    }

    public void addAll(IntAr array){
        addAll(array.items, 0, array.size);
    }

    public void addAll(IntAr array, int offset, int length){
        if(offset + length > array.size)
            throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
        addAll(array.items, offset, length);
    }

    public void addAll(int... array){
        addAll(array, 0, array.length);
    }

    public void addAll(int[] array, int offset, int length){
        ensureCapacity(length);
        for(int i = offset, n = i + length; i < n; i++)
            add(array[i]);
    }

    public void addAll(IntSet set){
        ensureCapacity(set.size);
        if(set.hasZeroValue) add(0);
        int[] keyTable = set.keyTable;
        for(int i = 0, n = keyTable.length; i < n; i++){
            int key = keyTable[i];
            if(key != 0) add(key);
        }
    }

    /**
     * Skips checks for existing keys, doesn't increment size, doesn't need to handle key 0.
     * 跳过对现有键的检查,不增加大小,无需处理键 0。
     */
    private void addResize(int key){
        int[] keyTable = this.keyTable;
        for(int i = place(key); ; i = (i + 1) & mask){
            if(keyTable[i] == 0){
                keyTable[i] = key;
                return;
            }
        }
    }

    /**
     * Returns true if the key was removed.
     * 如果键被移除了,返回 true。
     */
    public boolean remove(int key){
        if(key == 0){
            if(!hasZeroValue) return false;
            hasZeroValue = false;
            size--;
            return true;
        }

        int i = locateKey(key);
        if(i < 0) return false;
        int[] keyTable = this.keyTable;
        int mask = this.mask, next = i + 1 & mask;
        while((key = keyTable[next]) != 0){
            int placement = place(key);
            if((next - placement & mask) > (i - placement & mask)){
                keyTable[i] = key;
                i = next;
            }
            next = next + 1 & mask;
        }
        keyTable[i] = 0;
        size--;
        return true;
    }

    /**
     * Returns true if the set has one or more items.
     * 如果集合中有一个或多个条目,返回 true。
     */
    public boolean notEmpty(){
        return size > 0;
    }

    /**
     * Returns true if the set is empty.
     * 如果集合为空,返回 true。
     */
    public boolean isEmpty(){
        return size == 0;
    }

    /**
     * Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
     * nothing is done. If the set contains more items than the specified capacity, the next highest power of two capacity is used
     * instead.
     * <p>
     * 将底层数组的大小缩减为不超过指定容量 / loadFactor。如果容量已经更小,则不做任何操作。如果集合包含的条目多于指定容量,则改用次高的 2 的幂容量。
     */
    public void shrink(int maximumCapacity){
        if(maximumCapacity < 0) throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
        if(size > maximumCapacity) maximumCapacity = size;
        int tableSize = tableSize(maximumCapacity, loadFactor);
        if(keyTable.length > tableSize) resize(tableSize);
    }

    /**
     * Clears the set and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.
     * 清除集合,并将底层数组的大小缩减为指定容量 / loadFactor(如果它们更大的话)。
     */
    public void clear(int maximumCapacity){
        int tableSize = tableSize(maximumCapacity, loadFactor);
        if(keyTable.length <= tableSize){
            clear();
            return;
        }
        size = 0;
        hasZeroValue = false;
        resize(tableSize);
    }

    public void clear(){
        if(size == 0) return;
        size = 0;
        Arrays.fill(keyTable, 0);
        hasZeroValue = false;
    }

    public boolean contains(int key){
        if(key == 0) return hasZeroValue;
        return locateKey(key) >= 0;
    }

    public int first(){
        if(hasZeroValue) return 0;
        int[] keyTable = this.keyTable;
        for(int i = 0, n = keyTable.length; i < n; i++)
            if(keyTable[i] != 0) return keyTable[i];
        throw new IllegalStateException("IntSet is empty.");
    }

    /**
     * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
     * adding many items to avoid multiple backing array resizes.
     * <p>
     * 增大底层数组的大小,以容纳指定数量的额外条目 / loadFactor。在添加大量条目之前很有用,可避免多次底层数组扩容。
     */
    public void ensureCapacity(int additionalCapacity){
        int tableSize = tableSize(size + additionalCapacity, loadFactor);
        if(keyTable.length < tableSize) resize(tableSize);
    }

    private void resize(int newSize){
        int oldCapacity = keyTable.length;
        threshold = (int)(newSize * loadFactor);
        mask = newSize - 1;
        shift = Long.numberOfLeadingZeros(mask);

        int[] oldKeyTable = keyTable;

        keyTable = new int[newSize];

        if(size > 0){
            for(int i = 0; i < oldCapacity; i++){
                int key = oldKeyTable[i];
                if(key != 0) addResize(key);
            }
        }
    }

    public int hashCode(){
        int h = size;
        int[] keyTable = this.keyTable;
        for(int i = 0, n = keyTable.length; i < n; i++){
            int key = keyTable[i];
            if(key != 0) h += key;
        }
        return h;
    }

    public boolean equals(Object obj){
        if(!(obj instanceof IntSet)) return false;
        IntSet other = (IntSet)obj;
        if(other.size != size) return false;
        if(other.hasZeroValue != hasZeroValue) return false;
        int[] keyTable = this.keyTable;
        for(int i = 0, n = keyTable.length; i < n; i++)
            if(keyTable[i] != 0 && !other.contains(keyTable[i])) return false;
        return true;
    }

    public String toString(){
        if(size == 0) return "[]";
        java.lang.StringBuilder buffer = new java.lang.StringBuilder(32);
        buffer.append('[');
        int[] keyTable = this.keyTable;
        int i = keyTable.length;
        if(hasZeroValue)
            buffer.append("0");
        else{
            while(i-- > 0){
                int key = keyTable[i];
                if(key == 0) continue;
                buffer.append(key);
                break;
            }
        }
        while(i-- > 0){
            int key = keyTable[i];
            if(key == 0) continue;
            buffer.append(", ");
            buffer.append(key);
        }
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Returns an iterator for the keys in the set. Remove is supported.
     * <p>
     * Use the {@link IntSetIterator} constructor for nested or multithreaded iteration.
     * <p>
     * 返回集合中键的迭代器。支持移除。<p> 嵌套或多线程遍历时,请使用 {@link IntSetIterator} 构造函数。
     */
    public IntSetIterator iterator(){
        if(iterator1 == null){
            iterator1 = new IntSetIterator(this);
            iterator2 = new IntSetIterator(this);
        }
        if(!iterator1.valid){
            iterator1.reset();
            iterator1.valid = true;
            iterator2.valid = false;
            return iterator1;
        }
        iterator2.reset();
        iterator2.valid = true;
        iterator1.valid = false;
        return iterator2;
    }

    static public IntSet with(int... array){
        IntSet set = new IntSet(array.length);
        set.addAll(array);
        return set;
    }

    static public class IntSetIterator{
        static private final int INDEX_ILLEGAL = -2, INDEX_ZERO = -1;

        public boolean hasNext;

        final IntSet set;
        int nextIndex, currentIndex;
        boolean valid = true;

        public IntSetIterator(IntSet set){
            this.set = set;
            reset();
        }

        public void reset(){
            currentIndex = INDEX_ILLEGAL;
            nextIndex = INDEX_ZERO;
            if(set.hasZeroValue)
                hasNext = true;
            else
                findNextIndex();
        }

        void findNextIndex(){
            int[] keyTable = set.keyTable;
            for(int n = keyTable.length; ++nextIndex < n; ){
                if(keyTable[nextIndex] != 0){
                    hasNext = true;
                    return;
                }
            }
            hasNext = false;
        }

        public void remove(){
            int i = currentIndex;
            if(i == INDEX_ZERO && set.hasZeroValue){
                set.hasZeroValue = false;
            }else if(i < 0){
                throw new IllegalStateException("next must be called before remove.");
            }else{
                int[] keyTable = set.keyTable;
                int mask = set.mask, next = i + 1 & mask, key;
                while((key = keyTable[next]) != 0){
                    int placement = set.place(key);
                    if((next - placement & mask) > (i - placement & mask)){
                        keyTable[i] = key;
                        i = next;
                    }
                    next = next + 1 & mask;
                }
                keyTable[i] = 0;
                if(i != currentIndex) --nextIndex;
            }
            currentIndex = INDEX_ILLEGAL;
            set.size--;
        }

        public int next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            int key = nextIndex == INDEX_ZERO ? 0 : set.keyTable[nextIndex];
            currentIndex = nextIndex;
            findNextIndex();
            return key;
        }

        /**
         * Returns a new array containing the remaining keys.
         * 返回一个包含剩余键的新数组。
         */
        public IntAr toSeq(){
            IntAr array = new IntAr(true, set.size);
            while(hasNext)
                array.add(next());
            return array;
        }
    }
}
