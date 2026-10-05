package arc.math.geom;

import arc.math.*;

/**
 * A convenient 2D circle class.
 * <p>
 * 便捷的 2D 圆类。
 * @author mzechner
 */
public class Circle implements Shape2D{
    public float x, y;
    public float radius;

    /**
     * Constructs a new circle with all values set to zero
     * 构造一个所有值均为零的新圆
     */
    public Circle(){

    }

    /**
     * Constructs a new circle with the given X and Y coordinates and the given radius.
     * <p>
     * 用给定的 X、Y 坐标和半径构造新圆。
     * @param x X coordinate X 坐标
     * @param y Y coordinate Y 坐标
     * @param radius The radius of the circle 圆的半径
     */
    public Circle(float x, float y, float radius){
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    /**
     * Constructs a new circle using a given {@link Vec2} that contains the desired X and Y coordinates, and a given radius.
     * <p>
     * 使用给定的 {@link Vec2}(包含所需的 X 和 Y 坐标)和给定的半径构造新圆。
     * @param position The position {@link Vec2}. 位置 {@link Vec2}。
     * @param radius The radius 半径
     */
    public Circle(Vec2 position, float radius){
        this.x = position.x;
        this.y = position.y;
        this.radius = radius;
    }

    /**
     * Copy constructor
     * <p>
     * 拷贝构造函数
     * @param circle The circle to construct a copy of. 用于构造副本的圆。
     */
    public Circle(Circle circle){
        this.x = circle.x;
        this.y = circle.y;
        this.radius = circle.radius;
    }

    /**
     * Creates a new {@link Circle} in terms of its center and a point on its edge.
     * <p>
     * 用圆心和圆边上的一点创建新的 {@link Circle}。
     * @param center The center of the new circle 新圆的圆心
     * @param edge Any point on the edge of the given circle 给定圆边缘上的任意一点
     */
    public Circle(Vec2 center, Vec2 edge){
        this.x = center.x;
        this.y = center.y;
        this.radius = Mathf.len(center.x - edge.x, center.y - edge.y);
    }

    /**
     * Sets a new location and radius for this circle.
     * <p>
     * 设置此圆的新位置和半径。
     * @param x X coordinate X 坐标
     * @param y Y coordinate Y 坐标
     * @param radius Circle radius 圆的半径
     */
    public Circle set(float x, float y, float radius){
        this.x = x;
        this.y = y;
        this.radius = radius;
        return this;
    }

    /**
     * Sets a new location and radius for this circle.
     * <p>
     * 设置此圆的新位置和半径。
     * @param position Position {@link Vec2} for this circle. 此圆的位置 {@link Vec2}。
     * @param radius Circle radius 圆的半径
     */
    public Circle set(Vec2 position, float radius){
        this.x = position.x;
        this.y = position.y;
        this.radius = radius;
        return this;
    }

    /**
     * Sets a new location and radius for this circle, based upon another circle.
     * <p>
     * 基于另一个圆设置此圆的新位置和半径。
     * @param circle The circle to copy the position and radius of. 要复制其位置和半径的圆。
     */
    public Circle set(Circle circle){
        this.x = circle.x;
        this.y = circle.y;
        this.radius = circle.radius;
        return this;
    }

    /**
     * Sets this {@link Circle}'s values in terms of its center and a point on its edge.
     * <p>
     * 用圆心和圆边上的一点设置此 {@link Circle} 的值。
     * @param center The new center of the circle 圆的新圆心
     * @param edge Any point on the edge of the given circle 给定圆边缘上的任意一点
     */
    public Circle set(Vec2 center, Vec2 edge){
        this.x = center.x;
        this.y = center.y;
        this.radius = Mathf.len(center.x - edge.x, center.y - edge.y);
        return this;
    }

    /**
     * Sets the x and y-coordinates of circle center from vector
     * <p>
     * 从向量设置圆心的 x 和 y 坐标
     * @param position The position vector 位置向量
     */
    public Circle setPosition(Vec2 position){
        this.x = position.x;
        this.y = position.y;
        return this;
    }

    /**
     * Sets the x and y-coordinates of circle center
     * <p>
     * 设置圆心的 x 和 y 坐标
     * @param x The x-coordinate x 坐标
     * @param y The y-coordinate y 坐标
     */
    public Circle setPosition(float x, float y){
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * Sets the x-coordinate of circle center
     * <p>
     * 设置圆心的 x 坐标
     * @param x The x-coordinate x 坐标
     */
    public void setX(float x){
        this.x = x;
    }

    /**
     * Sets the y-coordinate of circle center
     * <p>
     * 设置圆心的 y 坐标
     * @param y The y-coordinate y 坐标
     */
    public void setY(float y){
        this.y = y;
    }

    /**
     * Sets the radius of circle
     * <p>
     * 设置圆的半径
     * @param radius The radius 半径
     */
    public void setRadius(float radius){
        this.radius = radius;
    }

    /**
     * Checks whether or not this circle contains a given point.
     * <p>
     * 检查此圆是否包含给定点。
     * @param x X coordinate X 坐标
     * @param y Y coordinate Y 坐标
     * @return true if this circle contains the given point. 若此圆包含给定点则为 true。
     */
    public boolean contains(float x, float y){
        x = this.x - x;
        y = this.y - y;
        return x * x + y * y <= radius * radius;
    }

    /**
     * Checks whether or not this circle contains a given point.
     * <p>
     * 检查此圆是否包含给定点。
     * @param point The {@link Vec2} that contains the point coordinates. 包含点坐标的 {@link Vec2}。
     * @return true if this circle contains this point; false otherwise. 若此圆包含该点则为 true;否则为 false。
     */
    public boolean contains(Vec2 point){
        float dx = x - point.x;
        float dy = y - point.y;
        return dx * dx + dy * dy <= radius * radius;
    }

    /**
     * @param c the other {@link Circle} 另一个 {@link Circle}
     * @return whether this circle contains the other circle. 此圆是否包含另一个圆。
     */
    public boolean contains(Circle c){
        final float radiusDiff = radius - c.radius;
        if(radiusDiff < 0f) return false; // Can't contain bigger circle
        // 无法包含更大的圆
        final float dx = x - c.x;
        final float dy = y - c.y;
        final float dst = dx * dx + dy * dy;
        final float radiusSum = radius + c.radius;
        return (!(radiusDiff * radiusDiff < dst) && (dst < radiusSum * radiusSum));
    }

    /**
     * @param c the other {@link Circle} 另一个 {@link Circle}
     * @return whether this circle overlaps the other circle. 此圆是否与另一个圆重叠。
     */
    public boolean overlaps(Circle c){
        float dx = x - c.x;
        float dy = y - c.y;
        float distance = dx * dx + dy * dy;
        float radiusSum = radius + c.radius;
        return distance < radiusSum * radiusSum;
    }

    /**
     * Returns a {@link String} representation of this {@link Circle} of the form {@code x,y,radius}.
     * 以 {@code x,y,radius} 的形式返回此 {@link Circle} 的 {@link String} 表示。
     */
    @Override
    public String toString(){
        return x + "," + y + "," + radius;
    }

    /**
     * @return The circumference of this circle (as 2 * {@link Mathf#PI2}) * {@code radius}
     * 此圆的周长(2 * {@link Mathf#PI2}) * {@code radius}
     */
    public float circumference(){
        return this.radius * Mathf.PI2;
    }

    /**
     * @return The area of this circle (as {@link Mathf#PI} * radius * radius).
     * 此圆的面积({@link Mathf#PI} * radius * radius)。
     */
    public float area(){
        return this.radius * this.radius * Mathf.PI;
    }

    @Override
    public boolean equals(Object o){
        if(o == this) return true;
        if(o == null || o.getClass() != this.getClass()) return false;
        Circle c = (Circle)o;
        return this.x == c.x && this.y == c.y && this.radius == c.radius;
    }

    @Override
    public int hashCode(){
        final int prime = 41;
        int result = 1;
        result = prime * result + Float.floatToRawIntBits(radius);
        result = prime * result + Float.floatToRawIntBits(x);
        result = prime * result + Float.floatToRawIntBits(y);
        return result;
    }
}
