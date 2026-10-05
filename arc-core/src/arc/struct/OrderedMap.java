package arc.struct;

import arc.util.*;

import java.util.*;

/**
 * An {@link ObjectMap} that also stores keys in an {@link Ar} using the insertion order. Iteration over the
 * {@link #entries()}, {@link #keys()}, and {@link #values()} is ordered and faster than an unordered map. Keys can also be
 * accessed and the order changed using {@link #orderedKeys()}. There is some additional overhead for put and remove. When used
 * for faster iteration versus ObjectMap and the order does not actually matter, copying during remove can be greatly reduced by
 * setting {@link Ar#ordered} to false for {@link OrderedMap#orderedKeys()}.
 * <p>
 * 一个 {@link ObjectMap},还会按插入顺序将键存储在 {@link Ar} 中。对 {@link #entries()}、{@link #keys()} 和 {@link #values()} 的遍历是有序的,且比无序映射更快。还可以使用 {@link #orderedKeys()} 访问键并改变顺序。put 和 remove 有一些额外的开销。当为了更快遍历而用它代替 ObjectMap 且顺序并不重要时,可通过将 {@link OrderedMap#orderedKeys()} 的 {@link Ar#ordered} 设为 false 来大幅减少移除时的复制。
 * @author Nathan Sweet
 */
public class OrderedMap<K, V> extends ObjectMap<K, V>{
    final Ar<K> keys;

    public static <K, V> OrderedMap<K, V> of(Object... values){
        OrderedMap<K, V> map = new OrderedMap<>();

        for(int i = 0; i < values.length / 2; i++){
            map.put((K)values[i * 2], (V)values[i * 2 + 1]);
        }

        return map;
    }

    /**
     * Creates a new map with an initial capacity of 51 and a load factor of 0.8.
     * 创建一个初始容量为 51、负载因子为 0.8 的新映射。
     */
    public OrderedMap(){
        keys = new Ar<>();
    }

    /**
     * Creates a new map with a load factor of 0.8.
     * <p>
     * 创建一个负载因子为 0.8 的新映射。
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two. 底层数组大小为 initialCapacity / loadFactor,并增加到下一个 2 的幂。
     */
    public OrderedMap(int initialCapacity){
        super(initialCapacity);
        keys = new Ar<>(initialCapacity);
    }

    /**
     * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
     * growing the backing table.
     * <p>
     * 创建一个具有指定初始容量和负载因子的新映射。在底层数组扩容之前,此映射可容纳 initialCapacity 个条目。
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two. 底层数组大小为 initialCapacity / loadFactor,并增加到下一个 2 的幂。
     */
    public OrderedMap(int initialCapacity, float loadFactor){
        super(initialCapacity, loadFactor);
        keys = new Ar<>(initialCapacity);
    }

    /**
     * Creates a new map containing the items in the specified map.
     * 创建一个包含指定映射条目的新映射。
     */
    public OrderedMap(OrderedMap<? extends K, ? extends V> map){
        super(map);
        keys = new Ar<>(map.keys);
    }

    @Override
    public V put(K key, V value){
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
        keys.add(key);
        if(++size >= threshold) resize(keyTable.length << 1);
        return null;
    }

    @Override
    public @Nullable V putMissing(K key, @Nullable V value){
        int i = locateKey(key);
        if(i >= 0) return valueTable[i]; // Existing key was found.
        // 已找到现有键。
        i = -(i + 1); // Empty space was found.
        // 已找到空位。
        keyTable[i] = key;
        valueTable[i] = value;
        keys.add(key);
        if(++size >= threshold) resize(keyTable.length << 1);
        return null;
    }

    public <T extends K> void putAll(OrderedMap<T, ? extends V> map){
        ensureCapacity(map.size);
        K[] keys = map.keys.items;
        for(int i = 0, n = map.keys.size; i < n; i++){
            K key = keys[i];
            put(key, map.get((T)key));
        }
    }

    @Override
    public V remove(K key){
        keys.remove(key, false);
        return super.remove(key);
    }

    public V removeIndex(int index){
        return super.remove(keys.remove(index));
    }

    /**
     * Changes the key {@code before} to {@code after} without changing its position in the order or its value. Returns true if
     * {@code after} has been added to the OrderedMap and {@code before} has been removed; returns false if {@code after} is
     * already present or {@code before} is not present. If you are iterating over an OrderedMap and have an index, you should
     * prefer {@link #alterIndex(int, Object)}, which doesn't need to search for an index like this does and so can be faster.
     * <p>
     * 将键 {@code before} 更改为 {@code after},而不改变其在顺序中的位置或其值。如果 {@code after} 已被添加到 OrderedMap 且 {@code before} 已被移除,返回 true;如果 {@code after} 已存在或 {@code before} 不存在,返回 false。如果你正在遍历 OrderedMap 并且已有索引,应优先使用 {@link #alterIndex(int, Object)},它无需像此方法那样搜索索引,因此更快。
     * @param before a key that must be present for this to succeed 一个必须存在的键,此操作才能成功
     * @param after a key that must not be in this map for this to succeed 一个必须不在此映射中的键,此操作才能成功
     * @return true if {@code before} was removed and {@code after} was added, false otherwise 如果 {@code before} 被移除且 {@code after} 被添加则为 true,否则为 false
     */
    public boolean alter(K before, K after){
        if(containsKey(after)) return false;
        int index = keys.indexOf(before, false);
        if(index == -1) return false;
        super.put(after, super.remove(before));
        keys.set(index, after);
        return true;
    }

    /**
     * Changes the key at the given {@code index} in the order to {@code after}, without changing the ordering of other entries or
     * any values. If {@code after} is already present, this returns false; it will also return false if {@code index} is invalid
     * for the size of this map. Otherwise, it returns true. Unlike {@link #alter(Object, Object)}, this operates in constant time.
     * <p>
     * 将顺序中给定 {@code index} 处的键更改为 {@code after},而不改变其他条目的顺序或任何值。如果 {@code after} 已存在,返回 false;如果 {@code index} 对此映射的大小无效,也返回 false。否则返回 true。与 {@link #alter(Object, Object)} 不同,此操作耗时为常数。
     * @param index the index in the order of the key to change; must be non-negative and less than {@link #size} 要更改的键在顺序中的索引;必须非负且小于 {@link #size}
     * @param after the key that will replace the contents at {@code index}; this key must not be present for this to succeed 将替换 {@code index} 处内容的键;该键必须不存在,此操作才能成功
     * @return true if {@code after} successfully replaced the key at {@code index}, false otherwise 如果 {@code after} 成功替换了 {@code index} 处的键则为 true,否则为 false
     */
    public boolean alterIndex(int index, K after){
        if(index < 0 || index >= size || containsKey(after)) return false;
        super.put(after, super.remove(keys.get(index)));
        keys.set(index, after);
        return true;
    }

    @Override
    public void clear(int maximumCapacity){
        keys.clear();
        super.clear(maximumCapacity);
    }

    @Override
    public void clear(){
        keys.clear();
        super.clear();
    }

    public Ar<K> orderedKeys(){
        return keys;
    }

    @Override
    public Entries<K, V> iterator(){
        return entries();
    }

    /**
     * Returns an iterator for the entries in the map. Remove is supported.
     * <p>
     * Use the {@link OrderedMapEntries} constructor for nested or multithreaded iteration.
     * <p>
     * 返回映射中条目的迭代器。支持移除。<p> 嵌套或多线程遍历时,请使用 {@link OrderedMapEntries} 构造函数。
     */
    @Override
    public Entries<K, V> entries(){
        if(entries1 == null){
            entries1 = new OrderedMapEntries<>(this);
            entries2 = new OrderedMapEntries<>(this);
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
     * Use the {@link OrderedMapValues} constructor for nested or multithreaded iteration.
     * <p>
     * 返回映射中值的迭代器。支持移除。<p> 嵌套或多线程遍历时,请使用 {@link OrderedMapValues} 构造函数。
     */
    @Override
    public Values<V> values(){
        if(values1 == null){
            values1 = new OrderedMapValues<>(this);
            values2 = new OrderedMapValues<>(this);
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
     * Use the {@link OrderedMapKeys} constructor for nested or multithreaded iteration.
     * <p>
     * 返回映射中键的迭代器。支持移除。<p> 嵌套或多线程遍历时,请使用 {@link OrderedMapKeys} 构造函数。
     */
    @Override
    public Keys<K> keys(){
        if(keys1 == null){
            keys1 = new OrderedMapKeys<>(this);
            keys2 = new OrderedMapKeys<>(this);
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

    @Override
    public String toString(String separator, boolean braces){
        if(size == 0) return braces ? "{}" : "";
        StringBuilder buffer = new StringBuilder(32);
        if(braces) buffer.append('{');
        Ar<K> keys = this.keys;
        for(int i = 0, n = keys.size; i < n; i++){
            K key = keys.get(i);
            if(i > 0) buffer.append(separator);
            buffer.append(key == this ? "(this)" : key);
            buffer.append('=');
            V value = get(key);
            buffer.append(value == this ? "(this)" : value);
        }
        if(braces) buffer.append('}');
        return buffer.toString();
    }

    public static class OrderedMapEntries<K, V> extends Entries<K, V>{
        private Ar<K> keys;

        public OrderedMapEntries(OrderedMap<K, V> map){
            super(map);
            keys = map.keys;
        }

        @Override
        public void reset(){
            currentIndex = -1;
            nextIndex = 0;
            hasNext = map.size > 0;
        }

        @Override
        public Entry<K, V> next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            currentIndex = nextIndex;
            entry.key = keys.get(nextIndex);
            entry.value = map.get(entry.key);
            nextIndex++;
            hasNext = nextIndex < map.size;
            return entry;
        }

        @Override
        public void remove(){
            if(currentIndex < 0) throw new IllegalStateException("next must be called before remove.");
            map.remove(entry.key);
            nextIndex--;
            currentIndex = -1;
        }
    }

    public static class OrderedMapKeys<K> extends Keys<K>{
        private Ar<K> keys;

        public OrderedMapKeys(OrderedMap<K, ?> map){
            super(map);
            keys = map.keys;
        }

        @Override
        public void reset(){
            currentIndex = -1;
            nextIndex = 0;
            hasNext = map.size > 0;
        }

        @Override
        public K next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            K key = keys.get(nextIndex);
            currentIndex = nextIndex;
            nextIndex++;
            hasNext = nextIndex < map.size;
            return key;
        }

        @Override
        public void remove(){
            if(currentIndex < 0) throw new IllegalStateException("next must be called before remove.");
            ((OrderedMap)map).removeIndex(currentIndex);
            nextIndex = currentIndex;
            currentIndex = -1;
        }

        @Override
        public Ar<K> toSeq(Ar<K> array){
            array.addAll(keys, nextIndex, keys.size - nextIndex);
            nextIndex = keys.size;
            hasNext = false;
            return array;
        }

        @Override
        public Ar<K> toSeq(){
            return toSeq(new Ar<>(true, keys.size - nextIndex));
        }
    }

    public static class OrderedMapValues<V> extends Values<V>{
        private Ar<V> keys;

        public OrderedMapValues(OrderedMap<?, V> map){
            super(map);
            keys = (Ar<V>)map.keys;
        }

        @Override
        public void reset(){
            currentIndex = -1;
            nextIndex = 0;
            hasNext = map.size > 0;
        }

        @Override
        public V next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            V value = map.get(keys.get(nextIndex));
            currentIndex = nextIndex;
            nextIndex++;
            hasNext = nextIndex < map.size;
            return value;
        }

        @Override
        public void remove(){
            if(currentIndex < 0) throw new IllegalStateException("next must be called before remove.");
            ((OrderedMap)map).removeIndex(currentIndex);
            nextIndex = currentIndex;
            currentIndex = -1;
        }

        @Override
        public Ar<V> toSeq(Ar<V> array){
            int n = keys.size;
            array.ensureCapacity(n - nextIndex);
            Object[] keys = this.keys.items;
            for(int i = nextIndex; i < n; i++)
                array.add(map.get(keys[i]));
            currentIndex = n - 1;
            nextIndex = n;
            hasNext = false;
            return array;
        }

        @Override
        public Ar<V> toSeq(){
            return toSeq(new Ar<>(true, keys.size - nextIndex));
        }
    }
}
