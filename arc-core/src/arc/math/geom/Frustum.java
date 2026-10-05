package arc.math.geom;

import arc.math.*;
import arc.math.geom.Plane.*;

/**
 * A truncated rectangular pyramid. Used to define the viewable region and its projection onto the screen.
 * 截头矩形锥体。用于定义可视区域及其到屏幕上的投影。
 */
public class Frustum{
    protected static final Vec3[] clipSpacePlanePoints = {
        new Vec3(-1, -1, -1), new Vec3(1, -1, -1), new Vec3(1, 1, -1), new Vec3(-1, 1, -1), // near clip
        // 近裁剪面
        new Vec3(-1, -1, 1), new Vec3(1, -1, 1), new Vec3(1, 1, 1), new Vec3(-1, 1, 1) // far clip
        // 远裁剪面
    };
    protected static final float[] clipSpacePlanePointsArray = new float[8 * 3];
    private final static Vec3 tmpV = new Vec3();

    static{
        int j = 0;
        for(Vec3 v : clipSpacePlanePoints){
            clipSpacePlanePointsArray[j++] = v.x;
            clipSpacePlanePointsArray[j++] = v.y;
            clipSpacePlanePointsArray[j++] = v.z;
        }
    }

    /**
     * the six clipping planes, near, far, left, right, top, bottom
     * 六个裁剪平面:near、far、left、right、top、bottom
     */
    public final Plane[] planes = new Plane[6];

    /**
     * eight points making up the near and far clipping "rectangles". order is counterclockwise, starting at bottom left
     * 构成近、远裁剪“矩形”的八个点。顺序为逆时针,从左下角开始
     */
    public final Vec3[] planePoints = {new Vec3(), new Vec3(), new Vec3(), new Vec3(), new Vec3(), new Vec3(), new Vec3(), new Vec3()};
    protected final float[] planePointsArray = new float[8 * 3];

    public Frustum(){
        for(int i = 0; i < 6; i++){
            planes[i] = new Plane(new Vec3(), 0);
        }
    }

    /**
     * Updates the clipping plane's based on the given inverse combined projection and view matrix.
     * <p>
     * 根据给定的投影与视图组合逆矩阵更新裁剪平面。
     * @param inverseProjectionView the combined projection and view matrices. 投影矩阵与视图矩阵的组合矩阵。
     */
    public void update(Mat3D inverseProjectionView){
        System.arraycopy(clipSpacePlanePointsArray, 0, planePointsArray, 0, clipSpacePlanePointsArray.length);
        Mat3D.prj(inverseProjectionView.val, planePointsArray, 0, 8, 3);
        for(int i = 0, j = 0; i < 8; i++){
            Vec3 v = planePoints[i];
            v.x = planePointsArray[j++];
            v.y = planePointsArray[j++];
            v.z = planePointsArray[j++];
        }

        planes[0].set(planePoints[1], planePoints[0], planePoints[2]);
        planes[1].set(planePoints[4], planePoints[5], planePoints[7]);
        planes[2].set(planePoints[0], planePoints[4], planePoints[3]);
        planes[3].set(planePoints[5], planePoints[1], planePoints[6]);
        planes[4].set(planePoints[2], planePoints[3], planePoints[6]);
        planes[5].set(planePoints[4], planePoints[0], planePoints[1]);
    }

    /**
     * @return whether the point is in the frustum.
     * 点是否在视锥体内。
     */
    public boolean containsPoint(Vec3 point){
        for(Plane plane : planes){
            if(plane.testPoint(point) == PlaneSide.back) return false;
        }
        return true;
    }

    /**
     * @param x The X coordinate of the point 点的 X 坐标
     * @param y The Y coordinate of the point 点的 Y 坐标
     * @param z The Z coordinate of the point 点的 Z 坐标
     * @return whether the point is in the frustum. 点是否在视锥体内。
     */
    public boolean containsPoint(float x, float y, float z){
        for(Plane plane : planes){
            if(plane.testPoint(x, y, z) == PlaneSide.back) return false;
        }
        return true;
    }

    /**
     * @param center The center of the sphere 球心
     * @param radius The radius of the sphere 球的半径
     * @return whether the sphere is in the frustum 球是否在视锥体内
     */
    public boolean containsSphere(Vec3 center, float radius){
        for(int i = 0; i < 6; i++)
            if((planes[i].normal.x * center.x + planes[i].normal.y * center.y + planes[i].normal.z * center.z) < (-radius - planes[i].d))
                return false;
        return true;
    }

    /**
     * Returns whether the given sphere is in the frustum.
     * <p>
     * 返回给定球是否在视锥体内。
     * @param x The X coordinate of the center of the sphere 球心的 X 坐标
     * @param y The Y coordinate of the center of the sphere 球心的 Y 坐标
     * @param z The Z coordinate of the center of the sphere 球心的 Z 坐标
     * @param radius The radius of the sphere 球的半径
     * @return whether the sphere is in the frustum 球是否在视锥体内
     */
    public boolean containsSphere(float x, float y, float z, float radius){
        for(int i = 0; i < 6; i++)
            if((planes[i].normal.x * x + planes[i].normal.y * y + planes[i].normal.z * z) < (-radius - planes[i].d)) return false;
        return true;
    }

    /**
     * @param center The center of the sphere 球心
     * @param radius The radius of the sphere 球的半径
     * @return whether the sphere is in the frustum,  not checking whether it is behind the near and far clipping plane. 球是否在视锥体内,不检查其是否位于近、远裁剪平面之后。
     */
    public boolean containsSphereWithoutNearFar(Vec3 center, float radius){
        for(int i = 2; i < 6; i++)
            if((planes[i].normal.x * center.x + planes[i].normal.y * center.y + planes[i].normal.z * center.z) < (-radius - planes[i].d))
                return false;
        return true;
    }

    /**
     * @param x The X coordinate of the center of the sphere 球心的 X 坐标
     * @param y The Y coordinate of the center of the sphere 球心的 Y 坐标
     * @param z The Z coordinate of the center of the sphere 球心的 Z 坐标
     * @param radius The radius of the sphere 球的半径
     * @return Whether the sphere is in the frustum,  not checking whether it is behind the near and far clipping plane. 球是否在视锥体内,不检查其是否位于近、远裁剪平面之后。
     */
    public boolean containsSphereWithoutNearFar(float x, float y, float z, float radius){
        for(int i = 2; i < 6; i++)
            if((planes[i].normal.x * x + planes[i].normal.y * y + planes[i].normal.z * z) < (-radius - planes[i].d)) return false;
        return true;
    }

    /**
     * @return Whether the bounding box is in the frustum
     * 包围盒是否在视锥体内
     */
    public boolean containsBounds(BoundingBox bounds){
        for(Plane plane : planes){
            if(plane.testPoint(bounds.getCorner000(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner001(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner010(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner011(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner100(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner101(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner110(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner111(tmpV)) != PlaneSide.back) continue;
            return false;
        }

        return true;
    }

    /**
     * @return Whether the bounding box is in the frustum
     * 包围盒是否在视锥体内
     */
    public boolean containsBounds(Vec3 center, Vec3 dimensions){
        return containsBounds(center.x, center.y, center.z, dimensions.x / 2, dimensions.y / 2, dimensions.z / 2);
    }

    /**
     * @return Whether the bounding box is in the frustum
     * 包围盒是否在视锥体内
     */
    public boolean containsBounds(float x, float y, float z, float halfWidth, float halfHeight, float halfDepth){
        for(Plane plane : planes){
            if(plane.testPoint(x + halfWidth, y + halfHeight, z + halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x + halfWidth, y + halfHeight, z - halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x + halfWidth, y - halfHeight, z + halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x + halfWidth, y - halfHeight, z - halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x - halfWidth, y + halfHeight, z + halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x - halfWidth, y + halfHeight, z - halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x - halfWidth, y - halfHeight, z + halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x - halfWidth, y - halfHeight, z - halfDepth) != PlaneSide.back) continue;
            return false;
        }

        return true;
    }
}