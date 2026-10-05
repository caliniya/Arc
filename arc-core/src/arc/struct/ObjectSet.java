package arc.struct;

import arc.func.*;
import arc.math.*;
import arc.util.*;

import java.util.*;

/**
 * An unordered set where the keys are objects. This implementation uses cuckoo
 * hashing using 3 hashes, random walking, and a
 * small stash for problematic keys. Null keys are not allowed. No allocation is
 * done except when growing the table size. <br>
 * <br>
 * This set performs very fast contains and remove (typically O(1), worst case
 * O(log(n))). Add may be a bit slower, depending on
 * hash collisions. Load factors greater than 0.91 greatly increase the chances
 * the set will have to rehash to the next higher POT
 * size.<br>
 * <br>
 * Iteration can be very slow for a set with a large capacity.
 * {@link #clear(int)} and {@link #shrink(int)} can be used to reduce
 * the capacity. {@link OrderedSet} provides much faster iteration.
 * 
 * <p>
 * 键为对象的无序集合。此实现使用布谷鸟哈希,采用 3 个哈希函数、随机游走以及一个存放问题键的小 stash。不允许 null 键。除扩容表大小外不进行任何分配。<br> <br> 此集合的 contains 和 remove 非常快(通常 O(1),最坏 O(log(n)))。add 可能稍慢,取决于哈希冲突。负载因子大于 0.91 会大大增加集合被迫重新哈希到下一个更高 2 的幂(POT)大小的机会。<br> <br> 对于容量较大的集合,遍历可能非常慢。可以使用 {@link #clear(int)} 和 {@link #shrink(int)} 来减小容量。{@link OrderedSet} 提供快得多的遍历。
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class ObjectSet<T> implements Iterable<T>, Eachable<T> {
  private static final int PRIME1 = 0xbe1f14b1;
  private static final int PRIME2 = 0xb4b82e39;
  private static final int PRIME3 = 0xced1c241;

  public int size;

  T[] keyTable;
  int capacity, stashSize;

  private float loadFactor;
  private int hashShift, mask, threshold;
  private int stashCapacity;
  private int pushIterations;

  private @Nullable ObjectSetIterator iterator1, iterator2;

  /**
   * Creates a new set with an initial capacity of 51 and a load factor of 0.8.
   * <p>
   * 创建一个初始容量为 51、负载因子为 0.8 的新集合。
   */
  public ObjectSet() {
    this(51, 0.8f);
  }

  /**
   * Creates a new set with a load factor of 0.8.
   * 
   * <p>
   * 创建一个负载因子为 0.8 的新集合。
   * @param initialCapacity If not a power of two, it is increased to the next 如果不是 2 的幂,则增加到下一个
   *                        nearest power of two.
   */
  public ObjectSet(int initialCapacity) {
    this(initialCapacity, 0.8f);
  }

  /**
   * Creates a new set with the specified initial capacity and load factor. This
   * set will hold initialCapacity items before
   * growing the backing table.
   * 
   * <p>
   * 创建一个具有指定初始容量和负载因子的新集合。在底层数组扩容之前,此集合可容纳 initialCapacity 个条目。
   * @param initialCapacity If not a power of two, it is increased to the next 如果不是 2 的幂,则增加到下一个
   *                        nearest power of two.
   */
  public ObjectSet(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0)
      throw new IllegalArgumentException("initialCapacity must be >= 0: " + initialCapacity);
    initialCapacity = Mathf.nextPowerOfTwo((int) Math.ceil(initialCapacity / loadFactor));
    if (initialCapacity > 1 << 30)
      throw new IllegalArgumentException("initialCapacity is too large: " + initialCapacity);
    capacity = initialCapacity;

    if (loadFactor <= 0)
      throw new IllegalArgumentException("loadFactor must be > 0: " + loadFactor);
    this.loadFactor = loadFactor;

    threshold = (int) (capacity * loadFactor);
    mask = capacity - 1;
    hashShift = 31 - Integer.numberOfTrailingZeros(capacity);
    stashCapacity = Math.max(3, (int) Math.ceil(Math.log(capacity)) * 2);
    pushIterations = Math.max(Math.min(capacity, 8), (int) Math.sqrt(capacity) / 8);

    keyTable = (T[]) new Object[capacity + stashCapacity];
  }

  /**
   * Creates a new set identical to the specified set.
   * 创建一个与指定集合完全相同的新集合。
   */
  public ObjectSet(ObjectSet<? extends T> set) {
    this((int) Math.floor(set.capacity * set.loadFactor), set.loadFactor);
    stashSize = set.stashSize;
    System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.length);
    size = set.size;
  }

  public static <T> ObjectSet<T> with(T... array) {
    ObjectSet<T> set = new ObjectSet<>();
    set.addAll(array);
    return set;
  }

  public static <T> ObjectSet<T> with(Ar<T> array) {
    ObjectSet<T> set = new ObjectSet<>();
    set.addAll(array);
    return set;
  }

  /**
   * Allocates a new set with all elements that match the predicate.
   * 分配一个新集合,包含所有匹配谓词的元素。
   */
  public ObjectSet<T> select(Boolf<T> predicate) {
    ObjectSet<T> arr = new ObjectSet<>();
    for (T t : this) {
      if (predicate.get(t))
        arr.add(t);
    }
    return arr;
  }

  public Ar<T> toAr() {
    return iterator().toAr();
  }

  public ObjectSet<T> copy() {
    ObjectSet<T> result = new ObjectSet<>();
    result.addAll(this);
    return result;
  }

  public T find(Boolf<T> predicate) {
    for (T t : this) {
      if (predicate.get(t)) return t;
    }
    return null;
  }

  public boolean any() {
    return size > 0;
  }

  /**
   * Returns true if the set has one or more items.
   * 如果集合中有一个或多个条目,返回 true。
   */
  public boolean notEmpty() {
    return size > 0;
  }

  @Override
  public void each(Cons<? super T> cons) {
    for (T t : this) {
      cons.get(t);
    }
  }

  /**
   * Returns true if the key was not already in the set. If this set already
   * contains the key, the call leaves the set unchanged
   * and returns false.
   * <p>
   * 如果键原本不在集合中,返回 true。如果此集合已包含该键,调用会保持集合不变并返回 false。
   */
  public boolean add(T key) {
    if (key == null)
      return false;
    T[] keyTable = this.keyTable;

    // Check for existing keys.
    // 检查已存在的键。
    int hashCode = key.hashCode();
    int index1 = hashCode & mask;
    T key1 = keyTable[index1];
    if (key.equals(key1))
      return false;

    int index2 = hash2(hashCode);
    T key2 = keyTable[index2];
    if (key.equals(key2))
      return false;

    int index3 = hash3(hashCode);
    T key3 = keyTable[index3];
    if (key.equals(key3))
      return false;

    // Find key in the stash.
    // 在 stash 中查找键。
    for (int i = capacity, n = i + stashSize; i < n; i++)
      if (key.equals(keyTable[i]))
        return false;

    // Check for empty buckets.
    // 检查空桶。
    if (key1 == null) {
      keyTable[index1] = key;
      if (size++ >= threshold)
        resize(capacity << 1);
      return true;
    }

    if (key2 == null) {
      keyTable[index2] = key;
      if (size++ >= threshold)
        resize(capacity << 1);
      return true;
    }

    if (key3 == null) {
      keyTable[index3] = key;
      if (size++ >= threshold)
        resize(capacity << 1);
      return true;
    }

    push(key, index1, key1, index2, key2, index3, key3);
    return true;
  }

  public void addAll(Ar<? extends T> array) {
    addAll(array.items, 0, array.size);
  }

  public void addAll(Ar<? extends T> array, int offset, int length) {
    if (offset + length > array.size)
      throw new IllegalArgumentException(
          "offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
    addAll(array.items, offset, length);
  }

  public boolean addAll(T... array) {
    return addAll(array, 0, array.length);
  }

  public boolean addAll(T[] array, int offset, int length) {
    ensureCapacity(length);
    int oldSize = size;
    for (int i = offset, n = i + length; i < n; i++)
      add(array[i]);
    return oldSize != size;
  }

  public void addAll(ObjectSet<? extends T> set) {
    ensureCapacity(set.size);
    for (T key : set)
      add(key);
  }

  public void removeAll(T[] array, int offset, int length) {
    for (int i = offset, n = i + length; i < n; i++)
      remove(array[i]);
  }

  public void removeAll(T[] array) {
    for (T t : array) {
      remove(t);
    }
  }

  public void removeAll(Ar<? extends T> array) {
    removeAll(array.items, 0, array.size);
  }

  /**
   * Skips checks for existing keys.
   * 跳过对现有键的检查。
   */
  private void addResize(T key) {
    // Check for empty buckets.
    // 检查空桶。
    int hashCode = key.hashCode();
    int index1 = hashCode & mask;
    T key1 = keyTable[index1];
    if (key1 == null) {
      keyTable[index1] = key;
      if (size++ >= threshold)
        resize(capacity << 1);
      return;
    }

    int index2 = hash2(hashCode);
    T key2 = keyTable[index2];
    if (key2 == null) {
      keyTable[index2] = key;
      if (size++ >= threshold)
        resize(capacity << 1);
      return;
    }

    int index3 = hash3(hashCode);
    T key3 = keyTable[index3];
    if (key3 == null) {
      keyTable[index3] = key;
      if (size++ >= threshold)
        resize(capacity << 1);
      return;
    }

    push(key, index1, key1, index2, key2, index3, key3);
  }

  private void push(T insertKey, int index1, T key1, int index2, T key2, int index3, T key3) {
    T[] keyTable = this.keyTable;
    int mask = this.mask;

    // Push keys until an empty bucket is found.
    // 持续放入键,直到找到空桶。
    T evictedKey;
    int i = 0, pushIterations = this.pushIterations;
    do {
      // Replace the key and value for one of the hashes.
      // 替换其中一个哈希的键和值。
      switch (Mathf.random(2)) {
        case 0:
          evictedKey = key1;
          keyTable[index1] = insertKey;
          break;
        case 1:
          evictedKey = key2;
          keyTable[index2] = insertKey;
          break;
        default:
          evictedKey = key3;
          keyTable[index3] = insertKey;
          break;
      }

      // If the evicted key hashes to an empty bucket, put it there and stop.
      // 如果被逐出的键哈希到了空桶,就把它放在那里并停止。
      int hashCode = evictedKey.hashCode();
      index1 = hashCode & mask;
      key1 = keyTable[index1];
      if (key1 == null) {
        keyTable[index1] = evictedKey;
        if (size++ >= threshold)
          resize(capacity << 1);
        return;
      }

      index2 = hash2(hashCode);
      key2 = keyTable[index2];
      if (key2 == null) {
        keyTable[index2] = evictedKey;
        if (size++ >= threshold)
          resize(capacity << 1);
        return;
      }

      index3 = hash3(hashCode);
      key3 = keyTable[index3];
      if (key3 == null) {
        keyTable[index3] = evictedKey;
        if (size++ >= threshold)
          resize(capacity << 1);
        return;
      }

      if (++i == pushIterations)
        break;

      insertKey = evictedKey;
    } while (true);

    addStash(evictedKey);
  }

  private void addStash(T key) {
    if (stashSize == stashCapacity) {
      // Too many pushes occurred and the stash is full, increase the table size.
      // 放入次数过多且 stash 已满,应增大表的大小。
      resize(capacity << 1);
      addResize(key);
      return;
    }
    // Store key in the stash.
    // 将键存入 stash。
    int index = capacity + stashSize;
    keyTable[index] = key;
    stashSize++;
    size++;
  }

  /**
   * Returns true if the key was removed.
   * 如果键被移除了,返回 true。
   */
  public boolean remove(T key) {
    int hashCode = key.hashCode();
    int index = hashCode & mask;
    if (key.equals(keyTable[index])) {
      keyTable[index] = null;
      size--;
      return true;
    }

    index = hash2(hashCode);
    if (key.equals(keyTable[index])) {
      keyTable[index] = null;
      size--;
      return true;
    }

    index = hash3(hashCode);
    if (key.equals(keyTable[index])) {
      keyTable[index] = null;
      size--;
      return true;
    }

    return removeStash(key);
  }

  boolean removeStash(T key) {
    T[] keyTable = this.keyTable;
    for (int i = capacity, n = i + stashSize; i < n; i++) {
      if (key.equals(keyTable[i])) {
        removeStashIndex(i);
        size--;
        return true;
      }
    }
    return false;
  }

  void removeStashIndex(int index) {
    // If the removed location was not last, move the last tuple to the removed
    // 如果被移除的位置不是最后一个,则把最后一个元组移动到被移除的
    // location.
    // 位置。
    stashSize--;
    int lastIndex = capacity + stashSize;
    if (index < lastIndex) {
      keyTable[index] = keyTable[lastIndex];
      keyTable[lastIndex] = null;
    }
  }

  /**
   * Returns true if the set is empty.
   * 如果集合为空,返回 true。
   */
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Reduces the size of the backing arrays to be the specified capacity or less.
   * If the capacity is already less, nothing is
   * done. If the set contains more items than the specified capacity, the next
   * highest power of two capacity is used instead.
   * <p>
   * 将底层数组的大小缩减为不超过指定容量。如果容量已经更小,则不做任何操作。如果集合包含的条目多于指定容量,则改用次高的 2 的幂容量。
   */
  public void shrink(int maximumCapacity) {
    if (maximumCapacity < 0)
      throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
    if (size > maximumCapacity)
      maximumCapacity = size;
    if (capacity <= maximumCapacity)
      return;
    maximumCapacity = Mathf.nextPowerOfTwo(maximumCapacity);
    resize(maximumCapacity);
  }

  /**
   * Clears the set and reduces the size of the backing arrays to be the specified
   * capacity, if they are larger. The reduction
   * is done by allocating new arrays, though for large arrays this can be faster
   * than clearing the existing array.
   * <p>
   * 清除集合并将底层数组的大小缩减为指定容量(如果它们更大的话)。缩减通过分配新数组完成,不过对大数组而言,这可能比清除现有数组更快。
   */
  public void clear(int maximumCapacity) {
    if (capacity <= maximumCapacity) {
      clear();
      return;
    }
    size = 0;
    resize(maximumCapacity);
  }

  /**
   * Clears the set, leaving the backing arrays at the current capacity. When the
   * capacity is high and the population is low,
   * iteration can be unnecessarily slow. {@link #clear(int)} can be used to
   * reduce the capacity.
   * <p>
   * 清除集合,底层数组保持当前容量。当容量很高而元素很少时,遍历可能会不必要地变慢。可以使用 {@link #clear(int)} 来减小容量。
   */
  public void clear() {
    if (size == 0)
      return;
    T[] keyTable = this.keyTable;
    for (int i = capacity + stashSize; i-- > 0;)
      keyTable[i] = null;
    size = 0;
    stashSize = 0;
  }

  public boolean contains(T key) {
    if (size == 0)
      return false;
    int hashCode = key.hashCode();
    int index = hashCode & mask;
    if (!key.equals(keyTable[index])) {
      index = hash2(hashCode);
      if (!key.equals(keyTable[index])) {
        index = hash3(hashCode);
        if (!key.equals(keyTable[index]))
          return getKeyStash(key) != null;
      }
    }
    return true;
  }

  /**
   * @return May be null. 可能为 null。
   */
  public T get(T key) {
    int hashCode = key.hashCode();
    int index = hashCode & mask;
    T found = keyTable[index];
    if (!key.equals(found)) {
      index = hash2(hashCode);
      found = keyTable[index];
      if (!key.equals(found)) {
        index = hash3(hashCode);
        found = keyTable[index];
        if (!key.equals(found))
          return getKeyStash(key);
      }
    }
    return found;
  }

  private T getKeyStash(T key) {
    T[] keyTable = this.keyTable;
    for (int i = capacity, n = i + stashSize; i < n; i++)
      if (key.equals(keyTable[i]))
        return keyTable[i];
    return null;
  }

  public T first() {
    T[] keyTable = this.keyTable;
    for (int i = 0, n = capacity + stashSize; i < n; i++)
      if (keyTable[i] != null)
        return keyTable[i];
    throw new IllegalStateException("ObjectSet is empty.");
  }

  /**
   * Increases the size of the backing array to accommodate the specified number
   * of additional items. Useful before adding many
   * items to avoid multiple backing array resizes.
   * <p>
   * 增大底层数组的大小,以容纳指定数量的额外条目。在添加大量条目之前很有用,可避免多次底层数组扩容。
   */
  public void ensureCapacity(int additionalCapacity) {
    if (additionalCapacity < 0)
      throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);
    int sizeNeeded = size + additionalCapacity;
    if (sizeNeeded >= threshold)
      resize(Mathf.nextPowerOfTwo((int) Math.ceil(sizeNeeded / loadFactor)));
  }

  private void resize(int newSize) {
    int oldEndIndex = capacity + stashSize;

    capacity = newSize;
    threshold = (int) (newSize * loadFactor);
    mask = newSize - 1;
    hashShift = 31 - Integer.numberOfTrailingZeros(newSize);
    stashCapacity = Math.max(3, (int) Math.ceil(Math.log(newSize)) * 2);
    pushIterations = Math.max(Math.min(newSize, 8), (int) Math.sqrt(newSize) / 8);

    T[] oldKeyTable = keyTable;

    keyTable = (T[]) new Object[newSize + stashCapacity];

    int oldSize = size;
    size = 0;
    stashSize = 0;
    if (oldSize > 0) {
      for (int i = 0; i < oldEndIndex; i++) {
        T key = oldKeyTable[i];
        if (key != null)
          addResize(key);
      }
    }
  }

  private int hash2(int h) {
    h *= PRIME2;
    return (h ^ h >>> hashShift) & mask;
  }

  private int hash3(int h) {
    h *= PRIME3;
    return (h ^ h >>> hashShift) & mask;
  }

  public int hashCode() {
    int h = 0;
    for (int i = 0, n = capacity + stashSize; i < n; i++)
      if (keyTable[i] != null)
        h += keyTable[i].hashCode();
    return h;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof ObjectSet))
      return false;
    ObjectSet other = (ObjectSet) obj;
    if (other.size != size)
      return false;
    T[] keyTable = this.keyTable;
    for (int i = 0, n = capacity + stashSize; i < n; i++)
      if (keyTable[i] != null && !other.contains(keyTable[i]))
        return false;
    return true;
  }

  public String toString() {
    return '{' + toString(", ") + '}';
  }

  public String toString(String separator) {
    if (size == 0)
      return "";
    StringBuilder buffer = new StringBuilder(32);
    T[] keyTable = this.keyTable;
    int i = keyTable.length;
    while (i-- > 0) {
      T key = keyTable[i];
      if (key == null)
        continue;
      buffer.append(key);
      break;
    }
    while (i-- > 0) {
      T key = keyTable[i];
      if (key == null)
        continue;
      buffer.append(separator);
      buffer.append(key);
    }
    return buffer.toString();
  }

  /**
   * Returns an iterator for the keys in the set. Remove is supported.
   * <p>
   * Use the {@link ObjectSetIterator} constructor for nested or multithreaded iteration.
   * <p>
   * 返回集合中键的迭代器。支持移除。<p> 嵌套或多线程遍历时,请使用 {@link ObjectSetIterator} 构造函数。
   */
  public ObjectSetIterator<T> iterator() {
    if (iterator1 == null) {
      iterator1 = new ObjectSetIterator(this);
      iterator2 = new ObjectSetIterator(this);
    }

    if (!iterator1.valid) {
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

  static int tableSize(int capacity, float loadFactor) {
    if (capacity < 0) throw new IllegalArgumentException("capacity must be >= 0: " + capacity);
    int tableSize = Mathf.nextPowerOfTwo(Math.max(2, (int) Math.ceil(capacity / loadFactor)));
    if (tableSize > 1 << 30) throw new IllegalArgumentException("The required capacity is too large: " + capacity);
    return tableSize;
  }

  public static class ObjectSetIterator<K> implements Iterable<K>, Iterator<K> {
    public boolean hasNext;

    final ObjectSet<K> set;
    int nextIndex, currentIndex;
    boolean valid = true;

    public ObjectSetIterator(ObjectSet<K> set) {
      this.set = set;
      reset();
    }

    public void reset() {
      currentIndex = -1;
      nextIndex = -1;
      findNextIndex();
    }

    private void findNextIndex() {
      hasNext = false;
      int n = set.capacity + set.stashSize;
      while (++nextIndex < n) {
        if (set.keyTable[nextIndex] != null) {
          hasNext = true;
          break;
        }
      }
    }

    @Override
    public void remove() {
      int i = currentIndex;
      if (i < 0) throw new IllegalStateException("next must be called before remove.");
      if (i >= set.capacity) {
        // stash 区:摘除后把最后一个 stash 项挪过来
        set.removeStashIndex(i);
        nextIndex = i - 1;
        findNextIndex();
      } else {
        set.keyTable[i] = null;
      }
      currentIndex = -1;
      set.size--;
    }

    @Override
    public boolean hasNext() {
      if (!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
      return hasNext;
    }

    @Override
    public K next() {
      if (!hasNext) throw new NoSuchElementException();
      if (!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
      K key = set.keyTable[nextIndex];
      currentIndex = nextIndex;
      findNextIndex();
      return key;
    }

    @Override
    public ObjectSetIterator<K> iterator() {
      return this;
    }

    /**
     * Adds the remaining values to the array.
     * 将剩余的值添加到数组。
     */
    public Ar<K> toAr(Ar<K> array) {
      while (hasNext)
        array.add(next());
      return array;
    }

    /**
     * Returns a new array containing the remaining values.
     * 返回一个包含剩余值的新数组。
     */
    public Ar<K> toAr() {
      return toAr(new Ar<>(true, set.size));
    }
  }
}
