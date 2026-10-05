package arc.scene.actions;

import arc.scene.Action;

/**
 * Adds an action to an actor.
 * <p>
 * 向元素添加一个动作。
 * @author Nathan Sweet
 */
public class AddAction extends Action{
    private Action action;

    @Override
    public boolean act(float delta){
        target.addAction(action);
        return true;
    }

    public Action getAction(){
        return action;
    }

    public void setAction(Action action){
        this.action = action;
    }

    @Override
    public void restart(){
        if(action != null) action.restart();
    }

    @Override
    public void reset(){
        super.reset();
        action = null;
    }
}
