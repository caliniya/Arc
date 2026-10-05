package arc.math.geom;

import arc.math.*;

/**
 * Encapsulates a 3D sphere with a center and a radius
 * <p>
 * 封装具有球心和半径的 3D 球体
 * @author badlogicgames@gmail.com
 */
public class Sphere{
    private static final float PI_4_3 = Mathf.PI * 4f / 3f;
    /**
     * the center of the sphere
     * 球心
     */
    public final Vec3 center;
    /**
     * the radius of the sphere
     * 球的半径
     */
    public float radius;

    /**
     * Constructs a sphere with the given center and radius
     * <p>
     * 用给定的球心和半径构造球体
     * @param center The center 中心点
     * @param radius The radius 半径
     */
    public Sphere(Vec3 center, float radius){
        this.center = new Vec3(center);
        this.radius = radius;
    }

    /**
     * @param sphere the other sphere 另一个球体
     * @return whether this and the other sphere overlap 此球与另一球是否重叠
     */
    public boolean overlaps(Sphere sphere){
        return center.dst2(sphere.center) < (radius + sphere.radius) * (radius + sphere.radius);
    }

    @Override
    public int hashCode(){
        final int prime = 71;
        int result = 1;
        result = prime * result + this.center.hashCode();
        result = prime * result + Float.floatToRawIntBits(this.radius);
        return result;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || o.getClass() != this.getClass()) return false;
        Sphere s = (Sphere)o;
        return this.radius == s.radius && this.center.equals(s.center);
    }

    public float volume(){
        return PI_4_3 * this.radius * this.radius * this.radius;
    }

    public float surfaceArea(){
        return 4 * Mathf.PI * this.radius * this.radius;
    }
}
