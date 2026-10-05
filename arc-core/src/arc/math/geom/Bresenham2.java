package arc.math.geom;

import arc.struct.Ar;
import arc.func.Intc2;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;

/**
 * Returns a list of points at integer coordinates for a line on a 2D grid, using the Bresenham algorithm.
 * <p>
 * <p>
 * Instances of this class own the returned array of points and the points themselves to avoid garbage struct as much as
 * possible. Calling any of the methods will result in the reuse of the previously returned array and vectors.
 * <p>
 * 使用 Bresenham 算法返回 2D 网格上一条直线在整数坐标处的点列表。 <p> <p> 此类实例拥有返回的点数组及点本身,以尽量避免产生垃圾。调用任何方法都会复用之前返回的数组和向量。
 * @author badlogic
 */
public class Bresenham2{
    private final Ar<Point2> points = new Ar<>();
    private final Pool<Point2> pool = Pools.get(Point2.class, Point2::new);

    /**
     * Iterates through a list of {@link Point2} instances along the given line, at integer coordinates.
     * <p>
     * 沿给定直线以整数坐标遍历 {@link Point2} 实例列表。
     * @param startX the start x coordinate of the line 直线起点的 x 坐标
     * @param startY the start y coordinate of the line 直线起点的 y 坐标
     * @param endX the end x coordinate of the line 直线终点的 x 坐标
     * @param endY the end y coordinate of the line 直线终点的 y 坐标
     */
    public static void line(int startX, int startY, int endX, int endY, Intc2 consumer){
        int dx = Math.abs(endX - startX);
        int dy = Math.abs(endY - startY);

        int sx = startX < endX ? 1 : -1;
        int sy = startY < endY ? 1 : -1;

        int err = dx - dy;
        int e2;
        while(true){
            consumer.get(startX, startY);
            if(startX == endX && startY == endY) break;

            e2 = 2 * err;
            if(e2 > -dy){
                err = err - dy;
                startX = startX + sx;
            }

            if(e2 < dx){
                err = err + dx;
                startY = startY + sy;
            }
        }
    }

    /**
     * Returns a list of {@link Point2} instances along the given line, at integer coordinates.
     * <p>
     * 返回沿给定直线、以整数坐标排列的 {@link Point2} 实例列表。
     * @param start the start of the line 直线的起点
     * @param end the end of the line 直线的终点
     * @return the list of points on the line at integer coordinates 直线上整数坐标点的列表
     */
    public Ar<Point2> line(Point2 start, Point2 end){
        return line(start.x, start.y, end.x, end.y);
    }

    /**
     * Returns a list of {@link Point2} instances along the given line, at integer coordinates.
     * <p>
     * 返回沿给定直线、以整数坐标排列的 {@link Point2} 实例列表。
     * @param startX the start x coordinate of the line 直线起点的 x 坐标
     * @param startY the start y coordinate of the line 直线起点的 y 坐标
     * @param endX the end x coordinate of the line 直线终点的 x 坐标
     * @param endY the end y coordinate of the line 直线终点的 y 坐标
     * @return the list of points on the line at integer coordinates 直线上整数坐标点的列表
     */
    public Ar<Point2> line(int startX, int startY, int endX, int endY){
        pool.freeAll(points);
        points.clear();
        return line(startX, startY, endX, endY, pool, points);
    }

    /**
     * Returns a list of {@link Point2} instances along the given line, at integer coordinates.
     * <p>
     * 返回沿给定直线、以整数坐标排列的 {@link Point2} 实例列表。
     * @param startX the start x coordinate of the line 直线起点的 x 坐标
     * @param startY the start y coordinate of the line 直线起点的 y 坐标
     * @param endX the end x coordinate of the line 直线终点的 x 坐标
     * @param endY the end y coordinate of the line 直线终点的 y 坐标
     * @param pool the pool from which Point2 instances are fetched 从中获取 Point2 实例的对象池
     * @param output the output array, will be cleared in this method 输出数组,将在此方法中被清空
     * @return the list of points on the line at integer coordinates 直线上整数坐标点的列表
     */
    public Ar<Point2> line(int startX, int startY, int endX, int endY, Pool<Point2> pool, Ar<Point2> output){

        int w = endX - startX;
        int h = endY - startY;
        int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0;
        if(w < 0){
            dx1 = -1;
            dx2 = -1;
        }else if(w > 0){
            dx1 = 1;
            dx2 = 1;
        }
        if(h < 0)
            dy1 = -1;
        else if(h > 0) dy1 = 1;
        int longest = Math.abs(w);
        int shortest = Math.abs(h);
        if(longest <= shortest){
            longest = Math.abs(h);
            shortest = Math.abs(w);
            if(h < 0)
                dy2 = -1;
            else if(h > 0) dy2 = 1;
            dx2 = 0;
        }
        int numerator = longest >> 1;
        for(int i = 0; i <= longest; i++){
            Point2 point = pool.obtain();
            point.set(startX, startY);
            output.add(point);
            numerator += shortest;
            if(numerator > longest){
                numerator -= longest;
                startX += dx1;
                startY += dy1;
            }else{
                startX += dx2;
                startY += dy2;
            }
        }
        return output;
    }

    /**
     * Returns a list of {@link Point2} instances along the given line at integer coordinates, with no diagonals.
     * <p>
     * 返回沿给定直线、以整数坐标排列且无对角线的 {@link Point2} 实例列表。
     * @param startX the start x coordinate of the line 直线起点的 x 坐标
     * @param startY the start y coordinate of the line 直线起点的 y 坐标
     * @param endX the end x coordinate of the line 直线终点的 x 坐标
     * @param endY the end y coordinate of the line 直线终点的 y 坐标
     * @param pool the pool from which Point2 instances are fetched 从中获取 Point2 实例的对象池
     * @param output the output array, will be cleared in this method 输出数组,将在此方法中被清空
     * @return the list of points on the line at integer coordinates 直线上整数坐标点的列表
     */
    public Ar<Point2> lineNoDiagonal(int startX, int startY, int endX, int endY, Pool<Point2> pool, Ar<Point2> output){
        int xDist = Math.abs(endX - startX);
        int yDist = -Math.abs(endY - startY);
        int xStep = (startX < endX ? +1 : -1);
        int yStep = (startY < endY ? +1 : -1);
        int error = xDist + yDist;

        output.add(pool.obtain().set(startX, startY));

        while(startX != endX || startY != endY){

            if(2 * error - yDist > xDist - 2 * error){
                error += yDist;
                startX += xStep;
            }else{
                error += xDist;
                startY += yStep;
            }

            output.add(pool.obtain().set(startX, startY));
        }
        return output;
    }
}
