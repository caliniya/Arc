package arc.math.geom;

import arc.math.*;
import arc.util.*;

/**
 * Encapsulates a 3D vector. Allows chaining operations by returning a reference to itself in all modification methods.
 * <p>
 * 封装 3D 向量。所有修改方法均返回自身引用以支持链式操作。
 * @author badlogicgames@gmail.com
 */
public class Vec3 implements Vector<Vec3>{
    public final static Vec3 X = new Vec3(1, 0, 0);
    public final static Vec3 Y = new Vec3(0, 1, 0);
    public final static Vec3 Z = new Vec3(0, 0, 1);
    public final static Vec3 Zero = new Vec3(0, 0, 0);
    private static final Mat tmpMat = new Mat();
    /**
     * the x-component of this vector
     * 此向量的 x 分量
     */
    public float x;
    /**
     * the y-component of this vector
     * 此向量的 y 分量
     */
    public float y;
    /**
     * the z-component of this vector
     * 此向量的 z 分量
     */
    public float z;

    /**
     * Constructs a vector at (0,0,0)
     * 在 (0,0,0) 处构造向量
     */
    public Vec3(){
    }

    /**
     * Creates a vector with the given components
     * <p>
     * 用给定的分量创建向量
     * @param x The x-component x 分量
     * @param y The y-component y 分量
     * @param z The z-component z 分量
     */
    public Vec3(float x, float y, float z){
        this.set(x, y, z);
    }

    public Vec3(double x, double y, double z){
        this((float)x, (float)y, (float)z);
    }

    /**
     * Creates a vector from the given vector
     * <p>
     * 根据给定向量创建向量
     * @param vector The vector 向量
     */
    public Vec3(Vec3 vector){
        this.set(vector);
    }

    /**
     * Creates a vector from the given array. The array must have at least 3 elements.
     * <p>
     * 根据给定数组创建向量。数组必须至少有 3 个元素。
     * @param values The array 数组
     */
    public Vec3(float[] values){
        this.set(values[0], values[1], values[2]);
    }

    /**
     * Creates a vector from the given vector and z-component
     * <p>
     * 根据给定向量和 z 分量创建向量
     * @param vector The vector 向量
     * @param z The z-component z 分量
     */
    public Vec3(Vec2 vector, float z){
        this.set(vector.x, vector.y, z);
    }

    /**
     * @return The euclidean length
     * 欧氏长度
     */
    public static float len(float x, float y, float z){
        return (float)Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * @return The squared euclidean length
     * 欧氏长度的平方
     */
    public static float len2(final float x, final float y, final float z){
        return x * x + y * y + z * z;
    }

    /**
     * @return The euclidean distance between the two specified vectors
     * 两指定向量之间的欧氏距离
     */
    public static float dst(final float x1, final float y1, final float z1, final float x2, final float y2, final float z2){
        final float a = x2 - x1;
        final float b = y2 - y1;
        final float c = z2 - z1;
        return (float)Math.sqrt(a * a + b * b + c * c);
    }

    /**
     * @return the squared distance between the given points
     * 给定两点之间距离的平方
     */
    public static float dst2(final float x1, final float y1, final float z1, final float x2, final float y2, final float z2){
        final float a = x2 - x1;
        final float b = y2 - y1;
        final float c = z2 - z1;
        return a * a + b * b + c * c;
    }

    /**
     * @return The dot product between the two vectors
     * 两向量之间的点积
     */
    public static float dot(float x1, float y1, float z1, float x2, float y2, float z2){
        return x1 * x2 + y1 * y2 + z1 * z2;
    }

    /**
     * Sets the vector to the given components
     * <p>
     * 将向量设置为给定的分量
     * @param x The x-component x 分量
     * @param y The y-component y 分量
     * @param z The z-component z 分量
     * @return this vector for chaining 此向量,用于链式调用
     */
    public Vec3 set(float x, float y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    @Override
    public Vec3 div(Vec3 other){
        x /= other.x;
        y /= other.y;
        z /= other.z;

        return this;
    }

    @Override
    public Vec3 set(final Vec3 vector){
        return this.set(vector.x, vector.y, vector.z);
    }

    /**
     * Sets the components from the array. The array must have at least 3 elements
     * <p>
     * 根据数组设置各分量。数组必须至少有 3 个元素
     * @param values The array 数组
     * @return this vector for chaining 此向量,用于链式调用
     */
    public Vec3 set(final float[] values){
        return this.set(values[0], values[1], values[2]);
    }

    public Vec3 set(final float[] values, int offset){
        return this.set(values[offset], values[offset + 1], values[offset + 2]);
    }

    /**
     * Sets the components of the given vector and z-component
     * <p>
     * 根据给定向量和 z 分量设置各分量
     * @param vector The vector 向量
     * @param z The z-component z 分量
     * @return This vector for chaining 此向量,用于链式调用
     */
    public Vec3 set(final Vec2 vector, float z){
        return this.set(vector.x, vector.y, z);
    }

    /**
     * Sets the components from the given spherical coordinate
     * <p>
     * 根据给定的球面坐标设置各分量
     * @param azimuthalAngle The angle between x-axis in radians [0, 2pi] 方位角,与 x 轴的夹角,单位弧度,范围 [0, 2pi]
     * @param polarAngle The angle between z-axis in radians [0, pi] 极角,与 z 轴的夹角,单位弧度,范围 [0, pi]
     * @return This vector for chaining 此向量,用于链式调用
     */
    public Vec3 setFromSpherical(float azimuthalAngle, float polarAngle){
        float cosPolar = Mathf.cos(polarAngle);
        float sinPolar = Mathf.sin(polarAngle);

        float cosAzim = Mathf.cos(azimuthalAngle);
        float sinAzim = Mathf.sin(azimuthalAngle);

        return this.set(cosAzim * sinPolar, sinAzim * sinPolar, cosPolar);
    }

    @Override
    public Vec3 setToRandomDirection(){
        return setToRandomDirection(Mathf.rand);
    }

    public Vec3 setToRandomDirection(Rand rand){
        float u = rand.random(1f);
        float v = rand.random(1f);

        float theta = Mathf.PI2 * u; // azimuthal angle
        // 方位角
        float phi = (float)Math.acos(2f * v - 1f); // polar angle
        // 极角

        return this.setFromSpherical(theta, phi);
    }

    @Override
    public Vec3 cpy(){
        return new Vec3(this);
    }

    @Override
    public Vec3 add(final Vec3 vector){
        return this.add(vector.x, vector.y, vector.z);
    }

    public Vec3 cpy(Vec3 dest){
        return dest.set(this);
    }

    public Vec3 add(Vec3 vector, float scale){
        return this.add(vector.x * scale, vector.y * scale, vector.z * scale);
    }

    public Vec3 sub(Vec3 vector, float scale){
        return this.sub(vector.x * scale, vector.y * scale, vector.z * scale);
    }

    /**
     * Adds the given vector to this component
     * <p>
     * 将给定向量加到此分量上
     * @param x The x-component of the other vector 另一个向量的 x 分量
     * @param y The y-component of the other vector 另一个向量的 y 分量
     * @param z The z-component of the other vector 另一个向量的 z 分量
     * @return This vector for chaining. 此向量,用于链式调用。
     */
    public Vec3 add(float x, float y, float z){
        return this.set(this.x + x, this.y + y, this.z + z);
    }

    /**
     * Adds the given value to all three components of the vector.
     * <p>
     * 将给定值加到向量的所有三个分量上。
     * @param values The value 值
     * @return This vector for chaining 此向量,用于链式调用
     */
    public Vec3 add(float values){
        return this.set(this.x + values, this.y + values, this.z + values);
    }

    @Override
    public Vec3 sub(final Vec3 a_vec){
        return this.sub(a_vec.x, a_vec.y, a_vec.z);
    }

    /**
     * Subtracts the other vector from this vector.
     * <p>
     * 从此向量中减去另一向量。
     * @param x The x-component of the other vector 另一个向量的 x 分量
     * @param y The y-component of the other vector 另一个向量的 y 分量
     * @param z The z-component of the other vector 另一个向量的 z 分量
     * @return This vector for chaining 此向量,用于链式调用
     */
    public Vec3 sub(float x, float y, float z){
        return this.set(this.x - x, this.y - y, this.z - z);
    }

    /**
     * Subtracts the given value from all components of this vector
     * <p>
     * 从此向量的所有分量中减去给定值
     * @param value The value 值
     * @return This vector for chaining 此向量,用于链式调用
     */
    public Vec3 sub(float value){
        return this.set(this.x - value, this.y - value, this.z - value);
    }

    @Override
    public Vec3 scl(float scalar){
        return this.set(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    @Override
    public Vec3 scl(final Vec3 other){
        return this.set(x * other.x, y * other.y, z * other.z);
    }

    /**
     * Scales this vector by the given values
     * <p>
     * 按给定值缩放此向量
     * @param vx X value X 值
     * @param vy Y value Y 值
     * @param vz Z value Z 值
     * @return This vector for chaining 此向量,用于链式调用
     */
    public Vec3 scl(float vx, float vy, float vz){
        return this.set(this.x * vx, this.y * vy, this.z * vz);
    }

    @Override
    public Vec3 mulAdd(Vec3 vec, float scalar){
        this.x += vec.x * scalar;
        this.y += vec.y * scalar;
        this.z += vec.z * scalar;
        return this;
    }

    @Override
    public Vec3 mulAdd(Vec3 vec, Vec3 mulVec){
        this.x += vec.x * mulVec.x;
        this.y += vec.y * mulVec.y;
        this.z += vec.z * mulVec.z;
        return this;
    }

    @Override
    public float len(){
        return (float)Math.sqrt(x * x + y * y + z * z);
    }

    @Override
    public float len2(){
        return x * x + y * y + z * z;
    }

    /**
     * @param vector The other vector 另一个向量
     * @return Whether this and the other vector are equal 此向量与另一向量是否相等
     */
    public boolean idt(final Vec3 vector){
        return x == vector.x && y == vector.y && z == vector.z;
    }

    @Override
    public float dst(final Vec3 vector){
        final float a = vector.x - x;
        final float b = vector.y - y;
        final float c = vector.z - z;
        return (float)Math.sqrt(a * a + b * b + c * c);
    }

    /**
     * @return the distance between this point and the given point
     * 此点与给定点之间的距离
     */
    public float dst(float x, float y, float z){
        final float a = x - this.x;
        final float b = y - this.y;
        final float c = z - this.z;
        return (float)Math.sqrt(a * a + b * b + c * c);
    }

    @Override
    public float dst2(Vec3 point){
        final float a = point.x - x;
        final float b = point.y - y;
        final float c = point.z - z;
        return a * a + b * b + c * c;
    }

    /**
     * Returns the squared distance between this point and the given point
     * <p>
     * 返回此点与给定点之间距离的平方
     * @param x The x-component of the other point 另一个点的 x 分量
     * @param y The y-component of the other point 另一个点的 y 分量
     * @param z The z-component of the other point 另一个点的 z 分量
     * @return The squared distance 距离的平方
     */
    public float dst2(float x, float y, float z){
        final float a = x - this.x;
        final float b = y - this.y;
        final float c = z - this.z;
        return a * a + b * b + c * c;
    }

    public boolean within(Vec3 v, float dst){
        return dst2(v) < dst * dst;
    }

    @Override
    public Vec3 nor(){
        final float len2 = this.len2();
        if(len2 == 0f || len2 == 1f) return this;
        return this.scl(1f / (float)Math.sqrt(len2));
    }

    @Override
    public float dot(final Vec3 vector){
        return x * vector.x + y * vector.y + z * vector.z;
    }

    /**
     * @return the angle to the other vector, in radians.
     * 到另一向量的以弧度表示的角度。
     */
    public float angleRad(final Vec3 vector){
        float l = len();
        float l2 = vector.len();
        return (float)Math.acos(dot(x / l, y / l, z / l, vector.x / l2, vector.y / l2, vector.z / l2));
    }

    /**
     * @return the angle to the other vector, in degrees.
     * 到另一向量的以度表示的角度。
     */
    public float angle(final Vec3 vector){
        return angleRad(vector) * Mathf.radDeg;
    }

    /**
     * Returns the dot product between this and the given vector.
     * <p>
     * 返回此向量与给定向量之间的点积。
     * @param x The x-component of the other vector 另一个向量的 x 分量
     * @param y The y-component of the other vector 另一个向量的 y 分量
     * @param z The z-component of the other vector 另一个向量的 z 分量
     * @return The dot product 点积
     */
    public float dot(float x, float y, float z){
        return this.x * x + this.y * y + this.z * z;
    }

    /**
     * Sets this vector to the cross product between it and the other vector.
     * <p>
     * 将此向量设置为它与另一向量之间的叉积。
     * @param vector The other vector 另一个向量
     * @return This vector for chaining 此向量,用于链式调用
     */
    public Vec3 crs(final Vec3 vector){
        return this.set(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x);
    }

    /**
     * Sets this vector to the cross product between it and the other vector.
     * <p>
     * 将此向量设置为它与另一向量之间的叉积。
     * @param x The x-component of the other vector 另一个向量的 x 分量
     * @param y The y-component of the other vector 另一个向量的 y 分量
     * @param z The z-component of the other vector 另一个向量的 z 分量
     * @return This vector for chaining 此向量,用于链式调用
     */
    public Vec3 crs(float x, float y, float z){
        return this.set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x);
    }

    /**
     * Left-multiplies the vector by the given 4x3 column major matrix. The matrix should be composed by a 3x3 matrix representing
     * rotation and scale plus a 1x3 matrix representing the translation.
     * <p>
     * 将向量左乘给定的 4x3 列主序矩阵。该矩阵应由表示旋转和缩放的 3x3 矩阵与表示平移的 1x3 矩阵组成。
     * @param matrix The matrix 矩阵
     * @return This vector for chaining 此向量,用于链式调用
     */
    public Vec3 mul4x3(float[] matrix){
        return set(x * matrix[0] + y * matrix[3] + z * matrix[6] + matrix[9], x * matrix[1] + y * matrix[4] + z * matrix[7]
        + matrix[10], x * matrix[2] + y * matrix[5] + z * matrix[8] + matrix[11]);
    }

    /**
     * Left-multiplies the vector by the given matrix.
     * <p>
     * 将向量左乘给定矩阵。
     * @param matrix The matrix 矩阵
     * @return This vector for chaining 此向量,用于链式调用
     */
    public Vec3 mul(Mat matrix){
        final float[] l_mat = matrix.val;
        return set(x * l_mat[Mat.M00] + y * l_mat[Mat.M01] + z * l_mat[Mat.M02], x * l_mat[Mat.M10] + y
        * l_mat[Mat.M11] + z * l_mat[Mat.M12], x * l_mat[Mat.M20] + y * l_mat[Mat.M21] + z * l_mat[Mat.M22]);
    }

    /**
     * Multiplies the vector by the transpose of the given matrix.
     * <p>
     * 将向量与给定矩阵的转置相乘。
     * @param matrix The matrix 矩阵
     * @return This vector for chaining 此向量,用于链式调用
     */
    public Vec3 traMul(Mat matrix){
        final float[] l_mat = matrix.val;
        return set(x * l_mat[Mat.M00] + y * l_mat[Mat.M10] + z * l_mat[Mat.M20], x * l_mat[Mat.M01] + y
        * l_mat[Mat.M11] + z * l_mat[Mat.M21], x * l_mat[Mat.M02] + y * l_mat[Mat.M12] + z * l_mat[Mat.M22]);
    }

    /**
     * Rotates this vector by the given angle in degrees around the given axis.
     * <p>
     * 将此向量绕给定轴按给定角度(单位为度)旋转。
     * @param axis the axis 轴
     * @param degrees the angle in degrees 以度表示的角度
     * @return This vector for chaining 此向量,用于链式调用
     */
    public Vec3 rotate(final Vec3 axis, float degrees){
        tmpMat.setToRotation(axis, degrees);
        return this.mul(tmpMat);
    }

    @Override
    public boolean isUnit(){
        return isUnit(0.000000001f);
    }

    @Override
    public boolean isUnit(final float margin){
        return Math.abs(len2() - 1f) < margin;
    }

    @Override
    public boolean isZero(){
        return x == 0 && y == 0 && z == 0;
    }

    @Override
    public boolean isZero(final float margin){
        return len2() < margin;
    }

    @Override
    public boolean isOnLine(Vec3 other, float epsilon){
        return len2(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= epsilon;
    }

    @Override
    public boolean isOnLine(Vec3 other){
        return len2(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= Mathf.FLOAT_ROUNDING_ERROR;
    }

    @Override
    public boolean isCollinear(Vec3 other, float epsilon){
        return isOnLine(other, epsilon) && hasSameDirection(other);
    }

    @Override
    public boolean isCollinear(Vec3 other){
        return isOnLine(other) && hasSameDirection(other);
    }

    @Override
    public boolean isCollinearOpposite(Vec3 other, float epsilon){
        return isOnLine(other, epsilon) && hasOppositeDirection(other);
    }

    @Override
    public boolean isCollinearOpposite(Vec3 other){
        return isOnLine(other) && hasOppositeDirection(other);
    }

    @Override
    public boolean isPerpendicular(Vec3 vector){
        return Mathf.zero(dot(vector));
    }

    @Override
    public boolean isPerpendicular(Vec3 vector, float epsilon){
        return Mathf.zero(dot(vector), epsilon);
    }

    @Override
    public boolean hasSameDirection(Vec3 vector){
        return dot(vector) > 0;
    }

    @Override
    public boolean hasOppositeDirection(Vec3 vector){
        return dot(vector) < 0;
    }

    @Override
    public Vec3 lerp(final Vec3 target, float alpha){
        x += alpha * (target.x - x);
        y += alpha * (target.y - y);
        z += alpha * (target.z - z);
        return this;
    }

    @Override
    public Vec3 interpolate(Vec3 target, float alpha, Interp interpolator){
        return lerp(target, interpolator.apply(0f, 1f, alpha));
    }

    /**
     * Spherically interpolates between this vector and the target vector by alpha which is in the range [0,1]. The result is
     * stored in this vector.
     * <p>
     * 在此向量与目标向量之间按 alpha(范围 [0,1])进行球面插值。结果存入此向量。
     * @param target The target vector 目标向量
     * @param alpha The interpolation coefficient 插值系数
     * @return This vector for chaining. 此向量,用于链式调用。
     */
    public Vec3 slerp(final Vec3 target, float alpha){
        final float dot = dot(target);
        // If the inputs are too close for comfort, simply linearly interpolate.
        // 如果输入过于接近,则直接进行线性插值。
        if(dot > 0.9995 || dot < -0.9995) return lerp(target, alpha);

        // theta0 = angle between input vectors
        // theta0 = 输入向量之间的角度
        final float theta0 = (float)Math.acos(dot);
        // theta = angle between this vector and result
        // theta = 此向量与结果之间的角度
        final float theta = theta0 * alpha;

        final float st = (float)Math.sin(theta);
        final float tx = target.x - x * dot;
        final float ty = target.y - y * dot;
        final float tz = target.z - z * dot;
        final float l2 = tx * tx + ty * ty + tz * tz;
        final float dl = st * ((l2 < 0.0001f) ? 1f : 1f / (float)Math.sqrt(l2));

        return scl((float)Math.cos(theta)).add(tx * dl, ty * dl, tz * dl).nor();
    }

    /**
     * Converts this {@code Vec3} to a string in the format {@code (x,y,z)}.
     * <p>
     * 将此 {@code Vec3} 转换为 {@code (x,y,z)} 格式的字符串。
     * @return a string representation of this object. 此对象的字符串表示。
     */
    @Override
    public String toString(){
        return "(" + x + "," + y + "," + z + ")";
    }

    /**
     * Sets this {@code Vec3} to the value represented by the specified string according to the format of {@link #toString()}.
     * <p>
     * 按照 {@link #toString()} 的格式,将此 {@code Vec3} 设置为由指定字符串表示的值。
     * @param v the string. 字符串。
     * @return this vector for chaining 此向量,用于链式调用
     */
    public Vec3 fromString(String v){
        int s0 = v.indexOf(',', 1);
        int s1 = v.indexOf(',', s0 + 1);
        if(s0 != -1 && s1 != -1 && v.charAt(0) == '(' && v.charAt(v.length() - 1) == ')'){
            try{
                float x = Float.parseFloat(v.substring(1, s0));
                float y = Float.parseFloat(v.substring(s0 + 1, s1));
                float z = Float.parseFloat(v.substring(s1 + 1, v.length() - 1));
                return this.set(x, y, z);
            }catch(NumberFormatException ex){
                // Throw a ArcRuntimeException
                // 抛出 ArcRuntimeException
            }
        }
        throw new ArcRuntimeException("Malformed Vec3: " + v);
    }

    @Override
    public Vec3 limit(float limit){
        return limit2(limit * limit);
    }

    @Override
    public Vec3 limit2(float limit2){
        float len2 = len2();
        if(len2 > limit2){
            scl((float)Math.sqrt(limit2 / len2));
        }
        return this;
    }

    @Override
    public Vec3 setLength(float len){
        return setLength2(len * len);
    }

    @Override
    public Vec3 setLength2(float len2){
        float oldLen2 = len2();
        return (oldLen2 == 0 || oldLen2 == len2) ? this : scl((float)Math.sqrt(len2 / oldLen2));
    }

    @Override
    public Vec3 clamp(float min, float max){
        final float len2 = len2();
        if(len2 == 0f) return this;
        float max2 = max * max;
        if(len2 > max2) return scl((float)Math.sqrt(max2 / len2));
        float min2 = min * min;
        if(len2 < min2) return scl((float)Math.sqrt(min2 / len2));
        return this;
    }

    @Override
    public int hashCode(){
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(x);
        result = prime * result + Float.floatToIntBits(y);
        result = prime * result + Float.floatToIntBits(z);
        return result;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        Vec3 other = (Vec3)obj;
        return Float.floatToIntBits(x) == Float.floatToIntBits(other.x) && Float.floatToIntBits(y) == Float.floatToIntBits(other.y) && Float.floatToIntBits(z) == Float.floatToIntBits(other.z);
    }

    @Override
    public boolean epsilonEquals(final Vec3 other, float epsilon){
        if(other == null) return false;
        if(Math.abs(other.x - x) > epsilon) return false;
        if(Math.abs(other.y - y) > epsilon) return false;
        return !(Math.abs(other.z - z) > epsilon);
    }

    /**
     * Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
     * <p>
     * 将此向量与另一向量比较,使用给定的 epsilon 进行近似相等测试。
     * @return whether the vectors are the same. 两向量是否相同。
     */
    public boolean epsilonEquals(float x, float y, float z, float epsilon){
        if(Math.abs(x - this.x) > epsilon) return false;
        if(Math.abs(y - this.y) > epsilon) return false;
        return !(Math.abs(z - this.z) > epsilon);
    }

    /**
     * Compares this vector with the other vector using Mathf.FLOAT_ROUNDING_ERROR for fuzzy equality testing
     * <p>
     * 使用 Mathf.FLOAT_ROUNDING_ERROR 将此向量与另一向量进行近似相等比较
     * @param other other vector to compare 用于比较的另一个向量
     * @return true if vector are equal, otherwise false 若向量相等则为 true,否则为 false
     */
    public boolean epsilonEquals(final Vec3 other){
        return epsilonEquals(other, Mathf.FLOAT_ROUNDING_ERROR);
    }

    /**
     * Compares this vector with the other vector using Mathf.FLOAT_ROUNDING_ERROR for fuzzy equality testing
     * <p>
     * 使用 Mathf.FLOAT_ROUNDING_ERROR 将此向量与另一向量进行近似相等比较
     * @param x x component of the other vector to compare 用于比较的另一个向量的 x 分量
     * @param y y component of the other vector to compare 用于比较的另一个向量的 y 分量
     * @param z z component of the other vector to compare 用于比较的另一个向量的 z 分量
     * @return true if vector are equal, otherwise false 若向量相等则为 true,否则为 false
     */
    public boolean epsilonEquals(float x, float y, float z){
        return epsilonEquals(x, y, z, Mathf.FLOAT_ROUNDING_ERROR);
    }

    @Override
    public Vec3 setZero(){
        this.x = 0;
        this.y = 0;
        this.z = 0;
        return this;
    }
}
