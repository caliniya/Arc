package arc.math.geom;

import arc.math.*;

/**
 * A simple quaternion class.
 * <p>
 * 简单的四元数类。
 * @author badlogicgames@gmail.com
 * @author vesuvio
 * @author xoppa
 * @see <a href="http://en.wikipedia.org/wiki/Quaternion">http://en.wikipedia.org/wiki/Quaternion</a>
 */
public class Quat{
    private static Quat tmp1 = new Quat(0, 0, 0, 0);
    private static Quat tmp2 = new Quat(0, 0, 0, 0);

    public float x;
    public float y;
    public float z;
    public float w;

    /**
     * Constructor, sets the four components of the quaternion.
     * <p>
     * 构造函数,设置四元数的四个分量。
     * @param x The x-component x 分量
     * @param y The y-component y 分量
     * @param z The z-component z 分量
     * @param w The w-component w 分量
     */
    public Quat(float x, float y, float z, float w){
        this.set(x, y, z, w);
    }

    public Quat(){
        idt();
    }

    /**
     * Constructor, sets the quaternion components from the given quaternion.
     * <p>
     * 构造函数,根据给定的四元数设置四元数分量。
     * @param quat The quaternion to copy. 要复制的四元数。
     */
    public Quat(Quat quat){
        this.set(quat);
    }

    /**
     * Constructor, sets the quaternion from the given axis vector and the angle around that axis in degrees.
     * <p>
     * 构造函数,根据给定的轴向量及绕该轴的角度(单位为度)设置四元数。
     * @param axis The axis 轴
     * @param angle The angle in degrees. 以度表示的角度。
     */
    public Quat(Vec3 axis, float angle){
        this.set(axis, angle);
    }

    /**
     * @return the euclidean length of the specified quaternion
     * 指定四元数的欧氏长度
     */
    public static float len(final float x, final float y, final float z, final float w){
        return (float)Math.sqrt(x * x + y * y + z * z + w * w);
    }

    public static float len2(final float x, final float y, final float z, final float w){
        return x * x + y * y + z * z + w * w;
    }

    /**
     * Get the dot product between the two quaternions (commutative).
     * <p>
     * 获取两个四元数之间的点积(可交换)。
     * @param x1 the x component of the first quaternion 第一个四元数的 x 分量
     * @param y1 the y component of the first quaternion 第一个四元数的 y 分量
     * @param z1 the z component of the first quaternion 第一个四元数的 z 分量
     * @param w1 the w component of the first quaternion 第一个四元数的 w 分量
     * @param x2 the x component of the second quaternion 第二个四元数的 x 分量
     * @param y2 the y component of the second quaternion 第二个四元数的 y 分量
     * @param z2 the z component of the second quaternion 第二个四元数的 z 分量
     * @param w2 the w component of the second quaternion 第二个四元数的 w 分量
     * @return the dot product between the first and second quaternion. 第一个与第二个四元数之间的点积。
     */
    public static float dot(final float x1, final float y1, final float z1, final float w1, final float x2, final float y2,
                            final float z2, final float w2){
        return x1 * x2 + y1 * y2 + z1 * z2 + w1 * w2;
    }

    /**
     * Sets the components of the quaternion
     * <p>
     * 设置四元数的各分量
     * @param x The x-component x 分量
     * @param y The y-component y 分量
     * @param z The z-component z 分量
     * @param w The w-component w 分量
     * @return This quaternion for chaining 此四元数,用于链式调用
     */
    public Quat set(float x, float y, float z, float w){
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /**
     * Sets the quaternion components from the given quaternion.
     * <p>
     * 根据给定的四元数设置四元数分量。
     * @param quat The quaternion. 四元数。
     * @return This quaternion for chaining. 此四元数,用于链式调用。
     */
    public Quat set(Quat quat){
        return this.set(quat.x, quat.y, quat.z, quat.w);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     * <p>
     * 根据给定的轴和绕该轴的角度设置四元数分量。
     * @param axis The axis 轴
     * @param angle The angle in degrees 以度表示的角度
     * @return This quaternion for chaining. 此四元数,用于链式调用。
     */
    public Quat set(Vec3 axis, float angle){
        return setFromAxis(axis.x, axis.y, axis.z, angle);
    }

    /**
     * @return a copy of this quaternion
     * 此四元数的副本
     */
    public Quat cpy(){
        return new Quat(this);
    }

    /**
     * @return the euclidean length of this quaternion
     * 此四元数的欧氏长度
     */
    public float len(){
        return (float)Math.sqrt(x * x + y * y + z * z + w * w);
    }

    @Override
    public String toString(){
        return "[" + x + "|" + y + "|" + z + "|" + w + "]";
    }

    /**
     * Sets the quaternion to the given euler angles in degrees.
     * <p>
     * 将四元数设置为给定的以度表示的欧拉角。
     * @param yaw the rotation around the y axis in degrees 偏航角,单位为度
     * @param pitch the rotation around the x axis in degrees 绕 x 轴的旋转,单位为度
     * @param roll the rotation around the z axis degrees 绕 z 轴的旋转,单位为度
     * @return this quaternion 此四元数
     */
    public Quat setEulerAngles(float yaw, float pitch, float roll){
        return setEulerAnglesRad(yaw * Mathf.degreesToRadians, pitch * Mathf.degreesToRadians, roll
        * Mathf.degreesToRadians);
    }

    /**
     * Sets the quaternion to the given euler angles in radians.
     * <p>
     * 将四元数设置为给定的以弧度表示的欧拉角。
     * @param yaw the rotation around the y axis in radians 偏航角,单位为弧度
     * @param pitch the rotation around the x axis in radians 绕 x 轴的旋转,单位为弧度
     * @param roll the rotation around the z axis in radians 绕 z 轴的旋转,单位为弧度
     * @return this quaternion 此四元数
     */
    public Quat setEulerAnglesRad(float yaw, float pitch, float roll){
        final float hr = roll * 0.5f;
        final float shr = (float)Math.sin(hr);
        final float chr = (float)Math.cos(hr);
        final float hp = pitch * 0.5f;
        final float shp = (float)Math.sin(hp);
        final float chp = (float)Math.cos(hp);
        final float hy = yaw * 0.5f;
        final float shy = (float)Math.sin(hy);
        final float chy = (float)Math.cos(hy);
        final float chy_shp = chy * shp;
        final float shy_chp = shy * chp;
        final float chy_chp = chy * chp;
        final float shy_shp = shy * shp;

        x = (chy_shp * chr) + (shy_chp * shr); // cos(yaw/2) * sin(pitch/2) * cos(roll/2) + sin(yaw/2) * cos(pitch/2) * sin(roll/2)
        // 欧拉角到四元数的 x 分量公式
        y = (shy_chp * chr) - (chy_shp * shr); // sin(yaw/2) * cos(pitch/2) * cos(roll/2) - cos(yaw/2) * sin(pitch/2) * sin(roll/2)
        // 欧拉角到四元数的 y 分量公式
        z = (chy_chp * shr) - (shy_shp * chr); // cos(yaw/2) * cos(pitch/2) * sin(roll/2) - sin(yaw/2) * sin(pitch/2) * cos(roll/2)
        // 欧拉角到四元数的 z 分量公式
        w = (chy_chp * chr) + (shy_shp * shr); // cos(yaw/2) * cos(pitch/2) * cos(roll/2) + sin(yaw/2) * sin(pitch/2) * sin(roll/2)
        // 欧拉角到四元数的 w 分量公式
        return this;
    }

    /**
     * Get the pole of the gimbal lock, if any.
     * <p>
     * 获取万向节锁的极点(如有)。
     * @return positive (+1) for north pole, negative (-1) for south pole, zero (0) when no gimbal lock 北极为正 (+1),南极为负 (-1),无万向节锁时为 0
     */
    public int getGimbalPole(){
        final float t = y * x + z * w;
        return t > 0.499f ? 1 : (t < -0.499f ? -1 : 0);
    }

    /**
     * Get the roll euler angle in radians, which is the rotation around the z axis. Requires that this quaternion is normalized.
     * <p>
     * 获取以弧度表示的翻滚(roll)欧拉角,即绕 z 轴的旋转。要求此四元数已归一化。
     * @return the rotation around the z axis in radians (between -PI and +PI) 绕 z 轴以弧度表示的旋转(-PI 到 +PI 之间)
     */
    public float getRollRad(){
        final int pole = getGimbalPole();
        return pole == 0 ? Mathf.atan2(1f - 2f * (x * x + z * z), 2f * (w * z + y * x)) : (float)pole * 2f
        * Mathf.atan2(w, y);
    }

    /**
     * Get the roll euler angle in degrees, which is the rotation around the z axis. Requires that this quaternion is normalized.
     * <p>
     * 获取以度表示的翻滚(roll)欧拉角,即绕 z 轴的旋转。要求此四元数已归一化。
     * @return the rotation around the z axis in degrees (between -180 and +180) 绕 z 轴以度表示的旋转(-180 到 +180 之间)
     */
    public float getRoll(){
        return getRollRad() * Mathf.radiansToDegrees;
    }

    /**
     * Get the pitch euler angle in radians, which is the rotation around the x axis. Requires that this quaternion is normalized.
     * <p>
     * 获取以弧度表示的俯仰(pitch)欧拉角,即绕 x 轴的旋转。要求此四元数已归一化。
     * @return the rotation around the x axis in radians (between -(PI/2) and +(PI/2)) 绕 x 轴以弧度表示的旋转(-(PI/2) 到 +(PI/2) 之间)
     */
    public float getPitchRad(){
        final int pole = getGimbalPole();
        return pole == 0 ? (float)Math.asin(Mathf.clamp(2f * (w * x - z * y), -1f, 1f)) : (float)pole * Mathf.PI * 0.5f;
    }

    /**
     * Get the pitch euler angle in degrees, which is the rotation around the x axis. Requires that this quaternion is normalized.
     * <p>
     * 获取以度表示的俯仰(pitch)欧拉角,即绕 x 轴的旋转。要求此四元数已归一化。
     * @return the rotation around the x axis in degrees (between -90 and +90) 绕 x 轴以度表示的旋转(-90 到 +90 之间)
     */
    public float getPitch(){
        return getPitchRad() * Mathf.radiansToDegrees;
    }

    /**
     * Get the yaw euler angle in radians, which is the rotation around the y axis. Requires that this quaternion is normalized.
     * <p>
     * 获取以弧度表示的偏航(yaw)欧拉角,即绕 y 轴的旋转。要求此四元数已归一化。
     * @return the rotation around the y axis in radians (between -PI and +PI) 绕 y 轴以弧度表示的旋转(-PI 到 +PI 之间)
     */
    public float getYawRad(){
        return getGimbalPole() == 0 ? Mathf.atan2(1f - 2f * (y * y + x * x), 2f * (y * w + x * z)) : 0f;
    }

    /**
     * Get the yaw euler angle in degrees, which is the rotation around the y axis. Requires that this quaternion is normalized.
     * <p>
     * 获取以度表示的偏航(yaw)欧拉角,即绕 y 轴的旋转。要求此四元数已归一化。
     * @return the rotation around the y axis in degrees (between -180 and +180) 绕 y 轴以度表示的旋转(-180 到 +180 之间)
     */
    public float getYaw(){
        return getYawRad() * Mathf.radiansToDegrees;
    }

    /**
     * @return the length of this quaternion without square root
     * 此四元数不开平方的长度
     */
    public float len2(){
        return x * x + y * y + z * z + w * w;
    }

    /**
     * Normalizes this quaternion to unit length
     * <p>
     * 将此四元数归一化到单位长度
     * @return the quaternion for chaining 用于链式调用的四元数
     */
    public Quat nor(){
        float len = len2();
        if(len != 0.f && !Mathf.equal(len, 1f)){
            len = (float)Math.sqrt(len);
            w /= len;
            x /= len;
            y /= len;
            z /= len;
        }
        return this;
    }

    // TODO : this would better fit into the Vec3 class
    // TODO:此功能更适合放到 Vec3 类中

    /**
     * Conjugate the quaternion.
     * <p>
     * 求四元数的共轭。
     * @return This quaternion for chaining 此四元数,用于链式调用
     */
    public Quat conjugate(){
        x = -x;
        y = -y;
        z = -z;
        return this;
    }

    /**
     * Transforms the given vector using this quaternion
     * <p>
     * 使用此四元数变换给定的向量
     * @param v Vector to transform 要变换的向量
     */
    public Vec3 transform(Vec3 v){
        tmp2.set(this);
        tmp2.conjugate();
        tmp2.mulLeft(tmp1.set(v.x, v.y, v.z, 0)).mulLeft(this);

        v.x = tmp2.x;
        v.y = tmp2.y;
        v.z = tmp2.z;
        return v;
    }

    /**
     * Multiplies this quaternion with another one in the form of this = this * other
     * <p>
     * 此四元数与另一四元数相乘,形式为 this = this * other
     * @param other Quaternion to multiply with 用于相乘的四元数
     * @return This quaternion for chaining 此四元数,用于链式调用
     */
    public Quat mul(final Quat other){
        final float newX = this.w * other.x + this.x * other.w + this.y * other.z - this.z * other.y;
        final float newY = this.w * other.y + this.y * other.w + this.z * other.x - this.x * other.z;
        final float newZ = this.w * other.z + this.z * other.w + this.x * other.y - this.y * other.x;
        final float newW = this.w * other.w - this.x * other.x - this.y * other.y - this.z * other.z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /**
     * Multiplies this quaternion with another one in the form of this = this * other
     * <p>
     * 此四元数与另一四元数相乘,形式为 this = this * other
     * @param x the x component of the other quaternion to multiply with 用于相乘的另一个四元数的 x 分量
     * @param y the y component of the other quaternion to multiply with 用于相乘的另一个四元数的 y 分量
     * @param z the z component of the other quaternion to multiply with 用于相乘的另一个四元数的 z 分量
     * @param w the w component of the other quaternion to multiply with 用于相乘的另一个四元数的 w 分量
     * @return This quaternion for chaining 此四元数,用于链式调用
     */
    public Quat mul(final float x, final float y, final float z, final float w){
        final float newX = this.w * x + this.x * w + this.y * z - this.z * y;
        final float newY = this.w * y + this.y * w + this.z * x - this.x * z;
        final float newZ = this.w * z + this.z * w + this.x * y - this.y * x;
        final float newW = this.w * w - this.x * x - this.y * y - this.z * z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /**
     * Multiplies this quaternion with another one in the form of this = other * this
     * <p>
     * 此四元数与另一四元数相乘,形式为 this = other * this
     * @param other Quaternion to multiply with 用于相乘的四元数
     * @return This quaternion for chaining 此四元数,用于链式调用
     */
    public Quat mulLeft(Quat other){
        final float newX = other.w * this.x + other.x * this.w + other.y * this.z - other.z * this.y;
        final float newY = other.w * this.y + other.y * this.w + other.z * this.x - other.x * this.z;
        final float newZ = other.w * this.z + other.z * this.w + other.x * this.y - other.y * this.x;
        final float newW = other.w * this.w - other.x * this.x - other.y * this.y - other.z * this.z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /**
     * Multiplies this quaternion with another one in the form of this = other * this
     * <p>
     * 此四元数与另一四元数相乘,形式为 this = other * this
     * @param x the x component of the other quaternion to multiply with 用于相乘的另一个四元数的 x 分量
     * @param y the y component of the other quaternion to multiply with 用于相乘的另一个四元数的 y 分量
     * @param z the z component of the other quaternion to multiply with 用于相乘的另一个四元数的 z 分量
     * @param w the w component of the other quaternion to multiply with 用于相乘的另一个四元数的 w 分量
     * @return This quaternion for chaining 此四元数,用于链式调用
     */
    public Quat mulLeft(final float x, final float y, final float z, final float w){
        final float newX = w * this.x + x * this.w + y * this.z - z * this.y;
        final float newY = w * this.y + y * this.w + z * this.x - x * this.z;
        final float newZ = w * this.z + z * this.w + x * this.y - y * this.x;
        final float newW = w * this.w - x * this.x - y * this.y - z * this.z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /**
     * Add the x,y,z,w components of the passed in quaternion to the ones of this quaternion
     * 将传入四元数的 x,y,z,w 分量加到此四元数的对应分量上
     */
    public Quat add(Quat quat){
        this.x += quat.x;
        this.y += quat.y;
        this.z += quat.z;
        this.w += quat.w;
        return this;
    }

    /**
     * Add the x,y,z,w components of the passed in quaternion to the ones of this quaternion
     * 将传入四元数的 x,y,z,w 分量加到此四元数的对应分量上
     */
    public Quat add(float qx, float qy, float qz, float qw){
        this.x += qx;
        this.y += qy;
        this.z += qz;
        this.w += qw;
        return this;
    }

    public void toMatrix (final float[] matrix) {
        final float xx = x * x;
        final float xy = x * y;
        final float xz = x * z;
        final float xw = x * w;
        final float yy = y * y;
        final float yz = y * z;
        final float yw = y * w;
        final float zz = z * z;
        final float zw = z * w;
        // Set matrix from quaternion
        // 根据四元数设置矩阵
        matrix[Mat3D.M00] = 1 - 2 * (yy + zz);
        matrix[Mat3D.M01] = 2 * (xy - zw);
        matrix[Mat3D.M02] = 2 * (xz + yw);
        matrix[Mat3D.M03] = 0;
        matrix[Mat3D.M10] = 2 * (xy + zw);
        matrix[Mat3D.M11] = 1 - 2 * (xx + zz);
        matrix[Mat3D.M12] = 2 * (yz - xw);
        matrix[Mat3D.M13] = 0;
        matrix[Mat3D.M20] = 2 * (xz - yw);
        matrix[Mat3D.M21] = 2 * (yz + xw);
        matrix[Mat3D.M22] = 1 - 2 * (xx + yy);
        matrix[Mat3D.M23] = 0;
        matrix[Mat3D.M30] = 0;
        matrix[Mat3D.M31] = 0;
        matrix[Mat3D.M32] = 0;
        matrix[Mat3D.M33] = 1;
    }

    /**
     * Sets the quaternion to an identity Quaternion
     * <p>
     * 将四元数设置为单位四元数
     * @return this quaternion for chaining 此四元数,用于链式调用
     */
    public Quat idt(){
        return this.set(0, 0, 0, 1);
    }

    /**
     * @return If this quaternion is an identity Quaternion
     * 此四元数是否为单位四元数
     */
    public boolean isIdentity(){
        return Mathf.zero(x) && Mathf.zero(y) && Mathf.zero(z) && Mathf.equal(w, 1f);
    }

    // todo : the setFromAxis(v3,float) method should replace the set(v3,float) method
    // todo:setFromAxis(v3,float) 方法应取代 set(v3,float) 方法

    /**
     * @return If this quaternion is an identity Quaternion
     * 此四元数是否为单位四元数
     */
    public boolean isIdentity(final float tolerance){
        return Mathf.zero(x, tolerance) && Mathf.zero(y, tolerance) && Mathf.zero(z, tolerance)
        && Mathf.equal(w, 1f, tolerance);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     * <p>
     * 根据给定的轴和绕该轴的角度设置四元数分量。
     * @param axis The axis 轴
     * @param degrees The angle in degrees 以度表示的角度
     * @return This quaternion for chaining. 此四元数,用于链式调用。
     */
    public Quat setFromAxis(final Vec3 axis, final float degrees){
        return setFromAxis(axis.x, axis.y, axis.z, degrees);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     * <p>
     * 根据给定的轴和绕该轴的角度设置四元数分量。
     * @param axis The axis 轴
     * @param radians The angle in radians 以弧度表示的角度
     * @return This quaternion for chaining. 此四元数,用于链式调用。
     */
    public Quat setFromAxisRad(final Vec3 axis, final float radians){
        return setFromAxisRad(axis.x, axis.y, axis.z, radians);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     * <p>
     * 根据给定的轴和绕该轴的角度设置四元数分量。
     * @param x X direction of the axis 轴的 X 方向
     * @param y Y direction of the axis 轴的 Y 方向
     * @param z Z direction of the axis 轴的 Z 方向
     * @param degrees The angle in degrees 以度表示的角度
     * @return This quaternion for chaining. 此四元数,用于链式调用。
     */
    public Quat setFromAxis(final float x, final float y, final float z, final float degrees){
        return setFromAxisRad(x, y, z, degrees * Mathf.degreesToRadians);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     * <p>
     * 根据给定的轴和绕该轴的角度设置四元数分量。
     * @param x X direction of the axis 轴的 X 方向
     * @param y Y direction of the axis 轴的 Y 方向
     * @param z Z direction of the axis 轴的 Z 方向
     * @param radians The angle in radians 以弧度表示的角度
     * @return This quaternion for chaining. 此四元数,用于链式调用。
     */
    public Quat setFromAxisRad(final float x, final float y, final float z, final float radians){
        float d = Vec3.len(x, y, z);
        if(d == 0f) return idt();
        d = 1f / d;
        float l_ang = radians < 0 ? Mathf.PI2 - (-radians % Mathf.PI2) : radians % Mathf.PI2;
        float l_sin = (float)Math.sin(l_ang / 2);
        float l_cos = (float)Math.cos(l_ang / 2);
        return this.set(d * x * l_sin, d * y * l_sin, d * z * l_sin, l_cos).nor();
    }

    /**
     * Sets the Quaternion from the given matrix, optionally removing any scaling.
     * 根据给定矩阵设置四元数,可选择去除缩放。
     */
    public Quat setFromMatrix (boolean normalizeAxes, Mat3D matrix) {
        return setFromAxes(normalizeAxes, matrix.val[Mat3D.M00], matrix.val[Mat3D.M01], matrix.val[Mat3D.M02],
        matrix.val[Mat3D.M10], matrix.val[Mat3D.M11], matrix.val[Mat3D.M12], matrix.val[Mat3D.M20],
        matrix.val[Mat3D.M21], matrix.val[Mat3D.M22]);
    }

    /**
     * Sets the Quaternion from the given rotation matrix, which must not contain scaling.
     * 根据给定的旋转矩阵设置四元数,该矩阵不得包含缩放。
     */
    public Quat setFromMatrix (Mat3D matrix) {
        return setFromMatrix(false, matrix);
    }

    /**
     * Sets the Quaternion from the given matrix, optionally removing any scaling.
     * 根据给定矩阵设置四元数,可选择去除缩放。
     */
    public Quat setFromMatrix (boolean normalizeAxes, Mat matrix) {
        return setFromAxes(normalizeAxes, matrix.val[Mat.M00], matrix.val[Mat.M01], matrix.val[Mat.M02],
        matrix.val[Mat.M10], matrix.val[Mat.M11], matrix.val[Mat.M12], matrix.val[Mat.M20],
        matrix.val[Mat.M21], matrix.val[Mat.M22]);
    }

    /**
     * Sets the Quaternion from the given rotation matrix, which must not contain scaling.
     * 根据给定的旋转矩阵设置四元数,该矩阵不得包含缩放。
     */
    public Quat setFromMatrix (Mat matrix) {
        return setFromMatrix(false, matrix);
    }

    /**
     * <p>
     * Sets the Quaternion from the given x-, y- and z-axis which have to be orthonormal.
     * </p>
     *
     * <p>
     * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
     * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
     * </p>
     * <p>
     * <p> 根据给定的 x、y、z 轴设置四元数,这三个轴必须是标准正交的。 </p> <p> 取自 JPCT 的 Bones 框架,参见 http://www.aptalkarga.com/bones/,其又取自 Graphics Gem 代码:ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z。 </p>
     * @param xx x-axis x-coordinate x 轴的 x 坐标
     * @param xy x-axis y-coordinate x 轴的 y 坐标
     * @param xz x-axis z-coordinate x 轴的 z 坐标
     * @param yx y-axis x-coordinate y 轴的 x 坐标
     * @param yy y-axis y-coordinate y 轴的 y 坐标
     * @param yz y-axis z-coordinate y 轴的 z 坐标
     * @param zx z-axis x-coordinate z 轴的 x 坐标
     * @param zy z-axis y-coordinate z 轴的 y 坐标
     * @param zz z-axis z-coordinate z 轴的 z 坐标
     */
    public Quat setFromAxes(float xx, float xy, float xz, float yx, float yy, float yz, float zx, float zy, float zz){
        return setFromAxes(false, xx, xy, xz, yx, yy, yz, zx, zy, zz);
    }

    /**
     * <p>
     * Sets the Quaternion from the given x-, y- and z-axis.
     * </p>
     *
     * <p>
     * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
     * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
     * </p>
     * <p>
     * <p> 根据给定的 x、y、z 轴设置四元数。 </p> <p> 取自 JPCT 的 Bones 框架,参见 http://www.aptalkarga.com/bones/,其又取自 Graphics Gem 代码:ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z。 </p>
     * @param normalizeAxes whether to normalize the axes (necessary when they contain scaling) 是否归一化各轴(当其中包含缩放时必要)
     * @param xx x-axis x-coordinate x 轴的 x 坐标
     * @param xy x-axis y-coordinate x 轴的 y 坐标
     * @param xz x-axis z-coordinate x 轴的 z 坐标
     * @param yx y-axis x-coordinate y 轴的 x 坐标
     * @param yy y-axis y-coordinate y 轴的 y 坐标
     * @param yz y-axis z-coordinate y 轴的 z 坐标
     * @param zx z-axis x-coordinate z 轴的 x 坐标
     * @param zy z-axis y-coordinate z 轴的 y 坐标
     * @param zz z-axis z-coordinate z 轴的 z 坐标
     */
    public Quat setFromAxes(boolean normalizeAxes, float xx, float xy, float xz, float yx, float yy, float yz, float zx,
                            float zy, float zz){
        if(normalizeAxes){
            final float lx = 1f / Vec3.len(xx, xy, xz);
            final float ly = 1f / Vec3.len(yx, yy, yz);
            final float lz = 1f / Vec3.len(zx, zy, zz);
            xx *= lx;
            xy *= lx;
            xz *= lx;
            yx *= ly;
            yy *= ly;
            yz *= ly;
            zx *= lz;
            zy *= lz;
            zz *= lz;
        }
        // the trace is the sum of the diagonal elements; see
        // 迹(trace)是对角线元素之和;参见
        // http://mathworld.wolfram.com/MatrixTrace.html
        final float t = xx + yy + zz;

        // we protect the division by s by ensuring that s>=1
        // 通过确保 s>=1 来保护除法
        if(t >= 0){ // |w| >= .5
        // w 的绝对值 >= .5
            float s = (float)Math.sqrt(t + 1); // |s|>=1 ...
            // s 的绝对值 >= 1 ……
            w = 0.5f * s;
            s = 0.5f / s; // so this division isn't bad
            // 因此这个除法没有问题
            x = (zy - yz) * s;
            y = (xz - zx) * s;
            z = (yx - xy) * s;
        }else if((xx > yy) && (xx > zz)){
            float s = (float)Math.sqrt(1.0 + xx - yy - zz); // |s|>=1
            // s 的绝对值 >= 1
            x = s * 0.5f; // |x| >= .5
            // x 的绝对值 >= .5
            s = 0.5f / s;
            y = (yx + xy) * s;
            z = (xz + zx) * s;
            w = (zy - yz) * s;
        }else if(yy > zz){
            float s = (float)Math.sqrt(1.0 + yy - xx - zz); // |s|>=1
            // s 的绝对值 >= 1
            y = s * 0.5f; // |y| >= .5
            // y 的绝对值 >= .5
            s = 0.5f / s;
            x = (yx + xy) * s;
            z = (zy + yz) * s;
            w = (xz - zx) * s;
        }else{
            float s = (float)Math.sqrt(1.0 + zz - xx - yy); // |s|>=1
            // s 的绝对值 >= 1
            z = s * 0.5f; // |z| >= .5
            // z 的绝对值 >= .5
            s = 0.5f / s;
            x = (xz + zx) * s;
            y = (zy + yz) * s;
            w = (yx - xy) * s;
        }

        return this;
    }

    /**
     * Set this quaternion to the rotation between two vectors.
     * <p>
     * 将此四元数设置为两个向量之间的旋转。
     * @param v1 The base vector, which should be normalized. 基向量,应当已归一化。
     * @param v2 The target vector, which should be normalized. 目标向量,应当已归一化。
     * @return This quaternion for chaining 此四元数,用于链式调用
     */
    public Quat setFromCross(final Vec3 v1, final Vec3 v2){
        final float dot = Mathf.clamp(v1.dot(v2), -1f, 1f);
        final float angle = (float)Math.acos(dot);
        return setFromAxisRad(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x, angle);
    }

    /**
     * Set this quaternion to the rotation between two vectors.
     * <p>
     * 将此四元数设置为两个向量之间的旋转。
     * @param x1 The base vectors x value, which should be normalized. 基向量的 x 值,应当已归一化。
     * @param y1 The base vectors y value, which should be normalized. 基向量的 y 值,应当已归一化。
     * @param z1 The base vectors z value, which should be normalized. 基向量的 z 值,应当已归一化。
     * @param x2 The target vector x value, which should be normalized. 目标向量的 x 值,应当已归一化。
     * @param y2 The target vector y value, which should be normalized. 目标向量的 y 值,应当已归一化。
     * @param z2 The target vector z value, which should be normalized. 目标向量的 z 值,应当已归一化。
     * @return This quaternion for chaining 此四元数,用于链式调用
     */
    public Quat setFromCross(final float x1, final float y1, final float z1, final float x2, final float y2, final float z2){
        final float dot = Mathf.clamp(Vec3.dot(x1, y1, z1, x2, y2, z2), -1f, 1f);
        final float angle = (float)Math.acos(dot);
        return setFromAxisRad(y1 * z2 - z1 * y2, z1 * x2 - x1 * z2, x1 * y2 - y1 * x2, angle);
    }

    /**
     * Spherical Linear interpolation between this quaternion and the other quaternion, based on the alpha value in the range
     * [0,1]. Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/
     * <p>
     * 在此四元数与另一四元数之间按 [0,1] 范围内的 alpha 值进行球面线性插值。取自 JPCT 的 Bones 框架,参见 http://www.aptalkarga.com/bones/
     * @param end the end quaternion 终止四元数
     * @param alpha alpha in the range [0,1] 范围 [0,1] 内的 alpha 值
     * @return this quaternion for chaining 此四元数,用于链式调用
     */
    public Quat slerp(Quat end, float alpha){
        final float d = this.x * end.x + this.y * end.y + this.z * end.z + this.w * end.w;
        float absDot = d < 0.f ? -d : d;

        // Set the first and second scale for the interpolation
        // 设置插值所需的第一个和第二个缩放系数
        float scale0 = 1f - alpha;
        float scale1 = alpha;

        // Check if the angle between the 2 quaternions was big enough to
        // 检查两个四元数之间的角度是否大到足以
        // warrant such calculations
        // 才值得进行这种计算
        if((1 - absDot) > 0.1){// Get the angle between the 2 quaternions,
        // 获取两个四元数之间的角度,
            // and then store the sin() of that angle
            // 然后存储该角度的 sin() 值
            final float angle = (float)Math.acos(absDot);
            final float invSinTheta = 1f / (float)Math.sin(angle);

            // Calculate the scale for q1 and q2, according to the angle and
            // 根据角度计算 q1 与 q2 的缩放系数,以及
            // it's sine value
            // 其正弦值
            scale0 = ((float)Math.sin((1f - alpha) * angle) * invSinTheta);
            scale1 = ((float)Math.sin((alpha * angle)) * invSinTheta);
        }

        if(d < 0.f) scale1 = -scale1;

        // Calculate the x, y, z and w values for the quaternion by using a
        // 使用如下方式计算四元数的 x、y、z 和 w 值
        // special form of Linear interpolation for quaternions.
        // 四元数专用的线性插值形式。
        x = (scale0 * x) + (scale1 * end.x);
        y = (scale0 * y) + (scale1 * end.y);
        z = (scale0 * z) + (scale1 * end.z);
        w = (scale0 * w) + (scale1 * end.w);

        // Return the interpolated quaternion
        // 返回插值后的四元数
        return this;
    }

    /**
     * Spherical linearly interpolates multiple quaternions and stores the result in this Quaternion. Will not destroy the data
     * previously inside the elements of q. result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n) where w_i=1/n.
     * <p>
     * 对多个四元数进行球面线性插值,结果存入此四元数。不会破坏 q 各元素中原有的数据。result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n),其中 w_i=1/n。
     * @param q List of quaternions 四元数列表
     * @return This quaternion for chaining 此四元数,用于链式调用
     */
    public Quat slerp(Quat[] q){

        // Calculate exponents and multiply everything from left to right
        // 计算指数并将所有元素从左到右相乘
        final float w = 1.0f / q.length;
        set(q[0]).exp(w);
        for(int i = 1; i < q.length; i++)
            mul(tmp1.set(q[i]).exp(w));
        nor();
        return this;
    }

    /**
     * Spherical linearly interpolates multiple quaternions by the given weights and stores the result in this Quaternion. Will not
     * destroy the data previously inside the elements of q or w. result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n) where the sum of w_i
     * is 1. Lists must be equal in length.
     * <p>
     * 按给定权重对多个四元数进行球面线性插值,结果存入此四元数。不会破坏 q 或 w 中原有的数据。result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n),其中 w_i 之和为 1。两个列表长度必须相等。
     * @param q List of quaternions 四元数列表
     * @param w List of weights 权重列表
     * @return This quaternion for chaining 此四元数,用于链式调用
     */
    public Quat slerp(Quat[] q, float[] w){

        // Calculate exponents and multiply everything from left to right
        // 计算指数并将所有元素从左到右相乘
        set(q[0]).exp(w[0]);
        for(int i = 1; i < q.length; i++)
            mul(tmp1.set(q[i]).exp(w[i]));
        nor();
        return this;
    }

    /**
     * Calculates (this quaternion)^alpha where alpha is a real number and stores the result in this quaternion. See
     * http://en.wikipedia.org/wiki/Quaternion#Exponential.2C_logarithm.2C_and_power
     * <p>
     * 计算此四元数的 alpha 次幂(alpha 为实数),结果存入此四元数。参见 http://en.wikipedia.org/wiki/Quaternion#Exponential.2C_logarithm.2C_and_power
     * @param alpha Exponent 指数
     * @return This quaternion for chaining 此四元数,用于链式调用
     */
    public Quat exp(float alpha){

        // Calculate |q|^alpha
        // 计算 |q|^alpha
        float norm = len();
        float normExp = (float)Math.pow(norm, alpha);

        // Calculate theta
        // 计算 theta
        float theta = (float)Math.acos(w / norm);

        // Calculate coefficient of basis elements
        // 计算基元素的系数
        float coeff = 0;
        if(Math.abs(theta) < 0.001) // If theta is small enough, use the limit of sin(alpha*theta) / sin(theta) instead of actual
        // 如果 theta 足够小,则使用 sin(alpha*theta) / sin(theta) 的极限值代替实际的
// value
// 值
            coeff = normExp * alpha / norm;
        else
            coeff = (float)(normExp * Math.sin(alpha * theta) / (norm * Math.sin(theta)));

        // Write results
        // 写入结果
        w = (float)(normExp * Math.cos(alpha * theta));
        x *= coeff;
        y *= coeff;
        z *= coeff;

        // Fix any possible discrepancies
        // 修正一切可能的偏差
        nor();

        return this;
    }

    @Override
    public int hashCode(){
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToRawIntBits(w);
        result = prime * result + Float.floatToRawIntBits(x);
        result = prime * result + Float.floatToRawIntBits(y);
        result = prime * result + Float.floatToRawIntBits(z);
        return result;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj){
            return true;
        }
        if(obj == null){
            return false;
        }
        if(!(obj instanceof Quat)){
            return false;
        }
        Quat other = (Quat)obj;
        return (Float.floatToRawIntBits(w) == Float.floatToRawIntBits(other.w))
        && (Float.floatToRawIntBits(x) == Float.floatToRawIntBits(other.x))
        && (Float.floatToRawIntBits(y) == Float.floatToRawIntBits(other.y))
        && (Float.floatToRawIntBits(z) == Float.floatToRawIntBits(other.z));
    }

    /**
     * Get the dot product between this and the other quaternion (commutative).
     * <p>
     * 获取此四元数与另一四元数之间的点积(可交换)。
     * @param other the other quaternion. 另一个四元数。
     * @return the dot product of this and the other quaternion. 此四元数与另一四元数的点积。
     */
    public float dot(final Quat other){
        return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
    }

    /**
     * Get the dot product between this and the other quaternion (commutative).
     * <p>
     * 获取此四元数与另一四元数之间的点积(可交换)。
     * @param x the x component of the other quaternion 另一个四元数的 x 分量
     * @param y the y component of the other quaternion 另一个四元数的 y 分量
     * @param z the z component of the other quaternion 另一个四元数的 z 分量
     * @param w the w component of the other quaternion 另一个四元数的 w 分量
     * @return the dot product of this and the other quaternion. 此四元数与另一四元数的点积。
     */
    public float dot(final float x, final float y, final float z, final float w){
        return this.x * x + this.y * y + this.z * z + this.w * w;
    }

    /**
     * Multiplies the components of this quaternion with the given scalar.
     * <p>
     * 将此四元数的各分量与给定标量相乘。
     * @param scalar the scalar. 标量。
     * @return this quaternion for chaining. 此四元数,用于链式调用。
     */
    public Quat mul(float scalar){
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        this.w *= scalar;
        return this;
    }

    /**
     * Get the axis angle representation of the rotation in degrees. The supplied vector will receive the axis (x, y and z values)
     * of the rotation and the value returned is the angle in degrees around that axis. Note that this method will alter the
     * supplied vector, the existing value of the vector is ignored. </p> This will normalize this quaternion if needed. The
     * received axis is a unit vector. However, if this is an identity quaternion (no rotation), then the length of the axis may be
     * zero.
     * <p>
     * 以度表示获取旋转的轴角表示。给定的向量将接收旋转的轴(x、y 和 z 值),返回值是绕该轴的以度表示的角度。注意此方法会修改给定的向量,向量原有值会被忽略。 </p> 必要时会归一化此四元数。接收到的轴是单位向量。但如果这是单位四元数(无旋转),轴的长度可能为零。
     * @param axis vector which will receive the axis 用于接收轴的向量
     * @return the angle in degrees 以度表示的角度
     * @see <a href="http://en.wikipedia.org/wiki/Axis%E2%80%93angle_representation">wikipedia</a>
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle">calculation</a>
     */
    public float getAxisAngle(Vec3 axis){
        return getAxisAngleRad(axis) * Mathf.radiansToDegrees;
    }

    /**
     * Get the axis-angle representation of the rotation in radians. The supplied vector will receive the axis (x, y and z values)
     * of the rotation and the value returned is the angle in radians around that axis. Note that this method will alter the
     * supplied vector, the existing value of the vector is ignored. </p> This will normalize this quaternion if needed. The
     * received axis is a unit vector. However, if this is an identity quaternion (no rotation), then the length of the axis may be
     * zero.
     * <p>
     * 以弧度表示获取旋转的轴角表示。给定的向量将接收旋转的轴(x、y 和 z 值),返回值是绕该轴的以弧度表示的角度。注意此方法会修改给定的向量,向量原有值会被忽略。 </p> 必要时会归一化此四元数。接收到的轴是单位向量。但如果这是单位四元数(无旋转),轴的长度可能为零。
     * @param axis vector which will receive the axis 用于接收轴的向量
     * @return the angle in radians 以弧度表示的角度
     * @see <a href="http://en.wikipedia.org/wiki/Axis%E2%80%93angle_representation">wikipedia</a>
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle">calculation</a>
     */
    public float getAxisAngleRad(Vec3 axis){
        if(this.w > 1)
            this.nor(); // if w>1 acos and sqrt will produce errors, this cant happen if quaternion is normalised
            // 若 w>1,acos 和 sqrt 会产生错误;四元数已归一化时不会发生这种情况
        float angle = (float)(2.0 * Math.acos(this.w));
        double s = Math.sqrt(1 - this.w * this.w); // assuming quaternion normalised then w is less than 1, so term always positive.
        // 假定四元数已归一化,则 w 小于 1,因此该项恒为正。
        if(s < Mathf.FLOAT_ROUNDING_ERROR){ // test to avoid divide by zero, s is always positive due to sqrt
        // 避免除以零的检查;由于 sqrt,s 恒为正
            // if s close to zero then direction of axis not important
            // 若 s 接近零,则轴的方向无关紧要
            axis.x = this.x; // if it is important that axis is normalised then replace with x=1; y=z=0;
            // 如果必须保证轴已归一化,可替换为 x=1; y=z=0;
            axis.y = this.y;
            axis.z = this.z;
        }else{
            axis.x = (float)(this.x / s); // normalise axis
            // 归一化轴
            axis.y = (float)(this.y / s);
            axis.z = (float)(this.z / s);
        }

        return angle;
    }

    /**
     * Get the angle in radians of the rotation this quaternion represents. Does not normalize the quaternion. Use
     * {@link #getAxisAngleRad(Vec3)} to get both the axis and the angle of this rotation. Use
     * {@link #getAngleAroundRad(Vec3)} to get the angle around a specific axis.
     * <p>
     * 获取此四元数所表示旋转的以弧度表示的角度。不归一化四元数。使用 {@link #getAxisAngleRad(Vec3)} 可同时获取旋转的轴和角度。使用 {@link #getAngleAroundRad(Vec3)} 可获取绕特定轴的角度。
     * @return the angle in radians of the rotation 旋转的以弧度表示的角度
     */
    public float getAngleRad(){
        return (float)(2.0 * Math.acos((this.w > 1) ? (this.w / len()) : this.w));
    }

    /**
     * Get the angle in degrees of the rotation this quaternion represents. Use {@link #getAxisAngle(Vec3)} to get both the axis
     * and the angle of this rotation. Use {@link #getAngleAround(Vec3)} to get the angle around a specific axis.
     * <p>
     * 获取此四元数所表示旋转的以度表示的角度。使用 {@link #getAxisAngle(Vec3)} 可同时获取旋转的轴和角度。使用 {@link #getAngleAround(Vec3)} 可获取绕特定轴的角度。
     * @return the angle in degrees of the rotation 旋转的以度表示的角度
     */
    public float getAngle(){
        return getAngleRad() * Mathf.radiansToDegrees;
    }

    /**
     * Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation around the
     * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation around an
     * axis perpendicular to the specified axis. </p> The swing and twist rotation can be used to reconstruct the original
     * quaternion: this = swing * twist
     * <p>
     * 获取指定轴的摆动旋转和扭转旋转。扭转旋转表示绕指定轴的旋转。摆动旋转表示指定轴本身的旋转,即绕垂直于指定轴的轴的旋转。 </p> 摆动和扭转旋转可用于重构原四元数:this = swing * twist
     * @param axisX the X component of the normalized axis for which to get the swing and twist rotation 已归一化轴的 X 分量,用于获取摆动和扭转旋转
     * @param axisY the Y component of the normalized axis for which to get the swing and twist rotation 已归一化轴的 Y 分量,用于获取摆动和扭转旋转
     * @param axisZ the Z component of the normalized axis for which to get the swing and twist rotation 已归一化轴的 Z 分量,用于获取摆动和扭转旋转
     * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis 用于接收摆动旋转:绕垂直于指定轴的轴的旋转
     * @param twist will receive the twist rotation: the rotation around the specified axis 用于接收扭转旋转:绕指定轴的旋转
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition">calculation</a>
     */
    public void getSwingTwist(final float axisX, final float axisY, final float axisZ, final Quat swing,
                              final Quat twist){
        final float d = Vec3.dot(this.x, this.y, this.z, axisX, axisY, axisZ);
        twist.set(axisX * d, axisY * d, axisZ * d, this.w).nor();
        if(d < 0) twist.mul(-1f);
        swing.set(twist).conjugate().mulLeft(this);
    }

    /**
     * Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation around the
     * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation around an
     * axis perpendicular to the specified axis. </p> The swing and twist rotation can be used to reconstruct the original
     * quaternion: this = swing * twist
     * <p>
     * 获取指定轴的摆动旋转和扭转旋转。扭转旋转表示绕指定轴的旋转。摆动旋转表示指定轴本身的旋转,即绕垂直于指定轴的轴的旋转。 </p> 摆动和扭转旋转可用于重构原四元数:this = swing * twist
     * @param axis the normalized axis for which to get the swing and twist rotation 已归一化的轴,用于获取摆动(swing)和扭转(twist)旋转
     * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis 用于接收摆动旋转:绕垂直于指定轴的轴的旋转
     * @param twist will receive the twist rotation: the rotation around the specified axis 用于接收扭转旋转:绕指定轴的旋转
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition">calculation</a>
     */
    public void getSwingTwist(final Vec3 axis, final Quat swing, final Quat twist){
        getSwingTwist(axis.x, axis.y, axis.z, swing, twist);
    }

    /**
     * Get the angle in radians of the rotation around the specified axis. The axis must be normalized.
     * <p>
     * 获取绕指定轴旋转的以弧度表示的角度。轴必须已归一化。
     * @param axisX the x component of the normalized axis for which to get the angle 已归一化轴的 x 分量,用于获取角度
     * @param axisY the y component of the normalized axis for which to get the angle 已归一化轴的 y 分量,用于获取角度
     * @param axisZ the z component of the normalized axis for which to get the angle 已归一化轴的 z 分量,用于获取角度
     * @return the angle in radians of the rotation around the specified axis 绕指定轴旋转的以弧度表示的角度
     */
    public float getAngleAroundRad(final float axisX, final float axisY, final float axisZ){
        final float d = Vec3.dot(this.x, this.y, this.z, axisX, axisY, axisZ);
        final float l2 = Quat.len2(axisX * d, axisY * d, axisZ * d, this.w);
        return Mathf.zero(l2) ? 0f : (float)(2.0 * Math.acos(Mathf.clamp(
        (float)((d < 0 ? -this.w : this.w) / Math.sqrt(l2)), -1f, 1f)));
    }

    /**
     * Get the angle in radians of the rotation around the specified axis. The axis must be normalized.
     * <p>
     * 获取绕指定轴旋转的以弧度表示的角度。轴必须已归一化。
     * @param axis the normalized axis for which to get the angle 已归一化的轴,用于获取角度
     * @return the angle in radians of the rotation around the specified axis 绕指定轴旋转的以弧度表示的角度
     */
    public float getAngleAroundRad(final Vec3 axis){
        return getAngleAroundRad(axis.x, axis.y, axis.z);
    }

    /**
     * Get the angle in degrees of the rotation around the specified axis. The axis must be normalized.
     * <p>
     * 获取绕指定轴旋转的以度表示的角度。轴必须已归一化。
     * @param axisX the x component of the normalized axis for which to get the angle 已归一化轴的 x 分量,用于获取角度
     * @param axisY the y component of the normalized axis for which to get the angle 已归一化轴的 y 分量,用于获取角度
     * @param axisZ the z component of the normalized axis for which to get the angle 已归一化轴的 z 分量,用于获取角度
     * @return the angle in degrees of the rotation around the specified axis 绕指定轴旋转的以度表示的角度
     */
    public float getAngleAround(final float axisX, final float axisY, final float axisZ){
        return getAngleAroundRad(axisX, axisY, axisZ) * Mathf.radiansToDegrees;
    }

    /**
     * Get the angle in degrees of the rotation around the specified axis. The axis must be normalized.
     * <p>
     * 获取绕指定轴旋转的以度表示的角度。轴必须已归一化。
     * @param axis the normalized axis for which to get the angle 已归一化的轴,用于获取角度
     * @return the angle in degrees of the rotation around the specified axis 绕指定轴旋转的以度表示的角度
     */
    public float getAngleAround(final Vec3 axis){
        return getAngleAround(axis.x, axis.y, axis.z);
    }
}
