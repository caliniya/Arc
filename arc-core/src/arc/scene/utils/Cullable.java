package arc.scene.utils;

import arc.math.geom.Rect;
import arc.scene.Group;

/**
 * Allows a parent to set the area that is visible on a child actor to allow the child to cull when drawing itself. This must only
 * be used for actors that are not rotated or scaled.
 * <p>
 * When Group is given a culling rectangle with {@link Group#setCullingArea(Rect)}, it will automatically call
 * {@link #setCullingArea(Rect)} on its children.
 * <p>
 * 允许父级设置子元素的可视区域,使子元素在绘制自身时可以进行剔除。此接口只能用于未旋转、未缩放的元素。 <p> 当 Group 通过 {@link Group#setCullingArea(Rect)} 获得剔除矩形时,会自动对其子元素调用 {@link #setCullingArea(Rect)}。
 * @author Nathan Sweet
 */
public interface Cullable{
    /** @param cullingArea The culling area in the child actor's coordinates. 以子元素坐标系表示的剔除区域。 */
    void setCullingArea(Rect cullingArea);
}
