package arc.scene.style;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.*;
import arc.util.Tmp;

/**
 * Drawable for a {@link TextureRegion}.
 * <p>
 * {@link TextureRegion} 的可绘制对象。
 * @author Nathan Sweet
 */
public class TextureRegionDrawable extends BaseDrawable implements TransformDrawable{
    protected TextureRegion region;
    protected Color tint = new Color(1f, 1f, 1f);
    protected float scale = 1f;

    /**
     * Creates an uninitialized TextureRegionDrawable. The texture region must be set before use.
     * 创建未初始化的 TextureRegionDrawable。使用前必须先设置纹理区域。
     */
    public TextureRegionDrawable(){
    }

    public TextureRegionDrawable(TextureRegion region){
        setRegion(region);
    }

    public TextureRegionDrawable(TextureRegion region, float scale){
        this.scale = scale;
        setRegion(region);
    }

    public TextureRegionDrawable(TextureRegionDrawable drawable){
        super(drawable);
        setRegion(drawable.region);
    }

    @Override
    public float imageSize(){
        return region.width;
    }

    @Override
    public void draw(float x, float y, float width, float height){
        Draw.color(Tmp.c1.set(tint).mul(Draw.getColor()).toFloatBits());
        Draw.rect(region, x + width/2f, y + height/2f, width, height);
    }

    @Override
    public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation){
        Draw.color(Tmp.c1.set(tint).mul(Draw.getColor()).toFloatBits());
        Draw.rect(region, x + width/2f, y + height/2f, width * scaleX, height * scaleY, originX, originY, rotation);
    }

    public TextureRegionDrawable set(TextureRegion region){
        setRegion(region);
        return this;
    }

    public TextureRegion getRegion(){
        return region;
    }

    public void setRegion(TextureRegion region){
        this.region = region;
        setMinWidth(Scl.scl(scale * region.width));
        setMinHeight(Scl.scl(scale * region.height));
    }

    /**
     * Creates a new drawable that renders the same as this drawable tinted the specified color.
     * 创建一个新可绘制对象,渲染效果与本可绘制对象相同,但以指定颜色着色。
     */
    public Drawable tint(float r, float g, float b, float a){
        return tint(Tmp.c1.set(r, g, b, a));
    }

    /**
     * Creates a new drawable that renders the same as this drawable tinted the specified color.
     * 创建一个新可绘制对象,渲染效果与本可绘制对象相同,但以指定颜色着色。
     */
    public Drawable tint(Color tint){
        TextureRegionDrawable drawable = new TextureRegionDrawable(region);
        drawable.tint.set(tint);
        return drawable;
    }
}
