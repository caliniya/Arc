package arc.util;

import arc.Application;
import arc.ApplicationListener;
import arc.Core;
import arc.Files;
import arc.struct.Ar;

/**
 * Executes tasks in the future on the main loop thread.
 * <p>
 * 在主循环线程上延迟执行任务。
 * @author Nathan Sweet
 */
// TimerThread access is synchronized using threadLock.
// 对 TimerThread 的访问通过 threadLock 同步。
// Timer access is synchronized using the Timer instance.
// 对 Timer 的访问通过 Timer 实例同步。
// Task access is synchronized using the Task instance.
// 对 Task 的访问通过 Task 实例同步。
public class Timer{
    static final Object threadLock = new Object();
    static TimerThread thread;

    final Ar<Task> tasks = new Ar<>(false, 8);

    public Timer(){
        start();
    }

    /**
     * Timer instance singleton for general application wide usage. Static methods on {@link Timer} make convenient use of this
     * instance.
     * <p>
     * 供整个应用程序通用使用的 Timer 单例。{@link Timer} 的静态方法会方便地使用该实例。
     */
    public static Timer instance(){
        synchronized(threadLock){
            TimerThread thread = thread();
            if(thread.instance == null) thread.instance = new Timer();
            return thread.instance;
        }
    }

    private static TimerThread thread(){
        synchronized(threadLock){
            if(thread == null || thread.files != Core.files){
                if(thread != null) thread.dispose();
                thread = new TimerThread();
            }
            return thread;
        }
    }

    /**
     * Schedules a task on {@link #instance}.
     * <p>
     * 在 {@link #instance} 上调度一个任务。
     * @see #postTask(Task)
     */
    public static Task post(Task task){
        return instance().postTask(task);
    }

    /**
     * Schedules a task on {@link #instance}.
     * <p>
     * 在 {@link #instance} 上调度一个任务。
     * @see #scheduleTask(Task, float)
     */
    public static Task schedule(Task task, float delaySeconds){
        return instance().scheduleTask(task, delaySeconds);
    }

    /**
     * Schedules a task on {@link #instance}.
     * <p>
     * 在 {@link #instance} 上调度一个任务。
     * @see #scheduleTask(Task, float, float)
     */
    public static Task schedule(Task task, float delaySeconds, float intervalSeconds){
        return instance().scheduleTask(task, delaySeconds, intervalSeconds);
    }

    /**
     * Schedules a task on {@link #instance}.
     * <p>
     * 在 {@link #instance} 上调度一个任务。
     * @see #scheduleTask(Task, float, float, int)
     */
    public static Task schedule(Task task, float delaySeconds, float intervalSeconds, int repeatCount){
        return instance().scheduleTask(task, delaySeconds, intervalSeconds, repeatCount);
    }

    /**
     * Schedules a task on {@link #instance}.
     * <p>
     * 在 {@link #instance} 上调度一个任务。
     * @see #scheduleTask(Task, float)
     */
    public static Task schedule(Runnable task, float delaySeconds){
        return instance().scheduleTask(new Task(){
            @Override
            public void run(){
                task.run();
            }
        }, delaySeconds);
    }

    /**
     * Schedules a task on {@link #instance}.
     * <p>
     * 在 {@link #instance} 上调度一个任务。
     * @see #scheduleTask(Task, float, float)
     */
    public static Task schedule(Runnable task, float delaySeconds, float intervalSeconds){
        return instance().scheduleTask(new Task(){
            @Override
            public void run(){
                task.run();
            }
        }, delaySeconds, intervalSeconds);
    }

    /**
     * Schedules a task on {@link #instance}.
     * <p>
     * 在 {@link #instance} 上调度一个任务。
     * @see #scheduleTask(Task, float, float, int)
     */
    public static Task schedule(Runnable task, float delaySeconds, float intervalSeconds, int repeatCount){
        return instance().scheduleTask(new Task(){
            @Override
            public void run(){
                task.run();
            }
        }, delaySeconds, intervalSeconds, repeatCount);
    }

    /**
     * Schedules a task to occur once as soon as possible, but not sooner than the start of the next frame.
     * 调度一个尽快执行一次的任务,但不会早于下一帧的开始。
     */
    public Task postTask(Task task){
        return scheduleTask(task, 0, 0, 0);
    }

    /**
     * Schedules a task to occur once after the specified delay.
     * 调度一个在指定延迟后执行一次的任务。
     */
    public Task scheduleTask(Task task, float delaySeconds){
        return scheduleTask(task, delaySeconds, 0, 0);
    }

    /**
     * Schedules a task to occur once after the specified delay and then repeatedly at the specified interval until cancelled.
     * 调度一个在指定延迟后执行一次、然后按指定间隔重复执行直到被取消的任务。
     */
    public Task scheduleTask(Task task, float delaySeconds, float intervalSeconds){
        return scheduleTask(task, delaySeconds, intervalSeconds, -1);
    }

    /**
     * Schedules a task to occur once after the specified delay and then a number of additional times at the specified interval.
     * <p>
     * 调度一个在指定延迟后执行一次、然后按指定间隔再执行若干次的任务。
     * @param repeatCount If negative, the task will repeat forever. 若为负数,任务将永远重复。
     */
    public Task scheduleTask(Task task, float delaySeconds, float intervalSeconds, int repeatCount){
        synchronized(this){
            synchronized(task){
                if(task.timer != null) throw new IllegalArgumentException("The same task may not be scheduled twice.");
                task.timer = this;
                task.executeTimeMillis = System.nanoTime() / 1000000 + (long)(delaySeconds * 1000);
                task.intervalMillis = (long)(intervalSeconds * 1000);
                task.repeatCount = repeatCount;
                tasks.add(task);
            }
        }
        synchronized(threadLock){
            threadLock.notifyAll();
        }
        return task;
    }

    /**
     * Stops the timer, tasks will not be executed and time that passes will not be applied to the task delays.
     * 停止计时器,任务将不会执行,流逝的时间也不会计入任务延迟。
     */
    public void stop(){
        synchronized(threadLock){
            thread().instances.remove(this, true);
        }
    }

    /**
     * Starts the timer if it was stopped.
     * 若计时器已停止则启动它。
     */
    public void start(){
        synchronized(threadLock){
            TimerThread thread = thread();
            Ar<Timer> instances = thread.instances;
            if(instances.contains(this, true)) return;
            instances.add(this);
            threadLock.notifyAll();
        }
    }

    /**
     * Cancels all tasks.
     * 取消所有任务。
     */
    public synchronized void clear(){
        for(int i = 0, n = tasks.size; i < n; i++){
            Task task = tasks.get(i);
            synchronized(task){
                task.executeTimeMillis = 0;
                task.timer = null;
            }
        }
        tasks.clear();
    }

    /**
     * Returns true if the timer has no tasks in the queue. Note that this can change at any time. Synchronize on the timer
     * instance to prevent tasks being added, removed, or updated.
     * <p>
     * 若计时器队列中没有任务则返回 true。注意该状态随时可能变化。请在计时器实例上同步,以防止任务被添加、移除或更新。
     */
    public synchronized boolean isEmpty(){
        return tasks.size == 0;
    }

    synchronized long update(long timeMillis, long waitMillis){
        for(int i = 0, n = tasks.size; i < n; i++){
            Task task = tasks.get(i);
            synchronized(task){
                if(task.executeTimeMillis > timeMillis){
                    waitMillis = Math.min(waitMillis, task.executeTimeMillis - timeMillis);
                    continue;
                }
                if(task.repeatCount == 0){
                    task.timer = null;
                    tasks.remove(i);
                    i--;
                    n--;
                }else{
                    task.executeTimeMillis = timeMillis + task.intervalMillis;
                    waitMillis = Math.min(waitMillis, task.intervalMillis);
                    if(task.repeatCount > 0) task.repeatCount--;
                }
                task.app.post(task);
            }
        }
        return waitMillis;
    }

    /**
     * Adds the specified delay to all tasks.
     * 为所有任务增加指定的延迟。
     */
    public synchronized void delay(long delayMillis){
        for(int i = 0, n = tasks.size; i < n; i++){
            Task task = tasks.get(i);
            synchronized(task){
                task.executeTimeMillis += delayMillis;
            }
        }
    }

    /**
     * Runnable that can be scheduled on a {@link Timer}.
     * <p>
     * 可在 {@link Timer} 上调度的 Runnable。
     * @author Nathan Sweet
     */
    static abstract public class Task implements Runnable{
        final Application app;
        long executeTimeMillis, intervalMillis;
        int repeatCount;
        volatile Timer timer;

        public Task(){
            app = Core.app; // Store which app to post
            // 记录要投递到哪个应用
            if(app == null) throw new IllegalStateException("Core.app not available.");
        }

        /**
         * If this is the last time the task will be ran or the task is first cancelled, it may be scheduled again in this
         * method.
         * <p>
         * 若这是任务最后一次运行,或任务首次被取消,则可以在此方法中重新调度。
         */
        abstract public void run();

        /**
         * Cancels the task. It will not be executed until it is scheduled again. This method can be called at any time.
         * 取消该任务。在再次被调度之前它不会执行。此方法可随时调用。
         */
        public void cancel(){
            Timer timer = this.timer;
            if(timer != null){
                synchronized(timer){
                    synchronized(this){
                        executeTimeMillis = 0;
                        this.timer = null;
                        timer.tasks.remove(this, true);
                    }
                }
            }else{
                synchronized(this){
                    executeTimeMillis = 0;
                    this.timer = null;
                }
            }
        }

        /**
         * Returns true if this task is scheduled to be executed in the future by a timer. The execution time may be reached at any
         * time after calling this method, which may change the scheduled state. To prevent the scheduled state from changing,
         * synchronize on this task object, eg:
         *
         * <pre>
         * synchronized (task) {
         * 	if (!task.isScheduled()) { ... }
         * }
         * </pre>
         * <p>
         * 若此任务已被某个计时器调度为在将来执行,则返回 true。调用此方法后,执行时间随时可能到达,从而改变调度状态。要防止调度状态变化,请在此任务对象上同步,例如:
         * <pre>
         * synchronized (task) {
         * 	if (!task.isScheduled()) { ... }
         * }
         * </pre>
         */
        public boolean isScheduled(){
            return timer != null;
        }

        /**
         * Returns the time in milliseconds when this task will be executed next.
         * 返回此任务下次执行的时间(毫秒)。
         */
        public synchronized long getExecuteTimeMillis(){
            return executeTimeMillis;
        }
    }

    /**
     * Manages a single thread for updating timers. Uses application events to pause, resume, and dispose the thread.
     * <p>
     * 用一个单独的线程管理计时器的更新。使用应用程序事件来暂停、恢复和销毁该线程。
     * @author Nathan Sweet
     */
    static class TimerThread implements Runnable, ApplicationListener{
        final Files files;
        final Ar<Timer> instances = new Ar<>(1);
        Timer instance;
        private long pauseMillis;

        public TimerThread(){
            files = Core.files;
            Core.app.addListener(this);
            resume();

            Thread thread = new Thread(this, "Timer");
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public void run(){
            while(true){
                synchronized(threadLock){
                    if(thread != this || files != Core.files) break;

                    long waitMillis = 5000;
                    if(pauseMillis == 0){
                        long timeMillis = System.nanoTime() / 1000000;
                        for(int i = 0, n = instances.size; i < n; i++){
                            try{
                                waitMillis = instances.get(i).update(timeMillis, waitMillis);
                            }catch(Throwable ex){
                                throw new ArcRuntimeException("Task failed: " + instances.get(i).getClass().getName(), ex);
                            }
                        }
                    }

                    if(thread != this || files != Core.files) break;

                    try{
                        if(waitMillis > 0) threadLock.wait(waitMillis);
                    }catch(InterruptedException ignored){
                    }
                }
            }
            dispose();
        }

        @Override
        public void resume(){
            if(Core.app.isDesktop()) return;
            synchronized(threadLock){
                long delayMillis = System.nanoTime() / 1000000 - pauseMillis;
                for(int i = 0, n = instances.size; i < n; i++)
                    instances.get(i).delay(delayMillis);
                pauseMillis = 0;
                threadLock.notifyAll();
            }
        }

        @Override
        public void pause(){
            //allow tasks to run in the background on desktop
            // 在桌面上允许任务在后台运行
            if(Core.app.isDesktop()) return;
            synchronized(threadLock){
                pauseMillis = System.nanoTime() / 1000000;
                threadLock.notifyAll();
            }
        }

        @Override
        public void dispose(){ // OK to call multiple times.
        // 可以多次调用。
            synchronized(threadLock){
                if(thread == this) thread = null;
                instances.clear();
                threadLock.notifyAll();
            }
            Core.app.removeListener(this);
        }
    }
}
