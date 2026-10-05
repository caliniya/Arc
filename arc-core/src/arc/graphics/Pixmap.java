package arc.graphics;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.PixmapIO.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.util.*;

import java.io.*;
import java.nio.*;

/**
 * <p>
 * A Pixmap represents an image in memory. It has a width and height expressed in pixels, with each pixel being stored in RGBA8888 format.
 * Coordinates of pixels are specified with respect to the top left corner of the image, with the x-axis pointing to the right and the y-axis pointing downwards.
 * </p>
 *
 * <p>
 * The {@link Pixmap#draw(Pixmap, int, int, int, int, int, int, int, int)} method will scale and stretch the source image to a
 * target image. There either nearest neighbour or bilinear filtering can be used.
 * </p>
 *
 * <p>
 * A Pixmap stores its data in native heap memory. It is mandatory to call {@link Pixmap#dispose()} when the pixmap is no longer
 * needed, otherwise memory leaks will result.
 * </p>
 * <p>
 * <p> Pixmap 表示内存中的一幅图像。它有以像素表示的宽和高,每个像素以 RGBA8888 格式存储。像素坐标相对于图像左上角,x 轴向右,y 轴向下。 </p> <p> {@link Pixmap#draw(Pixmap, int, int, int, int, int, int, int, int)} 方法会将源图像缩放拉伸到目标图像。可使用最近邻或双线性过滤。 </p> <p> Pixmap 将数据存储在原生堆内存中。不再需要时必须调用 {@link Pixmap#dispose()},否则会造成内存泄漏。 </p>
 * @author badlogicgames@gmail.com
 */
public class Pixmap implements Disposable{
    private static final boolean supportsBufferCopy = OS.javaVersionNumber >= 16 || (OS.isAndroid && Core.app != null && Core.app.getVersion() >= 35);

    /**
     * Size of the pixmap. Do not modify unless you know what you are doing.
     * 像素图的大小。除非清楚自己在做什么,否则请勿修改。
     */
    public int width, height;

    /**
     * Internal data, arranged as RGBA with 1 byte per component. This buffer must be direct or natively-allocated.
     * 内部数据,按 RGBA 排列,每个分量 1 字节。此缓冲区必须是直接缓冲区或原生分配的缓冲区。
     */
    public ByteBuffer pixels;

    /**
     * When natives are present, this handle is the address of the memory region.
     * When this pixmap is disposed/uninitialized, this value is 0.
     * When natives are not present, this value is -1.
     * <p>
     * 存在原生实现时,此句柄是内存区域的地址。此像素图被释放/未初始化时,该值为 0。不存在原生实现时,该值为 -1。
     */
    long handle;

    static{
        if(!supportsBufferCopy && !OS.isIos){
            UnsafeBuffers.checkInit();
        }
    }

    /**
     * Creates a new Pixmap instance with the given width and height.
     * 以给定宽高创建新的 Pixmap 实例。
     */
    public Pixmap(int width, int height){
        load(width, height);
    }

    /** @see #Pixmap(byte[], int, int) */
    public Pixmap(byte[] encodedData){
        this(encodedData, 0, encodedData.length);
    }

    /**
     * Creates a new Pixmap instance from the given encoded image data. The image can be encoded as JPEG, PNG or BMP.
     * <p>
     * 从给定的已编码图像数据创建新的 Pixmap 实例。图像可以是 JPEG、PNG 或 BMP 编码。
     * @param encodedData the image data to load, typically read from a PNG file 要加载的图像数据,通常从 PNG 文件读取
     */
    public Pixmap(byte[] encodedData, int offset, int len){
        load(encodedData, offset, len, null);
    }

    public Pixmap(String file){
        this(Core.files == null ? Fi.get(file) : Core.files.internal(file));
    }

    /**
     * Creates a new Pixmap instance from the given file. The file must be a Png, Jpeg or Bitmap.
     * <p>
     * 从给定文件创建新的 Pixmap 实例。文件必须是 PNG、JPEG 或 Bitmap。
     * @param file the {@link Fi} {@link Fi} 文件
     */
    public Pixmap(Fi file){
        byte[] bytes = file.readBytes();
        load(bytes, 0, bytes.length, file.toString());
    }

    /**
     * Creates a pixmap from a direct ByteBuffer.
     * 从直接 ByteBuffer 创建像素图。
     */
    public Pixmap(ByteBuffer buffer, int width, int height){
        if(!buffer.isDirect()) throw new ArcRuntimeException("Pixmaps may only use direct/native ByteBuffers!");

        this.width = width;
        this.height = height;
        this.pixels = buffer;
        this.handle = -1;

        buffer.position(0).limit(buffer.capacity());
    }

    /**
     * @return a newly allocated copy with the same pixels. 新分配的副本,包含相同的像素。
     */
    public Pixmap copy(){
        Pixmap out = new Pixmap(width, height);
        copyMem(pixels, 0, out.pixels, 0, pixels.capacity());
        return out;
    }

    /**
     * Iterates through every position in this Pixmap.
     * 遍历此 Pixmap 中的每个位置。
     */
    public void each(Intc2 cons){
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                cons.get(x, y);
            }
        }
    }

    public void replace(IntIntf func){
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                setRaw(x, y, func.get(getRaw(x, y)));
            }
        }
    }

    /**
     * Fills the complete bitmap with the specified color.
     * 用指定颜色填充整个位图。
     */
    public void fill(int color){
        int len = width * height * 4;
        for(int i = 0; i < len; i += 4){
            pixels.putInt(i, color);
        }
    }

    /**
     * Fills the complete bitmap with the specified color.
     * 用指定颜色填充整个位图。
     */
    public void fill(Color color){
        fill(color.rgba());
    }

    /**
     * @return whether this point is in the pixmap. 该点是否在像素图内。
     */
    public boolean in(int x, int y){
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public Pixmap crop(int x, int y, int width, int height){
        if(isDisposed()) throw new IllegalStateException("input is disposed.");
        Pixmap pixmap = new Pixmap(width, height);
        pixmap.draw(this, 0, 0, x, y, width, height);
        return pixmap;
    }

    /**
     * @return a newly allocated pixmap, flipped vertically. 新分配的像素图,已垂直翻转。
     */
    public Pixmap flipY(){
        Pixmap copy = new Pixmap(width, height);

        //TODO this can be optimized significantly by putting each line
        // TODO 通过逐行处理可以显著优化
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                copy.setRaw(x, height - 1 - y, getRaw(x, y));
            }
        }

        return copy;
    }

    /**
     * @return a newly allocated pixmap, flipped horizontally. 新分配的像素图,已水平翻转。
     */
    public Pixmap flipX(){
        Pixmap copy = new Pixmap(width, height);

        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                copy.set(width - 1 - x, y, getRaw(x, y));
            }
        }

        return copy;
    }

    /**
     * Draws a line between the given coordinates using the provided RGBA color.
     * 使用给定的 RGBA 颜色在给定坐标之间画一条线。
     */
    public void drawLine(int x1, int y1, int x2, int y2, int color){
        int x = x1, dx = Math.abs(x2 - x), sx = x < x2 ? 1 : -1;
        int y = y1, dy = Math.abs(y2 - y), sy = y < y2 ? 1 : -1;
        int e2, err = dx - dy;

        while(true){
            set(x, y, color);
            if(x == x2 && y == y2) return;

            e2 = 2 * err;
            if(e2 > -dy){
                err = err - dy;
                x = x + sx;
            }

            if(e2 < dx){
                err = err + dx;
                y = y + sy;
            }
        }
    }

    /**
     * Draws a rectangle outline starting at x, y extending by width to the right and by height downwards (y-axis points downwards) using the provided color.
     * 使用给定颜色从 x, y 开始向右延伸 width、向下延伸 height(y 轴向下)绘制矩形轮廓。
     */
    public void drawRect(int x, int y, int width, int height, int color){
        hline(x, x + width - 1, y, color);
        hline(x, x + width - 1, y + height - 1, color);
        vline(y, y + height - 1, x, color);
        vline(y, y + height - 1, x + width - 1, color);
    }

    void circlePoints(int cx, int cy, int x, int y, int col){
        if(x == 0){
            set(cx, cy + y, col);
            set(cx, cy - y, col);
            set(cx + y, cy, col);
            set(cx - y, cy, col);
        }else if(x == y){
            set(cx + x, cy + y, col);
            set(cx - x, cy + y, col);
            set(cx + x, cy - y, col);
            set(cx - x, cy - y, col);
        }else if(x < y){
            set(cx + x, cy + y, col);
            set(cx - x, cy + y, col);
            set(cx + x, cy - y, col);
            set(cx - x, cy - y, col);
            set(cx + y, cy + x, col);
            set(cx - y, cy + x, col);
            set(cx + y, cy - x, col);
            set(cx - y, cy - x, col);
        }
    }

    public void drawCircle(int x, int y, int radius, int col){
        int px = 0;
        int py = radius;
        int p = (5 - radius * 4) / 4;

        circlePoints(x, y, px, py, col);
        while(px < py){
            px++;
            if(p < 0){
                p += 2 * px + 1;
            }else{
                py--;
                p += 2 * (px - py) + 1;
            }
            circlePoints(x, y, px, py, col);
        }
    }

    public void draw(PixmapRegion region){
        draw(region.pixmap, region.x, region.y, region.width, region.height, 0, 0, region.width, region.height);
    }

    public void draw(PixmapRegion region, boolean blend){
        draw(region.pixmap, region.x, region.y, region.width, region.height, 0, 0, region.width, region.height, false, blend);
    }

    public void draw(PixmapRegion region, int x, int y){
        draw(region.pixmap, region.x, region.y, region.width, region.height, x, y, region.width, region.height);
    }

    public void draw(PixmapRegion region, int x, int y, int width, int height){
        draw(region.pixmap, region.x, region.y, region.width, region.height, x, y, width, height);
    }

    public void draw(PixmapRegion region, int x, int y, int srcx, int srcy, int srcWidth, int srcHeight){
        draw(region.pixmap, x, y, region.x + srcx, region.y + srcy, srcWidth, srcHeight);
    }

    public void draw(PixmapRegion region, int srcx, int srcy, int srcWidth, int srcHeight, int dstx, int dsty, int dstWidth, int dstHeight){
        draw(region.pixmap, region.x + srcx, region.y + srcy, srcWidth, srcHeight, dstx, dsty, dstWidth, dstHeight);
    }

    public void draw(Pixmap pixmap, boolean blend){
        draw(pixmap, 0, 0, pixmap.width, pixmap.height, 0, 0, pixmap.width, pixmap.height, false, blend);
    }

    public void draw(Pixmap pixmap){
        draw(pixmap, 0, 0);
    }

    /**
     * Draws an area from another Pixmap to this Pixmap.
     * <p>
     * 将另一个 Pixmap 的一块区域绘制到此 Pixmap。
     * @param pixmap The other Pixmap 另一个 Pixmap
     * @param x The target x-coordinate (top left corner) 目标 x 坐标(左上角)
     * @param y The target y-coordinate (top left corner) 目标 y 坐标(左上角)
     */
    public void draw(Pixmap pixmap, int x, int y){
        draw(pixmap, x, y, 0, 0, pixmap.width, pixmap.height);
    }

    /**
     * Draws an area from another Pixmap to this Pixmap.
     * <p>
     * 将另一个 Pixmap 的一块区域绘制到此 Pixmap。
     * @param pixmap The other Pixmap 另一个 Pixmap
     * @param x The target x-coordinate (top left corner) 目标 x 坐标(左上角)
     * @param y The target y-coordinate (top left corner) 目标 y 坐标(左上角)
     */
    public void draw(Pixmap pixmap, int x, int y, boolean blending){
        draw(pixmap, 0, 0, pixmap.width, pixmap.height, x, y, pixmap.width, pixmap.height, false, blending);
    }


    /**
     * Draws an area from another Pixmap to this Pixmap.
     * 将另一个 Pixmap 的一块区域绘制到此 Pixmap。
     */
    public void draw(Pixmap pixmap, int x, int y, int width, int height){
        draw(pixmap, x, y, width, height, false);
    }

    /**
     * Draws an area from another Pixmap to this Pixmap.
     * 将另一个 Pixmap 的一块区域绘制到此 Pixmap。
     */
    public void draw(Pixmap pixmap, int x, int y, int width, int height, boolean filter){
        draw(pixmap, 0, 0, pixmap.width, pixmap.height, x, y, width, height, filter);
    }

    /**
     * Draws an area from another Pixmap to this Pixmap.
     * 将另一个 Pixmap 的一块区域绘制到此 Pixmap。
     */
    public void draw(Pixmap pixmap, int x, int y, int srcx, int srcy, int srcWidth, int srcHeight){
        draw(pixmap, srcx, srcy, srcWidth, srcHeight, x, y, srcWidth, srcHeight);
    }

    /**
     * Draws an area from another Pixmap to this Pixmap. This will automatically scale and stretch the source image to the
     * specified target rectangle.
     * <p>
     * 将另一个 Pixmap 的一块区域绘制到此 Pixmap。会自动将源图像缩放拉伸到指定的目标矩形。
     */
    public void draw(Pixmap pixmap, int srcx, int srcy, int srcWidth, int srcHeight, int dstx, int dsty, int dstWidth, int dstHeight){
        draw(pixmap, srcx, srcy, srcWidth, srcHeight, dstx, dsty, dstWidth, dstHeight, false);
    }

    /**
     * Draws an area from another Pixmap to this Pixmap. This will automatically scale and stretch the source image to the
     * specified target rectangle.
     * <p>
     * 将另一个 Pixmap 的一块区域绘制到此 Pixmap。会自动将源图像缩放拉伸到指定的目标矩形。
     */
    public void draw(Pixmap pixmap, int srcx, int srcy, int srcWidth, int srcHeight, int dstx, int dsty, int dstWidth, int dstHeight, boolean filtering){
        draw(pixmap, srcx, srcy, srcWidth, srcHeight, dstx, dsty, dstWidth, dstHeight, filtering, false);
    }

    /**
     * Draws an area from another Pixmap to this Pixmap. This will automatically scale and stretch the source image to the
     * specified target rectangle. Blending is currently unsupported for stretched/scaled pixmaps.
     * <p>
     * 将另一个 Pixmap 的一块区域绘制到此 Pixmap。会自动将源图像缩放拉伸到指定的目标矩形。目前拉伸/缩放像素图不支持混合。
     * @param pixmap The other Pixmap 另一个 Pixmap
     * @param srcx The source x-coordinate (top left corner) 源 x 坐标(左上角)
     * @param srcy The source y-coordinate (top left corner); 源 y 坐标(左上角);
     * @param srcWidth The width of the area from the other Pixmap in pixels 另一个 Pixmap 中区域的宽度,以像素为单位
     * @param srcHeight The height of the area from the other Pixmap in pixels 另一个 Pixmap 中区域的高度,以像素为单位
     * @param dstx The target x-coordinate (top left corner) 目标 x 坐标(左上角)
     * @param dsty The target y-coordinate (top left corner) 目标 y 坐标(左上角)
     * @param dstWidth The target width 目标宽度
     * @param dstHeight the target height 目标高度
     */
    public void draw(Pixmap pixmap, int srcx, int srcy, int srcWidth, int srcHeight, int dstx, int dsty, int dstWidth, int dstHeight, boolean filtering, boolean blending){
        int width = this.width, height = this.height, owidth = pixmap.width, oheight = pixmap.height;

        //don't bother drawing invalid regions
        // 无效区域不做绘制
        if(srcWidth == 0 || srcHeight == 0 || dstWidth == 0 || dstHeight == 0){
            return;
        }

        if(srcWidth == dstWidth && srcHeight == dstHeight){

            //same-size blit, no filtering
            // 相同尺寸的块拷贝,无过滤
            int sx, dx;
            int sy = srcy, dy = dsty;

            if(blending){
                for(; sy < srcy + srcHeight; sy++, dy++){
                    if(sy < 0 || dy < 0) continue;
                    if(sy >= oheight || dy >= height) break;

                    for(sx = srcx, dx = dstx; sx < srcx + srcWidth; sx++, dx++){
                        if(sx < 0 || dx < 0) continue;
                        if(sx >= owidth || dx >= width) break;
                        setRaw(dx, dy, blend(pixmap.getRaw(sx, sy), getRaw(dx, dy)));
                    }
                }
            }else if(this.pixels != pixmap.pixels){ //make sure the buffers are different to prevent a crash
            // 确保两个缓冲区不同,以防止崩溃
                ByteBuffer pixels = this.pixels, otherPixels = pixmap.pixels;
                int
                startY = Math.max(dsty, 0),
                endY = Math.min(Math.min(dsty + Math.min(dstHeight, oheight), height), dsty - srcy + oheight),
                startX = Math.max(dstx, 0),
                endX = Math.min(Math.min(dstx + Math.min(dstWidth, owidth), width), dstx - srcx + owidth),
                offsetY = dsty - srcy,
                scanX = Math.max(Math.max(srcx, -dstx), 0),
                scanWidth = (endX - startX) * 4;

                while(startY < endY){
                    copyMem(
                        otherPixels,
                        ((startY - offsetY) * owidth + scanX) * 4,
                        pixels,
                        (startY * width + startX) * 4,
                        scanWidth
                    );
                    startY ++;
                }
            }else{ //drawing a pixmap onto itself is not a good idea, but it's better than crashing
            // 把像素图画到它自身不是好主意,但总比崩溃好
                for(; sy < srcy + srcHeight; sy++, dy++){
                    if(sy < 0 || dy < 0) continue;
                    if(sy >= oheight || dy >= height) break;

                    for(sx = srcx, dx = dstx; sx < srcx + srcWidth; sx++, dx++){
                        if(sx < 0 || dx < 0) continue;
                        if(sx >= owidth || dx >= width) break;
                        setRaw(dx, dy, pixmap.getRaw(sx, sy));
                    }
                }
            }
        }else{
            if(filtering){
                //blit with bilinear filtering
                // 使用双线性过滤进行块拷贝
                float x_ratio = ((float)srcWidth - 1) / dstWidth;
                float y_ratio = ((float)srcHeight - 1) / dstHeight;
                int rX = Math.max(Mathf.round(x_ratio), 1), rY = Math.max(Mathf.round(y_ratio), 1);
                float xdiff, ydiff;
                int spitch = 4 * owidth;
                int dx, dy, sx, sy, i = 0, j;
                ByteBuffer spixels = pixmap.pixels;

                for(; i < dstHeight; i++){
                    sy = (int)(i * y_ratio) + srcy;
                    dy = i + dsty;
                    ydiff = (y_ratio * i + srcy) - sy;
                    if(sy < 0 || dy < 0) continue;
                    if(sy >= oheight || dy >= height) break;

                    for(j = 0; j < dstWidth; j++){
                        sx = (int)(j * x_ratio) + srcx;
                        dx = j + dstx;
                        xdiff = (x_ratio * j + srcx) - sx;
                        if(sx < 0 || dx < 0) continue;
                        if(sx >= owidth || dx >= width) break;

                        int
                        srcp = (sx + sy * owidth) * 4,
                        c1 = spixels.getInt(srcp),
                        c2 = sx + rX < srcWidth ? spixels.getInt(srcp + 4 *rX) : c1,
                        c3 = sy + rY < srcHeight ? spixels.getInt(srcp + spitch * rY) : c1,
                        c4 = sx + rX < srcWidth && sy + rY < srcHeight ? spixels.getInt(srcp + 4 * rX + spitch * rY) : c1;

                        float ta = (1 - xdiff) * (1 - ydiff);
                        float tb = (xdiff) * (1 - ydiff);
                        float tc = (1 - xdiff) * (ydiff);
                        float td = (xdiff) * (ydiff);

                        int r = (int)(((c1 & 0xff000000) >>> 24) * ta + ((c2 & 0xff000000) >>> 24) * tb + ((c3 & 0xff000000) >>> 24) * tc + ((c4 & 0xff000000) >>> 24) * td) & 0xff;
                        int g = (int)(((c1 & 0xff0000) >>> 16) * ta + ((c2 & 0xff0000) >>> 16) * tb + ((c3 & 0xff0000) >>> 16) * tc + ((c4 & 0xff0000) >>> 16) * td) & 0xff;
                        int b = (int)(((c1 & 0xff00) >>> 8) * ta + ((c2 & 0xff00) >>> 8) * tb + ((c3 & 0xff00) >>> 8) * tc + ((c4 & 0xff00) >>> 8) * td) & 0xff;
                        int a = (int)((c1 & 0xff) * ta + (c2 & 0xff) * tb + (c3 & 0xff) * tc + (c4 & 0xff) * td) & 0xff;
                        int srccol = (r << 24) | (g << 16) | (b << 8) | a;

                        setRaw(dx, dy, !blending ? srccol : blend(srccol, getRaw(dx, dy)));
                    }
                }
            }else{
                //blit with nearest neighbor filtering
                // 使用最近邻过滤进行块拷贝
                int xratio = (srcWidth << 16) / dstWidth + 1;
                int yratio = (srcHeight << 16) / dstHeight + 1;
                int dx, dy, sx, sy;

                for(int i = 0; i < dstHeight; i++){
                    sy = ((i * yratio) >> 16) + srcy;
                    dy = i + dsty;
                    if(sy < 0 || dy < 0) continue;
                    if(sy >= oheight || dy >= height) break;

                    for(int j = 0; j < dstWidth; j++){
                        sx = ((j * xratio) >> 16) + srcx;
                        dx = j + dstx;
                        if(sx < 0 || dx < 0) continue;
                        if(sx >= owidth || dx >= width) break;

                        setRaw(dx, dy, !blending ? pixmap.getRaw(sx, sy) : blend(pixmap.getRaw(sx, sy), getRaw(dx, dy)));
                    }
                }
            }
        }
    }

    /**
     * Draws an area from another Pixmap to this Pixmap, resampled with a high quality filter that stays correct for large size reductions.
     * Unlike {@link #draw(Pixmap, int, int, int, int, int, int, int, int, boolean, boolean)}, this never takes a shortcut, not even for equal sizes.
     * <ul>
     * <li>When shrinking, every source pixel that overlaps a target pixel contributes, weighted by how much of it is covered (area averaging / box filter).
     * Nothing is skipped, so thin details fade out instead of aliasing or vanishing.</li>
     * <li>When enlarging, source pixels are bilinearly interpolated.</li>
     * </ul>
     * Colors are averaged premultiplied by alpha, so fully transparent pixels never darken or tint the edges of opaque ones.
     * Averaging is done directly on the stored color values, not in linear light.
     * Source pixels outside of this pixmap's bounds count as transparent; target pixels outside of this pixmap are not drawn.
     * <p>
     * 将另一个 Pixmap 的一块区域绘制到此 Pixmap,采用高质量重采样过滤,在大幅缩小时仍保持正确。与 {@link #draw(Pixmap, int, int, int, int, int, int, int, int, boolean, boolean)} 不同,此方法从不走捷径,即使尺寸相同也不例外。 <ul> <li>缩小时,每个与目标像素重叠的源像素都按覆盖比例参与计算(面积平均/盒式过滤)。不会跳过任何像素,因此细微细节会淡出,而不会走样或消失。</li> <li>放大时,对源像素进行双线性插值。</li> </ul> 颜色按 alpha 预乘后取平均,因此完全透明的像素绝不会使不透明像素的边缘变暗或染色。平均运算直接在存储的颜色值上进行,而非线性光空间。此像素图边界之外的源像素视为透明;此像素图之外的目标像素不会被绘制。
     * @param pixmap The other Pixmap 另一个 Pixmap
     * @param srcx The source x-coordinate (top left corner) 源 x 坐标(左上角)
     * @param srcy The source y-coordinate (top left corner) 源 y 坐标(左上角)
     * @param srcWidth The width of the area from the other Pixmap in pixels 另一个 Pixmap 中区域的宽度,以像素为单位
     * @param srcHeight The height of the area from the other Pixmap in pixels 另一个 Pixmap 中区域的高度,以像素为单位
     * @param dstx The target x-coordinate (top left corner) 目标 x 坐标(左上角)
     * @param dsty The target y-coordinate (top left corner) 目标 y 坐标(左上角)
     * @param dstWidth The target width 目标宽度
     * @param dstHeight The target height 目标高度
     * @param blending Whether to blend the result over the existing contents, instead of replacing them 是否将结果混合到现有内容上,而不是替换它们
     */
    public void drawScaled(Pixmap pixmap, int srcx, int srcy, int srcWidth, int srcHeight, int dstx, int dsty, int dstWidth, int dstHeight, boolean blending){
        if(srcWidth <= 0 || srcHeight <= 0 || dstWidth <= 0 || dstHeight <= 0) return;

        int owidth = pixmap.width, oheight = pixmap.height;

        int[] xStart = new int[dstWidth], yStart = new int[dstHeight];
        float[][] xWeights = resampleWeights(srcWidth, dstWidth, xStart), yWeights = resampleWeights(srcHeight, dstHeight, yStart);

        //horizontal pass: every source row is reduced to dstWidth columns of premultiplied rgba (0-255 scale)
        // 水平方向处理:每行源像素被缩减为 dstWidth 列预乘 RGBA(0-255 范围)
        float[] rows = new float[srcHeight * dstWidth * 4];

        for(int sy = 0; sy < srcHeight; sy++){
            int py = srcy + sy;
            if(py < 0 || py >= oheight) continue;

            for(int dx = 0; dx < dstWidth; dx++){
                float[] weights = xWeights[dx];
                float r = 0f, g = 0f, b = 0f, a = 0f;

                for(int i = 0; i < weights.length; i++){
                    int px = srcx + xStart[dx] + i;
                    if(px < 0 || px >= owidth) continue;

                    int color = pixmap.getRaw(px, py);
                    float w = weights[i], alpha = color & 0xff, pre = w * alpha / 255f;
                    r += ((color >>> 24) & 0xff) * pre;
                    g += ((color >>> 16) & 0xff) * pre;
                    b += ((color >>> 8) & 0xff) * pre;
                    a += w * alpha;
                }

                int index = (sy * dstWidth + dx) * 4;
                rows[index] = r;
                rows[index + 1] = g;
                rows[index + 2] = b;
                rows[index + 3] = a;
            }
        }

        //vertical pass, then un-premultiply and write
        // 垂直方向处理,然后去预乘并写入
        for(int dy = 0; dy < dstHeight; dy++){
            int ty = dsty + dy;
            if(ty < 0 || ty >= height) continue;

            float[] weights = yWeights[dy];

            for(int dx = 0; dx < dstWidth; dx++){
                int tx = dstx + dx;
                if(tx < 0 || tx >= width) continue;

                float r = 0f, g = 0f, b = 0f, a = 0f;
                for(int i = 0; i < weights.length; i++){
                    int index = ((yStart[dy] + i) * dstWidth + dx) * 4;
                    float w = weights[i];
                    r += rows[index] * w;
                    g += rows[index + 1] * w;
                    b += rows[index + 2] * w;
                    a += rows[index + 3] * w;
                }

                int result = 0;
                if(a > 0f){
                    float unpremultiply = 255f / a;
                    result = (scaledByte(r * unpremultiply) << 24) | (scaledByte(g * unpremultiply) << 16) | (scaledByte(b * unpremultiply) << 8) | scaledByte(a);
                }

                setRaw(tx, ty, blending ? blend(result, getRaw(tx, ty)) : result);
            }
        }
    }

    private static int scaledByte(float value){
        return Math.min((int)(value + 0.5f), 255);
    }

    /**
     * Computes how each of {@code dstSize} target pixels is built from a run of consecutive source pixels along one axis.
     * <p>
     * 计算沿某一轴的 {@code dstSize} 个目标像素各自如何由一段连续的源像素构成。
     * @param starts filled with the index of the first source pixel of each run 填入每段连续源像素中第一个像素的索引
     * @return the weights of every pixel in each run; they always sum to 1 每段中每个像素的权重;其总和始终为 1
     */
    private static float[][] resampleWeights(int srcSize, int dstSize, int[] starts){
        float[][] result = new float[dstSize][];
        double scale = (double)srcSize / dstSize;

        for(int d = 0; d < dstSize; d++){
            if(scale >= 1.0){
                //shrinking: overlap of the source span [lo, hi) covered by this target pixel with each source pixel
                // 缩小:该目标像素覆盖的源区间 [lo, hi) 与每个源像素的重叠部分
                double lo = d * scale, hi = Math.min((d + 1) * scale, srcSize);
                int first = Math.min((int)lo, srcSize - 1), last = Math.min((int)Math.ceil(hi) - 1, srcSize - 1);
                last = Math.max(last, first);

                float[] weights = new float[last - first + 1];
                for(int s = first; s <= last; s++){
                    weights[s - first] = (float)((Math.min(hi, s + 1) - Math.max(lo, s)) / scale);
                }

                starts[d] = first;
                result[d] = weights;
            }else{
                //enlarging: bilinear between the two source pixel centers surrounding this target pixel's center
                // 放大:在该目标像素中心两侧的两个源像素中心之间做双线性插值
                double center = (d + 0.5) * scale - 0.5;
                int i0 = (int)Math.floor(center);
                float f = (float)(center - i0);
                int a = Math.max(Math.min(i0, srcSize - 1), 0), b = Math.max(Math.min(i0 + 1, srcSize - 1), 0);

                starts[d] = a;
                result[d] = a == b ? new float[]{1f} : new float[]{1f - f, f};
            }
        }

        return result;
    }

    /**
     * Fills a rectangle starting at x, y extending by width to the right and by height downwards (y-axis points downwards) using
     * the current color.
     * <p>
     * 从 x, y 开始向右延伸 width、向下延伸 height(y 轴向下),使用当前颜色填充矩形。
     * @param x The x coordinate x 坐标
     * @param y The y coordinate y 坐标
     * @param width The width in pixels 宽度,以像素为单位
     * @param height The height in pixels 高度,以像素为单位
     */
    public void fillRect(int x, int y, int width, int height, int color){
        int x2 = x + width - 1;
        int y2 = y + height - 1;

        if(x >= this.width) return;
        if(y >= this.height) return;
        if(x2 < 0) return;
        if(y2 < 0) return;
        if(x < 0) x = 0;
        if(y < 0) y = 0;
        if(x2 >= this.width) x2 = this.width - 1;
        if(y2 >= this.height) y2 = this.height - 1;

        y2++;
        while(y != y2){
            hline(x, x2, y, color);
            y++;
        }
    }

    /**
     * Fills a circle with the center at x,y and a radius using the current color.
     * <p>
     * 以 x,y 为圆心、给定半径,使用当前颜色填充圆形。
     * @param radius The radius in pixels 半径,以像素为单位
     */
    public void fillCircle(int x, int y, int radius, int color){
        int f = 1 - radius;
        int ddF_x = 1;
        int ddF_y = -2 * radius;
        int px = 0;
        int py = radius;

        hline(x, x, y + radius, color);
        hline(x, x, y - radius, color);
        hline(x - radius, x + radius, y, color);

        while(px < py){
            if(f >= 0){
                py--;
                ddF_y += 2;
                f += ddF_y;
            }
            px++;
            ddF_x += 2;
            f += ddF_x;
            hline(x - px, x + px, y + py, color);
            hline(x - px, x + px, y - py, color);
            hline(x - py, x + py, y + px, color);
            hline(x - py, x + py, y - px, color);
        }
    }

    /**
     * @return The pixel color in RGBA8888 format, or 0 if out of bounds. RGBA8888 格式的像素颜色,越界时为 0。
     */
    public int get(int x, int y){
        return in(x, y) ? pixels.getInt((x + y * width) * 4) : 0;
    }

    /**
     * @return The pixel color in RGBA8888 format, clamped to image bounds. RGBA8888 格式的像素颜色,限制在图像边界内。
     */
    public int getClamp(int x, int y){
        return getRaw(Math.max(Math.min(x, width - 1), 0), Math.max(Math.min(y, height - 1), 0));
    }

    /**
     * @return The pixel color in RGBA8888 format. No bounds checks are done! RGBA8888 格式的像素颜色。不做边界检查!
     */
    public int getRaw(int x, int y){
        return pixels.getInt((x + y * width) * 4);
    }

    /**
     * @return The pixel alpha as a byte, 0-255. No bounds checks are done! 像素 alpha 值,字节类型,0-255。不做边界检查!
     */
    public int getA(int x, int y){
        return pixels.get((x + y * width) * 4 + 3) & 0xff;
    }

    /**
     * @return whether the alpha at a position is 0. No bounds checks are done. 某位置的 alpha 是否为 0。不做边界检查。
     */
    public boolean empty(int x, int y){
        return pixels.get((x + y * width) * 4 + 3) == 0;
    }

    /**
     * @return The width of the Pixmap in pixels. Pixmap 的宽度,以像素为单位。
     */
    public int getWidth(){
        return width;
    }

    /**
     * @return The height of the Pixmap in pixels. Pixmap 的高度,以像素为单位。
     */
    public int getHeight(){
        return height;
    }

    /**
     * Releases all resources associated with this Pixmap.
     * 释放此 Pixmap 关联的所有资源。
     */
    @Override
    public void dispose(){
        if(handle <= 0) return;
        free(handle);
        handle = 0;
    }

    @Override
    public boolean isDisposed(){
        return handle == 0;
    }

    public void set(int x, int y, Color color){
        set(x, y, color.rgba());
    }

    /**
     * Sets a pixel at the given location with the given color.
     * <p>
     * 用给定颜色设置给定位置的像素。
     * @param color the color in RGBA8888 format. RGBA8888 格式的颜色。
     */
    public void set(int x, int y, int color){
        if(in(x, y)){
            pixels.putInt((x + y * width) * 4, color);
        }
    }

    /**
     * Sets a pixel at the given location with the given color. No bounds checks are done!
     * <p>
     * 用给定颜色设置给定位置的像素。不做边界检查!
     * @param color the color in RGBA8888 format. RGBA8888 格式的颜色。
     */
    public void setRaw(int x, int y, int color){
        pixels.putInt((x + y * width) * 4, color);
    }

    /**
     * Returns the OpenGL ES format of this Pixmap. Used as the seventh parameter to
     * {@link Gl#texImage2D(int, int, int, int, int, int, int, int, java.nio.Buffer)}.
     * <p>
     * 返回此 Pixmap 的 OpenGL ES 格式。用作 {@link Gl#texImage2D(int, int, int, int, int, int, int, int, java.nio.Buffer)} 的第七个参数。
     * @return GL_RGBA
     */
    public int getGLFormat(){
        return Gl.rgba;
    }

    /**
     * Returns the OpenGL ES format of this Pixmap. Used as the third parameter to
     * {@link Gl#texImage2D(int, int, int, int, int, int, int, int, java.nio.Buffer)}.
     * <p>
     * 返回此 Pixmap 的 OpenGL ES 格式。用作 {@link Gl#texImage2D(int, int, int, int, int, int, int, int, java.nio.Buffer)} 的第三个参数。
     * @return GL_RGBA
     */
    public int getGLInternalFormat(){
        return Gl.rgba;
    }

    /**
     * Returns the OpenGL ES type of this Pixmap. Used as the eighth parameter to
     * {@link Gl#texImage2D(int, int, int, int, int, int, int, int, java.nio.Buffer)}.
     * <p>
     * 返回此 Pixmap 的 OpenGL ES 类型。用作 {@link Gl#texImage2D(int, int, int, int, int, int, int, int, java.nio.Buffer)} 的第八个参数。
     * @return Gl.unsignedByte
     */
    public int getGLType(){
        return Gl.unsignedByte;
    }

    /**
     * @return the direct {@link ByteBuffer} holding the pixel data. 保存像素数据的直接 {@link ByteBuffer}。
     */
    public ByteBuffer getPixels(){
        if(handle == 0) throw new ArcRuntimeException("Pixmap already disposed");
        return pixels;
    }

    void hline(int x1, int x2, int y, int color){
        if(y < 0 || y >= height) return;
        int tmp;

        if(x1 > x2){
            tmp = x1;
            x1 = x2;
            x2 = tmp;
        }

        if(x1 >= width) return;
        if(x2 < 0) return;

        if(x1 < 0) x1 = 0;
        if(x2 >= width) x2 = width - 1;
        x2++;

        while(x1 != x2){
            setRaw(x1++, y, color);
        }
    }

    void vline(int y1, int y2, int x, int color){
        if(x < 0 || x >= width) return;
        int tmp;

        if(y1 > y2){
            tmp = y1;
            y1 = y2;
            y2 = tmp;
        }

        if(y1 >= height) return;
        if(y2 < 0) return;

        if(y1 < 0) y1 = 0;
        if(y2 >= height) y2 = height - 1;
        y2++;

        while(y1 != y2){
            setRaw(x, y1++, color);
        }
    }

    /**
     * @return the blended result of the input colors 输入颜色混合后的结果
     */
    public static int blend(int src, int dst){
        int src_a = src & 0xff;
        if(src_a == 0) return dst;

        int dst_a = dst & 0xff;
        if(dst_a == 0) return src;
        int dst_b = (dst >>> 8) & 0xff;
        int dst_g = (dst >>> 16) & 0xff;
        int dst_r = (dst >>> 24) & 0xff;

        dst_a -= (dst_a * src_a) / 255;
        int a = dst_a + src_a;
        dst_r = (dst_r * dst_a + ((src >>> 24) & 0xff) * src_a) / a;
        dst_g = (dst_g * dst_a + ((src >>> 16) & 0xff) * src_a) / a;
        dst_b = (dst_b * dst_a + ((src >>> 8) & 0xff) * src_a) / a;
        return ((dst_r << 24) | (dst_g << 16) | (dst_b << 8) | a);
    }

    public static boolean empty(int i){
        return (i & 0x000000ff) == 0;
    }

    private void load(byte[] encodedData, int offset, int len, String file){
        //use native implementation when possible
        // 可能时使用原生实现
        if(ArcNativesLoader.loaded){
            try{
                //read with stb_image, which is slightly faster for large images and supports more formats
                // 使用 stb_image 读取,对大图略快且支持更多格式
                long[] nativeData = new long[3];
                pixels = loadJni(nativeData, encodedData, offset, len);
                if(pixels == null) throw new ArcRuntimeException("Error loading pixmap from image data: " + getFailureReason() + (file == null ? "" : " (" + file + ")"));

                handle = nativeData[0];
                width = (int)nativeData[1];
                height = (int)nativeData[2];
                pixels.position(0).limit(pixels.capacity());
            }catch(ArcRuntimeException e){
                //stb_image bug? some PNGs fail with "corrupt JPEG" as the error, try the Java implementation if so
                // stb_image 的 bug?某些 PNG 会报 "corrupt JPEG" 错误,此时改用 Java 实现
                if(e.getMessage() != null && e.getMessage().contains("Corrupt JPEG")){
                    try{
                        loadJava(encodedData, offset, len, file);
                        return;
                    }catch(Exception ignored){
                        //I did my best, fall through and throw the original exception
                        // 已尽力了,继续执行并抛出原始异常
                    }
                }
                throw e;
            }
        }else{
            loadJava(encodedData, offset, len, file);
        }
    }

    private void loadJava(byte[] encodedData, int offset, int len, String file){
        //read with the pure java implementation
        // 使用纯 Java 实现读取
        try{
            PngReader reader = new PngReader();
            pixels = reader.read(new ByteArrayInputStream(encodedData, offset, len));
            width = reader.width;
            height = reader.height;
            handle = -1;
            pixels.position(0).limit(pixels.capacity());
        }catch(Exception e){
            throw new ArcRuntimeException("Failed to load PNG data" + (file == null ? "" : " (" + file + ")"), e);
        }
    }

    private void load(int width, int height){
        //use native implementation when possible
        // 可能时使用原生实现
        if(ArcNativesLoader.loaded){
            long[] nativeData = new long[3];
            pixels = createJni(nativeData, width, height);
            if(pixels == null) throw new ArcRuntimeException("Error creating pixmap (out of memory?)");
            pixels.limit(pixels.capacity());

            this.handle = nativeData[0];
            this.width = (int)nativeData[1];
            this.height = (int)nativeData[2];
        }else{
            //use DirectByteBuffer instead
            // 改用 DirectByteBuffer
            this.handle = -1; //handle -1 means non-native buffer
            // handle 为 -1 表示非原生缓冲区
            this.width = width;
            this.height = height;
            this.pixels = ByteBuffer.allocateDirect(width * height * 4);
        }
    }

    @Override
    public String toString(){
        return "Pixmap:" + width + "x" + height;
    }

    static void copyMem(ByteBuffer src, int srcOffset, ByteBuffer dst, int dstOffset, int len){
        //Java 16 supports direct byte buffer transfer without modifying state. Older versions (+Android/iOS) don't, and likely never will
        // Java 16 支持直接字节缓冲区传输且不修改状态。旧版本(以及 Android/iOS)不支持,而且可能永远不会支持
        if(supportsBufferCopy){
            Java16Buffers.copy(src, srcOffset, dst, dstOffset, len);
        }else{
            if(!OS.isIos && !UnsafeBuffers.failed){
                UnsafeBuffers.copy(src, srcOffset, dst, dstOffset, len);
            }else{
                Buffers.copyJni(src, srcOffset, dst, dstOffset, len);
            }
        }
    }

    /*JNI

    #include <stdlib.h>
    #include <stdint.h>

    #include <stdlib.h>
    #define STB_IMAGE_IMPLEMENTATION
    #define STBI_FAILURE_USERMSG
    #define STBI_NO_STDIO
    #define STBI_MAX_DIMENSIONS 32768
    #define STBI_NO_BMP
    #define STBI_NO_PSD
    #define STBI_NO_TGA
    #define STBI_NO_GIF
    #define STBI_NO_HDR
    #define STBI_NO_PIC
    #define STBI_NO_PNM

    #ifdef __APPLE__
    #define STBI_NO_THREAD_LOCALS
    #endif
    #include "stb_image.h"

    */

    /**
     * Loads a pixmap from bytes and returns [address, width, height] in nativeData.
     * 从字节数组加载像素图,并在 nativeData 中返回 [地址, 宽度, 高度]。
     */
    static native ByteBuffer loadJni(long[] nativeData, byte[] buffer, int offset, int len); /*MANUAL
        const unsigned char* p_buffer = (const unsigned char*)env->GetPrimitiveArrayCritical(buffer, 0);

        int32_t width, height, format;

        //always use STBI_rgb_alpha (4) as the format, since that's the only thing pixmaps support
        //RGB images are generally uncommon and the memory savings don't really matter; formats have to be converted to RGBA for drawing anyway
        const unsigned char* pixels = stbi_load_from_memory(p_buffer + offset, len, &width, &height, &format, STBI_rgb_alpha);

        env->ReleasePrimitiveArrayCritical(buffer, (char*)p_buffer, 0);

        if(pixels == NULL) return NULL;

        jobject pixel_buffer = env->NewDirectByteBuffer((void*)pixels, width * height * 4);
        jlong* p_native_data = (jlong*)env->GetPrimitiveArrayCritical(nativeData, 0);
        p_native_data[0] = (jlong)pixels;
        p_native_data[1] = width;
        p_native_data[2] = height;
        env->ReleasePrimitiveArrayCritical(nativeData, p_native_data, 0);

        return pixel_buffer;
     */

    /**
     * Creates a new pixmap and returns [address, width, height] in nativeData.
     * 创建新的像素图,并在 nativeData 中返回 [地址, 宽度, 高度]。
     */
    static native ByteBuffer createJni(long[] nativeData, int width, int height); /*MANUAL
        const unsigned char* pixels = (unsigned char*)malloc(width * height * 4);

        if(!pixels) return 0;

        //fill pixel array with 0s
        //TODO use calloc insted?
        memset((void*)pixels, 0, width * height * 4);

        jobject pixel_buffer = env->NewDirectByteBuffer((void*)pixels, width * height * 4);
        jlong* p_native_data = (jlong*)env->GetPrimitiveArrayCritical(nativeData, 0);
        p_native_data[0] = (jlong)pixels;
        p_native_data[1] = width;
        p_native_data[2] = height;
        env->ReleasePrimitiveArrayCritical(nativeData, p_native_data, 0);

        return pixel_buffer;
     */

    static native void free(long buffer); /*
        free((void*)buffer);
     */

    static native String getFailureReason(); /*
        return env->NewStringUTF(stbi_failure_reason());
     */
}
