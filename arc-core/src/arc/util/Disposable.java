package arc.util;

/**
 * Interface for disposable resources.
 * <p>
 * 可释放资源的接口。
 * @author mzechner
 */
public interface Disposable{
    /**
     * Releases all resources of this object.
     * 释放该对象的所有资源。
     */
    void dispose();

    default boolean isDisposed(){
        return false;
    }
}
