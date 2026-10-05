package arc.util.viewport;

import arc.graphics.Camera;
import arc.math.geom.Vec2;
import arc.util.Scaling;

/**
 * A viewport that keeps the world aspect ratio by extending the world in one direction. The world is first scaled to fit within
 * the viewport, then the shorter dimension is lengthened to fill the viewport. A maximum size can be specified to limit how much
 * the world is extended and black bars (letterboxing) are used for any remaining space.
 * <p>
 * 一种通过在某个方向上扩展世界来保持世界宽高比的视口。世界先被缩放到视口内,然后较短的维度被拉长以填满视口。可指定最大尺寸来限制世界的扩展幅度,剩余空间则以黑边(letterboxing)填充。
 * @author Nathan Sweet
 */
public class ExtendViewport extends Viewport{
    private float minWorldWidth, minWorldHeight;
    private float maxWorldWidth, maxWorldHeight;

    /**
     * Creates a new viewport using a new {@link Camera} with no maximum world size.
     * 使用新的 {@link Camera} 且无最大世界尺寸创建新视口。
     */
    public ExtendViewport(float minWorldWidth, float minWorldHeight){
        this(minWorldWidth, minWorldHeight, 0, 0, new Camera());
    }

    /**
     * Creates a new viewport with no maximum world size.
     * 以无最大世界尺寸创建新视口。
     */
    public ExtendViewport(float minWorldWidth, float minWorldHeight, Camera camera){
        this(minWorldWidth, minWorldHeight, 0, 0, camera);
    }

    /**
     * Creates a new viewport using a new {@link Camera} and a maximum world size.
     * <p>
     * 使用新的 {@link Camera} 和最大世界尺寸创建新视口。
     * @see ExtendViewport#ExtendViewport(float, float, float, float, Camera)
     */
    public ExtendViewport(float minWorldWidth, float minWorldHeight, float maxWorldWidth, float maxWorldHeight){
        this(minWorldWidth, minWorldHeight, maxWorldWidth, maxWorldHeight, new Camera());
    }

    /**
     * Creates a new viewport with a maximum world size.
     * <p>
     * 以最大世界尺寸创建新视口。
     * @param maxWorldWidth User 0 for no maximum width. 最大宽度传 0 表示不限制。
     * @param maxWorldHeight User 0 for no maximum height. 最大高度传 0 表示不限制。
     */
    public ExtendViewport(float minWorldWidth, float minWorldHeight, float maxWorldWidth, float maxWorldHeight, Camera camera){
        this.minWorldWidth = minWorldWidth;
        this.minWorldHeight = minWorldHeight;
        this.maxWorldWidth = maxWorldWidth;
        this.maxWorldHeight = maxWorldHeight;
        setCamera(camera);
    }

    @Override
    public void update(int screenWidth, int screenHeight, boolean centerCamera){
        // Fit min size to the screen.
        // 将最小尺寸适配到屏幕。
        float worldWidth = minWorldWidth;
        float worldHeight = minWorldHeight;
        Vec2 scaled = Scaling.fit.apply(worldWidth, worldHeight, screenWidth, screenHeight);

        // Extend in the short direction.
        // 沿较短方向扩展。
        int viewportWidth = Math.round(scaled.x);
        int viewportHeight = Math.round(scaled.y);
        if(viewportWidth < screenWidth){
            float toViewportSpace = viewportHeight / worldHeight;
            float toWorldSpace = worldHeight / viewportHeight;
            float lengthen = (screenWidth - viewportWidth) * toWorldSpace;
            if(maxWorldWidth > 0) lengthen = Math.min(lengthen, maxWorldWidth - minWorldWidth);
            worldWidth += lengthen;
            viewportWidth += Math.round(lengthen * toViewportSpace);
        }else if(viewportHeight < screenHeight){
            float toViewportSpace = viewportWidth / worldWidth;
            float toWorldSpace = worldWidth / viewportWidth;
            float lengthen = (screenHeight - viewportHeight) * toWorldSpace;
            if(maxWorldHeight > 0) lengthen = Math.min(lengthen, maxWorldHeight - minWorldHeight);
            worldHeight += lengthen;
            viewportHeight += Math.round(lengthen * toViewportSpace);
        }

        setWorldSize(worldWidth, worldHeight);

        // Center.
        // 居中。
        setScreenBounds((screenWidth - viewportWidth) / 2, (screenHeight - viewportHeight) / 2, viewportWidth, viewportHeight);

        apply(centerCamera);
    }

    public float getMinWorldWidth(){
        return minWorldWidth;
    }

    public void setMinWorldWidth(float minWorldWidth){
        this.minWorldWidth = minWorldWidth;
    }

    public float getMinWorldHeight(){
        return minWorldHeight;
    }

    public void setMinWorldHeight(float minWorldHeight){
        this.minWorldHeight = minWorldHeight;
    }

    public float getMaxWorldWidth(){
        return maxWorldWidth;
    }

    public void setMaxWorldWidth(float maxWorldWidth){
        this.maxWorldWidth = maxWorldWidth;
    }

    public float getMaxWorldHeight(){
        return maxWorldHeight;
    }

    public void setMaxWorldHeight(float maxWorldHeight){
        this.maxWorldHeight = maxWorldHeight;
    }
}
