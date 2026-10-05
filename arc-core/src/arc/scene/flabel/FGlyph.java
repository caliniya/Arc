package arc.scene.flabel;

import arc.graphics.*;
import arc.graphics.font.Font.*;
import arc.graphics.g2d.GlyphLayout.*;
import arc.util.pooling.Pool.*;

/**
 * Extension of {@link Glyph} with additional data exposed to the user.
 * {@link Glyph} 的扩展,向用户暴露了额外的数据。
 */
public class FGlyph extends Glyph implements Poolable{
    /**
     * {@link GlyphRun} this glyph belongs to.
     * 此字形所属的 {@link GlyphRun}。
     */
    public GlyphRun run = null;
    /**
     * Internal index associated with this glyph. Internal use only. Defaults to -1.
     * 与此字形关联的内部索引。仅供内部使用。默认为 -1。
     */
    int internalIndex = -1;
    /**
     * Color of this glyph. If set to null, the run's color will be used. Defaults to null.
     * 此字形的颜色。如果设为 null,则使用所在 run 的颜色。默认为 null。
     */
    public Color color = null;

    public void set(Glyph from){
        id = from.id;
        srcX = from.srcX;
        srcY = from.srcY;
        width = from.width;
        height = from.height;
        texture = from.texture;
        u = from.u;
        v = from.v;
        u2 = from.u2;
        v2 = from.v2;
        xoffset = from.xoffset;
        yoffset = from.yoffset;
        xadvance = from.xadvance;
        kerning = from.kerning; // Keep the same instance, there's no reason to deep clone it
        // 保持同一个实例,没有理由对它进行深拷贝
        fixedWidth = from.fixedWidth;

        run = null;
        internalIndex = -1;
        color = null;
    }

    @Override
    public void reset(){
        id = 0;
        srcX = 0;
        srcY = 0;
        width = 0;
        height = 0;
        texture = null;
        u = 0;
        v = 0;
        u2 = 0;
        v2 = 0;
        xoffset = 0;
        yoffset = 0;
        xadvance = 0;
        kerning = null;
        fixedWidth = false;

        run = null;
        internalIndex = -1;
        color = null;
    }

}
