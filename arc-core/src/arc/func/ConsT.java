package arc.func;

/**
 * A cons that throws something.
 * 一个会抛出异常的 Cons(消费者)。
 */
public interface ConsT<T, E extends Throwable>{
    void get(T t) throws E;
}
