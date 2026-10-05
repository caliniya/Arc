package arc.struct;

import arc.func.*;
import arc.util.*;

import java.util.*;

/**
 * A resizable, ordered array of objects with efficient add and remove at the beginning and end. Values in the backing array may
 * wrap back to the beginning, making add and remove at the beginning and end O(1) (unless the backing array needs to resize when
 * adding). Deque functionality is provided via {@link #removeLast()} and {@link #addFirst(Object)}.
 * <p>
 * 一种可调整大小的有序对象数组,在开头和结尾的添加与删除都很高效。底层数组中的值可以回绕到开头,使得开头和结尾的添加与删除为 O(1)(除非添加时底层数组需要扩容)。双端队列功能通过 {@link #removeLast()} 和 {@link #addFirst(Object)} 提供。
 */
@SuppressWarnings("unchecked")
public class Queue<T> implements Iterable<T>, Eachable<T>{
    /**
     * Number of elements in the queue.
     * 队列中的元素数量。
     */
    public int size = 0;
    /**
     * Contains the values in the queue. Head and tail indices go in a circle around this array, wrapping at the end.
     * 包含队列中的值。头和尾索引围绕此数组循环,在末尾处回绕。
     */
    public T[] values;
    /**
     * Index of first element. Logically smaller than tail. Unless empty, it points to a valid element inside queue.
     * 第一个元素的索引。逻辑上比 tail 小。除非队列为空,它指向队列中的一个有效元素。
     */
    protected int head = 0;
    /**
     * Index of last element. Logically bigger than head. Usually points to an empty position, but points to the head when full
     * (size == values.length).
     * <p>
     * 最后一个元素的索引。逻辑上比 head 大。通常指向一个空位,但在满时(size == values.length)指向 head。
     */
    protected int tail = 0;
    private @Nullable QueueIterable<T> iterable;

    /**
     * Creates a new Queue which can hold 16 values without needing to resize backing array.
     * 创建一个新的 Queue,可容纳 16 个值而无需扩容底层数组。
     */
    public Queue(){
        this(16);
    }

    /**
     * Creates a new Queue which can hold the specified number of values without needing to resize backing array.
     * 创建一个新的 Queue,可容纳指定数量的值而无需扩容底层数组。
     */
    public Queue(int initialSize){
        // noinspection unchecked
        this.values = (T[])new Object[initialSize];
    }

    /**
     * Creates a new Queue which can hold the specified number of values without needing to resize backing array. This creates
     * backing array of the specified type via reflection, which is necessary only when accessing the backing array directly.
     * <p>
     * 创建一个新的 Queue,可容纳指定数量的值而无需扩容底层数组。这会通过反射创建指定类型的底层数组,仅在需要直接访问底层数组时才有必要。
     */
    public Queue(int initialSize, Class<T> type){
        this.values = (T[])java.lang.reflect.Array.newInstance(type, initialSize);
    }

    public T[] toArray(Class<T> type){
        T[] out = (T[])java.lang.reflect.Array.newInstance(type, size);
        for(int i = 0; i < size; i++){
            out[i] = get(i);
        }
        return out;
    }

    /**
     * Append given object to the tail. (enqueue to tail) Unless backing array needs resizing, operates in O(1) time.
     * <p>
     * 将给定对象追加到尾部。(入队到尾部)除非底层数组需要扩容,否则操作时间为 O(1)。
     * @param object can be null 可以为 null
     */
    public void addLast(T object){
        T[] values = this.values;

        if(size == values.length){
            resize(values.length << 1);// * 2
            values = this.values;
        }

        values[tail++] = object;
        if(tail == values.length){
            tail = 0;
        }
        size++;
    }

    /**
     * Adds an object to the tail.
     * 将对象添加到尾部。
     */
    public void add(T object){
        addLast(object);
    }

    /**
     * Prepend given object to the head. (enqueue to head) Unless backing array needs resizing, operates in O(1) time.
     * <p>
     * 将给定对象前插到头部。(入队到头部)除非底层数组需要扩容,否则操作时间为 O(1)。
     * @param object can be null 可以为 null
     * @see #addLast(Object)
     */
    public void addFirst(T object){
        T[] values = this.values;

        if(size == values.length){
            resize(values.length << 1);// * 2
            values = this.values;
        }

        int head = this.head;
        head--;
        if(head == -1){
            head = values.length - 1;
        }
        values[head] = object;

        this.head = head;
        this.size++;
    }

    /**
     * Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
     * have been removed, or if it is known that more items will not be added.
     * <p>
     * 将底层数组的大小缩减为实际条目数。当已移除大量条目,或确定不会再添加更多条目时,这对释放内存很有用。
     * @return {@link #values}
     */
    public T[] shrink(){
        if(values.length != size) resize(size);
        return values;
    }

    /**
     * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing array resizes.
     * <p>
     * 增大底层数组的大小,以容纳指定数量的额外条目。在添加大量条目之前很有用,可避免多次底层数组扩容。
     */
    public void ensureCapacity(int additional){
        final int needed = size + additional;
        if(values.length < needed){
            resize(needed);
        }
    }

    /**
     * Resize backing array. newSize must be bigger than current size.
     * 扩容底层数组。newSize 必须大于当前大小。
     */
    protected void resize(int newSize){
        final T[] values = this.values;
        final int head = this.head;
        final int tail = this.tail;

        final T[] newArray = (T[])java.lang.reflect.Array.newInstance(values.getClass().getComponentType(), newSize);
        if(head < tail){
            // Continuous
            // 连续
            System.arraycopy(values, head, newArray, 0, tail - head);
        }else if(size > 0){
            // Wrapped
            // 已回绕
            final int rest = values.length - head;
            System.arraycopy(values, head, newArray, 0, rest);
            System.arraycopy(values, 0, newArray, rest, tail);
        }
        this.values = newArray;
        this.head = 0;
        this.tail = size;
    }

    /**
     * Remove the first item from the queue. (dequeue from head) Always O(1).
     * <p>
     * 从队列中移除第一个条目。(从头部出队)始终 O(1)。
     * @return removed object 被移除的对象
     * @throws NoSuchElementException when queue is empty 当队列为空时
     */
    public T removeFirst(){
        if(size == 0){
            // Underflow
            // 下溢
            throw new NoSuchElementException("Queue is empty.");
        }

        final T[] values = this.values;

        final T result = values[head];
        values[head] = null;
        head++;
        if(head == values.length){
            head = 0;
        }
        size--;

        return result;
    }

    /**
     * Remove the last item from the queue. (dequeue from tail) Always O(1).
     * <p>
     * 从队列中移除最后一个条目。(从尾部出队)始终 O(1)。
     * @return removed object 被移除的对象
     * @throws NoSuchElementException when queue is empty 当队列为空时
     * @see #removeFirst()
     */
    public T removeLast(){
        if(size == 0){
            throw new NoSuchElementException("Queue is empty.");
        }

        final T[] values = this.values;
        int tail = this.tail;
        tail--;
        if(tail == -1){
            tail = values.length - 1;
        }
        final T result = values[tail];
        values[tail] = null;
        this.tail = tail;
        size--;

        return result;
    }

    public boolean contains(T value){
        return contains(value, true);
    }

    public boolean contains(T value, boolean identity){
        return indexOf(value, identity) != -1;
    }

    /**
     * Returns the index of first occurrence of value in the queue, or -1 if no such value exists.
     * <p>
     * 返回 value 在队列中第一次出现的索引,如果不存在这样的值则返回 -1。
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used. 如果为 true,使用 == 比较;如果为 false,使用 .equals() 比较。
     * @return An index of first occurrence of value in queue or -1 if no such value exists value 在队列中第一次出现的索引,如果不存在则为 -1
     */
    public int indexOf(T value, boolean identity){
        if(size == 0) return -1;
        T[] values = this.values;
        final int head = this.head, tail = this.tail;
        if(identity || value == null){
            if(head < tail){
                for(int i = head; i < tail; i++)
                    if(values[i] == value) return i - head;
            }else{
                for(int i = head, n = values.length; i < n; i++)
                    if(values[i] == value) return i - head;
                for(int i = 0; i < tail; i++)
                    if(values[i] == value) return i + values.length - head;
            }
        }else{
            if(head < tail){
                for(int i = head; i < tail; i++)
                    if(value.equals(values[i])) return i - head;
            }else{
                for(int i = head, n = values.length; i < n; i++)
                    if(value.equals(values[i])) return i - head;
                for(int i = 0; i < tail; i++)
                    if(value.equals(values[i])) return i + values.length - head;
            }
        }
        return -1;
    }

    public int indexOf(Boolf<T> value){
        if(size == 0) return -1;
        T[] values = this.values;
        final int head = this.head, tail = this.tail;
        if(head < tail){
            for(int i = head; i < tail; i++)
                if(value.get(values[i])) return i - head;
        }else{
            for(int i = head, n = values.length; i < n; i++)
                if(value.get(values[i])) return i - head;
            for(int i = 0; i < tail; i++)
                if(value.get(values[i])) return i + values.length - head;
        }
        return -1;
    }

    public boolean remove(Boolf<T> value){
        int i = indexOf(value);
        if(i != -1){
            removeIndex(i);
            return true;
        }
        return false;
    }

    public boolean remove(T value){
        return remove(value, false);
    }

    /**
     * Removes the first instance of the specified value in the queue.
     * <p>
     * 移除队列中指定值的第一个实例。
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used. 如果为 true,使用 == 比较;如果为 false,使用 .equals() 比较。
     * @return true if value was found and removed, false otherwise 如果值被找到并移除则为 true,否则为 false
     */
    public boolean remove(T value, boolean identity){
        int index = indexOf(value, identity);
        if(index == -1) return false;
        removeIndex(index);
        return true;
    }

    /**
     * Removes and returns the item at the specified index.
     * 移除并返回指定索引处的条目。
     */
    public T removeIndex(int index){
        if(index < 0) throw new IndexOutOfBoundsException("index can't be < 0: " + index);
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);

        T[] values = this.values;
        int head = this.head, tail = this.tail;
        index += head;
        T value;
        if(head < tail){ // index is between head and tail.
        // index 介于 head 和 tail 之间。
            value = values[index];
            System.arraycopy(values, index + 1, values, index, tail - index);
            values[tail] = null;
            this.tail--;
        }else if(index >= values.length){ // index is between 0 and tail.
        // index 介于 0 和 tail 之间。
            index -= values.length;
            value = values[index];
            System.arraycopy(values, index + 1, values, index, tail - index);
            this.tail--;
        }else{ // index is between head and values.length.
        // index 介于 head 和 values.length 之间。
            value = values[index];
            System.arraycopy(values, head, values, head + 1, index - head);
            values[head] = null;
            this.head++;
            if(this.head == values.length){
                this.head = 0;
            }
        }
        size--;
        return value;
    }

    /**
     * Returns true if the queue is empty.
     * 如果队列为空,返回 true。
     */
    public boolean isEmpty(){
        return size == 0;
    }

    /**
     * Returns the first (head) item in the queue (without removing it).
     * <p>
     * 返回队列中第一个(头部)条目(不移除)。
     * @throws NoSuchElementException when queue is empty 当队列为空时
     * @see #addFirst(Object)
     * @see #removeFirst()
     */
    public T first(){
        if(size == 0){
            // Underflow
            // 下溢
            throw new NoSuchElementException("Queue is empty.");
        }
        return values[head];
    }

    /**
     * Returns the last (tail) item in the queue (without removing it).
     * <p>
     * 返回队列中最后一个(尾部)条目(不移除)。
     * @throws NoSuchElementException when queue is empty 当队列为空时
     * @see #addLast(Object)
     * @see #removeLast()
     */
    public T last(){
        if(size == 0){
            // Underflow
            // 下溢
            throw new NoSuchElementException("Queue is empty.");
        }
        final T[] values = this.values;
        int tail = this.tail;
        tail--;
        if(tail == -1){
            tail = values.length - 1;
        }
        return values[tail];
    }

    /**
     * Retrieves the value in queue without removing it. Indexing is from the front to back, zero based. Therefore get(0) is the
     * same as {@link #first()}.
     * <p>
     * 检索队列中指定索引处的值而不移除它。索引从前到后、从零开始。因此 get(0) 与 {@link #first()} 相同。
     * @throws IndexOutOfBoundsException when the index is negative or >= size 当索引为负或 >= size 时
     */
    public T get(int index){
        if(index < 0) throw new IndexOutOfBoundsException("index can't be < 0: " + index);
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        final T[] values = this.values;

        int i = head + index;
        if(i >= values.length){
            i -= values.length;
        }
        return values[i];
    }

    /**
     * Removes all values from this queue. Values in backing array are set to null to prevent memory leak, so this operates in
     * O(n).
     * <p>
     * 从此队列中移除所有值。底层数组中的值会被置为 null 以防止内存泄漏,因此此操作为 O(n)。
     */
    public void clear(){
        if(size == 0) return;
        final T[] values = this.values;
        final int head = this.head;
        final int tail = this.tail;

        if(head < tail){
            // Continuous
            // 连续
            for(int i = head; i < tail; i++){
                values[i] = null;
            }
        }else{
            // Wrapped
            // 已回绕
            for(int i = head; i < values.length; i++){
                values[i] = null;
            }
            for(int i = 0; i < tail; i++){
                values[i] = null;
            }
        }
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    /**
     * Returns an iterator for the items in the queue. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the constructor for nested or multithreaded iteration.
     * <p>
     * 返回队列中条目的迭代器。支持移除。注意每次调用此方法都会返回相同的迭代器实例。嵌套或多线程遍历时,请使用构造函数。
     */
    @Override
    public Iterator<T> iterator(){
        if(iterable == null) iterable = new QueueIterable<>(this);
        return iterable.iterator();
    }

    @Override
    public void each(Cons<? super T> c){
        final T[] values = this.values;

        for(int index = 0; index < size; index++){
            int i = head + index;
            if(i >= values.length){
                i -= values.length;
            }
            c.get(values[i]);
        }
    }

    public T find(Boolf<T> func){
        final T[] values = this.values;

        for(int index = 0; index < size; index++){
            int i = head + index;
            if(i >= values.length){
                i -= values.length;
            }
            T val = values[i];
            if(func.get(val)){
                return val;
            }
        }
        return null;
    }

    public String toString(){
        if(size == 0){
            return "[]";
        }
        final T[] values = this.values;
        final int head = this.head;
        final int tail = this.tail;

        StringBuilder sb = new StringBuilder(64);
        sb.append('[');
        sb.append(values[head]);
        for(int i = (head + 1) % values.length; i != tail; i = (i + 1) % values.length){
            sb.append(", ").append(values[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public int hashCode(){
        final int size = this.size;
        final T[] values = this.values;
        final int backingLength = values.length;
        int index = this.head;

        int hash = size + 1;
        for(int s = 0; s < size; s++){
            final T value = values[index];

            hash *= 31;
            if(value != null) hash += value.hashCode();

            index++;
            if(index == backingLength) index = 0;
        }

        return hash;
    }

    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof Queue)) return false;

        Queue<?> q = (Queue<?>)o;
        final int size = this.size;

        if(q.size != size) return false;

        final T[] myValues = this.values;
        final int myBackingLength = myValues.length;
        final Object[] itsValues = q.values;
        final int itsBackingLength = itsValues.length;

        int myIndex = head;
        int itsIndex = q.head;
        for(int s = 0; s < size; s++){
            T myValue = myValues[myIndex];
            Object itsValue = itsValues[itsIndex];

            if(!(myValue == null ? itsValue == null : myValue.equals(itsValue))) return false;
            myIndex++;
            itsIndex++;
            if(myIndex == myBackingLength) myIndex = 0;
            if(itsIndex == itsBackingLength) itsIndex = 0;
        }
        return true;
    }

    public static class QueueIterable<T> implements Iterable<T>{
        final Queue<T> queue;
        final boolean allowRemove;
        private QueueIterator iterator1, iterator2;

        public QueueIterable(Queue<T> queue){
            this(queue, true);
        }

        public QueueIterable(Queue<T> queue, boolean allowRemove){
            this.queue = queue;
            this.allowRemove = allowRemove;
        }

        @Override
        public Iterator<T> iterator(){
            if(iterator1 == null){
                iterator1 = new QueueIterator();
                iterator2 = new QueueIterator();
            }

            if(iterator1.done){
                iterator1.index = 0;
                iterator1.done = false;
                return iterator1;
            }

            if(iterator2.done){
                iterator2.index = 0;
                iterator2.done = false;
                return iterator2;
            }
            //allocate new iterator in the case of 3+ nested loops.
            // 在 3 层及以上的嵌套循环情况下分配新迭代器。
            return new QueueIterator();
        }

        private class QueueIterator implements Iterator<T>, Iterable<T>{
            int index;
            boolean done = true;

            QueueIterator(){
            }

            @Override
            public boolean hasNext(){
                if(index >= queue.size) done = true;
                return index < queue.size;
            }

            @Override
            public T next(){
                if(index >= queue.size) throw new NoSuchElementException(String.valueOf(index));
                return queue.get(index++);
            }

            @Override
            public void remove(){
                if(!allowRemove) throw new ArcRuntimeException("Remove not allowed.");
                index--;
                queue.removeIndex(index);
            }

            public void reset(){
                index = 0;
            }

            @Override
            public Iterator<T> iterator(){
                return this;
            }
        }
    }
}
