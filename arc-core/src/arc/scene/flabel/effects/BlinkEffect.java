package arc.scene.flabel.effects;

import arc.scene.flabel.*;
import arc.graphics.*;
import arc.util.*;

/**
 * Blinks the entire text in two different colors at once, without interpolation.
 * 让整个文本同时以两种不同的颜色闪烁,不做插值。
 */
public class BlinkEffect extends FEffect{
    private static final float defaultFrequency = 1f;

    public Color color1 = new Color(Color.white); // First color of the effect.
    // 效果的第一种颜色。
    public Color color2 = new Color(Color.white); // Second color of the effect.
    // 效果的第二种颜色。
    public float frequency = 1; // How frequently the color pattern should move through the text.
    // 颜色图案在文本中移动的频率。
    public float threshold = 0.5f; // Point to switch colors.
    // 切换颜色的分界点。

    @Override
    public void applyParams(String[] params){
        if(params.length > 0) color1 = Strings.parseColor(params[0], color1);
        if(params.length > 1) color2 = Strings.parseColor(params[1], color2);
        if(params.length > 2) frequency = Strings.parseFloat(params[2], 1f);
        if(params.length > 3) threshold = Strings.parseFloat(params[3], 0.5f);
    }

    @Override
    protected void onApply(FLabel label, FGlyph glyph, int localIndex, float delta){
        // Calculate progress
        // 计算进度
        float frequencyMod = (1f / frequency) * defaultFrequency;
        float progress = calculateProgress(frequencyMod);

        // Calculate color
        // 计算颜色
        if(glyph.color == null) glyph.color = new Color(Color.white);
        glyph.color.set(progress <= threshold ? color1 : color2);
    }

}
