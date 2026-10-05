package arc.scene.actions;

/**
 * Sets the actor's rotation from its current value to a relative value.
 * <p>
 * 将元素的旋转从当前值设置为相对值。
 * @author Nathan Sweet
 */
public class RotateByAction extends RelativeTemporalAction{
    private float amount;

    @Override
    protected void updateRelative(float percentDelta){
        target.rotateBy(amount * percentDelta);
    }

    public float getAmount(){
        return amount;
    }

    public void setAmount(float rotationAmount){
        amount = rotationAmount;
    }
}
