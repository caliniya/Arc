package arc.scene.actions;

import arc.scene.*;
import arc.scene.event.Touchable;

/**
 * Sets the actor's {@link Element#touchable(Touchable) touchability}.
 * <p>
 * 设置元素的 {@link Element#touchable(Touchable) 可触碰性}。
 * @author Nathan Sweet
 */
public class TouchableAction extends Action{
    private Touchable touchable;

    @Override
    public boolean act(float delta){
        target.touchable = touchable;
        return true;
    }

    public Touchable getTouchable(){
        return touchable;
    }

    public void touchable(Touchable touchable){
        this.touchable = touchable;
    }
}
