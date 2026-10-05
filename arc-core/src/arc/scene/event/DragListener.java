package arc.scene.event;

/**
 * Detects mouse or finger touch drags on an element. A touch must go down over the element and a drag won't start until it is moved
 * outside the {@link #setTapSquareSize(float) tap square}. Any touch (not just the first) will trigger this listener. While
 * pressed, other touch downs are ignored.
 * <p>
 * 检测元素上的鼠标或手指触摸拖动。触摸必须在元素上方按下,且移动到 {@link #setTapSquareSize(float) tap square} 之外后拖动才会开始。任何触摸(不仅是第一次)都会触发此监听器。按下期间,其他触摸按下事件将被忽略。
 * @author Nathan Sweet
 */
public class DragListener extends InputListener{
    private float tapSquareSize = 14, touchDownX = -1, touchDownY = -1, stageTouchDownX = -1, stageTouchDownY = -1;
    private int pressedPointer = -1;
    private int button;
    private boolean dragging;
    private float deltaX, deltaY;

    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button){
        if(pressedPointer != -1) return false;
        if(pointer == 0 && this.button != -1 && button != this.button) return false;
        pressedPointer = pointer;
        touchDownX = x;
        touchDownY = y;
        stageTouchDownX = event.stageX;
        stageTouchDownY = event.stageY;
        return true;
    }

    @Override
    public void touchDragged(InputEvent event, float x, float y, int pointer){
        if(pointer != pressedPointer) return;
        if(!dragging && (Math.abs(touchDownX - x) > tapSquareSize || Math.abs(touchDownY - y) > tapSquareSize)){
            dragging = true;
            dragStart(event, x, y, pointer);
            deltaX = x;
            deltaY = y;
        }
        if(dragging){
            deltaX -= x;
            deltaY -= y;
            drag(event, x, y, pointer);
            deltaX = x;
            deltaY = y;
        }
    }

    public void touchUp(InputEvent event, float x, float y, int pointer, int button){
        if(pointer == pressedPointer){
            if(dragging) dragStop(event, x, y, pointer);
            cancel();
        }
    }

    public void dragStart(InputEvent event, float x, float y, int pointer){
    }

    public void drag(InputEvent event, float x, float y, int pointer){
    }

    public void dragStop(InputEvent event, float x, float y, int pointer){
    }

    /* If a drag is in progress, no further drag methods will be called until a new drag is started.
     如果拖动正在进行,则在新的拖动开始之前,不会再调用任何拖动方法。 */
    public void cancel(){
        dragging = false;
        pressedPointer = -1;
    }

    /**
     * Returns true if a touch has been dragged outside the tap square.
     * 如果触摸已被拖动到点按方形区域之外,则返回 true。
     */
    public boolean isDragging(){
        return dragging;
    }

    public float getTapSquareSize(){
        return tapSquareSize;
    }

    public void setTapSquareSize(float halfTapSquareSize){
        tapSquareSize = halfTapSquareSize;
    }

    public float getTouchDownX(){
        return touchDownX;
    }

    public float getTouchDownY(){
        return touchDownY;
    }

    public float getStageTouchDownX(){
        return stageTouchDownX;
    }

    public float getStageTouchDownY(){
        return stageTouchDownY;
    }

    /**
     * Returns the amount on the x axis that the touch has been dragged since the last drag event.
     * 返回自上次拖动事件以来,触摸在 x 轴上被拖动的距离。
     */
    public float getDeltaX(){
        return deltaX;
    }

    /**
     * Returns the amount on the y axis that the touch has been dragged since the last drag event.
     * 返回自上次拖动事件以来,触摸在 y 轴上被拖动的距离。
     */
    public float getDeltaY(){
        return deltaY;
    }

    public int getButton(){
        return button;
    }

    /**
     * Sets the button to listen for, all other buttons are ignored. Default is {@link Buttons#LEFT}. Use -1 for any button.
     * 设置要监听的按键,其他按键将被忽略。默认为 {@link Buttons#LEFT}。使用 -1 表示任意按键。
     */
    public void setButton(int button){
        this.button = button;
    }
}
