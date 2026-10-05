package arc.scene.flabel.effects;

import arc.scene.flabel.*;
import arc.math.*;
import arc.util.*;

/**
 * Makes the text jumps and falls as if there was gravity.
 * 让文本像受重力作用一样跳跃和下落。
 */
public class JumpEffect extends FEffect{
    private static final float defaultFrequency = 50f, defaultDistance = 1.33f, defaultIntensity = 1f;

    public float distance = 1; // How much of their height they should move
    // 移动距离相对于自身高度的比例
    public float frequency = 1; // How frequently the wave pattern repeats
    // 波形图案的重复频率
    public float intensity = 1; // How fast the glyphs should move
    // 字形移动的快慢

    @Override
    public void applyParams(String[] params){
        if(params.length > 0) distance = Strings.parseFloat(params[0], 1f);
        if(params.length > 1) frequency = Strings.parseFloat(params[1], 1f);
        if(params.length > 2) intensity = Strings.parseFloat(params[2], 1f);
    }

    @Override
    protected void onApply(FLabel label, FGlyph glyph, int localIndex, float delta){
        // Calculate progress
        // 计算进度
        float progressModifier = (1f / intensity) * defaultIntensity;
        float normalFrequency = (1f / frequency) * defaultFrequency;
        float progressOffset = localIndex / normalFrequency;
        float progress = calculateProgress(progressModifier, -progressOffset, false);

        // Calculate offset
        // 计算偏移
        float interpolation = 0;
        float split = 0.2f;
        if(progress < split){
            interpolation = Interp.pow2Out.apply(0, 1, progress / split);
        }else{
            interpolation = Interp.bounceOut.apply(1, 0, (progress - split) / (1f - split));
        }
        float y = getLineHeight(label) * distance * interpolation * defaultDistance;

        // Calculate fadeout
        // 计算淡出
        float fadeout = calculateFadeout();
        y *= fadeout;

        // Apply changes
        // 应用更改
        glyph.yoffset += y;
    }

}
