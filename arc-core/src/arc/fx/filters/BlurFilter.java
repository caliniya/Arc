package arc.fx.filters;

import arc.fx.*;
import arc.fx.util.*;
import arc.graphics.*;
import arc.graphics.gl.*;

public class BlurFilter extends FxFilter{
    public final GaussianBlurFilter blur;
    private final PingPongBuffer pingPongBuffer;
    private final CopyFilter copy;

    public Blending blending = Blending.disabled;

    // To keep track of the first render call.
    // 用于记录第一次渲染调用。
    private boolean firstRender = true;

    public BlurFilter(){
        this(8, GaussianBlurFilter.BlurType.gaussian5x5);
    }

    public BlurFilter(int blurPasses, GaussianBlurFilter.BlurType blurType){
        pingPongBuffer = new PingPongBuffer();

        copy = new CopyFilter();

        blur = new GaussianBlurFilter();
        blur.setPasses(blurPasses);
        blur.setType(blurType);
    }

    @Override
    public void dispose(){
        pingPongBuffer.dispose();
        blur.dispose();
        copy.dispose();
    }

    @Override
    public void resize(int width, int height){
        pingPongBuffer.resize(width, height);
        blur.resize(width, height);
        copy.resize(width, height);
    }

    @Override
    public void rebind(){
        pingPongBuffer.rebind();
        blur.setParams();
        copy.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dst){
        if(blur.getPasses() < 1){
            // Do not apply blur filter.
            // 不应用模糊滤镜。
            copy.setInput(src).setOutput(dst).render();
            return;
        }

        Gl.disable(Gl.blend);

        pingPongBuffer.begin();
        copy.setInput(src).setOutput(pingPongBuffer.getDstBuffer()).render();
        pingPongBuffer.swap();
        // Blur filter performs multiple passes of mixing ping-pong buffers and expects src and dst to have valid data.
        // 模糊滤镜会执行多次乒乓缓冲区混合,并要求 src 和 dst 中有有效数据。
        // So for the first run we just make both src and dst buffers identical.
        // 因此第一次运行时,我们只是让 src 和 dst 缓冲区的内容一致。
        if(firstRender){
            firstRender = false;
            copy.setInput(src).setOutput(pingPongBuffer.getDstBuffer()).render();
            pingPongBuffer.swap();
        }
        blur.render(pingPongBuffer);
        pingPongBuffer.end();

        if(blending != Blending.disabled){
            blending.apply();
        }

        copy.setInput(pingPongBuffer.getDstTexture())
        .setOutput(dst)
        .render();
    }

}
