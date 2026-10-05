package arc.scene.flabel.effects;

import arc.scene.flabel.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;

/**
 * Moves the text vertically easing it into the final position. Doesn't repeat itself.
 * 垂直移动文本,以缓动方式进入最终位置。不会重复。
 */
public class EaseEffect extends FEffect{
    private static final float defaultDistance = 0.15f;
    private static final float defaultIntensity = 0.075f;

    public float distance = 1; // How much of their height they should move
    // 移动距离相对于自身高度的比例
    public float intensity = 1; // How fast the glyphs should move
    // 字形移动的快慢
    public boolean elastic = false; // Whether or not the glyphs have an elastic movement
    // 字形是否带有弹性运动

    private IntFloatMap timePassedByGlyphIndex = new IntFloatMap();

    @Override
    public void applyParams(String[] params){
        if(params.length > 0) distance = Strings.parseFloat(params[0], 1f);
        if(params.length > 1) intensity = Strings.parseFloat(params[1], 1f);
        if(params.length > 2) elastic = Boolean.parseBoolean(params[2]);
    }

    @Override
    protected void onApply(FLabel label, FGlyph glyph, int localIndex, float delta){
        // Calculate real intensity
        // 计算实际强度
        float realIntensity = intensity * (elastic ? 3f : 1f) * defaultIntensity;

        // Calculate progress
        // 计算进度
        float timePassed = timePassedByGlyphIndex.increment(localIndex, 0, delta);
        float progress = timePassed / realIntensity;
        if(progress < 0 || progress > 1){
            return;
        }

        // Calculate offset
        // 计算偏移
        Interp interpolation = elastic ? Interp.swingOut : Interp.sine;
        float interpolatedValue = interpolation.apply(1, 0, progress);
        float y = getLineHeight(label) * distance * interpolatedValue * defaultDistance;

        // Apply changes
        // 应用更改
        glyph.yoffset += y;
    }

}
