package arc.input;

import arc.ApplicationListener;
import arc.Input;

/**
 * An InputProcessor is used to receive input events from the keyboard and the touch screen (mouse on the desktop). For this it
 * has to be registered with the {@link Input#addProcessor(InputProcessor)} method. It will be called each frame before the
 * call to {@link ApplicationListener#update()}. Each method returns a boolean in case you want to use this with the
 * {@link InputMultiplexer} to chain input processors.
 * <p>
 * InputProcessor 用于接收来自键盘和触摸屏(桌面上为鼠标)的输入事件。为此,必须通过 {@link Input#addProcessor(InputProcessor)} 方法注册它。它会在每帧调用 {@link ApplicationListener#update()} 之前被调用。如果你想配合 {@link InputMultiplexer} 链式使用多个输入处理器,每个方法都会返回一个布尔值。
 * @author mzechner
 */
public interface InputProcessor{

    /**
     * Called when a key was pressed
     * <p>
     * 按键被按下时调用
     * @return whether the input was processed 输入是否已被处理
     */
    default boolean keyDown(KeyCode keycode){
        return false;
    }

    /**
     * Called when a key was released
     * <p>
     * 按键被释放时调用
     * @return whether the input was processed 输入是否已被处理
     */
    default boolean keyUp(KeyCode keycode){
        return false;
    }

    /**
     * Called when a key was typed
     * <p>
     * 键入了字符时调用
     * @param character The character 该字符
     * @return whether the input was processed 输入是否已被处理
     */
    default boolean keyTyped(char character){
        return false;
    }

    /**
     * @param screenX The x coordinate, origin is in the upper left corner x 坐标,原点在左上角
     * @param screenY The y coordinate, origin is in the upper left corner y 坐标,原点在左上角
     * @param pointer the pointer for the event. 事件的指针。
     * @param button the button 按钮
     * @return whether the input was processed 输入是否已被处理
     */
    default boolean touchDown(int screenX, int screenY, int pointer, KeyCode button){
        return false;
    }

    /**
     * @param pointer the pointer for the event. 事件的指针。
     * @param button the button 按钮
     * @return whether the input was processed 输入是否已被处理
     */
    default boolean touchUp(int screenX, int screenY, int pointer, KeyCode button){
        return false;
    }

    /**
     * Called when a finger or the mouse was dragged.
     * <p>
     * 手指或鼠标被拖动时调用。
     * @param pointer the pointer for the event. 事件的指针。
     * @return whether the input was processed 输入是否已被处理
     */
    default boolean touchDragged(int screenX, int screenY, int pointer){
        return false;
    }

    /**
     * @return whether the input was processed 输入是否已被处理
     */
    default boolean mouseMoved(int screenX, int screenY){
        return false;
    }

    /**
     * Called when the mouse wheel was scrolled. Will not be called on iOS.
     * <p>
     * 滚动鼠标滚轮时调用。在 iOS 上不会被调用。
     * @param amountX the horizontal scroll amount, negative or positive depending on the direction the wheel was scrolled. 水平滚动量,根据滚轮滚动方向为负或正。
     * @param amountY the vertical scroll amount, negative or positive depending on the direction the wheel was scrolled. 垂直滚动量,根据滚轮滚动方向为负或正。
     * @return whether the input was processed. 输入是否已被处理。
     */
    default boolean scrolled(float amountX, float amountY){
        return false;
    }
}
