package arc.scene.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.util.*;

/**
 * Displays a {@link Drawable}, scaled various way within the widgets bounds. The preferred size is the min size of the drawable.
 * Only when using a {@link TextureRegionDrawable} will the actor's scale, rotation, and origin be used when drawing.
 * <p>
 * 显示一个 {@link Drawable},在控件边界内以多种方式进行缩放。首选大小为可绘制对象的最小尺寸。只有使用 {@link TextureRegionDrawable} 时,绘制才会使用元素的缩放、旋转和原点。
 * @author Nathan Sweet
 */
public class Image extends Element{
    protected float imageX, imageY, imageWidth, imageHeight;
    protected Scaling scaling;
    protected int align;
    protected Drawable drawable;

    /**
     * Creates an image with no region or patch, stretched, and aligned center.
     * 创建一个未设置区域或九宫格、拉伸显示且居中对齐的图像。
     */
    public Image(){
        this(Core.atlas.has("whiteui") ? Core.atlas.find("whiteui") : Core.atlas.find("white"));
    }


    public Image(Drawable name, Color color){
        this(name);
        setColor(color);
    }

    /**
     * Creates an image stretched, and aligned center.
     * <p>
     * 创建一个拉伸显示且居中对齐的图像。
     * @param patch May be null. 可以为 null。
     */
    public Image(NinePatch patch){
        this(new NinePatchDrawable(patch), Scaling.stretch, Align.center);
    }

    /**
     * Creates an image stretched, and aligned center.
     * <p>
     * 创建一个拉伸显示且居中对齐的图像。
     * @param region May be null. 可以为 null。
     */
    public Image(TextureRegion region){
        this(new TextureRegionDrawable(region), Scaling.stretch, Align.center);
    }

    /**
     * Creates an image stretched, and aligned center.
     * 创建一个拉伸显示且居中对齐的图像。
     */
    public Image(Texture texture){
        this(new TextureRegionDrawable(new TextureRegion(texture)));
    }

    /**
     * Creates an image stretched, and aligned center.
     * <p>
     * 创建一个拉伸显示且居中对齐的图像。
     * @param drawable May be null. 可以为 null。
     */
    public Image(Drawable drawable){
        this(drawable, Scaling.stretch, Align.center);
    }

    /**
     * Creates an image aligned center.
     * <p>
     * 创建一个居中对齐的图像。
     * @param drawable May be null. 可以为 null。
     */
    public Image(Drawable drawable, Scaling scaling){
        this(drawable, scaling, Align.center);
    }

    /** @param drawable May be null. 可以为 null。 */
    public Image(Drawable drawable, Scaling scaling, int align){
        setDrawable(drawable);
        this.scaling = scaling;
        this.align = align;
        setSize(getPrefWidth(), getPrefHeight());
    }

    @Override
    public void layout(){
        if(drawable == null) return;

        float regionWidth = drawable.getMinWidth();
        float regionHeight = drawable.getMinHeight();
        float width = getWidth();
        float height = getHeight();

        Vec2 size = scaling.apply(regionWidth, regionHeight, width, height);
        imageWidth = size.x;
        imageHeight = size.y;

        if((align & Align.left) != 0)
            imageX = 0;
        else if((align & Align.right) != 0)
            imageX = (int)(width - imageWidth);
        else
            imageX = (int)(width / 2 - imageWidth / 2);

        if((align & Align.top) != 0)
            imageY = (int)(height - imageHeight);
        else if((align & Align.bottom) != 0)
            imageY = 0;
        else
            imageY = (int)(height / 2 - imageHeight / 2);
    }

    @Override
    public void draw(){
        validate();

        float x = this.x;
        float y = this.y;
        float scaleX = this.scaleX;
        float scaleY = this.scaleY;
        Draw.color(color);
        Draw.alpha(parentAlpha * color.a);

        if(drawable instanceof TransformDrawable){
            float rotation = getRotation();
            if(scaleX != 1 || scaleY != 1 || rotation != 0){
                drawable.draw(x + imageX, y + imageY, originX - imageX, originY - imageY,
                imageWidth, imageHeight, scaleX, scaleY, rotation);
                return;
            }
        }
        if(drawable != null) drawable.draw(x + imageX, y + imageY, imageWidth * scaleX, imageHeight * scaleY);
    }

    public TextureRegion getRegion(){
        return ((TextureRegionDrawable)drawable).getRegion();
    }

    /** @return May be null. 可以为 null。 */
    public Drawable getDrawable(){
        return drawable;
    }

    public void setDrawable(TextureRegion region){
        setDrawable(new TextureRegionDrawable(region));
    }

    /** @param drawable May be null. 可以为 null。 */
    public void setDrawable(Drawable drawable){
        if(this.drawable == drawable) return;
        if(drawable != null){
            if(getPrefWidth() != drawable.getMinWidth() || getPrefHeight() != drawable.getMinHeight())
                invalidateHierarchy();
        }else
            invalidateHierarchy();
        this.drawable = drawable;
    }

    public Image setScaling(Scaling scaling){
        if(scaling == null) throw new IllegalArgumentException("scaling cannot be null.");
        this.scaling = scaling;
        invalidate();

        return this;
    }

    public void setAlign(int align){
        this.align = align;
        invalidate();
    }

    @Override
    public float getMinWidth(){
        return 0;
    }

    @Override
    public float getMinHeight(){
        return 0;
    }

    @Override
    public float getPrefWidth(){
        if(drawable != null) return drawable.getMinWidth();
        return 0;
    }

    @Override
    public float getPrefHeight(){
        if(drawable != null) return drawable.getMinHeight();
        return 0;
    }

    public float getImageX(){
        return imageX;
    }

    public float getImageY(){
        return imageY;
    }

    public float getImageWidth(){
        return imageWidth;
    }

    public float getImageHeight(){
        return imageHeight;
    }
}
