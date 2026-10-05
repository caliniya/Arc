package arc.util.viewport;

import arc.graphics.Camera;
import arc.util.Scaling;

/**
 * A ScalingViewport that uses {@link Scaling#fill} so it keeps the aspect ratio by scaling the world up to take the whole screen
 * (some of the world may be off screen).
 * <p>
 * 一种使用 {@link Scaling#fill} 的 ScalingViewport,通过将世界放大至铺满整个屏幕来保持宽高比(部分世界可能超出屏幕)。
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 */
public class FillViewport extends ScalingViewport{
    /**
     * Creates a new viewport using a new {@link Camera}.
     * 使用新的 {@link Camera} 创建新视口。
     */
    public FillViewport(float worldWidth, float worldHeight){
        super(Scaling.fill, worldWidth, worldHeight);
    }

    public FillViewport(float worldWidth, float worldHeight, Camera camera){
        super(Scaling.fill, worldWidth, worldHeight, camera);
    }
}
