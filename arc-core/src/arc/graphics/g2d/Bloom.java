package arc.graphics.g2d;

import arc.*;
import arc.graphics.*;
import arc.graphics.gl.*;

/**
 * Requires bloom shaders in 'bloomshaders' folder.
 * <p>
 * 需要 bloomshaders 文件夹中的泛光着色器。
 * @author kalle_h
 * @author Anuke
 */
public class Bloom{
    public int blurPasses = 1;
    public boolean blending = false;

    private Shader thresholdShader, bloomShader, blurShader;
    private FrameBuffer buffer, pingPong1, pingPong2;

    private float bloomIntensity, originalIntensity, threshold;
    private boolean capturing = false;
    private float r, g, b, a;

    /**
     * Rebinds the context. Necessary on Android/IOS. TODO or is it?
     * 重新绑定上下文。在 Android/IOS 上必需。TODO 真的吗?
     */
    public void resume(){
        bloomShader.bind();
        bloomShader.setUniformi("u_texture1", 1);

        setSize(pingPong1.width, pingPong1.height);
        setThreshold(threshold);
        setBloomIntensity(bloomIntensity);
        setOriginalIntensity(originalIntensity);
    }

    /**
     * Creates a bloom instance with no blending, no depth and 1/4 the screen size.
     * 创建无混合、无深度、1/4 屏幕尺寸的泛光实例。
     */
    public Bloom(){
        init(Core.graphics.getWidth() / 4, Core.graphics.getHeight() / 4, false, false);
    }

    public Bloom(boolean useBlending){
        init(Core.graphics.getWidth() / 4, Core.graphics.getHeight() / 4, false, useBlending);
    }

    /**
     * Initializes bloom class that encapsulates original scene capturate, thresholding, gaussian blurring and blending.
     * <p>
     * 初始化泛光类,封装原始场景捕获、阈值处理、高斯模糊和混合。
     * @param hasDepth Enables depth buffer. 启用深度缓冲区。
     * @param useBlending Enables alpha blending, allowing combining background graphics and only doing blooming on certain objects. 启用 alpha 混合,允许合并背景图形,仅对特定对象做泛光。
     */
    public Bloom(int width, int height, boolean hasDepth, boolean useBlending){
        init(width, height, hasDepth, useBlending);
    }

    public void resize(int width, int height){
        resize(width, height, 4);
    }

    public void resize(int width, int height, int scaling){
        boolean changed = (pingPong1.width != width / scaling || pingPong1.height != height / scaling);

        if(changed){
            pingPong1.resize(width / scaling, height / scaling);
            pingPong2.resize(width / scaling, height / scaling);
            buffer.resize(width, height);
            setSize(width / scaling, height / scaling);
        }
    }

    private void init(int width, int height, boolean hasDepth, boolean useBlending){
        blending = useBlending;

        buffer = new FrameBuffer(Core.graphics.getWidth(), Core.graphics.getHeight(), hasDepth ? Format.defaultColorDepth : Format.defaultColor);
        pingPong1 = new FrameBuffer(width, height);
        pingPong2 = new FrameBuffer(width, height);

        final String alpha = useBlending ? "alpha_" : "";

        bloomShader = createShader("screenspace", alpha + "bloom");
        thresholdShader = createShader("screenspace", alpha + "threshold");

        blurShader = createShader("blurspace", alpha + "gaussian");

        setSize(width, height);
        setBloomIntensity(2.5f);
        setOriginalIntensity(1f);
        setThreshold(0.5f);

        bloomShader.bind();
        bloomShader.setUniformi("u_texture1", 1);
    }

    /**
     * Set clearing color for capturing buffer.
     * 设置捕获缓冲区的清屏颜色。
     */
    public void setClearColor(float r, float g, float b, float a){
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    /**
     * Call this before rendering scene.
     * 渲染场景前调用。
     */
    public void capture(){
        if(!capturing){
            capturing = true;
            buffer.begin();
            Gl.clearColor(r, g, b, a);
            Gl.clear(Gl.colorBufferBit | Gl.depthBufferBit);
        }
    }

    /**
     * Pause capturing to the buffer.
     * 暂停捕获到缓冲区。
     */
    public void capturePause(){
        if(capturing){
            capturing = false;
            buffer.end();
        }
    }

    /**
     * Start capturing again after pause, no clearing is done to the buffer.
     * 暂停后重新开始捕获,不清空缓冲区。
     */
    public void captureContinue(){
        if(!capturing){
            capturing = true;
            buffer.begin();
        }
    }

    /**
     * Renders the bloomed scene.
     * 渲染泛光后的场景。
     */
    public void render(){
        if(capturing){
            capturing = false;
            buffer.end();
        }

        Gl.disable(Gl.blend);
        Gl.disable(Gl.depthTest);
        Gl.depthMask(false);

        //cut bright areas of the picture and blit to smaller fbo
        // 截取画面中较亮的区域并位块传输到较小的 fbo

        pingPong1.begin();
        buffer.blit(thresholdShader);
        pingPong1.end();

        //blur
        // 模糊
        for(int i = 0; i < blurPasses; i++){
            // horizontal
            // 水平
            pingPong2.begin();
            blurShader.bind();
            blurShader.setUniformf("dir", 1f, 0f);
            pingPong1.blit(blurShader);
            pingPong2.end();

            // vertical
            // 垂直
            pingPong1.begin();
            blurShader.bind();
            blurShader.setUniformf("dir", 0f, 1f);
            pingPong2.blit(blurShader);
            pingPong1.end();
        }

        if(blending){
            Gl.enable(Gl.blend);
            Gl.blendFunc(Gl.srcAlpha, Gl.oneMinusSrcAlpha);
        }

        pingPong1.texture.bind(1);
        buffer.blit(bloomShader);
    }

    /**
     * Set intensity for bloom. Higher means more brightening for spots that are over threshold.
     * <p>
     * 设置泛光强度。越高则超过阈值的区域越亮。
     * @param intensity Multiplier for blurred texture in combining phase. Must be positive. 合成阶段模糊纹理的乘数。必须为正。
     */
    public void setBloomIntensity(float intensity){
        bloomIntensity = intensity;
        bloomShader.bind();
        bloomShader.setUniformf("BloomIntensity", intensity);
    }

    /**
     * Set intensity for original scene. Under 1 means darkening and over 1 means lightening.
     * <p>
     * 设置原始场景强度。小于 1 变暗,大于 1 变亮。
     * @param intensity Multiplier for captured texture in combining phase. Must be positive. 合成阶段捕获纹理的乘数。必须为正。
     */
    public void setOriginalIntensity(float intensity){
        originalIntensity = intensity;
        bloomShader.bind();
        bloomShader.setUniformf("OriginalIntensity", intensity);
    }

    /**
     * Threshold for bright parts. Everything under threshold is set to 0.
     * <p>
     * 明亮部分的阈值。低于阈值的内容置为 0。
     * @param threshold Must be in range [0..1]. 必须在 [0..1] 范围内。
     */
    public void setThreshold(float threshold){
        this.threshold = threshold;
        thresholdShader.bind();
        thresholdShader.setUniformf("threshold", threshold, 1f / (1 - threshold));
    }

    private void setSize(int width, int height){
        blurShader.bind();
        blurShader.setUniformf("size", width, height);
    }

    /**
     * @return The unprocessed frame buffer this bloom captures. Advanced uses only.
     * @return The unprocessed frame buffer this bloom captures. Advanced uses only. 此泛光捕获的未处理帧缓冲区。仅供高级用法。
     */
    public FrameBuffer buffer(){
        return buffer;
    }

    /**
     * Disposes all resources.
     * 释放所有资源。
     */
    public void dispose(){
        try{
            buffer.dispose();
            pingPong1.dispose();
            pingPong2.dispose();

            blurShader.dispose();
            bloomShader.dispose();
            thresholdShader.dispose();
        }catch(Throwable ignored){}
    }

    private static Shader createShader(String vertexName, String fragmentName){
        return new Shader(Core.files.internal("bloomshaders/" + vertexName + ".vert"), Core.files.internal("bloomshaders/" + fragmentName + ".frag"));
    }
}
