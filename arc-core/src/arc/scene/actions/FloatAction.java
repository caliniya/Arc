package arc.scene.actions;

/**
 * An action that has a float, whose value is transitioned over time.
 * <p>
 * 拥有一个 float 值的动作,其值随时间过渡。
 * @author Nathan Sweet
 */
public class FloatAction extends TemporalAction{
    private float start, end;
    private float value;

    /**
     * Creates a FloatAction that transitions from 0 to 1.
     * 创建一个从 0 过渡到 1 的 FloatAction。
     */
    public FloatAction(){
        start = 0;
        end = 1;
    }

    /**
     * Creates a FloatAction that transitions from start to end.
     * 创建一个从 start 过渡到 end 的 FloatAction。
     */
    public FloatAction(float start, float end){
        this.start = start;
        this.end = end;
    }

    @Override
    protected void begin(){
        value = start;
    }

    @Override
    protected void update(float percent){
        value = start + (end - start) * percent;
    }

    /**
     * Gets the current float value.
     * 获取当前的 float 值。
     */
    public float getValue(){
        return value;
    }

    /**
     * Sets the current float value.
     * 设置当前的 float 值。
     */
    public void setValue(float value){
        this.value = value;
    }

    public float getStart(){
        return start;
    }

    /**
     * Sets the value to transition from.
     * 设置过渡的起始值。
     */
    public void setStart(float start){
        this.start = start;
    }

    public float getEnd(){
        return end;
    }

    /**
     * Sets the value to transition to.
     * 设置过渡的目标值。
     */
    public void setEnd(float end){
        this.end = end;
    }
}
