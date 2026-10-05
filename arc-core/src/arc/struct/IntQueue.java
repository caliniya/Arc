package arc.struct;

import java.util.NoSuchElementException;

/**
 * Queue for ints.
 * int 队列。
 */
public class IntQueue{
    /**
     * Number of elements in the queue.
     * 队列中的元素数量。
     */
    public int size = 0;
    /**
     * Contains the values in the queue. Head and tail indices go in a circle around this array, wrapping at the end.
     * 包含队列中的值。头和尾索引围绕此数组循环,在末尾处回绕。
     */
    public int[] values;
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

    /**
     * Creates a new Queue which can hold 16 values without needing to resize backing array.
     * 创建一个新的 Queue,可容纳 16 个值而无需扩容底层数组。
     */
    public IntQueue(){
        this(16);
    }

    /**
     * Creates a new Queue which can hold the specified number of values without needing to resize backing array.
     * 创建一个新的 Queue,可容纳指定数量的值而无需扩容底层数组。
     */
    public IntQueue(int initialSize){
        this.values = new int[initialSize];
    }

    /**
     * Append given object to the tail. (enqueue to tail) Unless backing array needs resizing, operates in O(1) time.
     * <p>
     * 将给定对象追加到尾部。(入队到尾部)除非底层数组需要扩容,否则操作时间为 O(1)。
     * @param object can be null 可以为 null
     */
    public void addLast(int object){
        int[] values = this.values;

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
     * Prepend given object to the head. (enqueue to head) Unless backing array needs resizing, operates in O(1) time.
     * <p>
     * 将给定对象前插到头部。(入队到头部)除非底层数组需要扩容,否则操作时间为 O(1)。
     * @param object can be null 可以为 null
     * @see #addLast(int)
     */
    public void addFirst(int object){
        int[] values = this.values;

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
    public int[] shrink(){
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
        final int[] values = this.values;
        final int head = this.head;
        final int tail = this.tail;

        final int[] newArray = new int[newSize];
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
    public int removeFirst(){
        if(size == 0){
            // Underflow
            // 下溢
            throw new NoSuchElementException("Queue is empty.");
        }

        final int[] values = this.values;

        final int result = values[head];
        values[head] = 0;
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
    public int removeLast(){
        if(size == 0){
            throw new NoSuchElementException("Queue is empty.");
        }

        final int[] values = this.values;
        int tail = this.tail;
        tail--;
        if(tail == -1){
            tail = values.length - 1;
        }
        final int result = values[tail];
        values[tail] = 0;
        this.tail = tail;
        size--;

        return result;
    }

    /**
     * Returns the index of first occurrence of value in the queue, or -1 if no such value exists.
     * <p>
     * 返回 value 在队列中第一次出现的索引,如果不存在这样的值则返回 -1。
     * @return An index of first occurrence of value in queue or -1 if no such value exists value 在队列中第一次出现的索引,如果不存在则为 -1
     */
    public int indexOf(int value){
        if(size == 0) return -1;
        int[] values = this.values;
        final int head = this.head, tail = this.tail;
        if(head < tail){
            for(int i = head; i < tail; i++)
                if(values[i] == value) return i - head;
        }else{
            for(int i = head, n = values.length; i < n; i++)
                if(values[i] == value) return i - head;
            for(int i = 0; i < tail; i++)
                if(values[i] == value) return i + values.length - head;
        }
        return -1;
    }

    /**
     * Removes the first instance of the specified value in the queue.
     * <p>
     * 移除队列中指定值的第一个实例。
     * @return true if value was found and removed, false otherwise 如果值被找到并移除则为 true,否则为 false
     */
    public boolean removeValue(int value){
        int index = indexOf(value);
        if(index == -1) return false;
        removeIndex(index);
        return true;
    }

    /**
     * Removes and returns the item at the specified index.
     * 移除并返回指定索引处的条目。
     */
    public int removeIndex(int index){
        if(index < 0) throw new IndexOutOfBoundsException("index can't be < 0: " + index);
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);

        int[] values = this.values;
        int head = this.head, tail = this.tail;
        index += head;
        int value;
        if(head < tail){ // index is between head and tail.
        // index 介于 head 和 tail 之间。
            value = values[index];
            System.arraycopy(values, index + 1, values, index, tail - index);
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
     * @see #addFirst(int)
     * @see #removeFirst()
     */
    public int first(){
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
     * @see #addLast(int)
     * @see #removeLast()
     */
    public int last(){
        if(size == 0){
            // Underflow
            // 下溢
            throw new NoSuchElementException("Queue is empty.");
        }
        final int[] values = this.values;
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
    public int get(int index){
        if(index < 0) throw new IndexOutOfBoundsException("index can't be < 0: " + index);
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        final int[] values = this.values;

        int i = head + index;
        if(i >= values.length){
            i -= values.length;
        }
        return values[i];
    }

    public void set(int index, int value){
        if(index < 0) throw new IndexOutOfBoundsException("index can't be < 0: " + index);
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        final int[] values = this.values;

        int i = head + index;
        if(i >= values.length){
            i -= values.length;
        }
        values[i] = value;
    }

    /**
     * Removes all values from this queue; O(1).
     * 移除此队列中的所有值;O(1)。
     */
    public void clear(){
        if(size == 0) return;
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    public String toString(){
        if(size == 0){
            return "[]";
        }
        final int[] values = this.values;
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
}
