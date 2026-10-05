package arc.util.pooling;

import arc.struct.Ar;

/**
 * A pool of objects that can be reused to avoid allocation.
 * <p>
 * 一个可复用的对象池,用于避免内存分配。
 * @author Nathan Sweet
 * @see Pools
 */
abstract public class Pool<T>{
    /**
     * The maximum number of objects that will be pooled.
     * 会被池化的最大对象数量。
     */
    public final int max;
    private final Ar<T> freeObjects;
    /**
     * The highest number of free objects. Can be reset any time.
     * 空闲对象的最大数量。可随时重置。
     */
    public int peak;

    /**
     * Creates a pool with an initial capacity of 16 and no maximum.
     * 创建初始容量为 16 且无上限的池。
     */
    public Pool(){
        this(16, Integer.MAX_VALUE);
    }

    /**
     * Creates a pool with the specified initial capacity and no maximum.
     * 以指定的初始容量创建无上限的池。
     */
    public Pool(int initialCapacity){
        this(initialCapacity, Integer.MAX_VALUE);
    }

    /**
     * @param max The maximum number of free objects to store in this pool.
     * 此池中可存储的空闲对象的最大数量。
     */
    public Pool(int initialCapacity, int max){
        freeObjects = new Ar<>(false, initialCapacity);
        this.max = max;
    }

    abstract protected T newObject();

    /**
     * Returns an object from this pool. The object may be new (from {@link #newObject()}) or reused (previously
     * {@link #free(Object) freed}).
     * <p>
     * 从此池返回一个对象。该对象可能是新建的(来自 {@link #newObject()}),也可能是复用的(之前 {@link #free(Object) 释放}的)。
     */
    public T obtain(){
        return freeObjects.size == 0 ? newObject() : freeObjects.pop();
    }

    /**
     * Puts the specified object in the pool, making it eligible to be returned by {@link #obtain()}. If the pool already contains
     * {@link #max} free objects, the specified object is reset but not added to the pool.
     * <p>
     * The pool does not check if an object is already freed, so the same object must not be freed multiple times.
     * <p>
     * 将指定对象放入池中,使其可被 {@link #obtain()} 返回。如果池中已有 {@link #max} 个空闲对象,则指定的对象会被重置但不会加入池中。 <p> 池不检查对象是否已被释放,因此同一个对象不得被多次释放。
     */
    public void free(T object){
        if(object == null) throw new IllegalArgumentException("object cannot be null.");
        if(freeObjects.size < max){
            freeObjects.add(object);
            peak = Math.max(peak, freeObjects.size);
        }
        reset(object);
    }

    /**
     * Called when an object is freed to clear the state of the object for possible later reuse. The default implementation calls
     * {@link Poolable#reset()} if the object is {@link Poolable}.
     * <p>
     * 当对象被释放时调用,用于清除对象状态以便日后复用。默认实现在对象是 {@link Poolable} 时调用 {@link Poolable#reset()}。
     */
    protected void reset(T object){
        if(object instanceof Poolable) ((Poolable)object).reset();
    }

    /**
     * Puts the specified objects in the pool. Null objects within the array are silently ignored.
     * <p>
     * The pool does not check if an object is already freed, so the same object must not be freed multiple times.
     * <p>
     * 将指定的一批对象放入池中。数组中的 null 对象会被静默忽略。 <p> 池不检查对象是否已被释放,因此同一个对象不得被多次释放。
     * @see #free(Object)
     */
    public void freeAll(Ar<T> objects){
        if(objects == null) throw new IllegalArgumentException("objects cannot be null.");
        Ar<T> freeObjects = this.freeObjects;
        int max = this.max;
        for(int i = 0; i < objects.size; i++){
            T object = objects.get(i);
            if(object == null) continue;
            if(freeObjects.size < max) freeObjects.add(object);
            reset(object);
        }
        peak = Math.max(peak, freeObjects.size);
    }

    /**
     * Removes all free objects from this pool.
     * 移除此池中所有空闲对象。
     */
    public void clear(){
        freeObjects.clear();
    }

    /**
     * The number of objects available to be obtained.
     * 可获取的对象数量。
     */
    public int getFree(){
        return freeObjects.size;
    }

    /**
     * Objects implementing this interface will have {@link #reset()} called when passed to {@link Pool#free(Object)}.
     * 实现此接口的对象在被传给 {@link Pool#free(Object)} 时会调用 {@link #reset()}。
     */
    public interface Poolable{
        /**
         * Resets the object for reuse. Object references should be nulled and fields may be set to default values.
         * 重置对象以便复用。对象引用应置为 null,字段可设为默认值。
         */
        void reset();
    }
}
