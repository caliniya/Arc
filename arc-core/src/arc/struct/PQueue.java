package arc.struct;

import arc.util.*;

import java.util.*;

/**
 * A priority queue.
 * 一个优先队列。
 */
@SuppressWarnings("unchecked")
public class PQueue<E>{
    private static final float CAPACITY_RATIO_LOW = 1.5f;
    private static final float CAPACITY_RATIO_HI = 2f;

    /**
     * Priority queue represented as a balanced binary heap: the two children of queue[n] are queue[2*n+1] and queue[2*(n+1)]. The
     * priority queue is ordered by the elements' natural ordering: For each node n in the heap and each descendant d of n, n <= d.
     * The element with the lowest value is in queue[0], assuming the queue is nonempty.
     * <p>
     * 以平衡二叉堆表示的优先队列:queue[n] 的两个子节点是 queue[2*n+1] 和 queue[2*(n+1)]。优先队列按元素的自然顺序排列:对于堆中的每个节点 n 及其每个后代 d,都有 n <= d。假设队列非空,值最小的元素位于 queue[0]。
     */
    public Object[] queue;
    /**
     * The number of elements in the priority queue.
     * 优先队列中的元素数量。
     */
    public int size = 0;
    /**
     * Function used for comparisons.
     * 用于比较的函数。
     */
    public Comparator<E> comparator;

    /**
     * Creates a {@code PriorityQueue} with the default initial capacity that orders its elements according to their
     * {@linkplain Comparable natural ordering}.
     * <p>
     * 创建一个具有默认初始容量的 {@code PriorityQueue},按元素的 {@linkplain Comparable 自然顺序} 排序。
     */
    public PQueue(){
        this(12, null);
    }

    /**
     * Creates a {@code PriorityQueue} with the specified initial capacity that orders its elements according to their
     * {@linkplain Comparable natural ordering}.
     * <p>
     * 创建一个具有指定初始容量的 {@code PriorityQueue},按元素的 {@linkplain Comparable 自然顺序} 排序。
     * @param initialCapacity the initial capacity for this priority queue 此优先队列的初始容量
     */
    public PQueue(int initialCapacity, Comparator<E> comparator){
        this.queue = new Object[initialCapacity];
        this.comparator = comparator;
    }

    public boolean empty(){
        return size == 0;
    }

    /**
     * Inserts the specified element into this priority queue.
     * <p>
     * 将指定元素插入此优先队列。
     * @return true if the element was added to this queue, else false 如果元素被添加到此队列则为 true,否则为 false
     * @throws ClassCastException if the specified element cannot be compared with elements currently in this priority queue 如果指定元素无法与此优先队列中当前的元素进行比较
     * according to the priority queue's ordering
     * @throws IllegalArgumentException if the specified element is null 如果指定元素为 null
     */
    public boolean add(E e){
        if(e == null) throw new IllegalArgumentException("Element cannot be null.");
        int i = size;
        if(i >= queue.length) growToSize(i + 1);
        size = i + 1;
        if(i == 0)
            queue[0] = e;
        else
            siftUp(i, e);
        return true;
    }

    /**
     * Retrieves, but does not remove, the head of this queue. If this queue is empty {@code null} is returned.
     * <p>
     * 检索但不移除此队列的头部。如果此队列为空,返回 {@code null}。
     * @return the head of this queue 此队列的头部
     */
    public E peek(){
        return size == 0 ? null : (E)queue[0];
    }

    /**
     * Retrieves the element at the specified index. If such an element doesn't exist {@code null} is returned.
     * <p>
     * Iterating the queue by index is <em>not</em> guaranteed to traverse the elements in any particular order.
     * <p>
     * 检索指定索引处的元素。如果该元素不存在,返回 {@code null}。<p> 按索引遍历队列不保证以任何特定顺序访问元素。
     * @return the element at the specified index in this queue. 此队列中指定索引处的元素。
     */
    public E get(int index){
        return index >= size ? null : (E)queue[index];
    }

    /**
     * Returns the number of elements in this queue.
     * 返回此队列中的元素数量。
     */
    public int size(){
        return size;
    }

    /**
     * Removes all of the elements from this priority queue. The queue will be empty after this call returns.
     * 移除此优先队列中的所有元素。此调用返回后,队列将为空。
     */
    public void clear(){
        for(int i = 0; i < size; i++) queue[i] = null;
        size = 0;
    }

    /**
     * Retrieves and removes the head of this queue, or returns {@code null} if this queue is empty.
     * <p>
     * 检索并移除此队列的头部,如果此队列为空则返回 {@code null}。
     * @return the head of this queue, or {@code null} if this queue is empty. 此队列的头部,如果此队列为空则为 {@code null}。
     */
    public E poll(){
        if(size == 0) return null;
        int s = --size;
        E result = (E)queue[0];
        E x = (E)queue[s];
        queue[s] = null;
        if(s != 0) siftDown(0, x);
        return result;
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by promoting x up the tree until it is greater than or equal to its
     * parent, or is the root.
     * <p>
     * 将元素 x 插入位置 k,通过反复将 x 沿树上移来维持堆不变式,直到它大于等于其父节点或成为根。
     * @param k the position to fill 要填充的位置
     * @param x the item to insert 要插入的项
     */
    private void siftUp(int k, E x){
        while(k > 0){
            int parent = (k - 1) >>> 1;
            E e = (E)queue[parent];
            if(compare(x, e) >= 0) break;
            queue[k] = e;
            k = parent;
        }
        queue[k] = x;
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by demoting x down the tree repeatedly until it is less than or
     * equal to its children or is a leaf.
     * <p>
     * 将元素 x 插入位置 k,通过反复将 x 沿树下移来维持堆不变式,直到它小于等于其子节点或成为叶子。
     * @param k the position to fill 要填充的位置
     * @param x the item to insert 要插入的项
     */
    private void siftDown(int k, E x){
        int half = size >>> 1; // loop while a non-leaf
        // 在非叶节点时循环
        while(k < half){
            int child = (k << 1) + 1; // assume left child is least
            // 假定左子节点最小
            E c = (E)queue[child];
            int right = child + 1;
            if(right < size && compare(c, (E)queue[right]) > 0) c = (E)queue[child = right];
            if(compare(x, c) <= 0) break;
            queue[k] = c;
            k = child;
        }
        queue[k] = x;
    }

    private int compare(E a, E b){
        return comparator == null ? ((Comparable<E>)a).compareTo(b) : comparator.compare(a, b);
    }

    /**
     * Increases the capacity of the array.
     * <p>
     * 增加数组的容量。
     * @param minCapacity the desired minimum capacity 期望的最小容量
     */
    private void growToSize(int minCapacity){
        if(minCapacity < 0) // overflow
        // 溢出
            throw new ArcRuntimeException("Capacity upper limit exceeded.");
        int oldCapacity = queue.length;
        // Double size if small; else grow by 50%
        // 小时加倍;否则增长 50%
        int newCapacity = (int)((oldCapacity < 64) ? ((oldCapacity + 1) * CAPACITY_RATIO_HI) : (oldCapacity * CAPACITY_RATIO_LOW));
        if(newCapacity < 0) // overflow
        // 溢出
            newCapacity = Integer.MAX_VALUE;
        if(newCapacity < minCapacity) newCapacity = minCapacity;
        Object[] newQueue = new Object[newCapacity];
        System.arraycopy(queue, 0, newQueue, 0, size);
        queue = newQueue;
    }

}
