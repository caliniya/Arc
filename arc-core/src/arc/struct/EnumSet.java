package arc.struct;

/**
 * Tiny array wrapper with a mask int for fast contains() checks.
 * 带有一个掩码 int 的小型数组包装器,用于快速的 contains() 检查。
 */
public class EnumSet<T extends Enum<T>>{
    private long mask;

    /**
     * Array, for iterating over. Do not change.
     * 用于遍历的数组。不要修改。
     */
    public T[] array;
    public int size;

    EnumSet(){
    }

    EnumSet(int size){
        this.size = size;
    }

    public static <T extends Enum<T>> EnumSet<T> of(T... arr){
        EnumSet<T> set = new EnumSet<>(arr.length);
        set.array = arr;
        for(T t : arr){
            set.mask |= (1L << t.ordinal());
        }
        return set;
    }

    /**
     * @return a new set with the specified enum, or itself if this flag is already present. 包含指定 enum 的新集合;如果此标志已存在,则返回其自身。
     */
    public EnumSet<T> with(T add){
        if(!contains(add)){
            T[] copy = (T[])java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), array.length + 1);
            System.arraycopy(array, 0, copy, 0, array.length);
            copy[copy.length - 1] = add;
            return of(copy);
        }
        return this;
    }

    public boolean contains(T t){
        return (mask & (1L << t.ordinal())) != 0;
    }

    public boolean containsAny(EnumSet<T> other){
        return (mask & other.mask) != 0;
    }

    public boolean containsAll(EnumSet<T> other){
        return (mask & other.mask) == other.mask;
    }
}
