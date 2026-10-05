package arc.graphics;

import arc.graphics.gl.*;

/**
 * Blending modes, can be instantiated to make custom blending.
 * 混合模式,可实例化以创建自定义混合。
 */
public class Blending{
    public static final Blending

    normal = new Blending(Gl.srcAlpha, Gl.oneMinusSrcAlpha, Gl.one, Gl.oneMinusSrcAlpha),
    additive = new Blending(Gl.srcAlpha, Gl.one, Gl.one, Gl.oneMinusSrcAlpha),
    disabled = new Blending(Gl.srcAlpha, Gl.oneMinusSrcAlpha, Gl.one, Gl.oneMinusSrcAlpha){
        @Override
        public void apply(){
            Gl.disable(Gl.blend);
        }
    };

    public final int src, dst, srcAlpha, dstAlpha;

    public Blending(int src, int dst){
        this.src = src;
        this.dst = dst;
        this.srcAlpha = src;
        this.dstAlpha = dst;
    }

    public Blending(int src, int dst, int srcAlpha, int dstAlpha){
        this.src = src;
        this.dst = dst;
        this.srcAlpha = srcAlpha;
        this.dstAlpha = dstAlpha;
    }

    /**
     * Enables/disables blending and sets the correct GL blend function.
     * 启用/禁用混合并设置正确的 GL 混合函数。
     */
    public void apply(){
        Gl.enable(Gl.blend);
        Gl.blendFuncSeparate(src, dst, srcAlpha, dstAlpha);
    }
}
