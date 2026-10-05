package arc.func;

/**
 * A runnable where anything might happen.
 * 一个任何事情都可能发生的 Runnable。
 */
public interface UnsafeRunnable{
    void run() throws Throwable;
}
