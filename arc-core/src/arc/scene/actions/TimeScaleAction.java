package arc.scene.actions;

/**
 * Multiplies the delta of an action.
 * <p>
 * 将动作的时间增量乘以一个系数。
 * @author Nathan Sweet
 */
public class TimeScaleAction extends DelegateAction{
    private float scale;

    @Override
    protected boolean delegate(float delta){
        return action == null || action.act(delta * scale);
    }

    public float getScale(){
        return scale;
    }

    public void setScale(float scale){
        this.scale = scale;
    }
}
