package arc.util;

import arc.struct.*;

public class TaskQueue{
    private final Ar<Runnable> runnables = new Ar<>();
    private final Ar<Runnable> executedRunnables = new Ar<>();

    public void run(){
        synchronized(runnables){
            executedRunnables.clear();
            executedRunnables.addAll(runnables);
            runnables.clear();
        }

        for(Runnable runnable : executedRunnables){
            runnable.run();
        }
    }

    public int size(){
        return runnables.size;
    }

    public void clear(){
        synchronized(runnables){
            runnables.clear();
        }
    }

    public void post(Runnable runnable){
        synchronized(runnables){
            runnables.add(runnable);
        }
    }
}
