package arc.math.geom;

/**
 * A point in a 2D grid, with integer x and y coordinates
 * <p>
 * 2D 网格中的点,具有整数 x 和 y 坐标
 * @author badlogic
 */
public class Point2{
    public int x;
    public int y;

    /**
     * Constructs a new 2D grid point.
     * 构造一个新的 2D 网格点。
     */
    public Point2(){
    }

    /**
     * Constructs a new 2D grid point.
     * <p>
     * 构造一个新的 2D 网格点。
     * @param x X coordinate X 坐标
     * @param y Y coordinate Y 坐标
     */
    public Point2(int x, int y){
        this.x = x;
        this.y = y;
    }

    /**
     * Copy constructor
     * <p>
     * 拷贝构造函数
     * @param point The 2D grid point to make a copy of. 要据以创建副本的 2D 网格点。
     */
    public Point2(Point2 point){
        this.x = point.x;
        this.y = point.y;
    }

    /**
     * @return a point unpacked from an integer.
     * 从整数解包得到的点
     */
    public static Point2 unpack(int pos){
        return new Point2((short)(pos >>> 16), (short)(pos & 0xFFFF));
    }

    /**
     * @return this point packed into a single int by casting its components to shorts.
     * 将各分量转换为 short 后打包成单个 int 的此点。
     */
    public static int pack(int x, int y){
        return (((short)x) << 16) | (((short)y) & 0xFFFF);
    }

    /**
     * @return the x component of a packed position.
     * 打包位置解出的 x 分量。
     */
    public static short x(int pos){
        return (short)(pos >>> 16);
    }

    /**
     * @return the y component of a packed position.
     * 打包位置解出的 y 分量。
     */
    public static short y(int pos){
        return (short)(pos & 0xFFFF);
    }

    /**
     * @return this point packed into a single int by casting its components to shorts.
     * 将各分量转换为 short 后打包成单个 int 的此点。
     */
    public int pack(){
        return pack(x, y);
    }

    /**
     * Sets the coordinates of this 2D grid point to that of another.
     * <p>
     * 将此 2D 网格点的坐标设置为另一个点的坐标。
     * @param point The 2D grid point to copy the coordinates of. 要复制其坐标的 2D 网格点。
     * @return this 2D grid point for chaining. 此 2D 网格点,用于链式调用。
     */
    public Point2 set(Point2 point){
        this.x = point.x;
        this.y = point.y;
        return this;
    }

    /**
     * Sets the coordinates of this 2D grid point.
     * <p>
     * 设置此 2D 网格点的坐标。
     * @param x X coordinate X 坐标
     * @param y Y coordinate Y 坐标
     * @return this 2D grid point for chaining. 此 2D 网格点,用于链式调用。
     */
    public Point2 set(int x, int y){
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * @param other The other point 另一个点
     * @return the squared distance between this point and the other point. 此点与另一点之间距离的平方。
     */
    public float dst2(Point2 other){
        int xd = other.x - x;
        int yd = other.y - y;

        return xd * xd + yd * yd;
    }

    /**
     * @param x The x-coordinate of the other point 另一个点的 x 坐标
     * @param y The y-coordinate of the other point 另一个点的 y 坐标
     * @return the squared distance between this point and the other point. 此点与另一点之间距离的平方。
     */
    public float dst2(int x, int y){
        int xd = x - this.x;
        int yd = y - this.y;

        return xd * xd + yd * yd;
    }

    /**
     * @param other The other point 另一个点
     * @return the distance between this point and the other vector. 此点与另一向量之间的距离。
     */
    public float dst(Point2 other){
        int xd = other.x - x;
        int yd = other.y - y;

        return (float)Math.sqrt(xd * xd + yd * yd);
    }

    /**
     * @param x The x-coordinate of the other point 另一个点的 x 坐标
     * @param y The y-coordinate of the other point 另一个点的 y 坐标
     * @return the distance between this point and the other point. 此点与另一点之间的距离。
     */
    public float dst(int x, int y){
        int xd = x - this.x;
        int yd = y - this.y;

        return (float)Math.sqrt(xd * xd + yd * yd);
    }

    /**
     * Adds another 2D grid point to this point.
     * <p>
     * 将另一个 2D 网格点加到此点上。
     * @param other The other point 另一个点
     * @return this 2d grid point for chaining. 此 2D 网格点,用于链式调用。
     */
    public Point2 add(Point2 other){
        x += other.x;
        y += other.y;
        return this;
    }

    /**
     * Adds another 2D grid point to this point.
     * <p>
     * 将另一个 2D 网格点加到此点上。
     * @param x The x-coordinate of the other point 另一个点的 x 坐标
     * @param y The y-coordinate of the other point 另一个点的 y 坐标
     * @return this 2d grid point for chaining. 此 2D 网格点,用于链式调用。
     */
    public Point2 add(int x, int y){
        this.x += x;
        this.y += y;
        return this;
    }

    /**
     * Subtracts another 2D grid point from this point.
     * <p>
     * 从此点中减去另一个 2D 网格点。
     * @param other The other point 另一个点
     * @return this 2d grid point for chaining. 此 2D 网格点,用于链式调用。
     */
    public Point2 sub(Point2 other){
        x -= other.x;
        y -= other.y;
        return this;
    }

    /**
     * Subtracts another 2D grid point from this point.
     * <p>
     * 从此点中减去另一个 2D 网格点。
     * @param x The x-coordinate of the other point 另一个点的 x 坐标
     * @param y The y-coordinate of the other point 另一个点的 y 坐标
     * @return this 2d grid point for chaining. 此 2D 网格点,用于链式调用。
     */
    public Point2 sub(int x, int y){
        this.x -= x;
        this.y -= y;
        return this;
    }

    /**
     * @return a copy of this grid point
     * 此网格点的副本
     */
    public Point2 cpy(){
        return new Point2(this);
    }

    /**
     * Rotates this point in 90-degree increments several times.
     * 以 90 度为步长将此点旋转多次。
     */
    public Point2 rotate(int steps){
        for(int i = 0; i < Math.abs(steps); i++){
            int x = this.x;
            if(steps >= 0){
                this.x = -y;
                y = x;
            }else{
                this.x = y;
                y = -x;
            }
        }
        return this;
    }

    public boolean equals(int x, int y){
        return this.x == x && this.y == y;
    }

    public static boolean equals(int x, int y, int ox, int oy){
        return x == ox && y  == oy;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || o.getClass() != this.getClass()) return false;
        Point2 g = (Point2)o;
        return this.x == g.x && this.y == g.y;
    }

    @Override
    public int hashCode(){
        return x * 0xC13F + y * 0x91E1;
    }

    @Override
    public String toString(){
        return "(" + x + ", " + y + ")";
    }
}
