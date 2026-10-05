

package arc.net;

import java.util.LinkedList;
import java.util.concurrent.*;

/**
 * Used to be notified about connection events.
 * <p>
 * 用于接收连接事件的通知。
 */
public interface NetListener{
    /**
     * Called when the remote end has been connected. This will be invoked
     * before any objects are received by {@link #received(Connection, Object)}.
     * This will be invoked on the same thread as {@link Client#update(int)} and
     * {@link Server#update(int)}. This method should not block for long periods
     * as other network activity will not be processed until it returns.
     * <p>
     * 当远程端连接上时调用。这会在 {@link #received(Connection, Object)} 收到任何对象之前被调用。它会在与 {@link Client#update(int)} 和 {@link Server#update(int)} 相同的线程上被调用。此方法不应长时间阻塞,否则在它返回之前其他网络活动将不会被处理。
     */
    default void connected(Connection connection){
    }

    /**
     * Called when the remote end is no longer connected. There is no guarantee
     * as to what thread will invoke this method.
     * <p>
     * 当远程端不再连接时调用。无法保证由哪个线程调用此方法。
     */
    default void disconnected(Connection connection, DcReason reason){
    }

    /**
     * Called when an object has been received from the remote end of the
     * connection. This will be invoked on the same thread as
     * {@link Client#update(int)} and {@link Server#update(int)}. This method
     * should not block for long periods as other network activity will not be
     * processed until it returns.
     * <p>
     * 当从连接的远程端收到对象时调用。它会在与 {@link Client#update(int)} 和 {@link Server#update(int)} 相同的线程上被调用。此方法不应长时间阻塞,否则在它返回之前其他网络活动将不会被处理。
     */
    default void received(Connection connection, Object object){
    }

    /**
     * Called when the connection is below the
     * {@link Connection#setIdleThreshold(float) idle threshold}.
     * <p>
     * 当连接低于 {@link Connection#setIdleThreshold(float) idle threshold} 时调用。
     */
    default void idle(Connection connection){
    }

    /**
     * Wraps a listener and queues notifications as {@link Runnable runnables}.
     * This allows the runnables to be processed on a different thread,
     * preventing the connection's update thread from being blocked.
     * <p>
     * 包装一个监听器,并将通知排入 {@link Runnable runnables} 队列。这允许在另一个线程上处理这些 runnable,防止连接的更新线程被阻塞。
     */
    abstract class QueuedListener implements NetListener{
        final NetListener listener;

        public QueuedListener(NetListener listener){
            if(listener == null)
                throw new IllegalArgumentException("listener cannot be null.");
            this.listener = listener;
        }

        public void connected(final Connection connection){
            queue(() -> listener.connected(connection));
        }

        public void disconnected(final Connection connection, DcReason reason){
            queue(() -> listener.disconnected(connection, reason));
        }

        public void received(final Connection connection, final Object object){
            queue(() -> listener.received(connection, object));
        }

        public void idle(final Connection connection){
            queue(() -> listener.idle(connection));
        }

        abstract protected void queue(Runnable runnable);
    }

    /**
     * Wraps a listener and processes notification events on a separate thread.
     * <p>
     * 包装一个监听器,并在单独的线程上处理通知事件。
     */
    class ThreadedListener extends QueuedListener{
        protected final ExecutorService threadPool;

        /**
         * Creates a single thread to process notification events.
         * <p>
         * 创建一个单线程来处理通知事件。
         */
        public ThreadedListener(NetListener listener){
            this(listener, Executors.newFixedThreadPool(1));
        }

        /**
         * Uses the specified threadPool to process notification events.
         * <p>
         * 使用指定的 threadPool 处理通知事件。
         */
        public ThreadedListener(NetListener listener, ExecutorService threadPool){
            super(listener);
            if(threadPool == null)
                throw new IllegalArgumentException(
                "threadPool cannot be null.");
            this.threadPool = threadPool;
        }

        public void queue(Runnable runnable){
            threadPool.execute(runnable);
        }
    }

    /**
     * Delays the notification of the wrapped listener to simulate lag on
     * incoming objects. Notification events are processed on a separate thread
     * after a delay. Note that only incoming objects are delayed. To delay
     * outgoing objects, use a LagListener at the other end of the connection.
     * <p>
     * 延迟通知被包装的监听器,以模拟接收对象时的延迟。通知事件会在延迟后于单独的线程上处理。注意,只有传入的对象会被延迟。若要延迟传出的对象,请在连接的另一端使用 LagListener。
     */
    class LagListener extends QueuedListener{
        private final ScheduledExecutorService threadPool;
        private final int lagMillisMin, lagMillisMax;
        final LinkedList<Runnable> runnables = new LinkedList<>();

        public LagListener(int lagMillisMin, int lagMillisMax,
                           NetListener listener){
            super(listener);
            this.lagMillisMin = lagMillisMin;
            this.lagMillisMax = lagMillisMax;
            threadPool = Executors.newScheduledThreadPool(1);
        }

        public void queue(Runnable runnable){
            synchronized(runnables){
                runnables.addFirst(runnable);
            }
            int lag = lagMillisMin + (int)(Math.random() * (lagMillisMax - lagMillisMin));
            threadPool.schedule(() -> {
                Runnable runnable1;
                synchronized(runnables){
                    runnable1 = runnables.removeLast();
                }
                runnable1.run();
            }, lag, TimeUnit.MILLISECONDS);
        }
    }
}
