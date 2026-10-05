package arc.scene.actions;

import arc.scene.Action;
import arc.util.pooling.Pool;

/**
 * An action that runs a {@link Runnable}. Alternatively, the {@link #run()} method can be overridden instead of setting a
 * runnable.
 * <p>
 * 执行一个 {@link Runnable} 的动作。也可以不设置 runnable,而是重写 {@link #run()} 方法。
 * @author Nathan Sweet
 */
public class RunnableAction extends Action{
    private Runnable runnable;
    private boolean ran;

    @Override
    public boolean act(float delta){
        if(!ran){
            ran = true;
            run();
        }
        return true;
    }

    /**
     * Called to run the runnable.
     * 被调用以执行 runnable。
     */
    public void run(){
        Pool pool = getPool();
        setPool(null); // Ensure this action can't be returned to the pool inside the runnable.
        // 确保此动作在 runnable 执行期间不会被归还到池中。
        try{
            runnable.run();
        }finally{
            setPool(pool);
        }
    }

    @Override
    public void restart(){
        ran = false;
    }

    @Override
    public void reset(){
        super.reset();
        runnable = null;
    }

    public Runnable getRunnable(){
        return runnable;
    }

    public void setRunnable(Runnable runnable){
        this.runnable = runnable;
    }
}
