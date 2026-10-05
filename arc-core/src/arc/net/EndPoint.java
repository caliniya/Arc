

package arc.net;

import java.io.IOException;

/**
 * Represents the local end point of a connection.
 * <p>
 * 表示连接的本地端点。
 * @author Nathan Sweet <misc@n4te.com>
 */
public interface EndPoint extends Runnable{

    /**
     * Adds a listener to the endpoint. If the listener already exists, it is
     * <i>not</i> added again.
     * <p>
     * 向端点添加监听器。如果监听器已存在,<i>不会</i>再次添加。
     */
    void addListener(NetListener listener);

    void removeListener(NetListener listener);

    /**
     * Continually updates this end point until {@link #stop()} is called.
     * <p>
     * 持续更新此端点,直到调用 {@link #stop()}。
     */
    void run();

    /**
     * Starts a new thread that calls {@link #run()}.
     * <p>
     * 启动一个调用 {@link #run()} 的新线程。
     */
    void start();

    /**
     * Closes this end point and causes {@link #run()} to return.
     * <p>
     * 关闭此端点并使 {@link #run()} 返回。
     */
    void stop();

    /**
     * @see Client
     * @see Server
     */
    void close();

    /**
     * @see Client#update(int)
     * @see Server#update(int)
     */
    void update(int timeout) throws IOException;

    /**
     * Returns the last thread that called {@link #update(int)} for this end
     * point. This can be useful to detect when long running code will be run on
     * the update thread.
     * <p>
     * 返回最后一个为此端点调用 {@link #update(int)} 的线程。这可用于检测长耗时的代码何时会在更新线程上运行。
     */
    Thread getUpdateThread();
}
