package arc.graphics;

import arc.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;

public class Camera{
    /**
     * temporary vector which is returned.
     * 作为返回值的临时向量。
     */
    private static final Vec2 tmpVector = new Vec2();
    /**
     * the position of the camera *
     * 相机的位置 *
     */
    public final Vec2 position = new Vec2();
    /**
     * the view matrix*
     * 视图矩阵*
     */
    public final Mat mat = new Mat();
    /**
     * the inverse view matrix *
     * 逆视图矩阵 *
     */
    public final Mat inv = new Mat();
    /**
     * the viewport width and height *
     * 视口宽度和高度 *
     */
    public float width, height;

    /**
     * Recalculates the projection and view matrix of this camera. Use this after you've manipulated
     * any of the attributes of the camera.
     * <p>
     * 重新计算此相机的投影矩阵和视图矩阵。在修改相机的任意属性后调用此方法。
     */
    public void update(){
        mat.setOrtho(position.x - width / 2f, position.y - height / 2f, width, height);
        inv.set(mat).inv();
    }

    public void resize(float viewportWidth, float viewportHeight){
        this.width = viewportWidth;
        this.height = viewportHeight;
        update();
    }

    /**
     * Function to translate a point given in screen coordinates to world space. It's the same as GLU gluUnProject, but does not
     * rely on OpenGL. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the bottom left corner, y
     * pointing up, x pointing to the right) as reported by the touch methods in {@link Input}. A z-coordinate of 0 will return a
     * point on the near plane, a z-coordinate of 1 will return a point on the far plane. This method allows you to specify the
     * viewport position and dimensions in the coordinate system expected by {@link Gl#viewport(int, int, int, int)}, with the
     * origin in the bottom left corner of the screen.
     * <p>
     * 将屏幕坐标表示的点转换到世界空间。与 GLU 的 gluUnProject 相同,但不依赖 OpenGL。vec 的 x、y 坐标被视为屏幕坐标(原点在左下角,y 向上,x 向右),与 {@link Input} 中触摸方法报告的坐标一致。z 坐标为 0 时返回近平面上的点,z 坐标为 1 时返回远平面上的点。此方法允许你按 {@link Gl#viewport(int, int, int, int)} 所期望的坐标系指定视口的位置和尺寸,原点位于屏幕左下角。
     * @param screenCoords the point in screen coordinates (origin top left) 屏幕坐标中的点(原点在左上)
     * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates. 视口左下角在 glViewport 坐标系中的坐标。
     * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates. 视口左下角在 glViewport 坐标系中的坐标。
     * @param viewportWidth the width of the viewport in pixels 视口宽度,以像素为单位
     * @param viewportHeight the height of the viewport in pixels 视口高度,以像素为单位
     * @return the mutated and unprojected screenCoords {@link Vec3} 变换并反投影后的 screenCoords {@link Vec3}
     */
    public Vec2 unproject(Vec2 screenCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight){
        float x = screenCoords.x - viewportX, y = screenCoords.y - viewportY;
        screenCoords.x = (2 * x) / viewportWidth - 1;
        screenCoords.y = (2 * y) / viewportHeight - 1;
        screenCoords.mul(inv);
        return screenCoords;
    }

    /**
     * Function to translate a point given in screen coordinates to world space. It's the same as GLU gluUnProject but does not
     * rely on OpenGL. The viewport is assumed to span the whole screen and is fetched from {@link Graphics#getWidth()} and
     * {@link Graphics#getHeight()}. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the bottom left
     * corner, y pointing up, x pointing to the right) as reported by the touch methods in {@link Input}. A z-coordinate of 0
     * will return a point on the near plane, a z-coordinate of 1 will return a point on the far plane.
     * <p>
     * 将屏幕坐标表示的点转换到世界空间。与 GLU 的 gluUnProject 相同,但不依赖 OpenGL。视口被假定覆盖整个屏幕,并从 {@link Graphics#getWidth()} 和 {@link Graphics#getHeight()} 获取。vec 的 x、y 坐标被视为屏幕坐标(原点在左下角,y 向上,x 向右),与 {@link Input} 中触摸方法报告的坐标一致。z 坐标为 0 时返回近平面上的点,z 坐标为 1 时返回远平面上的点。
     * @param screenCoords the point in screen coordinates 屏幕坐标中的点
     * @return the mutated and unprojected screenCoords {@link Vec3} 变换并反投影后的 screenCoords {@link Vec3}
     */
    public Vec2 unproject(Vec2 screenCoords){
        unproject(screenCoords, 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        return screenCoords;
    }

    /**
     * See {@link #unproject(Vec2)}. Returns the same Vec2 each time.
     * 参见 {@link #unproject(Vec2)}。每次返回同一个 Vec2。
     */
    public Vec2 unproject(float screenX, float screenY){
        unproject(tmpVector.set(screenX, screenY), 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        return tmpVector;
    }

    /**
     * Projects the {@link Vec3} given in world space to screen coordinates. It's the same as GLU gluProject with one small
     * deviation: The viewport is assumed to span the whole screen. The screen coordinate system has its origin in the
     * <b>bottom</b> left, with the y-axis pointing <b>upwards</b> and the x-axis pointing to the right.
     * <p>
     * 将世界空间中的 {@link Vec3} 投影到屏幕坐标。与 GLU 的 gluProject 相同,仅有一点差别:视口被假定覆盖整个屏幕。屏幕坐标系的原点在<b>左下角</b>,y 轴向<b>上</b>,x 轴向右。
     * @return the mutated and projected worldCoords {@link Vec3} 变换并投影后的 worldCoords {@link Vec3}
     */
    public Vec2 project(Vec2 worldCoords){
        project(worldCoords, 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        return worldCoords;
    }

    /**
     * See {@link #project(Vec2)}. Returns the same Vec2 each time.
     * 参见 {@link #project(Vec2)}。每次返回同一个 Vec2。
     */
    public Vec2 project(float screenX, float screenY){
        project(tmpVector.set(screenX, screenY), 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        return tmpVector;
    }

    /**
     * Projects the {@link Vec3} given in world space to screen coordinates. It's the same as GLU gluProject with one small
     * deviation: The viewport is assumed to span the whole screen. The screen coordinate system has its origin in the
     * <b>bottom</b> left, with the y-axis pointing <b>upwards</b> and the x-axis pointing to the right.
     * This method allows you to specify the viewport position and
     * dimensions in the coordinate system expected by {@link Gl#viewport(int, int, int, int)}, with the origin in the bottom
     * left corner of the screen.
     * <p>
     * 将世界空间中的 {@link Vec3} 投影到屏幕坐标。与 GLU 的 gluProject 相同,仅有一点差别:视口被假定覆盖整个屏幕。屏幕坐标系的原点在<b>左下角</b>,y 轴向<b>上</b>,x 轴向右。此方法允许你按 {@link Gl#viewport(int, int, int, int)} 所期望的坐标系指定视口的位置和尺寸,原点位于屏幕左下角。
     * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates. 视口左下角在 glViewport 坐标系中的坐标。
     * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates. 视口左下角在 glViewport 坐标系中的坐标。
     * @param viewportWidth the width of the viewport in pixels 视口宽度,以像素为单位
     * @param viewportHeight the height of the viewport in pixels 视口高度,以像素为单位
     * @return the mutated and projected worldCoords {@link Vec3} 变换并投影后的 worldCoords {@link Vec3}
     */
    public Vec2 project(Vec2 worldCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight){
        worldCoords.mul(mat);
        worldCoords.x = viewportWidth * (worldCoords.x + 1) / 2 + viewportX;
        worldCoords.y = viewportHeight * (worldCoords.y + 1) / 2 + viewportY;
        return worldCoords;
    }

    /**
     * Sets the specified rectangle to this camera's bounds.
     * 将指定矩形设置为此相机的边界。
     */
    public Rect bounds(Rect out){
        return out.setSize(width, height).setCenter(position);
    }
}
