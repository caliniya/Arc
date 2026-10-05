package arc.graphics.g2d;

import arc.graphics.*;

/**
 * Defines a region of a pixmap, like a TextureRegion.
 * 定义 pixmap 的一个区域,类似于 TextureRegion。
 */
public class PixmapRegion{
    public Pixmap pixmap;
    public int x, y, width, height;

    public PixmapRegion(Pixmap pixmap, int x, int y, int width, int height){
        set(pixmap, x, y, width, height);
    }

    public PixmapRegion(Pixmap pixmap){
        set(pixmap);
    }

    /**
     * @return the RGBA value at a region position.
     * @return the RGBA value at a region position. 区域位置处的 RGBA 值。
     */
    public int get(int x, int y){
        return pixmap.get(this.x + x, this.y + y);
    }

    /**
     * @return the RGBA value at a region position without bounds checks.
     * @return the RGBA value at a region position without bounds checks. 区域位置处的 RGBA 值,不进行边界检查。
     */
    public int getRaw(int x, int y){
        return pixmap.getRaw(this.x + x, this.y + y);
    }

    /**
     * @return the alpha value at a region position, 0 - 255.
     * @return the alpha value at a region position, 0 - 255. 区域位置处的 alpha 值(0 - 255)。
     */
    public int getA(int x, int y){
        return pixmap.getA(this.x + x, this.y + y);
    }

    public int get(int x, int y, Color color){
        int c = get(x, y);
        color.set(c);
        return c;
    }

    public PixmapRegion set(Pixmap pixmap){
        return set(pixmap, 0, 0, pixmap.width, pixmap.height);
    }

    public PixmapRegion set(Pixmap pixmap, int x, int y, int width, int height){
        this.pixmap = pixmap;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Allocates a new pixmap based on this region data.
     * 基于此区域数据分配新的 pixmap。
     */
    public Pixmap crop(){
        return Pixmaps.crop(pixmap, x, y, width, height);
    }

    /**
     * Allocates a new pixmap with specific offsets.
     * 以指定偏移量分配新的 pixmap。
     */
    public Pixmap crop(int x, int y, int width, int height){
        return Pixmaps.crop(pixmap, this.x + x, this.y + y, width, height);
    }
}
