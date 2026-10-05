package arc.struct;

import java.util.Comparator;

/**
 * Guarantees that array entries provided by {@link #begin()} between indexes 0 and {@link #size} at the time begin was called
 * will not be modified until {@link #end()} is called. If modification of the SnapshotArray occurs between begin/end, the backing
 * array is copied prior to the modification, ensuring that the backing array that was returned by {@link #begin()} is unaffected.
 * To avoid allocation, an attempt is made to reuse any extra array created as a result of this copy on subsequent copies.
 * <p>
 * It is suggested iteration be done in this specific way:
 *
 * <pre>
 * SnapshotArray array = new SnapshotArray();
 * // ...
 * Object[] items = array.begin();
 * for (int i = 0, n = array.size; i &lt; n; i++) {
 * 	Object item = items[i];
 * 	// ...
 * }
 * array.end();
 * </pre>
 * <p>
 * 保证在调用 {@link #begin()} 时索引 0 到 {@link #size} 之间由 begin 返回的数组条目,在调用 {@link #end()} 之前不会被修改。如果在 begin/end 之间发生了对 SnapshotArray 的修改,底层数组会在修改之前被复制,确保 {@link #begin()} 返回的底层数组不受影响。为避免内存分配,会在后续复制时尝试复用因该复制而创建的多余数组。<p> 建议按以下特定方式进行遍历:<pre> SnapshotArray array = new SnapshotArray(); // ... Object[] items = array.begin(); for (int i = 0, n = array.size; i &lt; n; i++) { Object item = items[i]; // ... } array.end(); </pre>
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class SnapshotAr<T> extends Ar<T>{
    private T[] snapshot, recycled;
    private int snapshots;

    public SnapshotAr(){
        super();
    }

    public SnapshotAr(Ar<T> array){
        super(array);
    }

    public SnapshotAr(boolean ordered, int capacity, Class<?> arrayType){
        super(ordered, capacity, arrayType);
    }

    public SnapshotAr(boolean ordered, int capacity){
        super(ordered, capacity);
    }

    public SnapshotAr(boolean ordered, T[] array, int startIndex, int count){
        super(ordered, array, startIndex, count);
    }

    public SnapshotAr(Class<?> arrayType){
        super(arrayType);
    }

    public SnapshotAr(int capacity){
        super(capacity);
    }

    public SnapshotAr(T[] array){
        super(array);
    }

    /** @see #SnapshotAr(Object[]) */
    public static <T> SnapshotAr<T> with(T... array){
        return new SnapshotAr<>(array);
    }

    /**
     * Returns the backing array, which is guaranteed to not be modified before {@link #end()}.
     * 返回底层数组,并保证在 {@link #end()} 之前它不会被修改。
     */
    public T[] begin(){
        modified();
        snapshot = items;
        snapshots++;
        return items;
    }

    /**
     * Releases the guarantee that the array returned by {@link #begin()} won't be modified.
     * 解除 {@link #begin()} 返回的数组不会被修改的保证。
     */
    public void end(){
        snapshots = Math.max(0, snapshots - 1);
        if(snapshot == null) return;
        if(snapshot != items && snapshots == 0){
            // The backing array was copied, keep around the old array.
            // 底层数组已被复制,保留旧数组以备复用。
            recycled = snapshot;
            for(int i = 0, n = recycled.length; i < n; i++)
                recycled[i] = null;
        }
        snapshot = null;
    }

    private void modified(){
        if(snapshot == null || snapshot != items) return;
        // Snapshot is in use, copy backing array to recycled array or create new backing array.
        // 快照正在使用中,将底层数组复制到回收的数组,或创建新的底层数组。
        if(recycled != null && recycled.length >= size){
            System.arraycopy(items, 0, recycled, 0, size);
            items = recycled;
            recycled = null;
        }else
            resize(items.length);
    }

    public void set(int index, T value){
        modified();
        super.set(index, value);
    }

    public void insert(int index, T value){
        modified();
        super.insert(index, value);
    }

    public void swap(int first, int second){
        modified();
        super.swap(first, second);
    }

    public boolean remove(T value, boolean identity){
        modified();
        return super.remove(value, identity);
    }

    public T remove(int index){
        modified();
        return super.remove(index);
    }

    public void removeRange(int start, int end){
        modified();
        super.removeRange(start, end);
    }

    public boolean removeAll(Ar<? extends T> array, boolean identity){
        modified();
        return super.removeAll(array, identity);
    }

    public T pop(){
        modified();
        return super.pop();
    }

    public Ar<T> clear(){
        modified();
        super.clear();
        return this;
    }

    public Ar<T> sort(){
        modified();
        return super.sort();
    }

    public Ar<T> sort(Comparator<? super T> comparator){
        modified();
        return super.sort(comparator);
    }

    public Ar<T> reverse(){
        modified();
        return super.reverse();
    }

    public Ar<T> shuffle(){
        modified();
        return super.shuffle();
    }

    public void truncate(int newSize){
        modified();
        super.truncate(newSize);
    }

    public T[] setSize(int newSize){
        modified();
        return super.setSize(newSize);
    }
}
