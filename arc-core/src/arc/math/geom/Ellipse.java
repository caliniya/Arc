package arc.math.geom;

import arc.math.*;

/**
 * A convenient 2D ellipse class, based on the circle class
 * <p>
 * 便捷的 2D 椭圆类,基于圆类实现
 * @author tonyp7
 */
public class Ellipse implements Shape2D{
    public float x, y;
    public float width, height;

    /**
     * Construct a new ellipse with all values set to zero
     * 构造一个所有值均为零的新椭圆
     */
    public Ellipse(){

    }

    /**
     * Copy constructor
     * <p>
     * 拷贝构造函数
     * @param ellipse Ellipse to construct a copy of. 用于构造副本的椭圆。
     */
    public Ellipse(Ellipse ellipse){
        this.x = ellipse.x;
        this.y = ellipse.y;
        this.width = ellipse.width;
        this.height = ellipse.height;
    }

    /**
     * Constructs a new ellipse
     * <p>
     * 构造一个新的椭圆
     * @param x X coordinate X 坐标
     * @param y Y coordinate Y 坐标
     * @param width the width of the ellipse 椭圆的宽
     * @param height the height of the ellipse 椭圆的高
     */
    public Ellipse(float x, float y, float width, float height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Costructs a new ellipse
     * <p>
     * 构造一个新的椭圆
     * @param position Position vector 位置向量
     * @param width the width of the ellipse 椭圆的宽
     * @param height the height of the ellipse 椭圆的高
     */
    public Ellipse(Vec2 position, float width, float height){
        this.x = position.x;
        this.y = position.y;
        this.width = width;
        this.height = height;
    }

    public Ellipse(Vec2 position, Vec2 size){
        this.x = position.x;
        this.y = position.y;
        this.width = size.x;
        this.height = size.y;
    }

    /**
     * Constructs a new {@link Ellipse} from the position and radius of a {@link Circle} (since circles are special cases of
     * ellipses).
     * <p>
     * 根据 {@link Circle} 的位置和半径构造新的 {@link Ellipse}(因为圆是椭圆的特例)。
     * @param circle The circle to take the values of 要取值的圆
     */
    public Ellipse(Circle circle){
        this.x = circle.x;
        this.y = circle.y;
        this.width = circle.radius * 2f;
        this.height = circle.radius * 2f;
    }

    /**
     * Checks whether or not this ellipse contains the given point.
     * <p>
     * 检查此椭圆是否包含给定点。
     * @param x X coordinate X 坐标
     * @param y Y coordinate Y 坐标
     * @return true if this ellipse contains the given point; false otherwise. 若此椭圆包含给定点则为 true;否则为 false。
     */
    public boolean contains(float x, float y){
        x = x - this.x;
        y = y - this.y;

        return (x * x) / (width * 0.5f * width * 0.5f) + (y * y) / (height * 0.5f * height * 0.5f) <= 1.0f;
    }

    /**
     * Checks whether or not this ellipse contains the given point.
     * <p>
     * 检查此椭圆是否包含给定点。
     * @param point Position vector 位置向量
     * @return true if this ellipse contains the given point; false otherwise. 若此椭圆包含给定点则为 true;否则为 false。
     */
    public boolean contains(Vec2 point){
        return contains(point.x, point.y);
    }

    /**
     * Sets a new position and size for this ellipse.
     * <p>
     * 设置此椭圆的新位置和大小。
     * @param x X coordinate X 坐标
     * @param y Y coordinate Y 坐标
     * @param width the width of the ellipse 椭圆的宽
     * @param height the height of the ellipse 椭圆的高
     */
    public void set(float x, float y, float width, float height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Sets a new position and size for this ellipse based upon another ellipse.
     * <p>
     * 基于另一个椭圆设置此椭圆的新位置和大小。
     * @param ellipse The ellipse to copy the position and size of. 要复制其位置和大小的椭圆。
     */
    public void set(Ellipse ellipse){
        x = ellipse.x;
        y = ellipse.y;
        width = ellipse.width;
        height = ellipse.height;
    }

    public void set(Circle circle){
        this.x = circle.x;
        this.y = circle.y;
        this.width = circle.radius * 2f;
        this.height = circle.radius * 2f;
    }

    public void set(Vec2 position, Vec2 size){
        this.x = position.x;
        this.y = position.y;
        this.width = size.x;
        this.height = size.y;
    }

    /**
     * Sets the x and y-coordinates of ellipse center from a {@link Vec2}.
     * <p>
     * 从 {@link Vec2} 设置椭圆中心的 x 和 y 坐标。
     * @param position The position vector 位置向量
     * @return this ellipse for chaining 此椭圆,用于链式调用
     */
    public Ellipse setPosition(Vec2 position){
        this.x = position.x;
        this.y = position.y;

        return this;
    }

    /**
     * Sets the x and y-coordinates of ellipse center
     * <p>
     * 设置椭圆中心的 x 和 y 坐标
     * @param x The x-coordinate x 坐标
     * @param y The y-coordinate y 坐标
     * @return this ellipse for chaining 此椭圆,用于链式调用
     */
    public Ellipse setPosition(float x, float y){
        this.x = x;
        this.y = y;

        return this;
    }

    /**
     * Sets the width and height of this ellipse
     * <p>
     * 设置此椭圆的宽和高
     * @param width The width 宽度
     * @param height The height 高度
     * @return this ellipse for chaining 此椭圆,用于链式调用
     */
    public Ellipse setSize(float width, float height){
        this.width = width;
        this.height = height;

        return this;
    }

    /**
     * @return The area of this {@link Ellipse} as {@link Mathf#PI} * {@link Ellipse#width} * {@link Ellipse#height}
     * 此 {@link Ellipse} 的面积({@link Mathf#PI} * {@link Ellipse#width} * {@link Ellipse#height})
     */
    public float area(){
        return Mathf.PI * (this.width * this.height) / 4;
    }

    /**
     * Approximates the circumference of this {@link Ellipse}. Oddly enough, the circumference of an ellipse is actually difficult
     * to compute exactly.
     * <p>
     * 近似计算此 {@link Ellipse} 的周长。奇怪的是,椭圆的周长其实很难精确计算。
     * @return The Ramanujan approximation to the circumference of an ellipse if one dimension is at least three times longer than 当一维至少比另一维长三倍时,椭圆周长的拉马努金近似
     * the other, else the simpler approximation
     */
    public float circumference(){
        float a = this.width / 2;
        float b = this.height / 2;
        if(a * 3 > b || b * 3 > a){
            // If one dimension is three times as long as the other...
            // 如果一维是另一维的三倍长……
            return (float)(Mathf.PI * ((3 * (a + b)) - Math.sqrt((3 * a + b) * (a + 3 * b))));
        }else{
            // We can use the simpler approximation, then
            // 这样就可以使用更简单的近似了
            return (float)(Mathf.PI2 * Math.sqrt((a * a + b * b) / 2));
        }
    }

    @Override
    public boolean equals(Object o){
        if(o == this) return true;
        if(o == null || o.getClass() != this.getClass()) return false;
        Ellipse e = (Ellipse)o;
        return this.x == e.x && this.y == e.y && this.width == e.width && this.height == e.height;
    }

    @Override
    public int hashCode(){
        final int prime = 53;
        int result = 1;
        result = prime * result + Float.floatToRawIntBits(this.height);
        result = prime * result + Float.floatToRawIntBits(this.width);
        result = prime * result + Float.floatToRawIntBits(this.x);
        result = prime * result + Float.floatToRawIntBits(this.y);
        return result;
    }
}
