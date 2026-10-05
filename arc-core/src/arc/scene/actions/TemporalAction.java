package arc.scene.actions;

import arc.math.Interp;
import arc.scene.Action;
import arc.util.pooling.Pool;

/**
 * Base class for actions that transition over time using the percent complete.
 * <p>
 * 基于完成百分比随时间过渡的动作的基类。
 * @author Nathan Sweet
 */
abstract public class TemporalAction extends Action{
    private float duration, time;
    private Interp interpolation;
    private boolean reverse, began, complete;

    public TemporalAction(){
    }

    public TemporalAction(float duration){
        this.duration = duration;
    }

    public TemporalAction(float duration, Interp interpolation){
        this.duration = duration;
        this.interpolation = interpolation;
    }

    @Override
    public boolean act(float delta){
        if(complete) return true;
        Pool pool = getPool();
        setPool(null); // Ensure this action can't be returned to the pool while executing.
        // 确保此动作在执行期间不会被归还到池中。
        try{
            if(!began){
                begin();
                began = true;
            }
            time += delta;
            complete = time >= duration;
            float percent;
            if(complete)
                percent = 1;
            else{
                percent = time / duration;
                if(interpolation != null) percent = interpolation.apply(percent);
            }
            update(reverse ? 1 - percent : percent);
            if(complete) end();
            return complete;
        }finally{
            setPool(pool);
        }
    }

    /**
     * Called the first time {@link #act(float)} is called. This is a good place to query the {@link #actor actor's} starting
     * state.
     * <p>
     * 在 {@link #act(float)} 第一次被调用时调用。这里是查询 {@link #actor 元素} 初始状态的好地方。
     */
    protected void begin(){
    }

    /**
     * Called the last time {@link #act(float)} is called.
     * 在 {@link #act(float)} 最后一次被调用时调用。
     */
    protected void end(){
    }

    /**
     * Called each frame.
     * <p>
     * 每帧调用。
     * @param percent The percentage of completion for this action, growing from 0 to 1 over the duration. If 此动作的完成百分比,随持续时间从 0 增长到 1。
     * {@link #setReverse(boolean) reversed}, this will shrink from 1 to 0. 若 {@link #setReverse(boolean) 反转},则将从 1 缩减到 0。
     */
    abstract protected void update(float percent);

    /**
     * Skips to the end of the transition.
     * 跳到过渡的末尾。
     */
    public void finish(){
        time = duration;
    }

    @Override
    public void restart(){
        time = 0;
        began = false;
        complete = false;
    }

    @Override
    public void reset(){
        super.reset();
        reverse = false;
        interpolation = null;
    }

    /**
     * Gets the transition time so far.
     * 获取迄今为止的过渡时间。
     */
    public float getTime(){
        return time;
    }

    /**
     * Sets the transition time so far.
     * 设置迄今为止的过渡时间。
     */
    public void setTime(float time){
        this.time = time;
    }

    public float getDuration(){
        return duration;
    }

    /**
     * Sets the length of the transition in seconds.
     * 以秒为单位设置过渡时长。
     */
    public void setDuration(float duration){
        this.duration = duration;
    }

    public Interp getInterpolation(){
        return interpolation;
    }

    public void setInterpolation(Interp interpolation){
        this.interpolation = interpolation;
    }

    public boolean isReverse(){
        return reverse;
    }

    /**
     * When true, the action's progress will go from 100% to 0%.
     * 若为 true,动作的进度将从 100% 变为 0%。
     */
    public void setReverse(boolean reverse){
        this.reverse = reverse;
    }
}
