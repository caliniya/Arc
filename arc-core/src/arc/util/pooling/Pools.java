package arc.util.pooling;

import arc.struct.Ar;
import arc.struct.ObjectMap;
import arc.func.Prov;

/**
 * Stores a map of {@link Pool}s by type for convenient static access.
 * <p>
 * 按类型存储 {@link Pool} 映射表,便于静态访问。
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class Pools{
    private static final ObjectMap<Class, Pool> typePools = new ObjectMap<>();

    private Pools(){
    }

    /**
     * Returns a new or existing pool for the specified type, stored in a Class to {@link Pool} map. Note that the max size is ignored for some reason.
     * if this is not the first time this pool has been requested.
     * <p>
     * 返回指定类型对应的新池或已有池,存储在 Class 到 {@link Pool} 的映射中。注意由于某种原因最大容量会被忽略,如果这不是第一次请求该池。
     */
    public static <T> Pool<T> get(Class<T> type, Prov<T> supplier, int max){
        Pool<T> pool = typePools.get(type);
        if(pool == null){
            pool = new Pool<T>(4, max){
                @Override
                protected T newObject(){
                    return supplier.get();
                }
            };
            typePools.put(type, pool);
        }
        return pool;
    }

    /**
     * Returns a new or existing pool for the specified type, stored in a Class to {@link Pool} map. The max size of the pool used
     * is 5000.
     * <p>
     * 返回指定类型对应的新池或已有池,存储在 Class 到 {@link Pool} 的映射中。所用池的最大容量为 5000。
     */
    public static <T> Pool<T> get(Class<T> type, Prov<T> supplier){
        return get(type, supplier, 5000);
    }

    /**
     * Sets an existing pool for the specified type, stored in a Class to {@link Pool} map.
     * 为指定类型设置已有的池,存储在 Class 到 {@link Pool} 的映射中。
     */
    public static <T> void set(Class<T> type, Pool<T> pool){
        typePools.put(type, pool);
    }

    /**
     * Obtains an object from the {@link #get(Class, Prov) pool}.
     * 从 {@link #get(Class, Prov) 池}中获取一个对象。
     */
    public static synchronized <T> T obtain(Class<T> type, Prov<T> supplier){
        return get(type, supplier).obtain();
    }

    /**
     * Frees an object from the {@link #get(Class, Prov) pool}.
     * 从 {@link #get(Class, Prov) 池}中释放一个对象。
     */
    public static synchronized void free(Object object){
        if(object == null) throw new IllegalArgumentException("Object cannot be null.");
        Pool pool = typePools.get(object.getClass());
        if(pool == null) return; // Ignore freeing an object that was never retained.
        // 忽略释放从未保留过的对象。
        pool.free(object);
    }

    /**
     * Frees the specified objects from the {@link #get(Class, Prov) pool}. Null objects within the array are silently ignored. Objects
     * don't need to be from the same pool.
     * <p>
     * 从 {@link #get(Class, Prov) 池}中释放指定的多个对象。数组中的 null 对象会被静默忽略。这些对象不必来自同一个池。
     */
    public static void freeAll(Ar objects){
        freeAll(objects, false);
    }

    /**
     * Frees the specified objects from the {@link #get(Class, Prov) pool}. Null objects within the array are silently ignored.
     * <p>
     * 从 {@link #get(Class, Prov) 池}中释放指定的多个对象。数组中的 null 对象会被静默忽略。
     * @param samePool If true, objects don't need to be from the same pool but the pool must be looked up for each object. 如果为 true,对象不必来自同一个池,但必须为每个对象各查找一次池。
     */
    public static void freeAll(Ar objects, boolean samePool){
        if(objects == null) throw new IllegalArgumentException("Objects cannot be null.");
        Pool pool = null;
        for(int i = 0, n = objects.size; i < n; i++){
            Object object = objects.get(i);
            if(object == null) continue;
            if(pool == null){
                pool = typePools.get(object.getClass());
                if(pool == null) continue; // Ignore freeing an object that was never retained.
                // 忽略释放从未保留过的对象。
            }
            pool.free(object);
            if(!samePool) pool = null;
        }
    }
}
