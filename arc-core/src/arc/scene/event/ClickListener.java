package arc.scene.event;

import arc.Core;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.util.*;

/**
 * Detects mouse over, mouse or finger touch presses, and clicks on an element. A touch must go down over the element and is
 * considered pressed as long as it is over the element or within the {@link #setTapSquareSize(float) tap square}. This behavior
 * makes it easier to press buttons on a touch interface when the initial touch happens near the edge of the element. Double clicks
 * can be detected using {@link #getTapCount()}. Any touch (not just the first) will trigger this listener. While pressed, other
 * touch downs are ignored.
 * <p>
 * 检测鼠标悬停、鼠标或手指按下,以及元素上的点击。触摸必须在元素上方按下,并且只要仍位于元素上或 {@link #setTapSquareSize(float) tap square} 内,就被视为处于按下状态。这种行为使得当初始触摸发生在元素边缘附近时,在触摸界面上更容易按到按钮。可以使用 {@link #getTapCount()} 检测双击。任何触摸(不仅是第一次)都会触发此监听器。按下期间,其他触摸按下事件将被忽略。
 * @author Nathan Sweet
 */
public class ClickListener extends InputListener{
    /**
     * Time in seconds {@link #isVisualPressed()} reports true after a press resulting in a click is released.
     * 在导致点击的按压释放之后,{@link #isVisualPressed()} 持续报告 true 的时长(秒)。
     */
    public static float visualPressedDuration = 0.1f;
    public static Runnable clicked = () -> {};

    protected float tapSquareSize = 14, touchDownX = -1, touchDownY = -1;
    protected int pressedPointer = -1;
    protected KeyCode pressedButton;
    protected KeyCode button = KeyCode.mouseLeft;
    protected boolean pressed, over, overAny, cancelled;
    protected long visualPressedTime;
    protected long tapCountInterval = (long)(0.4f * 1000000000L);
    protected int tapCount;
    protected long lastTapTime;
    protected boolean stop = false;

    /**
     * Create a listener where {@link #clicked(InputEvent, float, float)} is only called for left clicks.
     * 创建一个监听器,{@link #clicked(InputEvent, float, float)} 仅在左键点击时被调用。
     */
    public ClickListener(){}

    public ClickListener(KeyCode button){
        this.button = button;
    }

    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
        if(pressed) return false;
        if(pointer == 0 && this.button != null && button != this.button) return false;
        pressed = true;
        pressedPointer = pointer;
        pressedButton = button;
        touchDownX = x;
        touchDownY = y;
        visualPressedTime = Time.millis() + (long)(visualPressedDuration * 1000);
        return true;
    }

    @Override
    public void touchDragged(InputEvent event, float x, float y, int pointer){
        if(pointer != pressedPointer || cancelled) return;
        pressed = isOver(event.listenerActor, x, y);
        if(pressed && pointer == 0 && button != null && !Core.input.keyDown(button)) pressed = false;
        if(!pressed){
            // Once outside the tap square, don't use the tap square anymore.
            // 一旦超出点按方形区域,就不再使用该区域。
            invalidateTapSquare();
        }
    }

    @Override
    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
        if(pointer == pressedPointer){
            if(!cancelled){
                boolean touchUpOver = isOver(event.listenerActor, x, y);
                // Ignore touch up if the wrong mouse button.
                // 如果不是指定的鼠标按键,则忽略抬起事件。
                if(touchUpOver && pointer == 0 && this.button != null && button != this.button) touchUpOver = false;
                if(touchUpOver){
                    long time = Time.nanos();
                    if(time - lastTapTime > tapCountInterval) tapCount = 0;
                    tapCount++;
                    lastTapTime = time;

                    clicked.run();
                    clicked(event, x, y);
                }
            }
            pressed = false;
            pressedPointer = -1;
            pressedButton = null;
            cancelled = false;
        }
    }

    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
        if(pointer == -1 && !cancelled) over = true;
        if(!cancelled) overAny = true;
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
        if(pointer == -1 && !cancelled) over = false;
        if(!cancelled) overAny = false;
    }

    /**
     * If a touch down is being monitored, the drag and touch up events are ignored until the next touch up.
     * 如果正在监测一次触摸按下,则在下次触摸抬起之前,拖动和触摸抬起事件将被忽略。
     */
    public void cancel(){
        if(pressedPointer == -1) return;
        cancelled = true;
        pressed = false;
    }

    public void clicked(InputEvent event, float x, float y){
    }

    /**
     * Returns true if the specified position is over the specified element or within the tap square.
     * 如果指定位置位于指定元素上或点按方形区域内,则返回 true。
     */
    public boolean isOver(Element element, float x, float y){
        element.localToStageCoordinates(Tmp.v1.set(x, y));
        Element hit = Core.scene.hit(Tmp.v1.x, Tmp.v1.y, true);
        return hit != null && hit.isDescendantOf(element);
    }

    public boolean inTapSquare(float x, float y){
        return (!(touchDownX == -1) || !(touchDownY == -1)) && Math.abs(x - touchDownX) < tapSquareSize && Math.abs(y - touchDownY) < tapSquareSize;
    }

    /**
     * Returns true if a touch is within the tap square.
     * 如果有触摸位于点按方形区域内,则返回 true。
     */
    public boolean inTapSquare(){
        return touchDownX != -1;
    }

    /**
     * The tap square will not longer be used for the current touch.
     * 当前触摸将不再使用该点按方形区域。
     */
    public void invalidateTapSquare(){
        touchDownX = -1;
        touchDownY = -1;
    }

    /**
     * Returns true if a touch is over the element or within the tap square.
     * 如果有触摸位于元素上或点按方形区域内,则返回 true。
     */
    public boolean isPressed(){
        return pressed;
    }

    /**
     * Returns true if a touch is over the element or within the tap square or has been very recently. This allows the UI to show a
     * press and release that was so fast it occurred within a single frame.
     * <p>
     * 如果触摸当前位于元素上或点按方形区域内,或刚刚才离开,则返回 true。这允许 UI 显示一次快到在单帧内完成的按下和释放。
     */
    public boolean isVisualPressed(){
        if(pressed) return true;
        if(visualPressedTime <= 0) return false;
        if(visualPressedTime > Time.millis()) return true;
        visualPressedTime = 0;
        return false;
    }

    /**
     * Returns true if the mouse or touch is over the element or pressed and within the tap square.
     * 如果鼠标或触摸位于元素上,或已按下且位于点按方形区域内,则返回 true。
     */
    public boolean isOver(){
        return over || pressed;
    }

    public float getTapSquareSize(){
        return tapSquareSize;
    }

    public void setTapSquareSize(float halfTapSquareSize){
        tapSquareSize = halfTapSquareSize;
    }

    /** @param tapCountInterval time in seconds that must pass for two touch down/up sequences to be detected as consecutive taps. 两次按下/抬起序列被判定为连续点按所需经过的时间(秒)。 */
    public void setTapCountInterval(float tapCountInterval){
        this.tapCountInterval = (long)(tapCountInterval * 1000000000L);
    }

    /**
     * Returns the number of taps within the tap count interval for the most recent click event.
     * 返回最近一次点击事件中,点按计数间隔内的点按次数。
     */
    public int getTapCount(){
        return tapCount;
    }

    public void setTapCount(int tapCount){
        this.tapCount = tapCount;
    }

    public float getTouchDownX(){
        return touchDownX;
    }

    public float getTouchDownY(){
        return touchDownY;
    }

    /**
     * The button that initially pressed this button or -1 if the button is not pressed.
     * 最初按下该按钮的按键,如果按钮未被按下则为 -1。
     */
    public KeyCode getPressedButton(){
        return pressedButton;
    }

    /**
     * The pointer that initially pressed this button or -1 if the button is not pressed.
     * 最初按下该按钮的指针,如果按钮未被按下则为 -1。
     */
    public int getPressedPointer(){
        return pressedPointer;
    }

    public KeyCode getButton(){
        return button;
    }

    /**
     * Sets the button to listen for, all other buttons are ignored. Use null for any button.
     * 设置要监听的按键,其他按键将被忽略。传 null 表示任意按键。
     */
    public void setButton(KeyCode button){
        this.button = button;
    }
}
