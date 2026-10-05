package arc.util.viewport;

import arc.graphics.Camera;
import arc.math.geom.Vec2;
import arc.util.Scaling;

/**
 * A viewport that scales the world using {@link Scaling}.
 * <p>
 * {@link Scaling#fit} keeps the aspect ratio by scaling the world up to fit the screen, adding black bars (letterboxing) for the
 * remaining space.
 * <p>
 * {@link Scaling#fill} keeps the aspect ratio by scaling the world up to take the whole screen (some of the world may be off
 * screen).
 * <p>
 * {@link Scaling#stretch} does not keep the aspect ratio, the world is scaled to take the whole screen.
 * <p>
 * {@link Scaling#none} keeps the aspect ratio by using a fixed size world (the world may not fill the screen or some of the world
 * may be off screen).
 * <p>
 * 一种使用 {@link Scaling} 对世界进行缩放的视口。 <p> {@link Scaling#fit} 通过将世界放大至适应屏幕来保持宽高比,剩余空间以黑边(letterboxing)填充。 <p> {@link Scaling#fill} 通过将世界放大至铺满整个屏幕来保持宽高比(部分世界可能超出屏幕)。 <p> {@link Scaling#stretch} 不保持宽高比,世界被缩放至铺满整个屏幕。 <p> {@link Scaling#none} 使用固定大小的世界来保持宽高比(世界可能无法填满屏幕或部分世界超出屏幕)。
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 */
public class ScalingViewport extends Viewport{
    private Scaling scaling;

    /**
     * Creates a new viewport using a new {@link Camera}.
     * 使用新的 {@link Camera} 创建新视口。
     */
    public ScalingViewport(Scaling scaling, float worldWidth, float worldHeight){
        this(scaling, worldWidth, worldHeight, new Camera());
    }

    public ScalingViewport(Scaling scaling, float worldWidth, float worldHeight, Camera camera){
        this.scaling = scaling;
        setWorldSize(worldWidth, worldHeight);
        setCamera(camera);
    }

    @Override
    public void update(int screenWidth, int screenHeight, boolean centerCamera){
        Vec2 scaled = scaling.apply(getWorldWidth(), getWorldHeight(), screenWidth, screenHeight);
        int viewportWidth = Math.round(scaled.x);
        int viewportHeight = Math.round(scaled.y);

        // Center.
        // 居中。
        setScreenBounds((screenWidth - viewportWidth) / 2, (screenHeight - viewportHeight) / 2, viewportWidth, viewportHeight);

        apply(centerCamera);
    }

    public Scaling getScaling(){
        return scaling;
    }

    public void setScaling(Scaling scaling){
        this.scaling = scaling;
    }
}
