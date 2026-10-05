package arc.scene.ui;

import arc.*;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.input.KeyCode;
import arc.math.geom.*;
import arc.scene.Element;
import arc.scene.event.ChangeListener.ChangeEvent;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.Drawable;
import arc.util.pooling.Pools;

/**
 * An on-screen joystick. The movement area of the joystick is circular, centered on the touchpad, and its size determined by the
 * smaller touchpad dimension.
 * <p>
 * The preferred size of the touchpad is determined by the background.
 * <p>
 * {@link ChangeEvent} is fired when the touchpad knob is moved. Cancelling the event will move the knob to where it was
 * previously.
 * <p>
 * 屏幕上的虚拟摇杆。摇杆的移动区域是圆形的,以触控板为中心,大小由触控板较小的一边决定。 <p> 触控板的首选大小由背景决定。 <p> 移动触控板旋钮时会触发 {@link ChangeEvent}。取消该事件会将旋钮移回之前的位置。
 * @author Josh Street
 */
public class Touchpad extends Element{
    private final Circle knobBounds = new Circle(0, 0, 0);
    private final Circle touchBounds = new Circle(0, 0, 0);
    private final Circle deadzoneBounds = new Circle(0, 0, 0);
    private final Vec2 knobPosition = new Vec2();
    private final Vec2 knobPercent = new Vec2();
    boolean touched;
    boolean resetOnTouchUp = true;
    private TouchpadStyle style;
    private float deadzoneRadius;

    /** @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved. 旋钮开始移动所需的、距触控板中心的距离(像素)。 */
    public Touchpad(float deadzoneRadius){
        this(deadzoneRadius, Core.scene.getStyle(TouchpadStyle.class));
    }
    /** @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved. 旋钮开始移动所需的、距触控板中心的距离(像素)。 */
    public Touchpad(float deadzoneRadius, TouchpadStyle style){
        if(deadzoneRadius < 0) throw new IllegalArgumentException("deadzoneRadius must be > 0");
        this.deadzoneRadius = deadzoneRadius;

        knobPosition.set(getWidth() / 2f, getHeight() / 2f);

        setStyle(style);
        setSize(getPrefWidth(), getPrefHeight());

        addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(touched) return false;
                touched = true;
                calculatePositionAndValue(x, y, false);
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                calculatePositionAndValue(x, y, false);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                touched = false;
                calculatePositionAndValue(x, y, resetOnTouchUp);
            }
        });
    }

    void calculatePositionAndValue(float x, float y, boolean isTouchUp){
        float oldPositionX = knobPosition.x;
        float oldPositionY = knobPosition.y;
        float oldPercentX = knobPercent.x;
        float oldPercentY = knobPercent.y;
        float centerX = knobBounds.x;
        float centerY = knobBounds.y;
        knobPosition.set(centerX, centerY);
        knobPercent.set(0f, 0f);
        if(!isTouchUp){
            if(!deadzoneBounds.contains(x, y)){
                knobPercent.set((x - centerX) / knobBounds.radius, (y - centerY) / knobBounds.radius);
                float length = knobPercent.len();
                if(length > 1) knobPercent.scl(1 / length);
                if(knobBounds.contains(x, y)){
                    knobPosition.set(x, y);
                }else{
                    knobPosition.set(knobPercent).nor().scl(knobBounds.radius).add(knobBounds.x, knobBounds.y);
                }
            }
        }
        if(oldPercentX != knobPercent.x || oldPercentY != knobPercent.y){
            ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class, ChangeEvent::new);
            if(fire(changeEvent)){
                knobPercent.set(oldPercentX, oldPercentY);
                knobPosition.set(oldPositionX, oldPositionY);
            }
            Pools.free(changeEvent);
        }
    }

    /**
     * Returns the touchpad's style. Modifying the returned style may not have an effect until {@link #setStyle(TouchpadStyle)} is
     * called.
     * <p>
     * 返回触控板的样式。在调用 {@link #setStyle(TouchpadStyle)} 之前,修改返回的样式可能不会生效。
     */
    public TouchpadStyle getStyle(){
        return style;
    }

    public void setStyle(TouchpadStyle style){
        if(style == null) throw new IllegalArgumentException("style cannot be null");
        this.style = style;
        invalidateHierarchy();
    }

    @Override
    public Element hit(float x, float y, boolean touchable){
        return touchBounds.contains(x, y) ? this : null;
    }

    @Override
    public void layout(){
        // Recalc margin and deadzone bounds
        // 重新计算外边距和死区边界
        float halfWidth = getWidth() / 2;
        float halfHeight = getHeight() / 2;
        float radius = Math.min(halfWidth, halfHeight);
        touchBounds.set(halfWidth, halfHeight, radius);
        if(style.knob != null) radius -= Math.max(style.knob.getMinWidth(), style.knob.getMinHeight()) / 2;
        knobBounds.set(halfWidth, halfHeight, radius);
        deadzoneBounds.set(halfWidth, halfHeight, deadzoneRadius);
        // Recalc margin values and knob position
        // 重新计算外边距值和旋钮位置
        knobPosition.set(halfWidth, halfHeight);
        knobPercent.set(0, 0);
    }

    @Override
    public void draw(){
        validate();

        Color c = color;
        Draw.color(c.r, c.g, c.b, c.a * parentAlpha);

        float x = this.x;
        float y = this.y;
        float w = getWidth();
        float h = getHeight();

        final Drawable bg = style.background;
        if(bg != null) bg.draw(x, y, w, h);

        final Drawable knob = style.knob;
        if(knob != null){
            x += knobPosition.x - knob.getMinWidth() / 2f;
            y += knobPosition.y - knob.getMinHeight() / 2f;
            knob.draw(x, y, knob.getMinWidth(), knob.getMinHeight());
        }
    }

    @Override
    public float getPrefWidth(){
        return style.background != null ? style.background.getMinWidth() : 0;
    }

    @Override
    public float getPrefHeight(){
        return style.background != null ? style.background.getMinHeight() : 0;
    }

    public boolean isTouched(){
        return touched;
    }

    public boolean getResetOnTouchUp(){
        return resetOnTouchUp;
    }

    /** @param reset Whether to reset the knob to the center on touch up. 是否在触摸抬起时将旋钮重置到中心。 */
    public void setResetOnTouchUp(boolean reset){
        this.resetOnTouchUp = reset;
    }

    /** @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved. 旋钮开始移动所需的、距触控板中心的距离(像素)。 */
    public void setDeadzone(float deadzoneRadius){
        if(deadzoneRadius < 0) throw new IllegalArgumentException("deadzoneRadius must be > 0");
        this.deadzoneRadius = deadzoneRadius;
        invalidate();
    }

    /**
     * Returns the x-position of the knob relative to the center of the widget. The positive direction is right.
     * 返回旋钮相对于控件中心的 x 位置。正方向为右。
     */
    public float getKnobX(){
        return knobPosition.x;
    }

    /**
     * Returns the y-position of the knob relative to the center of the widget. The positive direction is up.
     * 返回旋钮相对于控件中心的 y 位置。正方向为上。
     */
    public float getKnobY(){
        return knobPosition.y;
    }

    /**
     * Returns the x-position of the knob as a percentage from the center of the touchpad to the edge of the circular movement
     * area. The positive direction is right.
     * <p>
     * 以百分比形式返回旋钮的 x 位置,范围是从触控板中心到圆形移动区域边缘。正方向为右。
     */
    public float getKnobPercentX(){
        return knobPercent.x;
    }

    /**
     * Returns the y-position of the knob as a percentage from the center of the touchpad to the edge of the circular movement
     * area. The positive direction is up.
     * <p>
     * 以百分比形式返回旋钮的 y 位置,范围是从触控板中心到圆形移动区域边缘。正方向为上。
     */
    public float getKnobPercentY(){
        return knobPercent.y;
    }

    /**
     * The style for a {@link Touchpad}.
     * <p>
     * {@link Touchpad} 的样式。
     * @author Josh Street
     */
    public static class TouchpadStyle{
        /**
         * Stretched in both directions. Optional.
         * 在两个方向上拉伸。可选。
         */
        public Drawable background;

        /**
         * Optional.
         * 可选。
         */
        public Drawable knob;

        public TouchpadStyle(){
        }

        public TouchpadStyle(Drawable background, Drawable knob){
            this.background = background;
            this.knob = knob;
        }

        public TouchpadStyle(TouchpadStyle style){
            this.background = style.background;
            this.knob = style.knob;
        }
    }
}
