package arc.fx.filters;

import arc.*;
import arc.fx.*;
import arc.fx.util.*;
import arc.graphics.Blending;
import arc.graphics.*;
import arc.graphics.gl.*;

public class BloomFilter extends FxFilter{
    public final PingPongBuffer buffer;

    public final GaussianBlurFilter blur;
    public final ThresholdFilter threshold;
    public final CombineFilter combine;

    public Blending blending = Blending.normal;
    public int scaling = 4;

    public BloomFilter(){
        buffer = new PingPongBuffer();

        blur = new GaussianBlurFilter();
        threshold = new ThresholdFilter();
        combine = new CombineFilter();

        blur.setPasses(2);
        blur.setAmount(10f);
        threshold.gamma = 0f;
        combine.src1int = 1f;
        combine.src1sat = 1f;
        combine.src2int = 2f;
        combine.src2sat = 2f;
        rebind();
    }

    @Override
    public void rebind(){
        threshold.rebind();
        combine.rebind();
        buffer.rebind();
    }

    @Override
    public void resize(int width, int height){
        width /= scaling;
        height /= scaling;

        buffer.resize(width, height);
        blur.resize(width, height);
        threshold.resize(width, height);
        combine.resize(width, height);
    }

    @Override
    public void dispose(){
        combine.dispose();
        threshold.dispose();
        blur.dispose();
        buffer.dispose();
    }

    @Override
    public void render(final FrameBuffer src, final FrameBuffer dst){
        Texture texSrc = src.texture;

        Gl.disable(Gl.blend);

        buffer.begin();
        Core.graphics.clear(Color.clear);

        // Threshold / high-pass filter
        // 阈值 / 高通滤波器
        // Only areas with pixels >= threshold are blit to smaller FBO
        // 只有像素 >= 阈值的区域才会被 blit 到更小的 FBO 中
        threshold.setInput(texSrc).setOutput(buffer.getDstBuffer()).render();
        buffer.swap();

        // Blur pass
        // 模糊处理
        blur.render(buffer);

        buffer.end();

        if(blending != Blending.disabled){
            blending.apply();
        }

        // Mix original scene and blurred threshold, modulate via set(Base|BloomEffect)(Saturation|Intensity)
        // 混合原始场景与模糊后的阈值结果,通过 set(Base|BloomEffect)(Saturation|Intensity) 进行调节
        combine.setInput(texSrc, buffer.getDstTexture())
        .setOutput(dst)
        .render();
    }
}
