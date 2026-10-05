package arc.fx.util;

import arc.fx.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.util.viewport.*;

public class FxWidgetGroup extends WidgetGroup{
    private final FxProcessor fxProcessor;
    private boolean initialized = false;
    private boolean resizePending = false;
    private boolean matchWidgetSize = false;

    public FxWidgetGroup(){
        fxProcessor = new FxProcessor();
        super.setTransform(false);
    }

    public FxProcessor getFxProcessor(){
        return fxProcessor;
    }

    public boolean isMatchWidgetSize(){
        return matchWidgetSize;
    }

    /**
     * @param matchWidgetSize if true, the internal {@link FxProcessor} will be resized
     * to match {@link FxWidgetGroup}'s size (stage units and not screen pixels). 若为 true,内部的 {@link FxProcessor} 将被调整为与 {@link FxWidgetGroup} 的大小一致(使用舞台单位而非屏幕像素)。
     */
    public void setMatchWidgetSize(boolean matchWidgetSize){
        if(this.matchWidgetSize == matchWidgetSize) return;

        this.matchWidgetSize = matchWidgetSize;
        resizePending = true;
    }

    @Override
    protected void setScene(Scene stage){
        super.setScene(stage);

        if(stage != null){
            initialize();
        }else{
            reset();
        }
    }

    @Override
    protected void sizeChanged(){
        super.sizeChanged();
        resizePending = true;
    }

    @Override
    public void draw(){
        Draw.flush();

        performPendingResize();

        fxProcessor.clear();
        fxProcessor.begin();

        validate();
        drawChildren();

        Draw.flush();

        fxProcessor.end();
        fxProcessor.applyEffects();

        // If something was captured, render result to the screen.
        // 如果有已捕获的内容,则将结果渲染到屏幕上。
        if(fxProcessor.hasResult()){
            Color color = this.color;
            Draw.color(color.r, color.g, color.b, color.a * parentAlpha);
            Draw.rect(Draw.wrap(fxProcessor.getResultBuffer().texture), x + width / 2f, y + height / 2f, width, height);
        }
    }

    @Override
    protected void drawChildren(){
        boolean capturing = fxProcessor.isCapturing();

        if(capturing){
            // Imitate "transform" child drawing for when capturing into VfxManager.
            // 在捕获到 VfxManager 时,模拟"变换"子元素绘制。
            super.setTransform(true);
        }
        if(!capturing){
            // Clip children to VfxWidget area when not capturing into FBO.
            // 当未捕获到 FBO 时,将子元素裁剪到 VfxWidget 区域内。
            clipBegin();
        }

        super.drawChildren();
        Draw.flush();

        if(capturing){
            super.setTransform(false);
        }

        if(!capturing){
            clipEnd();
        }
    }

    @Override
    public void setCullingArea(Rect cullingArea){
        throw new UnsupportedOperationException("VfxWidgetGroup doesn't support culling area.");
    }

    @Override
    public void setTransform(boolean transform){
        throw new UnsupportedOperationException("VfxWidgetGroup doesn't support transform.");
    }

    private void initialize(){
        if(initialized) return;

        performPendingResize();

        resizePending = false;
        initialized = true;
    }

    private void reset(){
        if(!initialized) return;

        fxProcessor.dispose();

        resizePending = false;
        initialized = false;
    }

    private void performPendingResize(){
        if(!resizePending) return;

        final int width;
        final int height;

        // Size may be zero if the widget wasn't laid out yet.
        // 如果元素尚未布局,大小可能为零。
        if((int)getWidth() == 0 || (int)getHeight() == 0){
            // If the size of the widget is not defined,
            // 如果元素的大小未定义,
            // just resize to a small buffer to keep the memory footprint low.
            // 就只调整到一个较小的缓冲区,以保持较低的内存占用。
            width = 16;
            height = 16;

        }else if(matchWidgetSize){
            // Set buffer to match the size of the widget.
            // 将缓冲区设置为与元素大小一致。
            width = Mathf.floor(getWidth());
            height = Mathf.floor(getHeight());

        }else{
            // Set buffer to match the screen pixel density.
            // 将缓冲区设置为与屏幕像素密度一致。
            Viewport viewport = getScene().getViewport();
            float ppu = viewport.getScreenWidth() / viewport.getWorldWidth();
            width = Mathf.floor(getWidth() * ppu);
            height = Mathf.floor(getHeight() * ppu);
        }

        fxProcessor.resize(width, height);

        resizePending = false;
    }
}
