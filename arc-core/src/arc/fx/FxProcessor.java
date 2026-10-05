package arc.fx;

import arc.*;
import arc.fx.util.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.struct.*;
import arc.util.*;

/**
 * Provides a way to beginCapture the rendered scene to an off-screen buffer and to apply a chain of effects on it before rendering to
 * screen.
 * <p>
 * Effects can be added or removed via {@link #addEffect(FxFilter)} and {@link #removeEffect(FxFilter)}.
 * <p>
 * 提供一种方式,可将渲染出的场景开始捕获到屏幕外缓冲区中,并在渲染到屏幕之前对其应用一系列特效。
 * 特效可通过 {@link #addEffect(FxFilter)} 和 {@link #removeEffect(FxFilter)} 添加或移除。
 * @author bmanuel
 * @author metaphore
 */
public final class FxProcessor implements Disposable{
    private final ObjectIntMap<FxFilter> priorities = new ObjectIntMap<>();
    /**
     * All effects ever added.
     * 曾添加过的所有特效。
     */
    private final Ar<FxFilter> effectsAll = new Ar<>();
    /**
     * Maintains a per-frame updated list of enabled effects
     * 维护每帧更新的已启用特效列表
     */
    private final Ar<FxFilter> effectsEnabled = new Ar<>();

    /**
     * A mesh that is shared among basic filters to draw to full screen.
     * 由基础滤镜共享、用于全屏绘制的网格。
     */
    private final FxBufferRenderer bufferRenderer = new FxBufferRenderer();

    private final Format[] fboFormat;
    private final PingPongBuffer pingPongBuffer;

    private boolean disabled = false;
    private boolean capturing = false;
    private boolean hasCaptured = false;
    private boolean applyingEffects = false;

    private boolean blendingEnabled = false;

    private int width, height;

    public FxProcessor(){
        this(Core.graphics.getBackBufferWidth(), Core.graphics.getBackBufferHeight(), Format.defaultColor);
    }

    public FxProcessor(int w, int h){
        this(w, h, Format.defaultColor);
    }

    public FxProcessor(int bufferWidth, int bufferHeight, Format[] fboFormat){
        this.fboFormat = fboFormat;
        this.pingPongBuffer = new PingPongBuffer(bufferWidth, bufferHeight, fboFormat);
        this.width = bufferWidth;
        this.height = bufferHeight;
    }

    @Override
    public void dispose(){
        pingPongBuffer.dispose();
    }

    public void resize(int width, int height){
        if(this.width != width || this.height != height){
            this.width = width;
            this.height = height;

            pingPongBuffer.resize(width, height);

            for(FxFilter filter : effectsAll){
                filter.resize(width, height);
                filter.rebind();
            }
        }
    }

    public void rebind(){
        bufferRenderer.rebind();

        for(FxFilter filter : effectsAll){
            filter.rebind();
        }
    }

    public int getWidth(){
        return width;
    }

    public int getHeight(){
        return height;
    }

    public boolean isDisabled(){
        return disabled;
    }

    /**
     * Sets whether or not the post-processor should be disabled
     * 设置后处理器(post-processor)是否应被禁用
     */
    public void setDisabled(boolean disabled){
        this.disabled = disabled;
    }

    public boolean isBlendingEnabled(){
        return blendingEnabled;
    }

    /**
     * Enables OpenGL blending for the effect chain rendering stage.
     * Disabled by default.
     * <p>
     * 为特效链渲染阶段启用 OpenGL 混合。
     * 默认禁用。
     */
    public void setBlendingEnabled(boolean blendingEnabled){
        this.blendingEnabled = blendingEnabled;
    }

    /**
     * Returns the internal framebuffer format, computed from the parameters specified during construction. NOTE: the returned
     * Format will be valid after construction and NOT early!
     * <p>
     * 返回内部帧缓冲区的格式,由构造时指定的参数计算得出。注意:返回的格式要等构造完成后才有效,而不是更早!
     */
    public Format[] getFramebufferFormat(){
        return fboFormat;
    }

    public void setBufferTextureParams(TextureWrap u, TextureWrap v, TextureFilter min, TextureFilter mag){
        pingPongBuffer.setTextureParams(u, v, min, mag);
    }

    public void setTextureFilter(TextureFilter filter){
        setBufferTextureParams(TextureWrap.clampToEdge, TextureWrap.clampToEdge, filter, filter);
    }

    public boolean isCapturing(){
        return capturing;
    }

    public boolean isApplyingEffects(){
        return applyingEffects;
    }

    public boolean hasResult(){
        return hasCaptured;
    }

    /**
     * @return the last active destination buffer. 最后处于活动状态的目标缓冲区。
     */
    public FrameBuffer getResultBuffer(){
        return pingPongBuffer.getDstBuffer();
    }

    /**
     * @return the internal ping-pong buffer. 内部乒乓缓冲区。
     */
    public PingPongBuffer getPingPongBuffer(){
        return pingPongBuffer;
    }

    public boolean hasEnabledEffects(){
        return effectsAll.contains(fx -> !fx.isDisabled());
    }

    /**
     * Adds an effect to the effect chain and transfers ownership to the VfxManager.
     * The order of the inserted effects IS important, since effects will be applied in a FIFO fashion,
     * the first added is the first being applied.
     * <p>
     * For more control over the order supply the effect with a priority - {@link #addEffect(FxFilter, int)}.
     * <p>
     * 向特效链添加一个特效,并将所有权转移给 VfxManager。
     * 插入特效的顺序很重要,因为特效会以 FIFO(先进先出)的方式应用,先添加的先被应用。
     * 若需要更精确地控制顺序,可为特效提供优先级 - {@link #addEffect(FxFilter, int)}。
     * @see #addEffect(FxFilter, int)
     */
    public void addEffect(FxFilter effect){
        addEffect(effect, 0);
    }

    public void addEffect(FxFilter effect, int priority){
        effectsAll.add(effect);
        priorities.put(effect, priority);
        effectsAll.sort(e -> priorities.get(effect, 0));
        effect.resize(width, height);
        effect.rebind();
    }

    /**
     * Removes the specified effect from the effect chain.
     * <p>
     * 从特效链中移除指定的特效。
     */
    public void removeEffect(FxFilter effect){
        effectsAll.remove(effect);
    }

    /**
     * Removes all effects from the effect chain.
     * <p>
     * 移除特效链中的所有特效。
     */
    public void removeAllEffects(){
        effectsAll.clear();
    }

    /**
     * Changes the order of the effect in the effect chain.
     * <p>
     * 更改特效在特效链中的顺序。
     */
    public void setEffectPriority(FxFilter effect, int priority){
        priorities.put(effect, priority);
        effectsAll.sort(e -> priorities.get(effect, 0));
    }

    /**
     * Cleans up managed {@link PingPongBuffer}s' with {@link Color#clear}.
     * <p>
     * 使用 {@link Color#clear} 清理受管理的 {@link PingPongBuffer}。
     */
    public void clear(){
        clear(Color.clear);
    }

    /**
     * Cleans up managed {@link PingPongBuffer}s' with specified color.
     * <p>
     * 使用指定颜色清理受管理的 {@link PingPongBuffer}。
     */
    public void clear(Color color){
        if(capturing) throw new IllegalStateException("Cannot clean up buffers when capturing.");
        if(applyingEffects) throw new IllegalStateException("Cannot clean up buffers when applying effects.");

        pingPongBuffer.clear(color);
        hasCaptured = false;
    }

    /**
     * Starts capturing the scene.
     * <p>
     * 开始捕获场景。
     * @return true or false, whether or not capturing has been initiated. true 或 false,表示是否已开始捕获。
     * Capturing will fail if the manager is disabled or capturing is already started. 若管理器已禁用或捕获已经开始,捕获将失败。
     */
    public boolean begin(){
        if(applyingEffects){
            throw new IllegalStateException("You cannot capture when you're applying the effects.");
        }

        if(disabled) return false;
        if(capturing) return false;

        Draw.flush();

        capturing = true;
        pingPongBuffer.begin();
        return true;
    }

    /**
     * Stops capturing the scene.
     * <p>
     * 停止捕获场景。
     * @return false if there was no capturing before that call. 若调用前没有正在进行的捕获,则返回 false。
     */
    public boolean end(){
        if(!capturing) return false;

        Draw.flush();

        hasCaptured = true;
        capturing = false;
        pingPongBuffer.end();
        return true;
    }

    /**
     * Applies the effect chain, if there is one.
     * <p>
     * 应用特效链(如果存在)。
     */
    public void applyEffects(){
        if(capturing){
            throw new IllegalStateException("You should call VfxManager.endCapture() before applying the effects.");
        }

        if(disabled) return;
        if(!hasCaptured) return;

        effectsAll.each(FxFilter::update);

        Ar<FxFilter> effectChain = effectsEnabled.selectFrom(effectsAll, e -> !e.isDisabled());

        applyingEffects = true;
        int count = effectChain.size;
        if(count > 0){
            // Enable blending to preserve buffer's alpha values.
            // 启用混合以保留缓冲区的 alpha 值。
            if(blendingEnabled){
                Gl.enable(Gl.blend);
            }else{
                //disable otherwise
                // 否则禁用混合
                Gl.disable(Gl.blend);
            }

            Gl.disable(Gl.cullFace);
            Gl.disable(Gl.depthTest);

            // Render the effect chain.
            // 渲染特效链。
            pingPongBuffer.swap(); // Swap buffers to get captured result in src buffer.
            // 交换缓冲区,使捕获结果位于 src 缓冲区中。
            pingPongBuffer.begin();
            for(int i = 0; i < count; i++){
                FxFilter effect = effectChain.get(i);
                effect.render(pingPongBuffer.getSrcBuffer(), pingPongBuffer.getDstBuffer());
                if(i < count - 1){
                    pingPongBuffer.swap();
                }
            }
            pingPongBuffer.end();

            // Ensure default texture unit #0 is active.
            // 确保默认纹理单元 #0 处于活动状态。
            Gl.activeTexture(Gl.texture0); //TODO Do we need this?
            // TODO 我们需要这个吗?

            if(blendingEnabled){
                Gl.disable(Gl.blend);
            }
        }
        applyingEffects = false;
    }

    public void render(){
        if(capturing){
            throw new IllegalStateException("You should call VfxManager.endCapture() before rendering the result.");
        }
        if(disabled) return;
        if(!hasCaptured) return;

        // Enable blending to preserve buffer's alpha values.
        // 启用混合以保留缓冲区的 alpha 值。
        if(blendingEnabled){
            Gl.enable(Gl.blend);
        }else{
            Gl.disable(Gl.blend);
        }
        bufferRenderer.renderToScreen(pingPongBuffer.getDstBuffer());
        if(blendingEnabled){
            Gl.disable(Gl.blend);
        }
    }

    public void render(int x, int y, int width, int height){
        if(capturing){
            throw new IllegalStateException("You should call VfxManager.endCapture() before rendering the result.");
        }
        if(disabled) return;
        if(!hasCaptured) return;

        // Enable blending to preserve buffer's alpha values.
        // 启用混合以保留缓冲区的 alpha 值。
        if(blendingEnabled){
            Gl.enable(Gl.blend);
        }
        bufferRenderer.renderToScreen(pingPongBuffer.getDstBuffer(), x, y, width, height);
        if(blendingEnabled){
            Gl.disable(Gl.blend);
        }
    }

    public void render(FrameBuffer output){
        if(capturing){
            throw new IllegalStateException("You should call VfxManager.endCapture() before rendering the result.");
        }
        if(disabled) return;
        if(!hasCaptured) return;

        // Enable blending to preserve buffer's alpha values.
        // 启用混合以保留缓冲区的 alpha 值。
        if(blendingEnabled){
            Gl.enable(Gl.blend);
        }
        bufferRenderer.renderToFbo(pingPongBuffer.getDstBuffer(), output);
        if(blendingEnabled){
            Gl.disable(Gl.blend);
        }
    }
}