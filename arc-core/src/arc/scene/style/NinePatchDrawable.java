package arc.scene.style;

import arc.graphics.Color;
import arc.graphics.g2d.NinePatch;

/**
 * Drawable for a {@link NinePatch}.
 * <p>
 * The drawable sizes are set when the ninepatch is set, but they are separate values. Eg, {@link Drawable#getLeftWidth()} could
 * be set to more than {@link NinePatch#getLeftWidth()} in order to provide more space on the left than actually exists in the
 * ninepatch.
 * <p>
 * The min size is set to the ninepatch total size by default. It could be set to the left+right and top+bottom, excluding the
 * middle size, to allow the drawable to be sized down as small as possible.
 * <p>
 * {@link NinePatch} 的可绘制对象。 <p> 设置 ninepatch 时会确定可绘制对象的尺寸,但两者是相互独立的值。例如,可以将 {@link Drawable#getLeftWidth()} 设置为大于 {@link NinePatch#getLeftWidth()} 的值,以便在左侧提供比 ninepatch 中实际存在的更多的空间。 <p> 最小尺寸默认为 ninepatch 的总尺寸。也可以将其设置为左+右、上+下之和(不含中间区域),从而使可绘制对象能缩小到尽可能小的尺寸。
 * @author Nathan Sweet
 */
public class NinePatchDrawable extends BaseDrawable implements TransformDrawable{
    protected NinePatch patch;

    /**
     * Creates an uninitialized NinePatchDrawable. The ninepatch must be {@link #setPatch(NinePatch) set} before use.
     * 创建未初始化的 NinePatchDrawable。使用前必须先 {@link #setPatch(NinePatch) 设置} ninepatch。
     */
    public NinePatchDrawable(){
    }

    public NinePatchDrawable(NinePatch patch){
        setPatch(patch);
    }

    public NinePatchDrawable(NinePatchDrawable drawable){
        super(drawable);
        setPatch(drawable.patch);
    }

    @Override
    public void draw(float x, float y, float width, float height){
        patch.draw(x, y, width, height);
    }

    @Override
    public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation){
        patch.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
    }

    public NinePatch getPatch(){
        return patch;
    }

    public void setPatch(NinePatch patch){
        this.patch = patch;
        setMinWidth(patch.getTotalWidth());
        setMinHeight(patch.getTotalHeight());
        setTopHeight(patch.getPadTop());
        setRightWidth(patch.getPadRight());
        setBottomHeight(patch.getPadBottom());
        setLeftWidth(patch.getPadLeft());
    }

    /**
     * Creates a new drawable that renders the same as this drawable tinted the specified color.
     * 创建一个新可绘制对象,渲染效果与本可绘制对象相同,但以指定颜色着色。
     */
    public NinePatchDrawable tint(Color tint){
        NinePatchDrawable drawable = new NinePatchDrawable(this);
        drawable.setPatch(new NinePatch(drawable.getPatch(), tint));
        return drawable;
    }

    /**
     * Creates a new drawable that renders the same as this drawable with a top-to-bottom gradient tint.
     * 创建一个新可绘制对象,渲染效果与本可绘制对象相同,但使用自上而下的渐变着色。
     */
    public NinePatchDrawable tint(Color top, Color bottom){
        NinePatchDrawable drawable = new NinePatchDrawable(this);
        drawable.setPatch(new NinePatch(drawable.getPatch(), top, bottom));
        return drawable;
    }
}
