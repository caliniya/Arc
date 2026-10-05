package arc.scene.flabel;

import arc.math.*;

/**
 * Abstract text effect.
 * 抽象文本效果。
 */
public abstract class FEffect{
    private static final float fadeoutSplit = 0.25f;

    public int indexStart = -1;
    public int indexEnd = -1;
    public float duration = Float.POSITIVE_INFINITY;
    public String endToken;
    protected float totalTime;

    /**
     * Applies token parameters from the parsed FLabel. Uses semicolons as separators.
     * 应用从解析后的 FLabel 得到的令牌参数。使用分号作为分隔符。
     */
    public void applyParams(String[] params){

    }

    public void update(float delta){
        totalTime += delta;
    }

    /**
     * Applies the effect to the given glyph.
     * 将该效果应用到给定的字形。
     */
    public final void apply(FLabel label, FGlyph glyph, int glyphIndex, float delta){
        int localIndex = glyphIndex - indexStart;
        onApply(label, glyph, localIndex, delta);
    }

    /**
     * Called when this effect should be applied to the given glyph.
     * 当此效果应应用到给定字形时调用。
     */
    protected abstract void onApply(FLabel label, FGlyph glyph, int localIndex, float delta);

    /**
     * Returns whether or not this effect is finished and should be removed. Note that effects are infinite by default.
     * 返回此效果是否已结束并应被移除。注意,效果默认是无限持续的。
     */
    public boolean isFinished(){
        return totalTime > duration;
    }

    /**
     * Calculates the fadeout of this effect, if any. Only considers the second half of the duration.
     * 计算此效果的淡出(如有)。只考虑持续时间的后半段。
     */
    protected float calculateFadeout(){
        if(Float.isInfinite(duration)) return 1;

        // Calculate raw progress
        // 计算原始进度
        float progress = Mathf.clamp(totalTime / duration, 0, 1);

        // If progress is before the split point, return a full factor
        // 如果进度在分界点之前,则返回完整系数
        if(progress < fadeoutSplit) return 1;

        // Otherwise calculate from the split point
        // 否则从分界点开始计算
        return Interp.smooth.apply(1, 0, (progress - fadeoutSplit) / (1f - fadeoutSplit));
    }

    /**
     * Calculates a linear progress dividing the total time by the given modifier. Returns a value between 0 and 1 that
     * loops in a ping-pong mode.
     * <p>
     * 计算线性进度,将总时间除以给定的修饰值。返回一个在 0 到 1 之间以乒乓模式循环的值。
     */
    protected float calculateProgress(float modifier){
        return calculateProgress(modifier, 0, true);
    }

    /**
     * Calculates a linear progress dividing the total time by the given modifier. Returns a value between 0 and 1 that
     * loops in a ping-pong mode.
     * <p>
     * 计算线性进度,将总时间除以给定的修饰值。返回一个在 0 到 1 之间以乒乓模式循环的值。
     */
    protected float calculateProgress(float modifier, float offset){
        return calculateProgress(modifier, offset, true);
    }

    /**
     * Calculates a linear progress dividing the total time by the given modifier. Returns a value between 0 and 1.
     * 计算线性进度,将总时间除以给定的修饰值。返回一个介于 0 和 1 之间的值。
     */
    protected float calculateProgress(float modifier, float offset, boolean pingpong){
        float progress = totalTime / modifier + offset;
        while(progress < 0.0f){
            progress += 2.0f;
        }
        if(pingpong){
            progress %= 2f;
            if(progress > 1.0f) progress = 1f - (progress - 1f);
        }else{
            progress %= 1.0f;
        }
        progress = Mathf.clamp(progress, 0, 1);
        return progress;
    }

    /**
     * Returns the line height of the label controlling this effect.
     * 返回控制此效果的标签的行高。
     */
    protected float getLineHeight(FLabel label){
        return label.getFontCache().getFont().getLineHeight() * label.getFontScaleY();
    }

}
