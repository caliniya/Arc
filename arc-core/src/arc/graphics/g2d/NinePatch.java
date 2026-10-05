package arc.graphics.g2d;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;

/**
 * A 3x3 grid of texture regions. Any of the regions may be omitted. Padding may be set as a hint on how to inset content on top
 * of the ninepatch (by default the eight "edge" textures of the nine-patch define the padding). When drawn the eight "edge"
 * patches will not be scaled, only the interior patch will be scaled.
 *
 * <p>
 * <b>NOTE</b>: This class expects a "post-processed" nine-patch, and not a raw ".9.png" texture. That is, the textures given to
 * this class should <em>not</em> include the meta-data pixels from a ".9.png" that describe the layout of the ninepatch over the
 * interior of the graphic. That information should be passed into the constructor either implicitly as the size of the individual
 * patch textures, or via the <code>left, right, top, bottom</code> parameters to {@link #NinePatch(Texture, int, int, int, int)}
 * or {@link #NinePatch(TextureRegion, int, int, int, int)}.
 *
 * <p>
 * A correctly created {@link TextureAtlas} is one way to generate a post-processed nine-patch from a ".9.png" file.
 * <p>
 * 3x3 的纹理区域网格。任何区域都可省略。可设置内边距作为在九宫格上方嵌入内容的提示(默认由九宫格的八个“边缘”纹理定义内边距)。绘制时八个“边缘”块不会缩放,只有内部块会缩放。 <p> <b>注意</b>:此类期望“后处理”过的九宫格,而非原始的 ".9.png" 纹理。也就是说,提供给此类的纹理 <em>不应</em> 包含 ".9.png" 中描述九宫格布局的元数据像素。该信息应隐式地以各块纹理的尺寸传入构造函数,或通过 {@link #NinePatch(Texture, int, int, int, int)} 或 {@link #NinePatch(TextureRegion, int, int, int, int)} 的 <code>left, right, top, bottom</code> 参数传入。 <p> 正确创建的 {@link TextureAtlas} 是从 ".9.png" 文件生成后处理九宫格的一种方式。
 */
public class NinePatch{
    public static final int TOP_LEFT = 0;
    public static final int TOP_CENTER = 1;
    public static final int TOP_RIGHT = 2;
    public static final int MIDDLE_LEFT = 3;
    public static final int MIDDLE_CENTER = 4;
    public static final int MIDDLE_RIGHT = 5;
    public static final int BOTTOM_LEFT = 6;
    /**
     * Indices for {@link #NinePatch(TextureRegion...)} constructor
     * {@link #NinePatch(TextureRegion...)} 构造函数的索引
     */
    // alphabetically first in javadoc
    // 按字母序排在 javadoc 最前
    public static final int BOTTOM_CENTER = 7;
    public static final int BOTTOM_RIGHT = 8;

    private static final Color tmpDrawColor = new Color();
    private final Color color = new Color(Color.white);
    /**
     * Bottom tint for a top-to-bottom gradient; equal to {@link #color} unless a gradient is set.
     * 自上而下渐变的底部色调;未设置渐变时等于 {@link #color}。
     */
    private final Color color2 = new Color(Color.white);
    private Texture texture;
    private int bottomLeft = -1, bottomCenter = -1, bottomRight = -1;
    private int middleLeft = -1, middleCenter = -1, middleRight = -1;
    private int topLeft = -1, topCenter = -1, topRight = -1;
    private float leftWidth, rightWidth, middleWidth, middleHeight, topHeight, bottomHeight;
    private float[] vertices = new float[9 * 4 * SpriteBatch.vertexSize];
    private int idx;
    private float padLeft = -1, padRight = -1, padTop = -1, padBottom = -1;

    /**
     * Create a ninepatch by cutting up the given texture into nine patches. The subsequent parameters define the 4 lines that
     * will cut the texture region into 9 pieces.
     * <p>
     * 将给定纹理切分为九块以创建九宫格。后续参数定义将纹理区域切成 9 块的 4 条线。
     * @param left Pixels from left edge. 距左边缘的像素数。
     * @param right Pixels from right edge. 距右边缘的像素数。
     * @param top Pixels from top edge. 距顶边缘的像素数。
     * @param bottom Pixels from bottom edge. 距底边缘的像素数。
     */
    public NinePatch(Texture texture, int left, int right, int top, int bottom){
        this(new TextureRegion(texture), left, right, top, bottom);
    }

    /**
     * Create a ninepatch by cutting up the given texture region into nine patches. The subsequent parameters define the 4 lines
     * that will cut the texture region into 9 pieces.
     * <p>
     * 将给定纹理区域切分为九块以创建九宫格。后续参数定义将纹理区域切成 9 块的 4 条线。
     * @param left Pixels from left edge. 距左边缘的像素数。
     * @param right Pixels from right edge. 距右边缘的像素数。
     * @param top Pixels from top edge. 距顶边缘的像素数。
     * @param bottom Pixels from bottom edge. 距底边缘的像素数。
     */
    public NinePatch(TextureRegion region, int left, int right, int top, int bottom){
        if(region == null) throw new IllegalArgumentException("region cannot be null.");
        final int middleWidth = region.width - left - right;
        final int middleHeight = region.height - top - bottom;

        TextureRegion[] patches = new TextureRegion[9];
        if(top > 0){
            if(left > 0) patches[TOP_LEFT] = new TextureRegion(region, 0, 0, left, top);
            if(middleWidth > 0) patches[TOP_CENTER] = new TextureRegion(region, left, 0, middleWidth, top);
            if(right > 0) patches[TOP_RIGHT] = new TextureRegion(region, left + middleWidth, 0, right, top);
        }
        if(middleHeight > 0){
            if(left > 0) patches[MIDDLE_LEFT] = new TextureRegion(region, 0, top, left, middleHeight);
            if(middleWidth > 0)
                patches[MIDDLE_CENTER] = new TextureRegion(region, left, top, middleWidth, middleHeight);
            if(right > 0)
                patches[MIDDLE_RIGHT] = new TextureRegion(region, left + middleWidth, top, right, middleHeight);
        }
        if(bottom > 0){
            if(left > 0) patches[BOTTOM_LEFT] = new TextureRegion(region, 0, top + middleHeight, left, bottom);
            if(middleWidth > 0)
                patches[BOTTOM_CENTER] = new TextureRegion(region, left, top + middleHeight, middleWidth, bottom);
            if(right > 0)
                patches[BOTTOM_RIGHT] = new TextureRegion(region, left + middleWidth, top + middleHeight, right, bottom);
        }

        // If split only vertical, move splits from right to center.
        // 若仅垂直分割,则将右分割值移到中心。
        if(left == 0 && middleWidth == 0){
            patches[TOP_CENTER] = patches[TOP_RIGHT];
            patches[MIDDLE_CENTER] = patches[MIDDLE_RIGHT];
            patches[BOTTOM_CENTER] = patches[BOTTOM_RIGHT];
            patches[TOP_RIGHT] = null;
            patches[MIDDLE_RIGHT] = null;
            patches[BOTTOM_RIGHT] = null;
        }
        // If split only horizontal, move splits from bottom to center.
        // 若仅水平分割,则将下分割值移到中心。
        if(top == 0 && middleHeight == 0){
            patches[MIDDLE_LEFT] = patches[BOTTOM_LEFT];
            patches[MIDDLE_CENTER] = patches[BOTTOM_CENTER];
            patches[MIDDLE_RIGHT] = patches[BOTTOM_RIGHT];
            patches[BOTTOM_LEFT] = null;
            patches[BOTTOM_CENTER] = null;
            patches[BOTTOM_RIGHT] = null;
        }

        load(patches);
    }

    /**
     * Construct a degenerate "nine" patch with only a center component.
     * 构造只含中心部分的退化“九”宫格。
     */
    public NinePatch(Texture texture, Color color){
        this(texture);
        setColor(color);
    }

    /**
     * Construct a degenerate "nine" patch with only a center component.
     * 构造只含中心部分的退化“九”宫格。
     */
    public NinePatch(Texture texture){
        this(new TextureRegion(texture));
    }

    /**
     * Construct a degenerate "nine" patch with only a center component.
     * 构造只含中心部分的退化“九”宫格。
     */
    public NinePatch(TextureRegion region, Color color){
        this(region);
        setColor(color);
    }

    /**
     * Construct a degenerate "nine" patch with only a center component.
     * 构造只含中心部分的退化“九”宫格。
     */
    public NinePatch(TextureRegion region){
        load(new TextureRegion[]{
        //
        null, null, null, //
        null, region, null, //
        null, null, null //
        });
    }

    /**
     * Construct a nine patch from the given nine texture regions. The provided patches must be consistently sized (e.g., any left
     * edge textures must have the same width, etc). Patches may be <code>null</code>. Patch indices are specified via the public
     * members {@link #TOP_LEFT}, {@link #TOP_CENTER}, etc.
     * <p>
     * 由给定的九个纹理区域构造九宫格。提供的各块必须尺寸一致(例如,所有左边缘纹理宽度必须相同)。各块可为 <code>null</code>。块索引通过公共成员 {@link #TOP_LEFT}、{@link #TOP_CENTER} 等指定。
     */
    public NinePatch(TextureRegion... patches){
        if(patches == null || patches.length != 9)
            throw new IllegalArgumentException("NinePatch needs nine TextureRegions");

        load(patches);

        float leftWidth = getLeftWidth();
        if((patches[TOP_LEFT] != null && patches[TOP_LEFT].width != leftWidth)
        || (patches[MIDDLE_LEFT] != null && patches[MIDDLE_LEFT].width != leftWidth)
        || (patches[BOTTOM_LEFT] != null && patches[BOTTOM_LEFT].width != leftWidth)){
            throw new ArcRuntimeException("Left side patches must have the same width");
        }

        float rightWidth = getRightWidth();
        if((patches[TOP_RIGHT] != null && patches[TOP_RIGHT].width != rightWidth)
        || (patches[MIDDLE_RIGHT] != null && patches[MIDDLE_RIGHT].width != rightWidth)
        || (patches[BOTTOM_RIGHT] != null && patches[BOTTOM_RIGHT].width != rightWidth)){
            throw new ArcRuntimeException("Right side patches must have the same width");
        }

        float bottomHeight = getBottomHeight();
        if((patches[BOTTOM_LEFT] != null && patches[BOTTOM_LEFT].height != bottomHeight)
        || (patches[BOTTOM_CENTER] != null && patches[BOTTOM_CENTER].height != bottomHeight)
        || (patches[BOTTOM_RIGHT] != null && patches[BOTTOM_RIGHT].height != bottomHeight)){
            throw new ArcRuntimeException("Bottom side patches must have the same height");
        }

        float topHeight = getTopHeight();
        if((patches[TOP_LEFT] != null && patches[TOP_LEFT].height != topHeight)
        || (patches[TOP_CENTER] != null && patches[TOP_CENTER].height != topHeight)
        || (patches[TOP_RIGHT] != null && patches[TOP_RIGHT].height != topHeight)){
            throw new ArcRuntimeException("Top side patches must have the same height");
        }
    }

    public NinePatch(NinePatch ninePatch){
        this(ninePatch, ninePatch.color, ninePatch.color2);
    }

    public NinePatch(NinePatch ninePatch, Color color){
        this(ninePatch, color, color);
    }

    /**
     * Copies {@code ninePatch}, gradient-tinted from {@code top} to {@code bottom}.
     * 复制 {@code ninePatch},并施加从 {@code top} 到 {@code bottom} 的渐变着色。
     */
    public NinePatch(NinePatch ninePatch, Color top, Color bottom){
        texture = ninePatch.texture;

        bottomLeft = ninePatch.bottomLeft;
        bottomCenter = ninePatch.bottomCenter;
        bottomRight = ninePatch.bottomRight;
        middleLeft = ninePatch.middleLeft;
        middleCenter = ninePatch.middleCenter;
        middleRight = ninePatch.middleRight;
        topLeft = ninePatch.topLeft;
        topCenter = ninePatch.topCenter;
        topRight = ninePatch.topRight;

        leftWidth = ninePatch.leftWidth;
        rightWidth = ninePatch.rightWidth;
        middleWidth = ninePatch.middleWidth;
        middleHeight = ninePatch.middleHeight;
        topHeight = ninePatch.topHeight;
        bottomHeight = ninePatch.bottomHeight;

        padLeft = ninePatch.padLeft;
        padTop = ninePatch.padTop;
        padBottom = ninePatch.padBottom;
        padRight = ninePatch.padRight;

        vertices = new float[ninePatch.vertices.length];
        System.arraycopy(ninePatch.vertices, 0, vertices, 0, ninePatch.vertices.length);
        idx = ninePatch.idx;
        this.color.set(top);
        this.color2.set(bottom);
    }

    private void load(TextureRegion[] patches){
        final float color = Color.whiteFloatBits; // placeholder color, overwritten at draw time
        // 占位颜色,绘制时会被覆盖

        if(patches[BOTTOM_LEFT] != null){
            bottomLeft = add(patches[BOTTOM_LEFT], color, false, false);
            leftWidth = patches[BOTTOM_LEFT].width;
            bottomHeight = patches[BOTTOM_LEFT].height;
        }
        if(patches[BOTTOM_CENTER] != null){
            bottomCenter = add(patches[BOTTOM_CENTER], color, true, false);
            middleWidth = Math.max(middleWidth, patches[BOTTOM_CENTER].width);
            bottomHeight = Math.max(bottomHeight, patches[BOTTOM_CENTER].height);
        }
        if(patches[BOTTOM_RIGHT] != null){
            bottomRight = add(patches[BOTTOM_RIGHT], color, false, false);
            rightWidth = Math.max(rightWidth, patches[BOTTOM_RIGHT].width);
            bottomHeight = Math.max(bottomHeight, patches[BOTTOM_RIGHT].height);
        }
        if(patches[MIDDLE_LEFT] != null){
            middleLeft = add(patches[MIDDLE_LEFT], color, false, true);
            leftWidth = Math.max(leftWidth, patches[MIDDLE_LEFT].width);
            middleHeight = Math.max(middleHeight, patches[MIDDLE_LEFT].height);
        }
        if(patches[MIDDLE_CENTER] != null){
            middleCenter = add(patches[MIDDLE_CENTER], color, true, true);
            middleWidth = Math.max(middleWidth, patches[MIDDLE_CENTER].width);
            middleHeight = Math.max(middleHeight, patches[MIDDLE_CENTER].height);
        }
        if(patches[MIDDLE_RIGHT] != null){
            middleRight = add(patches[MIDDLE_RIGHT], color, false, true);
            rightWidth = Math.max(rightWidth, patches[MIDDLE_RIGHT].width);
            middleHeight = Math.max(middleHeight, patches[MIDDLE_RIGHT].height);
        }
        if(patches[TOP_LEFT] != null){
            topLeft = add(patches[TOP_LEFT], color, false, false);
            leftWidth = Math.max(leftWidth, patches[TOP_LEFT].width);
            topHeight = Math.max(topHeight, patches[TOP_LEFT].height);
        }
        if(patches[TOP_CENTER] != null){
            topCenter = add(patches[TOP_CENTER], color, true, false);
            middleWidth = Math.max(middleWidth, patches[TOP_CENTER].width);
            topHeight = Math.max(topHeight, patches[TOP_CENTER].height);
        }
        if(patches[TOP_RIGHT] != null){
            topRight = add(patches[TOP_RIGHT], color, false, false);
            rightWidth = Math.max(rightWidth, patches[TOP_RIGHT].width);
            topHeight = Math.max(topHeight, patches[TOP_RIGHT].height);
        }
        if(idx < vertices.length){
            float[] newVertices = new float[idx];
            System.arraycopy(vertices, 0, newVertices, 0, idx);
            vertices = newVertices;
        }
    }

    private int add(TextureRegion region, float color, boolean isStretchW, boolean isStretchH){
        if(texture == null)
            texture = region.texture;
        else if(texture.getHandle() != region.texture.getHandle()) //
            throw new IllegalArgumentException("All regions must be from the same texture.");

        float u = region.u;
        float v = region.v2;
        float u2 = region.u2;
        float v2 = region.v;
        float depth = region.getDepth();

        // Add half pixel offsets on stretchable dimensions to avoid color bleeding when GL_LINEAR
        // 在可拉伸维度上添加半像素偏移,以避免使用 GL_LINEAR
        // filtering is used for the texture. This nudges the texture coordinate to the center
        // 过滤纹理时出现颜色渗色。这将纹理坐标微调到纹素中心,
        // of the texel where the neighboring pixel has 0% contribution in Linear blending mode.
        // 在该模式下相邻像素的贡献为 0%。
        //NOTE: This is now unconditional. If you are using this for pixel art, you will have a bad time, but Mindustry isn't, so good luck.
        // 注意:此行为现在是无条件的。如果你将其用于像素画,那会有麻烦,但 Mindustry 并非如此,祝你好运。
        if(isStretchW){
            float halfTexelWidth = 0.5f / texture.width;
            u += halfTexelWidth;
            u2 -= halfTexelWidth;
        }
        if(isStretchH){
            float halfTexelHeight = 0.5f / texture.height;
            v -= halfTexelHeight;
            v2 += halfTexelHeight;
        }

        final float[] vertices = this.vertices;

        vertices[idx + 2]  = u;
        vertices[idx + 3]  = v;
        vertices[idx + 4]  = depth;
        vertices[idx + 5]  = color;

        vertices[idx + 9]  = u;
        vertices[idx + 10] = v2;
        vertices[idx + 11] = depth;
        vertices[idx + 12] = color;

        vertices[idx + 16] = u2;
        vertices[idx + 17] = v2;
        vertices[idx + 18] = depth;
        vertices[idx + 19] = color;

        vertices[idx + 23] = u2;
        vertices[idx + 24] = v;
        vertices[idx + 25] = depth;
        vertices[idx + 26] = color;
        idx += SpriteBatch.spriteSize;

        return idx - SpriteBatch.spriteSize;
    }

    /**
     * Set the coordinates of a ninth of the patch, with separate bottom/top edge vertex colors.
     * 设置九宫格中一块的坐标,底/顶边缘顶点颜色可分别指定。
     */
    private void set(int idx, float x, float y, float width, float height, float colorBottom, float colorTop){
        final float fx2 = x + width;
        final float fy2 = y + height;
        final float[] vertices = this.vertices;
        vertices[idx]      = x;
        vertices[idx + 1]  = y;
        vertices[idx + 5]  = colorBottom;

        vertices[idx + 7]  = x;
        vertices[idx + 8]  = fy2;
        vertices[idx + 12] = colorTop;

        vertices[idx + 14] = fx2;
        vertices[idx + 15] = fy2;
        vertices[idx + 19] = colorTop;

        vertices[idx + 21] = fx2;
        vertices[idx + 22] = y;
        vertices[idx + 26] = colorBottom;
    }

    /**
     * @return the vertex color at absolute height {@code rowY}, lerped from {@link #color2} to {@link #color}.
     * @return the vertex color at absolute height {@code rowY}, lerped from {@link #color2} to {@link #color}. 绝对高度 {@code rowY} 处的顶点颜色,由 {@link #color2} 到 {@link #color} 插值。
     */
    private float rowColor(float rowY, float baseY, float totalHeight){
        float t = totalHeight > 0.0001f ? (rowY - baseY) / totalHeight : 1f;
        return tmpDrawColor.set(color2).lerp(color, t).mul(Draw.getColor()).toFloatBits();
    }

    private void prepareVertices(float x, float y, float width, float height){
        final float centerColumnX = x + leftWidth;
        final float rightColumnX = x + width - rightWidth;
        final float middleRowY = y + bottomHeight;
        final float topRowY = y + height - topHeight;

        final float cBottom = rowColor(y, y, height);
        final float cMiddleBottom = rowColor(middleRowY, y, height);
        final float cMiddleTop = rowColor(topRowY, y, height);
        final float cTop = rowColor(y + height, y, height);

        if(bottomLeft != -1) set(bottomLeft, x, y, centerColumnX - x, middleRowY - y, cBottom, cMiddleBottom);
        if(bottomCenter != -1) set(bottomCenter, centerColumnX, y, rightColumnX - centerColumnX, middleRowY - y, cBottom, cMiddleBottom);
        if(bottomRight != -1) set(bottomRight, rightColumnX, y, x + width - rightColumnX, middleRowY - y, cBottom, cMiddleBottom);
        if(middleLeft != -1) set(middleLeft, x, middleRowY, centerColumnX - x, topRowY - middleRowY, cMiddleBottom, cMiddleTop);
        if(middleCenter != -1) set(middleCenter, centerColumnX, middleRowY, rightColumnX - centerColumnX, topRowY - middleRowY, cMiddleBottom, cMiddleTop);
        if(middleRight != -1) set(middleRight, rightColumnX, middleRowY, x + width - rightColumnX, topRowY - middleRowY, cMiddleBottom, cMiddleTop);
        if(topLeft != -1) set(topLeft, x, topRowY, centerColumnX - x, y + height - topRowY, cMiddleTop, cTop);
        if(topCenter != -1) set(topCenter, centerColumnX, topRowY, rightColumnX - centerColumnX, y + height - topRowY, cMiddleTop, cTop);
        if(topRight != -1) set(topRight, rightColumnX, topRowY, x + width - rightColumnX, y + height - topRowY, cMiddleTop, cTop);
    }

    public void draw(float x, float y, float width, float height){
        prepareVertices(x, y, width, height);
        Draw.vert(texture, vertices, 0, idx);
    }

    public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX,
                     float scaleY, float rotation){
        prepareVertices(x, y, width, height);
        float worldOriginX = x + originX, worldOriginY = y + originY;
        int n = this.idx;
        float[] vertices = this.vertices;
        if(rotation != 0){
            for(int i = 0; i < n; i += SpriteBatch.vertexSize){
                float vx = (vertices[i] - worldOriginX) * scaleX, vy = (vertices[i + 1] - worldOriginY) * scaleY;
                float cos = Mathf.cosDeg(rotation), sin = Mathf.sinDeg(rotation);
                vertices[i] = cos * vx - sin * vy + worldOriginX;
                vertices[i + 1] = sin * vx + cos * vy + worldOriginY;
            }
        }else if(scaleX != 1 || scaleY != 1){
            for(int i = 0; i < n; i += SpriteBatch.vertexSize){
                vertices[i] = (vertices[i] - worldOriginX) * scaleX + worldOriginX;
                vertices[i + 1] = (vertices[i + 1] - worldOriginY) * scaleY + worldOriginY;
            }
        }
        Draw.vert(texture, vertices, 0, n);
    }

    public Color getColor(){
        return color;
    }

    /**
     * @return the bottom tint of a top-to-bottom gradient, same as {@link #getColor()} if none is set.
     * @return the bottom tint of a top-to-bottom gradient, same as {@link #getColor()} if none is set. 自上而下渐变的底部色调,未设置时与 {@link #getColor()} 相同。
     */
    public Color getColor2(){
        return color2;
    }

    /**
     * Copy given color. The color will be blended with the batch color, then combined with the texture colors at draw time.
     * Default is {@link Color#white}.
     * <p>
     * 复制给定颜色。该颜色会与批处理颜色混合,并在绘制时与纹理颜色合成。默认为 {@link Color#white}。
     */
    public void setColor(Color color){
        this.color.set(color);
        this.color2.set(color);
    }

    /**
     * Sets a top-to-bottom gradient tint from {@code top} to {@code bottom}.
     * 设置从 {@code top} 到 {@code bottom} 的自上而下渐变着色。
     */
    public void setColor(Color top, Color bottom){
        this.color.set(top);
        this.color2.set(bottom);
    }

    public float getLeftWidth(){
        return leftWidth;
    }

    /**
     * Set the draw-time width of the three left edge patches
     * 设置左侧三块绘制时的宽度
     */
    public void setLeftWidth(float leftWidth){
        this.leftWidth = leftWidth;
    }

    public float getRightWidth(){
        return rightWidth;
    }

    /**
     * Set the draw-time width of the three right edge patches
     * 设置右侧三块绘制时的宽度
     */
    public void setRightWidth(float rightWidth){
        this.rightWidth = rightWidth;
    }

    public float getTopHeight(){
        return topHeight;
    }

    /**
     * Set the draw-time height of the three top edge patches
     * 设置顶部三块绘制时的高度
     */
    public void setTopHeight(float topHeight){
        this.topHeight = topHeight;
    }

    public float getBottomHeight(){
        return bottomHeight;
    }

    /**
     * Set the draw-time height of the three bottom edge patches
     * 设置底部三块绘制时的高度
     */
    public void setBottomHeight(float bottomHeight){
        this.bottomHeight = bottomHeight;
    }

    public float getMiddleWidth(){
        return middleWidth;
    }

    /**
     * Set the width of the middle column of the patch. At render time, this is implicitly the requested render-width of the
     * entire nine patch, minus the left and right width. This value is only used for computing the {@link #getTotalWidth() default
     * total width}.
     * <p>
     * 设置九宫格中间列的宽度。渲染时,它隐式等于整个九宫格请求的渲染宽度减去左右宽度。此值仅用于计算 {@link #getTotalWidth() 默认总宽度}。
     */
    public void setMiddleWidth(float middleWidth){
        this.middleWidth = middleWidth;
    }

    public float getMiddleHeight(){
        return middleHeight;
    }

    /**
     * Set the height of the middle row of the patch. At render time, this is implicitly the requested render-height of the entire
     * nine patch, minus the top and bottom height. This value is only used for computing the {@link #getTotalHeight() default
     * total height}.
     * <p>
     * 设置九宫格中间行的高度。渲染时,它隐式等于整个九宫格请求的渲染高度减去上下高度。此值仅用于计算 {@link #getTotalHeight() 默认总高度}。
     */
    public void setMiddleHeight(float middleHeight){
        this.middleHeight = middleHeight;
    }

    public float getTotalWidth(){
        return leftWidth + middleWidth + rightWidth;
    }

    public float getTotalHeight(){
        return topHeight + middleHeight + bottomHeight;
    }

    /**
     * Set the padding for content inside this ninepatch. By default the padding is set to match the exterior of the ninepatch, so
     * the content should fit exactly within the middle patch.
     * <p>
     * 设置此九宫格内部内容的内边距。默认内边距与九宫格外围匹配,因此内容应恰好适配中间块。
     */
    public void setPadding(float left, float right, float top, float bottom){
        this.padLeft = left;
        this.padRight = right;
        this.padTop = top;
        this.padBottom = bottom;
    }

    /**
     * Returns the left padding if set, else returns {@link #getLeftWidth()}.
     * 返回左内边距(若已设置),否则返回 {@link #getLeftWidth()}。
     */
    public float getPadLeft(){
        if(padLeft == -1) return getLeftWidth();
        return padLeft;
    }

    /**
     * See {@link #setPadding(float, float, float, float)}
     * 见 {@link #setPadding(float, float, float, float)}
     */
    public void setPadLeft(float left){
        this.padLeft = left;
    }

    /**
     * Returns the right padding if set, else returns {@link #getRightWidth()}.
     * 返回右内边距(若已设置),否则返回 {@link #getRightWidth()}。
     */
    public float getPadRight(){
        if(padRight == -1) return getRightWidth();
        return padRight;
    }

    /**
     * See {@link #setPadding(float, float, float, float)}
     * 见 {@link #setPadding(float, float, float, float)}
     */
    public void setPadRight(float right){
        this.padRight = right;
    }

    /**
     * Returns the top padding if set, else returns {@link #getTopHeight()}.
     * 返回顶部内边距(若已设置),否则返回 {@link #getTopHeight()}。
     */
    public float getPadTop(){
        if(padTop == -1) return getTopHeight();
        return padTop;
    }

    /**
     * See {@link #setPadding(float, float, float, float)}
     * 见 {@link #setPadding(float, float, float, float)}
     */
    public void setPadTop(float top){
        this.padTop = top;
    }

    /**
     * Returns the bottom padding if set, else returns {@link #getBottomHeight()}.
     * 返回底部内边距(若已设置),否则返回 {@link #getBottomHeight()}。
     */
    public float getPadBottom(){
        if(padBottom == -1) return getBottomHeight();
        return padBottom;
    }

    /**
     * See {@link #setPadding(float, float, float, float)}
     * 见 {@link #setPadding(float, float, float, float)}
     */
    public void setPadBottom(float bottom){
        this.padBottom = bottom;
    }

    /**
     * Multiplies the top/left/bottom/right sizes and padding by the specified amount.
     * 将上/左/下/右尺寸和内边距乘以指定量。
     */
    public void scale(float scaleX, float scaleY){
        leftWidth *= scaleX;
        rightWidth *= scaleX;
        topHeight *= scaleY;
        bottomHeight *= scaleY;
        middleWidth *= scaleX;
        middleHeight *= scaleY;
        if(padLeft != -1) padLeft *= scaleX;
        if(padRight != -1) padRight *= scaleX;
        if(padTop != -1) padTop *= scaleY;
        if(padBottom != -1) padBottom *= scaleY;
    }

    public Texture getTexture(){
        return texture;
    }
}
