package arc.scene.ui;

import arc.Core;
import arc.func.Floatc;
import arc.graphics.g2d.NinePatch;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.Interp;
import arc.scene.Element;
import arc.scene.event.ChangeListener.ChangeEvent;
import arc.scene.event.HandCursorListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.Drawable;
import arc.util.pooling.Pools;

import static arc.Core.scene;

/**
 * A slider is a horizontal indicator that allows a user to set a value. The slider has a range (min, max) and a stepping between
 * each value the slider represents.
 * <p>
 * {@link ChangeEvent} is fired when the slider knob is moved. Canceling the event will move the knob to where it was previously.
 * <p>
 * For a horizontal progress bar, its preferred height is determined by the larger of the knob and background, and the preferred width
 * is 140, a relatively arbitrary size. These parameters are reversed for a vertical progress bar.
 * <p>
 * 滑块是一种允许用户设置数值的水平指示器。滑块有一个范围(min、max)以及每个值之间的步长。 <p> 移动滑块旋钮时会触发 {@link ChangeEvent}。取消该事件会将旋钮移回之前的位置。 <p> 对于水平进度条,其首选高度由旋钮和背景中较大者决定,首选宽度为 140(相对随意的一个值)。垂直进度条的这些参数正好相反。
 * @author mzechner
 * @author Nathan Sweet
 */
public class Slider extends ProgressBar{
    int draggingPointer = -1;
    boolean mouseOver;
    private Interp visualInterpolationInverse = Interp.linear;
    private float[] snapValues;
    private float threshold;

    public Slider(float min, float max, float stepSize, boolean vertical){
        this(min, max, stepSize, vertical, scene.getStyle(SliderStyle.class));
    }

    /**
     * Creates a new slider. If horizontal, its width is determined by the prefWidth parameter, its height is determined by the maximum of
     * the height of either the slider {@link NinePatch} or slider handle {@link TextureRegion}. The min and max values determine
     * the range the values of this slider can take on, the stepSize parameter specifies the distance between individual values.
     * E.g. min could be 4, max could be 10 and stepSize could be 0.2, giving you a total of 30 values, 4.0 4.2, 4.4 and so on.
     * <p>
     * 创建一个新的滑块。若为水平方向,其宽度由 prefWidth 参数决定,高度由滑块 {@link NinePatch} 和滑块手柄 {@link TextureRegion} 二者高度的较大值决定。min 和 max 值决定此滑块可取值的范围,stepSize 参数指定各个值之间的间隔。例如,min 可为 4,max 可为 10,stepSize 可为 0.2,总共得到 30 个值:4.0、4.2、4.4 等等。
     * @param min the minimum value 最小值。
     * @param max the maximum value 最大值。
     * @param stepSize the step size between values 各值之间的步长。
     * @param style the {@link SliderStyle} 滑块样式。
     */
    public Slider(float min, float max, float stepSize, boolean vertical, SliderStyle style){
        super(min, max, stepSize, vertical, style);

        addListener(new HandCursorListener());
        addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(disabled) return false;
                if(draggingPointer != -1) return false;
                draggingPointer = pointer;
                calculatePositionAndValue(x, y);
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(pointer != draggingPointer) return;
                draggingPointer = -1;
                if(!calculatePositionAndValue(x, y)){
                    // Fire an event on touchUp even if the value didn't change, so listeners can see when a drag ends via isDragging.
                    // 即使值未改变,也在 touchUp 时触发事件,以便监听器通过 isDragging 得知拖动何时结束。
                    ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class, ChangeEvent::new);
                    fire(changeEvent);
                    Pools.free(changeEvent);
                }
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                calculatePositionAndValue(x, y);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                if(pointer == -1) mouseOver = true;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
                if(pointer == -1) mouseOver = false;
            }
        });
    }

    /**
     * Returns the slider's style. Modifying the returned style may not have an effect until {@link #setStyle(SliderStyle)} is
     * called.
     * <p>
     * 返回滑块的样式。在调用 {@link #setStyle(SliderStyle)} 之前,修改返回的样式可能不会生效。
     */
    @Override
    public SliderStyle getStyle(){
        return (SliderStyle)super.getStyle();
    }

    public void setStyle(SliderStyle style){
        if(style == null) throw new NullPointerException("style cannot be null");
        if(!(style instanceof SliderStyle)) throw new IllegalArgumentException("style must be a SliderStyle.");
        super.setStyle(style);
    }

    @Override
    protected Drawable getKnobDrawable(){
        SliderStyle style = getStyle();
        return (disabled && style.disabledKnob != null) ? style.disabledKnob
        : (isDragging() && style.knobDown != null) ? style.knobDown
        : ((mouseOver && style.knobOver != null) ? style.knobOver : style.knob);
    }

    private float valueAt(float position, float span, float min, float max){
        if(!(span > 0)) return min;
        return min + (max - min) * visualInterpolationInverse.apply(position / span);
    }

    boolean calculatePositionAndValue(float x, float y){
        if(!Float.isFinite(x) || !Float.isFinite(y)) return false;

        final SliderStyle style = getStyle();
        final Drawable knob = getKnobDrawable();
        final Drawable bg = (disabled && style.disabledBackground != null) ? style.disabledBackground : style.background;

        float value;
        float oldPosition = position;
        float span;

        final float min = getMinValue();
        final float max = getMaxValue();

        if(vertical){
            float height = getHeight() - bg.getTopHeight() - bg.getBottomHeight();
            float knobHeight = knob == null ? 0 : knob.getMinHeight();
            span = height - knobHeight;
            position = y - bg.getBottomHeight() - knobHeight * 0.5f;
        }else{
            float width = getWidth() - bg.getLeftWidth() - bg.getRightWidth();
            float knobWidth = knob == null ? 0 : knob.getMinWidth();
            span = width - knobWidth;
            position = x - bg.getLeftWidth() - knobWidth * 0.5f;
        }

        value = valueAt(position, span, min, max);

        if(!Float.isFinite(value) || !Float.isFinite(position)){
            position = oldPosition;
            return false;
        }

        position = Math.max(0, position);
        position = Math.min(span, position);

        float oldValue = value;
        if(!Core.input.keyDown(KeyCode.shiftLeft) && !Core.input.keyDown(KeyCode.shiftRight))
            value = snap(value);
        boolean valueSet = setValue(value);
        if(value == oldValue) position = oldPosition;
        return valueSet;
    }

    public void moved(Floatc listener){
        changed(() -> listener.get(getValue()));
    }

    /**
     * Returns a snapped value.
     * 返回吸附后的值。
     */
    protected float snap(float value){
        if(snapValues == null) return value;
        for(int i = 0; i < snapValues.length; i++){
            if(Math.abs(value - snapValues[i]) <= threshold) return snapValues[i];
        }
        return value;
    }

    /**
     * Will make this progress bar snap to the specified values, if the knob is within the threshold.
     * <p>
     * 若旋钮在阈值范围内,则使此进度条吸附到指定值。
     * @param values May be null. 可以为 null。
     */
    public void setSnapToValues(float[] values, float threshold){
        this.snapValues = values;
        this.threshold = threshold;
    }

    /**
     * Returns true if the slider is being dragged.
     * 若滑块正在被拖动则返回 true。
     */
    public boolean isDragging(){
        return draggingPointer != -1;
    }

    /**
     * Sets the inverse interpolation to use for display. This should perform the inverse of the
     * {@link #setVisualInterpolation(Interp) visual interpolation}.
     * <p>
     * 设置显示时使用的反向插值。它应执行 {@link #setVisualInterpolation(Interp) 可视插值} 的逆运算。
     */
    public void setVisualInterpolationInverse(Interp interpolation){
        this.visualInterpolationInverse = interpolation;
    }

    /**
     * The style for a slider, see {@link Slider}.
     * <p>
     * 滑块的样式,见 {@link Slider}。
     * @author mzechner
     * @author Nathan Sweet
     */
    public static class SliderStyle extends ProgressBarStyle{
        /**
         * Optional.
         * 可选。
         */
        public Drawable knobOver, knobDown;
    }
}