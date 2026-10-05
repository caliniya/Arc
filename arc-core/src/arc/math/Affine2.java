package arc.math;

import arc.math.geom.*;
import arc.util.*;

/**
 * A specialized 3x3 matrix that can represent sequences of 2D translations, scales, flips, rotations, and shears. <a
 * href="http://en.wikipedia.org/wiki/Affine_transformation">Affine transformations</a> preserve straight lines, and
 * parallel lines remain parallel after the transformation. Operations on affine matrices are faster because the last row can
 * always be assumed (0, 0, 1).
 * <p>
 * 一种特殊的 3x3 矩阵,可表示 2D 平移、缩放、翻转、旋转和剪切的组合序列。<a href="http://en.wikipedia.org/wiki/Affine_transformation">仿射变换</a>保持直线性,平行线变换后仍保持平行。仿射矩阵上的运算更快,因为最后一行总可以假定 (0, 0, 1)。
 * @author vmilea
 */
public final class Affine2{
    public float m00 = 1, m01 = 0, m02 = 0;
    public float m10 = 0, m11 = 1, m12 = 0;

    // constant: m21 = 0, m21 = 1, m22 = 1
    // 常数:m21 = 0, m21 = 1, m22 = 1

    /**
     * Constructs an identity matrix.
     * 构造单位矩阵。
     */
    public Affine2(){
    }

    /**
     * Constructs a matrix from the given affine matrix.
     * <p>
     * 根据给定的仿射矩阵构造矩阵。
     * @param other The affine matrix to copy. This matrix will not be modified. 要复制的仿射矩阵。此矩阵不会被修改。
     */
    public Affine2(Affine2 other){
        set(other);
    }

    /**
     * Sets this matrix to the identity matrix
     * <p>
     * 将此矩阵设置为单位矩阵
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 idt(){
        m00 = 1;
        m01 = 0;
        m02 = 0;
        m10 = 0;
        m11 = 1;
        m12 = 0;
        return this;
    }

    /**
     * Copies the values from the provided affine matrix to this matrix.
     * <p>
     * 将给定仿射矩阵的值复制到此矩阵。
     * @param other The affine matrix to copy. 要复制的仿射矩阵。
     * @return This matrix for the purposes of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 set(Affine2 other){
        m00 = other.m00;
        m01 = other.m01;
        m02 = other.m02;
        m10 = other.m10;
        m11 = other.m11;
        m12 = other.m12;
        return this;
    }

    /**
     * Copies the values from the provided matrix to this matrix.
     * <p>
     * 将给定矩阵的值复制到此矩阵。
     * @param matrix The matrix to copy, assumed to be an affine transformation. 要复制的矩阵,假定为仿射变换。
     * @return This matrix for the purposes of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 set(Mat matrix){
        float[] other = matrix.val;

        m00 = other[Mat.M00];
        m01 = other[Mat.M01];
        m02 = other[Mat.M02];
        m10 = other[Mat.M10];
        m11 = other[Mat.M11];
        m12 = other[Mat.M12];
        return this;
    }

    /**
     * Sets this matrix to a translation matrix.
     * <p>
     * 将此矩阵设置为平移矩阵。
     * @param x The translation in x x 方向的平移
     * @param y The translation in y y 方向的平移
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToTranslation(float x, float y){
        m00 = 1;
        m01 = 0;
        m02 = x;
        m10 = 0;
        m11 = 1;
        m12 = y;
        return this;
    }

    /**
     * Sets this matrix to a translation matrix.
     * <p>
     * 将此矩阵设置为平移矩阵。
     * @param trn The translation vector. 平移向量。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToTranslation(Vec2 trn){
        return setToTranslation(trn.x, trn.y);
    }

    /**
     * Sets this matrix to a scaling matrix.
     * <p>
     * 将此矩阵设置为缩放矩阵。
     * @param scaleX The scale in x. x 方向上的缩放。
     * @param scaleY The scale in y. y 方向上的缩放。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToScaling(float scaleX, float scaleY){
        m00 = scaleX;
        m01 = 0;
        m02 = 0;
        m10 = 0;
        m11 = scaleY;
        m12 = 0;
        return this;
    }

    /**
     * Sets this matrix to a scaling matrix.
     * <p>
     * 将此矩阵设置为缩放矩阵。
     * @param scale The scale vector. 缩放向量。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToScaling(Vec2 scale){
        return setToScaling(scale.x, scale.y);
    }

    /**
     * Sets this matrix to a rotation matrix that will rotate any vector in counter-clockwise direction around the z-axis.
     * <p>
     * 将此矩阵设置为绕 z 轴逆时针旋转任意向量的旋转矩阵。
     * @param degrees The angle in degrees. 以度表示的角度。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToRotation(float degrees){
        float cos = Mathf.cosDeg(degrees);
        float sin = Mathf.sinDeg(degrees);

        m00 = cos;
        m01 = -sin;
        m02 = 0;
        m10 = sin;
        m11 = cos;
        m12 = 0;
        return this;
    }

    /**
     * Sets this matrix to a rotation matrix that will rotate any vector in counter-clockwise direction around the z-axis.
     * <p>
     * 将此矩阵设置为绕 z 轴逆时针旋转任意向量的旋转矩阵。
     * @param radians The angle in radians. 以弧度表示的角度。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToRotationRad(float radians){
        float cos = Mathf.cos(radians);
        float sin = Mathf.sin(radians);

        m00 = cos;
        m01 = -sin;
        m02 = 0;
        m10 = sin;
        m11 = cos;
        m12 = 0;
        return this;
    }

    /**
     * Sets this matrix to a rotation matrix that will rotate any vector in counter-clockwise direction around the z-axis.
     * <p>
     * 将此矩阵设置为绕 z 轴逆时针旋转任意向量的旋转矩阵。
     * @param cos The angle cosine. 角度的余弦值。
     * @param sin The angle sine. 角度的正弦值。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToRotation(float cos, float sin){
        m00 = cos;
        m01 = -sin;
        m02 = 0;
        m10 = sin;
        m11 = cos;
        m12 = 0;
        return this;
    }

    /**
     * Sets this matrix to a shearing matrix.
     * <p>
     * 将此矩阵设置为剪切矩阵。
     * @param shearX The shear in x direction. x 方向上的剪切。
     * @param shearY The shear in y direction. y 方向上的剪切。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToShearing(float shearX, float shearY){
        m00 = 1;
        m01 = shearX;
        m02 = 0;
        m10 = shearY;
        m11 = 1;
        m12 = 0;
        return this;
    }

    /**
     * Sets this matrix to a shearing matrix.
     * <p>
     * 将此矩阵设置为剪切矩阵。
     * @param shear The shear vector. 剪切向量。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToShearing(Vec2 shear){
        return setToShearing(shear.x, shear.y);
    }

    /**
     * Sets this matrix to a concatenation of translation, rotation and scale. It is a more efficient form for:
     * <code>idt().translate(x, y).rotate(degrees).scale(scaleX, scaleY)</code>
     * <p>
     * 将此矩阵设置为平移、旋转和缩放的组合。这是 <code>idt().translate(x, y).rotate(degrees).scale(scaleX, scaleY)</code> 更高效的形式
     * @param x The translation in x. x 方向的平移。
     * @param y The translation in y. y 方向的平移。
     * @param degrees The angle in degrees. 以度表示的角度。
     * @param scaleX The scale in y. y 方向上的缩放。
     * @param scaleY The scale in x. x 方向上的缩放。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToTrnRotScl(float x, float y, float degrees, float scaleX, float scaleY){
        m02 = x;
        m12 = y;

        if(degrees == 0){
            m00 = scaleX;
            m01 = 0;
            m10 = 0;
            m11 = scaleY;
        }else{
            float sin = Mathf.sinDeg(degrees);
            float cos = Mathf.cosDeg(degrees);

            m00 = cos * scaleX;
            m01 = -sin * scaleY;
            m10 = sin * scaleX;
            m11 = cos * scaleY;
        }
        return this;
    }

    /**
     * Sets this matrix to a concatenation of translation, rotation and scale. It is a more efficient form for:
     * <code>idt().translate(trn).rotate(degrees).scale(scale)</code>
     * <p>
     * 将此矩阵设置为平移、旋转和缩放的组合。这是 <code>idt().translate(trn).rotate(degrees).scale(scale)</code> 更高效的形式
     * @param trn The translation vector. 平移向量。
     * @param degrees The angle in degrees. 以度表示的角度。
     * @param scale The scale vector. 缩放向量。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToTrnRotScl(Vec2 trn, float degrees, Vec2 scale){
        return setToTrnRotScl(trn.x, trn.y, degrees, scale.x, scale.y);
    }

    /**
     * Sets this matrix to a concatenation of translation, rotation and scale. It is a more efficient form for:
     * <code>idt().translate(x, y).rotateRad(radians).scale(scaleX, scaleY)</code>
     * <p>
     * 将此矩阵设置为平移、旋转和缩放的组合。这是 <code>idt().translate(x, y).rotateRad(radians).scale(scaleX, scaleY)</code> 更高效的形式
     * @param x The translation in x. x 方向的平移。
     * @param y The translation in y. y 方向的平移。
     * @param radians The angle in radians. 以弧度表示的角度。
     * @param scaleX The scale in y. y 方向上的缩放。
     * @param scaleY The scale in x. x 方向上的缩放。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToTrnRotRadScl(float x, float y, float radians, float scaleX, float scaleY){
        m02 = x;
        m12 = y;

        if(radians == 0){
            m00 = scaleX;
            m01 = 0;
            m10 = 0;
            m11 = scaleY;
        }else{
            float sin = Mathf.sin(radians);
            float cos = Mathf.cos(radians);

            m00 = cos * scaleX;
            m01 = -sin * scaleY;
            m10 = sin * scaleX;
            m11 = cos * scaleY;
        }
        return this;
    }

    /**
     * Sets this matrix to a concatenation of translation, rotation and scale. It is a more efficient form for:
     * <code>idt().translate(trn).rotateRad(radians).scale(scale)</code>
     * <p>
     * 将此矩阵设置为平移、旋转和缩放的组合。这是 <code>idt().translate(trn).rotateRad(radians).scale(scale)</code> 更高效的形式
     * @param trn The translation vector. 平移向量。
     * @param radians The angle in radians. 以弧度表示的角度。
     * @param scale The scale vector. 缩放向量。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToTrnRotRadScl(Vec2 trn, float radians, Vec2 scale){
        return setToTrnRotRadScl(trn.x, trn.y, radians, scale.x, scale.y);
    }

    /**
     * Sets this matrix to a concatenation of translation and scale. It is a more efficient form for:
     * <code>idt().translate(x, y).scale(scaleX, scaleY)</code>
     * <p>
     * 将此矩阵设置为平移与缩放的组合。这是 <code>idt().translate(x, y).scale(scaleX, scaleY)</code> 更高效的形式
     * @param x The translation in x. x 方向的平移。
     * @param y The translation in y. y 方向的平移。
     * @param scaleX The scale in y. y 方向上的缩放。
     * @param scaleY The scale in x. x 方向上的缩放。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToTrnScl(float x, float y, float scaleX, float scaleY){
        m00 = scaleX;
        m01 = 0;
        m02 = x;
        m10 = 0;
        m11 = scaleY;
        m12 = y;
        return this;
    }

    /**
     * Sets this matrix to a concatenation of translation and scale. It is a more efficient form for:
     * <code>idt().translate(trn).scale(scale)</code>
     * <p>
     * 将此矩阵设置为平移与缩放的组合。这是 <code>idt().translate(trn).scale(scale)</code> 更高效的形式
     * @param trn The translation vector. 平移向量。
     * @param scale The scale vector. 缩放向量。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToTrnScl(Vec2 trn, Vec2 scale){
        return setToTrnScl(trn.x, trn.y, scale.x, scale.y);
    }

    /**
     * Sets this matrix to the product of two matrices.
     * <p>
     * 将此矩阵设置为两个矩阵的乘积。
     * @param l Left matrix. 左矩阵。
     * @param r Right matrix. 右矩阵。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 setToProduct(Affine2 l, Affine2 r){
        m00 = l.m00 * r.m00 + l.m01 * r.m10;
        m01 = l.m00 * r.m01 + l.m01 * r.m11;
        m02 = l.m00 * r.m02 + l.m01 * r.m12 + l.m02;
        m10 = l.m10 * r.m00 + l.m11 * r.m10;
        m11 = l.m10 * r.m01 + l.m11 * r.m11;
        m12 = l.m10 * r.m02 + l.m11 * r.m12 + l.m12;
        return this;
    }

    /**
     * Inverts this matrix given that the determinant is != 0.
     * <p>
     * 在行列式 != 0 的前提下求此矩阵的逆。
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     * @throws ArcRuntimeException if the matrix is singular (not invertible) 矩阵奇异(不可逆)时抛出。
     */
    public Affine2 inv(){
        float det = det();
        if(det == 0) throw new ArcRuntimeException("Can't invert a singular affine matrix");

        float invDet = 1.0f / det;

        float tmp00 = m11;
        float tmp01 = -m01;
        float tmp02 = m01 * m12 - m11 * m02;
        float tmp10 = -m10;
        float tmp11 = m00;
        float tmp12 = m10 * m02 - m00 * m12;

        m00 = invDet * tmp00;
        m01 = invDet * tmp01;
        m02 = invDet * tmp02;
        m10 = invDet * tmp10;
        m11 = invDet * tmp11;
        m12 = invDet * tmp12;
        return this;
    }

    /**
     * Postmultiplies this matrix with the provided matrix and stores the result in this matrix. For example:
     *
     * <pre>
     * A.mul(B) results in A := AB
     * </pre>
     * <p>
     * 后乘给定矩阵,结果存入此矩阵。例如: <pre> A.mul(B) results in A := AB </pre>
     * @param other Matrix to multiply by. 用于相乘的矩阵。
     * @return This matrix for the purpose of chaining operations together. 此矩阵,用于链式操作。
     */
    public Affine2 mul(Affine2 other){
        float tmp00 = m00 * other.m00 + m01 * other.m10;
        float tmp01 = m00 * other.m01 + m01 * other.m11;
        float tmp02 = m00 * other.m02 + m01 * other.m12 + m02;
        float tmp10 = m10 * other.m00 + m11 * other.m10;
        float tmp11 = m10 * other.m01 + m11 * other.m11;
        float tmp12 = m10 * other.m02 + m11 * other.m12 + m12;

        m00 = tmp00;
        m01 = tmp01;
        m02 = tmp02;
        m10 = tmp10;
        m11 = tmp11;
        m12 = tmp12;
        return this;
    }

    /**
     * Premultiplies this matrix with the provided matrix and stores the result in this matrix. For example:
     *
     * <pre>
     * A.preMul(B) results in A := BA
     * </pre>
     * <p>
     * 前乘给定矩阵,结果存入此矩阵。例如: <pre> A.preMul(B) results in A := BA </pre>
     * @param other The other Matrix to multiply by 用于相乘的另一个矩阵
     * @return This matrix for the purpose of chaining operations. 此矩阵,用于链式操作。
     */
    public Affine2 preMul(Affine2 other){
        float tmp00 = other.m00 * m00 + other.m01 * m10;
        float tmp01 = other.m00 * m01 + other.m01 * m11;
        float tmp02 = other.m00 * m02 + other.m01 * m12 + other.m02;
        float tmp10 = other.m10 * m00 + other.m11 * m10;
        float tmp11 = other.m10 * m01 + other.m11 * m11;
        float tmp12 = other.m10 * m02 + other.m11 * m12 + other.m12;

        m00 = tmp00;
        m01 = tmp01;
        m02 = tmp02;
        m10 = tmp10;
        m11 = tmp11;
        m12 = tmp12;
        return this;
    }

    /**
     * Postmultiplies this matrix by a translation matrix.
     * <p>
     * 后乘一个平移矩阵。
     * @param x The x-component of the translation vector. 平移向量的 x 分量。
     * @param y The y-component of the translation vector. 平移向量的 y 分量。
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 translate(float x, float y){
        m02 += m00 * x + m01 * y;
        m12 += m10 * x + m11 * y;
        return this;
    }

    /**
     * Postmultiplies this matrix by a translation matrix.
     * <p>
     * 后乘一个平移矩阵。
     * @param trn The translation vector. 平移向量。
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 translate(Vec2 trn){
        return translate(trn.x, trn.y);
    }

    /**
     * Premultiplies this matrix by a translation matrix.
     * <p>
     * 前乘一个平移矩阵。
     * @param x The x-component of the translation vector. 平移向量的 x 分量。
     * @param y The y-component of the translation vector. 平移向量的 y 分量。
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 preTranslate(float x, float y){
        m02 += x;
        m12 += y;
        return this;
    }

    /**
     * Premultiplies this matrix by a translation matrix.
     * <p>
     * 前乘一个平移矩阵。
     * @param trn The translation vector. 平移向量。
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 preTranslate(Vec2 trn){
        return preTranslate(trn.x, trn.y);
    }

    /**
     * Postmultiplies this matrix with a scale matrix.
     * <p>
     * 后乘一个缩放矩阵。
     * @param scaleX The scale in the x-axis. x 轴上的缩放。
     * @param scaleY The scale in the y-axis. y 轴上的缩放。
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 scale(float scaleX, float scaleY){
        m00 *= scaleX;
        m01 *= scaleY;
        m10 *= scaleX;
        m11 *= scaleY;
        return this;
    }

    /**
     * Postmultiplies this matrix with a scale matrix.
     * <p>
     * 后乘一个缩放矩阵。
     * @param scale The scale vector. 缩放向量。
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 scale(Vec2 scale){
        return scale(scale.x, scale.y);
    }

    /**
     * Premultiplies this matrix with a scale matrix.
     * <p>
     * 前乘一个缩放矩阵。
     * @param scaleX The scale in the x-axis. x 轴上的缩放。
     * @param scaleY The scale in the y-axis. y 轴上的缩放。
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 preScale(float scaleX, float scaleY){
        m00 *= scaleX;
        m01 *= scaleX;
        m02 *= scaleX;
        m10 *= scaleY;
        m11 *= scaleY;
        m12 *= scaleY;
        return this;
    }

    /**
     * Premultiplies this matrix with a scale matrix.
     * <p>
     * 前乘一个缩放矩阵。
     * @param scale The scale vector. 缩放向量。
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 preScale(Vec2 scale){
        return preScale(scale.x, scale.y);
    }

    /**
     * Postmultiplies this matrix with a (counter-clockwise) rotation matrix.
     * <p>
     * 后乘一个(逆时针)旋转矩阵。
     * @param degrees The angle in degrees 以度表示的角度
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 rotate(float degrees){
        if(degrees == 0) return this;

        float cos = Mathf.cosDeg(degrees);
        float sin = Mathf.sinDeg(degrees);

        float tmp00 = m00 * cos + m01 * sin;
        float tmp01 = m00 * -sin + m01 * cos;
        float tmp10 = m10 * cos + m11 * sin;
        float tmp11 = m10 * -sin + m11 * cos;

        m00 = tmp00;
        m01 = tmp01;
        m10 = tmp10;
        m11 = tmp11;
        return this;
    }

    /**
     * Postmultiplies this matrix with a (counter-clockwise) rotation matrix.
     * <p>
     * 后乘一个(逆时针)旋转矩阵。
     * @param radians The angle in radians 以弧度表示的角度
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 rotateRad(float radians){
        if(radians == 0) return this;

        float cos = Mathf.cos(radians);
        float sin = Mathf.sin(radians);

        float tmp00 = m00 * cos + m01 * sin;
        float tmp01 = m00 * -sin + m01 * cos;
        float tmp10 = m10 * cos + m11 * sin;
        float tmp11 = m10 * -sin + m11 * cos;

        m00 = tmp00;
        m01 = tmp01;
        m10 = tmp10;
        m11 = tmp11;
        return this;
    }

    /**
     * Premultiplies this matrix with a (counter-clockwise) rotation matrix.
     * <p>
     * 前乘一个(逆时针)旋转矩阵。
     * @param degrees The angle in degrees 以度表示的角度
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 preRotate(float degrees){
        if(degrees == 0) return this;

        float cos = Mathf.cosDeg(degrees);
        float sin = Mathf.sinDeg(degrees);

        float tmp00 = cos * m00 - sin * m10;
        float tmp01 = cos * m01 - sin * m11;
        float tmp02 = cos * m02 - sin * m12;
        float tmp10 = sin * m00 + cos * m10;
        float tmp11 = sin * m01 + cos * m11;
        float tmp12 = sin * m02 + cos * m12;

        m00 = tmp00;
        m01 = tmp01;
        m02 = tmp02;
        m10 = tmp10;
        m11 = tmp11;
        m12 = tmp12;
        return this;
    }

    /**
     * Premultiplies this matrix with a (counter-clockwise) rotation matrix.
     * <p>
     * 前乘一个(逆时针)旋转矩阵。
     * @param radians The angle in radians 以弧度表示的角度
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 preRotateRad(float radians){
        if(radians == 0) return this;

        float cos = Mathf.cos(radians);
        float sin = Mathf.sin(radians);

        float tmp00 = cos * m00 - sin * m10;
        float tmp01 = cos * m01 - sin * m11;
        float tmp02 = cos * m02 - sin * m12;
        float tmp10 = sin * m00 + cos * m10;
        float tmp11 = sin * m01 + cos * m11;
        float tmp12 = sin * m02 + cos * m12;

        m00 = tmp00;
        m01 = tmp01;
        m02 = tmp02;
        m10 = tmp10;
        m11 = tmp11;
        m12 = tmp12;
        return this;
    }

    /**
     * Postmultiplies this matrix by a shear matrix.
     * <p>
     * 后乘一个剪切矩阵。
     * @param shearX The shear in x direction. x 方向上的剪切。
     * @param shearY The shear in y direction. y 方向上的剪切。
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 shear(float shearX, float shearY){
        float tmp0 = m00 + shearY * m01;
        float tmp1 = m01 + shearX * m00;
        m00 = tmp0;
        m01 = tmp1;

        tmp0 = m10 + shearY * m11;
        tmp1 = m11 + shearX * m10;
        m10 = tmp0;
        m11 = tmp1;
        return this;
    }

    /**
     * Postmultiplies this matrix by a shear matrix.
     * <p>
     * 后乘一个剪切矩阵。
     * @param shear The shear vector. 剪切向量。
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 shear(Vec2 shear){
        return shear(shear.x, shear.y);
    }

    /**
     * Premultiplies this matrix by a shear matrix.
     * <p>
     * 前乘一个剪切矩阵。
     * @param shearX The shear in x direction. x 方向上的剪切。
     * @param shearY The shear in y direction. y 方向上的剪切。
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 preShear(float shearX, float shearY){
        float tmp00 = m00 + shearX * m10;
        float tmp01 = m01 + shearX * m11;
        float tmp02 = m02 + shearX * m12;
        float tmp10 = m10 + shearY * m00;
        float tmp11 = m11 + shearY * m01;
        float tmp12 = m12 + shearY * m02;

        m00 = tmp00;
        m01 = tmp01;
        m02 = tmp02;
        m10 = tmp10;
        m11 = tmp11;
        m12 = tmp12;
        return this;
    }

    /**
     * Premultiplies this matrix by a shear matrix.
     * <p>
     * 前乘一个剪切矩阵。
     * @param shear The shear vector. 剪切向量。
     * @return This matrix for the purpose of chaining. 此矩阵,用于链式调用。
     */
    public Affine2 preShear(Vec2 shear){
        return preShear(shear.x, shear.y);
    }

    /**
     * Calculates the determinant of the matrix.
     * <p>
     * 计算矩阵的行列式。
     * @return The determinant of this matrix. 此矩阵的行列式。
     */
    public float det(){
        return m00 * m11 - m01 * m10;
    }

    /**
     * Get the x-y translation component of the matrix.
     * <p>
     * 获取矩阵的 x-y 平移分量。
     * @param position Output vector. 输出向量。
     * @return Filled position. 已填充的位置。
     */
    public Vec2 getTranslation(Vec2 position){
        position.x = m02;
        position.y = m12;
        return position;
    }

    /**
     * Check if the this is a plain translation matrix.
     * <p>
     * 检查此矩阵是否为纯平移矩阵。
     * @return True if scale is 1 and rotation is 0. 若缩放为 1 且旋转为 0 则为 true。
     */
    public boolean isTranslation(){
        return (m00 == 1 && m11 == 1 && m01 == 0 && m10 == 0);
    }

    /**
     * Check if this is an indentity matrix.
     * <p>
     * 检查此矩阵是否为单位矩阵。
     * @return True if scale is 1 and rotation is 0. 若缩放为 1 且旋转为 0 则为 true。
     */
    public boolean isIdt(){
        return (m00 == 1 && m02 == 0 && m12 == 0 && m11 == 1 && m01 == 0 && m10 == 0);
    }

    /**
     * Applies the affine transformation on a vector.
     * 对向量应用仿射变换。
     */
    public void applyTo(Vec2 point){
        float x = point.x;
        float y = point.y;
        point.x = m00 * x + m01 * y + m02;
        point.y = m10 * x + m11 * y + m12;
    }

    @Override
    public String toString(){
        return "[" + m00 + "|" + m01 + "|" + m02 + "]\n[" + m10 + "|" + m11 + "|" + m12 + "]\n[0.0|0.0|0.1]";
    }
}
