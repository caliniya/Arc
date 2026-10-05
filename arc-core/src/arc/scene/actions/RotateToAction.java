package arc.scene.actions;

import arc.math.Mathf;

/**
 * Sets the actor's rotation from its current value to a specific value.
 * <p>
 * By default, the rotation will take you from the starting value to the specified value via simple subtraction. For example,
 * setting the start at 350 and the target at 10 will result in 340 degrees of movement.
 * <p>
 * If the action is instead set to useShortestDirection instead, it will rotate straight to the target angle, regardless of where
 * the angle starts and stops. For example, starting at 350 and rotating to 10 will cause 20 degrees of rotation.
 * <p>
 * 将元素的旋转从当前值设置为指定值。 <p> 默认情况下,旋转会通过简单的减法从起始值转到指定值。例如,起始值设为 350、目标值设为 10,将产生 340 度的旋转。 <p> 若将动作改为设置 useShortestDirection,则会直接旋转到目标角度,而不论角度从何处开始和结束。例如,从 350 旋转到 10 将产生 20 度的旋转。
 * @author Nathan Sweet
 */
public class RotateToAction extends TemporalAction{
    private float start, end;

    private boolean useShortestDirection = false;

    public RotateToAction(){
    }

    /** @param useShortestDirection Set to true to move directly to the closest angle 设为 true 则直接移动到最近的角度 */
    public RotateToAction(boolean useShortestDirection){
        this.useShortestDirection = useShortestDirection;
    }

    @Override
    protected void begin(){
        start = target.getRotation();
    }

    @Override
    protected void update(float percent){
        if(useShortestDirection)
            target.setRotation(Mathf.slerp(this.start, this.end, percent));
        else
            target.setRotation(start + (end - start) * percent);
    }

    public float getRotation(){
        return end;
    }

    public void setRotation(float rotation){
        this.end = rotation;
    }

    public boolean isUseShortestDirection(){
        return useShortestDirection;
    }

    public void setUseShortestDirection(boolean useShortestDirection){
        this.useShortestDirection = useShortestDirection;
    }
}
