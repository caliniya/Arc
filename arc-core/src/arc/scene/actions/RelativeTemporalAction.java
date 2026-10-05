package arc.scene.actions;

/**
 * Base class for actions that transition over time using the percent complete since the last frame.
 * <p>
 * 基于自上一帧以来的完成百分比随时间过渡的动作的基类。
 * @author Nathan Sweet
 */
abstract public class RelativeTemporalAction extends TemporalAction{
    private float lastPercent;

    @Override
    protected void begin(){
        lastPercent = 0;
    }

    @Override
    protected void update(float percent){
        updateRelative(percent - lastPercent);
        lastPercent = percent;
    }

    abstract protected void updateRelative(float percentDelta);
}
