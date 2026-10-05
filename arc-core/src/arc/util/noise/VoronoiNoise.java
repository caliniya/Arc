package arc.util.noise;

import arc.math.Rand;

import java.util.Random;

public class VoronoiNoise{

    /// Noise module that outputs Voronoi cells.
    // / 输出 Voronoi 单元的噪声模块。
    ///
    /// In mathematics, a <i>Voronoi cell</i> is a region containing all the
    // / 在数学中,<i>Voronoi 单元</i>是这样一片区域:其中的所有点
    /// points that are closer to a specific <i>seed point</i> than to any
    // / 到某个特定 <i>种子点</i>的距离
    /// other seed point.  These cells mesh with one another, producing
    // / 都比到其他种子点更近。这些单元相互拼接,形成
    /// polygon-like formations.
    // / 类似多边形的形状。
    ///
    /// By default, this noise module randomly places a seed point within
    // / 默认情况下,该噪声模块会在每个
    /// each unit cube.  By modifying the <i>frequency</i> of the seed points,
    // / 单位立方体中随机放置一个种子点。通过修改种子点的 <i>frequency(频率)</i>,
    /// an application can change the distance between seed points.  The
    // / 应用程序即可改变种子点之间的距离。
    /// higher the frequency, the closer together this noise module places
    // / 频率越高,该噪声模块放置的种子点就越密集,
    /// the seed points, which reduces the size of the cells.  To specify the
    // / 从而缩小单元的尺寸。要指定各单元的
    /// frequency of the cells, call the setFrequency() method.
    // / 频率,请调用 setFrequency() 方法。
    ///
    /// This noise module assigns each Voronoi cell with a random constant
    // / 该噪声模块会为每个 Voronoi 单元分配一个随机的常量
    /// value from a coherent-noise function.  The <i>displacement value</i>
    // / 值,该值来自相干噪声函数。<i>位移值</i>
    /// controls the range of random values to assign to each cell.  The
    // / 控制着分配给每个单元的随机值的范围。
    /// range of random values is +/- the displacement value.  Call the
    // / 随机值的范围为 +/- 位移值。调用
    /// setDisplacement() method to specify the displacement value.
    // / setDisplacement() 方法来指定位移值。
    ///
    /// To modify the random positions of the seed points, call the SetSeed()
    // / 要修改种子点的随机位置,请调用 SetSeed()
    /// method.
    // / 方法。
    ///
    /// This noise module can optionally add the distance from the nearest
    // / 该噪声模块可以选择性地把到最近
    /// seed to the output value.  To enable this feature, call the
    // / 种子的距离加到输出值上。要启用该特性,请调用
    /// enableDistance() method.  This causes the points in the Voronoi cells
    // / enableDistance() 方法。这会使得 Voronoi 单元中的点
    /// to increase in value the further away that point is from the nearest
    // / 值随该点离最近
    /// seed point.
    // / 种子点越远而增大。

    //for speed, we can approximate the sqrt term in the distance funtions
    // 为了速度,可以对距离函数中的 sqrt 项取近似
    private static final double SQRT_2 = 1.4142135623730950488;
    private static final double SQRT_3 = 1.7320508075688772935;

    //You can either use the feature point height (for biomes or solid pillars), or the distance to the feature point
    // 你可以使用特征点高度(用于生物群系或实心立柱),也可以使用到特征点的距离
    private boolean useDistance = false;

    private long seed;
    private boolean useManhattan;
    private Rand rnd = new Rand();

    public VoronoiNoise(long seed, boolean useManhattan){
        this.seed = seed;
        this.useManhattan = useManhattan;
    }

    /**
     * To avoid having to store the feature points, we use a hash function
     * of the coordinates and the seed instead. Those big scary numbers are
     * arbitrary primes.
     * <p>
     * 为了避免存储特征点,我们改用坐标与种子的哈希函数代替。那些吓人的大数字是随意选取的质数。
     */
    public static double valueNoise2D(int x, int z, long seed){
        long n = (1619 * x + 6971 * z + 1013 * seed) & 0x7fffffff;
        n = (n >> 13) ^ n;
        return 1.0 - ((double)((n * (n * n * 60493 + 19990303) + 1376312589) & 0x7fffffff) / 1073741824.0);
    }

    public static double valueNoise3D(int x, int y, int z, long seed){
        long n = (1619 * x + 31337 * y + 6971 * z + 1013 * seed) & 0x7fffffff;
        n = (n >> 13) ^ n;
        return 1.0 - ((double)((n * (n * n * 60493 + 19990303) + 1376312589) & 0x7fffffff) / 1073741824.0);
    }

    private double getDistance(double xDist, double zDist){
        return useManhattan ? xDist + zDist : Math.sqrt(xDist * xDist + zDist * zDist) / SQRT_2;
    }

    private double getDistance(double xDist, double yDist, double zDist){
        return useManhattan ? xDist + yDist + zDist : Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) / SQRT_3;
    }

    public boolean isUseDistance(){
        return useDistance;
    }

    public void setUseDistance(boolean useDistance){
        this.useDistance = useDistance;
    }

    public long getSeed(){
        return seed;
    }

    public void setSeed(long seed){
        this.seed = seed;
    }

    public double noise(double x, double z, double frequency){
        x *= frequency;
        z *= frequency;
        rnd.setSeed(seed);
        long result = rnd.nextLong();

        int xInt = (x > .0 ? (int)x : (int)x - 1);
        int zInt = (z > .0 ? (int)z : (int)z - 1);

        double minDist = 32000000.0;

        double xCandidate = 0;
        double zCandidate = 0;

        for(int zCur = zInt - 2; zCur <= zInt + 2; zCur++){
            for(int xCur = xInt - 2; xCur <= xInt + 2; xCur++){

                double xPos = xCur + valueNoise2D(xCur, zCur, seed);
                double zPos = zCur + valueNoise2D(xCur, zCur, result);
                double xDist = xPos - x;
                double zDist = zPos - z;
                double dist = xDist * xDist + zDist * zDist;

                if(dist < minDist){
                    minDist = dist;
                    xCandidate = xPos;
                    zCandidate = zPos;
                }
            }
        }

        if(useDistance){
            double xDist = xCandidate - x;
            double zDist = zCandidate - z;
            return getDistance(xDist, zDist);
        }else return (VoronoiNoise.valueNoise2D((int)(Math.floor(xCandidate)), (int)(Math.floor(zCandidate)), seed));
    }

    public double noise(double x, double y, double z, double frequency){
        // Inside each unit cube, there is a seed point at a random position.  Go
        // 在每个单位立方体内,都有一个位于随机位置的种子点。逐个检查
        // through each of the nearby cubes until we find a cube with a seed point
        // 附近的立方体,直到找到某个立方体,其种子点
        // that is closest to the specified position.
        // 距指定位置最近。
        x *= frequency;
        y *= frequency;
        z *= frequency;

        int xInt = (x > .0 ? (int)x : (int)x - 1);
        int yInt = (y > .0 ? (int)y : (int)y - 1);
        int zInt = (z > .0 ? (int)z : (int)z - 1);

        double minDist = 32000000.0;

        double xCandidate = 0;
        double yCandidate = 0;
        double zCandidate = 0;

        Random rand = new Random(seed);

        for(int zCur = zInt - 2; zCur <= zInt + 2; zCur++){
            for(int yCur = yInt - 2; yCur <= yInt + 2; yCur++){
                for(int xCur = xInt - 2; xCur <= xInt + 2; xCur++){
                    // Calculate the position and distance to the seed point inside of
                    // 计算位于该单位立方体内的
                    // this unit cube.
                    // 种子点的位置及与其的距离。

                    double xPos = xCur + valueNoise3D(xCur, yCur, zCur, seed);
                    double yPos = yCur + valueNoise3D(xCur, yCur, zCur, rand.nextLong());
                    double zPos = zCur + valueNoise3D(xCur, yCur, zCur, rand.nextLong());
                    double xDist = xPos - x;
                    double yDist = yPos - y;
                    double zDist = zPos - z;
                    double dist = xDist * xDist + yDist * yDist + zDist * zDist;

                    if(dist < minDist){
                        // This seed point is closer to any others found so far, so record
                        // 该种子点比迄今为止找到的任何其他种子点都更近,因此
                        // this seed point.
                        // 记录该种子点。
                        minDist = dist;
                        xCandidate = xPos;
                        yCandidate = yPos;
                        zCandidate = zPos;
                    }
                }
            }
        }

        if(useDistance){
            double xDist = xCandidate - x;
            double yDist = yCandidate - y;
            double zDist = zCandidate - z;

            return getDistance(xDist, yDist, zDist);
        }else return valueNoise3D(
        (int)(Math.floor(xCandidate)),
        (int)(Math.floor(yCandidate)),
        (int)(Math.floor(zCandidate)), seed);
    }
}
