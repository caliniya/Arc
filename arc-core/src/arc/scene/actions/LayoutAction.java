package arc.scene.actions;

import arc.scene.Action;

/**
 * Sets an actor's {@link Layout#setLayoutEnabled(boolean) layout} to enabled or disabled. The actor must implements
 * {@link Layout}.
 * <p>
 * 将元素的 {@link Layout#setLayoutEnabled(boolean) 布局} 设置为启用或禁用。该元素必须实现 {@link Layout}。
 * @author Nathan Sweet
 */
public class LayoutAction extends Action{
    private boolean enabled;

    @Override
    public boolean act(float delta){
        target.setLayoutEnabled(enabled);
        return true;
    }

    public boolean isEnabled(){
        return enabled;
    }

    public void setLayoutEnabled(boolean enabled){
        this.enabled = enabled;
    }
}
