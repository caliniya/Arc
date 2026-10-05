package arc.math.geom;

import arc.struct.*;

/**
 * Encapsulates an axis aligned bounding box represented by a minimum and a maximum Vector. Additionally you can query for the
 * bounding box's center, dimensions and corner points.
 * <p>
 * 封装由最小值和最大值向量表示的轴对齐包围盒。此外还可查询包围盒的中心、尺寸和角点。
 * @author badlogicgames@gmail.com, Xoppa
 */
public class BoundingBox{
    public final Vec3 min = new Vec3();
    public final Vec3 max = new Vec3();

    private final Vec3 cnt = new Vec3();
    private final Vec3 dim = new Vec3();

    /**
     * Constructs a new bounding box with the minimum and maximum vector set to zeros.
     * 构造一个最小值向量和最大值向量均为零的新包围盒。
     */
    public BoundingBox(){
        clr();
    }

    /**
     * Constructs a new bounding box from the given bounding box.
     * <p>
     * 根据给定的包围盒构造新的包围盒。
     * @param bounds The bounding box to copy 要复制的包围盒
     */
    public BoundingBox(BoundingBox bounds){
        this.set(bounds);
    }

    /**
     * Constructs the new bounding box using the given minimum and maximum vector.
     * <p>
     * 用给定的最小值和最大值向量构造新的包围盒。
     * @param minimum The minimum vector 最小值向量
     * @param maximum The maximum vector 最大值向量
     */
    public BoundingBox(Vec3 minimum, Vec3 maximum){
        this.set(minimum, maximum);
    }

    static float min(final float a, final float b){
        return a > b ? b : a;
    }

    static float max(final float a, final float b){
        return a > b ? a : b;
    }

    /**
     * @param out The {@link Vec3} to receive the center of the bounding box. 用于接收包围盒中心的 {@link Vec3}。
     * @return The vector specified with the out argument. out 参数指定的向量。
     */
    public Vec3 getCenter(Vec3 out){
        return out.set(cnt);
    }

    public float getCenterX(){
        return cnt.x;
    }

    public float getCenterY(){
        return cnt.y;
    }

    public float getCenterZ(){
        return cnt.z;
    }

    public Vec3 getCorner000(final Vec3 out){
        return out.set(min.x, min.y, min.z);
    }

    public Vec3 getCorner001(final Vec3 out){
        return out.set(min.x, min.y, max.z);
    }

    public Vec3 getCorner010(final Vec3 out){
        return out.set(min.x, max.y, min.z);
    }

    public Vec3 getCorner011(final Vec3 out){
        return out.set(min.x, max.y, max.z);
    }

    public Vec3 getCorner100(final Vec3 out){
        return out.set(max.x, min.y, min.z);
    }

    public Vec3 getCorner101(final Vec3 out){
        return out.set(max.x, min.y, max.z);
    }

    public Vec3 getCorner110(final Vec3 out){
        return out.set(max.x, max.y, min.z);
    }

    public Vec3 getCorner111(final Vec3 out){
        return out.set(max.x, max.y, max.z);
    }

    /**
     * @param out The {@link Vec3} to receive the dimensions of this bounding box on all three axis. 用于接收此包围盒三个轴方向尺寸的 {@link Vec3}。
     * @return The vector specified with the out argument out 参数指定的向量
     */
    public Vec3 getDimensions(final Vec3 out){
        return out.set(dim);
    }

    public float getWidth(){
        return dim.x;
    }

    public float getHeight(){
        return dim.y;
    }

    public float getDepth(){
        return dim.z;
    }

    /**
     * @param out The {@link Vec3} to receive the minimum values. 用于接收最小值的 {@link Vec3}。
     * @return The vector specified with the out argument out 参数指定的向量
     */
    public Vec3 getMin(final Vec3 out){
        return out.set(min);
    }

    /**
     * @param out The {@link Vec3} to receive the maximum values. 用于接收最大值的 {@link Vec3}。
     * @return The vector specified with the out argument out 参数指定的向量
     */
    public Vec3 getMax(final Vec3 out){
        return out.set(max);
    }

    /**
     * Sets the given bounding box.
     * <p>
     * 设置给定的包围盒。
     * @param bounds The bounds. 边界。
     * @return This bounding box for chaining. 此包围盒,用于链式调用。
     */
    public BoundingBox set(BoundingBox bounds){
        return this.set(bounds.min, bounds.max);
    }

    /**
     * Sets the given minimum and maximum vector.
     * <p>
     * 设置给定的最小值和最大值向量。
     * @param minimum The minimum vector 最小值向量
     * @param maximum The maximum vector 最大值向量
     * @return This bounding box for chaining. 此包围盒,用于链式调用。
     */
    public BoundingBox set(Vec3 minimum, Vec3 maximum){
        min.set(minimum.x < maximum.x ? minimum.x : maximum.x, minimum.y < maximum.y ? minimum.y : maximum.y,
        minimum.z < maximum.z ? minimum.z : maximum.z);
        max.set(minimum.x > maximum.x ? minimum.x : maximum.x, minimum.y > maximum.y ? minimum.y : maximum.y,
        minimum.z > maximum.z ? minimum.z : maximum.z);
        cnt.set(min).add(max).scl(0.5f);
        dim.set(max).sub(min);
        return this;
    }

    /**
     * Sets the bounding box minimum and maximum vector from the given points.
     * <p>
     * 根据给定的点设置包围盒的最小值和最大值向量。
     * @param points The points. 点集。
     * @return This bounding box for chaining. 此包围盒,用于链式调用。
     */
    public BoundingBox set(Vec3[] points){
        this.inf();
        for(Vec3 l_point : points)
            this.ext(l_point);
        return this;
    }

    /**
     * Sets the bounding box minimum and maximum vector from the given points.
     * <p>
     * 根据给定的点设置包围盒的最小值和最大值向量。
     * @param points The points. 点集。
     * @return This bounding box for chaining. 此包围盒,用于链式调用。
     */
    public BoundingBox set(Ar<Vec3> points){
        this.inf();
        for(Vec3 l_point : points)
            this.ext(l_point);
        return this;
    }

    /**
     * Sets the minimum and maximum vector to positive and negative infinity.
     * <p>
     * 将最小值和最大值向量设为正、负无穷。
     * @return This bounding box for chaining. 此包围盒,用于链式调用。
     */
    public BoundingBox inf(){
        min.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        max.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        cnt.set(0, 0, 0);
        dim.set(0, 0, 0);
        return this;
    }

    /**
     * Extends the bounding box to incorporate the given {@link Vec3}.
     * <p>
     * 扩展包围盒以包含给定的 {@link Vec3}。
     * @param point The vector 向量
     * @return This bounding box for chaining. 此包围盒,用于链式调用。
     */
    public BoundingBox ext(Vec3 point){
        return this.set(min.set(min(min.x, point.x), min(min.y, point.y), min(min.z, point.z)),
        max.set(Math.max(max.x, point.x), Math.max(max.y, point.y), Math.max(max.z, point.z)));
    }

    /**
     * Sets the minimum and maximum vector to zeros.
     * <p>
     * 将最小值和最大值向量设为零。
     * @return This bounding box for chaining. 此包围盒,用于链式调用。
     */
    public BoundingBox clr(){
        return this.set(min.set(0, 0, 0), max.set(0, 0, 0));
    }

    /**
     * Returns whether this bounding box is valid. This means that {@link #max} is greater than or equal to {@link #min}.
     * <p>
     * 返回此包围盒是否有效。即 {@link #max} 大于等于 {@link #min}。
     * @return True in case the bounding box is valid, false otherwise 若包围盒有效则为 true,否则为 false
     */
    public boolean isValid(){
        return min.x <= max.x && min.y <= max.y && min.z <= max.z;
    }

    /**
     * Extends this bounding box by the given bounding box.
     * <p>
     * 用给定的包围盒扩展此包围盒。
     * @param a_bounds The bounding box 包围盒
     * @return This bounding box for chaining. 此包围盒,用于链式调用。
     */
    public BoundingBox ext(BoundingBox a_bounds){
        return this.set(min.set(min(min.x, a_bounds.min.x), min(min.y, a_bounds.min.y), min(min.z, a_bounds.min.z)),
        max.set(max(max.x, a_bounds.max.x), max(max.y, a_bounds.max.y), max(max.z, a_bounds.max.z)));
    }

    /**
     * Extends this bounding box by the given sphere.
     * <p>
     * 用给定的球体扩展此包围盒。
     * @param center Sphere center 球心
     * @param radius Sphere radius 球的半径
     * @return This bounding box for chaining. 此包围盒,用于链式调用。
     */
    public BoundingBox ext(Vec3 center, float radius){
        return this.set(min.set(min(min.x, center.x - radius), min(min.y, center.y - radius), min(min.z, center.z - radius)),
        max.set(max(max.x, center.x + radius), max(max.y, center.y + radius), max(max.z, center.z + radius)));
    }

    /**
     * Returns whether the given bounding box is contained in this bounding box.
     * <p>
     * 返回给定的包围盒是否包含在此包围盒中。
     * @param b The bounding box 包围盒
     * @return Whether the given bounding box is contained 是否包含给定的包围盒
     */
    public boolean contains(BoundingBox b){
        return !isValid()
        || (min.x <= b.min.x && min.y <= b.min.y && min.z <= b.min.z && max.x >= b.max.x && max.y >= b.max.y && max.z >= b.max.z);
    }

    /**
     * Returns whether the given bounding box is intersecting this bounding box (at least one point in).
     * <p>
     * 返回给定包围盒是否与此包围盒相交(至少一个点在内)。
     * @param b The bounding box 包围盒
     * @return Whether the given bounding box is intersected 是否与给定的包围盒相交
     */
    public boolean intersects(BoundingBox b){
        if(!isValid()) return false;

        // test using SAT (separating axis theorem)
        // 使用 SAT(分离轴定理)进行测试

        float lx = Math.abs(this.cnt.x - b.cnt.x);
        float sumx = (this.dim.x / 2.0f) + (b.dim.x / 2.0f);

        float ly = Math.abs(this.cnt.y - b.cnt.y);
        float sumy = (this.dim.y / 2.0f) + (b.dim.y / 2.0f);

        float lz = Math.abs(this.cnt.z - b.cnt.z);
        float sumz = (this.dim.z / 2.0f) + (b.dim.z / 2.0f);

        return (lx <= sumx && ly <= sumy && lz <= sumz);

    }

    /**
     * Returns whether the given vector is contained in this bounding box.
     * <p>
     * 返回给定向量是否包含在此包围盒中。
     * @param v The vector 向量
     * @return Whether the vector is contained or not. 向量是否被包含。
     */
    public boolean contains(Vec3 v){
        return min.x <= v.x && max.x >= v.x && min.y <= v.y && max.y >= v.y && min.z <= v.z && max.z >= v.z;
    }

    @Override
    public String toString(){
        return "[" + min + "|" + max + "]";
    }

    /**
     * Extends the bounding box by the given vector.
     * <p>
     * 用给定向量扩展包围盒。
     * @param x The x-coordinate x 坐标
     * @param y The y-coordinate y 坐标
     * @param z The z-coordinate z 坐标
     * @return This bounding box for chaining. 此包围盒,用于链式调用。
     */
    public BoundingBox ext(float x, float y, float z){
        return this.set(min.set(min(min.x, x), min(min.y, y), min(min.z, z)), max.set(max(max.x, x), max(max.y, y), max(max.z, z)));
    }
}
