package arc.scene.event;

/**
 * Low level interface for receiving events. Typically there is a listener class for each specific event class.
 * <p>
 * 用于接收事件的底层接口。通常每个具体的事件类都对应一个监听器类。
 * @author Nathan Sweet
 * @see InputListener
 * @see InputEvent
 */
public interface EventListener{
    /**
     * Try to handle the given event, if it is applicable.
     * <p>
     * 尝试处理给定的事件(如果适用)。
     * @return true if the event should be considered {@link SceneEvent#handle() handled} by scene2d. 如果该事件应被 scene2d 视为已处理,则返回 true。
     */
    boolean handle(SceneEvent event);
}
