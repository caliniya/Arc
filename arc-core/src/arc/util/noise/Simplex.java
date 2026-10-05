package arc.util.noise;

import static arc.math.Mathf.*;

//TODO this class a disaster:
// TODO 这个类简直一团糟:
//- nobody knows what range functions return
// - 没有人知道各函数的返回值范围
//- generally bizarre outputs
// - 输出通常很怪异
//- bad parameter inputs, have to use 1/scale
// - 参数输入不友好,必须使用 1/scale
//- may need a replacement with a completely different class
// - 可能需要用完全不同的类来替代
//- should use float instead of double, nobody needs this sort of precision
// - 应使用 float 而不是 double,没人需要这种精度
public class Simplex{
    static final int[][] grad3 = {
    {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
    {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
    {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}
    };

    static final int[][] grad4 = {
    {0, 1, 1, 1}, {0, 1, 1, -1}, {0, 1, -1, 1}, {0, 1, -1, -1},
    {0, -1, 1, 1}, {0, -1, 1, -1}, {0, -1, -1, 1}, {0, -1, -1, -1},
    {1, 0, 1, 1}, {1, 0, 1, -1}, {1, 0, -1, 1}, {1, 0, -1, -1},
    {-1, 0, 1, 1}, {-1, 0, 1, -1}, {-1, 0, -1, 1}, {-1, 0, -1, -1},
    {1, 1, 0, 1}, {1, 1, 0, -1}, {1, -1, 0, 1}, {1, -1, 0, -1},
    {-1, 1, 0, 1}, {-1, 1, 0, -1}, {-1, -1, 0, 1}, {-1, -1, 0, -1},
    {1, 1, 1, 0}, {1, 1, -1, 0}, {1, -1, 1, 0}, {1, -1, -1, 0},
    {-1, 1, 1, 0}, {-1, 1, -1, 0}, {-1, -1, 1, 0}, {-1, -1, -1, 0}
    };
    static final int[][] simplex = {
    {0, 1, 2, 3}, {0, 1, 3, 2}, {0, 0, 0, 0}, {0, 2, 3, 1}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {1, 2, 3, 0},
    {0, 2, 1, 3}, {0, 0, 0, 0}, {0, 3, 1, 2}, {0, 3, 2, 1}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {1, 3, 2, 0},
    {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
    {1, 2, 0, 3}, {0, 0, 0, 0}, {1, 3, 0, 2}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {2, 3, 0, 1}, {2, 3, 1, 0},
    {1, 0, 2, 3}, {1, 0, 3, 2}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {2, 0, 3, 1}, {0, 0, 0, 0}, {2, 1, 3, 0},
    {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
    {2, 0, 1, 3}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {3, 0, 1, 2}, {3, 0, 2, 1}, {0, 0, 0, 0}, {3, 1, 2, 0},
    {2, 1, 0, 3}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {3, 1, 0, 2}, {0, 0, 0, 0}, {3, 2, 0, 1}, {3, 2, 1, 0}
    };

    //static only
    // 仅包含静态成员
    private Simplex(){}

    // 2D Multi-octave Simplex noise.
    // 2D 多倍频 Simplex 噪声。
    //
    // For each octave, a higher frequency/lower amplitude function will be added to the original.
    // 每个倍频都会向原始结果叠加一个频率更高/振幅更低的函数。
    // The higher the persistence [0-1], the more of each succeeding octave will be added.
    // 持续度(persistence)[0-1] 越高,后续每个倍频叠加的部分就越多。
    public static float noise2d(int seed, double octaves, double persistence, double scale, double x, double y){
        double total = 0;
        double frequency = scale;
        double amplitude = 1;

        // We have to keep track of the largest possible amplitude,
        // 我们必须记录可能出现的最大振幅,
        // because each octave adds more, and we need a value in [-1, 1].
        // 因为每个倍频都会叠加更多,而我们需要一个 [-1, 1] 范围内的值。
        double maxAmplitude = 0;

        for(int i = 0; i < octaves; i++){
            total += (raw2d(seed, x * frequency, y * frequency) + 1f) / 2f * amplitude;

            frequency *= 2;
            maxAmplitude += amplitude;
            amplitude *= persistence;
        }

        return (float)(total / maxAmplitude);
    }


    // 3D Multi-octave Simplex noise.
    // 3D 多倍频 Simplex 噪声。
    //
    // For each octave, a higher frequency/lower amplitude function will be added to the original.
    // 每个倍频都会向原始结果叠加一个频率更高/振幅更低的函数。
    // The higher the persistence [0-1], the more of each succeeding octave will be added.
    // 持续度(persistence)[0-1] 越高,后续每个倍频叠加的部分就越多。
    public static float noise3d(int seed, double octaves, double persistence, double scale, double x, double y, double z){
        double total = 0;
        double frequency = scale;
        double amplitude = 1;

        // We have to keep track of the largest possible amplitude,
        // 我们必须记录可能出现的最大振幅,
        // because each octave adds more, and we need a value in [-1, 1].
        // 因为每个倍频都会叠加更多,而我们需要一个 [-1, 1] 范围内的值。
        double maxAmplitude = 0;

        for(int i = 0; i < octaves; i++){
            total += (raw3d(seed, x * frequency, y * frequency, z * frequency) + 1f) / 2f * amplitude;

            frequency *= 2;
            maxAmplitude += amplitude;
            amplitude *= persistence;
        }

        return (float)(total / maxAmplitude);
    }


    // 4D Multi-octave Simplex noise.
    // 4D 多倍频 Simplex 噪声。
    //
    // For each octave, a higher frequency/lower amplitude function will be added to the original.
    // 每个倍频都会向原始结果叠加一个频率更高/振幅更低的函数。
    // The higher the persistence [0-1], the more of each succeeding octave will be added.
    // 持续度(persistence)[0-1] 越高,后续每个倍频叠加的部分就越多。
    public static float noise4d(double octaves, double persistence, double scale, double x, double y, double z, double w){
        double total = 0;
        double frequency = scale;
        double amplitude = 1;

        // We have to keep track of the largest possible amplitude,
        // 我们必须记录可能出现的最大振幅,
        // because each octave adds more, and we need a value in [-1, 1].
        // 因为每个倍频都会叠加更多,而我们需要一个 [-1, 1] 范围内的值。
        double maxAmplitude = 0;

        for(int i = 0; i < octaves; i++){
            total += raw4d(x * frequency, y * frequency, z * frequency, w * frequency) * amplitude;

            frequency *= 2;
            maxAmplitude += amplitude;
            amplitude *= persistence;
        }

        return (float)(total / maxAmplitude);
    }

    // 2D raw Simplex noise
    // 2D 原始 Simplex 噪声
    public static double raw2d(int seed, double x, double y){
        // Noise contributions from the three corners
        // 来自三个角点的噪声贡献
        double n0, n1, n2;

        // Skew the input space to determine which simplex cell we're in
        // 对输入空间进行偏斜,以确定我们处于哪个单纯形单元中
        double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
        // Hairy factor for 2D
        // 2D 用的棘手系数
        double s = (x + y) * F2;
        int i = fastfloor(x + s);
        int j = fastfloor(y + s);

        double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;
        double t = (i + j) * G2;
        // Unskew the cell origin back to (x,y) space
        // 将单元原点反偏斜回 (x,y) 空间
        double X0 = i - t;
        double Y0 = j - t;
        // The x,y distances from the cell origin
        // 距单元原点的 x,y 距离
        double x0 = x - X0;
        double y0 = y - Y0;

        // For the 2D case, the simplex shape is an equilateral triangle.
        // 对于 2D 情形,单纯形是一个等边三角形。
        // Determine which simplex we are in.
        // 判断我们处于哪个单纯形中。
        int i1, j1; // Offsets for second (middle) corner of simplex in (i,j) coords
        // (i,j) 坐标中单纯形第二个(中间)角点的偏移量
        if(x0 > y0){
            i1 = 1;
            j1 = 0;
        } // lower triangle, XY order: (0,0)->(1,0)->(1,1)
        // 下三角,XY 顺序:(0,0)->(1,0)->(1,1)
        else{
            i1 = 0;
            j1 = 1;
        } // upper triangle, YX order: (0,0)->(0,1)->(1,1)
        // 上三角,YX 顺序:(0,0)->(0,1)->(1,1)

        // A step of (1,0) in (i,j) means a step of (1-c,-c) in (x,y), and
        // 在 (i,j) 中步进 (1,0) 相当于在 (x,y) 中步进 (1-c,-c),且
        // a step of (0,1) in (i,j) means a step of (-c,1-c) in (x,y), where
        // 在 (i,j) 中步进 (0,1) 相当于在 (x,y) 中步进 (-c,1-c),其中
        // c = (3-sqrt(3))/6
        // c = (3-sqrt(3))/6,即偏斜量。
        double x1 = x0 - i1 + G2; // Offsets for middle corner in (x,y) unskewed coords
        // (x,y) 非偏斜坐标中中间角点的偏移量
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0 + 2.0 * G2; // Offsets for last corner in (x,y) unskewed coords
        // (x,y) 非偏斜坐标中最后一个角点的偏移量
        double y2 = y0 - 1.0 + 2.0 * G2;

        // Work out the hashed gradient indices of the three simplex corners
        // 计算三个单纯形角点的哈希梯度索引
        int ii = i & 255;
        int jj = j & 255;
        int gi0 = perm(seed, ii + perm(seed, jj)) % 12;
        int gi1 = perm(seed, ii + i1 + perm(seed, jj + j1)) % 12;
        int gi2 = perm(seed, ii + 1 + perm(seed, jj + 1)) % 12;

        // Calculate the contribution from the three corners
        // 计算三个角点的贡献
        double t0 = 0.5 - x0 * x0 - y0 * y0;
        if(t0 < 0) n0 = 0.0;
        else{
            t0 *= t0;
            n0 = t0 * t0 * dot(grad3[gi0], x0, y0); // (x,y) of grad3 used for 2D gradient
            // grad3 中用于 2D 梯度的 (x,y) 分量
        }

        double t1 = 0.5 - x1 * x1 - y1 * y1;
        if(t1 < 0) n1 = 0.0;
        else{
            t1 *= t1;
            n1 = t1 * t1 * dot(grad3[gi1], x1, y1);
        }

        double t2 = 0.5 - x2 * x2 - y2 * y2;
        if(t2 < 0) n2 = 0.0;
        else{
            t2 *= t2;
            n2 = t2 * t2 * dot(grad3[gi2], x2, y2);
        }

        // Add contributions from each corner to get the final noise value.
        // 将各角点的贡献相加,得到最终噪声值。
        // The result is scaled to return values in the interval [-1,1].
        // 结果经过缩放,返回值位于区间 [-1,1] 内。
        return 70.0 * (n0 + n1 + n2);
    }

    // 3D raw Simplex noise
    // 3D 原始 Simplex 噪声
    public static double raw3d(int seed, double x, double y, double z){
        double n0, n1, n2, n3; // Noise contributions from the four corners
        // 来自四个角点的噪声贡献

        // Skew the input space to determine which simplex cell we're in
        // 对输入空间进行偏斜,以确定我们处于哪个单纯形单元中
        double F3 = 1.0 / 3.0;
        double s = (x + y + z) * F3; // Very nice and simple skew factor for 3D
        // 3D 用的非常简洁的偏斜因子
        int i = fastfloor(x + s);
        int j = fastfloor(y + s);
        int k = fastfloor(z + s);

        double G3 = 1.0 / 6.0; // Very nice and simple unskew factor, too
        // 3D 用的同样非常简洁的反偏斜因子
        double t = (i + j + k) * G3;
        double X0 = i - t; // Unskew the cell origin back to (x,y,z) space
        // 将单元原点反偏斜回 (x,y,z) 空间
        double Y0 = j - t;
        double Z0 = k - t;
        double x0 = x - X0; // The x,y,z distances from the cell origin
        // 距单元原点的 x,y,z 距离
        double y0 = y - Y0;
        double z0 = z - Z0;

        // For the 3D case, the simplex shape is a slightly irregular tetrahedron.
        // 对于 3D 情形,单纯形是一个略不规则的四面体。
        // Determine which simplex we are in.
        // 判断我们处于哪个单纯形中。
        int i1, j1, k1; // Offsets for second corner of simplex in (i,j,k) coords
        // (i,j,k) 坐标中单纯形第二个角点的偏移量
        int i2, j2, k2; // Offsets for third corner of simplex in (i,j,k) coords
        // (i,j,k) 坐标中单纯形第三个角点的偏移量

        if(x0 >= y0){
            if(y0 >= z0){
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            } // X Y Z order
            // X Y Z 顺序
            else if(x0 >= z0){
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            } // X Z Y order
            // X Z Y 顺序
            else{
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            } // Z X Y order
            // Z X Y 顺序
        }else{ // x0<y0
        // x0 小于 y0
            if(y0 < z0){
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 0;
                j2 = 1;
                k2 = 1;
            } // Z Y X order
            // Z Y X 顺序
            else if(x0 < z0){
                i1 = 0;
                j1 = 1;
                k1 = 0;
                i2 = 0;
                j2 = 1;
                k2 = 1;
            } // Y Z X order
            // Y Z X 顺序
            else{
                i1 = 0;
                j1 = 1;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            } // Y X Z order
            // Y X Z 顺序
        }

        // A step of (1,0,0) in (i,j,k) means a step of (1-c,-c,-c) in (x,y,z),
        // 在 (i,j,k) 中步进 (1,0,0) 相当于在 (x,y,z) 中步进 (1-c,-c,-c),
        // a step of (0,1,0) in (i,j,k) means a step of (-c,1-c,-c) in (x,y,z), and
        // 在 (i,j,k) 中步进 (0,1,0) 相当于在 (x,y,z) 中步进 (-c,1-c,-c),且
        // a step of (0,0,1) in (i,j,k) means a step of (-c,-c,1-c) in (x,y,z), where
        // 在 (i,j,k) 中步进 (0,0,1) 相当于在 (x,y,z) 中步进 (-c,-c,1-c),其中
        // c = 1/6.
        // c = 1/6,即偏斜量。
        double x1 = x0 - i1 + G3; // Offsets for second corner in (x,y,z) coords
        // (x,y,z) 坐标中第二个角点的偏移量
        double y1 = y0 - j1 + G3;
        double z1 = z0 - k1 + G3;
        double x2 = x0 - i2 + 2.0 * G3; // Offsets for third corner in (x,y,z) coords
        // (x,y,z) 坐标中第三个角点的偏移量
        double y2 = y0 - j2 + 2.0 * G3;
        double z2 = z0 - k2 + 2.0 * G3;
        double x3 = x0 - 1.0 + 3.0 * G3; // Offsets for last corner in (x,y,z) coords
        // (x,y,z) 坐标中最后一个角点的偏移量
        double y3 = y0 - 1.0 + 3.0 * G3;
        double z3 = z0 - 1.0 + 3.0 * G3;

        // Work out the hashed gradient indices of the four simplex corners
        // 计算四个单纯形角点的哈希梯度索引
        int ii = i & 255;
        int jj = j & 255;
        int kk = k & 255;
        int gi0 = perm(seed, ii + perm(seed, jj + perm(seed, kk))) % 12;
        int gi1 = perm(seed, ii + i1 + perm(seed, jj + j1 + perm(seed, kk + k1))) % 12;
        int gi2 = perm(seed, ii + i2 + perm(seed, jj + j2 + perm(seed, kk + k2))) % 12;
        int gi3 = perm(seed, ii + 1 + perm(seed, jj + 1 + perm(seed, kk + 1))) % 12;

        // Calculate the contribution from the four corners
        // 计算四个角点的贡献
        double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0;
        if(t0 < 0) n0 = 0.0;
        else{
            t0 *= t0;
            n0 = t0 * t0 * dot(grad3[gi0], x0, y0, z0);
        }

        double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1;
        if(t1 < 0) n1 = 0.0;
        else{
            t1 *= t1;
            n1 = t1 * t1 * dot(grad3[gi1], x1, y1, z1);
        }

        double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2;
        if(t2 < 0) n2 = 0.0;
        else{
            t2 *= t2;
            n2 = t2 * t2 * dot(grad3[gi2], x2, y2, z2);
        }

        double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3;
        if(t3 < 0) n3 = 0.0;
        else{
            t3 *= t3;
            n3 = t3 * t3 * dot(grad3[gi3], x3, y3, z3);
        }

        // Add contributions from each corner to get the final noise value.
        // 将各角点的贡献相加,得到最终噪声值。
        // The result is scaled to stay just inside [-1,1]
        // 结果经过缩放,恰好保持在 [-1,1] 之内
        return 32.0 * (n0 + n1 + n2 + n3);
    }


    // 4D raw Simplex noise
    // 4D 原始 Simplex 噪声
    public static double raw4d(double x, double y, double z, double w){
        // The skewing and unskewing factors are hairy again for the 4D case
        // 4D 情形的偏斜与反偏斜因子又变得相当棘手
        double F4 = (Math.sqrt(5.0) - 1.0) / 4.0;
        double G4 = (5.0 - Math.sqrt(5.0)) / 20.0;
        double n0, n1, n2, n3, n4; // Noise contributions from the five corners
        // 来自五个角点的噪声贡献

        // Skew the (x,y,z,w) space to determine which cell of 24 simplices we're in
        // 对 (x,y,z,w) 空间进行偏斜,以确定我们处于 24 个单纯形单元的哪一个中
        double s = (x + y + z + w) * F4; // Factor for 4D skewing
        // 4D 偏斜因子
        int i = fastfloor(x + s);
        int j = fastfloor(y + s);
        int k = fastfloor(z + s);
        int l = fastfloor(w + s);
        double t = (i + j + k + l) * G4; // Factor for 4D unskewing
        // 4D 反偏斜因子
        double X0 = i - t; // Unskew the cell origin back to (x,y,z,w) space
        // 将单元原点反偏斜回 (x,y,z,w) 空间
        double Y0 = j - t;
        double Z0 = k - t;
        double W0 = l - t;

        double x0 = x - X0; // The x,y,z,w distances from the cell origin
        // 距单元原点的 x,y,z,w 距离
        double y0 = y - Y0;
        double z0 = z - Z0;
        double w0 = w - W0;

        // For the 4D case, the simplex is a 4D shape I won't even try to describe.
        // 对于 4D 情形,单纯形是一个 4D 形状,我就不试图描述了。
        // To find out which of the 24 possible simplices we're in, we need to
        // 要确定我们处于 24 种可能单纯形中的哪一个,
        // determine the magnitude ordering of x0, y0, z0 and w0.
        // 确定 x0、y0、z0 和 w0 的幅值次序。
        // The method below is a good way of finding the ordering of x,y,z,w and
        // 下面的方法是确定 x,y,z,w 大小次序的一个好办法,
        // then find the correct traversal order for the simplex we're in.
        // 进而找到所在单纯形的正确遍历顺序。
        // First, six pair-wise comparisons are performed between each possible pair
        // 首先,对四个坐标的所有两两组合
        // of the four coordinates, and the results are used to add up binary bits
        // 共进行六次比较,其结果用于累加二进制位,
        // for an integer index.
        // 从而构成一个整数索引。
        int c1 = (x0 > y0) ? 32 : 0;
        int c2 = (x0 > z0) ? 16 : 0;
        int c3 = (y0 > z0) ? 8 : 0;
        int c4 = (x0 > w0) ? 4 : 0;
        int c5 = (y0 > w0) ? 2 : 0;
        int c6 = (z0 > w0) ? 1 : 0;
        int c = c1 + c2 + c3 + c4 + c5 + c6;

        int i1, j1, k1, l1; // The integer offsets for the second simplex corner
        // 单纯形第二个角点的整数偏移
        int i2, j2, k2, l2; // The integer offsets for the third simplex corner
        // 单纯形第三个角点的整数偏移
        int i3, j3, k3, l3; // The integer offsets for the fourth simplex corner
        // 单纯形第四个角点的整数偏移

        // simplex[c] is a 4-vector with the numbers 0, 1, 2 and 3 in some order.
        // simplex[c] 是一个 4 维向量,以某种顺序包含数字 0、1、2、3。
        // Many values of c will never occur, since e.g. x>y>z>w makes x<z, y<w and x<w
        // c 的许多取值永远不会出现,例如 x>y>z>w 会使得 x<z、y<w 和 x<w
        // impossible. Only the 24 indices which have non-zero entries make any sense.
        // 同时成立。只有那 24 个含非零项的索引才有意义。
        // We use a thresholding to set the coordinates in turn from the largest magnitude.
        // 我们通过阈值判断,依次按幅值从大到小设置各坐标。
        // The number 3 in the "simplex" array is at the position of the largest coordinate.
        // “simplex” 数组中的数字 3 位于最大坐标处。
        i1 = simplex[c][0] >= 3 ? 1 : 0;
        j1 = simplex[c][1] >= 3 ? 1 : 0;
        k1 = simplex[c][2] >= 3 ? 1 : 0;
        l1 = simplex[c][3] >= 3 ? 1 : 0;
        // The number 2 in the "simplex" array is at the second largest coordinate.
        // “simplex” 数组中的数字 2 位于第二大的坐标处。
        i2 = simplex[c][0] >= 2 ? 1 : 0;
        j2 = simplex[c][1] >= 2 ? 1 : 0;
        k2 = simplex[c][2] >= 2 ? 1 : 0;
        l2 = simplex[c][3] >= 2 ? 1 : 0;
        // The number 1 in the "simplex" array is at the second smallest coordinate.
        // “simplex” 数组中的数字 1 位于第二小的坐标处。
        i3 = simplex[c][0] >= 1 ? 1 : 0;
        j3 = simplex[c][1] >= 1 ? 1 : 0;
        k3 = simplex[c][2] >= 1 ? 1 : 0;
        l3 = simplex[c][3] >= 1 ? 1 : 0;
        // The fifth corner has all coordinate offsets = 1, so no need to look that up.
        // 第五个角点的所有坐标偏移均为 1,因此无需查表。

        double x1 = x0 - i1 + G4; // Offsets for second corner in (x,y,z,w) coords
        // (x,y,z,w) 坐标中第二个角点的偏移量
        double y1 = y0 - j1 + G4;
        double z1 = z0 - k1 + G4;
        double w1 = w0 - l1 + G4;
        double x2 = x0 - i2 + 2.0 * G4; // Offsets for third corner in (x,y,z,w) coords
        // (x,y,z,w) 坐标中第三个角点的偏移量
        double y2 = y0 - j2 + 2.0 * G4;
        double z2 = z0 - k2 + 2.0 * G4;
        double w2 = w0 - l2 + 2.0 * G4;
        double x3 = x0 - i3 + 3.0 * G4; // Offsets for fourth corner in (x,y,z,w) coords
        // (x,y,z,w) 坐标中第四个角点的偏移量
        double y3 = y0 - j3 + 3.0 * G4;
        double z3 = z0 - k3 + 3.0 * G4;
        double w3 = w0 - l3 + 3.0 * G4;
        double x4 = x0 - 1.0 + 4.0 * G4; // Offsets for last corner in (x,y,z,w) coords
        // (x,y,z,w) 坐标中最后一个角点的偏移量
        double y4 = y0 - 1.0 + 4.0 * G4;
        double z4 = z0 - 1.0 + 4.0 * G4;
        double w4 = w0 - 1.0 + 4.0 * G4;

        // Work out the hashed gradient indices of the five simplex corners
        // 计算五个单纯形角点的哈希梯度索引
        int ii = i & 255;
        int jj = j & 255;
        int kk = k & 255;
        int ll = l & 255;
        int gi0 = (ii + (jj + (kk + (ll)))) % 32;
        int gi1 = (ii + i1 + (jj + j1 + (kk + k1 + (ll + l1)))) % 32;
        int gi2 = (ii + i2 + (jj + j2 + (kk + k2 + (ll + l2)))) % 32;
        int gi3 = (ii + i3 + (jj + j3 + (kk + k3 + (ll + l3)))) % 32;
        int gi4 = (ii + 1 + (jj + 1 + (kk + 1 + (ll + 1)))) % 32;

        // Calculate the contribution from the five corners
        // 计算五个角点的贡献
        double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0 - w0 * w0;
        if(t0 < 0) n0 = 0.0;
        else{
            t0 *= t0;
            n0 = t0 * t0 * dot(grad4[gi0], x0, y0, z0, w0);
        }

        double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1 - w1 * w1;
        if(t1 < 0) n1 = 0.0;
        else{
            t1 *= t1;
            n1 = t1 * t1 * dot(grad4[gi1], x1, y1, z1, w1);
        }

        double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2 - w2 * w2;
        if(t2 < 0) n2 = 0.0;
        else{
            t2 *= t2;
            n2 = t2 * t2 * dot(grad4[gi2], x2, y2, z2, w2);
        }

        double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3 - w3 * w3;
        if(t3 < 0) n3 = 0.0;
        else{
            t3 *= t3;
            n3 = t3 * t3 * dot(grad4[gi3], x3, y3, z3, w3);
        }

        double t4 = 0.6 - x4 * x4 - y4 * y4 - z4 * z4 - w4 * w4;
        if(t4 < 0) n4 = 0.0;
        else{
            t4 *= t4;
            n4 = t4 * t4 * dot(grad4[gi4], x4, y4, z4, w4);
        }

        // Sum up and scale the result to cover the range [-1,1]
        // 求和并将结果缩放到 [-1,1] 范围
        return 27.0 * (n0 + n1 + n2 + n3 + n4);
    }

    public static double rawTiled(double x, double y, double x1, double y1, double w, double h, double scl){
        x /= scl;
        y /= scl;
        w /= scl;
        h /= scl;

        double x2 = x1 + w, y2 = y1 + h;

        double s = x / w, t = y / h;
        double dx = x2 - x1, dy = y2 - y1;

        double nx = x1 + Math.cos(s*2*PI)*dx/PI2;
        double ny = y1 + Math.cos(t*2*PI)*dy/PI2;
        double nz = x1 + Math.sin(s*2*PI)*dx/PI2;
        double nw = y1 + Math.sin(t*2*PI)*dy/PI2;

        return raw4d(nx, ny, nz, nw);
    }

    //hash function: seed (any) + x (will be masked to fit in 0-255) -> 0-255
    // 哈希函数:seed(任意)+ x(会被掩码截到 0-255)-> 0-255
    //thanks to TEttinger on Discord for the negative coordinate discontinuity fix
    // 感谢 Discord 上的 TEttinger 修复负坐标不连续问题
    static int perm(int seed, int x){
        x = (x & 255) * 0x45d9f3b;
        x = ((x >>> 16) ^ x) * (0x45d9f3b + seed);
        x = (x >>> 16) ^ x;
        return x & 0xff;
    }

    static int fastfloor(double x){
        return x > 0 ? (int)x : (int)x - 1;
    }

    static double dot(int[] g, double x, double y){
        return g[0] * x + g[1] * y;
    }

    static double dot(int[] g, double x, double y, double z){
        return g[0] * x + g[1] * y + g[2] * z;
    }

    static double dot(int[] g, double x, double y, double z, double w){
        return g[0] * x + g[1] * y + g[2] * z + g[3] * w;
    }
}
