package arc.scene.actions;

import arc.scene.Action;
import arc.scene.Element;
import arc.util.pooling.Pool;

/**
 * Base class for an action that wraps another action.
 * <p>
 * 包装另一个动作的动作的基类。
 * @author Nathan Sweet
 */
abstract public class DelegateAction extends Action{
    protected Action action;

    public Action getAction(){
        return action;
    }

    /**
     * Sets the wrapped action.
     * 设置被包装的动作。
     */
    public void setAction(Action action){
        this.action = action;
    }

    abstract protected boolean delegate(float delta);

    @Override
    public final boolean act(float delta){
        Pool pool = getPool();
        setPool(null); // Ensure this action can't be returned to the pool inside the delegate action.
        // 确保此动作在被包装动作执行期间不会被归还到池中。
        try{
            return delegate(delta);
        }finally{
            setPool(pool);
        }
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

    @Override
    public void setActor(Element actor){
        if(action != null) action.setActor(actor);
        super.setActor(actor);
    }

    @Override
    public void setTarget(Element target){
        if(action != null) action.setTarget(target);
        super.setTarget(target);
    }

    public String toString(){
        return super.toString() + (action == null ? "" : "(" + action + ")");
    }
}
