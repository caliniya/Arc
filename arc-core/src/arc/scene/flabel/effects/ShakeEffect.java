package arc.scene.flabel.effects;

import arc.scene.flabel.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;

/**
 * Shakes the text in a random pattern.
 * 以随机模式抖动文本。
 */
public class ShakeEffect extends FEffect{
    private static final float defaultDistance = 0.12f, defaultIntensity = 0.5f;

    private final FloatAr lastOffsets = new FloatAr();

    public float distance = 1; // How far the glyphs should move
    // 字形应移动的距离
    public float intensity = 1; // How fast the glyphs should move
    // 字形移动的快慢

    @Override
    public void applyParams(String[] params){
        if(params.length > 0) distance = Strings.parseFloat(params[0], 1f);
        if(params.length > 1) intensity = Strings.parseFloat(params[1], 1f);
    }

    @Override
    protected void onApply(FLabel label, FGlyph glyph, int localIndex, float delta){
        // Make sure we can hold enough entries for the current index
        // 确保能容纳当前索引所需的条目数量
        if(localIndex >= lastOffsets.size / 2){
            lastOffsets.setSize(lastOffsets.size + 16);
        }

        // Get last offsets
        // 获取上一次的偏移
        float lastX = lastOffsets.get(localIndex * 2);
        float lastY = lastOffsets.get(localIndex * 2 + 1);

        // Calculate new offsets
        // 计算新的偏移
        float x = getLineHeight(label) * distance * Mathf.random(-1, 1) * defaultDistance;
        float y = getLineHeight(label) * distance * Mathf.random(-1, 1) * defaultDistance;

        // Apply intensity
        // 应用强度
        float normalIntensity = Mathf.clamp(intensity * defaultIntensity, 0, 1);
        x = Interp.linear.apply(lastX, x, normalIntensity);
        y = Interp.linear.apply(lastY, y, normalIntensity);

        // Apply fadeout
        // 应用淡出
        float fadeout = calculateFadeout();
        x *= fadeout;
        y *= fadeout;
        x = Math.round(x);
        y = Math.round(y);

        // Store offsets for the next tick
        // 保存偏移量,供下一帧使用
        lastOffsets.set(localIndex * 2, x);
        lastOffsets.set(localIndex * 2 + 1, y);

        // Apply changes
        // 应用更改
        glyph.xoffset += x;
        glyph.yoffset += y;
    }

}
