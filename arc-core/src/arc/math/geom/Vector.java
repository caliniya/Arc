package arc.math.geom;

import arc.math.Interp;

/**
 * Encapsulates a general vector. Allows chaining operations by returning a reference to itself in all modification methods. See
 * {@link Vec2} and {@link Vec3} for specific implementations.
 * <p>
 * 封装通用向量。所有修改方法均返回自身引用以支持链式操作。具体实现参见 {@link Vec2} 和 {@link Vec3}。
 * @author Xoppa
 */
public interface Vector<T extends Vector<T>>{
    /**
     * @return a copy of this vector
     * 此向量的副本
     */
    T cpy();

    /**
     * @return The euclidean length
     * 欧氏长度
     */
    float len();

    /**
     * This method is faster than {@link Vector#len()} because it avoids calculating a square root. It is useful for comparisons,
     * but not for getting exact lengths, as the return value is the square of the actual length.
     * <p>
     * 此方法比 {@link Vector#len()} 更快,因为它避免了平方根计算。适合比较,但不适合获得精确长度,因为返回值是实际长度的平方。
     * @return The squared euclidean length 欧氏长度的平方
     */
    float len2();

    /**
     * Limits the length of this vector, based on the desired maximum length.
     * <p>
     * 根据目标最大长度限制此向量的长度。
     * @param limit desired maximum length for this vector 此向量的目标最大长度
     * @return this vector for chaining 此向量,用于链式调用
     */
    T limit(float limit);

    /**
     * Limits the length of this vector, based on the desired maximum length squared.
     * <p/>
     * This method is slightly faster than limit().
     * <p>
     * 根据目标最大长度的平方限制此向量的长度。 <p/> 此方法比 limit() 稍快。
     * @param limit2 squared desired maximum length for this vector 此向量的目标最大长度的平方
     * @return this vector for chaining 此向量,用于链式调用
     * @see #len2()
     */
    T limit2(float limit2);

    /**
     * Sets the length of this vector. Does nothing if this vector is zero.
     * <p>
     * 设置此向量的长度。零向量不做任何操作。
     * @param len desired length for this vector 此向量的目标长度
     * @return this vector for chaining 此向量,用于链式调用
     */
    T setLength(float len);

    /**
     * Sets the length of this vector, based on the square of the desired length. Does nothing if this vector is zero.
     * <p/>
     * This method is slightly faster than setLength().
     * <p>
     * 根据目标长度的平方设置此向量的长度。零向量不做任何操作。 <p/> 此方法比 setLength() 稍快。
     * @param len2 desired square of the length for this vector 此向量的目标长度的平方
     * @return this vector for chaining 此向量,用于链式调用
     * @see #len2()
     */
    T setLength2(float len2);

    /**
     * Clamps this vector's length to given min and max values
     * <p>
     * 将此向量的长度限制到给定的最小值和最大值
     * @param min Min length 最小长度
     * @param max Max length 最大长度
     * @return This vector for chaining 此向量,用于链式调用
     */
    T clamp(float min, float max);

    /**
     * Sets this vector from the given vector
     * <p>
     * 根据给定的向量设置此向量
     * @param v The vector 向量
     * @return This vector for chaining 此向量,用于链式调用
     */
    T set(T v);

    /**
     * Subtracts the given vector from this vector.
     * <p>
     * 从此向量中减去给定的向量。
     * @param v The vector 向量
     * @return This vector for chaining 此向量,用于链式调用
     */
    T sub(T v);

    /**
     * Normalizes this vector. Does nothing if it is zero.
     * <p>
     * 归一化此向量。零向量不做任何操作。
     * @return This vector for chaining 此向量,用于链式调用
     */
    T nor();

    /**
     * Adds the given vector to this vector
     * <p>
     * 将给定向量加到此向量上
     * @param v The vector 向量
     * @return This vector for chaining 此向量,用于链式调用
     */
    T add(T v);

    /**
     * @param v The other vector 另一个向量
     * @return The dot product between this and the other vector 此向量与另一向量之间的点积
     */
    float dot(T v);

    /**
     * Scales this vector by a scalar
     * <p>
     * 按标量缩放此向量
     * @param scalar The scalar 标量
     * @return This vector for chaining 此向量,用于链式调用
     */
    T scl(float scalar);

    /**
     * Scales this vector by another vector
     * <p>
     * 按另一向量缩放此向量
     * @return This vector for chaining 此向量,用于链式调用
     */
    T scl(T v);

    /**
     * Inverse of scl()
     * scl() 的逆操作
     */
    T div(T other);

    /**
     * @param v The other vector 另一个向量
     * @return the distance between this and the other vector 此向量与另一向量之间的距离
     */
    float dst(T v);

    /**
     * This method is faster than {@link Vector#dst(Vector)} because it avoids calculating a square root. It is useful for
     * comparisons, but not for getting accurate distances, as the return value is the square of the actual distance.
     * <p>
     * 此方法比 {@link Vector#dst(Vector)} 更快,因为它避免了平方根计算。适合比较,但不适合获得精确距离,因为返回值是实际距离的平方。
     * @param v The other vector 另一个向量
     * @return the squared distance between this and the other vector 此向量与另一向量之间距离的平方
     */
    float dst2(T v);

    /**
     * Linearly interpolates between this vector and the target vector by alpha which is in the range [0,1]. The result is stored
     * in this vector.
     * <p>
     * 在此向量与目标向量之间按 alpha(范围 [0,1])进行线性插值。结果存入此向量。
     * @param target The target vector 目标向量
     * @param alpha The interpolation coefficient 插值系数
     * @return This vector for chaining. 此向量,用于链式调用。
     */
    T lerp(T target, float alpha);

    /**
     * Interpolates between this vector and the given target vector by alpha (within range [0,1]) using the given Interpolation
     * method. the result is stored in this vector.
     * <p>
     * 使用给定的 Interpolation 方法,按 alpha(范围 [0,1])在原向量和给定目标向量之间插值。结果存入原向量。
     * @param target The target vector 目标向量
     * @param alpha The interpolation coefficient 插值系数
     * @param interpolator An Interpolation object describing the used interpolation method 描述所用插值方法的 Interpolation 对象
     * @return This vector for chaining. 此向量,用于链式调用。
     */
    T interpolate(T target, float alpha, Interp interpolator);

    /**
     * Sets this vector to the unit vector with a random direction
     * <p>
     * 将此向量设置为随机方向的单位向量
     * @return This vector for chaining 此向量,用于链式调用
     */
    T setToRandomDirection();

    /**
     * @return Whether this vector is a unit length vector
     * 此向量是否为单位长度向量
     */
    boolean isUnit();

    /**
     * @return Whether this vector is a unit length vector within the given margin.
     * 此向量是否为给定余量内的单位长度向量。
     */
    boolean isUnit(final float margin);

    /**
     * @return Whether this vector is a zero vector
     * 此向量是否为零向量
     */
    boolean isZero();

    /**
     * @return Whether the length of this vector is smaller than the given margin
     * 此向量的长度是否小于给定的余量
     */
    boolean isZero(final float margin);

    /**
     * @return true if this vector is in line with the other vector (either in the same or the opposite direction)
     * 此向量是否与另一向量共线(同向或反向)
     */
    boolean isOnLine(T other, float epsilon);

    /**
     * @return true if this vector is in line with the other vector (either in the same or the opposite direction)
     * 此向量是否与另一向量共线(同向或反向)
     */
    boolean isOnLine(T other);

    /**
     * @return true if this vector is collinear with the other vector ({@link #isOnLine(Vector, float)} && 此向量是否与另一向量共线({@link #isOnLine(Vector, float)} &&
     * {@link #hasSameDirection(Vector)}).
     */
    boolean isCollinear(T other, float epsilon);

    /**
     * @return true if this vector is collinear with the other vector ({@link #isOnLine(Vector)} && 此向量是否与另一向量共线({@link #isOnLine(Vector)} &&
     * {@link #hasSameDirection(Vector)}).
     */
    boolean isCollinear(T other);

    /**
     * @return true if this vector is opposite collinear with the other vector ({@link #isOnLine(Vector, float)} && 此向量是否与另一向量反向共线({@link #isOnLine(Vector, float)} &&
     * {@link #hasOppositeDirection(Vector)}).
     */
    boolean isCollinearOpposite(T other, float epsilon);

    /**
     * @return true if this vector is opposite collinear with the other vector ({@link #isOnLine(Vector)} && 此向量是否与另一向量反向共线({@link #isOnLine(Vector)} &&
     * {@link #hasOppositeDirection(Vector)}).
     */
    boolean isCollinearOpposite(T other);

    /**
     * @return Whether this vector is perpendicular with the other vector. True if the dot product is 0.
     * 此向量是否与另一向量垂直。点积为 0 则为 true。
     */
    boolean isPerpendicular(T other);

    /**
     * @param epsilon a positive small number close to zero 接近零的正小数
     * @return Whether this vector is perpendicular with the other vector. True if the dot product is 0. 此向量是否与另一向量垂直。点积为 0 则为 true。
     */
    boolean isPerpendicular(T other, float epsilon);

    /**
     * @return Whether this vector has similar direction compared to the other vector. True if the normalized dot product is > 0.
     * 此向量与另一向量方向是否相近。若归一化点积 > 0 则为 true。
     */
    boolean hasSameDirection(T other);

    /**
     * @return Whether this vector has opposite direction compared to the other vector. True if the normalized dot product is < 0.
     * 此向量与另一向量方向是否相反。若归一化点积 < 0 则为 true。
     */
    boolean hasOppositeDirection(T other);

    /**
     * Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
     * <p>
     * 将此向量与另一向量比较,使用给定的 epsilon 进行近似相等测试。
     * @return whether the vectors have fuzzy equality. 两向量是否近似相等。
     */
    boolean epsilonEquals(T other, float epsilon);

    /**
     * First scale a supplied vector, then add it to this vector.
     * <p>
     * 先缩放给定向量,然后将其加到此向量上。
     * @param v addition vector 加法向量
     * @param scalar for scaling the addition vector 用于缩放加法向量的标量
     */
    T mulAdd(T v, float scalar);

    /**
     * First scale a supplied vector, then add it to this vector.
     * <p>
     * 先缩放给定向量,然后将其加到此向量上。
     * @param v addition vector 加法向量
     * @param mulVec vector by whose values the addition vector will be scaled 其分量值用于缩放加法向量的向量
     */
    T mulAdd(T v, T mulVec);

    /**
     * Sets the components of this vector to 0
     * <p>
     * 将此向量的各分量设为 0
     * @return This vector for chaining 此向量,用于链式调用
     */
    T setZero();

    default T plus(T other){
        return add(other);
    }

    default T minus(T other){
        return sub(other);
    }

    default T unaryMinus(){
        return scl(-1);
    }

    default T times(T other){
        return scl(other);
    }
}
