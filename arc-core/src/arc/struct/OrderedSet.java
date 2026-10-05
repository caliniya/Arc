package arc.struct;

import arc.util.*;

import java.util.*;

/**
 * An {@link ObjectSet} that also stores keys in an {@link Ar} using the insertion order. {@link #iterator() Iteration} is
 * ordered and faster than an unordered set. Keys can also be accessed and the order changed using {@link #orderedItems()}. There
 * is some additional overhead for put and remove. When used for faster iteration versus ObjectSet and the order does not actually
 * matter, copying during remove can be greatly reduced by setting {@link Ar#ordered} to false for
 * {@link OrderedSet#orderedItems()}.
 * <p>
 * 一个 {@link ObjectSet},还会按插入顺序将键存储在 {@link Ar} 中。{@link #iterator() Iteration} 是有序的,且比无序集合更快。还可以使用 {@link #orderedItems()} 访问键并改变顺序。put 和 remove 有一些额外的开销。当为了更快遍历而用它代替 ObjectSet 且顺序并不重要时,可通过将 {@link OrderedSet#orderedItems()} 的 {@link Ar#ordered} 设为 false 来大幅减少移除时的复制。
 * @author Nathan Sweet
 */

public class OrderedSet<T> extends ObjectSet<T>{
    final Ar<T> items;
    transient OrderedSetIterator iterator1, iterator2;

    public OrderedSet(){
        items = new Ar();
    }

    public OrderedSet(int initialCapacity, float loadFactor){
        super(initialCapacity, loadFactor);
        items = new Ar(initialCapacity);
    }

    public OrderedSet(int initialCapacity){
        super(initialCapacity);
        items = new Ar(initialCapacity);
    }

    public OrderedSet(OrderedSet<? extends T> set){
        super(set);
        items = new Ar(set.items);
    }

    public boolean add(T key){
        if(!super.add(key)) return false;
        items.add(key);
        return true;
    }

    /**
     * Sets the key at the specfied index. Returns true if the key was added to the set or false if it was already in the set. If
     * this set already contains the key, the existing key's index is changed if needed and false is returned.
     * <p>
     * 设置指定索引处的键。如果键被添加到集合中则返回 true,如果它已在集合中则返回 false。如果此集合已包含该键,现有键的索引会在需要时被更改,并返回 false。
     */
    public boolean add(T key, int index){
        if(!super.add(key)){
            int oldIndex = items.indexOf(key, true);
            if(oldIndex != index) items.insert(index, items.remove(oldIndex));
            return false;
        }
        items.insert(index, key);
        return true;
    }

    public void addAll(OrderedSet<T> set){
        ensureCapacity(set.size);
        T[] keys = set.items.items;
        for(int i = 0, n = set.items.size; i < n; i++)
            add(keys[i]);
    }

    public void ensureCapacity(int additionalCapacity){
        super.ensureCapacity(additionalCapacity);
        items.ensureCapacity(additionalCapacity);
    }

    public boolean remove(T key){
        if(!super.remove(key)) return false;
        items.remove(key, false);
        return true;
    }

    public T removeIndex(int index){
        T key = items.remove(index);
        super.remove(key);
        return key;
    }

    /**
     * Changes the item {@code before} to {@code after} without changing its position in the order. Returns true if {@code after}
     * has been added to the OrderedSet and {@code before} has been removed; returns false if {@code after} is already present or
     * {@code before} is not present. If you are iterating over an OrderedSet and have an index, you should prefer
     * {@link #alterIndex(int, Object)}, which doesn't need to search for an index like this does and so can be faster.
     * <p>
     * 将项 {@code before} 更改为 {@code after},而不改变其在顺序中的位置。如果 {@code after} 已被添加到 OrderedSet 且 {@code before} 已被移除,返回 true;如果 {@code after} 已存在或 {@code before} 不存在,返回 false。如果你正在遍历 OrderedSet 并且已有索引,应优先使用 {@link #alterIndex(int, Object)},它无需像此方法那样搜索索引,因此更快。
     * @param before an item that must be present for this to succeed 一个必须存在的项,此操作才能成功
     * @param after an item that must not be in this set for this to succeed 一个必须不在此集合中的项,此操作才能成功
     * @return true if {@code before} was removed and {@code after} was added, false otherwise 如果 {@code before} 被移除且 {@code after} 被添加则为 true,否则为 false
     */
    public boolean alter(T before, T after){
        if(contains(after)) return false;
        if(!super.remove(before)) return false;
        super.add(after);
        items.set(items.indexOf(before, false), after);
        return true;
    }

    /**
     * Changes the item at the given {@code index} in the order to {@code after}, without changing the ordering of other items. If
     * {@code after} is already present, this returns false; it will also return false if {@code index} is invalid for the size of
     * this set. Otherwise, it returns true. Unlike {@link #alter(Object, Object)}, this operates in constant time.
     * <p>
     * 将顺序中给定 {@code index} 处的项更改为 {@code after},而不改变其他项的顺序。如果 {@code after} 已存在,返回 false;如果 {@code index} 对此集合的大小无效,也返回 false。否则返回 true。与 {@link #alter(Object, Object)} 不同,此操作耗时为常数。
     * @param index the index in the order of the item to change; must be non-negative and less than {@link #size} 要更改的项在顺序中的索引;必须非负且小于 {@link #size}
     * @param after the item that will replace the contents at {@code index}; this item must not be present for this to succeed 将替换 {@code index} 处内容的项;该项必须不存在,此操作才能成功
     * @return true if {@code after} successfully replaced the contents at {@code index}, false otherwise 如果 {@code after} 成功替换了 {@code index} 处的内容则为 true,否则为 false
     */
    public boolean alterIndex(int index, T after){
        if(index < 0 || index >= size || contains(after)) return false;
        super.remove(items.get(index));
        super.add(after);
        items.set(index, after);
        return true;
    }

    public void clear(int maximumCapacity){
        items.clear();
        super.clear(maximumCapacity);
    }

    public void clear(){
        items.clear();
        super.clear();
    }

    public Ar<T> orderedItems(){
        return items;
    }

    public T first(){
        return items.first();
    }

    public int hashCode(){
        int h = size;
        T[] items = this.items.items;
        for(int i = 0, n = this.items.size; i < n; i++){
            T key = items[i];
            if(key != null) h += key.hashCode();
        }
        return h;
    }

    public boolean equals(Object obj){
        if(!(obj instanceof ObjectSet)) return false;
        ObjectSet other = (ObjectSet)obj;
        if(other.size != size) return false;
        T[] items = this.items.items;
        for(int i = 0, n = this.items.size; i < n; i++)
            if(items[i] != null && !other.contains(items[i])) return false;
        return true;
    }

    public OrderedSetIterator<T> iterator(){
        if(iterator1 == null){
            iterator1 = new OrderedSetIterator(this);
            iterator2 = new OrderedSetIterator(this);
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

    public String toString(){
        if(size == 0) return "{}";
        T[] items = this.items.items;
        java.lang.StringBuilder buffer = new java.lang.StringBuilder(32);
        buffer.append('{');
        buffer.append(items[0]);
        for(int i = 1; i < size; i++){
            buffer.append(", ");
            buffer.append(items[i]);
        }
        buffer.append('}');
        return buffer.toString();
    }

    public String toString(String separator){
        return items.toString(separator);
    }

    static public class OrderedSetIterator<K> extends ObjectSetIterator<K>{
        private Ar<K> items;

        public OrderedSetIterator(OrderedSet<K> set){
            super(set);
            items = set.items;
        }

        public void reset(){
            nextIndex = 0;
            hasNext = set.size > 0;
        }

        public K next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            K key = items.get(nextIndex);
            nextIndex++;
            hasNext = nextIndex < set.size;
            return key;
        }

        public void remove(){
            if(nextIndex < 0) throw new IllegalStateException("next must be called before remove.");
            nextIndex--;
            ((OrderedSet)set).removeIndex(nextIndex);
        }

        public Ar<K> toSeq(Ar<K> array){
            array.addAll(items, nextIndex, items.size - nextIndex);
            nextIndex = items.size;
            hasNext = false;
            return array;
        }

        public Ar<K> toSeq(){
            return toSeq(new Ar(true, set.size - nextIndex));
        }
    }

    static public <T> OrderedSet<T> with(T... array){
        OrderedSet<T> set = new OrderedSet<T>();
        set.addAll(array);
        return set;
    }
}
