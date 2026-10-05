package arc.scene.actions;

import arc.graphics.Color;
import arc.scene.*;

/**
 * Sets the alpha for an actor's color (or a specified color), from the current alpha to the new alpha. Note this action
 * transitions from the alpha at the time the action starts to the specified alpha.
 * <p>
 * 设置元素颜色(或指定颜色)的 alpha,从当前 alpha 过渡到新 alpha。注意,此动作从动作开始时的 alpha 过渡到指定的 alpha。
 * @author Nathan Sweet
 */
public class AlphaAction extends TemporalAction{
    private float start, end;
    private Color color;

    @Override
    protected void begin(){
        if(color == null) color = target.color;
        start = color.a;
    }

    @Override
    protected void update(float percent){
        color.a = start + (end - start) * percent;
    }

    @Override
    public void reset(){
        super.reset();
        color = null;
    }

    public Color getColor(){
        return color;
    }

    /**
     * Sets the color to modify. If null (the default), the {@link #getActor() actor's} {@link Element#getColor() color} will be
     * used.
     * <p>
     * 设置要修改的颜色。若为 null(默认),则使用 {@link #getActor() 元素} 的 {@link Element#getColor() 颜色}。
     */
    public void setColor(Color color){
        this.color = color;
    }

    public float getAlpha(){
        return end;
    }

    public void setAlpha(float alpha){
        this.end = alpha;
    }
}
