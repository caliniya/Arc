package arc.scene.actions;

import arc.scene.Action;

/**
 * Removes an action from an actor.
 * <p>
 * 从元素移除一个动作。
 * @author Nathan Sweet
 */
public class RemoveAction extends Action{
    private Action action;

    @Override
    public boolean act(float delta){
        target.removeAction(action);
        return true;
    }

    public Action getAction(){
        return action;
    }

    public void setAction(Action action){
        this.action = action;
    }

    @Override
    public void reset(){
        super.reset();
        action = null;
    }
}
