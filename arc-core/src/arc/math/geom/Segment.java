package arc.math.geom;

/**
 * A Segment is a line in 3-space having a staring and an ending position.
 * <p>
 * 线段(Segment)是三维空间中具有起点和终点的直线。
 * @author mzechner
 */
public class Segment{
    /**
     * the starting position
     * 起始位置
     */
    public final Vec3 a = new Vec3();
    /**
     * the ending position
     * 终止位置
     */
    public final Vec3 b = new Vec3();

    /**
     * Constructs a new Segment from the two points given.
     * <p>
     * 根据给定的两点构造新的线段。
     * @param a the first point 第一个点
     * @param b the second point 第二个点
     */
    public Segment(Vec3 a, Vec3 b){
        this.a.set(a);
        this.b.set(b);
    }

    /**
     * Constructs a new Segment from the two points given.
     * <p>
     * 根据给定的两点构造新的线段。
     * @param aX the x-coordinate of the first point 第一个点的 x 坐标
     * @param aY the y-coordinate of the first point 第一个点的 y 坐标
     * @param aZ the z-coordinate of the first point 第一个点的 z 坐标
     * @param bX the x-coordinate of the second point 第二个点的 x 坐标
     * @param bY the y-coordinate of the second point 第二个点的 y 坐标
     * @param bZ the z-coordinate of the second point 第二个点的 z 坐标
     */
    public Segment(float aX, float aY, float aZ, float bX, float bY, float bZ){
        this.a.set(aX, aY, aZ);
        this.b.set(bX, bY, bZ);
    }

    public float len(){
        return a.dst(b);
    }

    public float len2(){
        return a.dst2(b);
    }

    @Override
    public boolean equals(Object o){
        if(o == this) return true;
        if(o == null || o.getClass() != this.getClass()) return false;
        Segment s = (Segment)o;
        return this.a.equals(s.a) && this.b.equals(s.b);
    }

    @Override
    public int hashCode(){
        final int prime = 71;
        int result = 1;
        result = prime * result + this.a.hashCode();
        result = prime * result + this.b.hashCode();
        return result;
    }
}
