package arc.scene.actions;

/**
 * Delays execution of an action or inserts a pause in a {@link SequenceAction}.
 * <p>
 * 延迟动作的执行,或在 {@link SequenceAction} 中插入一段停顿。
 * @author Nathan Sweet
 */
public class DelayAction extends DelegateAction{
    private float duration, time;

    public DelayAction(){
    }

    public DelayAction(float duration){
        this.duration = duration;
    }

    @Override
    protected boolean delegate(float delta){
        if(time < duration){
            time += delta;
            if(time < duration) return false;
            delta = time - duration;
        }
        return action == null || action.act(delta);
    }

    /**
     * Causes the delay to be complete.
     * 使延迟立即完成。
     */
    public void finish(){
        time = duration;
    }

    @Override
    public void restart(){
        super.restart();
        time = 0;
    }

    /**
     * Gets the time spent waiting for the delay.
     * 获取等待延迟已花费的时间。
     */
    public float getTime(){
        return time;
    }

    /**
     * Sets the time spent waiting for the delay.
     * 设置等待延迟已花费的时间。
     */
    public void setTime(float time){
        this.time = time;
    }

    public float getDuration(){
        return duration;
    }

    /**
     * Sets the length of the delay in seconds.
     * 以秒为单位设置延迟时长。
     */
    public void setDuration(float duration){
        this.duration = duration;
    }
}
