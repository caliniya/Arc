package arc.struct;

import arc.func.*;
import arc.util.*;

import java.util.*;

import static arc.struct.ObjectSet.*;

/**
 * An unordered map that uses int keys. This implementation is a cuckoo hash map using 3 hashes, random walking, and a small
 * stash for problematic keys. Null values are allowed. No allocation is done except when growing the table size. <br>
 * <br>
 * This map performs very fast get, containsKey, and remove (typically O(1), worst case O(log(n))). Put may be a bit slower,
 * depending on hash collisions. Load factors greater than 0.91 greatly increase the chances the map will have to rehash to the
 * next higher POT size.
 * <p>
 * 使用 int 键的无序映射。此实现是布谷鸟哈希映射,使用 3 个哈希函数、随机游走以及一个存放问题键的小 stash。允许 null 值。除扩容表大小外不进行任何分配。<br> <br> 此映射的 get、containsKey 和 remove 非常快(通常 O(1),最坏 O(log(n)))。put 可能稍慢,取决于哈希冲突。负载因子大于 0.91 会大大增加映射被迫重新哈希到下一个更高 2 的幂(POT)大小的机会。
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class IntMap<V> implements Iterable<IntMap.Entry<V>>{
    public int size;

    int[] keyTable;
    V[] valueTable;

    V zeroValue;
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

    private transient Entries entries1, entries2;
    private transient Values values1, values2;
    private transient Keys keys1, keys2;

    public static <V> IntMap<V> of(Object... values){
        IntMap<V> map = new IntMap<>();

        for(int i = 0; i < values.length / 2; i++){
            Object key = values[i * 2];
            int keyInt = (key instanceof Character ? ((Character) key).charValue() : (Integer)key);
            map.put(keyInt, (V) values[i * 2 + 1]);
        }

        return map;
    }

    /**
     * Creates a new map with an initial capacity of 51 and a load factor of 0.8.
     * 创建一个初始容量为 51、负载因子为 0.8 的新映射。
     */
    public IntMap(){
        this(51, 0.8f);
    }

    /**
     * Creates a new map with a load factor of 0.8.
     * <p>
     * 创建一个负载因子为 0.8 的新映射。
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two. 底层数组大小为 initialCapacity / loadFactor,并增加到下一个 2 的幂。
     */
    public IntMap(int initialCapacity){
        this(initialCapacity, 0.8f);
    }

    /**
     * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
     * growing the backing table.
     * <p>
     * 创建一个具有指定初始容量和负载因子的新映射。在底层数组扩容之前,此映射可容纳 initialCapacity 个条目。
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two. 底层数组大小为 initialCapacity / loadFactor,并增加到下一个 2 的幂。
     */
    public IntMap(int initialCapacity, float loadFactor){
        if(loadFactor <= 0f || loadFactor >= 1f)
            throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
        this.loadFactor = loadFactor;

        int tableSize = tableSize(initialCapacity, loadFactor);
        threshold = (int)(tableSize * loadFactor);
        mask = tableSize - 1;
        shift = Long.numberOfLeadingZeros(mask);

        keyTable = new int[tableSize];
        valueTable = (V[])new Object[tableSize];
    }

    /**
     * Creates a new map identical to the specified map.
     * 创建一个与指定映射完全相同的新映射。
     */
    public IntMap(IntMap<? extends V> map){
        this((int)(map.keyTable.length * map.loadFactor), map.loadFactor);
        System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
        System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
        size = map.size;
        zeroValue = map.zeroValue;
        hasZeroValue = map.hasZeroValue;
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

    public @Nullable V put(int key, @Nullable V value){
        if(key == 0){
            V oldValue = zeroValue;
            zeroValue = value;
            if(!hasZeroValue){
                hasZeroValue = true;
                size++;
            }
            return oldValue;
        }
        int i = locateKey(key);
        if(i >= 0){ // Existing key was found.
        // 已找到现有键。
            V oldValue = valueTable[i];
            valueTable[i] = value;
            return oldValue;
        }
        i = -(i + 1); // Empty space was found.
        // 已找到空位。
        keyTable[i] = key;
        valueTable[i] = value;
        if(++size >= threshold) resize(keyTable.length << 1);
        return null;
    }

    public @Nullable V putMissing(int key, @Nullable V value){
        if(key == 0){
            V oldValue = zeroValue;
            if(!hasZeroValue){
                zeroValue = value;
                hasZeroValue = true;
                size++;
            }
            return oldValue;
        }
        int i = locateKey(key);
        if(i >= 0) return valueTable[i]; // Existing key was found.
        // 已找到现有键。
        i = -(i + 1); // Empty space was found.
        // 已找到空位。
        keyTable[i] = key;
        valueTable[i] = value;
        if(++size >= threshold) resize(keyTable.length << 1);
        return null;
    }

    public void putAll(IntMap<? extends V> map){
        ensureCapacity(map.size);
        if(map.hasZeroValue) put(0, map.zeroValue);
        int[] keyTable = map.keyTable;
        V[] valueTable = map.valueTable;
        for(int i = 0, n = keyTable.length; i < n; i++){
            int key = keyTable[i];
            if(key != 0) put(key, valueTable[i]);
        }
    }

    /**
     * Skips checks for existing keys, doesn't increment size, doesn't need to handle key 0.
     * 跳过对现有键的检查,不增加大小,无需处理键 0。
     */
    private void putResize(int key, @Nullable V value){
        int[] keyTable = this.keyTable;
        for(int i = place(key); ; i = (i + 1) & mask){
            if(keyTable[i] == 0){
                keyTable[i] = key;
                valueTable[i] = value;
                return;
            }
        }
    }

    public V get(int key){
        if(key == 0) return hasZeroValue ? zeroValue : null;
        int i = locateKey(key);
        return i >= 0 ? valueTable[i] : null;
    }

    public V get(int key, @Nullable V defaultValue){
        if(key == 0) return hasZeroValue ? zeroValue : defaultValue;
        int i = locateKey(key);
        return i >= 0 ? valueTable[i] : defaultValue;
    }

    public V get(int key, Prov<V> defaultValue){
        V out = get(key);
        if(out == null){
            out = defaultValue.get();
            put(key, out);
        }
        return out;
    }

    /**
     * Returns the value for the removed key, or null if the key is not in the map.
     * 返回被移除键对应的值,如果键不在映射中则返回 null。
     */
    public @Nullable V remove(int key){
        if(key == 0){
            if(!hasZeroValue) return null;
            hasZeroValue = false;
            V oldValue = zeroValue;
            zeroValue = null;
            size--;
            return oldValue;
        }

        int i = locateKey(key);
        if(i < 0) return null;
        int[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        V oldValue = valueTable[i];
        int mask = this.mask, next = i + 1 & mask;
        while((key = keyTable[next]) != 0){
            int placement = place(key);
            if((next - placement & mask) > (i - placement & mask)){
                keyTable[i] = key;
                valueTable[i] = valueTable[next];
                i = next;
            }
            next = next + 1 & mask;
        }
        keyTable[i] = 0;
        valueTable[i] = null;
        size--;
        return oldValue;
    }

    /**
     * Returns true if the map has one or more items.
     * 如果映射中有一个或多个条目,返回 true。
     */
    public boolean notEmpty(){
        return size > 0;
    }

    /**
     * Returns true if the map is empty.
     * 如果映射为空,返回 true。
     */
    public boolean isEmpty(){
        return size == 0;
    }

    /**
     * Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
     * nothing is done. If the map contains more items than the specified capacity, the next highest power of two capacity is used
     * instead.
     * <p>
     * 将底层数组的大小缩减为不超过指定容量 / loadFactor。如果容量已经更小,则不做任何操作。如果映射包含的条目多于指定容量,则改用次高的 2 的幂容量。
     */
    public void shrink(int maximumCapacity){
        if(maximumCapacity < 0) throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
        if(size > maximumCapacity) maximumCapacity = size;
        int tableSize = tableSize(maximumCapacity, loadFactor);
        if(keyTable.length > tableSize) resize(tableSize);
    }

    /**
     * Clears the map and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.
     * 清除映射,并将底层数组的大小缩减为指定容量 / loadFactor(如果它们更大的话)。
     */
    public void clear(int maximumCapacity){
        int tableSize = tableSize(maximumCapacity, loadFactor);
        if(keyTable.length <= tableSize){
            clear();
            return;
        }
        size = 0;
        hasZeroValue = false;
        zeroValue = null;
        resize(tableSize);
    }

    public void clear(){
        if(size == 0) return;
        size = 0;
        Arrays.fill(keyTable, 0);
        Arrays.fill(valueTable, null);
        zeroValue = null;
        hasZeroValue = false;
    }

    /**
     * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
     * be an expensive operation.
     * <p>
     * 如果指定 value 在映射中,返回 true。注意这会遍历整个映射并比较每个值,可能是昂贵的操作。
     * @param identity If true, uses == to compare the specified value with values in the map. If false, uses 如果为 true,使用 == 将指定值与映射中的值比较;如果为 false,使用 .equals()
     * {@link #equals(Object)}.
     */
    public boolean containsValue(@Nullable Object value, boolean identity){
        V[] valueTable = this.valueTable;
        if(value == null){
            if(hasZeroValue && zeroValue == null) return true;
            int[] keyTable = this.keyTable;
            for(int i = valueTable.length - 1; i >= 0; i--)
                if(keyTable[i] != 0 && valueTable[i] == null) return true;
        }else if(identity){
            if(value == zeroValue) return true;
            for(int i = valueTable.length - 1; i >= 0; i--)
                if(valueTable[i] == value) return true;
        }else{
            if(hasZeroValue && value.equals(zeroValue)) return true;
            for(int i = valueTable.length - 1; i >= 0; i--)
                if(value.equals(valueTable[i])) return true;
        }
        return false;

    }

    public boolean containsKey(int key){
        if(key == 0) return hasZeroValue;
        return locateKey(key) >= 0;
    }

    /**
     * Returns the key for the specified value, or <tt>notFound</tt> if it is not in the map. Note this traverses the entire map
     * and compares every value, which may be an expensive operation.
     * <p>
     * 返回指定 value 对应的 key,如果它不在映射中则返回 <tt>notFound</tt>。注意这会遍历整个映射并比较每个值,可能是昂贵的操作。
     * @param identity If true, uses == to compare the specified value with values in the map. If false, uses 如果为 true,使用 == 将指定值与映射中的值比较;如果为 false,使用 .equals()
     * {@link #equals(Object)}.
     */
    public int findKey(@Nullable Object value, boolean identity, int notFound){
        V[] valueTable = this.valueTable;
        if(value == null){
            if(hasZeroValue && zeroValue == null) return 0;
            int[] keyTable = this.keyTable;
            for(int i = valueTable.length - 1; i >= 0; i--)
                if(keyTable[i] != 0 && valueTable[i] == null) return keyTable[i];
        }else if(identity){
            if(value == zeroValue) return 0;
            for(int i = valueTable.length - 1; i >= 0; i--)
                if(valueTable[i] == value) return keyTable[i];
        }else{
            if(hasZeroValue && value.equals(zeroValue)) return 0;
            for(int i = valueTable.length - 1; i >= 0; i--)
                if(value.equals(valueTable[i])) return keyTable[i];
        }
        return notFound;
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
        V[] oldValueTable = valueTable;

        keyTable = new int[newSize];
        valueTable = (V[])new Object[newSize];

        if(size > 0){
            for(int i = 0; i < oldCapacity; i++){
                int key = oldKeyTable[i];
                if(key != 0) putResize(key, oldValueTable[i]);
            }
        }
    }

    public int hashCode(){
        int h = size;
        if(hasZeroValue && zeroValue != null) h += zeroValue.hashCode();
        int[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        for(int i = 0, n = keyTable.length; i < n; i++){
            int key = keyTable[i];
            if(key != 0){
                h += key * 31;
                V value = valueTable[i];
                if(value != null) h += value.hashCode();
            }
        }
        return h;
    }

    public boolean equals(Object obj){
        if(obj == this) return true;
        if(!(obj instanceof IntMap)) return false;
        IntMap other = (IntMap)obj;
        if(other.size != size) return false;
        if(other.hasZeroValue != hasZeroValue) return false;
        if(hasZeroValue){
            if(other.zeroValue == null){
                if(zeroValue != null) return false;
            }else{
                if(!other.zeroValue.equals(zeroValue)) return false;
            }
        }
        int[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        for(int i = 0, n = keyTable.length; i < n; i++){
            int key = keyTable[i];
            if(key != 0){
                V value = valueTable[i];
                if(value == null){
                    if(other.get(key, ObjectMap.dummy) != null) return false;
                }else{
                    if(!value.equals(other.get(key))) return false;
                }
            }
        }
        return true;
    }

    /**
     * Uses == for comparison of each value.
     * 对每个值使用 == 进行比较。
     */
    public boolean equalsIdentity(@Nullable Object obj){
        if(obj == this) return true;
        if(!(obj instanceof IntMap)) return false;
        IntMap other = (IntMap)obj;
        if(other.size != size) return false;
        if(other.hasZeroValue != hasZeroValue) return false;
        if(hasZeroValue && zeroValue != other.zeroValue) return false;
        int[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        for(int i = 0, n = keyTable.length; i < n; i++){
            int key = keyTable[i];
            if(key != 0 && valueTable[i] != other.get(key, ObjectMap.dummy)) return false;
        }
        return true;
    }

    public String toString(){
        if(size == 0) return "[]";
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('[');
        int[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        int i = keyTable.length;
        if(hasZeroValue){
            buffer.append("0=");
            buffer.append(zeroValue);
        }else{
            while(i-- > 0){
                int key = keyTable[i];
                if(key == 0) continue;
                buffer.append(key);
                buffer.append('=');
                buffer.append(valueTable[i]);
                break;
            }
        }
        while(i-- > 0){
            int key = keyTable[i];
            if(key == 0) continue;
            buffer.append(", ");
            buffer.append(key);
            buffer.append('=');
            buffer.append(valueTable[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }

    public Iterator<Entry<V>> iterator(){
        return entries();
    }

    /**
     * Returns an iterator for the entries in the map. Remove is supported.
     * <p>
     * Use the {@link Entries} constructor for nested or multithreaded iteration.
     * <p>
     * 返回映射中条目的迭代器。支持移除。<p> 嵌套或多线程遍历时,请使用 {@link Entries} 构造函数。
     */
    public Entries<V> entries(){
        if(entries1 == null){
            entries1 = new Entries(this);
            entries2 = new Entries(this);
        }
        if(!entries1.valid){
            entries1.reset();
            entries1.valid = true;
            entries2.valid = false;
            return entries1;
        }
        entries2.reset();
        entries2.valid = true;
        entries1.valid = false;
        return entries2;
    }

    /**
     * Returns an iterator for the values in the map. Remove is supported.
     * <p>
     * Use the {@link Entries} constructor for nested or multithreaded iteration.
     * <p>
     * 返回映射中值的迭代器。支持移除。<p> 嵌套或多线程遍历时,请使用 {@link Entries} 构造函数。
     */
    public Values<V> values(){
        if(values1 == null){
            values1 = new Values(this);
            values2 = new Values(this);
        }
        if(!values1.valid){
            values1.reset();
            values1.valid = true;
            values2.valid = false;
            return values1;
        }
        values2.reset();
        values2.valid = true;
        values1.valid = false;
        return values2;
    }

    /**
     * Returns an iterator for the keys in the map. Remove is supported.
     * <p>
     * Use the {@link Entries} constructor for nested or multithreaded iteration.
     * <p>
     * 返回映射中键的迭代器。支持移除。<p> 嵌套或多线程遍历时,请使用 {@link Entries} 构造函数。
     */
    public Keys keys(){
        if(keys1 == null){
            keys1 = new Keys(this);
            keys2 = new Keys(this);
        }
        if(!keys1.valid){
            keys1.reset();
            keys1.valid = true;
            keys2.valid = false;
            return keys1;
        }
        keys2.reset();
        keys2.valid = true;
        keys1.valid = false;
        return keys2;
    }

    static public class Entry<V>{
        public int key;
        public @Nullable V value;

        public String toString(){
            return key + "=" + value;
        }
    }

    static private class MapIterator<V>{
        static private final int INDEX_ILLEGAL = -2;
        static final int INDEX_ZERO = -1;

        public boolean hasNext;

        final IntMap<V> map;
        int nextIndex, currentIndex;
        boolean valid = true;

        public MapIterator(IntMap<V> map){
            this.map = map;
            reset();
        }

        public void reset(){
            currentIndex = INDEX_ILLEGAL;
            nextIndex = INDEX_ZERO;
            if(map.hasZeroValue)
                hasNext = true;
            else
                findNextIndex();
        }

        void findNextIndex(){
            int[] keyTable = map.keyTable;
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
            if(i == INDEX_ZERO && map.hasZeroValue){
                map.hasZeroValue = false;
                map.zeroValue = null;
            }else if(i < 0){
                throw new IllegalStateException("next must be called before remove.");
            }else{
                int[] keyTable = map.keyTable;
                V[] valueTable = map.valueTable;
                int mask = map.mask, next = i + 1 & mask, key;
                while((key = keyTable[next]) != 0){
                    int placement = map.place(key);
                    if((next - placement & mask) > (i - placement & mask)){
                        keyTable[i] = key;
                        valueTable[i] = valueTable[next];
                        i = next;
                    }
                    next = next + 1 & mask;
                }
                keyTable[i] = 0;
                valueTable[i] = null;
                if(i != currentIndex) --nextIndex;
            }
            currentIndex = INDEX_ILLEGAL;
            map.size--;
        }
    }

    static public class Entries<V> extends MapIterator<V> implements Iterable<Entry<V>>, Iterator<Entry<V>>{
        private final Entry<V> entry = new Entry();

        public Entries(IntMap map){
            super(map);
        }

        /**
         * Note the same entry instance is returned each time this method is called.
         * 注意每次调用此方法都返回相同的 entry 实例。
         */
        public Entry<V> next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            int[] keyTable = map.keyTable;
            if(nextIndex == INDEX_ZERO){
                entry.key = 0;
                entry.value = map.zeroValue;
            }else{
                entry.key = keyTable[nextIndex];
                entry.value = map.valueTable[nextIndex];
            }
            currentIndex = nextIndex;
            findNextIndex();
            return entry;
        }

        public boolean hasNext(){
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return hasNext;
        }

        public Iterator<Entry<V>> iterator(){
            return this;
        }
    }

    static public class Values<V> extends MapIterator<V> implements Iterable<V>, Iterator<V>{
        public Values(IntMap<V> map){
            super(map);
        }

        public boolean hasNext(){
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return hasNext;
        }

        public @Nullable V next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            V value;
            if(nextIndex == INDEX_ZERO)
                value = map.zeroValue;
            else
                value = map.valueTable[nextIndex];
            currentIndex = nextIndex;
            findNextIndex();
            return value;
        }

        public Iterator<V> iterator(){
            return this;
        }

        /**
         * Returns a new array containing the remaining values.
         * 返回一个包含剩余值的新数组。
         */
        public Ar<V> toArray(){
            Ar<V> array = new Ar<>(true, map.size);
            while(hasNext)
                array.add(next());
            return array;
        }
    }

    static public class Keys extends MapIterator{
        public Keys(IntMap map){
            super(map);
        }

        public int next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            int key = nextIndex == INDEX_ZERO ? 0 : map.keyTable[nextIndex];
            currentIndex = nextIndex;
            findNextIndex();
            return key;
        }

        /**
         * Returns a new array containing the remaining keys.
         * 返回一个包含剩余键的新数组。
         */
        public IntAr toSeq(){
            IntAr array = new IntAr(true, map.size);
            while(hasNext)
                array.add(next());
            return array;
        }

        /**
         * Adds the remaining values to the specified array.
         * 将剩余的值添加到指定数组。
         */
        public IntAr toSeq(IntAr array){
            while(hasNext)
                array.add(next());
            return array;
        }
    }
}
