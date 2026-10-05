package arc.math.geom;

/**
 * Interface that specifies a path of type T within the window 0.0<=t<=1.0.
 * <p>
 * 定义窗口 0.0<=t<=1.0 内类型为 T 的路径的接口。
 * @author Xoppa
 */
public interface Path<T>{
    T derivativeAt(T out, float t);

    /**
     * @return The value of the path at t where 0<=t<=1
     * 路径在 t 处的值,其中 0<=t<=1
     */
    T valueAt(T out, float t);

    /**
     * @return The approximated value (between 0 and 1) on the path which is closest to the specified value. Note that the 路径上最接近指定值的近似值(0 到 1 之间)。注意
     * implementation of this method might be optimized for speed against precision, see {@link #locate(Object)} for a more
     * precise (but more intensive) method.
     */
    float approximate(T v);

    /**
     * @return The precise location (between 0 and 1) on the path which is closest to the specified value. Note that the 路径上最接近指定值的精确位置(0 到 1 之间)。注意
     * implementation of this method might be CPU intensive, see {@link #approximate(Object)} for a faster (but less
     * precise) method.
     */
    float locate(T v);


    /**
     * @param samples The amount of divisions used to approximate length. Higher values will produce more precise results, 用于近似长度的分割数量。值越大结果越精确,
     * but will be more CPU intensive.
     * @return An approximated length of the spline through sampling the curve and accumulating the euclidean distances between 通过采样曲线并累加各采样点间欧氏距离得到的样条近似长度
     * the sample points.
     */
    float approxLength(int samples);

}