package arc.scene.style;

/**
 * A drawable that supports scale and rotation.
 * 支持缩放和旋转的可绘制对象。
 */
public interface TransformDrawable extends Drawable{
    @Override
    void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation);
}
