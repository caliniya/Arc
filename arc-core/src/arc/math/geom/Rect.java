package arc.math.geom;

import arc.util.*;

/**
 * Encapsulates a 2D rectangle defined by its corner point in the bottom left and its extents in x (width) and y (height).
 * <p>
 * 封装由左下角点及 x(宽)、y(高)方向尺寸定义的 2D 矩形。
 * @author badlogicgames@gmail.com
 */
public class Rect implements Shape2D{
    /**
     * Static temporary rectangle. Use with care! Use only when sure other code will not also use this.
     * 静态临时矩形。小心使用!只在确定其他代码不会同时使用时才可使用。
     */
    public static final Rect tmp = new Rect();

    /**
     * Static temporary rectangle. Use with care! Use only when sure other code will not also use this.
     * 静态临时矩形。小心使用!只在确定其他代码不会同时使用时才可使用。
     */
    public static final Rect tmp2 = new Rect();

    public float x, y;
    public float width, height;

    /**
     * Constructs a new rectangle with all values set to zero
     * 构造一个所有值均为零的新矩形
     */
    public Rect(){

    }

    /**
     * Constructs a new rectangle with the given corner point in the bottom left and dimensions.
     * <p>
     * 用给定的左下角点和尺寸构造新矩形。
     * @param x The corner point x-coordinate 角点的 x 坐标
     * @param y The corner point y-coordinate 角点的 y 坐标
     * @param width The width 宽度
     * @param height The height 高度
     */
    public Rect(float x, float y, float width, float height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Constructs a rectangle based on the given rectangle
     * <p>
     * 根据给定的矩形构造矩形
     * @param rect The rectangle 矩形
     */
    public Rect(Rect rect){
        x = rect.x;
        y = rect.y;
        width = rect.width;
        height = rect.height;
    }

    public Rect setCentered(float x, float y, float size){
        return set(x - size/2f, y - size/2f, size, size);
    }

    public Rect setCentered(float x, float y, float width, float height){
        return set(x - width/2f, y - height/2f, width, height);
    }

    /**
     * @param x bottom-left x coordinate 左下角 x 坐标
     * @param y bottom-left y coordinate 左下角 y 坐标
     * @param width width 宽度
     * @param height height 高度
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect set(float x, float y, float width, float height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        return this;
    }

    /**
     * @return the x-coordinate of the bottom left corner
     * 左下角的 x 坐标
     */
    public float getX(){
        return x;
    }

    /**
     * Sets the x-coordinate of the bottom left corner
     * <p>
     * 设置左下角的 x 坐标
     * @param x The x-coordinate x 坐标
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect setX(float x){
        this.x = x;

        return this;
    }

    /**
     * @return the y-coordinate of the bottom left corner
     * 左下角的 y 坐标
     */
    public float getY(){
        return y;
    }

    /**
     * Sets the y-coordinate of the bottom left corner
     * <p>
     * 设置左下角的 y 坐标
     * @param y The y-coordinate y 坐标
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect setY(float y){
        this.y = y;

        return this;
    }

    /**
     * @return the width
     * 宽度
     */
    public float getWidth(){
        return width;
    }

    /**
     * Sets the width of this rectangle
     * <p>
     * 设置此矩形的宽度
     * @param width The width 宽度
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect setWidth(float width){
        this.width = width;

        return this;
    }

    /**
     * @return the height
     * 高度
     */
    public float getHeight(){
        return height;
    }

    /**
     * Sets the height of this rectangle
     * <p>
     * 设置此矩形的高度
     * @param height The height 高度
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect setHeight(float height){
        this.height = height;

        return this;
    }

    /**
     * return the Vec2 with coordinates of this rectangle
     * <p>
     * 返回包含此矩形坐标的 Vec2
     * @param position The Vec2 Vec2
     */
    public Vec2 getPosition(Vec2 position){
        return position.set(x, y);
    }

    /**
     * Sets the x and y-coordinates of the bottom left corner from vector
     * <p>
     * 从向量设置左下角的 x 和 y 坐标
     * @param position The position vector 位置向量
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect setPosition(Vec2 position){
        this.x = position.x;
        this.y = position.y;

        return this;
    }

    /**
     * Sets the x and y-coordinates of the bottom left corner
     * <p>
     * 设置左下角的 x 和 y 坐标
     * @param x The x-coordinate x 坐标
     * @param y The y-coordinate y 坐标
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect setPosition(float x, float y){
        this.x = x;
        this.y = y;

        return this;
    }

    public Rect move(float cx, float cy){
        x += cx;
        y += cy;
        return this;
    }

    /**
     * Sets the width and height of this rectangle
     * <p>
     * 设置此矩形的宽和高
     * @param width The width 宽度
     * @param height The height 高度
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect setSize(float width, float height){
        this.width = width;
        this.height = height;

        return this;
    }

    /**
     * Sets the squared size of this rectangle
     * <p>
     * 设置此矩形尺寸的平方
     * @param sizeXY The size 尺寸
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect setSize(float sizeXY){
        this.width = sizeXY;
        this.height = sizeXY;

        return this;
    }

    /**
     * @param size The Vec2 Vec2
     * @return the Vec2 with size of this rectangle 具有此矩形大小的 Vec2
     */
    public Vec2 getSize(Vec2 size){
        return size.set(width, height);
    }

    public static boolean contains(float x, float y, float width, float height, float px, float py){
        return x <= px && x + width >= px && y <= py && y + height >= py;
    }

    public static boolean contains(float x, float y, float width, float height, float rx, float ry, float rwidth, float rheight){
        float xmax = rx + rwidth;

        float ymax = ry + rheight;

        return ((rx > x && rx < x + width) && (xmax > x && xmax < x + width))
        && ((ry > y && ry < y + height) && (ymax > y && ymax < y + height));
    }

    /**
     * @param x point x coordinate 点的 x 坐标
     * @param y point y coordinate 点的 y 坐标
     * @return whether the point is contained in the rectangle 点是否包含在矩形内
     */
    public boolean contains(float x, float y){
        return this.x <= x && this.x + this.width >= x && this.y <= y && this.y + this.height >= y;
    }

    /**
     * @param point The coordinates vector 坐标向量
     * @return whether the point is contained in the rectangle 点是否包含在矩形内
     */
    public boolean contains(Vec2 point){
        return contains(point.x, point.y);
    }

    /**
     * @param circle the circle 圆
     * @return whether the circle is contained in the rectangle 圆是否包含在矩形内
     */
    public boolean contains(Circle circle){
        return (circle.x - circle.radius >= x) && (circle.x + circle.radius <= x + width)
        && (circle.y - circle.radius >= y) && (circle.y + circle.radius <= y + height);
    }

    /**
     * @param rect the other {@link Rect}. 另一个 {@link Rect}。
     * @return whether the other rectangle is contained in this rectangle. 另一矩形是否包含在此矩形内。
     */
    public boolean contains(Rect rect){
        float xmin = rect.x;
        float xmax = xmin + rect.width;

        float ymin = rect.y;
        float ymax = ymin + rect.height;

        return ((xmin > x && xmin < x + width) && (xmax > x && xmax < x + width))
        && ((ymin > y && ymin < y + height) && (ymax > y && ymax < y + height));
    }

    /**
     * @param r the other {@link Rect} 另一个 {@link Rect}
     * @return whether this rectangle overlaps the other rectangle. 此矩形是否与另一矩形重叠。
     */
    public boolean overlaps(Rect r){
        return x < r.x + r.width && x + width > r.x && y < r.y + r.height && y + height > r.y;
    }

    /**
     * @return whether this rectangle overlaps the other rectangle.
     * 此矩形是否与另一矩形重叠。
     */
    public boolean overlaps(float rx, float ry, float rwidth, float rheight){
        return x < rx + rwidth && x + width > rx && y < ry + rheight && y + height > ry;
    }

    /**
     * Sets the values of the given rectangle to this rectangle.
     * <p>
     * 将给定矩形的值设置为此矩形的值。
     * @param rect the other rectangle 另一个矩形
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect set(Rect rect){
        this.x = rect.x;
        this.y = rect.y;
        this.width = rect.width;
        this.height = rect.height;

        return this;
    }

    public Rect grow(float amount){
        return grow(amount, amount);
    }

    public Rect grow(float amountX, float amountY){
        x -= amountX/2f;
        y -= amountY/2f;
        width += amountX;
        height += amountY;
        return this;
    }

    /**
     * Merges this rectangle with the other rectangle. The rectangle should not have negative width or negative height.
     * <p>
     * 将此矩形与另一矩形合并。矩形不应有负的宽度或高度。
     * @param rect the other rectangle 另一个矩形
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect merge(Rect rect){
        float minX = Math.min(x, rect.x);
        float maxX = Math.max(x + width, rect.x + rect.width);
        x = minX;
        width = maxX - minX;

        float minY = Math.min(y, rect.y);
        float maxY = Math.max(y + height, rect.y + rect.height);
        y = minY;
        height = maxY - minY;

        return this;
    }

    /**
     * "fixes" negative size dimensions.
     * 修正负的尺寸维度
     */
    public Rect normalize(){
        if(width < 0){
            x += width;
            width = -width;
        }

        if(height < 0){
            y += height;
            height = -height;
        }
        return this;
    }

    /**
     * Merges this rectangle with a point. The rectangle should not have negative width or negative height.
     * <p>
     * 将此矩形与一个点合并。矩形不应有负的宽度或高度。
     * @param x the x coordinate of the point 点的 x 坐标
     * @param y the y coordinate of the point 点的 y 坐标
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect merge(float x, float y){
        float minX = Math.min(this.x, x);
        float maxX = Math.max(this.x + width, x);
        this.x = minX;
        this.width = maxX - minX;

        float minY = Math.min(this.y, y);
        float maxY = Math.max(this.y + height, y);
        this.y = minY;
        this.height = maxY - minY;

        return this;
    }

    /**
     * Merges this rectangle with a point. The rectangle should not have negative width or negative height.
     * <p>
     * 将此矩形与一个点合并。矩形不应有负的宽度或高度。
     * @param vec the vector describing the point 描述该点的向量
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect merge(Vec2 vec){
        return merge(vec.x, vec.y);
    }

    /**
     * Merges this rectangle with a list of points. The rectangle should not have negative width or negative height.
     * <p>
     * 将此矩形与一组点合并。矩形不应有负的宽度或高度。
     * @param vecs the vectors describing the points 描述这些点的向量
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect merge(Vec2[] vecs){
        float minX = x;
        float maxX = x + width;
        float minY = y;
        float maxY = y + height;
        for(int i = 0; i < vecs.length; ++i){
            Vec2 v = vecs[i];
            minX = Math.min(minX, v.x);
            maxX = Math.max(maxX, v.x);
            minY = Math.min(minY, v.y);
            maxY = Math.max(maxY, v.y);
        }
        x = minX;
        width = maxX - minX;
        y = minY;
        height = maxY - minY;
        return this;
    }

    /**
     * Calculates the aspect ratio ( width / height ) of this rectangle
     * <p>
     * 计算此矩形的宽高比( width / height )
     * @return the aspect ratio of this rectangle. Returns Float.NaN if height is 0 to avoid ArithmeticException 此矩形的宽高比。height 为 0 时返回 Float.NaN 以避免 ArithmeticException
     */
    public float getAspectRatio(){
        return (height == 0) ? Float.NaN : width / height;
    }

    /**
     * Calculates the center of the rectangle. Results are located in the given Vec2
     * <p>
     * 计算矩形的中心。结果存入给定的 Vec2 中
     * @param vector the Vec2 to use 要使用的 Vec2
     * @return the given vector with results stored inside 结果存入其中后返回的给定向量
     */
    public Vec2 getCenter(Vec2 vector){
        vector.x = x + width / 2;
        vector.y = y + height / 2;
        return vector;
    }

    /**
     * Moves this rectangle so that its center point is located at a given position
     * <p>
     * 移动此矩形,使其中心点位于给定位置
     * @param x the position's x 位置的 x
     * @param y the position's y 位置的 y
     * @return this for chaining 此对象,用于链式调用
     */
    public Rect setCenter(float x, float y){
        setPosition(x - width / 2, y - height / 2);
        return this;
    }

    /**
     * Moves this rectangle so that its center point is located at a given position
     * <p>
     * 移动此矩形,使其中心点位于给定位置
     * @param position the position 位置
     * @return this for chaining 此对象,用于链式调用
     */
    public Rect setCenter(Vec2 position){
        setPosition(position.x - width / 2, position.y - height / 2);
        return this;
    }

    /**
     * Fits this rectangle around another rectangle while maintaining aspect ratio. This scales and centers the rectangle to the
     * other rectangle (e.g. Having a camera translate and scale to show a given area)
     * <p>
     * 在保持宽高比的前提下将此矩形环绕适配到另一矩形。这会对矩形进行缩放并居中到另一矩形(例如让相机平移和缩放以显示给定区域)
     * @param rect the other rectangle to fit this rectangle around 用于环绕适配的另一个矩形
     * @return this rectangle for chaining 此矩形,用于链式调用
     * @see Scaling
     */
    public Rect fitOutside(Rect rect){
        float ratio = getAspectRatio();

        if(ratio > rect.getAspectRatio()){
            // Wider than tall
            // 宽大于高
            setSize(rect.height * ratio, rect.height);
        }else{
            // Taller than wide
            // 高大于宽
            setSize(rect.width, rect.width / ratio);
        }

        setPosition((rect.x + rect.width / 2) - width / 2, (rect.y + rect.height / 2) - height / 2);
        return this;
    }

    /**
     * Fits this rectangle into another rectangle while maintaining aspect ratio. This scales and centers the rectangle to the
     * other rectangle (e.g. Scaling a texture within a arbitrary cell without squeezing)
     * <p>
     * 在保持宽高比的前提下将此矩形内嵌适配到另一矩形。这会对矩形进行缩放并居中到另一矩形(例如在任意单元格内缩放纹理而不挤压变形)
     * @param rect the other rectangle to fit this rectangle inside 用于内嵌适配的另一个矩形
     * @return this rectangle for chaining 此矩形,用于链式调用
     * @see Scaling
     */
    public Rect fitInside(Rect rect){
        float ratio = getAspectRatio();

        if(ratio < rect.getAspectRatio()){
            // Taller than wide
            // 高大于宽
            setSize(rect.height * ratio, rect.height);
        }else{
            // Wider than tall
            // 宽大于高
            setSize(rect.width, rect.width / ratio);
        }

        setPosition((rect.x + rect.width / 2) - width / 2, (rect.y + rect.height / 2) - height / 2);
        return this;
    }

    /**
     * Converts this {@code Rectangle} to a string in the format {@code [x,y,width,height]}.
     * <p>
     * 将此 {@code Rectangle} 转换为 {@code [x,y,width,height]} 格式的字符串。
     * @return a string representation of this object. 此对象的字符串表示。
     */
    public String toString(){
        return "[" + x + "," + y + "," + width + "," + height + "]";
    }

    /**
     * Sets this {@code Rectangle} to the value represented by the specified string according to the format of {@link #toString()}
     * .
     * <p>
     * 按照 {@link #toString()} 的格式,将此 {@code Rectangle} 设置为由指定字符串表示的值。
     * @param v the string. 字符串。
     * @return this rectangle for chaining 此矩形,用于链式调用
     */
    public Rect fromString(String v){
        int s0 = v.indexOf(',', 1);
        int s1 = v.indexOf(',', s0 + 1);
        int s2 = v.indexOf(',', s1 + 1);
        if(s0 != -1 && s1 != -1 && s2 != -1 && v.charAt(0) == '[' && v.charAt(v.length() - 1) == ']'){
            try{
                float x = Float.parseFloat(v.substring(1, s0));
                float y = Float.parseFloat(v.substring(s0 + 1, s1));
                float width = Float.parseFloat(v.substring(s1 + 1, s2));
                float height = Float.parseFloat(v.substring(s2 + 1, v.length() - 1));
                return this.set(x, y, width, height);
            }catch(NumberFormatException ex){
                // Throw a ArcRuntimeException
                // 抛出 ArcRuntimeException
            }
        }
        throw new ArcRuntimeException("Malformed Rectangle: " + v);
    }

    public float area(){
        return this.width * this.height;
    }

    public float perimeter(){
        return 2 * (this.width + this.height);
    }

    public int hashCode(){
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToRawIntBits(height);
        result = prime * result + Float.floatToRawIntBits(width);
        result = prime * result + Float.floatToRawIntBits(x);
        result = prime * result + Float.floatToRawIntBits(y);
        return result;
    }

    public boolean equals(Object obj){
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        Rect other = (Rect)obj;
        if(Float.floatToRawIntBits(height) != Float.floatToRawIntBits(other.height)) return false;
        if(Float.floatToRawIntBits(width) != Float.floatToRawIntBits(other.width)) return false;
        if(Float.floatToRawIntBits(x) != Float.floatToRawIntBits(other.x)) return false;
        return Float.floatToRawIntBits(y) == Float.floatToRawIntBits(other.y);
    }

}
