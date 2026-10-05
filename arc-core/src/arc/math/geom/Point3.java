package arc.math.geom;

/**
 * A point in a 3D grid, with integer x and y coordinates
 * <p>
 * 3D 网格中的点,具有整数 x 和 y 坐标
 * @author badlogic
 */
public class Point3{
    private static final long serialVersionUID = 5922187982746752830L;

    public int x;
    public int y;
    public int z;

    /**
     * Constructs a 3D grid point with all coordinates pointing to the origin (0, 0, 0).
     * 构造一个所有坐标都指向原点 (0, 0, 0) 的 3D 网格点。
     */
    public Point3(){
    }

    /**
     * Constructs a 3D grid point.
     * <p>
     * 构造一个 3D 网格点。
     * @param x X coordinate X 坐标
     * @param y Y coordinate Y 坐标
     * @param z Z coordinate Z 坐标
     */
    public Point3(int x, int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Copy constructor
     * <p>
     * 拷贝构造函数
     * @param point The 3D grid point to make a copy of. 要据以创建副本的 3D 网格点。
     */
    public Point3(Point3 point){
        this.x = point.x;
        this.y = point.y;
        this.z = point.z;
    }

    /**
     * Sets the coordinates of this 3D grid point to that of another.
     * <p>
     * 将此 3D 网格点的坐标设置为另一个点的坐标。
     * @param point The 3D grid point to copy coordinates of. 要复制其坐标的 3D 网格点。
     * @return this Point3 for chaining. 此 Point3,用于链式调用。
     */
    public Point3 set(Point3 point){
        this.x = point.x;
        this.y = point.y;
        this.z = point.z;
        return this;
    }

    /**
     * Sets the coordinates of this GridPoint3D.
     * <p>
     * 设置此 GridPoint3D 的坐标。
     * @param x X coordinate X 坐标
     * @param y Y coordinate Y 坐标
     * @param z Z coordinate Z 坐标
     * @return this GridPoint3D for chaining. 此 GridPoint3D,用于链式调用。
     */
    public Point3 set(int x, int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * @param other The other point 另一个点
     * @return the squared distance between this point and the other point. 此点与另一点之间距离的平方。
     */
    public float dst2(Point3 other){
        int xd = other.x - x;
        int yd = other.y - y;
        int zd = other.z - z;

        return xd * xd + yd * yd + zd * zd;
    }

    /**
     * @param x The x-coordinate of the other point 另一个点的 x 坐标
     * @param y The y-coordinate of the other point 另一个点的 y 坐标
     * @param z The z-coordinate of the other point 另一个点的 z 坐标
     * @return the squared distance between this point and the other point. 此点与另一点之间距离的平方。
     */
    public float dst2(int x, int y, int z){
        int xd = x - this.x;
        int yd = y - this.y;
        int zd = z - this.z;

        return xd * xd + yd * yd + zd * zd;
    }

    /**
     * @param other The other point 另一个点
     * @return the distance between this point and the other vector. 此点与另一向量之间的距离。
     */
    public float dst(Point3 other){
        int xd = other.x - x;
        int yd = other.y - y;
        int zd = other.z - z;

        return (float)Math.sqrt(xd * xd + yd * yd + zd * zd);
    }

    /**
     * @param x The x-coordinate of the other point 另一个点的 x 坐标
     * @param y The y-coordinate of the other point 另一个点的 y 坐标
     * @param z The z-coordinate of the other point 另一个点的 z 坐标
     * @return the distance between this point and the other point. 此点与另一点之间的距离。
     */
    public float dst(int x, int y, int z){
        int xd = x - this.x;
        int yd = y - this.y;
        int zd = z - this.z;

        return (float)Math.sqrt(xd * xd + yd * yd + zd * zd);
    }

    /**
     * Adds another 3D grid point to this point.
     * <p>
     * 将另一个 3D 网格点加到此点上。
     * @param other The other point 另一个点
     * @return this 3d grid point for chaining. 此 3D 网格点,用于链式调用。
     */
    public Point3 add(Point3 other){
        x += other.x;
        y += other.y;
        z += other.z;
        return this;
    }

    /**
     * Adds another 3D grid point to this point.
     * <p>
     * 将另一个 3D 网格点加到此点上。
     * @param x The x-coordinate of the other point 另一个点的 x 坐标
     * @param y The y-coordinate of the other point 另一个点的 y 坐标
     * @param z The z-coordinate of the other point 另一个点的 z 坐标
     * @return this 3d grid point for chaining. 此 3D 网格点,用于链式调用。
     */
    public Point3 add(int x, int y, int z){
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    /**
     * Subtracts another 3D grid point from this point.
     * <p>
     * 从此点中减去另一个 3D 网格点。
     * @param other The other point 另一个点
     * @return this 3d grid point for chaining. 此 3D 网格点,用于链式调用。
     */
    public Point3 sub(Point3 other){
        x -= other.x;
        y -= other.y;
        z -= other.z;
        return this;
    }

    /**
     * Subtracts another 3D grid point from this point.
     * <p>
     * 从此点中减去另一个 3D 网格点。
     * @param x The x-coordinate of the other point 另一个点的 x 坐标
     * @param y The y-coordinate of the other point 另一个点的 y 坐标
     * @param z The z-coordinate of the other point 另一个点的 z 坐标
     * @return this 3d grid point for chaining. 此 3D 网格点,用于链式调用。
     */
    public Point3 sub(int x, int y, int z){
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    /**
     * @return a copy of this grid point
     * 此网格点的副本
     */
    public Point3 cpy(){
        return new Point3(this);
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || o.getClass() != this.getClass()) return false;
        Point3 g = (Point3)o;
        return this.x == g.x && this.y == g.y && this.z == g.z;
    }

    @Override
    public int hashCode(){
        final int prime = 17;
        int result = 1;
        result = prime * result + this.x;
        result = prime * result + this.y;
        result = prime * result + this.z;
        return result;
    }

    @Override
    public String toString(){
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
