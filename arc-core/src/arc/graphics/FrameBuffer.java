package arc.graphics;

import arc.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.struct.*;
import arc.util.*;

import java.nio.*;

/**
 * <p>
 * Encapsulates OpenGL ES 2.0 frame buffer objects. This is a simple helper class which should cover most FBO uses.
 * </p>
 *
 * <p>
 * A FrameBuffer must be disposed if it is no longer needed.
 * </p>
 * <p>
 * <p> 封装 OpenGL ES 2.0 帧缓冲对象。这是一个简单的辅助类,应能覆盖大多数 FBO 用途。 </p> <p> FrameBuffer 不再使用时必须释放。 </p>
 * @author mzechner, realitix
 */
public class FrameBuffer implements Disposable{
    /**
     * the currently bound framebuffer; null for the default one.
     * 当前绑定的帧缓冲;默认帧缓冲时为 null。
     */
    protected static FrameBuffer currentBoundFramebuffer;
    /**
     * the default framebuffer handle, a.k.a screen.
     * 默认帧缓冲的句柄,即屏幕。
     */
    protected static int defaultFramebufferHandle = -1;
    /**
     * # of nested buffers right now
     * 当前嵌套缓冲区的数量
     */
    protected static int bufferNesting;

    /**
     * the framebuffer that was bound before this one began (null to indicate that nothing was bound) *
     * 在此帧缓冲开始前绑定的帧缓冲(null 表示之前没有绑定) *
     */
    protected FrameBuffer lastBoundFramebuffer = null;

    /**
     * all texture attachments, defined in the same order as the formats were specified. *
     * 所有纹理附件,按指定格式的顺序排列。 *
     */
    public Ar<Texture> textureAttachments = new Ar<>();
    public int width, height;
    public @Nullable Texture texture, depthTexture, stencilTexture;

    protected Format[] formats;
    /**
     * the framebuffer handle *
     * 帧缓冲句柄 *
     */
    protected int framebufferHandle;

    public FrameBuffer(int width, int height, Format... formats){
        init(width, height, formats.length == 0 ? Format.defaultColor : formats);
    }

    public FrameBuffer(Format... formats){
        this(2, 2, formats);
    }

    /**
     * Note that this does nothing if the width and height are the same.
     * 注意,若宽高相同则什么也不做。
     */
    public boolean resize(int width, int height){
        //prevent incomplete attachment issues.
        // 防止出现附件不完整的问题。
        width = Math.max(width, 2);
        height = Math.max(height, 2);

        //ignore pointless resizing
        // 忽略无意义的尺寸调整
        if(width == this.width && height == this.height) return false;

        init(width, height, formats);

        return true;
    }

    protected void init(int width, int height, Format[] formats){
        Ar<Texture> oldFilters = textureAttachments.isEmpty() ? null : textureAttachments.copy();

        //init() can be called multiple times, so dispose the old textures
        // init() 可能被多次调用,因此需释放旧纹理
        disposeTextures();

        this.formats = formats;
        this.width = width;
        this.height = height;

        //save last buffer's handle
        // 保存上一个缓冲区的句柄
        int lastHandle = currentBoundFramebuffer == null ? getDefaultFramebufferHandle() : currentBoundFramebuffer.framebufferHandle;

        if(framebufferHandle == 0) framebufferHandle = Gl.genFramebuffer();
        Gl.bindFramebuffer(Gl.framebuffer, framebufferHandle);

        int index = 0;
        int colorTextureCounter = 0;
        for(Format format : formats){
            Texture result = new Texture();
            result.width = width;
            result.height = height;
            result.bind();

            Gl.texImage2D(Gl.texture2d, 0, format.glType, width, height, 0, format.baseFormat, format.baseType, null);

            if(format.isColor()) texture = result;
            if(format.isDepth()) depthTexture = result;
            if(format.isStencil()) stencilTexture = result;

            //preserve filters from previous init
            // 保留上一次 init 的过滤器设置
            if(oldFilters != null && index < oldFilters.size){
                Texture prev = oldFilters.get(index);
                result.setFilter(prev.getMinFilter(), prev.getMagFilter());
                result.setWrap(prev.getUWrap(), prev.getVWrap());
            }else{
                result.setFilter(format.isLinearFilterable() ? TextureFilter.linear : TextureFilter.nearest);
                result.setWrap(TextureWrap.clampToEdge);
            }

            textureAttachments.add(result);

            int point = format.isColor() ? Gl.colorAttachment0 + (colorTextureCounter ++) : format.attachmentPoint;
            Gl.framebufferTexture2D(Gl.framebuffer, point, Gl.texture2d, result.getHandle(), 0);

            index ++;
        }

        //specify active buffers with MRT
        // 在 MRT 中指定活动的缓冲区
        if(colorTextureCounter > 1){
            IntBuffer buffer = Buffers.newIntBuffer(colorTextureCounter);
            for(int i = 0; i < colorTextureCounter; i++){
                buffer.put(Gl.colorAttachment0 + i);
            }
            buffer.position(0);
            Gl.drawBuffers(colorTextureCounter, buffer);
        }

        int result = Gl.checkFramebufferStatus(Gl.framebuffer);

        //restore old bound buffer
        // 恢复旧的绑定缓冲区
        Gl.bindFramebuffer(Gl.framebuffer, lastHandle);

        if(result != Gl.framebufferComplete){
            dispose();

            if(result == Gl.framebufferIncompleteAttachment) throw new IllegalStateException("Frame buffer couldn't be constructed: incomplete attachment (" + width + "x" + height + ")");
            if(result == Gl.framebufferIncompleteDimensions) throw new IllegalStateException("Frame buffer couldn't be constructed: incomplete dimensions");
            if(result == Gl.framebufferIncompleteMissingAttachment) throw new IllegalStateException("Frame buffer couldn't be constructed: missing attachment");
            if(result == Gl.framebufferUnsupported) throw new IllegalStateException("Frame buffer couldn't be constructed: unsupported combination of formats");
            throw new IllegalStateException("Frame buffer couldn't be constructed: unknown error " + result);
        }
    }

    public void blit(Shader shader){
        Draw.blit(this, shader);
    }

    /**
     * Makes the frame buffer current so everything gets drawn to it.
     * 使该帧缓冲成为当前帧缓冲,之后所有内容都绘制到它上面。
     */
    public void bind(){
        Gl.bindFramebuffer(Gl.framebuffer, framebufferHandle);
    }

    public boolean isBound(){
        return currentBoundFramebuffer == this;
    }

    /**
     * Flushes the batch, begins this buffer and clears the screen.
     * 刷新批处理器,开始该缓冲区并清屏。
     */
    public void begin(Color clearColor){
        begin();
        Gl.clearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a);
        Gl.clear(Gl.colorBufferBit | (depthTexture != null ? Gl.depthBufferBit : 0) | (stencilTexture != null ? Gl.stencilBufferBit : 0));
    }

    /**
     * Binds the frame buffer and sets the viewport accordingly, so everything gets drawn to it.
     * 绑定该帧缓冲并相应设置视口,之后所有内容都绘制到它上面。
     */
    public void begin(){
        Draw.flush();
        //save last buffer
        // 保存上一个缓冲区
        if(currentBoundFramebuffer == this) throw new IllegalArgumentException("Do not begin() twice.");
        //save last buffer
        // 保存上一个缓冲区
        lastBoundFramebuffer = currentBoundFramebuffer;
        currentBoundFramebuffer = this;
        bufferNesting ++;
        bind();
        Gl.viewport(0, 0, width, height);
    }

    /**
     * Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.
     * 解绑帧缓冲,此后所有绘制都在普通帧缓冲上进行。
     */
    public void end(){
        Draw.flush();
        //there was a buffer before this one
        // 在此之前存在一个缓冲区
        if(lastBoundFramebuffer != null){
            //rebind the last framebuffer and set its viewport
            // 重新绑定上一个帧缓冲并设置其视口
            lastBoundFramebuffer.bind();
            Gl.viewport(0, 0, lastBoundFramebuffer.width, lastBoundFramebuffer.height);
        }else{
            //bind to default buffer and viewport
            // 绑定默认缓冲区和视口
            Gl.bindFramebuffer(Gl.framebuffer, getDefaultFramebufferHandle());
            Gl.viewport(0, 0, Core.graphics.getBackBufferWidth(), Core.graphics.getBackBufferHeight());
        }

        bufferNesting --;

        //set last bound framebuffer as current
        // 将最后绑定的帧缓冲设为当前帧缓冲
        currentBoundFramebuffer = lastBoundFramebuffer;
        //no longer bound, so nothing came last
        // 已不再绑定,因此之前没有缓冲区
        lastBoundFramebuffer = null;
    }

    /**
     * @return The OpenGL handle of the framebuffer 帧缓冲的 OpenGL 句柄
     */
    public int getHandle(){
        return framebufferHandle;
    }

    public int getWidth(){
        return width;
    }

    public int getHeight(){
        return height;
    }

    public @Nullable Texture getTexture(){
        return texture;
    }

    protected void disposeTextures(){
        for(Texture texture : textureAttachments){
            texture.dispose();
        }
        textureAttachments.clear();
    }

    /**
     * Releases all resources associated with the FrameBuffer.
     * 释放 FrameBuffer 关联的所有资源。
     */
    @Override
    public void dispose(){
        disposeTextures();

        Gl.deleteFramebuffer(framebufferHandle);
    }

    protected static int getDefaultFramebufferHandle(){
        // iOS uses a different framebuffer handle! (not necessarily 0)
        // iOS 使用不同的帧缓冲句柄!(不一定为 0)
        if(defaultFramebufferHandle == -1){
            defaultFramebufferHandle = Core.app.isIOS() ? Gl.getInt(Gl.framebufferBinding) : 0;
        }
        return defaultFramebufferHandle;
    }

}
