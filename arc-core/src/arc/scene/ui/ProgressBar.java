package arc.scene.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.ChangeListener.*;
import arc.scene.style.*;
import arc.scene.utils.*;
import arc.util.pooling.*;

/**
 * A progress bar is a widget that visually displays the progress of some activity or a value within given range. The progress
 * bar has a range (min, max) and a stepping between each value it represents. The percentage of completeness typically starts out
 * as an empty progress bar and gradually becomes filled in as the task or variable value progresses.
 * <p>
 * {@link ChangeEvent} is fired when the progress bar knob is moved. Cancelling the event will move the knob to where it was
 * previously.
 * <p>
 * For a horizontal progress bar, its preferred height is determined by the larger of the knob and background, and the preferred width
 * is 140, a relatively arbitrary size. These parameters are reversed for a vertical progress bar.
 * <p>
 * 进度条是一种以可视化方式显示某项活动的进度或某个给定范围内取值的控件。进度条有一个范围(min、max)以及每个值之间的步长。完成百分比通常从空进度条开始,随着任务或变量值的推进逐渐填满。 <p> 移动进度条旋钮时会触发 {@link ChangeEvent}。取消该事件会将旋钮移回之前的位置。 <p> 对于水平进度条,其首选高度由旋钮和背景中较大者决定,首选宽度为 140(相对随意的一个值)。垂直进度条的这些参数正好相反。
 * @author mzechner
 * @author Nathan Sweet
 */
public class ProgressBar extends Element implements Disableable{
    final boolean vertical;
    float position;
    boolean disabled;
    private ProgressBarStyle style;
    private float min, max, stepSize;
    private float value, animateFromValue;
    private float animateDuration, animateTime;
    private Interp animateInterpolation = Interp.linear;
    private Interp visualInterpolation = Interp.linear;
    private boolean round = true;

    /**
     * Creates a new progress bar. If horizontal, its width is determined by the prefWidth parameter, and its height is determined by the
     * maximum of the height of either the progress bar {@link NinePatch} or progress bar handle {@link TextureRegion}. The min and
     * max values determine the range the values of this progress bar can take on, the stepSize parameter specifies the distance
     * between individual values.
     * <p>
     * E.g. min could be 4, max could be 10 and stepSize could be 0.2, giving you a total of 30 values, 4.0 4.2, 4.4 and so on.
     * <p>
     * 创建一个新的进度条。若为水平方向,其宽度由 prefWidth 参数决定,高度由进度条 {@link NinePatch} 和进度条旋钮 {@link TextureRegion} 二者高度的较大值决定。min 和 max 值决定此进度条可取值的范围,stepSize 参数指定各个值之间的间隔。 <p> 例如,min 可为 4,max 可为 10,stepSize 可为 0.2,总共得到 30 个值:4.0、4.2、4.4 等等。
     * @param min the minimum value 最小值。
     * @param max the maximum value 最大值。
     * @param stepSize the step size between values 各值之间的步长。
     * @param style the {@link ProgressBarStyle} 进度条样式。
     */
    public ProgressBar(float min, float max, float stepSize, boolean vertical, ProgressBarStyle style){
        if(min > max) throw new IllegalArgumentException("max must be > min. min,max: " + min + ", " + max);
        if(stepSize < 0) throw new IllegalArgumentException("stepSize must be finite and >= 0: " + stepSize);
        setStyle(style);
        this.min = min;
        this.max = max;
        this.stepSize = stepSize;
        this.vertical = vertical;
        this.value = min;
        setSize(getPrefWidth(), getPrefHeight());
    }

    /**
     * Returns the progress bar's style. Modifying the returned style may not have an effect until
     * {@link #setStyle(ProgressBarStyle)} is called.
     * <p>
     * 返回进度条的样式。在调用 {@link #setStyle(ProgressBarStyle)} 之前,修改返回的样式可能不会生效。
     */
    public ProgressBarStyle getStyle(){
        return style;
    }

    public void setStyle(ProgressBarStyle style){
        if(style == null) throw new IllegalArgumentException("style cannot be null.");
        this.style = style;
        invalidateHierarchy();
    }

    @Override
    public void act(float delta){
        super.act(delta);
        if(animateTime > 0){
            animateTime -= delta;
            Scene stage = getScene();
            if(stage != null && stage.getActionsRequestRendering()) Core.graphics.requestRendering();
        }
    }

    @Override
    public void draw(){
        ProgressBarStyle style = this.style;
        boolean disabled = this.disabled;
        final Drawable knob = getKnobDrawable();
        final Drawable bg = (disabled && style.disabledBackground != null) ? style.disabledBackground : style.background;
        final Drawable knobBefore = (disabled && style.disabledKnobBefore != null) ? style.disabledKnobBefore : style.knobBefore;
        final Drawable knobAfter = (disabled && style.disabledKnobAfter != null) ? style.disabledKnobAfter : style.knobAfter;

        Color color = this.color;
        float x = this.x;
        float y = this.y;
        float width = getWidth();
        float height = getHeight();
        float knobHeight = knob == null ? 0 : knob.getMinHeight();
        float knobWidth = knob == null ? 0 : knob.getMinWidth();
        float percent = getVisualPercent();

        Draw.color(color.r, color.g, color.b, color.a * parentAlpha);

        if(vertical){
            float positionHeight = height;

            float bgTopHeight = 0;
            if(bg != null){
                if(round)
                    bg.draw(Math.round(x + (width - bg.getMinWidth()) * 0.5f), y, Math.round(bg.getMinWidth()), height);
                else
                    bg.draw(x + width - bg.getMinWidth() * 0.5f, y, bg.getMinWidth(), height);
                bgTopHeight = bg.getTopHeight();
                positionHeight -= bgTopHeight + bg.getBottomHeight();
            }

            float knobHeightHalf = 0;
            if(min != max){
                if(knob == null){
                    knobHeightHalf = knobBefore == null ? 0 : knobBefore.getMinHeight() * 0.5f;
                    position = (positionHeight - knobHeightHalf) * percent;
                    position = Math.min(positionHeight - knobHeightHalf, position);
                }else{
                    knobHeightHalf = knobHeight * 0.5f;
                    position = (positionHeight - knobHeight) * percent;
                    position = Math.min(positionHeight - knobHeight, position) + bg.getBottomHeight();
                }
                position = Math.max(0, position);
            }

            if(knobBefore != null){
                float offset = 0;
                if(bg != null) offset = bgTopHeight;
                if(round)
                    knobBefore.draw(Math.round(x + (width - knobBefore.getMinWidth()) * 0.5f), Math.round(y + offset), Math.round(knobBefore.getMinWidth()),
                    Math.round(position + knobHeightHalf));
                else
                    knobBefore.draw(x + (width - knobBefore.getMinWidth()) * 0.5f, y + offset, knobBefore.getMinWidth(),
                    position + knobHeightHalf);
            }
            if(knobAfter != null){
                if(round)
                    knobAfter.draw(Math.round(x + (width - knobAfter.getMinWidth()) * 0.5f), Math.round(y + position + knobHeightHalf),
                    Math.round(knobAfter.getMinWidth()), Math.round(height - position - knobHeightHalf));
                else
                    knobAfter.draw(x + (width - knobAfter.getMinWidth()) * 0.5f, y + position + knobHeightHalf,
                    knobAfter.getMinWidth(), height - position - knobHeightHalf);
            }
            if(knob != null){
                if(round)
                    knob.draw(Math.round(x + (width - knobWidth) * 0.5f), Math.round(y + position), Math.round(knobWidth), Math.round(knobHeight));
                else
                    knob.draw(x + (width - knobWidth) * 0.5f, y + position, knobWidth, knobHeight);
            }
        }else{
            float positionWidth = width;
            float bgLeftWidth = 0;

            if(bg != null){
                //currently draws background under *everything*, not limited by bg height
                // 当前将背景绘制在所有内容之下,不受背景高度限制
                bg.draw(x, y, width, height);
            }

            float knobWidthHalf = 0;
            if(min != max){
                if(knob == null){
                    knobWidthHalf = knobBefore == null ? 0 : knobBefore.getMinWidth() * 0.5f;
                    position = (positionWidth - knobWidthHalf) * percent;
                    position = Math.min(positionWidth - knobWidthHalf, position);
                }else{
                    knobWidthHalf = knobWidth * 0.5f;
                    position = (positionWidth - knobWidth) * percent;
                    position = Math.min(positionWidth - knobWidth, position) + bgLeftWidth;
                }
                position = Math.max(0, position);
            }

            if(knobBefore != null){
                float offset = 0;
                if(bg != null) offset = bgLeftWidth;
                if(round)
                    knobBefore.draw(Math.round(x + offset), Math.round(y + (height - knobBefore.getMinHeight()) * 0.5f),
                    Math.round(position + knobWidthHalf), Math.round(knobBefore.getMinHeight()));
                else
                    knobBefore.draw(x + offset, y + (height - knobBefore.getMinHeight()) * 0.5f,
                    position + knobWidthHalf, knobBefore.getMinHeight());
            }
            if(knobAfter != null){
                if(round)
                    knobAfter.draw(Math.round(x + position + knobWidthHalf), Math.round(y + (height - knobAfter.getMinHeight()) * 0.5f),
                    Math.round(width - position - knobWidthHalf), Math.round(knobAfter.getMinHeight()));
                else
                    knobAfter.draw(x + position + knobWidthHalf, y + (height - knobAfter.getMinHeight()) * 0.5f,
                    width - position - knobWidthHalf, knobAfter.getMinHeight());
            }
            if(knob != null){
                if(round)
                    knob.draw(Math.round(x + position), Math.round(y + (height - knobHeight) * 0.5f), Math.round(knobWidth), Math.round(knobHeight));
                else
                    knob.draw(x + position, y + (height - knobHeight) * 0.5f, knobWidth, knobHeight);
            }
        }
    }

    public float getValue(){
        return value;
    }

    /**
     * If {@link #setAnimateDuration(float) animating} the progress bar value, this returns the value current displayed.
     * 若正在 {@link #setAnimateDuration(float) 动画} 进度条数值,则返回当前显示的值。
     */
    public float getVisualValue(){
        if(animateTime > 0 && animateDuration > 0)
            return animateInterpolation.apply(animateFromValue, value, 1 - animateTime / animateDuration);
        return value;
    }

    public float getPercent(){
        return percentOf(value);
    }

    public float getVisualPercent(){
        return visualInterpolation.apply(percentOf(getVisualValue()));
    }

    private float percentOf(float value){
        double range = (double)max - min;
        if(!(range > 0)) return 0f;
        return (float)(((double)value - min) / range);
    }

    protected Drawable getKnobDrawable(){
        return (disabled && style.disabledKnob != null) ? style.disabledKnob : style.knob;
    }

    /**
     * Returns progress bar visual position within the range.
     * 返回进度条在范围内的可视化位置。
     */
    protected float getKnobPosition(){
        return this.position;
    }

    /**
     * Sets the progress bar position, rounded to the nearest step size and clamped to the minimum and maximum values.
     * {@link #clamp(float)} can be overridden to allow values outside of the progress bar's min/max range.
     * <p>
     * 设置进度条位置,四舍五入到最近的步长并钳制在最小值和最大值之间。可以重写 {@link #clamp(float)} 以允许超出进度条 min/max 范围的值。
     * @return false if the value was not changed because the progress bar already had the value or it was canceled by a 若值未更改则返回 false,因为进度条已是该值,或该变更被
     * listener. 监听器取消。
     */
    public boolean setValue(float value){
        return setValue(value, true);
    }

    /**
     * Sets the value, optionally skipping the changed event.
     * 设置值,可选择跳过变更事件。
     */
    public boolean setValue(float value, boolean fireChanged){
        if(Float.isNaN(value)) return false;
        if(stepSize > 0 && !Float.isInfinite(value)) value = Math.round(value / stepSize) * stepSize;
        value = clamp(value);
        float oldValue = this.value;
        if(value == oldValue) return false;
        float oldVisualValue = getVisualValue();
        this.value = value;
        ChangeEvent changeEvent = fireChanged ? Pools.obtain(ChangeEvent.class, ChangeEvent::new) : null;
        boolean cancelled = fireChanged && fire(changeEvent);
        if(cancelled)
            this.value = oldValue;
        else if(animateDuration > 0){
            animateFromValue = oldVisualValue;
            animateTime = animateDuration;
        }
        if(fireChanged) Pools.free(changeEvent);
        return !cancelled;
    }

    /**
     * Clamps the value to the progress bar's min/max range. This can be overridden to allow a range different from the progress
     * bar knob's range.
     * <p>
     * 将值钳制在进度条的 min/max 范围内。可以重写此方法以允许与进度条旋钮范围不同的范围。
     */
    protected float clamp(float value){
        return Mathf.clamp(value, min, max);
    }

    /**
     * Sets the range of this progress bar. The progress bar's current value is clamped to the range.
     * 设置此进度条的范围。进度条当前值会被钳制在该范围内。
     */
    public void setRange(float min, float max){
        if(min > max) throw new IllegalArgumentException("min must be <= max");
        this.min = min;
        this.max = max;
        if(value < min)
            setValue(min);
        else if(value > max) setValue(max);
    }

    @Override
    public float getPrefWidth(){
        if(vertical){
            final Drawable knob = getKnobDrawable();
            final Drawable bg = (disabled && style.disabledBackground != null) ? style.disabledBackground : style.background;
            return Math.max(knob == null ? 0 : knob.getMinWidth(), bg.getMinWidth());
        }else
            return 140;
    }

    @Override
    public float getPrefHeight(){
        if(vertical)
            return 140;
        else{
            final Drawable knob = getKnobDrawable();
            final Drawable bg = (disabled && style.disabledBackground != null) ? style.disabledBackground : style.background;
            return Math.max(knob == null ? 0 : knob.getMinHeight(), bg == null ? 0 : bg.getMinHeight());
        }
    }

    public float getMinValue(){
        return this.min;
    }

    public float getMaxValue(){
        return this.max;
    }

    public float getStepSize(){
        return this.stepSize;
    }

    public void setStepSize(float stepSize){
        if(stepSize < 0) throw new IllegalArgumentException("steps must be finite and >= 0: " + stepSize);
        this.stepSize = stepSize;
    }

    /**
     * If > 0, changes to the progress bar value via {@link #setValue(float)} will happen over this duration in seconds.
     * 若 > 0,通过 {@link #setValue(float)} 改变进度条值时,将在该时长(秒)内过渡完成。
     */
    public void setAnimateDuration(float duration){
        this.animateDuration = duration;
    }

    /**
     * Sets the interpolation to use for {@link #setAnimateDuration(float)}.
     * 设置 {@link #setAnimateDuration(float)} 使用的插值。
     */
    public void setAnimateInterpolation(Interp animateInterpolation){
        if(animateInterpolation == null) throw new IllegalArgumentException("animateInterpolation cannot be null.");
        this.animateInterpolation = animateInterpolation;
    }

    /**
     * Sets the interpolation to use for display.
     * 设置显示时使用的插值。
     */
    public void setVisualInterpolation(Interp interpolation){
        this.visualInterpolation = interpolation;
    }

    /**
     * If true (the default), inner Drawable positions and sizes are rounded to integers.
     * 为 true(默认)时,内部 Drawable 的位置和尺寸会四舍五入为整数。
     */
    public void setRound(boolean round){
        this.round = round;
    }

    @Override
    public boolean isDisabled(){
        return disabled;
    }

    @Override
    public void setDisabled(boolean disabled){
        this.disabled = disabled;
    }

    /**
     * True if the progress bar is vertical, false if it is horizontal.
     * 进度条为垂直方向时为 true,水平方向时为 false。
     */
    public boolean isVertical(){
        return vertical;
    }

    /**
     * The style for a progress bar, see {@link ProgressBar}.
     * <p>
     * 进度条的样式,见 {@link ProgressBar}。
     * @author mzechner
     * @author Nathan Sweet
     */
    public static class ProgressBarStyle extends Style{
        /**
         * The progress bar background, stretched only in one direction. Optional.
         * 进度条背景,仅沿一个方向拉伸。可选。
         */
        public Drawable background;
        /**
         * Optional.
         * 可选。
         */
        public Drawable disabledBackground;
        /**
         * Optional, centered on the background.
         * 可选,居中于背景之上。
         */
        public Drawable knob, disabledKnob;
        /**
         * Optional.
         * 可选。
         */
        public Drawable knobBefore, knobAfter, disabledKnobBefore, disabledKnobAfter;

        public ProgressBarStyle(){
        }

        public ProgressBarStyle(Drawable background, Drawable knob){
            this.background = background;
            this.knob = knob;
        }

        public ProgressBarStyle(ProgressBarStyle style){
            this.background = style.background;
            this.disabledBackground = style.disabledBackground;
            this.knob = style.knob;
            this.disabledKnob = style.disabledKnob;
            this.knobBefore = style.knobBefore;
            this.knobAfter = style.knobAfter;
            this.disabledKnobBefore = style.disabledKnobBefore;
            this.disabledKnobAfter = style.disabledKnobAfter;
        }
    }
}