package arc.graphics.font;

import arc.graphics.*;
import arc.graphics.font.Font.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.GlyphLayout.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;

import java.util.Arrays;

/**
 * Caches glyph geometry for a BitmapFont, providing a fast way to render static text. This saves needing to compute the glyph
 * geometry each frame.
 * <p>
 * 为 BitmapFont 缓存字形几何数据,提供渲染静态文本的快速途径,免去每帧计算字形几何的开销。
 * @author Nathan Sweet
 * @author davebaol
 * @author Alexander Dorokhov
 */
public class FontCache{
    private static final Color tempColor = new Color(1, 1, 1, 1);

    private final Font font;
    private final Ar<GlyphLayout> layouts = new Ar<>();
    private final Ar<GlyphLayout> pooledLayouts = new Ar<>();
    private final Color color = new Color(1, 1, 1, 1);
    private boolean integer;
    private int glyphCount;
    private float x, y;
    private float currentTint;

    /**
     * Texture for each page slot; slots are keyed by glyph texture so fallback glyphs from other fonts can be drawn.
     * 每个页面槽位的纹理;槽位以字形纹理为键,因此可以绘制来自其他字体的回退字形。
     */
    private final Ar<Texture> pageTextures = new Ar<>();
    /**
     * Vertex data per page.
     * 每页的顶点数据。
     */
    private float[][] pageVertices;
    /**
     * Number of vertex data entries per page.
     * 每页的顶点数据条目数。
     */
    private int[] idx;
    /**
     * For each page, an array with a value for each glyph from that page, where the value is the index of the character in the
     * full text being cached.
     * <p>
     * 每页一个数组,该页的每个字形对应一个值,即所缓存全文中字符的索引。
     */
    private IntAr[] pageGlyphIndices;
    /**
     * Used internally to ensure a correct capacity for multi-page font vertex data.
     * 内部使用,确保多页字体顶点数据的容量正确。
     */
    private int[] tempGlyphCount;

    public FontCache(Font font){
        this(font, font.usesIntegerPositions());
    }

    /**
     * @param integer If true, rendering positions will be at integer values to avoid filtering artifacts.
     * @param integer If true, rendering positions will be at integer values to avoid filtering artifacts. 若为 true,渲染位置将取整数值,以避免过滤瑕疵。
     */
    public FontCache(Font font, boolean integer){
        this.font = font;
        this.integer = integer;

        if(font.regions.size == 0)
            throw new IllegalArgumentException("The specified font must contain at least one texture page.");

        for(TextureRegion region : font.regions){
            if(!pageTextures.contains(region.texture, true)) pageTextures.add(region.texture);
        }

        int pageCount = pageTextures.size;
        pageVertices = new float[pageCount][];
        idx = new int[pageCount];
        if(pageCount > 1){
            // Contains the indices of the glyph in the cache as they are added.
            // 按添加顺序保存缓存中字形的索引。
            pageGlyphIndices = new IntAr[pageCount];
            for(int i = 0; i < pageCount; i++)
                pageGlyphIndices[i] = new IntAr();
        }
        tempGlyphCount = new int[pageCount];
    }

    /**
     * @return the page slot for the glyph's texture, adding one if this texture hasn't been seen yet.
     * @return the page slot for the glyph's texture, adding one if this texture hasn't been seen yet. 该字形纹理对应的页面槽位,若此纹理尚未出现则新增一个。
     */
    private int pageOf(Glyph glyph){
        Texture[] textures = pageTextures.items;
        for(int i = 0, n = pageTextures.size; i < n; i++){
            if(textures[i] == glyph.texture) return i;
        }
        return addPage(glyph.texture);
    }

    private int addPage(Texture texture){
        int page = pageTextures.size, count = page + 1;
        pageTextures.add(texture);

        pageVertices = Arrays.copyOf(pageVertices, count);
        idx = Arrays.copyOf(idx, count);
        tempGlyphCount = Arrays.copyOf(tempGlyphCount, count);

        if(count > 1){
            IntAr[] indices = new IntAr[count];
            if(pageGlyphIndices != null){
                System.arraycopy(pageGlyphIndices, 0, indices, 0, pageGlyphIndices.length);
            }else{
                // Going multi-page: everything cached so far is on page 0, in glyph order.
                // 切换到多页:目前为止缓存的所有内容都在第 0 页上,按字形顺序排列。
                indices[0] = new IntAr();
                for(int i = 0; i < glyphCount; i++) indices[0].add(i);
            }
            indices[page] = new IntAr();
            pageGlyphIndices = indices;
        }
        return page;
    }

    /**
     * Like {@link #pageOf(Glyph)}, for glyphs already registered by {@link #requireGlyphs(GlyphLayout)}.
     * 类似 {@link #pageOf(Glyph)},用于已被 {@link #requireGlyphs(GlyphLayout)} 注册的字形。
     */
    private int knownPageOf(Glyph glyph){
        return pageVertices.length == 1 ? 0 : pageOf(glyph);
    }

    /**
     * Sets the position of the text, relative to the position when the cached text was created.
     * <p>
     * 设置文本位置,相对于缓存文本创建时的位置。
     * @param x The x coordinate x 坐标
     * @param y The y coordinate y 坐标
     */
    public void setPosition(float x, float y){
        translate(x - this.x, y - this.y);
    }

    /**
     * Sets the position of the text, relative to its current position.
     * <p>
     * 设置文本位置,相对于其当前位置。
     * @param xAmount The amount in x to move the text 文本在 x 方向移动的量
     * @param yAmount The amount in y to move the text 文本在 y 方向移动的量
     */
    public void translate(float xAmount, float yAmount){
        if(xAmount == 0 && yAmount == 0) return;
        if(integer){
            xAmount = Math.round(xAmount);
            yAmount = Math.round(yAmount);
        }
        x += xAmount;
        y += yAmount;

        float[][] pageVertices = this.pageVertices;
        for(int i = 0, n = pageVertices.length; i < n; i++){
            float[] vertices = pageVertices[i];
            for(int ii = 0, nn = idx[i]; ii < nn; ii += SpriteBatch.vertexSize){
                vertices[ii] += xAmount;
                vertices[ii + 1] += yAmount;
            }
        }
    }

    /**
     * Sets the rotation of the text, relative to the anchor points given.
     * <p>
     * 相对给定锚点设置文本旋转。
     * @param angleDeg Angle amount in degrees 角度(度)
     * @param anchorX the anchor's x coordinate 锚点的 x 坐标
     * @param anchorY the anchor's y coordinate 锚点的 y 坐标
     */
    public void setRotation(float angleDeg, float anchorX, float anchorY){
        if(angleDeg == 0) return;
        float rad = angleDeg * Mathf.degreesToRadians;
        float cos = (float)Math.cos(rad);
        float sin = (float)Math.sin(rad);

        float[][] pageVertices = this.pageVertices;
        for(int i = 0, n = pageVertices.length; i < n; i++){
            float[] vertices = pageVertices[i];
            for(int ii = 0, nn = idx[i]; ii < nn; ii += SpriteBatch.vertexSize){
                float dx = vertices[ii] - anchorX;
                float dy = vertices[ii + 1] - anchorY;
                vertices[ii] = anchorX + dx * cos - dy * sin;
                vertices[ii + 1] = anchorY + dx * sin + dy * cos;
            }
        }
    }

    /**
     * Tints all text currently in the cache. Does not affect subsequently added text.
     * 为缓存中当前所有文本着色。不影响之后添加的文本。
     */
    public void tint(Color tint){
        float newTint = tint.toFloatBits();
        if(currentTint == newTint) return;
        currentTint = newTint;

        int[] tempGlyphCount = this.tempGlyphCount;
        for(int i = 0, n = tempGlyphCount.length; i < n; i++)
            tempGlyphCount[i] = 0;

        for(int i = 0, n = layouts.size; i < n; i++){
            GlyphLayout layout = layouts.get(i);
            for(int ii = 0, nn = layout.runs.size; ii < nn; ii++){
                GlyphRun run = layout.runs.get(ii);
                Ar<Glyph> glyphs = run.glyphs;
                float colorFloat = tempColor.set(run.color).mul(tint).toFloatBits();
                for(int iii = 0, nnn = glyphs.size; iii < nnn; iii++){
                    Glyph glyph = glyphs.get(iii);
                    int page = knownPageOf(glyph);
                    int offset = tempGlyphCount[page] * SpriteBatch.spriteSize + 5;
                    tempGlyphCount[page]++;
                    float[] vertices = pageVertices[page];
                    for(int v = 0; v < SpriteBatch.spriteSize; v += SpriteBatch.vertexSize)
                        vertices[offset + v] = colorFloat;
                }
            }
        }
    }

    /**
     * Sets the alpha component of all text currently in the cache. Does not affect subsequently added text.
     * 设置缓存中当前所有文本的 alpha 分量。不影响之后添加的文本。
     */
    public void setAlphas(float alpha){
        int alphaBits = ((int)(254 * alpha)) << 24;
        float prev = 0, newColor = 0;
        for(int j = 0, length = pageVertices.length; j < length; j++){
            float[] vertices = pageVertices[j];
            for(int i = 5, n = idx[j]; i < n; i += SpriteBatch.vertexSize){
                float c = vertices[i];
                if(c == prev && i != 5){
                    vertices[i] = newColor;
                }else{
                    prev = c;
                    int rgba = Color.floatToIntColor(c);
                    rgba = (rgba & 0x00FFFFFF) | alphaBits;
                    newColor = Color.intToFloatColor(rgba);
                    vertices[i] = newColor;
                }
            }
        }
    }

    /**
     * Sets the color of all text currently in the cache. Does not affect subsequently added text.
     * 设置缓存中当前所有文本的颜色。不影响之后添加的文本。
     */
    public void setColors(float color){
        for(int j = 0, length = pageVertices.length; j < length; j++){
            float[] vertices = pageVertices[j];
            for(int i = 5, n = idx[j]; i < n; i += SpriteBatch.vertexSize)
                vertices[i] = color;
        }
    }

    /**
     * Sets the color of all text currently in the cache. Does not affect subsequently added text.
     * 设置缓存中当前所有文本的颜色。不影响之后添加的文本。
     */
    public void setColors(Color tint){
        setColors(tint.toFloatBits());
    }

    /**
     * Sets the color of all text currently in the cache. Does not affect subsequently added text.
     * 设置缓存中当前所有文本的颜色。不影响之后添加的文本。
     */
    public void setColors(float r, float g, float b, float a){
        int intBits = ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
        setColors(Color.intToFloatColor(intBits));
    }

    /**
     * Sets the color of the specified characters. This may only be called after {@link #setText(CharSequence, float, float)} and
     * is reset every time setText is called.
     * <p>
     * 设置指定字符的颜色。只能在 {@link #setText(CharSequence, float, float)} 之后调用,且每次调用 setText 后都会重置。
     */
    public void setColors(Color tint, int start, int end){
        setColors(tint.toFloatBits(), start, end);
    }

    /**
     * Sets the color of the specified characters. This may only be called after {@link #setText(CharSequence, float, float)} and
     * is reset every time setText is called.
     * <p>
     * 设置指定字符的颜色。只能在 {@link #setText(CharSequence, float, float)} 之后调用,且每次调用 setText 后都会重置。
     */
    public void setColors(float color, int start, int end){
        if(pageVertices.length == 1){ // One page.
        // 单页。
            float[] vertices = pageVertices[0];
            for(int i = start * SpriteBatch.spriteSize + 5, n = end * SpriteBatch.spriteSize; i < n; i += SpriteBatch.vertexSize)
                vertices[i] = color;
            return;
        }

        int pageCount = pageVertices.length;
        for(int i = 0; i < pageCount; i++){
            float[] vertices = pageVertices[i];
            IntAr glyphIndices = pageGlyphIndices[i];
            // Loop through the indices and determine whether the glyph is inside begin/end.
            // 遍历索引,判断字形是否位于 begin/end 范围内。
            for(int j = 0, n = glyphIndices.size; j < n; j++){
                int glyphIndex = glyphIndices.items[j];

                // Break early if the glyph is out of bounds.
                // 若字形越界则提前退出。
                if(glyphIndex >= end) break;

                // If inside start and end, change its colour.
                // 若位于 start 和 end 之间,则更改其颜色。
                if(glyphIndex >= start){ // && glyphIndex < end
                    for(int off = 0; off < SpriteBatch.spriteSize; off += SpriteBatch.vertexSize)
                        vertices[off + (j * SpriteBatch.spriteSize + 5)] = color;
                }
            }
        }
    }

    /**
     * Returns the color used for subsequently added text. Modifying the color affects text subsequently added to the cache, but
     * does not affect existing text currently in the cache.
     * <p>
     * 返回之后添加文本所用的颜色。修改该颜色会影响之后添加到缓存的文本,但不影响缓存中已有的文本。
     */
    public Color getColor(){
        return color;
    }

    /**
     * A convenience method for setting the cache color. The color can also be set by modifying {@link #getColor()}.
     * 设置缓存颜色的便捷方法。也可以通过修改 {@link #getColor()} 来设置颜色。
     */
    public void setColor(Color color){
        this.color.set(color);
    }

    /**
     * A convenience method for setting the cache color. The color can also be set by modifying {@link #getColor()}.
     * 设置缓存颜色的便捷方法。也可以通过修改 {@link #getColor()} 来设置颜色。
     */
    public void setColor(float r, float g, float b, float a){
        color.set(r, g, b, a);
    }

    public void draw(){
        for(int j = 0, n = pageVertices.length; j < n; j++){
            if(idx[j] > 0){ // ignore if this texture has no glyphs
            // 若此纹理没有字形则忽略
                float[] vertices = pageVertices[j];
                Draw.vert(pageTextures.get(j), vertices, 0, idx[j]);
            }
        }
    }

    public void draw(int start, int end){
        if(pageVertices.length == 1){ // 1 page.
        // 1 页。
            Draw.vert(pageTextures.get(0), pageVertices[0], start * SpriteBatch.spriteSize, (end - start) * SpriteBatch.spriteSize);
            return;
        }

        // Determine vertex offset and count to render for each page. Some pages might not need to be rendered at all.
        // 确定每页要渲染的顶点偏移量和数量。有些页面可能完全无需渲染。
        for(int i = 0, pageCount = pageVertices.length; i < pageCount; i++){
            int offset = -1, count = 0;

            // For each set of glyph indices, determine where to begin within the start/end bounds.
            // 为每组字形索引确定在 start/end 范围内的起始位置。
            IntAr glyphIndices = pageGlyphIndices[i];
            for(int ii = 0, n = glyphIndices.size; ii < n; ii++){
                int glyphIndex = glyphIndices.get(ii);

                // Break early if the glyph is out of bounds.
                // 若字形越界则提前退出。
                if(glyphIndex >= end) break;

                // Determine if this glyph is within bounds. Use the first match of that for the offset.
                // 判断该字形是否在范围内。使用第一个匹配来确定偏移量。
                if(offset == -1 && glyphIndex >= start) offset = ii;

                // Determine the vertex count by counting glyphs within bounds.
                // 通过统计范围内的字形数量来确定顶点数。
                if(glyphIndex >= start) // && gInd < end
                    count++;
            }

            // Page doesn't need to be rendered.
            // 该页无需渲染。
            if(offset == -1 || count == 0) continue;

            // Render the page vertex data with the offset and count.
            // 使用偏移量和数量渲染该页的顶点数据。
            Draw.vert(pageTextures.get(i), pageVertices[i], offset * SpriteBatch.spriteSize, count * SpriteBatch.spriteSize);
        }
    }

    public void draw(float alphaModulation){
        if(alphaModulation == 1){
            draw();
            return;
        }
        Color color = getColor();
        float oldAlpha = color.a;
        color.a *= alphaModulation;
        setColors(color);
        draw();
        color.a = oldAlpha;
        setColors(color);
    }

    /**
     * Removes all glyphs in the cache.
     * 移除缓存中的所有字形。
     */
    public void clear(){
        x = 0;
        y = 0;
        glyphCount = 0;
        Pools.freeAll(pooledLayouts, true);
        pooledLayouts.clear();
        layouts.clear();
        for(int i = 0, n = idx.length; i < n; i++){
            if(pageGlyphIndices != null) pageGlyphIndices[i].clear();
            idx[i] = 0;
        }
    }

    private void requireGlyphs(GlyphLayout layout){
        //TODO: this will break if fallbacks are spread across pages, not handled for the sake of performance for now
        // TODO:若回退字形分布于多页,此处会出错;出于性能考虑暂不处理
        if(pageVertices.length == 1){
            // Simpler counting if we just have one page.
            // 只有一页时计数更简单。
            int newGlyphCount = 0;
            for(int i = 0, n = layout.runs.size; i < n; i++)
                newGlyphCount += layout.runs.get(i).glyphs.size;
            requirePageGlyphs(0, newGlyphCount);
        }else{
            for(int i = 0, n = tempGlyphCount.length; i < n; i++)
                tempGlyphCount[i] = 0;
            // Determine # of glyphs in each page; this may add pages, so re-read tempGlyphCount each time.
            // 确定每页的字形数量;这可能会新增页面,因此每次都要重新读取 tempGlyphCount。
            for(int i = 0, n = layout.runs.size; i < n; i++){
                Ar<Glyph> glyphs = layout.runs.get(i).glyphs;
                for(int ii = 0, nn = glyphs.size; ii < nn; ii++){
                    int page = pageOf(glyphs.get(ii));
                    tempGlyphCount[page]++;
                }
            }
            // Require that many for each page.
            // 为每页预留相应容量。
            for(int i = 0, n = tempGlyphCount.length; i < n; i++)
                requirePageGlyphs(i, tempGlyphCount[i]);
        }
    }

    private void requirePageGlyphs(int page, int glyphCount){
        if(pageGlyphIndices != null){
            if(glyphCount > pageGlyphIndices[page].items.length)
                pageGlyphIndices[page].ensureCapacity(glyphCount - pageGlyphIndices[page].items.length);
        }

        int vertexCount = idx[page] + glyphCount * SpriteBatch.spriteSize;
        float[] vertices = pageVertices[page];
        if(vertices == null){
            pageVertices[page] = new float[vertexCount];
        }else if(vertices.length < vertexCount){
            float[] newVertices = new float[vertexCount];
            System.arraycopy(vertices, 0, newVertices, 0, idx[page]);
            pageVertices[page] = newVertices;
        }
    }

    private void addToCache(GlyphLayout layout, float x, float y){
        layouts.add(layout);
        requireGlyphs(layout);
        for(int i = 0, n = layout.runs.size; i < n; i++){
            GlyphRun run = layout.runs.get(i);
            Ar<Glyph> glyphs = run.glyphs;
            FloatAr xAdvances = run.xAdvances;
            float color = run.color.toFloatBits();
            float gx = x + run.x, gy = y + run.y;
            for(int ii = 0, nn = glyphs.size; ii < nn; ii++){
                Glyph glyph = glyphs.get(ii);
                gx += xAdvances.get(ii);
                addGlyph(glyph, gx, gy, color);
            }
        }

        currentTint = Color.whiteFloatBits; // Cached glyphs have changed, reset the current tint.
        // 缓存的字形已改变,重置当前色调。
    }

    private void addGlyph(Glyph glyph, float x, float y, float color){
        final float scaleX = font.data.scaleX, scaleY = font.data.scaleY;
        x += glyph.xoffset * scaleX;
        y += glyph.yoffset * scaleY;
        float width = glyph.width * scaleX, height = glyph.height * scaleY;
        final float u = glyph.u, u2 = glyph.u2, v = glyph.v, v2 = glyph.v2, depth = glyph.texture.getDepth();

        if(integer){
            x = Math.round(x);
            y = Math.round(y);
            width = Math.round(width);
            height = Math.round(height);
        }
        final float x2 = x + width, y2 = y + height;

        final int page = knownPageOf(glyph);
        int idx = this.idx[page];
        this.idx[page] += SpriteBatch.spriteSize;

        if(pageGlyphIndices != null) pageGlyphIndices[page].add(glyphCount);
        glyphCount++;

        final float[] vertices = pageVertices[page];

        vertices[idx++] = x;
        vertices[idx++] = y;
        vertices[idx++] = u;
        vertices[idx++] = v;
        vertices[idx++] = depth;
        vertices[idx++] = color;
        idx++; //mix color = 0
        // 混合颜色 = 0

        vertices[idx++] = x;
        vertices[idx++] = y2;
        vertices[idx++] = u;
        vertices[idx++] = v2;
        vertices[idx++] = depth;
        vertices[idx++] = color;
        idx++; //mix color = 0
        // 混合颜色 = 0

        vertices[idx++] = x2;
        vertices[idx++] = y2;
        vertices[idx++] = u2;
        vertices[idx++] = v2;
        vertices[idx++] = depth;
        vertices[idx++] = color;
        idx++; //mix color = 0
        // 混合颜色 = 0

        vertices[idx++] = x2;
        vertices[idx++] = y;
        vertices[idx++] = u2;
        vertices[idx++] = v;
        vertices[idx++] = depth;
        vertices[idx++] = color;
        //idx++; //mix color = 0
        // idx++; //混合颜色 = 0
    }

    /**
     * Clears any cached glyphs and adds glyphs for the specified text.
     * <p>
     * 清除所有缓存字形并添加指定文本的字形。
     * @see #addText(CharSequence, float, float, int, int, float, int, boolean, String)
     */
    public GlyphLayout setText(CharSequence str, float x, float y){
        clear();
        return addText(str, x, y, 0, str.length(), 0, Align.left, false);
    }

    /**
     * Clears any cached glyphs and adds glyphs for the specified text.
     * <p>
     * 清除所有缓存字形并添加指定文本的字形。
     * @see #addText(CharSequence, float, float, int, int, float, int, boolean, String)
     */
    public GlyphLayout setText(CharSequence str, float x, float y, float targetWidth, int halign, boolean wrap){
        clear();
        return addText(str, x, y, 0, str.length(), targetWidth, halign, wrap);
    }

    /**
     * Clears any cached glyphs and adds glyphs for the specified text.
     * <p>
     * 清除所有缓存字形并添加指定文本的字形。
     * @see #addText(CharSequence, float, float, int, int, float, int, boolean, String)
     */
    public GlyphLayout setText(CharSequence str, float x, float y, int start, int end, float targetWidth, int halign,
                               boolean wrap){
        clear();
        return addText(str, x, y, start, end, targetWidth, halign, wrap);
    }

    /**
     * Clears any cached glyphs and adds glyphs for the specified text.
     * <p>
     * 清除所有缓存字形并添加指定文本的字形。
     * @see #addText(CharSequence, float, float, int, int, float, int, boolean, String)
     */
    public GlyphLayout setText(CharSequence str, float x, float y, int start, int end, float targetWidth, int halign,
                               boolean wrap, String truncate){
        clear();
        return addText(str, x, y, start, end, targetWidth, halign, wrap, truncate);
    }

    /**
     * Clears any cached glyphs and adds the specified glyphs.
     * <p>
     * 清除所有缓存字形并添加指定的字形。
     * @see #addText(CharSequence, float, float, int, int, float, int, boolean, String)
     */
    public void setText(GlyphLayout layout, float x, float y){
        clear();
        addText(layout, x, y);
    }

    /**
     * Adds glyphs for the specified text.
     * <p>
     * 为指定文本添加字形。
     * @see #addText(CharSequence, float, float, int, int, float, int, boolean, String)
     */
    public GlyphLayout addText(CharSequence str, float x, float y){
        return addText(str, x, y, 0, str.length(), 0, Align.left, false, null);
    }

    /**
     * Adds glyphs for the specified text.
     * <p>
     * 为指定文本添加字形。
     * @see #addText(CharSequence, float, float, int, int, float, int, boolean, String)
     */
    public GlyphLayout addText(CharSequence str, float x, float y, float targetWidth, int halign, boolean wrap){
        return addText(str, x, y, 0, str.length(), targetWidth, halign, wrap, null);
    }

    /**
     * Adds glyphs for the specified text.
     * <p>
     * 为指定文本添加字形。
     * @see #addText(CharSequence, float, float, int, int, float, int, boolean, String)
     */
    public GlyphLayout addText(CharSequence str, float x, float y, int start, int end, float targetWidth, int halign,
                               boolean wrap){
        return addText(str, x, y, start, end, targetWidth, halign, wrap, null);
    }

    /**
     * Adds glyphs for the the specified text.
     * <p>
     * 为指定文本添加字形。
     * @param x The x position for the left most character. 最左侧字符的 x 位置。
     * @param y The y position for the top of most capital letters in the font (the {@link FontData#capHeight cap height}). 字体中大多数字母顶部所在的 y 位置({@link FontData#capHeight cap height})。
     * @param start The first character of the string to draw. 要绘制字符串的第一个字符。
     * @param end The last character of the string to draw (exclusive). 要绘制字符串的最后一个字符(不含)。
     * @param targetWidth The width of the area the text will be drawn, for wrapping or truncation. 文本绘制区域的宽度,用于换行或截断。
     * @param halign Horizontal alignment of the text, see {@link Align}. 文本的水平对齐方式,见 {@link Align}。
     * @param wrap If true, the text will be wrapped within targetWidth. 若为 true,文本将在 targetWidth 内换行。
     * @param truncate If not null, the text will be truncated within targetWidth with this string appended. May be an empty
     * string. 若不为 null,文本将在 targetWidth 内截断并追加此字符串。可为空字符串。
     * @return The glyph layout for the cached string (the layout's height is the distance from y to the baseline). 缓存字符串的 glyph layout(layout 高度为 y 到基线的距离)。
     */
    public GlyphLayout addText(CharSequence str, float x, float y, int start, int end, float targetWidth, int halign,
                               boolean wrap, String truncate){
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        pooledLayouts.add(layout);
        layout.setText(font, str, start, end, color, targetWidth, halign, wrap, truncate);
        addText(layout, x, y);
        return layout;
    }

    /**
     * Adds the specified glyphs.
     * 添加指定的字形。
     */
    public void addText(GlyphLayout layout, float x, float y){
        addToCache(layout, x, y + font.data.ascent);
    }

    /**
     * Returns the x position of the cached string, relative to the position when the string was cached.
     * 返回缓存字符串的 x 位置,相对于字符串缓存时的位置。
     */
    public float getX(){
        return x;
    }

    /**
     * Returns the y position of the cached string, relative to the position when the string was cached.
     * 返回缓存字符串的 y 位置,相对于字符串缓存时的位置。
     */
    public float getY(){
        return y;
    }

    public Font getFont(){
        return font;
    }

    /**
     * Specifies whether to use integer positions or not. Default is to use them so filtering doesn't kick in as badly.
     * 指定是否使用整数位置。默认使用,以免过滤效果过于明显。
     */
    public void setUseIntegerPositions(boolean use){
        this.integer = use;
    }

    /**
     * @return whether this font uses integer positions for drawing.
     * @return whether this font uses integer positions for drawing. 此字体绘制时是否使用整数位置。
     */
    public boolean usesIntegerPositions(){
        return integer;
    }

    public float[] getVertices(){
        return getVertices(0);
    }

    public float[] getVertices(int page){
        return pageVertices[page];
    }

    public int getVertexCount(int page){
        return idx[page];
    }

    public Ar<GlyphLayout> getLayouts(){
        return layouts;
    }
}