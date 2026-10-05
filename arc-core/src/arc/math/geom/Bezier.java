package arc.math.geom;

import arc.struct.Ar;
import arc.math.Mathf;
import arc.util.ArcRuntimeException;

/**
 * Implementation of the Bezier curve.
 * <p>
 * 贝塞尔曲线的实现。
 * @author Xoppa
 */
public class Bezier<T extends Vector<T>> implements Path<T>{
    public Ar<T> points = new Ar<>();
    private T tmp;
    private T tmp2;
    private T tmp3;

    public Bezier(){
    }

    public Bezier(final T... points){
        set(points);
    }

    public Bezier(final T[] points, final int offset, final int length){
        set(points, offset, length);
    }
    public Bezier(final Ar<T> points, final int offset, final int length){
        set(points, offset, length);
    }

    /**
     * Simple Linear interpolation
     * <p>
     * 简单线性插值
     * @param out The {@link Vector} to set to the result. 用于保存结果的 {@link Vector}。
     * @param t The location (ranging 0..1) on the line. 直线上的位置(范围 0..1)。
     * @param p0 The start point. 起点。
     * @param p1 The end point. 终点。
     * @param tmp A temporary vector to be used by the calculation. 计算过程中使用的临时向量。
     * @return The value specified by out for chaining 用于链式调用的 out 指定的值
     */
    public static <T extends Vector<T>> T linear(final T out, final float t, final T p0, final T p1, final T tmp){
        // B1(t) = p0 + (p1-p0)*t
        // 一阶贝塞尔曲线公式
        return out.set(p0).scl(1f - t).add(tmp.set(p1).scl(t)); // Could just use lerp...
        // 其实直接用 lerp 就行……
    }

    /**
     * Simple Linear interpolation derivative
     * <p>
     * 简单线性插值导数
     * @param out The {@link Vector} to set to the result. 用于保存结果的 {@link Vector}。
     * @param t The location (ranging 0..1) on the line. 直线上的位置(范围 0..1)。
     * @param p0 The start point. 起点。
     * @param p1 The end point. 终点。
     * @param tmp A temporary vector to be used by the calculation. 计算过程中使用的临时向量。
     * @return The value specified by out for chaining 用于链式调用的 out 指定的值
     */
    public static <T extends Vector<T>> T linearDerivative(final T out, final float t, final T p0, final T p1, final T tmp){
        // B1'(t) = p1-p0
        // 一阶贝塞尔曲线的导数公式
        return out.set(p1).sub(p0);
    }

    /**
     * Quadratic Bezier curve
     * <p>
     * 二次贝塞尔曲线
     * @param out The {@link Vector} to set to the result. 用于保存结果的 {@link Vector}。
     * @param t The location (ranging 0..1) on the curve. 曲线上的位置(范围 0..1)。
     * @param p0 The first bezier point. 第一个贝塞尔点。
     * @param p1 The second bezier point. 第二个贝塞尔点。
     * @param p2 The third bezier point. 第三个贝塞尔点。
     * @param tmp A temporary vector to be used by the calculation. 计算过程中使用的临时向量。
     * @return The value specified by out for chaining 用于链式调用的 out 指定的值
     */
    public static <T extends Vector<T>> T quadratic(final T out, final float t, final T p0, final T p1, final T p2, final T tmp){
        // B2(t) = (1 - t) * (1 - t) * p0 + 2 * (1-t) * t * p1 + t*t*p2
        // 二阶贝塞尔曲线公式
        final float dt = 1f - t;
        return out.set(p0).scl(dt * dt).add(tmp.set(p1).scl(2 * dt * t)).add(tmp.set(p2).scl(t * t));
    }

    /**
     * Quadratic Bezier curve derivative
     * <p>
     * 二次贝塞尔曲线导数
     * @param out The {@link Vector} to set to the result. 用于保存结果的 {@link Vector}。
     * @param t The location (ranging 0..1) on the curve. 曲线上的位置(范围 0..1)。
     * @param p0 The first bezier point. 第一个贝塞尔点。
     * @param p1 The second bezier point. 第二个贝塞尔点。
     * @param p2 The third bezier point. 第三个贝塞尔点。
     * @param tmp A temporary vector to be used by the calculation. 计算过程中使用的临时向量。
     * @return The value specified by out for chaining 用于链式调用的 out 指定的值
     */
    public static <T extends Vector<T>> T quadraticDerivative(final T out, final float t, final T p0, final T p1, final T p2,
                                                              final T tmp){
        // B2'(t) = 2 * (1 - t) * (p1 - p0) + 2 * t * (p2 - p1)
        // 二阶贝塞尔曲线的导数公式
        final float dt = 1f - t;
        return out.set(p1).sub(p0).scl(2).scl(1 - t).add(tmp.set(p2).sub(p1).scl(t).scl(2));
    }

    /**
     * Cubic Bezier curve
     * <p>
     * 三次贝塞尔曲线
     * @param out The {@link Vector} to set to the result. 用于保存结果的 {@link Vector}。
     * @param t The location (ranging 0..1) on the curve. 曲线上的位置(范围 0..1)。
     * @param p0 The first bezier point. 第一个贝塞尔点。
     * @param p1 The second bezier point. 第二个贝塞尔点。
     * @param p2 The third bezier point. 第三个贝塞尔点。
     * @param p3 The fourth bezier point. 第四个贝塞尔点。
     * @param tmp A temporary vector to be used by the calculation. 计算过程中使用的临时向量。
     * @return The value specified by out for chaining 用于链式调用的 out 指定的值
     */
    public static <T extends Vector<T>> T cubic(final T out, final float t, final T p0, final T p1, final T p2, final T p3,
                                                final T tmp){
        // B3(t) = (1-t) * (1-t) * (1-t) * p0 + 3 * (1-t) * (1-t) * t * p1 + 3 * (1-t) * t * t * p2 + t * t * t * p3
        // 三阶贝塞尔曲线公式
        final float dt = 1f - t;
        final float dt2 = dt * dt;
        final float t2 = t * t;
        return out.set(p0).scl(dt2 * dt).add(tmp.set(p1).scl(3 * dt2 * t)).add(tmp.set(p2).scl(3 * dt * t2))
        .add(tmp.set(p3).scl(t2 * t));
    }

    /**
     * Cubic Bezier curve derivative
     * <p>
     * 三次贝塞尔曲线导数
     * @param out The {@link Vector} to set to the result. 用于保存结果的 {@link Vector}。
     * @param t The location (ranging 0..1) on the curve. 曲线上的位置(范围 0..1)。
     * @param p0 The first bezier point. 第一个贝塞尔点。
     * @param p1 The second bezier point. 第二个贝塞尔点。
     * @param p2 The third bezier point. 第三个贝塞尔点。
     * @param p3 The fourth bezier point. 第四个贝塞尔点。
     * @param tmp A temporary vector to be used by the calculation. 计算过程中使用的临时向量。
     * @return The value specified by out for chaining 用于链式调用的 out 指定的值
     */
    public static <T extends Vector<T>> T cubicDerivative(final T out, final float t, final T p0, final T p1, final T p2,
                                                          final T p3, final T tmp){
        // B3'(t) = 3 * (1-t) * (1-t) * (p1 - p0) + 6 * (1 - t) * t * (p2 - p1) + 3 * t * t * (p3 - p2)
        // 三阶贝塞尔曲线的导数公式
        final float dt = 1f - t;
        final float dt2 = dt * dt;
        final float t2 = t * t;
        return out.set(p1).sub(p0).scl(dt2 * 3).add(tmp.set(p2).sub(p1).scl(dt * t * 6)).add(tmp.set(p3).sub(p2).scl(t2 * 3));
    }

    public Bezier<T> set(final T... points){
        return set(points, 0, points.length);
    }

    public Bezier<T> set(final T[] points, final int offset, final int length){
        if(length < 2 || length > 4)
            throw new ArcRuntimeException("Only first, second and third degree Bezier curves are supported.");
        if(tmp == null) tmp = points[0].cpy();
        if(tmp2 == null) tmp2 = points[0].cpy();
        if(tmp3 == null) tmp3 = points[0].cpy();
        this.points.clear();
        this.points.addAll(points, offset, length);
        return this;
    }

    public Bezier<T> set(T p1, T p2, T p3){
        if(tmp == null) tmp = p1.cpy();
        if(tmp2 == null) tmp2 = p2.cpy();
        if(tmp3 == null) tmp3 = p3.cpy();
        this.points.clear();
        this.points.add(p1, p2, p3);
        return this;
    }

    public Bezier<T> set(final Ar<T> points){
        return set(points, 0, points.size);
    }

    public Bezier<T> set(final Ar<T> points, final int offset, final int length){
        if(length < 2 || length > 4)
            throw new ArcRuntimeException("Only first, second and third degree Bezier curves are supported.");
        if(tmp == null) tmp = points.get(0).cpy();
        if(tmp2 == null) tmp2 = points.get(0).cpy();
        if(tmp3 == null) tmp3 = points.get(0).cpy();
        this.points.clear();
        this.points.addAll(points, offset, length);
        return this;
    }

    @Override
    public T valueAt(final T out, final float t){
        final int n = points.size;
        if(n == 2)
            linear(out, t, points.get(0), points.get(1), tmp);
        else if(n == 3)
            quadratic(out, t, points.get(0), points.get(1), points.get(2), tmp);
        else if(n == 4) cubic(out, t, points.get(0), points.get(1), points.get(2), points.get(3), tmp);
        return out;
    }

    @Override
    public T derivativeAt(final T out, final float t){
        final int n = points.size;
        if(n == 2)
            linearDerivative(out, t, points.get(0), points.get(1), tmp);
        else if(n == 3)
            quadraticDerivative(out, t, points.get(0), points.get(1), points.get(2), tmp);
        else if(n == 4) cubicDerivative(out, t, points.get(0), points.get(1), points.get(2), points.get(3), tmp);
        return out;
    }

    @Override
    public float approximate(final T v){
        // TODO: make a real approximate method
        // TODO:做一个真正的近似方法
        T p1 = points.get(0);
        T p2 = points.get(points.size - 1);
        float l1Sqr = p1.dst2(p2);
        float l2Sqr = v.dst2(p2);
        float l3Sqr = v.dst2(p1);
        float l1 = (float)Math.sqrt(l1Sqr);
        float s = (l2Sqr + l1Sqr - l3Sqr) / (2 * l1);
        return Mathf.clamp((l1 - s) / l1, 0f, 1f);
    }

    @Override
    public float locate(T v){
        // TODO implement a precise method
        // TODO 实现精确方法
        return approximate(v);
    }

    @Override
    public float approxLength(int samples){
        float tempLength = 0;
        for(int i = 0; i < samples; ++i){
            tmp2.set(tmp3);
            valueAt(tmp3, (i) / ((float)samples - 1));
            if(i > 0) tempLength += tmp2.dst(tmp3);
        }
        return tempLength;
    }
}
