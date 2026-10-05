package arc.math;

import arc.struct.BoolAr;
import arc.struct.FloatAr;
import arc.struct.IntAr;
import arc.struct.ShortAr;
import arc.math.geom.Geometry;
import arc.math.geom.Intersector;
import arc.math.geom.Vec2;

/**
 * Delaunay triangulation. Adapted from Paul Bourke's triangulate: http://paulbourke.net/papers/triangulate/
 * <p>
 * Delaunay 三角剖分。改编自 Paul Bourke 的 triangulate:http://paulbourke.net/papers/triangulate/
 * @author Nathan Sweet
 */
public class DelaunayTriangulator{
    private static final float EPSILON = 0.000001f;
    private static final int INSIDE = 0;
    private static final int COMPLETE = 1;
    private static final int INCOMPLETE = 2;

    private final IntAr quicksortStack = new IntAr();
    private final ShortAr triangles = new ShortAr(false, 16);
    private final ShortAr originalIndices = new ShortAr(false, 0);
    private final IntAr edges = new IntAr();
    private final BoolAr complete = new BoolAr(false, 16);
    private final float[] superTriangle = new float[6];
    private final Vec2 centroid = new Vec2();
    private float[] sortedPoints;

    /** @see #computeTriangles(float[], int, int, boolean) */
    public ShortAr computeTriangles(FloatAr points, boolean sorted){
        return computeTriangles(points.items, 0, points.size, sorted);
    }

    /** @see #computeTriangles(float[], int, int, boolean) */
    public ShortAr computeTriangles(float[] polygon, boolean sorted){
        return computeTriangles(polygon, 0, polygon.length, sorted);
    }

    /**
     * Triangulates the given point cloud to a list of triangle indices that make up the Delaunay triangulation.
     * <p>
     * 将给定点云三角剖分为构成 Delaunay 三角剖分的三角形索引列表。
     * @param points x,y pairs describing points. Duplicate points will result in undefined behavior. 以 x,y 对描述的点。重复的点会导致未定义行为。
     * @param sorted If false, the points will be sorted by the x coordinate, which is required by the triangulation algorithm. If 若为 false,点将按 x 坐标排序,这是三角剖分算法所要求的。若
     * sorting is done the input array is not modified, the returned indices are for the input array, and count*2
     * additional working memory is needed.
     * @return triples of indices into the points that describe the triangles in clockwise order. Note the returned array is reused 以顺时针顺序描述各三角形的点的索引三元组。注意返回的数组会被复用
     * for later calls to the same method.
     */
    public ShortAr computeTriangles(float[] points, int offset, int count, boolean sorted){
        ShortAr triangles = this.triangles;
        triangles.clear();
        if(count < 6) return triangles;
        triangles.ensureCapacity(count);

        if(!sorted){
            if(sortedPoints == null || sortedPoints.length < count) sortedPoints = new float[count];
            System.arraycopy(points, offset, sortedPoints, 0, count);
            points = sortedPoints;
            offset = 0;
            sort(points, count);
        }

        int end = offset + count;

        // Determine bounds for super triangle.
        // 确定超级三角形的边界。
        float xmin = points[0], ymin = points[1];
        float xmax = xmin, ymax = ymin;
        for(int i = offset + 2; i < end; i++){
            float value = points[i];
            if(value < xmin) xmin = value;
            if(value > xmax) xmax = value;
            i++;
            value = points[i];
            if(value < ymin) ymin = value;
            if(value > ymax) ymax = value;
        }
        float dx = xmax - xmin, dy = ymax - ymin;
        float dmax = (dx > dy ? dx : dy) * 20f;
        float xmid = (xmax + xmin) / 2f, ymid = (ymax + ymin) / 2f;

        // Setup the super triangle, which contains all points.
        // 设置包含所有点的超级三角形。
        float[] superTriangle = this.superTriangle;
        superTriangle[0] = xmid - dmax;
        superTriangle[1] = ymid - dmax;
        superTriangle[2] = xmid;
        superTriangle[3] = ymid + dmax;
        superTriangle[4] = xmid + dmax;
        superTriangle[5] = ymid - dmax;

        IntAr edges = this.edges;
        edges.ensureCapacity(count / 2);

        BoolAr complete = this.complete;
        complete.clear();
        complete.ensureCapacity(count);

        // Add super triangle.
        // 添加超级三角形。
        triangles.add(end);
        triangles.add(end + 2);
        triangles.add(end + 4);
        complete.add(false);

        // Include each point one at a time into the existing mesh.
        // 逐点将每个点加入已有的网格。
        for(int pointIndex = offset; pointIndex < end; pointIndex += 2){
            float x = points[pointIndex], y = points[pointIndex + 1];

            // If x,y lies inside the circumcircle of a triangle, the edges are stored and the triangle removed.
            // 如果 x,y 位于某个三角形的外接圆内,则保存其边并移除该三角形。
            short[] trianglesArray = triangles.items;
            boolean[] completeArray = complete.items;
            for(int triangleIndex = triangles.size - 1; triangleIndex >= 0; triangleIndex -= 3){
                int completeIndex = triangleIndex / 3;
                if(completeArray[completeIndex]) continue;
                int p1 = trianglesArray[triangleIndex - 2];
                int p2 = trianglesArray[triangleIndex - 1];
                int p3 = trianglesArray[triangleIndex];
                float x1, y1, x2, y2, x3, y3;
                if(p1 >= end){
                    int i = p1 - end;
                    x1 = superTriangle[i];
                    y1 = superTriangle[i + 1];
                }else{
                    x1 = points[p1];
                    y1 = points[p1 + 1];
                }
                if(p2 >= end){
                    int i = p2 - end;
                    x2 = superTriangle[i];
                    y2 = superTriangle[i + 1];
                }else{
                    x2 = points[p2];
                    y2 = points[p2 + 1];
                }
                if(p3 >= end){
                    int i = p3 - end;
                    x3 = superTriangle[i];
                    y3 = superTriangle[i + 1];
                }else{
                    x3 = points[p3];
                    y3 = points[p3 + 1];
                }
                switch(circumCircle(x, y, x1, y1, x2, y2, x3, y3)){
                    case COMPLETE:
                        completeArray[completeIndex] = true;
                        break;
                    case INSIDE:
                        edges.add(p1);
                        edges.add(p2);
                        edges.add(p2);
                        edges.add(p3);
                        edges.add(p3);
                        edges.add(p1);

                        triangles.removeIndex(triangleIndex);
                        triangles.removeIndex(triangleIndex - 1);
                        triangles.removeIndex(triangleIndex - 2);
                        complete.removeIndex(completeIndex);
                        break;
                }
            }

            int[] edgesArray = edges.items;
            for(int i = 0, n = edges.size; i < n; i += 2){
                // Skip multiple edges. If all triangles are anticlockwise then all interior edges are opposite pointing in direction.
                // 跳过多条边。若所有三角形都是逆时针的,则所有内部边的方向都相反。
                int p1 = edgesArray[i];
                if(p1 == -1) continue;
                int p2 = edgesArray[i + 1];
                boolean skip = false;
                for(int ii = i + 2; ii < n; ii += 2){
                    if(p1 == edgesArray[ii + 1] && p2 == edgesArray[ii]){
                        skip = true;
                        edgesArray[ii] = -1;
                    }
                }
                if(skip) continue;

                // Form new triangles for the current point. Edges are arranged in clockwise order.
                // 为当前点构造新三角形。边按顺时针方向排列。
                triangles.add(p1);
                triangles.add(edgesArray[i + 1]);
                triangles.add(pointIndex);
                complete.add(false);
            }
            edges.clear();
        }

        // Remove triangles with super triangle vertices.
        // 移除带有超级三角形顶点的三角形。
        short[] trianglesArray = triangles.items;
        for(int i = triangles.size - 1; i >= 0; i -= 3){
            if(trianglesArray[i] >= end || trianglesArray[i - 1] >= end || trianglesArray[i - 2] >= end){
                triangles.removeIndex(i);
                triangles.removeIndex(i - 1);
                triangles.removeIndex(i - 2);
            }
        }

        // Convert sorted to unsorted indices.
        // 将排序索引转换为未排序索引。
        if(!sorted){
            short[] originalIndicesArray = originalIndices.items;
            for(int i = 0, n = triangles.size; i < n; i++)
                trianglesArray[i] = (short)(originalIndicesArray[trianglesArray[i] / 2] * 2);
        }

        // Adjust triangles to start from zero and count by 1, not by vertex x,y coordinate pairs.
        // 调整三角形索引,使其从 0 开始按 1 递增计数,而不是按顶点 x,y 坐标对计数。
        if(offset == 0){
            for(int i = 0, n = triangles.size; i < n; i++)
                trianglesArray[i] = (short)(trianglesArray[i] / 2);
        }else{
            for(int i = 0, n = triangles.size; i < n; i++)
                trianglesArray[i] = (short)((trianglesArray[i] - offset) / 2);
        }

        return triangles;
    }

    /**
     * Returns INSIDE if point xp,yp is inside the circumcircle made up of the points x1,y1, x2,y2, x3,y3. Returns COMPLETE if xp
     * is to the right of the entire circumcircle. Otherwise returns INCOMPLETE. Note: a point on the circumcircle edge is
     * considered inside.
     * <p>
     * 若点 xp,yp 位于由 x1,y1、x2,y2、x3,y3 构成的外接圆内则返回 INSIDE。若 xp 在整个外接圆的右侧则返回 COMPLETE。否则返回 INCOMPLETE。注意:外接圆边上的点视为在圆内。
     */
    private int circumCircle(float xp, float yp, float x1, float y1, float x2, float y2, float x3, float y3){
        float xc, yc;
        float y1y2 = Math.abs(y1 - y2);
        float y2y3 = Math.abs(y2 - y3);
        if(y1y2 < EPSILON){
            if(y2y3 < EPSILON) return INCOMPLETE;
            float m2 = -(x3 - x2) / (y3 - y2);
            float mx2 = (x2 + x3) / 2f;
            float my2 = (y2 + y3) / 2f;
            xc = (x2 + x1) / 2f;
            yc = m2 * (xc - mx2) + my2;
        }else{
            float m1 = -(x2 - x1) / (y2 - y1);
            float mx1 = (x1 + x2) / 2f;
            float my1 = (y1 + y2) / 2f;
            if(y2y3 < EPSILON){
                xc = (x3 + x2) / 2f;
                yc = m1 * (xc - mx1) + my1;
            }else{
                float m2 = -(x3 - x2) / (y3 - y2);
                float mx2 = (x2 + x3) / 2f;
                float my2 = (y2 + y3) / 2f;
                xc = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2);
                yc = m1 * (xc - mx1) + my1;
            }
        }

        float dx = x2 - xc;
        float dy = y2 - yc;
        float rsqr = dx * dx + dy * dy;

        dx = xp - xc;
        dx *= dx;
        dy = yp - yc;
        if(dx + dy * dy - rsqr <= EPSILON) return INSIDE;
        return xp > xc && dx > rsqr ? COMPLETE : INCOMPLETE;
    }

    /**
     * Sorts x,y pairs of values by the x value.
     * <p>
     * 按 x 值对 x,y 点对排序。
     * @param count Number of indices, must be even. 索引数量,必须为偶数。
     */
    private void sort(float[] values, int count){
        int pointCount = count / 2;
        originalIndices.clear();
        originalIndices.ensureCapacity(pointCount);
        short[] originalIndicesArray = originalIndices.items;
        for(short i = 0; i < pointCount; i++)
            originalIndicesArray[i] = i;

        int lower = 0;
        int upper = count - 1;
        IntAr stack = quicksortStack;
        stack.add(lower);
        stack.add(upper - 1);
        while(stack.size > 0){
            upper = stack.pop();
            lower = stack.pop();
            if(upper <= lower) continue;
            int i = quicksortPartition(values, lower, upper, originalIndicesArray);
            if(i - lower > upper - i){
                stack.add(lower);
                stack.add(i - 2);
            }
            stack.add(i + 2);
            stack.add(upper);
            if(upper - i >= i - lower){
                stack.add(lower);
                stack.add(i - 2);
            }
        }
    }

    private int quicksortPartition(final float[] values, int lower, int upper, short[] originalIndices){
        float value = values[lower];
        int up = upper;
        int down = lower + 2;
        float tempValue;
        short tempIndex;
        while(down < up){
            while(down < up && values[down] <= value)
                down = down + 2;
            while(values[up] > value)
                up = up - 2;
            if(down < up){
                tempValue = values[down];
                values[down] = values[up];
                values[up] = tempValue;

                tempValue = values[down + 1];
                values[down + 1] = values[up + 1];
                values[up + 1] = tempValue;

                tempIndex = originalIndices[down / 2];
                originalIndices[down / 2] = originalIndices[up / 2];
                originalIndices[up / 2] = tempIndex;
            }
        }
        values[lower] = values[up];
        values[up] = value;

        tempValue = values[lower + 1];
        values[lower + 1] = values[up + 1];
        values[up + 1] = tempValue;

        tempIndex = originalIndices[lower / 2];
        originalIndices[lower / 2] = originalIndices[up / 2];
        originalIndices[up / 2] = tempIndex;
        return up;
    }

    /**
     * Removes all triangles with a centroid outside the specified hull, which may be concave. Note some triangulations may have
     * triangles whose centroid is inside the hull but a portion is outside.
     * <p>
     * 移除所有质心位于指定凸包(可能为凹)之外的三角形。注意某些三角剖分中可能存在质心在凸包内但部分区域在外的三角形。
     */
    public void trim(ShortAr triangles, float[] points, float[] hull, int offset, int count){
        short[] trianglesArray = triangles.items;
        for(int i = triangles.size - 1; i >= 0; i -= 3){
            int p1 = trianglesArray[i - 2] * 2;
            int p2 = trianglesArray[i - 1] * 2;
            int p3 = trianglesArray[i] * 2;
            Geometry.triangleCentroid(points[p1], points[p1 + 1], points[p2], points[p2 + 1], points[p3], points[p3 + 1],
            centroid);
            if(!Intersector.isInPolygon(hull, offset, count, centroid.x, centroid.y)){
                triangles.removeIndex(i);
                triangles.removeIndex(i - 1);
                triangles.removeIndex(i - 2);
            }
        }
    }
}
