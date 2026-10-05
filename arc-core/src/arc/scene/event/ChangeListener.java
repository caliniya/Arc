package arc.scene.event;

import arc.scene.Element;

/**
 * Listener for {@link ChangeEvent}.
 * <p>
 * {@link ChangeEvent} 的监听器。
 * @author Nathan Sweet
 */
abstract public class ChangeListener implements EventListener{
    @Override
    public boolean handle(SceneEvent event){
        if(!(event instanceof ChangeEvent)) return false;
        changed((ChangeEvent)event, event.targetActor);
        return false;
    }

    /** @param actor The event target, which is the actor that emitted the change event. 事件目标,即发出该 change 事件的元素。 */
    abstract public void changed(ChangeEvent event, Element actor);

    /**
     * Fired when something in an actor has changed. This is a generic event, exactly what changed in an actor will vary.
     * <p>
     * 当元素中的某些内容发生变化时触发。这是一个通用事件,元素中具体改变了什么会有所不同。
     * @author Nathan Sweet
     */
    public static class ChangeEvent extends SceneEvent{
    }
}
