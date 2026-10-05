package arc.graphics.g2d;

import arc.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;

import java.nio.*;
import java.util.*;

/**
 * Draws 2D images, optimized for geometry that does not change. Sprites and/or textures are cached and given an ID, which can
 * later be used for drawing. The size, color, and texture region for each cached image cannot be modified. This information is
 * stored in video memory and does not have to be sent to the GPU each time it is drawn.<br>
 * <br>
 * To cache {@link Texture textures}, first call {@link SpriteCache#beginCache()}, then call the
 * appropriate add method to define the images. To complete the cache, call {@link SpriteCache#endCache()} and store the returned
 * cache ID.<br>
 * <br>
 * To draw with SpriteCache, first call {@link #begin()}, then call {@link #draw(int)} with a cache ID. When SpriteCache drawing
 * is complete, call {@link #end()}.<br>
 * <br>
 * By default, SpriteCache draws using screen coordinates and uses an x-axis pointing to the right, an y-axis pointing upwards and
 * the origin is the bottom left corner of the screen. The default transformation and projection matrices can be changed. If the
 * screen is {@link ApplicationListener#resize(int, int) resized}, the SpriteCache's matrices must be updated. For example:<br>
 * <code>cache.getProjection().setOrtho(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());</code><br>
 * <br>
 * Note that SpriteCache does not manage blending. You will need to enable blending (<i>Gl.enable(Gl.blend);</i>) and
 * set the blend func as needed before or between calls to {@link #draw(int)}.<br>
 * <br>
 * SpriteCache must be disposed once it is no longer needed.
 * <p>
 * 绘制 2D 图像,针对不变的几何体进行优化。精灵和/或纹理被缓存并获得一个 ID,之后可用于绘制。每个缓存图像的大小、颜色和纹理区域不可修改。这些信息存储在显存中,无需在每次绘制时发送到 GPU。 <br> <br> 要缓存 {@link Texture texture},先调用 {@link SpriteCache#beginCache()},再调用相应的 add 方法定义图像;调用 {@link SpriteCache#endCache()} 完成缓存并保存返回的缓存 ID。 <br> <br> 要用 SpriteCache 绘制,先调用 {@link #begin()},再用缓存 ID 调用 {@link #draw(int)};绘制完成后调用 {@link #end()}。 <br> <br> 默认情况下,SpriteCache 使用屏幕坐标绘制,x 轴向右,y 轴向上,原点位于屏幕左下角。默认的变换矩阵和投影矩阵可以更改。若屏幕被 {@link ApplicationListener#resize(int, int) resized},则必须更新 SpriteCache 的矩阵。 <br> <br> 注意 SpriteCache 不管理混合。需要在调用 {@link #draw(int)} 之前或之间启用混合(<i>Gl.enable(Gl.blend);</i>)并按需设置混合函数。 <br> <br> SpriteCache 不再使用后必须释放。
 * @author Nathan Sweet
 */
public class SpriteCache implements Disposable{
    //xy + uv + depth + color
    // xy + uv + 深度 + 颜色
    static final int vertexSize = 2 + 2 + 1 + 1;

    private static final float[] tempVertices = new float[vertexSize * 6];

    private final Mesh mesh;
    private final Mat transformMatrix = new Mat();
    private final Mat projectionMatrix = new Mat();
    private final Mat combinedMatrix = new Mat();
    private final Shader shader;
    private final Ar<Texture> textures = new Ar<>(8);
    private final IntAr counts = new IntAr(8);
    /**
     * Number of render calls since the last {@link #begin()}.
     * 自上次 {@link #begin()} 以来的渲染调用次数。
     */
    public int renderCalls = 0;
    /**
     * Number of rendering calls, ever. Will not be reset unless set manually.
     * 历史渲染调用总数。除非手动设置,否则不会重置。
     */
    public int totalRenderCalls = 0;
    private boolean drawing;
    private Ar<Cache> caches;
    private Cache currentCache;
    private float colorPacked = Color.whiteFloatBits;
    private Shader customShader = null;

    /**
     * Creates a cache that uses indexed geometry and can contain up to 1000 images.
     * 创建使用索引几何、最多可容纳 1000 张图像的缓存。
     */
    public SpriteCache(){
        this(1000, false);
    }

    /**
     * Creates a cache with the specified size, using a default shader.
     * <p>
     * 以指定大小创建缓存,使用默认着色器。
     * @param size The maximum number of images this cache can hold. The memory required to hold the images is allocated up front.
     * Max of 8191 if indices are used. 此缓存可容纳的最大图像数。容纳图像所需的内存会预先分配。使用索引时最大为 8191。
     * @param useIndices If true, indexed geometry will be used. 若为 true,将使用索引几何。
     */
    public SpriteCache(int size, boolean useIndices){
        this(size, 16, getDefaultShader(), useIndices);
    }

    public SpriteCache(int size, int cacheSize, boolean useIndices){
        this(size, cacheSize, getDefaultShader(), useIndices);
    }

    /**
     * Creates a cache with the specified size and OpenGL ES 2.0 shader.
     * <p>
     * 以指定大小和 OpenGL ES 2.0 着色器创建缓存。
     * @param size The maximum number of images this cache can hold. The memory required to hold the images is allocated up front.
     * Max of 8191 if indices are used. 此缓存可容纳的最大图像数。容纳图像所需的内存会预先分配。使用索引时最大为 8191。
     * @param useIndices If true, indexed geometry will be used. 若为 true,将使用索引几何。
     */
    public SpriteCache(int size, int cacheSize, Shader shader, boolean useIndices){
        this.shader = shader;

        if(useIndices && size > 16382)
            throw new IllegalArgumentException("Can't have more than 16382 sprites per batch: " + size);

        mesh = new Mesh(true, size * (useIndices ? 4 : 6), 0,
        VertexAttribute.position,
        VertexAttribute.texCoords3,
        VertexAttribute.color
        );
        if(useIndices){
            mesh.indices = SpriteIndices.get();
        }

        caches = new Ar<>(cacheSize);

        projectionMatrix.setOrtho(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
    }

    private static @Nullable Shader defaultShader;

    public static Shader getDefaultShader(){
        if(defaultShader != null) return defaultShader;
        String vertexShader = "attribute vec4 " + Shader.positionAttribute + ";\n" //
        + "attribute vec4 " + Shader.colorAttribute + ";\n" //
        + "attribute vec3 " + Shader.texcoordAttribute + "0;\n" //
        + "uniform mat4 u_projectionViewMatrix;\n" //
        + "varying vec4 v_color;\n" //
        + "varying vec3 v_texCoords;\n" //
        + "\n" //
        + "void main(){\n" //
        + "   v_color = " + Shader.colorAttribute + ";\n" //
        + "   v_color.a = v_color.a * (255.0/254.0);\n" //
        + "   v_texCoords = " + Shader.texcoordAttribute + "0;\n" //
        + "   gl_Position =  u_projectionViewMatrix * " + Shader.positionAttribute + ";\n" //
        + "}\n";
        String fragmentShader =
          "varying lowp vec4 v_color;\n" //
        + "varying highp vec3 v_texCoords;\n" //
        + "uniform highp sampler2DArray u_texture;\n"
        + "void main(){\n" //
        + "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" //
        + "}";
        return defaultShader = new Shader(vertexShader, fragmentShader);
    }

    public Ar<Cache> getCaches(){
        return caches;
    }

    /** @see #setColor(Color) */
    public void setColor(float r, float g, float b, float a){
        colorPacked = Color.toFloatBits(r, g, b, a);
    }

    /**
     * Sets the color used to tint images when they are added to the SpriteCache. Default is {@link Color#white}.
     * 设置图像加入 SpriteCache 时用于着色的颜色。默认为 {@link Color#white}。
     */
    public void setColor(Color tint){
        colorPacked = tint.toFloatBits();
    }

    public float getPackedColor(){
        return colorPacked;
    }

    /**
     * Sets the color of this sprite cache, expanding the alpha from 0-254 to 0-255.
     * <p>
     * 设置此精灵缓存的颜色,将 alpha 从 0-254 扩展到 0-255。
     * @see Color#toFloatBits()
     */
    public void setPackedColor(float packedColor){
        colorPacked = packedColor;
    }

    /**
     * Starts the definition of a new cache, allowing the add and {@link #endCache()} methods to be called.
     * 开始定义新缓存,之后可调用 add 和 {@link #endCache()} 方法。
     */
    public void beginCache(){
        if(drawing) throw new IllegalStateException("end must be called before beginCache");
        if(currentCache != null) throw new IllegalStateException("endCache must be called before begin.");
        mesh.getVertices().position(caches.isEmpty() ? 0 : caches.peek().offset + caches.peek().maxCount);
        currentCache = new Cache(caches.size, mesh.getVertices().position());
        caches.add(currentCache);
        mesh.getVertices().limit(mesh.getVertices().capacity());
    }

    /**
     * Starts the redefinition of an existing cache, allowing the add and {@link #endCache()} methods to be called. It cannot have more entries added to it than when it was first created.
     * To do that, use {@link #clear()} and then {@link #begin()}.
     * <p>
     * 开始重定义现有缓存,之后可调用 add 和 {@link #endCache()} 方法。添加的条目不能超过首次创建时的数量;若需如此,请先 {@link #clear()} 再 {@link #begin()}。
     */
    public void beginCache(int cacheID){
        if(drawing) throw new IllegalStateException("end must be called before beginCache");
        if(currentCache != null) throw new IllegalStateException("endCache must be called before begin.");
        currentCache = caches.get(cacheID);
        Arrays.fill(currentCache.counts, 0);
        mesh.getVertices().position(currentCache.offset);
    }

    /**
     * Ends the definition of a cache, returning the cache ID to be used with {@link #draw(int)}.
     * 结束缓存定义,返回与 {@link #draw(int)} 配套使用的缓存 ID。
     */
    public int endCache(){
        if(currentCache == null) throw new IllegalStateException("beginCache must be called before endCache.");
        Cache cache = currentCache;
        int cacheCount = mesh.getVertices().position() - cache.offset;
        if(cache.textures == null){
            // New cache.
            // 新建缓存。
            cache.maxCount = cacheCount;
            cache.textureCount = textures.size;
            cache.textures = textures.toArray(Texture.class);
            cache.counts = new int[cache.textureCount];
            for(int i = 0, n = counts.size; i < n; i++)
                cache.counts[i] = counts.get(i);
        }else{
            // Redefine existing cache.
            // 重定义现有缓存。
            if(cacheCount > cache.maxCount){
                throw new ArcRuntimeException(
                "If a cache is not the last created, it cannot be redefined with more entries than when it was first created: "
                + cacheCount + " (" + cache.maxCount + " max)");
            }

            cache.textureCount = textures.size;

            if(cache.textures.length < cache.textureCount) cache.textures = new Texture[cache.textureCount];
            for(int i = 0, n = cache.textureCount; i < n; i++)
                cache.textures[i] = textures.get(i);

            if(cache.counts.length < cache.textureCount) cache.counts = new int[cache.textureCount];
            for(int i = 0, n = cache.textureCount; i < n; i++)
                cache.counts[i] = counts.get(i);

            FloatBuffer vertices = mesh.getVertices();
            vertices.position(0);
            Cache lastCache = caches.get(caches.size - 1);
            vertices.limit(lastCache.offset + lastCache.maxCount);
        }

        currentCache = null;
        textures.clear();
        counts.clear();

        //fixes teaVM bug, since it draws based on offset apparently
        // 修复 teaVM 的 bug,因为它显然是基于偏移量绘制的
        if(Core.app.isWeb()){
            mesh.getVertices().position(0);
        }

        return cache.id;
    }

    /**
     * Invalidates all cache IDs and resets the SpriteCache so new caches can be added.
     * 使所有缓存 ID 失效并重置 SpriteCache,以便添加新缓存。
     */
    public void clear(){
        caches.clear();
        mesh.getVertices().clear().flip();
    }

    /** Ensures that this cache can hold this amount of sprites. Only call at the end of cache.
     * <p>
     * 确保此缓存能容纳该数量的精灵。仅在缓存结束时调用。
     * @return number of new sprites actually reserved. 实际新预留的精灵数量。 */
    public int reserve(int sprites){
        if(currentCache == null) throw new IllegalStateException("beginCache must be called before ensureSize.");

        //size of each sprite
        // 每个精灵的大小
        int spriteSize = vertexSize * (mesh.getNumIndices() > 0 ? 4 : 6);
        //currently used vertices
        // 当前已使用的顶点数
        int currentUsed = currentCache.maxCount;
        //vertices that need to be guaranteed
        // 需要保证可用的顶点数
        int required = sprites * spriteSize;
        //number of extra vertices to reserve
        // 额外预留的顶点数
        int toAdd = required - currentUsed;
        if(toAdd > 0){
            currentCache.maxCount += toAdd;
            mesh.getVertices().position(currentCache.offset + currentCache.maxCount);
            return toAdd / spriteSize;
        }
        return 0;
    }

    public int getSpritesUsed(){
        return (caches.isEmpty() ? 0 : caches.peek().offset + caches.peek().maxCount) / (vertexSize * (mesh.getNumIndices() > 0 ? 4 : 6));
    }

    public int getSpriteCapacity(){
        return mesh.getVertices().limit() / (vertexSize * (mesh.getNumIndices() > 0 ? 4 : 6));
    }

    /**
     * Adds the specified vertices to the cache. Each vertex should have 5 elements, one for each of the attributes: x, y, color,
     * u, and v. If indexed geometry is used, each image should be specified as 4 vertices, otherwise each image should be
     * specified as 6 vertices.
     * <p>
     * 将指定顶点加入缓存。每个顶点应包含 5 个元素,对应属性 x、y、color、u、v。若使用索引几何,每张图像应指定 4 个顶点,否则应指定 6 个顶点。
     */
    public void add(Texture texture, float[] vertices, int offset, int length){
        if(currentCache == null) throw new IllegalStateException("beginCache must be called before add.");
        if(mesh.getVertices().position() + length >= mesh.getVertices().limit())
            throw new IllegalStateException("Out of vertex space! Size: " + mesh.getVertices().capacity() + " Required: " + (mesh.getVertices().position() + length));

        int verticesPerImage = mesh.getNumIndices() > 0 ? 4 : 6;
        int count = length / (verticesPerImage * vertexSize) * 6;
        int lastIndex = textures.size - 1;
        if(lastIndex < 0 || textures.get(lastIndex).getHandle() != texture.getHandle()){
            textures.add(texture);
            counts.add(count);
        }else
            counts.incr(lastIndex, count);

        mesh.getVertices().put(vertices, offset, length);
    }

    /**
     * Adds the specified region to the cache.
     * 将指定区域加入缓存。
     */
    public void add(TextureRegion region, float x, float y){
        add(region, x, y, region.width, region.height);
    }

    /**
     * Adds the specified region to the cache.
     * 将指定区域加入缓存。
     */
    public void add(TextureRegion region, float x, float y, float width, float height){
        final float fx2 = x + width;
        final float fy2 = y + height;
        final float u = region.u;
        final float v = region.v2;
        final float u2 = region.u2;
        final float v2 = region.v;
        float depth = region.getDepth();

        tempVertices[0] = x;
        tempVertices[1] = y;
        tempVertices[2] = u;
        tempVertices[3] = v;
        tempVertices[4] = depth;
        tempVertices[5] = colorPacked;

        tempVertices[6] = x;
        tempVertices[7] = fy2;
        tempVertices[8] = u;
        tempVertices[9] = v2;
        tempVertices[10] = depth;
        tempVertices[11] = colorPacked;

        tempVertices[12] = fx2;
        tempVertices[13] = fy2;
        tempVertices[14] = u2;
        tempVertices[15] = v2;
        tempVertices[16] = depth;
        tempVertices[17] = colorPacked;

        if(mesh.getNumIndices() > 0){
            tempVertices[18] = fx2;
            tempVertices[19] = y;
            tempVertices[20] = u2;
            tempVertices[21] = v;
            tempVertices[22] = depth;
            tempVertices[23] = colorPacked;
            add(region.texture, tempVertices, 0, 24);
        }else{
            tempVertices[18] = fx2;
            tempVertices[19] = fy2;
            tempVertices[20] = u2;
            tempVertices[21] = v2;
            tempVertices[22] = depth;
            tempVertices[23] = colorPacked;

            tempVertices[24] = fx2;
            tempVertices[25] = y;
            tempVertices[26] = u2;
            tempVertices[27] = v;
            tempVertices[28] = depth;
            tempVertices[29] = colorPacked;

            tempVertices[30] = x;
            tempVertices[31] = y;
            tempVertices[32] = u;
            tempVertices[33] = v;
            tempVertices[34] = depth;
            tempVertices[35] = colorPacked;
            add(region.texture, tempVertices, 0, 36);
        }
    }

    /**
     * Adds the specified region to the cache.
     * 将指定区域加入缓存。
     */
    public void add(TextureRegion region, float x, float y, float originX, float originY, float width, float height,
                    float scaleX, float scaleY, float rotation){

        // bottom left and top right corner points relative to origin
        // 相对于原点的左下角和右上角顶点
        final float worldOriginX = x + originX;
        final float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;

        // scale
        // 缩放
        if(scaleX != 1 || scaleY != 1){
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }

        // construct corner points, start from top left and go counter clockwise
        // 构造角点,从左上角开始按逆时针方向进行
        final float p1x = fx;
        final float p1y = fy;
        final float p2x = fx;
        final float p2y = fy2;
        final float p3x = fx2;
        final float p3y = fy2;
        final float p4x = fx2;
        final float p4y = fy;

        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float x4;
        float y4;

        // rotate
        // 旋转
        if(rotation != 0){
            final float cos = Mathf.cosDeg(rotation);
            final float sin = Mathf.sinDeg(rotation);

            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;

            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;

            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;

            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        }else{
            x1 = p1x;
            y1 = p1y;

            x2 = p2x;
            y2 = p2y;

            x3 = p3x;
            y3 = p3y;

            x4 = p4x;
            y4 = p4y;
        }

        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;

        final float u = region.u;
        final float v = region.v2;
        final float u2 = region.u2;
        final float v2 = region.v;
        float depth = region.getDepth();

        tempVertices[0] = x1;
        tempVertices[1] = y1;
        tempVertices[2] = u;
        tempVertices[3] = v;
        tempVertices[4] = depth;
        tempVertices[5] = colorPacked;

        tempVertices[6] = x2;
        tempVertices[7] = y2;
        tempVertices[8] = u;
        tempVertices[9] = v2;
        tempVertices[10] = depth;
        tempVertices[11] = colorPacked;

        tempVertices[12] = x3;
        tempVertices[13] = y3;
        tempVertices[14] = u2;
        tempVertices[15] = v2;
        tempVertices[16] = depth;
        tempVertices[17] = colorPacked;

        if(mesh.getNumIndices() > 0){
            tempVertices[18] = x4;
            tempVertices[19] = y4;
            tempVertices[20] = u2;
            tempVertices[21] = v;
            tempVertices[22] = depth;
            tempVertices[23] = colorPacked;
            add(region.texture, tempVertices, 0, 24);
        }else{
            tempVertices[18] = x3;
            tempVertices[19] = y3;
            tempVertices[20] = u2;
            tempVertices[21] = v2;
            tempVertices[22] = depth;
            tempVertices[23] = colorPacked;

            tempVertices[24] = x4;
            tempVertices[25] = y4;
            tempVertices[26] = u2;
            tempVertices[27] = v;
            tempVertices[28] = depth;
            tempVertices[29] = colorPacked;

            tempVertices[30] = x1;
            tempVertices[31] = y1;
            tempVertices[32] = u;
            tempVertices[33] = v;
            tempVertices[34] = depth;
            tempVertices[35] = colorPacked;
            add(region.texture, tempVertices, 0, 36);
        }
    }

    /**
     * Prepares the OpenGL state for SpriteCache rendering.
     * 为 SpriteCache 渲染准备 OpenGL 状态。
     */
    public void begin(){
        begin(true);
    }

    /**
     * Prepares the OpenGL state for SpriteCache rendering.
     * 为 SpriteCache 渲染准备 OpenGL 状态。
     */
    public void begin(boolean writeUniforms){
        if(drawing) throw new IllegalStateException("end must be called before begin.");
        if(currentCache != null) throw new IllegalStateException("endCache must be called before begin");
        renderCalls = 0;
        Shader shader = customShader != null ? customShader : this.shader;
        shader.bind();

        if(writeUniforms){
            combinedMatrix.set(projectionMatrix).mul(transformMatrix);
            shader.setUniformMatrix4("u_projectionViewMatrix", combinedMatrix);
        }

        mesh.bind(shader);
        drawing = true;
    }

    /**
     * Completes rendering for this SpriteCache.
     * 完成此 SpriteCache 的渲染。
     */
    public void end(){
        if(!drawing) throw new IllegalStateException("begin must be called before end.");
        drawing = false;

        if(customShader != null)
            mesh.unbind(customShader);
        else
            mesh.unbind(shader);
    }

    /**
     * Draws all the images defined for the specified cache ID.
     * 绘制指定缓存 ID 定义的所有图像。
     */
    public void draw(int cacheID){
        if(!drawing) throw new IllegalStateException("SpriteCache.begin must be called before draw.");

        Cache cache = caches.get(cacheID);
        int verticesPerImage = mesh.getNumIndices() > 0 ? 4 : 6;
        int offset = cache.offset / (verticesPerImage * vertexSize) * 6;
        Texture[] textures = cache.textures;
        int[] counts = cache.counts;
        int textureCount = cache.textureCount;
        Shader shader = customShader != null ? customShader : this.shader;
        for(int i = 0; i < textureCount; i++){
            int count = counts[i];
            textures[i].bind();

            mesh.render(shader, Gl.triangles, offset, count, false);
            offset += count;
        }
        renderCalls += textureCount;
        totalRenderCalls += textureCount;
    }

    /**
     * Draws a subset of images defined for the specified cache ID.
     * <p>
     * 绘制指定缓存 ID 定义的图像子集。
     * @param offset The first image to render. 要渲染的第一张图像。
     * @param length The number of images from the first image (inclusive) to render. 从第一张图像(含)起要渲染的图像数。
     */
    public void draw(int cacheID, int offset, int length){
        if(!drawing) throw new IllegalStateException("SpriteCache.begin must be called before draw.");

        Cache cache = caches.get(cacheID);
        offset = offset * 6 + cache.offset;
        length *= 6;
        Texture[] textures = cache.textures;
        int[] counts = cache.counts;
        int textureCount = cache.textureCount;
        for(int i = 0; i < textureCount; i++){
            textures[i].bind();
            int count = counts[i];
            if(count > length){
                i = textureCount;
                count = length;
            }else
                length -= count;
            if(customShader != null)
                mesh.render(customShader, Gl.triangles, offset, count, false);
            else
                mesh.render(shader, Gl.triangles, offset, count, false);
            offset += count;
        }
        renderCalls += cache.textureCount;
        totalRenderCalls += textureCount;
    }

    /**
     * Releases all resources held by this SpriteCache.
     * 释放此 SpriteCache 持有的所有资源。
     */
    @Override
    public void dispose(){
        mesh.dispose();
        if(shader != null && shader != getDefaultShader()) shader.dispose();
    }

    public Mat getProjectionMatrix(){
        return projectionMatrix;
    }

    public void setProjectionMatrix(Mat projection){
        if(drawing) throw new IllegalStateException("Can't set the matrix within begin/end.");
        projectionMatrix.set(projection);
    }

    public Mat getTransformMatrix(){
        return transformMatrix;
    }

    public void setTransformMatrix(Mat transform){
        if(drawing) throw new IllegalStateException("Can't set the matrix within begin/end.");
        transformMatrix.set(transform);
    }

    /**
     * Sets the shader to be used in a GLES 2.0 environment. Vertex position attribute is called "a_position", the texture
     * coordinates attribute is called called "a_texCoords", the color attribute is called "a_color". The projection matrix is
     * uploaded via a mat4 uniform called "u_proj", the transform matrix is uploaded via a uniform called "u_trans", the combined
     * transform and projection matrx is is uploaded via a mat4 uniform called "u_projTrans". The texture sampler is passed via a
     * uniform called "u_texture".
     * <p>
     * Call this method with a null argument to use the default shader.
     * <p>
     * 设置在 GLES 2.0 环境中使用的着色器。顶点位置属性名为 "a_position",纹理坐标属性名为 "a_texCoords",颜色属性名为 "a_color"。投影矩阵通过名为 "u_proj" 的 mat4 uniform 上传,变换矩阵通过名为 "u_trans" 的 uniform 上传,变换与投影的合成矩阵通过名为 "u_projTrans" 的 mat4 uniform 上传。纹理采样器通过名为 "u_texture" 的 uniform 传入。 <p> 传入 null 参数即可使用默认着色器。
     * @param shader the {@link Shader} or null to use the default shader. {@link Shader};为 null 则使用默认着色器。
     */
    public void setShader(Shader shader){
        customShader = shader;
    }

    public boolean isDrawing(){
        return drawing;
    }

    private static class Cache{
        final int id;
        final int offset;
        int maxCount;
        int textureCount;
        Texture[] textures;
        int[] counts;

        public Cache(int id, int offset){
            this.id = id;
            this.offset = offset;
        }
    }
}