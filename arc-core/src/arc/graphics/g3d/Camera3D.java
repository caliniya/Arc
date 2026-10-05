package arc.graphics.g3d;

import arc.*;
import arc.math.*;
import arc.math.geom.*;

public class Camera3D{
    /**
     * field of view.
     * 视场角。
     */
    public float fov = 67;
    /**
     * the near clipping plane distance, has to be positive
     * 近裁剪面距离,必须为正
     */
    public float near = 1;
    /**
     * the far clipping plane distance, has to be positive
     * 远裁剪面距离,必须为正
     */
    public float far = 100;
    /**
     * if set to true, a perspective projection is used.
     * 若为 true,使用透视投影。
     */
    public boolean perspective = true;
    /**
     * the viewport width and height
     * 视口宽度和高度
     */
    public float width, height;
    /**
     * the position of the camera
     * 相机位置
     */
    public final Vec3 position = new Vec3();
    /**
     * the unit length direction vector of the camera
     * 相机的单位长度方向向量
     */
    public final Vec3 direction = new Vec3(0, 0, -1);
    /**
     * the unit length up vector of the camera
     * 相机的单位长度 up 向量
     */
    public final Vec3 up = new Vec3(0, 1, 0);

    /**
     * the combined projection and view matrix
     * 投影与视图的合成矩阵
     */
    public final Mat3D combined = new Mat3D();
    /**
     * the projection matrix
     * 投影矩阵
     */
    public final Mat3D projection = new Mat3D();
    /**
     * the view matrix
     * 视图矩阵
     */
    public final Mat3D view = new Mat3D();
    /**
     * the inverse combined projection and view matrix
     * 投影与视图合成矩阵的逆矩阵
     */
    public final Mat3D invProjectionView = new Mat3D();

    /**
     * the frustum, for clipping operations
     * 视锥体,用于裁剪操作
     */
    public final Frustum frustum = new Frustum();

    private final Vec3 tmpVec = new Vec3();
    private final Ray ray = new Ray(new Vec3(), new Vec3());

    public void update(){
        if(perspective){
            projection.setToProjection(Math.abs(near), Math.abs(far), fov, width / height);
        }else{
            projection.setToOrtho(-width / 2, width / 2, -height / 2, height / 2, near, far);
        }

        view.setToLookAt(position, tmpVec.set(position).add(direction), up);
        combined.set(projection).mul(view);
        invProjectionView.set(combined).inv();
        frustum.update(invProjectionView);
    }

    public void resize(float width, float height){
        this.width = width;
        this.height = height;
    }

    public void lookAt(float x, float y, float z){
        tmpVec.set(x, y, z).sub(position).nor();
        if(!tmpVec.isZero()){
            float dot = tmpVec.dot(up); // up and direction must ALWAYS be orthonormal vectors
            // up 和 direction 必须始终是标准正交向量
            if(Math.abs(dot - 1) < 0.000000001f){
                // Collinear
                // 共线
                up.set(direction).scl(-1);
            }else if(Math.abs(dot + 1) < 0.000000001f){
                // Collinear opposite
                // 反向共线
                up.set(direction);
            }
            direction.set(tmpVec);
            normalizeUp();
        }
    }

    /**
     * Recalculates the direction of the camera to look at the point (x, y, z).
     * <p>
     * 重新计算相机方向,使其看向点 (x, y, z)。
     * @param target the point to look at 要看向的点
     */
    public void lookAt(Vec3 target){
        lookAt(target.x, target.y, target.z);
    }

    /**
     * Normalizes the up vector by first calculating the right vector via a cross product between direction and up, and then
     * recalculating the up vector via a cross product between right and direction.
     * <p>
     * 规范化 up 向量:先通过 direction 与 up 的叉积计算 right 向量,再通过 right 与 direction 的叉积重新计算 up 向量。
     */
    public void normalizeUp(){
        tmpVec.set(direction).crs(up);
        up.set(tmpVec).crs(direction).nor();
    }

    /**
     * Function to translate a point given in screen coordinates to world space. It's the same as GLU gluUnProject, but does not
     * rely on OpenGL. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the top left corner, y
     * pointing down, x pointing to the right) as reported by the touch methods in {@link Input}. A z-coordinate of 0 will return a
     * point on the near plane, a z-coordinate of 1 will return a point on the far plane. This method allows you to specify the
     * viewport position and dimensions in the coordinate system expected by glViewport(int, int, int, int), with the
     * origin in the bottom left corner of the screen.
     * <p>
     * 将屏幕坐标表示的点转换到世界空间。与 GLU 的 gluUnProject 相同,但不依赖 OpenGL。vec 的 x、y 坐标被假定为由 {@link Input} 的触摸方法报告的屏幕坐标(原点在左上角,y 向下,x 向右)。z 坐标为 0 返回近裁剪面上的点,z 坐标为 1 返回远裁剪面上的点。此方法允许按 glViewport(int, int, int, int) 期望的坐标系(原点位于屏幕左下角)指定视口位置和尺寸。
     * @param screenCoords the point in screen coordinates (origin top left) 屏幕坐标下的点(原点在左上)
     * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates. 视口左下角在 glViewport 坐标系中的坐标。
     * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates. 视口左下角在 glViewport 坐标系中的坐标。
     * @param viewportWidth the width of the viewport in pixels 视口宽度(像素)
     * @param viewportHeight the height of the viewport in pixels 视口高度(像素)
     * @return the mutated and unprojected screenCoords {@link Vec3} 原地转换并反投影后的 screenCoords {@link Vec3}
     */
    public Vec3 unproject(Vec3 screenCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight){
        float x = screenCoords.x, y = screenCoords.y;
        x = x - viewportX;
        y = y - viewportY;
        screenCoords.x = (2 * x) / viewportWidth - 1;
        screenCoords.y = (2 * y) / viewportHeight - 1;
        screenCoords.z = 2 * screenCoords.z - 1;
        Mat3D.prj(screenCoords, invProjectionView);
        return screenCoords;
    }

    /**
     * Function to translate a point given in screen coordinates to world space. It's the same as GLU gluUnProject but does not
     * rely on OpenGL. The viewport is assumed to span the whole screen and is fetched from {@link Graphics#getWidth()} and
     * {@link Graphics#getHeight()}. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the top left
     * corner, y pointing down, x pointing to the right) as reported by the touch methods in {@link Input}. A z-coordinate of 0
     * will return a point on the near plane, a z-coordinate of 1 will return a point on the far plane.
     * <p>
     * 将屏幕坐标表示的点转换到世界空间。与 GLU 的 gluUnProject 相同,但不依赖 OpenGL。假定视口覆盖整个屏幕,并从 {@link Graphics#getWidth()} 和 {@link Graphics#getHeight()} 获取。vec 的 x、y 坐标被假定为由 {@link Input} 的触摸方法报告的屏幕坐标(原点在左上角,y 向下,x 向右)。z 坐标为 0 返回近裁剪面上的点,z 坐标为 1 返回远裁剪面上的点。
     * @param screenCoords the point in screen coordinates 屏幕坐标下的点
     * @return the mutated and unprojected screenCoords {@link Vec3} 原地转换并反投影后的 screenCoords {@link Vec3}
     */
    public Vec3 unproject(Vec3 screenCoords){
        unproject(screenCoords, 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        return screenCoords;
    }

    /**
     * Projects the {@link Vec3} given in world space to screen coordinates. It's the same as GLU gluProject with one small
     * deviation: The viewport is assumed to span the whole screen. The screen coordinate system has its origin in the
     * <b>bottom</b> left, with the y-axis pointing <b>upwards</b> and the x-axis pointing to the right. This makes it easily
     * useable in conjunction with Batch and similar classes.
     * <p>
     * 将世界空间的 {@link Vec3} 投影到屏幕坐标。与 GLU 的 gluProject 相同,仅有一处小偏差:假定视口覆盖整个屏幕。屏幕坐标系原点位于<b>左</b>下角,y 轴向<b>上</b>,x 轴向右。这使其易于与 Batch 等类配合使用。
     * @return the mutated and projected worldCoords {@link Vec3} 原地投影后的 worldCoords {@link Vec3}
     */
    public Vec3 project(Vec3 worldCoords){
        project(worldCoords, 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        return worldCoords;
    }

    /**
     * Projects the {@link Vec3} given in world space to screen coordinates. It's the same as GLU gluProject with one small
     * deviation: The viewport is assumed to span the whole screen. The screen coordinate system has its origin in the
     * <b>bottom</b> left, with the y-axis pointing <b>upwards</b> and the x-axis pointing to the right. This makes it easily
     * useable in conjunction with Batch and similar classes. This method allows you to specify the viewport position and
     * dimensions in the coordinate system expected by glViewport(int, int, int, int), with the origin in the bottom
     * left corner of the screen.
     * <p>
     * 将世界空间的 {@link Vec3} 投影到屏幕坐标。与 GLU 的 gluProject 相同,仅有一处小偏差:假定视口覆盖整个屏幕。屏幕坐标系原点位于<b>左</b>下角,y 轴向<b>上</b>,x 轴向右。这使其易于与 Batch 等类配合使用。此方法允许按 glViewport(int, int, int, int) 期望的坐标系(原点位于屏幕左下角)指定视口位置和尺寸。
     * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates. 视口左下角在 glViewport 坐标系中的坐标。
     * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates. 视口左下角在 glViewport 坐标系中的坐标。
     * @param viewportWidth the width of the viewport in pixels 视口宽度(像素)
     * @param viewportHeight the height of the viewport in pixels 视口高度(像素)
     * @return the mutated and projected worldCoords {@link Vec3} 原地投影后的 worldCoords {@link Vec3}
     */
    public Vec3 project(Vec3 worldCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight){
        Mat3D.prj(worldCoords, combined);
        worldCoords.x = viewportWidth * (worldCoords.x + 1) / 2 + viewportX;
        worldCoords.y = viewportHeight * (worldCoords.y + 1) / 2 + viewportY;
        worldCoords.z = (worldCoords.z + 1) / 2;
        return worldCoords;
    }

    public Ray getMouseRay(){
        return getPickRay(Core.input.mouseX(), Core.input.mouseY());
    }

    /**
     * Creates a picking {@link Ray} from the coordinates given in screen coordinates. It is assumed that the viewport spans the
     * whole screen. The screen coordinates origin is assumed to be in the top left corner, its y-axis pointing down, the x-axis
     * pointing to the right. The returned instance is not a new instance but an internal member only accessible via this function.
     * <p>
     * 根据屏幕坐标创建拾取 {@link Ray}。假定视口覆盖整个屏幕,屏幕坐标原点在左上角,y 轴向下,x 轴向右。返回的实例不是新实例,而是仅能通过此函数访问的内部成员。
     * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates. 视口左下角在 glViewport 坐标系中的坐标。
     * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates. 视口左下角在 glViewport 坐标系中的坐标。
     * @param viewportWidth the width of the viewport in pixels 视口宽度(像素)
     * @param viewportHeight the height of the viewport in pixels 视口高度(像素)
     * @return the picking Ray. 拾取射线。
     */
    public Ray getPickRay(float screenX, float screenY, float viewportX, float viewportY, float viewportWidth, float viewportHeight){
        unproject(ray.origin.set(screenX, screenY, 0), viewportX, viewportY, viewportWidth, viewportHeight);
        unproject(ray.direction.set(screenX, screenY, 1), viewportX, viewportY, viewportWidth, viewportHeight);
        ray.direction.sub(ray.origin).nor();
        return ray;
    }

    /**
     * Creates a picking {@link Ray} from the coordinates given in screen coordinates. It is assumed that the viewport spans the
     * whole screen. The screen coordinates origin is assumed to be in the top left corner, its y-axis pointing down, the x-axis
     * pointing to the right. The returned instance is not a new instance but an internal member only accessible via this function.
     * <p>
     * 根据屏幕坐标创建拾取 {@link Ray}。假定视口覆盖整个屏幕,屏幕坐标原点在左上角,y 轴向下,x 轴向右。返回的实例不是新实例,而是仅能通过此函数访问的内部成员。
     * @return the picking Ray. 拾取射线。
     */
    public Ray getPickRay(float screenX, float screenY){
        return getPickRay(screenX, screenY, 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
    }
}
