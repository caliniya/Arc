package arc.math.geom;

/**
 * A plane defined via a unit length normal and the distance from the origin, as you learned in your math class.
 * <p>
 * 由单位长度法线和到原点的距离定义的平面,正如数学课上学到的那样。
 * @author badlogicgames@gmail.com
 */
public class Plane{
    public final Vec3 normal = new Vec3();
    public float d = 0;

    /**
     * Constructs a new plane with all values set to 0
     * 构造一个所有值均为 0 的新平面
     */
    public Plane(){

    }

    /**
     * Constructs a new plane based on the normal and distance to the origin.
     * <p>
     * 根据法线和到原点的距离构造新平面。
     * @param normal The plane normal 平面法线
     * @param d The distance to the origin 到原点的距离
     */
    public Plane(Vec3 normal, float d){
        this.normal.set(normal).nor();
        this.d = d;
    }

    /**
     * Constructs a new plane based on the normal and a point on the plane.
     * <p>
     * 根据法线和平面上的一个点构造新平面。
     * @param normal The normal 法线
     * @param point The point on the plane 平面上的点
     */
    public Plane(Vec3 normal, Vec3 point){
        this.normal.set(normal).nor();
        this.d = -this.normal.dot(point);
    }

    /**
     * Constructs a new plane out of the three given points that are considered to be on the plane. The normal is calculated via a
     * cross product between (point1-point2)x(point2-point3)
     * <p>
     * 由给定的三个点(视为在平面上)构造新平面。法线通过 (point1-point2)x(point2-point3) 叉积计算
     * @param point1 The first point 第一个点
     * @param point2 The second point 第二个点
     * @param point3 The third point 第三个点
     */
    public Plane(Vec3 point1, Vec3 point2, Vec3 point3){
        set(point1, point2, point3);
    }

    /**
     * Sets the plane normal and distance to the origin based on the three given points which are considered to be on the plane.
     * The normal is calculated via a cross product between (point1-point2)x(point2-point3)
     * <p>
     * 根据给定的三个在平面上的点设置平面法线和到原点的距离。法线通过 (point1-point2)x(point2-point3) 叉积计算
     */
    public void set(Vec3 point1, Vec3 point2, Vec3 point3){
        normal.set(point1).sub(point2).crs(point2.x - point3.x, point2.y - point3.y, point2.z - point3.z).nor();
        d = -point1.dot(normal);
    }

    /**
     * Sets the plane normal and distance
     * <p>
     * 设置平面法线和距离
     * @param nx normal x-component 法线的 x 分量
     * @param ny normal y-component 法线的 y 分量
     * @param nz normal z-component 法线的 z 分量
     * @param d distance to origin 到原点的距离
     */
    public void set(float nx, float ny, float nz, float d){
        normal.set(nx, ny, nz);
        this.d = d;
    }

    /** Projects the supplied vector onto this plane.
     * <p>
     * 将给定的向量投影到此平面上。
     * @param v the vector to project onto this plane. 要投影到此平面上的向量。 */
    public Vec3 project(Vec3 v){
        float npd = normal.dot(v) + d;
        return v.sub(npd * normal.x, npd * normal.y, npd * normal.z);
    }

    /**
     * Calculates the shortest signed distance between the plane and the given point.
     * <p>
     * 计算平面与给定点之间的最短有向距离。
     * @param point The point 点
     * @return the shortest signed distance between the plane and the point 平面与点之间的最短有向距离
     */
    public float distance(Vec3 point){
        return normal.dot(point) + d;
    }

    /**
     * Returns on which side the given point lies relative to the plane and its normal. PlaneSide.Front refers to the side the
     * plane normal points to.
     * <p>
     * 返回给定点相对平面及其法线的位置。PlaneSide.Front 指平面法线指向的一侧。
     * @param point The point 点
     * @return The side the point lies relative to the plane 点相对平面所在的侧
     */
    public PlaneSide testPoint(Vec3 point){
        float dist = normal.dot(point) + d;

        if(dist == 0)
            return PlaneSide.onPlane;
        else if(dist < 0)
            return PlaneSide.back;
        else
            return PlaneSide.front;
    }

    /**
     * Returns on which side the given point lies relative to the plane and its normal. PlaneSide.Front refers to the side the
     * plane normal points to.
     * <p>
     * 返回给定点相对平面及其法线的位置。PlaneSide.Front 指平面法线指向的一侧。
     * @return The side the point lies relative to the plane 点相对平面所在的侧
     */
    public PlaneSide testPoint(float x, float y, float z){
        float dist = normal.dot(x, y, z) + d;

        if(dist == 0)
            return PlaneSide.onPlane;
        else if(dist < 0)
            return PlaneSide.back;
        else
            return PlaneSide.front;
    }

    /**
     * Returns whether the plane is facing the direction vector. Think of the direction vector as the direction a camera looks in.
     * This method will return true if the front side of the plane determined by its normal faces the camera.
     * <p>
     * 返回平面是否朝向给定的方向向量。可将方向向量理解为相机观察的方向。若由法线确定的平面正面朝向相机,此方法返回 true。
     * @param direction the direction 方向
     * @return whether the plane is front facing 平面是否朝前
     */
    public boolean isFrontFacing(Vec3 direction){
        float dot = normal.dot(direction);
        return dot <= 0;
    }

    /**
     * @return The normal
     * 法线
     */
    public Vec3 getNormal(){
        return normal;
    }

    /**
     * @return The distance to the origin
     * 到原点的距离
     */
    public float getD(){
        return d;
    }

    /**
     * Sets the plane to the given point and normal.
     * <p>
     * 将平面设置为给定的点和法线。
     * @param point the point on the plane 平面上的点
     * @param normal the normal of the plane 平面的法线
     */
    public void set(Vec3 point, Vec3 normal){
        this.normal.set(normal);
        d = -point.dot(normal);
    }

    public void set(float pointX, float pointY, float pointZ, float norX, float norY, float norZ){
        this.normal.set(norX, norY, norZ);
        d = -(pointX * norX + pointY * norY + pointZ * norZ);
    }

    /**
     * Sets this plane from the given plane
     * <p>
     * 根据给定的平面设置此平面
     * @param plane the plane 平面
     */
    public void set(Plane plane){
        this.normal.set(plane.normal);
        this.d = plane.d;
    }

    @Override
    public String toString(){
        return normal.toString() + ", " + d;
    }

    /**
     * Enum specifying on which side a point lies respective to the plane and it's normal. {@link PlaneSide#front} is the side to
     * which the normal points.
     * <p>
     * 枚举,指定点相对平面及其法线的位置。{@link PlaneSide#front} 是法线指向的一侧。
     * @author mzechner
     */
    public enum PlaneSide{
        onPlane, back, front
    }
}
