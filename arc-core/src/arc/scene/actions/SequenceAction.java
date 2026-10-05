package arc.scene.actions;

import arc.scene.Action;
import arc.util.pooling.Pool;

/**
 * Executes a number of actions one at a time.
 * <p>
 * 依次执行多个动作。
 * @author Nathan Sweet
 */
public class SequenceAction extends ParallelAction{
    private int index;

    public SequenceAction(){
    }

    public SequenceAction(Action action1){
        addAction(action1);
    }

    public SequenceAction(Action action1, Action action2){
        addAction(action1);
        addAction(action2);
    }

    public SequenceAction(Action action1, Action action2, Action action3){
        addAction(action1);
        addAction(action2);
        addAction(action3);
    }

    public SequenceAction(Action action1, Action action2, Action action3, Action action4){
        addAction(action1);
        addAction(action2);
        addAction(action3);
        addAction(action4);
    }

    public SequenceAction(Action action1, Action action2, Action action3, Action action4, Action action5){
        addAction(action1);
        addAction(action2);
        addAction(action3);
        addAction(action4);
        addAction(action5);
    }

    @Override
    public boolean act(float delta){
        if(index >= actions.size) return true;
        Pool pool = getPool();
        setPool(null); // Ensure this action can't be returned to the pool while executings.
        // 确保此动作在执行期间不会被归还到池中。
        try{
            if(actions.get(index).act(delta)){
                if(actor == null) return true; // This action was removed.
                // 此动作已被移除。
                index++;
                return index >= actions.size;
            }
            return false;
        }finally{
            setPool(pool);
        }
    }

    @Override
    public void restart(){
        super.restart();
        index = 0;
    }
}
