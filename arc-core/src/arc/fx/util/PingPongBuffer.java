package arc.fx.util;

import arc.*;
import arc.graphics.*;
import arc.graphics.gl.*;

/**
 * Encapsulates a framebuffer with the ability to ping-pong between two buffers.
 * <p>
 * Upon {@link #begin()} the buffer is reset to a known initial state, this is usually done just before the first usage of the
 * buffer.
 * <p>
 * Subsequent {@link #swap()} calls will initiate writing to the next available buffer, returning the previously used one,
 * effectively ping-ponging between the two. Until {@link #end()} is called, chained rendering will be possible by retrieving the
 * necessary buffers via {@link #getSrcTexture()}, {@link #getSrcBuffer()}, {@link #getDstTexture()} or
 * {@link #getDstBuffer}.
 * <p>
 * When finished, {@link #end()} should be called to stop capturing. When the OpenGL context is lost, {@link #rebind()} should be
 * called.
 * <p>
 * 封装一个可在两个缓冲区之间乒乓切换的帧缓冲区。
 * 调用 {@link #begin()} 时,缓冲区会被重置为已知的初始状态,这通常在第一次使用缓冲区之前完成。
 * 随后的 {@link #swap()} 调用会开始写入下一个可用缓冲区,并返回之前使用的那个,从而有效地在两者之间乒乓切换。在调用 {@link #end()} 之前,可以通过 {@link #getSrcTexture()}、{@link #getSrcBuffer()}、{@link #getDstTexture()} 或 {@link #getDstBuffer} 获取所需的缓冲区,进行链式渲染。
 * 完成后,应调用 {@link #end()} 停止捕获。当 OpenGL 上下文丢失时,应调用 {@link #rebind()}。
 * @author bmanuel
 * @author metaphore
 */
public final class PingPongBuffer{
    private final FrameBuffer buffer1;
    private final FrameBuffer buffer2;

    private FrameBuffer bufDst;
    private FrameBuffer bufSrc;

    /**
     * Keeps track of the current active buffer.
     * false - first buffer,
     * true - second buffer.
     * <p>
     * 跟踪当前活动缓冲区。
     * false - 第一个缓冲区,
     * true - 第二个缓冲区。
     **/
    private boolean writeState;

    /**
     * Where capturing is started. Should be true between {@link #begin()} and {@link #end()}.
     * 是否已开始捕获。在 {@link #begin()} 和 {@link #end()} 之间应为 true。
     */
    private boolean capturing;

    private TextureWrap wrapU = TextureWrap.clampToEdge;
    private TextureWrap wrapV = TextureWrap.clampToEdge;
    private TextureFilter filterMin = TextureFilter.linear;
    private TextureFilter filterMag = TextureFilter.linear;

    public PingPongBuffer(){
        this(Format.defaultColor);
    }

    /**
     * Initializes ping-pong buffer with the size of the client's area (usually window size).
     * <p>
     * 以客户端区域的大小(通常是窗口大小)初始化乒乓缓冲区。
     * @param fbFormat Pixel format of buffer. 缓冲区的像素格式。
     */
    public PingPongBuffer(Format[] fbFormat){
        this(Core.graphics.getWidth(), Core.graphics.getHeight(), fbFormat);
    }

    /**
     * Initializes ping-pong buffer with the given size.
     * <p>
     * 以给定大小初始化乒乓缓冲区。
     * @param fbFormat Pixel format of buffer. 缓冲区的像素格式。
     */
    public PingPongBuffer(int width, int height, Format[] fbFormat){
        this.buffer1 = new FrameBuffer(width, height, fbFormat);
        this.buffer2 = new FrameBuffer( width, height, fbFormat);
        rebind();

        // Setup src/dst buffers.
        // 设置 src/dst 缓冲区。
        writeState = false;
        this.bufDst = buffer1;
        this.bufSrc = buffer2;
    }

    public void dispose(){
        buffer1.dispose();
        buffer2.dispose();
    }

    public void resize(int width, int height){
        this.buffer1.resize(width, height);
        this.buffer2.resize(width, height);
        rebind();
        clear(Color.clear);
    }

    /**
     * Restores buffer OpenGL parameters. Could be useful in case of OpenGL context loss.
     * <p>
     * 恢复缓冲区的 OpenGL 参数。在 OpenGL 上下文丢失的情况下可能有用。
     */
    public void rebind(){
        // FBOs might be null if the instance wasn't initialized with #resize(int, int) yet.
        // 如果实例尚未通过 #resize(int, int) 初始化,FBO 可能为 null。
        if(buffer1 != null){
            Texture texture = buffer1.texture;
            texture.setWrap(wrapU, wrapV);
            texture.setFilter(filterMin, filterMag);
        }
        if(buffer2 != null){
            Texture texture = buffer2.texture;
            texture.setWrap(wrapU, wrapV);
            texture.setFilter(filterMin, filterMag);
        }
    }

    /**
     * Start capturing into the destination buffer.
     * To swap buffers during capturing, call {@link #swap()}.
     * {@link #end()} shall be called after rendering to ping-pong buffer is done.
     * <p>
     * 开始捕获到目标缓冲区。
     * 若要在捕获期间交换缓冲区,请调用 {@link #swap()}。
     * 渲染到乒乓缓冲区完成后,应调用 {@link #end()}。
     */
    public void begin(){
        if(capturing){
            throw new IllegalStateException("Ping pong buffer is already in capturing state.");
        }

        capturing = true;
        bufDst.begin();
    }

    /**
     * Swaps source/target buffers.
     * May be called outside of capturing state.
     * <p>
     * 交换源/目标缓冲区。
     * 可以在非捕获状态下调用。
     */
    public void swap(){
        if(capturing){
            bufDst.end();
        }

        // Swap buffers
        // 交换缓冲区
        if(writeState){
            bufSrc = buffer1;
            bufDst = buffer2;
        }else{
            bufSrc = buffer2;
            bufDst = buffer1;
        }

        if(capturing){
            bufDst.begin();
        }

        writeState = !writeState;
    }

    /**
     * Finishes ping-ponging. Must be called after {@link #begin()}.
     * <p>
     * 结束乒乓切换。必须在 {@link #begin()} 之后调用。
     **/
    public void end(){
        if(!capturing){
            throw new IllegalStateException("Ping pong is not in capturing state. You should call begin() before calling end().");
        }
        bufDst.end();
        capturing = false;
    }

    /**
     * @return the source texture of the current ping-pong chain. 当前乒乓链的源纹理。
     */
    public Texture getSrcTexture(){
        return bufSrc.texture;
    }

    /**
     * @return the source buffer of the current ping-pong chain. 当前乒乓链的源缓冲区。
     */
    public FrameBuffer getSrcBuffer(){
        return bufSrc;
    }

    /**
     * @return the result's texture of the latest {@link #swap()}. 最近一次 {@link #swap()} 结果的纹理。
     */
    public Texture getDstTexture(){
        return bufDst.texture;
    }

    /**
     * @return Returns the result's buffer of the latest {@link #swap()}. 返回最近一次 {@link #swap()} 结果的缓冲区。
     */
    public FrameBuffer getDstBuffer(){
        return bufDst;
    }

    public void setTextureParams(TextureWrap u, TextureWrap v, TextureFilter min, TextureFilter mag){
        wrapU = u;
        wrapV = v;
        filterMin = min;
        filterMag = mag;
        rebind();
    }

    /**
     * Cleans up managed buffers with specified color.
     * 使用指定颜色清理受管理的缓冲区。
     */
    public void clear(Color clearColor){
        clear(clearColor.r, clearColor.g, clearColor.b, clearColor.a);
    }

    /**
     * Cleans up managed buffers with specified color.
     * 使用指定颜色清理受管理的缓冲区。
     */
    public void clear(float r, float g, float b, float a){
        final boolean wasCapturing = this.capturing;

        if(!wasCapturing){
            begin();
        }

        Gl.clearColor(r, g, b, a);
        Gl.clear(Gl.colorBufferBit);
        swap();
        Gl.clear(Gl.colorBufferBit);

        if(!wasCapturing){
            end();
        }
    }
}
