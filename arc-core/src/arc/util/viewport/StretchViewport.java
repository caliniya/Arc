package arc.util.viewport;

import arc.graphics.Camera;
import arc.util.Scaling;

/**
 * A ScalingViewport that uses {@link Scaling#stretch} so it does not keep the aspect ratio, the world is scaled to take the whole
 * screen.
 * <p>
 * 一种使用 {@link Scaling#stretch} 的 ScalingViewport,不保持宽高比,世界被缩放至铺满整个屏幕。
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 */
public class StretchViewport extends ScalingViewport{
    /**
     * Creates a new viewport using a new {@link Camera}.
     * 使用新的 {@link Camera} 创建新视口。
     */
    public StretchViewport(float worldWidth, float worldHeight){
        super(Scaling.stretch, worldWidth, worldHeight);
    }

    public StretchViewport(float worldWidth, float worldHeight, Camera camera){
        super(Scaling.stretch, worldWidth, worldHeight, camera);
    }
}
