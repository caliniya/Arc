package arc.input;

import arc.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.Timer.*;

/**
 * {@link InputProcessor} implementation that detects gestures (tap, long press, fling, pan, zoom, pinch) and hands them to a
 * {@link GestureListener}.
 * <p>
 * 检测手势(点按、长按、快速滑动、平移、缩放、双指缩放)并将其交给 {@link GestureListener} 的 {@link InputProcessor} 实现。
 * @author mzechner
 */
public class GestureDetector implements InputProcessor{
    final GestureListener listener;
    private final VelocityTracker tracker = new VelocityTracker();
    private final Vec2 pointer2 = new Vec2();
    private final Vec2 initialPointer1 = new Vec2();
    private final Vec2 initialPointer2 = new Vec2();
    boolean longPressFired;
    Vec2 pointer1 = new Vec2();
    private final Task longPressTask = new Task(){
        @Override
        public void run(){
            if(!longPressFired) longPressFired = listener.longPress(pointer1.x, pointer1.y);
        }
    };
    private float tapRectangleWidth;
    private float tapRectangleHeight;
    private long tapCountInterval;
    private float longPressSeconds;
    private long maxFlingDelay;
    private boolean inTapRectangle;
    private int tapCount;
    private long lastTapTime;
    private float lastTapX, lastTapY;
    private int lastTapPointer;
    private KeyCode lastTapButton;
    private boolean pinching;
    private boolean panning;
    private float tapRectangleCenterX, tapRectangleCenterY;
    private long gestureStartTime;

    /**
     * Creates a new GestureDetector with default values: halfTapSquareSize=20, tapCountInterval=0.4f, longPressDuration=1.1f,
     * maxFlingDelay=0.15f.
     * <p>
     * 使用默认值创建新的 GestureDetector:halfTapSquareSize=20、tapCountInterval=0.4f、longPressDuration=1.1f、maxFlingDelay=0.15f。
     */
    public GestureDetector(GestureListener listener){
        this(20, 0.4f, 1.1f, 0.15f, listener);
    }

    /**
     * @param halfTapSquareSize half width in pixels of the square around an initial touch event 初始触控事件周围正方形的一半宽度(像素)
     * @param tapCountInterval time in seconds that must pass for two touch down/up sequences to be detected as consecutive taps. 两次按下/抬起序列被判定为连续点按所需经过的时间(秒)。
     * @param longPressDuration time in seconds that must pass for the detector to fire a
     * {@link GestureListener#longPress(float, float)} event. 触发 {@link GestureListener#longPress(float, float)} 事件所需经过的时间(秒)。
     * @param maxFlingDelay time in seconds the finger must have been dragged for a fling event to be fired 触发 fling 事件时手指必须已拖动的时间(秒)
     * @param listener May be null if the listener will be set later. 如果稍后才设置监听器,可以为 null。
     */
    public GestureDetector(float halfTapSquareSize, float tapCountInterval, float longPressDuration, float maxFlingDelay,
                           GestureListener listener){
        this(halfTapSquareSize, halfTapSquareSize, tapCountInterval, longPressDuration, maxFlingDelay, listener);
    }

    /**
     * @param halfTapRectangleWidth half width in pixels of the rectangle around an initial touch event 初始触控事件周围矩形的一半宽度(像素)
     * @param halfTapRectangleHeight half height in pixels of the rectangle around an initial touch event 初始触控事件周围矩形的一半高度(像素)
     * @param tapCountInterval time in seconds that must pass for two touch down/up sequences to be detected as consecutive taps. 两次按下/抬起序列被判定为连续点按所需经过的时间(秒)。
     * @param longPressDuration time in seconds that must pass for the detector to fire a
     * {@link GestureListener#longPress(float, float)} event. 触发 {@link GestureListener#longPress(float, float)} 事件所需经过的时间(秒)。
     * @param maxFlingDelay time in seconds the finger must have been dragged for a fling event to be fired 触发 fling 事件时手指必须已拖动的时间(秒)
     * @param listener May be null if the listener will be set later. 如果稍后才设置监听器,可以为 null。
     */
    public GestureDetector(float halfTapRectangleWidth, float halfTapRectangleHeight, float tapCountInterval, float longPressDuration, float maxFlingDelay,
                           GestureListener listener){
        this.tapRectangleWidth = halfTapRectangleWidth;
        this.tapRectangleHeight = halfTapRectangleHeight;
        this.tapCountInterval = (long)(tapCountInterval * 1000000000L);
        this.longPressSeconds = longPressDuration;
        this.maxFlingDelay = (long)(maxFlingDelay * 1000000000L);
        this.listener = listener;
    }

    public GestureListener getListener(){
        return listener;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, KeyCode button){
        return touchDown((float)x, (float)y, pointer, button);
    }

    public boolean touchDown(float x, float y, int pointer, KeyCode button){
        if(pointer > 1) return false;

        if(pointer == 0){
            pointer1.set(x, y);
            gestureStartTime = Core.input.getCurrentEventTime();
            tracker.start(x, y, gestureStartTime);
            if(Core.input.isTouched(1)){
                // Start pinch.
                // 开始双指缩放。
                inTapRectangle = false;
                pinching = true;
                initialPointer1.set(pointer1);
                initialPointer2.set(pointer2);
                longPressTask.cancel();
            }else{
                // Normal touch down.
                // 普通触控按下。
                inTapRectangle = true;
                pinching = false;
                longPressFired = false;
                tapRectangleCenterX = x;
                tapRectangleCenterY = y;
                if(!longPressTask.isScheduled()) Timer.schedule(longPressTask, longPressSeconds);
            }
        }else{
            // Start pinch.
            // 开始双指缩放。
            pointer2.set(x, y);
            inTapRectangle = false;
            pinching = true;
            initialPointer1.set(pointer1);
            initialPointer2.set(pointer2);
            longPressTask.cancel();
        }
        return listener.touchDown(x, y, pointer, button);
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer){
        return touchDragged((float)x, (float)y, pointer);
    }

    public boolean touchDragged(float x, float y, int pointer){
        if(pointer > 1) return false;
        if(longPressFired) return false;

        if(pointer == 0)
            pointer1.set(x, y);
        else
            pointer2.set(x, y);

        // handle pinch zoom
        // 处理双指缩放
        if(pinching){
            if(listener != null){
                boolean result = listener.pinch(initialPointer1, initialPointer2, pointer1, pointer2);
                return listener.zoom(initialPointer1.dst(initialPointer2), pointer1.dst(pointer2)) || result;
            }
            return false;
        }

        // update tracker
        // 更新跟踪器
        tracker.update(x, y, Core.input.getCurrentEventTime());

        // check if we are still tapping.
        // 检查是否仍在点按。
        if(inTapRectangle && !isWithinTapRectangle(x, y, tapRectangleCenterX, tapRectangleCenterY)){
            longPressTask.cancel();
            inTapRectangle = false;
        }

        // if we have left the tap square, we are panning
        // 如果离开了点按方形区域,则处于平移状态
        if(!inTapRectangle){
            panning = true;
            return listener.pan(x, y, tracker.deltaX, tracker.deltaY);
        }

        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, KeyCode button){
        return touchUp((float)x, (float)y, pointer, button);
    }

    public boolean touchUp(float x, float y, int pointer, KeyCode button){
        if(pointer > 1) return false;

        // check if we are still tapping.
        // 检查是否仍在点按。
        if(inTapRectangle && !isWithinTapRectangle(x, y, tapRectangleCenterX, tapRectangleCenterY))
            inTapRectangle = false;

        boolean wasPanning = panning;
        panning = false;

        longPressTask.cancel();
        if(longPressFired) return false;

        if(inTapRectangle){
            // handle taps
            // 处理点按
            if(lastTapButton != button || lastTapPointer != pointer || Time.nanos() - lastTapTime > tapCountInterval
            || !isWithinTapRectangle(x, y, lastTapX, lastTapY)) tapCount = 0;
            tapCount++;
            lastTapTime = Time.nanos();
            lastTapX = x;
            lastTapY = y;
            lastTapButton = button;
            lastTapPointer = pointer;
            gestureStartTime = 0;
            return listener.tap(x, y, tapCount, button);
        }

        if(pinching){
            // handle pinch end
            // 处理双指缩放结束
            pinching = false;
            listener.pinchStop();
            panning = true;
            // we are in pan mode again, reset velocity tracker
            // 重新进入平移模式,重置速度跟踪器
            if(pointer == 0){
                // first pointer has lifted off, set up panning to use the second pointer...
                // 第一个手指已抬起,设置平移改用第二个手指...
                tracker.start(pointer2.x, pointer2.y, Core.input.getCurrentEventTime());
            }else{
                // second pointer has lifted off, set up panning to use the first pointer...
                // 第二个手指已抬起,设置平移改用第一个手指...
                tracker.start(pointer1.x, pointer1.y, Core.input.getCurrentEventTime());
            }
            return false;
        }

        // handle no longer panning
        // 处理不再平移
        boolean handled = false;
        if(wasPanning && !panning) handled = listener.panStop(x, y, pointer, button);

        // handle fling
        // 处理快速滑动(fling)
        gestureStartTime = 0;
        long time = Core.input.getCurrentEventTime();
        if(time - tracker.lastTime < maxFlingDelay){
            tracker.update(x, y, time);
            handled = listener.fling(tracker.getVelocityX(), tracker.getVelocityY(), button) || handled;
        }
        return handled;
    }

    /**
     * No further gesture events will be triggered for the current touch, if any.
     * 当前触控(如有)不会再触发任何手势事件。
     */
    public void cancel(){
        longPressTask.cancel();
        longPressFired = true;
    }

    /**
     * @return whether the user touched the screen long enough to trigger a long press event. 用户触摸屏幕的时长是否足以触发长按事件。
     */
    public boolean isLongPressed(){
        return isLongPressed(longPressSeconds);
    }

    /**
     * @return whether the user touched the screen for as much or more than the given duration. 用户触摸屏幕的时长是否达到或超过给定时长。
     */
    public boolean isLongPressed(float duration){
        if(gestureStartTime == 0) return false;
        return Time.nanos() - gestureStartTime > (long)(duration * 1000000000L);
    }

    public boolean isPanning(){
        return panning;
    }

    public void reset(){
        gestureStartTime = 0;
        panning = false;
        inTapRectangle = false;
        tracker.lastTime = 0;
    }

    private boolean isWithinTapRectangle(float x, float y, float centerX, float centerY){
        return Math.abs(x - centerX) < tapRectangleWidth && Math.abs(y - centerY) < tapRectangleHeight;
    }

    /**
     * The tap square will not longer be used for the current touch.
     * 当前触控将不再使用点按方形区域。
     */
    public void invalidateTapSquare(){
        inTapRectangle = false;
    }

    public void setTapSquareSize(float halfTapSquareSize){
        setTapRectangleSize(halfTapSquareSize, halfTapSquareSize);
    }

    public void setTapRectangleSize(float halfTapRectangleWidth, float halfTapRectangleHeight){
        this.tapRectangleWidth = halfTapRectangleWidth;
        this.tapRectangleHeight = halfTapRectangleHeight;
    }

    /**
     * @param tapCountInterval time in seconds that must pass for two touch down/up sequences to be detected as consecutive taps. 两次按下/抬起序列被判定为连续点按所需经过的时间(秒)。
     */
    public void setTapCountInterval(float tapCountInterval){
        this.tapCountInterval = (long)(tapCountInterval * 1000000000L);
    }

    public void setLongPressSeconds(float longPressSeconds){
        this.longPressSeconds = longPressSeconds;
    }

    public void setMaxFlingDelay(long maxFlingDelay){
        this.maxFlingDelay = maxFlingDelay;
    }

    /**
     * Register an instance of this class with a {@link GestureDetector} to receive gestures such as taps, long presses, flings,
     * panning or pinch zooming. Each method returns a boolean indicating if the event should be handed to the next listener (false
     * to hand it to the next listener, true otherwise).
     * <p>
     * 向 {@link GestureDetector} 注册此类的实例以接收点按、长按、快速滑动、平移或双指缩放手势。每个方法返回一个布尔值,指示事件是否应交给下一个监听器(false 表示交给下一个监听器,否则为 true)。
     * @author mzechner
     */
    public interface GestureListener{
        default boolean touchDown(float x, float y, int pointer, KeyCode button){
            return false;
        }

        /**
         * Called when a tap occured. A tap happens if a touch went down on the screen and was lifted again without moving outside
         * of the tap square. The tap square is a rectangular area around the initial touch position as specified on construction
         * time of the {@link GestureDetector}.
         * <p>
         * 点按时调用。如果触摸按在屏幕上并在未移出点按方形区域的情况下抬起,就会发生点按。点按方形区域是初始触摸位置周围的矩形区域,在 {@link GestureDetector} 构造时指定。
         * @param count the number of taps. 点按次数。
         */
        default boolean tap(float x, float y, int count, KeyCode button){
            return false;
        }

        default boolean longPress(float x, float y){
            return false;
        }

        /**
         * Called when the user dragged a finger over the screen and lifted it. Reports the last known velocity of the finger in
         * pixels per second.
         * <p>
         * 用户在屏幕上拖动手指并抬起时调用。以像素/秒为单位报告手指的最后已知速度。
         * @param velocityX velocity on x in seconds x 方向上的速度(像素/秒)
         * @param velocityY velocity on y in seconds y 方向上的速度(像素/秒)
         */
        default boolean fling(float velocityX, float velocityY, KeyCode button){
            return false;
        }

        /**
         * Called when the user drags a finger over the screen.
         * <p>
         * 用户在屏幕上拖动手指时调用。
         * @param deltaX the difference in pixels to the last drag event on x. 与上一次 x 方向拖动事件的像素差值。
         * @param deltaY the difference in pixels to the last drag event on y. 与上一次 y 方向拖动事件的像素差值。
         */
        default boolean pan(float x, float y, float deltaX, float deltaY){
            return false;
        }

        /**
         * Called when no longer panning.
         * 不再平移时调用。
         */
        default boolean panStop(float x, float y, int pointer, KeyCode button){
            return false;
        }

        /**
         * Called when the user performs a pinch zoom gesture. The original distance is the distance in pixels when the gesture
         * started.
         * <p>
         * 用户执行双指缩放手势时调用。原始距离是手势开始时两指间的像素距离。
         * @param initialDistance distance between fingers when the gesture started. 手势开始时两指间的距离。
         * @param distance current distance between fingers. 两指间的当前距离。
         */
        default boolean zoom(float initialDistance, float distance){
            return false;
        }

        /**
         * Called when a user performs a pinch zoom gesture. Reports the initial positions of the two involved fingers and their
         * current positions.
         * <p>
         * 用户执行双指缩放手势时调用。报告所涉及两个手指的初始位置及其当前位置。
         */
        default boolean pinch(Vec2 initialPointer1, Vec2 initialPointer2, Vec2 pointer1, Vec2 pointer2){
            return false;
        }

        /**
         * Called when no longer pinching.
         * 不再双指缩放时调用。
         */
        default void pinchStop(){
        }
    }

    static class VelocityTracker{
        int sampleSize = 10;
        float lastX, lastY;
        float deltaX, deltaY;
        long lastTime;
        int numSamples;
        float[] meanX = new float[sampleSize];
        float[] meanY = new float[sampleSize];
        long[] meanTime = new long[sampleSize];

        public void start(float x, float y, long timeStamp){
            lastX = x;
            lastY = y;
            deltaX = 0;
            deltaY = 0;
            numSamples = 0;
            for(int i = 0; i < sampleSize; i++){
                meanX[i] = 0;
                meanY[i] = 0;
                meanTime[i] = 0;
            }
            lastTime = timeStamp;
        }

        public void update(float x, float y, long currTime){
            deltaX = x - lastX;
            deltaY = y - lastY;
            lastX = x;
            lastY = y;
            long deltaTime = currTime - lastTime;
            lastTime = currTime;
            int index = numSamples % sampleSize;
            meanX[index] = deltaX;
            meanY[index] = deltaY;
            meanTime[index] = deltaTime;
            numSamples++;
        }

        public float getVelocityX(){
            float meanX = getAverage(this.meanX, numSamples);
            float meanTime = getAverage(this.meanTime, numSamples) / 1000000000.0f;
            if(meanTime == 0) return 0;
            return meanX / meanTime;
        }

        public float getVelocityY(){
            float meanY = getAverage(this.meanY, numSamples);
            float meanTime = getAverage(this.meanTime, numSamples) / 1000000000.0f;
            if(meanTime == 0) return 0;
            return meanY / meanTime;
        }

        private float getAverage(float[] values, int numSamples){
            numSamples = Math.min(sampleSize, numSamples);
            float sum = 0;
            for(int i = 0; i < numSamples; i++){
                sum += values[i];
            }
            return sum / numSamples;
        }

        private long getAverage(long[] values, int numSamples){
            numSamples = Math.min(sampleSize, numSamples);
            long sum = 0;
            for(int i = 0; i < numSamples; i++){
                sum += values[i];
            }
            if(numSamples == 0) return 0;
            return sum / numSamples;
        }

        private float getSum(float[] values, int numSamples){
            numSamples = Math.min(sampleSize, numSamples);
            float sum = 0;
            for(int i = 0; i < numSamples; i++){
                sum += values[i];
            }
            if(numSamples == 0) return 0;
            return sum;
        }
    }
}
