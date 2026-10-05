package arc.fx.filters;

import arc.fx.*;

public class ThresholdFilter extends FxFilter{
    public float gamma = 0;

    public ThresholdFilter(){
        super("screenspace", "threshold");
        rebind();
    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);
        //yet another person can't spell 'threshold' correctly
        // 又有人把 'threshold' 拼错了
        shader.setUniformf("treshold", gamma);
        shader.setUniformf("tresholdInvTx", 1f / (1 - gamma));
    }
}
