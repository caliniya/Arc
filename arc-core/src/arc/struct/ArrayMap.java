package arc.struct;

import arc.func.*;
import arc.struct.ObjectMap.*;
import arc.math.*;
import arc.util.*;

import java.util.*;

/**
 * An ordered or unordered map of objects. This implementation uses arrays to store the keys and values, which means
 * {@link #getKey(Object, boolean) gets} do a comparison for each key in the map. This is slower than a typical hash map
 * implementation, but may be acceptable for small maps and has the benefits that keys and values can be accessed by index, which
 * makes iteration fast. Like {@link Ar}, if ordered is false, this class avoids a memory copy when removing elements (the last
 * element is moved to the removed element's position).
 * <p>
 * 有序或无序的对象映射。此实现使用数组存储键和值,这意味着 {@link #getKey(Object, boolean) gets} 会对映射中的每个键做一次比较。这比典型的哈希映射实现慢,但对小映射是可以接受的,好处是键和值都可以按索引访问,使遍历很快。与 {@link Ar} 一样,如果 ordered 为 false,该类在移除元素时避免内存复制(将最后一个元素移动到被移除元素的位置)。
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class ArrayMap<K, V> implements Iterable<ObjectMap.Entry<K, V>>{
    private static final Object[] empty = {};

    public K[] keys;
    public V[] values;
    public int size;
    public boolean ordered;

    private Entries entries1, entries2;
    private Values valuesIter1, valuesIter2;
    private Keys keysIter1, keysIter2;

    /**
     * Creates an ordered map with a capacity of 16.
     * 创建一个容量为 16 的有序映射。
     */
    public ArrayMap(){
        this(true, 16);
    }

    /**
     * Creates an ordered map with the specified capacity.
     * 创建一个具有指定容量的有序映射。
     */
    public ArrayMap(int capacity){
        this(true, capacity);
    }

    /**
     * @param ordered If false, methods that remove elements may change the order of other elements in the arrays, which avoids a 如果为 false,移除元素的方法可能改变数组中其他元素的顺序,从而避免
     * memory copy.
     * @param capacity Any elements added beyond this will cause the backing arrays to be grown. 超出此值添加的任何元素都会导致底层数组增长。
     */
    public ArrayMap(boolean ordered, int capacity){
        this.ordered = ordered;
        //optimization: don't allocate anything until it's used
        // 优化:在被使用之前不分配任何东西
        keys = capacity == 0 ?  (K[])empty : (K[])new Object[capacity];
        values = capacity == 0 ?  (V[])empty : (V[])new Object[capacity];
    }

    /**
     * Creates a new map with {@link #keys} and {@link #values} of the specified type.
     * <p>
     * 创建一个具有指定类型的 {@link #keys} 和 {@link #values} 的新映射。
     * @param ordered If false, methods that remove elements may change the order of other elements in the arrays, which avoids a 如果为 false,移除元素的方法可能改变数组中其他元素的顺序,从而避免
     * memory copy.
     * @param capacity Any elements added beyond this will cause the backing arrays to be grown. 超出此值添加的任何元素都会导致底层数组增长。
     */
    public ArrayMap(boolean ordered, int capacity, Class keyArrayType, Class valueArrayType){
        this.ordered = ordered;
        keys = (K[])java.lang.reflect.Array.newInstance(keyArrayType, capacity);
        values = (V[])java.lang.reflect.Array.newInstance(valueArrayType, capacity);
    }

    /**
     * Creates an ordered map with {@link #keys} and {@link #values} of the specified type and a capacity of 16.
     * 创建一个具有指定类型的 {@link #keys} 和 {@link #values}、容量为 16 的有序映射。
     */
    public ArrayMap(Class keyArrayType, Class valueArrayType){
        this(false, 16, keyArrayType, valueArrayType);
    }

    /**
     * Creates a new map containing the elements in the specified map. The new map will have the same type of backing arrays and
     * will be ordered if the specified map is ordered. The capacity is set to the number of elements, so any subsequent elements
     * added will cause the backing arrays to be grown.
     * <p>
     * 创建一个包含指定映射元素的新映射。新映射将具有相同类型的底层数组,且若指定映射有序则新映射也有序。容量被设为元素数量,因此之后添加的任何元素都会导致底层数组增长。
     */
    public ArrayMap(ArrayMap array){
        this(array.ordered, array.size, array.keys.getClass().getComponentType(), array.values.getClass().getComponentType());
        size = array.size;
        System.arraycopy(array.keys, 0, keys, 0, size);
        System.arraycopy(array.values, 0, values, 0, size);
    }

    /**
     * Iterates through key/value pairs.
     * 遍历键/值对。
     */
    public void each(Cons2<K, V> cons){
        for(Entry<K, V> entry : entries()){
            cons.get(entry.key, entry.value);
        }
    }

    public int putIndex(K key, V value){
        int index = indexOfKey(key);
        if(index == -1){
            if(size == keys.length) resize(Math.max(8, (int)(size * 1.75f)));
            index = size++;
        }
        keys[index] = key;
        values[index] = value;
        return index;
    }


    public ArrayMap<K, V> put(K key, V value){
        putIndex(key, value);
        return this;
    }

    public int put(K key, V value, int index){
        int existingIndex = indexOfKey(key);
        if(existingIndex != -1)
            removeIndex(existingIndex);
        else if(size == keys.length) //
            resize(Math.max(8, (int)(size * 1.75f)));
        System.arraycopy(keys, index, keys, index + 1, size - index);
        System.arraycopy(values, index, values, index + 1, size - index);
        keys[index] = key;
        values[index] = value;
        size++;
        return index;
    }

    public void putAll(ArrayMap<? extends K, ? extends V> map){
        putAll(map, 0, map.size);
    }

    public void putAll(ArrayMap<? extends K, ? extends V> map, int offset, int length){
        if(offset + length > map.size)
            throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + map.size);
        int sizeNeeded = size + length - offset;
        if(sizeNeeded >= keys.length) resize(Math.max(8, (int)(sizeNeeded * 1.75f)));
        System.arraycopy(map.keys, offset, keys, size, length);
        System.arraycopy(map.values, offset, values, size, length);
        size += length;
    }

    /**
     * Returns the value for the specified key. Note this does a .equals() comparison of each key in reverse order until the
     * specified key is found.
     * <p>
     * 返回指定键对应的值。注意这会按相反顺序对每个键做 .equals() 比较,直到找到指定的键。
     */
    public V get(K key){
        Object[] keys = this.keys;
        int i = size - 1;
        if(key == null){
            for(; i >= 0; i--)
                if(keys[i] == key) return values[i];
        }else{
            for(; i >= 0; i--)
                if(key.equals(keys[i])) return values[i];
        }
        return null;
    }

    /**
     * Returns the key for the specified value. Note this does a comparison of each value in reverse order until the specified
     * value is found.
     * <p>
     * 返回指定 value 对应的 key。注意这会按相反顺序比较每个值,直到找到指定的 value。
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used. 如果为 true,使用 == 比较;如果为 false,使用 .equals() 比较。
     */
    public K getKey(V value, boolean identity){
        Object[] values = this.values;
        int i = size - 1;
        if(identity || value == null){
            for(; i >= 0; i--)
                if(values[i] == value) return keys[i];
        }else{
            for(; i >= 0; i--)
                if(value.equals(values[i])) return keys[i];
        }
        return null;
    }

    public K getKeyAt(int index){
        if(index >= size) throw new IndexOutOfBoundsException(String.valueOf(index));
        return keys[index];
    }

    public V getValueAt(int index){
        if(index >= size) throw new IndexOutOfBoundsException(String.valueOf(index));
        return values[index];
    }

    public K firstKey(){
        if(size == 0) throw new IllegalStateException("Map is empty.");
        return keys[0];
    }

    public V firstValue(){
        if(size == 0) throw new IllegalStateException("Map is empty.");
        return values[0];
    }

    public void setKey(int index, K key){
        if(index >= size) throw new IndexOutOfBoundsException(String.valueOf(index));
        keys[index] = key;
    }

    public void setValue(int index, V value){
        if(index >= size) throw new IndexOutOfBoundsException(String.valueOf(index));
        values[index] = value;
    }

    public void insert(int index, K key, V value){
        if(index > size) throw new IndexOutOfBoundsException(String.valueOf(index));
        if(size == keys.length) resize(Math.max(8, (int)(size * 1.75f)));
        if(ordered){
            System.arraycopy(keys, index, keys, index + 1, size - index);
            System.arraycopy(values, index, values, index + 1, size - index);
        }else{
            keys[size] = keys[index];
            values[size] = values[index];
        }
        size++;
        keys[index] = key;
        values[index] = value;
    }

    public boolean containsKey(K key){
        K[] keys = this.keys;
        int i = size - 1;
        if(key == null){
            while(i >= 0)
                if(keys[i--] == key) return true;
        }else{
            while(i >= 0)
                if(key.equals(keys[i--])) return true;
        }
        return false;
    }

    /**
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used. 如果为 true,使用 == 比较;如果为 false,使用 .equals() 比较。
     */
    public boolean containsValue(V value, boolean identity){
        V[] values = this.values;
        int i = size - 1;
        if(identity || value == null){
            while(i >= 0)
                if(values[i--] == value) return true;
        }else{
            while(i >= 0)
                if(value.equals(values[i--])) return true;
        }
        return false;
    }

    public int indexOfKey(K key){
        Object[] keys = this.keys;
        if(key == null){
            for(int i = 0, n = size; i < n; i++)
                if(keys[i] == key) return i;
        }else{
            for(int i = 0, n = size; i < n; i++)
                if(key.equals(keys[i])) return i;
        }
        return -1;
    }

    public int indexOfValue(V value, boolean identity){
        Object[] values = this.values;
        if(identity || value == null){
            for(int i = 0, n = size; i < n; i++)
                if(values[i] == value) return i;
        }else{
            for(int i = 0, n = size; i < n; i++)
                if(value.equals(values[i])) return i;
        }
        return -1;
    }

    public V removeKey(K key){
        Object[] keys = this.keys;
        if(key == null){
            for(int i = 0, n = size; i < n; i++){
                if(keys[i] == key){
                    V value = values[i];
                    removeIndex(i);
                    return value;
                }
            }
        }else{
            for(int i = 0, n = size; i < n; i++){
                if(key.equals(keys[i])){
                    V value = values[i];
                    removeIndex(i);
                    return value;
                }
            }
        }
        return null;
    }

    public boolean removeValue(V value, boolean identity){
        Object[] values = this.values;
        if(identity || value == null){
            for(int i = 0, n = size; i < n; i++){
                if(values[i] == value){
                    removeIndex(i);
                    return true;
                }
            }
        }else{
            for(int i = 0, n = size; i < n; i++){
                if(value.equals(values[i])){
                    removeIndex(i);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes and returns the key/values pair at the specified index.
     * 移除并返回指定索引处的键/值对。
     */
    public void removeIndex(int index){
        if(index >= size) throw new IndexOutOfBoundsException(String.valueOf(index));
        Object[] keys = this.keys;
        size--;
        if(ordered){
            System.arraycopy(keys, index + 1, keys, index, size - index);
            System.arraycopy(values, index + 1, values, index, size - index);
        }else{
            keys[index] = keys[size];
            values[index] = values[size];
        }
        keys[size] = null;
        values[size] = null;
    }

    /**
     * Returns true if the map is empty.
     * 如果映射为空,返回 true。
     */
    public boolean isEmpty(){
        return size == 0;
    }

    /**
     * Returns the last key.
     * 返回最后一个键。
     */
    public K peekKey(){
        return keys[size - 1];
    }

    /**
     * Returns the last value.
     * 返回最后一个值。
     */
    public V peekValue(){
        return values[size - 1];
    }

    /**
     * Clears the map and reduces the size of the backing arrays to be the specified capacity if they are larger.
     * 清除映射,并将底层数组的大小缩减为指定容量(如果它们更大的话)。
     */
    public void clear(int maximumCapacity){
        if(keys.length <= maximumCapacity){
            clear();
            return;
        }
        size = 0;
        resize(maximumCapacity);
    }

    public ArrayMap<K, V> clear(){
        K[] keys = this.keys;
        V[] values = this.values;
        for(int i = 0, n = size; i < n; i++){
            keys[i] = null;
            values[i] = null;
        }
        size = 0;
        return this;
    }

    /**
     * Reduces the size of the backing arrays to the size of the actual number of entries. This is useful to release memory when
     * many items have been removed, or if it is known that more entries will not be added.
     * <p>
     * 将底层数组的大小缩减为实际条目数。当已移除大量条目,或确定不会再添加更多条目时,这对释放内存很有用。
     */
    public void shrink(){
        if(keys.length == size) return;
        resize(size);
    }

    /**
     * Increases the size of the backing arrays to accommodate the specified number of additional entries. Useful before adding
     * many entries to avoid multiple backing array resizes.
     * <p>
     * 增大底层数组的大小,以容纳指定数量的额外条目。在添加大量条目之前很有用,可避免多次底层数组扩容。
     */
    public void ensureCapacity(int additionalCapacity){
        if(additionalCapacity < 0)
            throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);
        int sizeNeeded = size + additionalCapacity;
        if(sizeNeeded >= keys.length) resize(Math.max(8, sizeNeeded));
    }

    protected void resize(int newSize){
        K[] newKeys = (K[])java.lang.reflect.Array.newInstance(keys.getClass().getComponentType(), newSize);
        System.arraycopy(keys, 0, newKeys, 0, Math.min(size, newKeys.length));
        this.keys = newKeys;

        V[] newValues = (V[])java.lang.reflect.Array.newInstance(values.getClass().getComponentType(), newSize);
        System.arraycopy(values, 0, newValues, 0, Math.min(size, newValues.length));
        this.values = newValues;
    }

    public void reverse(){
        for(int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++){
            int ii = lastIndex - i;
            K tempKey = keys[i];
            keys[i] = keys[ii];
            keys[ii] = tempKey;

            V tempValue = values[i];
            values[i] = values[ii];
            values[ii] = tempValue;
        }
    }

    public void shuffle(){
        for(int i = size - 1; i >= 0; i--){
            int ii = Mathf.random(i);
            K tempKey = keys[i];
            keys[i] = keys[ii];
            keys[ii] = tempKey;

            V tempValue = values[i];
            values[i] = values[ii];
            values[ii] = tempValue;
        }
    }

    /**
     * Reduces the size of the arrays to the specified size. If the arrays are already smaller than the specified size, no action
     * is taken.
     * <p>
     * 将数组大小缩减到指定大小。如果数组已经小于指定大小,则不执行任何操作。
     */
    public void truncate(int newSize){
        if(size <= newSize) return;
        for(int i = newSize; i < size; i++){
            keys[i] = null;
            values[i] = null;
        }
        size = newSize;
    }

    public int hashCode(){
        K[] keys = this.keys;
        V[] values = this.values;
        int h = 0;
        for(int i = 0, n = size; i < n; i++){
            K key = keys[i];
            V value = values[i];
            if(key != null) h += key.hashCode() * 31;
            if(value != null) h += value.hashCode();
        }
        return h;
    }

    public boolean equals(Object obj){
        if(obj == this) return true;
        if(!(obj instanceof ArrayMap)) return false;
        ArrayMap<K, V> other = (ArrayMap)obj;
        if(other.size != size) return false;
        K[] keys = this.keys;
        V[] values = this.values;
        for(int i = 0, n = size; i < n; i++){
            K key = keys[i];
            V value = values[i];
            if(value == null){
                if(!other.containsKey(key) || other.get(key) != null) return false;
            }else{
                if(!value.equals(other.get(key))) return false;
            }
        }
        return true;
    }

    public String toString(){
        if(size == 0) return "{}";
        K[] keys = this.keys;
        V[] values = this.values;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('{');
        buffer.append(keys[0]);
        buffer.append('=');
        buffer.append(values[0]);
        for(int i = 1; i < size; i++){
            buffer.append(", ");
            buffer.append(keys[i]);
            buffer.append('=');
            buffer.append(values[i]);
        }
        buffer.append('}');
        return buffer.toString();
    }

    public Iterator<Entry<K, V>> iterator(){
        return entries();
    }

    /**
     * Returns an iterator for the entries in the map. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the {@link Entries} constructor for nested or multithreaded iteration.
     * <p>
     * 返回映射中条目的迭代器。支持移除。注意每次调用此方法都会返回相同的迭代器实例。嵌套或多线程遍历时,请使用 {@link Entries} 构造函数。
     */
    public Entries<K, V> entries(){
        if(entries1 == null){
            entries1 = new Entries(this);
            entries2 = new Entries(this);
        }
        if(!entries1.valid){
            entries1.index = 0;
            entries1.valid = true;
            entries2.valid = false;
            return entries1;
        }
        entries2.index = 0;
        entries2.valid = true;
        entries1.valid = false;
        return entries2;
    }

    /**
     * Returns an iterator for the values in the map. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the {@link Entries} constructor for nested or multithreaded iteration.
     * <p>
     * 返回映射中值的迭代器。支持移除。注意每次调用此方法都会返回相同的迭代器实例。嵌套或多线程遍历时,请使用 {@link Entries} 构造函数。
     */
    public Values<V> values(){
        if(valuesIter1 == null){
            valuesIter1 = new Values(this);
            valuesIter2 = new Values(this);
        }
        if(!valuesIter1.valid){
            valuesIter1.index = 0;
            valuesIter1.valid = true;
            valuesIter2.valid = false;
            return valuesIter1;
        }
        valuesIter2.index = 0;
        valuesIter2.valid = true;
        valuesIter1.valid = false;
        return valuesIter2;
    }

    /**
     * Returns an iterator for the keys in the map. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the {@link Entries} constructor for nested or multithreaded iteration.
     * <p>
     * 返回映射中键的迭代器。支持移除。注意每次调用此方法都会返回相同的迭代器实例。嵌套或多线程遍历时,请使用 {@link Entries} 构造函数。
     */
    public Keys<K> keys(){
        if(keysIter1 == null){
            keysIter1 = new Keys(this);
            keysIter2 = new Keys(this);
        }
        if(!keysIter1.valid){
            keysIter1.index = 0;
            keysIter1.valid = true;
            keysIter2.valid = false;
            return keysIter1;
        }
        keysIter2.index = 0;
        keysIter2.valid = true;
        keysIter1.valid = false;
        return keysIter2;
    }

    public static class Entries<K, V> implements Iterable<Entry<K, V>>, Iterator<Entry<K, V>>{
        private final ArrayMap<K, V> map;
        Entry<K, V> entry = new Entry();
        int index;
        boolean valid = true;

        public Entries(ArrayMap<K, V> map){
            this.map = map;
        }

        public boolean hasNext(){
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return index < map.size;
        }

        public Iterator<Entry<K, V>> iterator(){
            return this;
        }

        /**
         * Note the same entry instance is returned each time this method is called.
         * 注意每次调用此方法都返回相同的 entry 实例。
         */
        public Entry<K, V> next(){
            if(index >= map.size) throw new NoSuchElementException(String.valueOf(index));
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            entry.key = map.keys[index];
            entry.value = map.values[index++];
            return entry;
        }

        public void remove(){
            index--;
            map.removeIndex(index);
        }

        public void reset(){
            index = 0;
        }
    }

    public static class Values<V> implements Iterable<V>, Iterator<V>{
        private final ArrayMap<Object, V> map;
        int index;
        boolean valid = true;

        public Values(ArrayMap<Object, V> map){
            this.map = map;
        }

        public boolean hasNext(){
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return index < map.size;
        }

        public Iterator<V> iterator(){
            return this;
        }

        public V next(){
            if(index >= map.size) throw new NoSuchElementException(String.valueOf(index));
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return map.values[index++];
        }

        public void remove(){
            index--;
            map.removeIndex(index);
        }

        public void reset(){
            index = 0;
        }

        public Ar<V> toSeq(){
            return new Ar(true, map.values, index, map.size - index);
        }

        public Ar<V> toSeq(Ar array){
            array.addAll(map.values, index, map.size - index);
            return array;
        }
    }

    public static class Keys<K> implements Iterable<K>, Iterator<K>{
        private final ArrayMap<K, Object> map;
        int index;
        boolean valid = true;

        public Keys(ArrayMap<K, Object> map){
            this.map = map;
        }

        public boolean hasNext(){
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return index < map.size;
        }

        public Iterator<K> iterator(){
            return this;
        }

        public K next(){
            if(index >= map.size) throw new NoSuchElementException(String.valueOf(index));
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return map.keys[index++];
        }

        public void remove(){
            index--;
            map.removeIndex(index);
        }

        public void reset(){
            index = 0;
        }

        public Ar<K> toSeq(){
            return new Ar(true, map.keys, index, map.size - index);
        }

        public Ar<K> toSeq(Ar array){
            array.addAll(map.keys, index, map.size - index);
            return array;
        }
    }
}
