package arc.graphics.g2d;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.TextureAtlas.*;

/**
 * Defines a rectangular area of a texture. The coordinate system used has its origin in the upper left corner with the x-axis
 * pointing to the right and the y axis pointing downwards.
 * <p>
 * 定义纹理中的一个矩形区域。所用坐标系原点位于左上角,x 轴向右,y 轴向下。
 * @author mzechner
 * @author Nathan Sweet
 */
public class TextureRegion{
    public Texture texture;

    /**
     * Read-only. Use setters to change.
     * 只读。请使用 setter 修改。
     */
    public float u, v, u2, v2;
    /**
     * Read-only. Use setters to change.
     * 只读。请使用 setter 修改。
     */
    public int width, height;

    public float scale = 1.0f;

    /**
     * Constructs a region with no texture and no coordinates defined.
     * 构造未定义纹理和坐标的区域。
     */
    public TextureRegion(){
    }

    /**
     * Constructs a region the size of the specified texture.
     * 构造与指定纹理等大的区域。
     */
    public TextureRegion(Texture texture){
        if(texture == null) throw new IllegalArgumentException("texture cannot be null.");
        this.texture = texture;
        set(0, 0, texture.width, texture.height);
    }

    /**
     * @param width The width of the texture region. May be negative to flip the sprite when drawn. 纹理区域的宽度。可为负,绘制时精灵将翻转。
     * @param height The height of the texture region. May be negative to flip the sprite when drawn. 纹理区域的高度。可为负,绘制时精灵将翻转。
     */
    public TextureRegion(Texture texture, int width, int height){
        this.texture = texture;
        set(0, 0, width, height);
    }

    /**
     * @param width The width of the texture region. May be negative to flip the sprite when drawn. 纹理区域的宽度。可为负,绘制时精灵将翻转。
     * @param height The height of the texture region. May be negative to flip the sprite when drawn. 纹理区域的高度。可为负,绘制时精灵将翻转。
     */
    public TextureRegion(Texture texture, int x, int y, int width, int height){
        this.texture = texture;
        set(x, y, width, height);
    }

    public TextureRegion(Texture texture, float u, float v, float u2, float v2){
        this.texture = texture;
        set(u, v, u2, v2);
    }

    /**
     * Constructs a region with the same texture and coordinates of the specified region.
     * 构造与指定区域具有相同纹理和坐标的区域。
     */
    public TextureRegion(TextureRegion region){
        set(region);
    }

    /**
     * Constructs a region with the same texture as the specified region and sets the coordinates relative to the specified region.
     * <p>
     * 构造与指定区域纹理相同的区域,并设置相对于该区域的坐标。
     * @param width The width of the texture region. May be negative to flip the sprite when drawn. 纹理区域的宽度。可为负,绘制时精灵将翻转。
     * @param height The height of the texture region. May be negative to flip the sprite when drawn. 纹理区域的高度。可为负,绘制时精灵将翻转。
     */
    public TextureRegion(TextureRegion region, int x, int y, int width, int height){
        set(region, x, y, width, height);
    }

    /**
     * Helper function to create tiles out of the given {@link Texture} starting from the top left corner going to the right and
     * ending at the bottom right corner. Only complete tiles will be returned so if the texture's width or height are not a
     * multiple of the tile width and height not all of the texture will be used.
     * <p>
     * 辅助函数,从给定 {@link Texture} 的左上角开始向右、直到右下角切分出图块。只返回完整的图块,因此若纹理的宽或高不是图块宽高的整数倍,纹理不会被全部使用。
     * @param texture the Texture 纹理
     * @param tileWidth a tile's width in pixels 图块宽度(像素)
     * @param tileHeight a tile's height in pixels 图块高度(像素)
     * @return a 2D array of TextureRegions indexed by [row][column]. 按 [row][column] 索引的 TextureRegion 二维数组。
     */
    public static TextureRegion[][] split(Texture texture, int tileWidth, int tileHeight){
        TextureRegion region = new TextureRegion(texture);
        return region.split(tileWidth, tileHeight);
    }

    public AtlasRegion asAtlas(){
        return (AtlasRegion)this;
    }

    public boolean found(){
        return Core.atlas != null && Core.atlas.error != this;
    }

    /**
     * Sets the texture and sets the coordinates to the size of the specified texture.
     * 设置纹理,并将坐标设为该纹理的大小。
     */
    public void set(Texture texture){
        this.texture = texture;
        set(0, 0, texture.width, texture.height);
    }

    /**
     * @param width The width of the texture region. May be negative to flip the sprite when drawn. 纹理区域的宽度。可为负,绘制时精灵将翻转。
     * @param height The height of the texture region. May be negative to flip the sprite when drawn. 纹理区域的高度。可为负,绘制时精灵将翻转。
     */
    public TextureRegion set(int x, int y, int width, int height){
        float invTexWidth = 1f / texture.width;
        float invTexHeight = 1f / texture.height;
        set(x * invTexWidth, y * invTexHeight, (x + width) * invTexWidth, (y + height) * invTexHeight);
        this.width = Math.abs(width);
        this.height = Math.abs(height);

        return this;
    }

    public void set(float u, float v, float u2, float v2){
        int texWidth = texture.width, texHeight = texture.height;
        width = Math.round(Math.abs(u2 - u) * texWidth);
        height = Math.round(Math.abs(v2 - v) * texHeight);

        // For a 1x1 region, adjust UVs toward pixel center to avoid filtering artifacts on AMD GPUs when drawing very stretched.
        // 对于 1x1 区域,将 UV 调整到像素中心,以避免在 AMD GPU 上极度拉伸绘制时出现过滤瑕疵。
        if(width == 1 && height == 1){
            float adjustX = 0.25f / texWidth;
            u += adjustX;
            u2 -= adjustX;
            float adjustY = 0.25f / texHeight;
            v += adjustY;
            v2 -= adjustY;
        }

        this.u = u;
        this.v = v;
        this.u2 = u2;
        this.v2 = v2;
    }

    /**
     * Sets the texture and coordinates to the specified region.
     * 将纹理和坐标设置为指定区域。
     */
    public void set(TextureRegion region){
        texture = region.texture;
        scale = region.scale;
        u = region.u;
        v = region.v;
        u2 = region.u2;
        v2 = region.v2;
        width = region.width;
        height = region.height;
    }

    /**
     * Sets the texture to that of the specified region and sets the coordinates relative to the specified region.
     * 将纹理设为指定区域的纹理,并设置相对于该区域的坐标。
     */
    public void set(TextureRegion region, int x, int y, int width, int height){
        texture = region.texture;
        scale = region.scale;
        set(region.getX() + x, region.getY() + y, width, height);
    }

    /**
     * Sets the texture to that of the specified region and sets the coordinates relative to the specified region.
     * 将纹理设为指定区域的纹理,并设置相对于该区域的坐标。
     */
    public void set(Texture texture, int x, int y, int width, int height){
        this.texture = texture;
        set(x, y, width, height);
    }

    public void setU(float u){
        this.u = u;
        width = Math.round(Math.abs(u2 - u) * texture.width);
    }

    public void setV(float v){
        this.v = v;
        height = Math.round(Math.abs(v2 - v) * texture.height);
    }

    public void setU2(float u2){
        this.u2 = u2;
        width = Math.round(Math.abs(u2 - u) * texture.width);
    }

    public void setV2(float v2){
        this.v2 = v2;
        height = Math.round(Math.abs(v2 - v) * texture.height);
    }

    public int getX(){
        return Math.round(u * texture.width);
    }

    public void setX(int x){
        setU(x / (float)texture.width);
    }

    public void setX(float x){
        setU(x / texture.width);
    }

    public int getY(){
        return Math.round(v * texture.height);
    }

    public void setY(int y){
        setV(y / (float)texture.height);
    }

    public void setY(float y){
        setV(y / texture.height);
    }

    public void setWidth(int width){
        if(isFlipX()){
            setU(u2 + width / (float)texture.width);
        }else{
            setU2(u + width / (float)texture.width);
        }
    }

    public void setHeight(int height){
        if(isFlipY()){
            setV(v2 + height / (float)texture.height);
        }else{
            setV2(v + height / (float)texture.height);
        }
    }

    public void setWidth(float width){
        if(isFlipX()){
            setU(u2 + width / texture.width);
        }else{
            setU2(u + width / texture.width);
        }
    }

    public void setHeight(float height){
        if(isFlipY()){
            setV(v2 + height / texture.height);
        }else{
            setV2(v + height / texture.height);
        }
    }

    public void flip(boolean x, boolean y){
        if(x){
            float temp = u;
            u = u2;
            u2 = temp;
        }
        if(y){
            float temp = v;
            v = v2;
            v2 = temp;
        }
    }

    /**
     * @return x/y aspect ratio
     * @return x/y aspect ratio x/y 宽高比
     */
    public float ratio(){
        return (float)width / height;
    }

    public boolean isFlipX(){
        return u > u2;
    }

    public boolean isFlipY(){
        return v > v2;
    }

    /**
     * Offsets the region relative to the current region. Generally the region's size should be the entire size of the texture in
     * the direction(s) it is scrolled.
     * <p>
     * 相对于当前区域偏移该区域。通常,在滚动的方向上区域大小应为整个纹理的大小。
     * @param xAmount The percentage to offset horizontally. 水平偏移的百分比。
     * @param yAmount The percentage to offset vertically. This is done in texture space, so up is negative. 垂直偏移的百分比。在纹理空间中进行,因此向上为负。
     */
    public void scroll(float xAmount, float yAmount){
        if(xAmount != 0){
            float width = (u2 - u) * texture.width;
            u = (u + xAmount) % 1;
            u2 = u + width / texture.width;
        }
        if(yAmount != 0){
            float height = (v2 - v) * texture.height;
            v = (v + yAmount) % 1;
            v2 = v + height / texture.height;
        }
    }

    /**
     * Helper function to create tiles out of this TextureRegion starting from the top left corner going to the right and ending at
     * the bottom right corner. Only complete tiles will be returned so if the region's width or height are not a multiple of the
     * tile width and height not all of the region will be used. This will not work on texture regions returned form a TextureAtlas
     * that either have whitespace removed or where flipped before the region is split.
     * <p>
     * 辅助函数,从此 TextureRegion 的左上角开始向右、直到右下角切分出图块。只返回完整的图块,因此若区域的宽或高不是图块宽高的整数倍,区域不会被全部使用。对来自 TextureAtlas、已去除空白或在切分前翻转过的纹理区域无效。
     * @param tileWidth a tile's width in pixels 图块宽度(像素)
     * @param tileHeight a tile's height in pixels 图块高度(像素)
     * @return a 2D array of TextureRegions indexed by [row][column]. 按 [row][column] 索引的 TextureRegion 二维数组。
     */
    public TextureRegion[][] split(int tileWidth, int tileHeight){
        if(texture == null) return null;
        int x = getX();
        int y = getY();
        int width = this.width;
        int height = this.height;

        int sw = width / tileWidth;
        int sh = height / tileHeight;

        int startX = x;
        TextureRegion[][] tiles = new TextureRegion[sw][sh];
        for(int cy = 0; cy < sh; cy++, y += tileHeight){
            x = startX;
            for(int cx = 0; cx < sw; cx++, x += tileWidth){
                tiles[cx][cy] = new TextureRegion(texture, x, y, tileWidth, tileHeight);
            }
        }

        return tiles;
    }

    public float scl(){
        return scale * Draw.scl;
    }

    public float getDepth(){
        return texture.getDepth();
    }

    @Override
    public String toString(){
        return "TextureRegion{" +
        "texture=" + texture +
        ", width=" + width +
        ", height=" + height +
        '}';
    }
}
