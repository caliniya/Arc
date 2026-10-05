package arc.math.geom;

import arc.struct.FloatAr;
import arc.struct.IntAr;
import arc.struct.ShortAr;

/**
 * Computes the convex hull of a set of points using the monotone chain convex hull algorithm (aka Andrew's algorithm).
 * <p>
 * 使用单调链(monotone chain)凸包算法(又称 Andrew 算法)计算点集的凸包。
 * @author Nathan Sweet
 */
public class ConvexHull{
    private final IntAr quicksortStack = new IntAr();
    private final FloatAr hull = new FloatAr();
    private final IntAr indices = new IntAr();
    private final ShortAr originalIndices = new ShortAr(false, 0);
    private float[] sortedPoints;

    /** @see #computePolygon(float[], int, int, boolean) */
    public FloatAr computePolygon(FloatAr points, boolean sorted){
        return computePolygon(points.items, 0, points.size, sorted);
    }

    /** @see #computePolygon(float[], int, int, boolean) */
    public FloatAr computePolygon(float[] polygon, boolean sorted){
        return computePolygon(polygon, 0, polygon.length, sorted);
    }

    /** Returns a list of points on the convex hull in counter-clockwise order. Note: the last point in the returned list is the
     * <p>
     * 以逆时针顺序返回凸包上的点列表。注意:返回列表中最后一个点与第一个点相同。
     * same as the first one. */
    /**
     * Returns the convex hull polygon for the given point cloud.
     * <p>
     * 返回给定点云的凸包多边形。
     * @param points x,y pairs describing points. Duplicate points will result in undefined behavior. 以 x,y 对描述的点。重复的点会导致未定义行为。
     * @param sorted If false, the points will be sorted by the x coordinate then the y coordinate, which is required by the convex 若为 false,点将先按 x 坐标再按 y 坐标排序,这是凸包算法所要求的
     * hull algorithm. If sorting is done the input array is not modified and count additional working memory is needed.
     * @return pairs of coordinates that describe the convex hull polygon in counterclockwise order. Note the returned array is 以逆时针顺序描述凸包多边形的坐标对。注意返回的数组会被复用
     * reused for later calls to the same method.
     */
    public FloatAr computePolygon(float[] points, int offset, int count, boolean sorted){
        int end = offset + count;

        if(!sorted){
            if(sortedPoints == null || sortedPoints.length < count) sortedPoints = new float[count];
            System.arraycopy(points, offset, sortedPoints, 0, count);
            points = sortedPoints;
            offset = 0;
            sort(points, count);
        }

        FloatAr hull = this.hull;
        hull.clear();

        // Lower hull.
        // 下凸包。
        for(int i = offset; i < end; i += 2){
            float x = points[i];
            float y = points[i + 1];
            while(hull.size >= 4 && ccw(x, y) <= 0)
                hull.size -= 2;
            hull.add(x);
            hull.add(y);
        }

        // Upper hull.
        // 上凸包。
        for(int i = end - 4, t = hull.size + 2; i >= offset; i -= 2){
            float x = points[i];
            float y = points[i + 1];
            while(hull.size >= t && ccw(x, y) <= 0)
                hull.size -= 2;
            hull.add(x);
            hull.add(y);
        }

        return hull;
    }

    /** @see #computeIndices(float[], int, int, boolean, boolean) */
    public IntAr computeIndices(FloatAr points, boolean sorted, boolean yDown){
        return computeIndices(points.items, 0, points.size, sorted, yDown);
    }

    /** @see #computeIndices(float[], int, int, boolean, boolean) */
    public IntAr computeIndices(float[] polygon, boolean sorted, boolean yDown){
        return computeIndices(polygon, 0, polygon.length, sorted, yDown);
    }

    /**
     * Computes a hull the same as {@link #computePolygon(float[], int, int, boolean)} but returns indices of the specified points.
     * 计算凸包的方式与 {@link #computePolygon(float[], int, int, boolean)} 相同,但返回指定点的索引。
     */
    public IntAr computeIndices(float[] points, int offset, int count, boolean sorted, boolean yDown){
        int end = offset + count;

        if(!sorted){
            if(sortedPoints == null || sortedPoints.length < count) sortedPoints = new float[count];
            System.arraycopy(points, offset, sortedPoints, 0, count);
            points = sortedPoints;
            offset = 0;
            sortWithIndices(points, count, yDown);
        }

        IntAr indices = this.indices;
        indices.clear();

        FloatAr hull = this.hull;
        hull.clear();

        // Lower hull.
        // 下凸包。
        for(int i = offset, index = i / 2; i < end; i += 2, index++){
            float x = points[i];
            float y = points[i + 1];
            while(hull.size >= 4 && ccw(x, y) <= 0){
                hull.size -= 2;
                indices.size--;
            }
            hull.add(x);
            hull.add(y);
            indices.add(index);
        }

        // Upper hull.
        // 上凸包。
        for(int i = end - 4, index = i / 2, t = hull.size + 2; i >= offset; i -= 2, index--){
            float x = points[i];
            float y = points[i + 1];
            while(hull.size >= t && ccw(x, y) <= 0){
                hull.size -= 2;
                indices.size--;
            }
            hull.add(x);
            hull.add(y);
            indices.add(index);
        }

        // Convert sorted to unsorted indices.
        // 将排序索引转换为未排序索引。
        if(!sorted){
            short[] originalIndicesArray = originalIndices.items;
            int[] indicesArray = indices.items;
            for(int i = 0, n = indices.size; i < n; i++)
                indicesArray[i] = originalIndicesArray[indicesArray[i]];
        }

        return indices;
    }

    /**
     * Returns > 0 if the points are a counterclockwise turn, < 0 if clockwise, and 0 if colinear.
     * 点为逆时针转向时返回 > 0,顺时针时返回 < 0,共线时返回 0。
     */
    private float ccw(float p3x, float p3y){
        FloatAr hull = this.hull;
        int size = hull.size;
        float p1x = hull.get(size - 4);
        float p1y = hull.get(size - 3);
        float p2x = hull.get(size - 2);
        float p2y = hull.peek();
        return (p2x - p1x) * (p3y - p1y) - (p2y - p1y) * (p3x - p1x);
    }

    /**
     * Sorts x,y pairs of values by the x value, then the y value.
     * <p>
     * 先按 x 值再按 y 值对 x,y 点对排序。
     * @param count Number of indices, must be even. 索引数量,必须为偶数。
     */
    private void sort(float[] values, int count){
        int lower = 0;
        int upper = count - 1;
        IntAr stack = quicksortStack;
        stack.add(lower);
        stack.add(upper - 1);
        while(stack.size > 0){
            upper = stack.pop();
            lower = stack.pop();
            if(upper <= lower) continue;
            int i = quicksortPartition(values, lower, upper);
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

    private int quicksortPartition(final float[] values, int lower, int upper){
        float x = values[lower];
        float y = values[lower + 1];
        int up = upper;
        int down = lower;
        float temp;
        short tempIndex;
        while(down < up){
            while(down < up && values[down] <= x)
                down = down + 2;
            while(values[up] > x || (values[up] == x && values[up + 1] < y))
                up = up - 2;
            if(down < up){
                temp = values[down];
                values[down] = values[up];
                values[up] = temp;

                temp = values[down + 1];
                values[down + 1] = values[up + 1];
                values[up + 1] = temp;
            }
        }
        values[lower] = values[up];
        values[up] = x;

        values[lower + 1] = values[up + 1];
        values[up + 1] = y;

        return up;
    }

    /**
     * Sorts x,y pairs of values by the x value, then the y value and stores unsorted original indices.
     * <p>
     * 先按 x 值再按 y 值对 x,y 点对排序,并保存未排序的原始索引。
     * @param count Number of indices, must be even. 索引数量,必须为偶数。
     */
    private void sortWithIndices(float[] values, int count, boolean yDown){
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
            int i = quicksortPartitionWithIndices(values, lower, upper, yDown, originalIndicesArray);
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

    private int quicksortPartitionWithIndices(final float[] values, int lower, int upper, boolean yDown, short[] originalIndices){
        float x = values[lower];
        float y = values[lower + 1];
        int up = upper;
        int down = lower;
        float temp;
        short tempIndex;
        while(down < up){
            while(down < up && values[down] <= x)
                down = down + 2;
            if(yDown){
                while(values[up] > x || (values[up] == x && values[up + 1] < y))
                    up = up - 2;
            }else{
                while(values[up] > x || (values[up] == x && values[up + 1] > y))
                    up = up - 2;
            }
            if(down < up){
                temp = values[down];
                values[down] = values[up];
                values[up] = temp;

                temp = values[down + 1];
                values[down + 1] = values[up + 1];
                values[up + 1] = temp;

                tempIndex = originalIndices[down / 2];
                originalIndices[down / 2] = originalIndices[up / 2];
                originalIndices[up / 2] = tempIndex;
            }
        }
        values[lower] = values[up];
        values[up] = x;

        values[lower + 1] = values[up + 1];
        values[up + 1] = y;

        tempIndex = originalIndices[lower / 2];
        originalIndices[lower / 2] = originalIndices[up / 2];
        originalIndices[up / 2] = tempIndex;

        return up;
    }
}
