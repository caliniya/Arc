package arc.fx;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;

/**
 * Base class for any single-pass filter.
 * <p>
 * 任意单通道(single-pass)滤镜的基类。
 */
public abstract class FxFilter implements Disposable{
    protected static final int u_texture0 = 0;
    protected static final int u_texture1 = 1;
    protected static final int u_texture2 = 2;
    protected static final int u_texture3 = 3;

    protected final Shader shader;

    protected Texture inputTexture = null;
    protected FrameBuffer outputBuffer = null;
    protected boolean disabled = false, autobind = false;

    public float time = 0f;

    public FxFilter(){
        this(null);
    }

    public FxFilter(String vert, String frag){
        this(compileShader(Core.files.internal("vfxshaders/" +vert+".vert"), Core.files.internal("vfxshaders/" +frag+".frag")));
    }

    public FxFilter(Shader shader){
        this.shader = shader;
    }

    public static Shader compileShader(Fi vertexFile, Fi fragmentFile){
        return compileShader(vertexFile, fragmentFile, "");
    }

    public static Shader compileShader(Fi vertexFile, Fi fragmentFile, String defines){
        return new Shader(defines + "\n" + vertexFile.readString(), defines + "\n" + fragmentFile.readString());
    }

    public FxFilter setInput(Texture input){
        this.inputTexture = input;
        return this;
    }

    public FxFilter setInput(FrameBuffer input){
        return setInput(input.texture);
    }

    public FxFilter setOutput(FrameBuffer output){
        this.outputBuffer = output;
        return this;
    }

    @Override
    public void dispose(){
        shader.dispose();
    }

    /**
     * This method should be called once filter will be added.
     * Also it must be called on every application resize as usual.
     * <p>
     * 滤镜被添加后应调用一次此方法。另外,通常在应用每次改变尺寸时也必须调用它。
     */
    public void resize(int width, int height){

    }

    public void rebind(){
        if(shader == null) return;

        shader.bind();
        setParams();
    }

    /**
     * Concrete objects shall be responsible to recreate or rebind its own resources whenever its needed, usually when the OpenGL
     * context is lost. Eg., framebuffer textures should be updated and shader parameters should be reuploaded/rebound.
     * <p>
     * 具体实现类应负责在需要时重建或重新绑定自身资源,通常是在 OpenGL 上下文丢失时。例如,应更新帧缓冲区纹理,并重新上传/重新绑定着色器参数。
     */
    protected void setParams(){
        if(shader != null){
            shader.setUniformi("u_texture0", u_texture0);
        }
    }

    /*
     * Sets the parameter to the specified value for this filter. This is for one-off operations since the shader is being bound
     * and unbound once per call: for a batch-ready version of this function see and use setParams instead.
     * 为该滤镜将参数设置为指定值。由于每次调用都要绑定并解绑一次着色器,这只适用于一次性操作:若需要可批量处理的版本,请参见并使用 setParams。
     */
    public void render(){
        boolean manualBufferBind = outputBuffer != null && !outputBuffer.isBound();
        if(manualBufferBind){
            outputBuffer.begin();
        }

        // Gives a chance to filters to perform needed operations just before the rendering operation takes place.
        // 让滤镜有机会在渲染操作开始之前执行所需的操作。
        onBeforeRender();

        shader.bind();
        if(autobind){
            setParams();
        }
        Draw.blit(shader);

        if(manualBufferBind){
            outputBuffer.end();
        }
    }

    /**
     * This method gets called just before rendering.
     * 该方法会在渲染开始前被调用。
     */
    protected void onBeforeRender(){
        inputTexture.bind(u_texture0);
    }

    /**
     * Concrete objects shall implements its own rendering, given the source and destination buffers.
     * 具体实现类应实现自己的渲染逻辑,源缓冲区和目标缓冲区由参数给定。
     */
    public void render(FrameBuffer src, FrameBuffer dst){
        setInput(src).setOutput(dst).render();
    }

    /**
     * Whether or not this effect is disabled and shouldn't be processed
     * 此特效是否已被禁用且不应被处理
     */
    public boolean isDisabled(){
        return disabled;
    }

    /**
     * Sets this effect disabled or not
     * 设置此特效是否被禁用
     */
    public void setDisabled(boolean enabled){
        this.disabled = enabled;
    }

    public void update(){
        time = Time.globalTime;
    }
}
