package arc.scene.actions;

import arc.scene.Action;

/**
 * Removes an actor from the stage.
 * <p>
 * 从舞台移除一个元素。
 * @author Nathan Sweet
 */
public class RemoveActorAction extends Action{
    private boolean removed;

    @Override
    public boolean act(float delta){
        if(!removed){
            removed = true;
            target.remove();
        }
        return true;
    }

    @Override
    public void restart(){
        removed = false;
    }
}
