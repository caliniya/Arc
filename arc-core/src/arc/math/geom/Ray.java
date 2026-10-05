package arc.math.geom;

/**
 * Encapsulates a ray having a starting position and a unit length direction.
 * <p>
 * 封装具有起始位置和单位长度方向的射线。
 * @author badlogicgames@gmail.com
 */
public class Ray{
    public final Vec3 origin = new Vec3();
    public final Vec3 direction = new Vec3();

    public Ray(){
    }

    /**
     * Constructor, sets the starting position of the ray and the direction.
     * <p>
     * 构造函数,设置射线的起始位置和方向。
     * @param origin The starting position 起始位置
     * @param direction The direction 方向
     */
    public Ray(Vec3 origin, Vec3 direction){
        this.origin.set(origin);
        this.direction.set(direction).nor();
    }

    /**
     * @return a copy of this ray.
     * 此射线的副本。
     */
    public Ray cpy(){
        return new Ray(this.origin, this.direction);
    }

    /**
     * Returns the endpoint given the distance. This is calculated as startpoint + distance * direction.
     * <p>
     * 根据距离返回端点。计算方式为 startpoint + distance * direction。
     * @param out The vector to set to the result 用于保存结果的向量
     * @param distance The distance from the end point to the start point. 终点到起点的距离。
     * @return The out param out 参数
     */
    public Vec3 getEndPoint(final Vec3 out, final float distance){
        return out.set(direction).scl(distance).add(origin);
    }

    /**
     * Sets the starting position and the direction of this ray.
     * <p>
     * 设置此射线的起始位置和方向。
     * @param origin The starting position 起始位置
     * @param direction The direction 方向
     * @return this ray for chaining 此射线,用于链式调用
     */
    public Ray set(Vec3 origin, Vec3 direction){
        this.origin.set(origin);
        this.direction.set(direction);
        return this;
    }

    /**
     * Sets this ray from the given starting position and direction.
     * <p>
     * 根据给定的起始位置和方向设置此射线。
     * @param x The x-component of the starting position 起始位置的 x 分量
     * @param y The y-component of the starting position 起始位置的 y 分量
     * @param z The z-component of the starting position 起始位置的 z 分量
     * @param dx The x-component of the direction 方向的 x 分量
     * @param dy The y-component of the direction 方向的 y 分量
     * @param dz The z-component of the direction 方向的 z 分量
     * @return this ray for chaining 此射线,用于链式调用
     */
    public Ray set(float x, float y, float z, float dx, float dy, float dz){
        this.origin.set(x, y, z);
        this.direction.set(dx, dy, dz);
        return this;
    }

    /**
     * Sets the starting position and direction from the given ray
     * <p>
     * 根据给定的射线设置起始位置和方向
     * @param ray The ray 射线
     * @return This ray for chaining 此射线,用于链式调用
     */
    public Ray set(Ray ray){
        this.origin.set(ray.origin);
        this.direction.set(ray.direction);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String toString(){
        return "ray [" + origin + ":" + direction + "]";
    }

    @Override
    public boolean equals(Object o){
        if(o == this) return true;
        if(o == null || o.getClass() != this.getClass()) return false;
        Ray r = (Ray)o;
        return this.direction.equals(r.direction) && this.origin.equals(r.origin);
    }

    @Override
    public int hashCode(){
        final int prime = 73;
        int result = 1;
        result = prime * result + this.direction.hashCode();
        result = prime * result + this.origin.hashCode();
        return result;
    }
}
