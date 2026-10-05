package arc.scene.event;

import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;

/**
 * EventListener for low-level input events. Unpacks {@link InputEvent}s and calls the appropriate method. By default the methods
 * here do nothing with the event. Users are expected to override the methods they are interested in, like this:
 * <p>
 * 用于底层输入事件的 EventListener。解包 {@link InputEvent} 并调用相应的方法。默认情况下,这里的方法不会对事件做任何处理。用户应按如下方式重写自己感兴趣的方法:
 */
public class InputListener implements EventListener{
    private static final Vec2 tmpCoords = new Vec2();

    @Override
    public boolean handle(SceneEvent e){
        if(!(e instanceof InputEvent)) return false;
        InputEvent event = (InputEvent)e;

        switch(event.type){
            case keyDown: return keyDown(event, event.keyCode);
            case keyUp: return keyUp(event, event.keyCode);
            case keyTyped: return keyTyped(event, event.character);
        }

        event.toCoordinates(event.listenerActor, tmpCoords);

        switch(event.type){
            case touchDown:
                return touchDown(event, tmpCoords.x, tmpCoords.y, event.pointer, event.keyCode);
            case touchUp:
                touchUp(event, tmpCoords.x, tmpCoords.y, event.pointer, event.keyCode);
                return true;
            case touchDragged:
                touchDragged(event, tmpCoords.x, tmpCoords.y, event.pointer);
                return true;
            case mouseMoved:
                return mouseMoved(event, tmpCoords.x, tmpCoords.y);
            case scrolled:
                return scrolled(event, tmpCoords.x, tmpCoords.y, event.scrollAmountX, event.scrollAmountY);
            case enter:
                enter(event, tmpCoords.x, tmpCoords.y, event.pointer, event.relatedActor);
                return false;
            case exit:
                exit(event, tmpCoords.x, tmpCoords.y, event.pointer, event.relatedActor);
                return false;
        }
        return false;
    }

    /**
     * Called when a mouse button or a finger touch goes down on the element. If true is returned, this listener will receive all
     * touchDragged and touchUp events, even those not over this element, until touchUp is received. Also when true is returned, the
     * event is {@link SceneEvent#handle() handled}.
     * <p>
     * 当鼠标按键或手指触摸在元素上按下时调用。如果返回 true,此监听器将接收后续所有的 touchDragged 和 touchUp 事件,即使是发生在该元素之外的,直到收到 touchUp 为止。此外,返回 true 时,该事件会被 {@link SceneEvent#handle() handled}。
     * @see InputEvent
     */
    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
        return false;
    }

    /**
     * Called when a mouse button or a finger touch goes up anywhere, but only if touchDown previously returned true for the mouse
     * button or touch. The touchUp event is always {@link SceneEvent#handle() handled}.
     * <p>
     * 当鼠标按键或手指触摸在任意位置抬起时调用,但仅当之前 touchDown 对该鼠标按键或触摸返回 true 时。touchUp 事件总是会被 {@link SceneEvent#handle() handled}。
     * @see InputEvent
     */
    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
    }

    /**
     * Called when a mouse button or a finger touch is moved anywhere, but only if touchDown previously returned true for the mouse
     * button or touch. The touchDragged event is always {@link SceneEvent#handle() handled}.
     * <p>
     * 当鼠标按键或手指触摸在任意位置移动时调用,但仅当之前 touchDown 对该鼠标按键或触摸返回 true 时。touchDragged 事件总是会被 {@link SceneEvent#handle() handled}。
     * @see InputEvent
     */
    public void touchDragged(InputEvent event, float x, float y, int pointer){
    }

    /**
     * Called any time the mouse is moved when a button is not down. This event only occurs on the desktop. When true is returned,
     * the event is {@link SceneEvent#handle() handled}.
     * <p>
     * 在没有按键按下的情况下,鼠标每次移动时都会调用。此事件仅发生在桌面端。返回 true 时,该事件会被 {@link SceneEvent#handle() handled}。
     * @see InputEvent
     */
    public boolean mouseMoved(InputEvent event, float x, float y){
        return false;
    }

    /**
     * Called any time the mouse cursor or a finger touch is moved over an element. On the desktop, this event occurs even when no
     * mouse buttons are pressed (pointer will be -1).
     * <p>
     * 当鼠标光标或手指触摸移动到某个元素上时调用。在桌面端,即使没有按下任何鼠标按键,此事件也会发生(pointer 将为 -1)。
     * @param fromActor May be null. 可能为 null。
     * @see InputEvent
     */
    public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
    }

    /**
     * Called any time the mouse cursor or a finger touch is moved out of an element. On the desktop, this event occurs even when no
     * mouse buttons are pressed (pointer will be -1).
     * <p>
     * 当鼠标光标或手指触摸移出某个元素时调用。在桌面端,即使没有按下任何鼠标按键,此事件也会发生(pointer 将为 -1)。
     * @param toActor May be null. 可能为 null。
     * @see InputEvent
     */
    public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
    }

    /**
     * Called when the mouse wheel has been scrolled. When true is returned, the event is {@link SceneEvent#handle() handled}.
     * 当鼠标滚轮滚动时调用。返回 true 时,该事件会被 {@link SceneEvent#handle() handled}。
     */
    public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
        return false;
    }

    /**
     * Called when a key goes down. When true is returned, the event is {@link SceneEvent#handle() handled}.
     * 当按键按下时调用。返回 true 时,该事件会被 {@link SceneEvent#handle() handled}。
     */
    public boolean keyDown(InputEvent event, KeyCode keycode){
        return false;
    }

    /**
     * Called when a key goes up. When true is returned, the event is {@link SceneEvent#handle() handled}.
     * 当按键释放时调用。返回 true 时,该事件会被 {@link SceneEvent#handle() handled}。
     */
    public boolean keyUp(InputEvent event, KeyCode keycode){
        return false;
    }

    /**
     * Called when a key is typed. When true is returned, the event is {@link SceneEvent#handle() handled}.
     * 当键入字符时调用。返回 true 时,该事件会被 {@link SceneEvent#handle() handled}。
     */
    public boolean keyTyped(InputEvent event, char character){
        return false;
    }
}
