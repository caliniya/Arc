package arc.struct;

import arc.util.*;

import java.util.*;

import static arc.struct.ObjectSet.*;

/**
 * An unordered map where the values are ints. This implementation is a cuckoo hash map using 3 hashes, random walking, and a
 * small stash for problematic keys. Null keys are not allowed. No allocation is done except when growing the table size. <br>
 * <br>
 * This map performs very fast get, containsKey, and remove (typically O(1), worst case O(log(n))). Put may be a bit slower,
 * depending on hash collisions. Load factors greater than 0.91 greatly increase the chances the map will have to rehash to the
 * next higher POT size.
 * <p>
 * 值为 int 的无序映射。此实现是布谷鸟哈希映射,使用 3 个哈希函数、随机游走以及一个存放问题键的小 stash。不允许 null 键。除扩容表大小外不进行任何分配。<br> <br> 此映射的 get、containsKey 和 remove 非常快(通常 O(1),最坏 O(log(n)))。put 可能稍慢,取决于哈希冲突。负载因子大于 0.91 会大大增加映射被迫重新哈希到下一个更高 2 的幂(POT)大小的机会。
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class ObjectIntMap<K> implements Iterable<ObjectIntMap.Entry<K>>{
    public int size;

    K[] keyTable;
    int[] valueTable;

    float loadFactor;
    int threshold;

    /**
     * Used by {@link #place(Object)} to bit shift the upper bits of a {@code long} into a usable range (&gt;= 0 and &lt;=
     * {@link #mask}). The shift can be negative, which is convenient to match the number of bits in mask: if mask is a 7-bit
     * number, a shift of -7 shifts the upper 7 bits into the lowest 7 positions. This class sets the shift &gt; 32 and &lt; 64,
     * which if used with an int will still move the upper bits of an int to the lower bits due to Java's implicit modulus on
     * shifts.
     * <p>
     * {@link #mask} can also be used to mask the low bits of a number, which may be faster for some hashcodes, if
     * {@link #place(Object)} is overridden.
     * <p>
     * 供 {@link #place(Object)} 使用,将 {@code long} 的高位移入可用范围(&gt;= 0 且 &lt;= {@link #mask})。shift 可以为负,这便于匹配 mask 的位数:如果 mask 是一个 7 位数,-7 的 shift 会把高 7 位移入最低 7 位。此类将 shift 设为 &gt; 32 且 &lt; 64,若与 int 一起使用,由于 Java 对移位的隐式取模,int 的高位仍会移到低位。<p> {@link #mask} 也可用于屏蔽数字的低位,如果重写了 {@link #place(Object)},这对某些哈希码可能更快。
     */
    protected int shift;

    /**
     * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
     * minus 1. If {@link #place(Object)} is overridden, this can be used instead of {@link #shift} to isolate usable bits of a
     * hash.
     * <p>
     * 用于将哈希码限制在表大小范围内的位掩码。其低位必须全为 1,即 2 的幂减 1。如果重写了 {@link #place(Object)},可以用它代替 {@link #shift} 来隔离哈希的可用位。
     */
    protected int mask;

    transient Entries entries1, entries2;
    transient Values values1, values2;
    transient Keys keys1, keys2;

    /**
     * Creates a new map with an initial capacity of 51 and a load factor of 0.8.
     * 创建一个初始容量为 51、负载因子为 0.8 的新映射。
     */
    public ObjectIntMap(){
        this(51, 0.8f);
    }

    /**
     * Creates a new map with a load factor of 0.8.
     * <p>
     * 创建一个负载因子为 0.8 的新映射。
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two. 底层数组大小为 initialCapacity / loadFactor,并增加到下一个 2 的幂。
     */
    public ObjectIntMap(int initialCapacity){
        this(initialCapacity, 0.8f);
    }

    /**
     * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
     * growing the backing table.
     * <p>
     * 创建一个具有指定初始容量和负载因子的新映射。在底层数组扩容之前,此映射可容纳 initialCapacity 个条目。
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two. 底层数组大小为 initialCapacity / loadFactor,并增加到下一个 2 的幂。
     */
    public ObjectIntMap(int initialCapacity, float loadFactor){
        if(loadFactor <= 0f || loadFactor >= 1f)
            throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
        this.loadFactor = loadFactor;

        int tableSize = tableSize(initialCapacity, loadFactor);
        threshold = (int)(tableSize * loadFactor);
        mask = tableSize - 1;
        shift = Long.numberOfLeadingZeros(mask);

        keyTable = (K[])new Object[tableSize];
        valueTable = new int[tableSize];
    }

    /**
     * Creates a new map identical to the specified map.
     * 创建一个与指定映射完全相同的新映射。
     */
    public ObjectIntMap(ObjectIntMap<? extends K> map){
        this((int)(map.keyTable.length * map.loadFactor), map.loadFactor);
        System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
        System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
        size = map.size;
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
     * hashcodes and don't need Fibonacci hashing: {@code return item.hashCode() & mask;}
     * <p>
     * 为指定的 {@code item} 返回一个 >= 0 且 <= {@link #mask} 的索引。<p> 默认实现对 item 的 {@link Object#hashCode()} 使用斐波那契哈希:哈希码乘以一个 long 常量(2 的 64 次方除以黄金分割比),然后将最高位移入最低位,以获得所需范围内的索引。用 long 相乘可能比 int 慢(例如在 GWT 上),但能大大改进重新哈希,使得即使非常差的哈希码(例如只有高位不同的哈希码)也能在不会产生高冲突率的情况下使用。当全部或大多数哈希码是较大斐波那契数的倍数时,斐波那契哈希的冲突率会升高(见 <a href= "https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/">Malte Skarupke 的博客文章</a>)。<p> 可以重写此方法来自定义哈希。这在以下情况下可能有用:例如绝大多数哈希码恰为斐波那契数这种罕见情况、键提供的哈希码质量差或不正确时,或当键提供的哈希码质量很高、不需要斐波那契哈希从而简化哈希时:{@code return item.hashCode() & mask;}
     */
    protected int place(K item){
        return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
    }

    /**
     * Returns the index of the key if already present, else -(index + 1) for the next empty index. This can be overridden in this
     * pacakge to compare for equality differently than {@link Object#equals(Object)}.
     * <p>
     * 如果键已存在,返回其索引;否则返回 -(index + 1),即下一个空索引。在本包中可以重写此方法,以使用不同于 {@link Object#equals(Object)} 的相等比较。
     */
    int locateKey(K key){
        if(key == null) throw new IllegalArgumentException("key cannot be null.");
        K[] keyTable = this.keyTable;
        for(int i = place(key); ; i = i + 1 & mask){
            K other = keyTable[i];
            if(other == null) return -(i + 1); // Empty space is available.
            // 有空位可用。
            if(other.equals(key)) return i; // Same key was found.
            // 已找到相同的键。
        }
    }

    public void put(K key, int value){
        int i = locateKey(key);
        if(i >= 0){ // Existing key was found.
        // 已找到现有键。
            valueTable[i] = value;
            return;
        }
        i = -(i + 1); // Empty space was found.
        // 已找到空位。
        keyTable[i] = key;
        valueTable[i] = value;
        if(++size >= threshold) resize(keyTable.length << 1);
    }

    /**
     * Returns the old value associated with the specified key, or the specified default value.
     * 返回指定键关联的旧值,或指定的默认值。
     */
    public int put(K key, int value, int defaultValue){
        int i = locateKey(key);
        if(i >= 0){ // Existing key was found.
        // 已找到现有键。
            int oldValue = valueTable[i];
            valueTable[i] = value;
            return oldValue;
        }
        i = -(i + 1); // Empty space was found.
        // 已找到空位。
        keyTable[i] = key;
        valueTable[i] = value;
        if(++size >= threshold) resize(keyTable.length << 1);
        return defaultValue;
    }

    public void putMissing(K key, int value){
        int i = locateKey(key);
        if(i >= 0) return; // Existing key was found.
        // 已找到现有键。
        i = -(i + 1); // Empty space was found.
        // 已找到空位。
        keyTable[i] = key;
        valueTable[i] = value;
        if(++size >= threshold) resize(keyTable.length << 1);
    }

    public int putMissing(K key, int value, int defaultValue){
        int i = locateKey(key);
        if(i >= 0) return valueTable[i]; // Existing key was found.
        // 已找到现有键。
        i = -(i + 1); // Empty space was found.
        // 已找到空位。
        keyTable[i] = key;
        valueTable[i] = value;
        if(++size >= threshold) resize(keyTable.length << 1);
        return defaultValue;
    }

    public void putAll(ObjectIntMap<? extends K> map){
        ensureCapacity(map.size);
        K[] keyTable = map.keyTable;
        int[] valueTable = map.valueTable;
        K key;
        for(int i = 0, n = keyTable.length; i < n; i++){
            key = keyTable[i];
            if(key != null) put(key, valueTable[i]);
        }
    }

    public void putAll(Object... values){
        for(int i = 0; i < values.length / 2; i++){
            put((K)values[i * 2], (int)values[i * 2 + 1]);
        }
    }

    public ObjectIntMap<K> copy(){
        return new ObjectIntMap<>(this);
    }

    public void set(ObjectIntMap<K> value){
        clear();
        putAll(value);
    }


    /**
     * Skips checks for existing keys, doesn't increment size.
     * 跳过对现有键的检查,不增加大小。
     */
    private void putResize(K key, int value){
        K[] keyTable = this.keyTable;
        for(int i = place(key); ; i = (i + 1) & mask){
            if(keyTable[i] == null){
                keyTable[i] = key;
                valueTable[i] = value;
                return;
            }
        }
    }

    public int get(K key){
        return get(key, 0);
    }

    /**
     * Returns the value for the specified key, or the default value if the key is not in the map.
     * 返回指定键对应的值,如果键不在映射中则返回默认值。
     */
    public int get(K key, int defaultValue){
        if(key == null) return defaultValue;
        int i = locateKey(key);
        return i < 0 ? defaultValue : valueTable[i];
    }

    public int increment(K key){
        return increment(key, 0, 1);
    }

    public int increment(K key, int increment){
        return increment(key, 0, increment);
    }

    /**
     * Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue + increment is
     * put into the map and defaultValue is returned.
     * <p>
     * 返回键当前的值并递增存储的值。如果键不在映射中,则将 defaultValue + increment 放入映射并返回 defaultValue。
     */
    public int increment(K key, int defaultValue, int increment){
        int i = locateKey(key);
        if(i >= 0){ // Existing key was found.
        // 已找到现有键。
            int oldValue = valueTable[i];
            valueTable[i] += increment;
            return oldValue;
        }
        i = -(i + 1); // Empty space was found.
        // 已找到空位。
        keyTable[i] = key;
        valueTable[i] = defaultValue + increment;
        if(++size >= threshold) resize(keyTable.length << 1);
        return defaultValue;
    }

    public int remove(K key){
        return remove(key, 0);
    }

    /**
     * Returns the value for the removed key, or the default value if the key is not in the map.
     * 返回被移除键对应的值,如果键不在映射中则返回默认值。
     */
    public int remove(K key, int defaultValue){
        int i = locateKey(key);
        if(i < 0) return defaultValue;
        K[] keyTable = this.keyTable;
        int[] valueTable = this.valueTable;
        int oldValue = valueTable[i];
        int mask = this.mask, next = i + 1 & mask;
        while((key = keyTable[next]) != null){
            int placement = place(key);
            if((next - placement & mask) > (i - placement & mask)){
                keyTable[i] = key;
                valueTable[i] = valueTable[next];
                i = next;
            }
            next = next + 1 & mask;
        }
        keyTable[i] = null;
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
        resize(tableSize);
    }

    public void clear(){
        if(size == 0) return;
        size = 0;
        Arrays.fill(keyTable, null);
    }

    /**
     * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
     * be an expensive operation.
     * <p>
     * 如果指定 value 在映射中,返回 true。注意这会遍历整个映射并比较每个值,可能是昂贵的操作。
     */
    public boolean containsValue(int value){
        K[] keyTable = this.keyTable;
        int[] valueTable = this.valueTable;
        for(int i = valueTable.length - 1; i >= 0; i--)
            if(keyTable[i] != null && valueTable[i] == value) return true;
        return false;
    }

    public boolean containsKey(K key){
        return locateKey(key) >= 0;
    }

    /**
     * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
     * every value, which may be an expensive operation.
     * <p>
     * 返回指定 value 对应的 key,如果它不在映射中则返回 null。注意这会遍历整个映射并比较每个值,可能是昂贵的操作。
     */
    public @Nullable K findKey(int value){
        K[] keyTable = this.keyTable;
        int[] valueTable = this.valueTable;
        for(int i = valueTable.length - 1; i >= 0; i--){
            K key = keyTable[i];
            if(key != null && valueTable[i] == value) return key;
        }
        return null;
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

    final void resize(int newSize){
        int oldCapacity = keyTable.length;
        threshold = (int)(newSize * loadFactor);
        mask = newSize - 1;
        shift = Long.numberOfLeadingZeros(mask);

        K[] oldKeyTable = keyTable;
        int[] oldValueTable = valueTable;

        keyTable = (K[])new Object[newSize];
        valueTable = new int[newSize];

        if(size > 0){
            for(int i = 0; i < oldCapacity; i++){
                K key = oldKeyTable[i];
                if(key != null) putResize(key, oldValueTable[i]);
            }
        }
    }

    public int hashCode(){
        int h = size;
        K[] keyTable = this.keyTable;
        int[] valueTable = this.valueTable;
        for(int i = 0, n = keyTable.length; i < n; i++){
            K key = keyTable[i];
            if(key != null) h += key.hashCode() + valueTable[i];
        }
        return h;
    }

    public boolean equals(Object obj){
        if(obj == this) return true;
        if(!(obj instanceof ObjectIntMap)) return false;
        ObjectIntMap other = (ObjectIntMap)obj;
        if(other.size != size) return false;
        K[] keyTable = this.keyTable;
        int[] valueTable = this.valueTable;
        for(int i = 0, n = keyTable.length; i < n; i++){
            K key = keyTable[i];
            if(key != null){
                int otherValue = other.get(key, 0);
                if(otherValue == 0 && !other.containsKey(key)) return false;
                if(otherValue != valueTable[i]) return false;
            }
        }
        return true;
    }

    public String toString(String separator){
        return toString(separator, false);
    }

    public String toString(){
        return toString(", ", true);
    }

    private String toString(String separator, boolean braces){
        if(size == 0) return braces ? "{}" : "";
        java.lang.StringBuilder buffer = new java.lang.StringBuilder(32);
        if(braces) buffer.append('{');
        K[] keyTable = this.keyTable;
        int[] valueTable = this.valueTable;
        int i = keyTable.length;
        while(i-- > 0){
            K key = keyTable[i];
            if(key == null) continue;
            buffer.append(key);
            buffer.append('=');
            buffer.append(valueTable[i]);
            break;
        }
        while(i-- > 0){
            K key = keyTable[i];
            if(key == null) continue;
            buffer.append(separator);
            buffer.append(key);
            buffer.append('=');
            buffer.append(valueTable[i]);
        }
        if(braces) buffer.append('}');
        return buffer.toString();
    }

    @Override
    public Entries<K> iterator(){
        return entries();
    }

    /**
     * Returns an iterator for the entries in the map. Remove is supported.
     * <p>
     * Use the {@link Entries} constructor for nested or multithreaded iteration.
     * <p>
     * 返回映射中条目的迭代器。支持移除。<p> 嵌套或多线程遍历时,请使用 {@link Entries} 构造函数。
     */
    public Entries<K> entries(){
        if(entries1 == null){
            entries1 = new Entries<>(this);
            entries2 = new Entries<>(this);
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
     * Use the {@link Values} constructor for nested or multithreaded iteration.
     * <p>
     * 返回映射中值的迭代器。支持移除。<p> 嵌套或多线程遍历时,请使用 {@link Values} 构造函数。
     */
    public Values values(){
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
     * Use the {@link Keys} constructor for nested or multithreaded iteration.
     * <p>
     * 返回映射中键的迭代器。支持移除。<p> 嵌套或多线程遍历时,请使用 {@link Keys} 构造函数。
     */
    public Keys<K> keys(){
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

    static public class Entry<K>{
        public K key;
        public int value;

        public String toString(){
            return key + "=" + value;
        }
    }

    static private class MapIterator<K>{
        public boolean hasNext;

        final ObjectIntMap<K> map;
        int nextIndex, currentIndex;
        boolean valid = true;

        public MapIterator(ObjectIntMap<K> map){
            this.map = map;
            reset();
        }

        public void reset(){
            currentIndex = -1;
            nextIndex = -1;
            findNextIndex();
        }

        void findNextIndex(){
            K[] keyTable = map.keyTable;
            for(int n = keyTable.length; ++nextIndex < n; ){
                if(keyTable[nextIndex] != null){
                    hasNext = true;
                    return;
                }
            }
            hasNext = false;
        }

        public void remove(){
            int i = currentIndex;
            if(i < 0) throw new IllegalStateException("next must be called before remove.");
            K[] keyTable = map.keyTable;
            int[] valueTable = map.valueTable;
            int mask = map.mask, next = i + 1 & mask;
            K key;
            while((key = keyTable[next]) != null){
                int placement = map.place(key);
                if((next - placement & mask) > (i - placement & mask)){
                    keyTable[i] = key;
                    valueTable[i] = valueTable[next];
                    i = next;
                }
                next = next + 1 & mask;
            }
            keyTable[i] = null;
            map.size--;
            if(i != currentIndex) --nextIndex;
            currentIndex = -1;
        }
    }

    static public class Entries<K> extends MapIterator<K> implements Iterable<Entry<K>>, Iterator<Entry<K>>{
        Entry<K> entry = new Entry<K>();

        public Entries(ObjectIntMap<K> map){
            super(map);
        }

        /**
         * Note the same entry instance is returned each time this method is called.
         * 注意每次调用此方法都返回相同的 entry 实例。
         */
        @Override
        public Entry<K> next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            K[] keyTable = map.keyTable;
            entry.key = keyTable[nextIndex];
            entry.value = map.valueTable[nextIndex];
            currentIndex = nextIndex;
            findNextIndex();
            return entry;
        }

        @Override
        public boolean hasNext(){
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return hasNext;
        }

        @Override
        public Entries<K> iterator(){
            return this;
        }

        public Ar<Entry<K>> toSeq(){
            Ar<Entry<K>> out = new Ar<>(map.size);
            while(hasNext()){
                Entry<K> entry = next();
                Entry<K> e = new Entry<>();
                e.key = entry.key;
                e.value = entry.value;
                out.add(e);
            }
            return out;
        }
    }

    static public class Values extends MapIterator<Object>{
        public Values(ObjectIntMap<?> map){
            super((ObjectIntMap<Object>)map);
        }

        public boolean hasNext(){
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return hasNext;
        }

        public int next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            int value = map.valueTable[nextIndex];
            currentIndex = nextIndex;
            findNextIndex();
            return value;
        }

        public Values iterator(){
            return this;
        }

        /**
         * Returns a new array containing the remaining values.
         * 返回一个包含剩余值的新数组。
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

    static public class Keys<K> extends MapIterator<K> implements Iterable<K>, Iterator<K>{
        public Keys(ObjectIntMap<K> map){
            super(map);
        }

        @Override
        public boolean hasNext(){
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return hasNext;
        }

        @Override
        public K next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            K key = map.keyTable[nextIndex];
            currentIndex = nextIndex;
            findNextIndex();
            return key;
        }

        @Override
        public Keys<K> iterator(){
            return this;
        }

        /**
         * Returns a new array containing the remaining keys.
         * 返回一个包含剩余键的新数组。
         */
        public Ar<K> toSeq(){
            return toSeq(new Ar<K>(true, map.size));
        }

        /**
         * Adds the remaining keys to the array.
         * 将剩余的键添加到数组。
         */
        public Ar<K> toSeq(Ar<K> array){
            while(hasNext)
                array.add(next());
            return array;
        }
    }
}
