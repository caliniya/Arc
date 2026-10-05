package arc.util;

import arc.math.geom.Vec2;

/**
 * Various scaling types for fitting one rectangle into another.
 * <p>
 * 用于将一个矩形适配到另一个矩形的各种缩放类型。
 * @author Nathan Sweet
 */
public enum Scaling{
    /**
     * Scales the source to fit the target while keeping the same aspect ratio. This may cause the source to be smaller than the
     * target in one direction.
     * <p>
     * 缩放源矩形以适应目标矩形,同时保持宽高比。这可能导致源矩形在某一个方向上小于目标矩形。
     */
    fit,
    /**
     * Scales the source to fit the target if it is larger, otherwise does not scale.
     * <p>
     * 若源矩形较大,则缩放以适应目标矩形,否则不缩放。
     */
    bounded,
    /**
     * Scales the source to fill the target while keeping the same aspect ratio. This may cause the source to be larger than the
     * target in one direction.
     * <p>
     * 缩放源矩形以填满目标矩形,同时保持宽高比。这可能导致源矩形在某一个方向上大于目标矩形。
     */
    fill,
    /**
     * Scales the source to fill the target in the x direction while keeping the same aspect ratio. This may cause the source to be
     * smaller or larger than the target in the y direction.
     * <p>
     * 缩放源矩形以在 x 方向填满目标矩形,同时保持宽高比。这可能导致源矩形在 y 方向上大于或小于目标矩形。
     */
    fillX,
    /**
     * Scales the source to fill the target in the y direction while keeping the same aspect ratio. This may cause the source to be
     * smaller or larger than the target in the x direction.
     * <p>
     * 缩放源矩形以在 y 方向填满目标矩形,同时保持宽高比。这可能导致源矩形在 x 方向上大于或小于目标矩形。
     */
    fillY,
    /**
     * Scales the source to fill the target. This may cause the source to not keep the same aspect ratio.
     * 缩放源矩形以填满目标矩形。这可能导致源矩形不再保持宽高比。
     */
    stretch,
    /**
     * Scales the source to fill the target in the x direction, without changing the y direction. This may cause the source to not
     * keep the same aspect ratio.
     * <p>
     * 缩放源矩形以在 x 方向填满目标矩形,而不改变 y 方向。这可能导致源矩形不再保持宽高比。
     */
    stretchX,
    /**
     * Scales the source to fill the target in the y direction, without changing the x direction. This may cause the source to not
     * keep the same aspect ratio.
     * <p>
     * 缩放源矩形以在 y 方向填满目标矩形,而不改变 x 方向。这可能导致源矩形不再保持宽高比。
     */
    stretchY,
    /**
     * The source is not scaled.
     * 不缩放源矩形。
     */
    none;

    private static final Vec2 temp = new Vec2();

    /**
     * Returns the size of the source scaled to the target. Note the same Vec2 instance is always returned and should never be
     * cached.
     * <p>
     * 返回源矩形缩放到目标矩形后的尺寸。注意始终返回同一个 Vec2 实例,不应缓存它。
     */
    public Vec2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight){
        switch(this){
            case fit:{
                float targetRatio = targetHeight / targetWidth;
                float sourceRatio = sourceHeight / sourceWidth;
                float scale = targetRatio > sourceRatio ? targetWidth / sourceWidth : targetHeight / sourceHeight;
                temp.x = sourceWidth * scale;
                temp.y = sourceHeight * scale;
                break;
            }
            case fill:{
                float targetRatio = targetHeight / targetWidth;
                float sourceRatio = sourceHeight / sourceWidth;
                float scale = targetRatio < sourceRatio ? targetWidth / sourceWidth : targetHeight / sourceHeight;
                temp.x = sourceWidth * scale;
                temp.y = sourceHeight * scale;
                break;
            }
            case fillX:{
                float scale = targetWidth / sourceWidth;
                temp.x = sourceWidth * scale;
                temp.y = sourceHeight * scale;
                break;
            }
            case fillY:{
                float scale = targetHeight / sourceHeight;
                temp.x = sourceWidth * scale;
                temp.y = sourceHeight * scale;
                break;
            }
            case stretch:
                temp.x = targetWidth;
                temp.y = targetHeight;
                break;
            case stretchX:
                temp.x = targetWidth;
                temp.y = sourceHeight;
                break;
            case stretchY:
                temp.x = sourceWidth;
                temp.y = targetHeight;
                break;
            case bounded:
                if(sourceHeight > targetHeight || sourceWidth > targetWidth){
                    return fit.apply(sourceWidth, sourceHeight, targetWidth, targetHeight);
                }else{
                    return none.apply(sourceWidth, sourceHeight, targetWidth, targetHeight);
                }
            case none:
                temp.x = sourceWidth;
                temp.y = sourceHeight;
                break;
        }
        return temp;
    }
}
