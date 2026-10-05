package arc.math.geom;

import arc.math.*;
import arc.math.geom.Plane.*;
import arc.struct.*;

import java.util.*;

public class Intersector3D{
    private static final Vec3 v0 = new Vec3();
    private static final Vec3 v1 = new Vec3();
    private static final Vec3 v2 = new Vec3();
    private static final Plane p = new Plane(new Vec3(), 0);
    private static final Vec3 i = new Vec3();
    static Vec3 best = new Vec3();
    static Vec3 tmp = new Vec3();
    static Vec3 tmp1 = new Vec3();
    static Vec3 tmp2 = new Vec3();
    static Vec3 tmp3 = new Vec3();
    static Vec3 intersection = new Vec3();

    /**
     * Intersects a {@link Ray} and a {@link Plane}. The intersection point is stored in intersection in case an intersection is
     * present.
     * <p>
     * 求射线与 {@link Plane} 的交点。若存在交点,交点存入 intersection。
     * @param ray The ray 射线
     * @param plane The plane 平面
     * @param intersection The vector the intersection point is written to (optional) 用于写入交点的向量(可选)
     * @return Whether an intersection is present. 是否存在交点。
     */
    public static boolean intersectRayPlane(Ray ray, Plane plane, Vec3 intersection){
        float denom = ray.direction.dot(plane.normal);
        if(denom != 0){
            float t = -(ray.origin.dot(plane.normal) + plane.d) / denom;
            if(t < 0) return false;

            if(intersection != null) intersection.set(ray.origin).add(v0.set(ray.direction).scl(t));
            return true;
        }else if(plane.testPoint(ray.origin) == Plane.PlaneSide.onPlane){
            if(intersection != null) intersection.set(ray.origin);
            return true;
        }else
            return false;
    }

    /**
     * Intersects a line and a plane. The intersection is returned as the distance from the first point to the plane. In case an
     * intersection happened, the return value is in the range [0,1]. The intersection point can be recovered by point1 + t *
     * (point2 - point1) where t is the return value of this method.
     * <p>
     * 求直线与平面的交点。交点以从第一个点到平面的距离形式返回。若发生相交,返回值在 [0,1] 范围内。交点可通过 point1 + t * (point2 - point1) 求得,其中 t 为本方法的返回值。
     */
    public static float intersectLinePlane(float x, float y, float z, float x2, float y2, float z2, Plane plane,
                                           Vec3 intersection){
        Vec3 direction = tmp.set(x2, y2, z2).sub(x, y, z);
        Vec3 origin = tmp2.set(x, y, z);
        float denom = direction.dot(plane.normal);
        if(denom != 0){
            float t = -(origin.dot(plane.normal) + plane.d) / denom;
            if(intersection != null) intersection.set(origin).add(direction.scl(t));
            return t;
        }else if(plane.testPoint(origin) == Plane.PlaneSide.onPlane){
            if(intersection != null) intersection.set(origin);
            return 0;
        }

        return -1;
    }

    /**
     * Intersect a {@link Ray} and a triangle, returning the intersection point in intersection.
     * <p>
     * 求射线与三角形的交点,交点存入 intersection。
     * @param ray The ray 射线
     * @param t1 The first vertex of the triangle 三角形的第一个顶点
     * @param t2 The second vertex of the triangle 三角形的第二个顶点
     * @param t3 The third vertex of the triangle 三角形的第三个顶点
     * @param intersection The intersection point (optional) 交点(可选)
     * @return True in case an intersection is present. 若存在交点则为 true。
     */
    public static boolean intersectRayTriangle(Ray ray, Vec3 t1, Vec3 t2, Vec3 t3, Vec3 intersection){
        Vec3 edge1 = v0.set(t2).sub(t1);
        Vec3 edge2 = v1.set(t3).sub(t1);

        Vec3 pvec = v2.set(ray.direction).crs(edge2);
        float det = edge1.dot(pvec);
        if(Mathf.zero(det)){
            p.set(t1, t2, t3);
            if(p.testPoint(ray.origin) == PlaneSide.onPlane && Intersector.isInTriangle(ray.origin, t1, t2, t3)){
                if(intersection != null) intersection.set(ray.origin);
                return true;
            }
            return false;
        }

        det = 1.0f / det;

        Vec3 tvec = i.set(ray.origin).sub(t1);
        float u = tvec.dot(pvec) * det;
        if(u < 0.0f || u > 1.0f) return false;

        Vec3 qvec = tvec.crs(edge1);
        float v = ray.direction.dot(qvec) * det;
        if(v < 0.0f || u + v > 1.0f) return false;

        float t = edge2.dot(qvec) * det;
        if(t < 0) return false;

        if(intersection != null){
            if(t <= Mathf.FLOAT_ROUNDING_ERROR){
                intersection.set(ray.origin);
            }else{
                ray.getEndPoint(intersection, t);
            }
        }

        return true;
    }

    /**
     * Intersects a {@link Ray} and a sphere, returning the intersection point in intersection.
     * <p>
     * 求射线与球体的交点,交点存入 intersection。
     * @param ray The ray, the direction component must be normalized before calling this method 射线,调用此方法前其方向分量必须已归一化
     * @param center The center of the sphere 球心
     * @param radius The radius of the sphere 球的半径
     * @param intersection The intersection point (optional, can be null) 交点(可选,可为 null)
     * @return Whether an intersection is present. 是否存在交点。
     */
    public static boolean intersectRaySphere(Ray ray, Vec3 center, float radius, Vec3 intersection){
        final float len = ray.direction.dot(center.x - ray.origin.x, center.y - ray.origin.y, center.z - ray.origin.z);
        if(len < 0.f) // behind the ray
        // 射线后方
            return false;
        final float dst2 = center.dst2(ray.origin.x + ray.direction.x * len, ray.origin.y + ray.direction.y * len,
        ray.origin.z + ray.direction.z * len);
        final float r2 = radius * radius;
        if(dst2 > r2) return false;
        if(intersection != null)
            intersection.set(ray.direction).scl(len - (float)Math.sqrt(r2 - dst2)).add(ray.origin);
        return true;
    }

    /**
     * Intersects a {@link Ray} and a {@link BoundingBox}, returning the intersection point in intersection. This intersection is
     * defined as the point on the ray closest to the origin which is within the specified bounds.
     *
     * <p>
     * The returned intersection (if any) is guaranteed to be within the bounds of the bounding box, but it can occasionally
     * diverge slightly from ray, due to small floating-point errors.
     * </p>
     *
     * <p>
     * If the origin of the ray is inside the box, this method returns true and the intersection point is set to the origin of the
     * ray, accordingly to the definition above.
     * </p>
     * <p>
     * 求射线与 {@link BoundingBox} 的交点,交点存入 intersection。此交点定义为射线上位于指定边界内、距原点最近的点。 <p> 返回的交点(若有)保证在包围盒边界内,但由于微小的浮点误差,偶尔可能与射线略有偏差。 </p> <p> 若射线原点在盒内,按上述定义,此方法返回 true 且交点设为射线原点。 </p>
     * @param ray The ray 射线
     * @param box The box 盒子
     * @param intersection The intersection point (optional) 交点(可选)
     * @return Whether an intersection is present. 是否存在交点。
     */
    public static boolean intersectRayBounds(Ray ray, BoundingBox box, Vec3 intersection){
        if(box.contains(ray.origin)){
            if(intersection != null) intersection.set(ray.origin);
            return true;
        }
        float lowest = 0, t;
        boolean hit = false;

        // min x
        // x 的最小值
        if(ray.origin.x <= box.min.x && ray.direction.x > 0){
            t = (box.min.x - ray.origin.x) / ray.direction.x;
            if(t >= 0){
                v2.set(ray.direction).scl(t).add(ray.origin);
                if(v2.y >= box.min.y && v2.y <= box.max.y && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)){
                    hit = true;
                    lowest = t;
                }
            }
        }
        // max x
        // x 的最大值
        if(ray.origin.x >= box.max.x && ray.direction.x < 0){
            t = (box.max.x - ray.origin.x) / ray.direction.x;
            if(t >= 0){
                v2.set(ray.direction).scl(t).add(ray.origin);
                if(v2.y >= box.min.y && v2.y <= box.max.y && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)){
                    hit = true;
                    lowest = t;
                }
            }
        }
        // min y
        // y 的最小值
        if(ray.origin.y <= box.min.y && ray.direction.y > 0){
            t = (box.min.y - ray.origin.y) / ray.direction.y;
            if(t >= 0){
                v2.set(ray.direction).scl(t).add(ray.origin);
                if(v2.x >= box.min.x && v2.x <= box.max.x && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)){
                    hit = true;
                    lowest = t;
                }
            }
        }
        // max y
        // y 的最大值
        if(ray.origin.y >= box.max.y && ray.direction.y < 0){
            t = (box.max.y - ray.origin.y) / ray.direction.y;
            if(t >= 0){
                v2.set(ray.direction).scl(t).add(ray.origin);
                if(v2.x >= box.min.x && v2.x <= box.max.x && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)){
                    hit = true;
                    lowest = t;
                }
            }
        }
        // min z
        // z 的最小值
        if(ray.origin.z <= box.min.z && ray.direction.z > 0){
            t = (box.min.z - ray.origin.z) / ray.direction.z;
            if(t >= 0){
                v2.set(ray.direction).scl(t).add(ray.origin);
                if(v2.x >= box.min.x && v2.x <= box.max.x && v2.y >= box.min.y && v2.y <= box.max.y && (!hit || t < lowest)){
                    hit = true;
                    lowest = t;
                }
            }
        }
        // max y
        // y 的最大值
        if(ray.origin.z >= box.max.z && ray.direction.z < 0){
            t = (box.max.z - ray.origin.z) / ray.direction.z;
            if(t >= 0){
                v2.set(ray.direction).scl(t).add(ray.origin);
                if(v2.x >= box.min.x && v2.x <= box.max.x && v2.y >= box.min.y && v2.y <= box.max.y && (!hit || t < lowest)){
                    hit = true;
                    lowest = t;
                }
            }
        }
        if(hit && intersection != null){
            intersection.set(ray.direction).scl(lowest).add(ray.origin);
            if(intersection.x < box.min.x){
                intersection.x = box.min.x;
            }else if(intersection.x > box.max.x){
                intersection.x = box.max.x;
            }
            if(intersection.y < box.min.y){
                intersection.y = box.min.y;
            }else if(intersection.y > box.max.y){
                intersection.y = box.max.y;
            }
            if(intersection.z < box.min.z){
                intersection.z = box.min.z;
            }else if(intersection.z > box.max.z){
                intersection.z = box.max.z;
            }
        }
        return hit;
    }

    /**
     * Quick check whether the given {@link Ray} and {@link BoundingBox} intersect.
     * <p>
     * 快速判断给定的射线与包围盒是否相交。
     * @param ray The ray 射线
     * @param box The bounding box 包围盒
     * @return Whether the ray and the bounding box intersect. 射线与包围盒是否相交。
     */
    public static boolean intersectRayBoundsFast(Ray ray, BoundingBox box){
        return intersectRayBoundsFast(ray, box.getCenter(tmp1), box.getDimensions(tmp2));
    }

    /**
     * Quick check whether the given {@link Ray} and {@link BoundingBox} intersect.
     * <p>
     * 快速判断给定的射线与包围盒是否相交。
     * @param ray The ray 射线
     * @param center The center of the bounding box 包围盒的中心
     * @param dimensions The dimensions (width, height and depth) of the bounding box 包围盒的尺寸(宽、高和深)
     * @return Whether the ray and the bounding box intersect. 射线与包围盒是否相交。
     */
    public static boolean intersectRayBoundsFast(Ray ray, Vec3 center, Vec3 dimensions){
        final float divX = 1f / ray.direction.x;
        final float divY = 1f / ray.direction.y;
        final float divZ = 1f / ray.direction.z;

        float minx = ((center.x - dimensions.x * .5f) - ray.origin.x) * divX;
        float maxx = ((center.x + dimensions.x * .5f) - ray.origin.x) * divX;
        if(minx > maxx){
            final float t = minx;
            minx = maxx;
            maxx = t;
        }

        float miny = ((center.y - dimensions.y * .5f) - ray.origin.y) * divY;
        float maxy = ((center.y + dimensions.y * .5f) - ray.origin.y) * divY;
        if(miny > maxy){
            final float t = miny;
            miny = maxy;
            maxy = t;
        }

        float minz = ((center.z - dimensions.z * .5f) - ray.origin.z) * divZ;
        float maxz = ((center.z + dimensions.z * .5f) - ray.origin.z) * divZ;
        if(minz > maxz){
            final float t = minz;
            minz = maxz;
            maxz = t;
        }

        float min = Math.max(Math.max(minx, miny), minz);
        float max = Math.min(Math.min(maxx, maxy), maxz);

        return max >= 0 && max >= min;
    }

    public static boolean intersectSegmentPlane(Vec3 start, Vec3 end, Plane plane, Vec3 intersection){
        Vec3 dir = v0.set(end).sub(start);
        float denom = dir.dot(plane.normal);
        if(denom == 0f) return false;
        float t = -(start.dot(plane.normal) + plane.d) / denom;
        if(t < 0 || t > 1) return false;

        intersection.set(start).add(dir.scl(t));
        return true;
    }

    /**
     * Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
     * <p>
     * 用给定的射线与三角形列表求交。最近的交点存入 intersection
     * @param ray The ray 射线
     * @param triangles The triangles, each successive 3 elements from a vertex 三角形数组,每连续 3 个元素对应一个顶点
     * @param intersection The nearest intersection point (optional) 最近的交点(可选)
     * @return Whether the ray and the triangles intersect. 射线与三角形是否相交。
     */
    public static boolean intersectRayTriangles(Ray ray, float[] triangles, Vec3 intersection){
        float min_dist = Float.MAX_VALUE;
        boolean hit = false;

        if(triangles.length / 3 % 3 != 0) throw new RuntimeException("triangle list size is not a multiple of 3");

        for(int i = 0; i < triangles.length - 6; i += 9){
            boolean result = intersectRayTriangle(ray, tmp1.set(triangles[i], triangles[i + 1], triangles[i + 2]),
            tmp2.set(triangles[i + 3], triangles[i + 4], triangles[i + 5]),
            tmp3.set(triangles[i + 6], triangles[i + 7], triangles[i + 8]), tmp);

            if(result){
                float dist = ray.origin.dst2(tmp);
                if(dist < min_dist){
                    min_dist = dist;
                    best.set(tmp);
                    hit = true;
                }
            }
        }

        if(!hit)
            return false;
        else{
            if(intersection != null) intersection.set(best);
            return true;
        }
    }

    /**
     * Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
     * <p>
     * 用给定的射线与三角形列表求交。最近的交点存入 intersection
     * @param ray The ray 射线
     * @param vertices the vertices 顶点
     * @param indices the indices, each successive 3 shorts index the 3 vertices of a triangle 索引数组,每连续 3 个 short 索引一个三角形的 3 个顶点
     * @param vertexSize the size of a vertex in floats 顶点的大小(以浮点数计)
     * @param intersection The nearest intersection point (optional) 最近的交点(可选)
     * @return Whether the ray and the triangles intersect. 射线与三角形是否相交。
     */
    public static boolean intersectRayTriangles(Ray ray, float[] vertices, short[] indices, int vertexSize,
                                                Vec3 intersection){
        float min_dist = Float.MAX_VALUE;
        boolean hit = false;

        if(indices.length % 3 != 0) throw new RuntimeException("triangle list size is not a multiple of 3");

        for(int i = 0; i < indices.length; i += 3){
            int i1 = indices[i] * vertexSize;
            int i2 = indices[i + 1] * vertexSize;
            int i3 = indices[i + 2] * vertexSize;

            boolean result = intersectRayTriangle(ray, tmp1.set(vertices[i1], vertices[i1 + 1], vertices[i1 + 2]),
            tmp2.set(vertices[i2], vertices[i2 + 1], vertices[i2 + 2]),
            tmp3.set(vertices[i3], vertices[i3 + 1], vertices[i3 + 2]), tmp);

            if(result){
                float dist = ray.origin.dst2(tmp);
                if(dist < min_dist){
                    min_dist = dist;
                    best.set(tmp);
                    hit = true;
                }
            }
        }

        if(!hit)
            return false;
        else{
            if(intersection != null) intersection.set(best);
            return true;
        }
    }

    /**
     * Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
     * <p>
     * 用给定的射线与三角形列表求交。最近的交点存入 intersection
     * @param ray The ray 射线
     * @param triangles The triangles 三角形
     * @param intersection The nearest intersection point (optional) 最近的交点(可选)
     * @return Whether the ray and the triangles intersect. 射线与三角形是否相交。
     */
    public static boolean intersectRayTriangles(Ray ray, Ar<Vec3> triangles, Vec3 intersection){
        float min_dist = Float.MAX_VALUE;
        boolean hit = false;

        if(triangles.size % 3 != 0) throw new RuntimeException("triangle list size is not a multiple of 3");

        for(int i = 0; i < triangles.size - 2; i += 3){
            boolean result = intersectRayTriangle(ray, triangles.get(i), triangles.get(i + 1), triangles.get(i + 2), tmp);

            if(result){
                float dist = ray.origin.dst2(tmp);
                if(dist < min_dist){
                    min_dist = dist;
                    best.set(tmp);
                    hit = true;
                }
            }
        }

        if(!hit)
            return false;
        else{
            if(intersection != null) intersection.set(best);
            return true;
        }
    }


    /**
     * Splits the triangle by the plane. The result is stored in the SplitTriangle instance. Depending on where the triangle is
     * relative to the plane, the result can be:
     *
     * <ul>
     * <li>Triangle is fully in front/behind: {@link SplitTriangle#front} or {@link SplitTriangle#back} will contain the original
     * triangle, {@link SplitTriangle#total} will be one.</li>
     * <li>Triangle has two vertices in front, one behind: {@link SplitTriangle#front} contains 2 triangles,
     * {@link SplitTriangle#back} contains 1 triangles, {@link SplitTriangle#total} will be 3.</li>
     * <li>Triangle has one vertex in front, two behind: {@link SplitTriangle#front} contains 1 triangle,
     * {@link SplitTriangle#back} contains 2 triangles, {@link SplitTriangle#total} will be 3.</li>
     * </ul>
     * <p>
     * The input triangle should have the form: x, y, z, x2, y2, z2, x3, y3, z3. One can add additional attributes per vertex which
     * will be interpolated if split, such as texture coordinates or normals. Note that these additional attributes won't be
     * normalized, as might be necessary in case of normals.
     * <p>
     * 按平面分割三角形。结果存入 SplitTriangle 实例。根据三角形相对平面的位置,结果可能是: <ul> <li>三角形完全在前方/后方:{@link SplitTriangle#front} 或 {@link SplitTriangle#back} 包含原三角形,{@link SplitTriangle#total} 为 1。</li> <li>两个顶点在前方,一个在后方:{@link SplitTriangle#front} 包含 2 个三角形,{@link SplitTriangle#back} 包含 1 个,{@link SplitTriangle#total} 为 3。</li> <li>一个顶点在前方,两个在后方:{@link SplitTriangle#front} 包含 1 个三角形,{@link SplitTriangle#back} 包含 2 个,{@link SplitTriangle#total} 为 3。</li> </ul> <p> 输入三角形应具有如下形式:x, y, z, x2, y2, z2, x3, y3, z3。可以为每个顶点添加额外属性,分割时会被插值,例如纹理坐标或法线。注意这些额外属性不会被归一化,法线等情况可能需要归一化。
     * @param split output SplitTriangle 输出的 SplitTriangle
     */
    public static void splitTriangle(float[] triangle, Plane plane, SplitTriangle split){
        int stride = triangle.length / 3;
        boolean r1 = plane.testPoint(triangle[0], triangle[1], triangle[2]) == PlaneSide.back;
        boolean r2 = plane.testPoint(triangle[stride], triangle[1 + stride], triangle[2 + stride]) == PlaneSide.back;
        boolean r3 = plane.testPoint(triangle[stride * 2], triangle[1 + stride * 2],
        triangle[2 + stride * 2]) == PlaneSide.back;

        split.reset();

        // easy case, triangle is on one side (point on plane means front).
        // 简单情况,三角形位于一侧(点在平面上视为在前方)。
        if(r1 == r2 && r2 == r3){
            split.total = 1;
            if(r1){
                split.numBack = 1;
                System.arraycopy(triangle, 0, split.back, 0, triangle.length);
            }else{
                split.numFront = 1;
                System.arraycopy(triangle, 0, split.front, 0, triangle.length);
            }
            return;
        }

        // set number of triangles
        // 设置三角形数量
        split.total = 3;
        split.numFront = (r1 ? 0 : 1) + (r2 ? 0 : 1) + (r3 ? 0 : 1);
        split.numBack = split.total - split.numFront;

        // hard case, split the three edges on the plane
        // 困难情况,将三条边在平面上分割
        // determine which array to fill first, front or back, flip if we
        // 决定先填充哪个数组(front 还是 back),必要时翻转
        // cross the plane
        // 穿越平面
        split.setSide(!r1);

        // split first edge
        // 分割第一条边
        int first = 0;
        int second = stride;
        if(r1 != r2){
            // split the edge
            // 分割该边
            splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0);

            // add first edge vertex and new vertex to current side
            // 将第一条边的顶点和新顶点加入当前侧
            split.add(triangle, first, stride);
            split.add(split.edgeSplit, 0, stride);

            // flip side and add new vertex and second edge vertex to current side
            // 翻转当前侧,并将新顶点和第二条边的顶点加入该侧
            split.setSide(!split.getSide());
            split.add(split.edgeSplit, 0, stride);
        }else{
            // add both vertices
            // 添加两个顶点
            split.add(triangle, first, stride);
        }

        // split second edge
        // 分割第二条边
        first = stride;
        second = stride + stride;
        if(r2 != r3){
            // split the edge
            // 分割该边
            splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0);

            // add first edge vertex and new vertex to current side
            // 将第一条边的顶点和新顶点加入当前侧
            split.add(triangle, first, stride);
            split.add(split.edgeSplit, 0, stride);

            // flip side and add new vertex and second edge vertex to current side
            // 翻转当前侧,并将新顶点和第二条边的顶点加入该侧
            split.setSide(!split.getSide());
            split.add(split.edgeSplit, 0, stride);
        }else{
            // add both vertices
            // 添加两个顶点
            split.add(triangle, first, stride);
        }

        // split third edge
        // 分割第三条边
        first = stride + stride;
        second = 0;
        if(r3 != r1){
            // split the edge
            // 分割该边
            splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0);

            // add first edge vertex and new vertex to current side
            // 将第一条边的顶点和新顶点加入当前侧
            split.add(triangle, first, stride);
            split.add(split.edgeSplit, 0, stride);

            // flip side and add new vertex and second edge vertex to current side
            // 翻转当前侧,并将新顶点和第二条边的顶点加入该侧
            split.setSide(!split.getSide());
            split.add(split.edgeSplit, 0, stride);
        }else{
            // add both vertices
            // 添加两个顶点
            split.add(triangle, first, stride);
        }

        // triangulate the side with 2 triangles
        // 用 2 个三角形对该侧进行三角剖分
        if(split.numFront == 2){
            System.arraycopy(split.front, stride * 2, split.front, stride * 3, stride * 2);
            System.arraycopy(split.front, 0, split.front, stride * 5, stride);
        }else{
            System.arraycopy(split.back, stride * 2, split.back, stride * 3, stride * 2);
            System.arraycopy(split.back, 0, split.back, stride * 5, stride);
        }
    }

    private static void splitEdge(float[] vertices, int s, int e, int stride, Plane plane, float[] split, int offset){
        float t = intersectLinePlane(vertices[s], vertices[s + 1], vertices[s + 2], vertices[e], vertices[e + 1],
        vertices[e + 2], plane, intersection);
        split[offset] = intersection.x;
        split[offset + 1] = intersection.y;
        split[offset + 2] = intersection.z;
        for(int i = 3; i < stride; i++){
            float a = vertices[s + i];
            float b = vertices[e + i];
            split[offset + i] = a + t * (b - a);
        }
    }


    public static class SplitTriangle{
        public float[] front;
        public float[] back;
        public int numFront;
        public int numBack;
        public int total;
        float[] edgeSplit;
        boolean frontCurrent = false;
        int frontOffset = 0;
        int backOffset = 0;

        /**
         * Creates a new instance, assuming numAttributes attributes per triangle vertex.
         * <p>
         * 创建新实例,假定每个三角形顶点有 numAttributes 个属性。
         * @param numAttributes must be >= 3 必须 >= 3
         */
        public SplitTriangle(int numAttributes){
            front = new float[numAttributes * 3 * 2];
            back = new float[numAttributes * 3 * 2];
            edgeSplit = new float[numAttributes];
        }

        @Override
        public String toString(){
            return "SplitTriangle [front=" + Arrays.toString(front) + ", back=" + Arrays.toString(back) + ", numFront=" + numFront
            + ", numBack=" + numBack + ", total=" + total + "]";
        }

        boolean getSide(){
            return frontCurrent;
        }

        void setSide(boolean front){
            frontCurrent = front;
        }

        void add(float[] vertex, int offset, int stride){
            if(frontCurrent){
                System.arraycopy(vertex, offset, front, frontOffset, stride);
                frontOffset += stride;
            }else{
                System.arraycopy(vertex, offset, back, backOffset, stride);
                backOffset += stride;
            }
        }

        void reset(){
            frontCurrent = false;
            frontOffset = 0;
            backOffset = 0;
            numFront = 0;
            numBack = 0;
            total = 0;
        }
    }
}