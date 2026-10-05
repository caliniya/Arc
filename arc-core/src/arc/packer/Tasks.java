package arc.packer;

import arc.*;
import arc.struct.*;
import arc.util.*;

import java.util.concurrent.*;

/**
 * Small helper for running leaf tasks on {@link Core#executor}.
 * Leaf tasks must never wait on other tasks themselves.
 * <p>
 * 用于在 {@link Core#executor} 上运行叶子任务的小工具。
 * 叶子任务绝不能等待其他任务。
 */
final class Tasks{

    private Tasks(){
    }

    /**
     * @return whether there is any point in scheduling work on other threads. 在其他线程上调度工作是否还有意义。
     */
    static boolean parallel(){
        return OS.cores > 1;
    }

    /**
     * Schedules a task. It must be {@link #join(FutureTask) joined} to get the result.
     * 调度一个任务。必须 {@link #join(FutureTask) 等待(join)} 它才能获得结果。
     */
    static <T> FutureTask<T> submit(Callable<T> callable){
        FutureTask<T> task = new FutureTask<>(callable);
        if(parallel()){
            try{
                Core.executor.execute(task);
            }catch(RejectedExecutionException ignored){
                //executor was shut down; join() will simply run the task on the calling thread
                // 执行器已关闭;join() 将直接在调用线程上运行该任务
            }
        }
        return task;
    }

    /**
     * Waits for a task (running it on this thread if it hasn't started yet) and returns its result.
     * 等待任务(如果尚未开始,则在此线程上运行它)并返回其结果。
     */
    static <T> T join(FutureTask<T> task){
        task.run(); //no-op if already started or finished by another thread
        // 如果已由其他线程启动或完成,则为空操作
        try{
            return task.get();
        }catch(ExecutionException e){
            Throwable cause = e.getCause();
            if(cause instanceof RuntimeException) throw (RuntimeException)cause;
            if(cause instanceof Error) throw (Error)cause;
            throw new ArcRuntimeException(cause);
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new ArcRuntimeException(e);
        }
    }

    /**
     * Joins every task, so nothing is still running when this returns, then rethrows the first failure (in order), if any.
     * 等待每个任务完成,因此此方法返回时没有任何任务仍在运行,然后按顺序重新抛出第一个失败(如有)。
     */
    static void joinAll(Ar<? extends FutureTask<?>> tasks){
        RuntimeException first = null;
        for(FutureTask<?> task : tasks){
            try{
                join(task);
            }catch(RuntimeException e){
                if(first == null) first = e;
            }
        }
        if(first != null) throw first;
    }

    /**
     * Waits until every task is finished, one way or another, ignoring failures.
     * 等待所有任务以某种方式结束,忽略失败。
     */
    static void awaitAll(Ar<? extends FutureTask<?>> tasks){
        for(FutureTask<?> task : tasks){
            try{
                task.run(); //no-op unless it never started, in which case it was cancelled and this does nothing either
                // 空操作,除非它从未启动——那样的话它已被取消,这样做同样没有效果
                task.get();
            }catch(Exception ignored){
            }
        }
    }

    /**
     * Prevents tasks that have not started yet from running.
     * 阻止尚未开始的任务运行。
     */
    static void cancelAll(Ar<? extends FutureTask<?>> tasks){
        for(FutureTask<?> task : tasks){
            task.cancel(false);
        }
    }
}
