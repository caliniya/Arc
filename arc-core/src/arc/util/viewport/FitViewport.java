package arc.util.viewport;

import arc.graphics.Camera;
import arc.util.Scaling;

/**
 * A ScalingViewport that uses {@link Scaling#fit} so it keeps the aspect ratio by scaling the world up to fit the screen, adding
 * black bars (letterboxing) for the remaining space.
 * <p>
 * 一种使用 {@link Scaling#fit} 的 ScalingViewport,通过将世界放大至适应屏幕来保持宽高比,剩余空间以黑边(letterboxing)填充。
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 */
public class FitViewport extends ScalingViewport{
    /**
     * Creates a new viewport using a new {@link Camera}.
     * 使用新的 {@link Camera} 创建新视口。
     */
    public FitViewport(float worldWidth, float worldHeight){
        super(Scaling.fit, worldWidth, worldHeight);
    }

    public FitViewport(float worldWidth, float worldHeight, Camera camera){
        super(Scaling.fit, worldWidth, worldHeight, camera);
    }
}
