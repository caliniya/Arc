package arc.scene.flabel.effects;

import arc.math.*;
import arc.scene.flabel.*;
import arc.util.*;

/**
 * Moves the text vertically in a sine wave pattern.
 * 让文本按正弦波图案垂直移动。
 */
public class WaveEffect extends FEffect{
    private static final float defaultFrequency = 15f, defaultDistance = 0.33f, defaultIntensity = 0.5f;

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
        float progress = calculateProgress(progressModifier, progressOffset);

        // Calculate offset
        // 计算偏移
        float y = getLineHeight(label) * distance * Interp.sine.apply(-1, 1, progress) * defaultDistance;

        // Calculate fadeout
        // 计算淡出
        float fadeout = calculateFadeout();
        y *= fadeout;

        // Apply changes
        // 应用更改
        glyph.yoffset += y;
    }

}
