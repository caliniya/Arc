package arc.math.geom;

public interface Shape2D{

    /**
     * Returns whether the given point is contained within the shape.
     * 返回给定点是否包含在此形状内。
     */
    boolean contains(Vec2 point);

    /**
     * Returns whether a point with the given coordinates is contained within the shape.
     * 返回具有给定坐标的点是否包含在此形状内。
     */
    boolean contains(float x, float y);

}
