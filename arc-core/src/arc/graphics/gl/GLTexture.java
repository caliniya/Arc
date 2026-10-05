package arc.graphics.gl;

import arc.graphics.*;
import arc.util.*;

/**
 * Class representing an OpenGL texture by its target and handle. Keeps track of its state like the TextureFilter and TextureWrap.
 * Also provides some (protected) static methods to create TextureData and upload image data.
 * <p>
 * 通过目标和句柄表示 OpenGL 纹理的类。跟踪 TextureFilter 和 TextureWrap 等状态。还提供一些(受保护的)静态方法来创建 TextureData 并上传图像数据。
 * @author badlogic, Xoppa
 */
public abstract class GLTexture implements Disposable{
    /**
     * The target of this texture, used when binding the texture, e.g. GL_TEXTURE_2D
     * 此纹理的目标,绑定纹理时使用,例如 GL_TEXTURE_2D
     */
    public final int glTarget;
    /**
     * Do not change. This is read-only and only set after texture data is loaded.
     * 请勿修改。此字段只读,仅在纹理数据加载后设置。
     */
    public int width, height;

    protected int glHandle;
    protected TextureFilter minFilter = TextureFilter.nearest;
    protected TextureFilter magFilter = TextureFilter.nearest;
    protected TextureWrap uWrap = TextureWrap.clampToEdge;
    protected TextureWrap vWrap = TextureWrap.clampToEdge;

    /**
     * Generates a new OpenGL texture with the specified target.
     * 以指定目标生成新的 OpenGL 纹理。
     */
    public GLTexture(int glTarget){
        this(glTarget, Gl.genTexture());
    }

    public GLTexture(int glTarget, int glHandle){
        this.glTarget = glTarget;
        this.glHandle = glHandle;
    }

    /**
     * @return the depth of the texture in pixels 纹理的深度,以像素为单位
     */
    public int getDepth(){
        return 0;
    }

    /**
     * Binds this texture. The texture will be bound to the currently active texture unit specified.
     * 绑定此纹理。纹理将绑定到当前激活的纹理单元。
     */
    public void bind(){
        Gl.bindTexture(glTarget, glHandle);
    }

    /**
     * Binds the texture to the given texture unit. Sets the currently active texture unit.
     * <p>
     * 将纹理绑定到给定的纹理单元。设置当前激活的纹理单元。
     * @param unit the unit (0 to MAX_TEXTURE_UNITS). 单元(0 到 MAX_TEXTURE_UNITS)。
     */
    public void bind(int unit){
        Gl.activeTexture(Gl.texture0 + unit);
        Gl.bindTexture(glTarget, glHandle);
    }

    /**
     * @return The {@link TextureFilter} used for minification. 用于缩小过滤的 {@link TextureFilter}。
     */
    public TextureFilter getMinFilter(){
        return minFilter;
    }

    /**
     * @return The {@link TextureFilter} used for magnification. 用于放大过滤的 {@link TextureFilter}。
     */
    public TextureFilter getMagFilter(){
        return magFilter;
    }

    /**
     * @return The {@link TextureWrap} used for horizontal (U) texture coordinates. 水平(U)纹理坐标使用的 {@link TextureWrap}。
     */
    public TextureWrap getUWrap(){
        return uWrap;
    }

    /**
     * @return The {@link TextureWrap} used for vertical (V) texture coordinates. 垂直(V)纹理坐标使用的 {@link TextureWrap}。
     */
    public TextureWrap getVWrap(){
        return vWrap;
    }

    /**
     * @return The OpenGL handle for this texture. 此纹理的 OpenGL 句柄。
     */
    public int getHandle(){
        return glHandle;
    }

    /**
     * advanced usage only
     * 仅限高级用法
     */
    public void overwriteHandle(int handle){
        this.glHandle = handle;
    }

    public void setWrap(TextureWrap wrap){
        setWrap(wrap, wrap);
    }

    /**
     * Sets the {@link TextureWrap} for this texture on the u and v axis. This will bind this texture!
     * <p>
     * 为此纹理设置 u、v 轴的 {@link TextureWrap}。这会绑定此纹理!
     * @param u the u wrap u 方向环绕方式
     * @param v the v wrap v 方向环绕方式
     */
    public void setWrap(TextureWrap u, TextureWrap v){
        bind();
        Gl.texParameteri(glTarget, Gl.textureWrapS, (this.uWrap = u).getGLEnum());
        Gl.texParameteri(glTarget, Gl.textureWrapT, (this.vWrap = v).getGLEnum());
    }

    public void setFilter(TextureFilter filter){
        setFilter(filter, filter);
    }

    /**
     * Sets the {@link TextureFilter} for this texture for minification and magnification. This will bind this texture!
     * <p>
     * 为此纹理设置缩小和放大的 {@link TextureFilter}。这会绑定此纹理!
     * @param minFilter the minification filter 缩小过滤器
     * @param magFilter the magnification filter 放大过滤器
     */
    public void setFilter(TextureFilter minFilter, TextureFilter magFilter){
        bind();
        Gl.texParameteri(glTarget, Gl.textureMinFilter, (this.minFilter = minFilter).glEnum);
        Gl.texParameteri(glTarget, Gl.textureMagFilter, (this.magFilter = magFilter).glEnum);
    }

    @Override
    public boolean isDisposed(){
        return glHandle == 0;
    }

    @Override
    public void dispose(){
        if(glHandle != 0){
            Gl.deleteTexture(glHandle);
            glHandle = 0;
        }
    }
}
