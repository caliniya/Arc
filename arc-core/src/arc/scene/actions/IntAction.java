package arc.scene.actions;

/**
 * An action that has an int, whose value is transitioned over time.
 * <p>
 * 拥有一个 int 值的动作,其值随时间过渡。
 * @author Nathan Sweet
 */
public class IntAction extends TemporalAction{
    private int start, end;
    private int value;

    /**
     * Creates an IntAction that transitions from 0 to 1.
     * 创建一个从 0 过渡到 1 的 IntAction。
     */
    public IntAction(){
        start = 0;
        end = 1;
    }

    /**
     * Creates an IntAction that transitions from start to end.
     * 创建一个从 start 过渡到 end 的 IntAction。
     */
    public IntAction(int start, int end){
        this.start = start;
        this.end = end;
    }

    @Override
    protected void begin(){
        value = start;
    }

    @Override
    protected void update(float percent){
        value = (int)(start + (end - start) * percent);
    }

    /**
     * Gets the current int value.
     * 获取当前的 int 值。
     */
    public int getValue(){
        return value;
    }

    /**
     * Sets the current int value.
     * 设置当前的 int 值。
     */
    public void setValue(int value){
        this.value = value;
    }

    public int getStart(){
        return start;
    }

    /**
     * Sets the value to transition from.
     * 设置过渡的起始值。
     */
    public void setStart(int start){
        this.start = start;
    }

    public int getEnd(){
        return end;
    }

    /**
     * Sets the value to transition to.
     * 设置过渡的目标值。
     */
    public void setEnd(int end){
        this.end = end;
    }
}
