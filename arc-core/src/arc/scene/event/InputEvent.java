package arc.scene.event;

import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.Scene;

/**
 * Event for actor input: touch, mouse, keyboard, and scroll.
 * <p>
 * 元素输入事件:触摸、鼠标、键盘和滚动。
 * @see InputListener
 */
public class InputEvent extends SceneEvent{
    public InputEventType type;
    public float stageX, stageY;
    public int pointer;
    public float scrollAmountX, scrollAmountY;
    public KeyCode keyCode;
    public char character;
    public Element relatedActor;

    @Override
    public void reset(){
        super.reset();
        relatedActor = null;
    }

    /**
     * Sets actorCoords to this event's coordinates relative to the specified actor.
     * <p>
     * 将 actorCoords 设置为此事件相对于指定元素的坐标。
     * @param actorCoords Output for resulting coordinates. 用于存放结果坐标的输出参数。
     */
    public Vec2 toCoordinates(Element actor, Vec2 actorCoords){
        actorCoords.set(stageX, stageY);
        actor.stageToLocalCoordinates(actorCoords);
        return actorCoords;
    }

    /**
     * Returns true of this event is a touchUp triggered by {@link Scene#cancelTouchFocus()}.
     * 如果此事件是由 {@link Scene#cancelTouchFocus()} 触发的 touchUp,则返回 true。
     */
    public boolean isTouchFocusCancel(){
        return stageX == Integer.MIN_VALUE || stageY == Integer.MIN_VALUE;
    }

    @Override
    public String toString(){
        return type.toString();
    }

    /**
     * Types of low-level input events supported by scene2d.
     * scene2d 支持的底层输入事件类型。
     */
    public enum InputEventType{
        /**
         * A new touch for a pointer on the stage was detected
         * 检测到指针在舞台上的一次新的触摸
         */
        touchDown,
        /**
         * A pointer has stopped touching the stage.
         * 指针已停止触摸舞台。
         */
        touchUp,
        /**
         * A pointer that is touching the stage has moved.
         * 正在触摸舞台的指针发生了移动。
         */
        touchDragged,
        /**
         * The mouse pointer has moved (without a mouse button being active).
         * 鼠标指针发生了移动(没有任何鼠标按键处于按下状态)。
         */
        mouseMoved,
        /**
         * The mouse pointer or an active touch have entered (i.e., {@link Element#hit(float, float, boolean) hit}) an actor.
         * 鼠标指针或处于活动状态的触摸进入了某个元素(即 {@link Element#hit(float, float, boolean) hit})。
         */
        enter,
        /**
         * The mouse pointer or an active touch have exited an actor.
         * 鼠标指针或处于活动状态的触摸离开了某个元素。
         */
        exit,
        /**
         * The mouse scroll wheel has changed.
         * 鼠标滚轮发生了滚动。
         */
        scrolled,
        /**
         * A keyboard key has been pressed.
         * 键盘按键被按下。
         */
        keyDown,
        /**
         * A keyboard key has been released.
         * 键盘按键被释放。
         */
        keyUp,
        /**
         * A keyboard key has been pressed and released.
         * 键盘按键被按下并释放。
         */
        keyTyped
    }
}
