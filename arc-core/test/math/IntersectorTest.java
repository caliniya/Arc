package math;

import arc.math.Mathf;

import static org.junit.Assert.*;

public class IntersectorTest{

    /**
     * Compares two triangles for equality. Triangles must have the same winding, but may begin with different vertex. Values are
     * epsilon compared, with default tolerance. Triangles are assumed to be valid triangles - no duplicate vertices.
     * <p>
     * 比较两个三角形是否相等。三角形必须具有相同的环绕方向,但可以从不同的顶点开始。数值按默认容差进行 epsilon 比较。假定三角形都是有效三角形 - 没有重复顶点。
     */
    private static boolean triangleEquals(float[] base, int baseOffset, int stride, float[] comp){
        assertTrue(stride >= 3);
        assertTrue(base.length - baseOffset >= 9);
        assertEquals(9, comp.length);

        int offset = -1;
        // Find first comp vertex in base triangle
        // 在基准三角形中找到与 comp 的第一个匹配顶点
        for(int i = 0; i < 3; i++){
            int b = baseOffset + i * stride;
            if(Mathf.equal(base[b], comp[0]) && Mathf.equal(base[b + 1], comp[1])
            && Mathf.equal(base[b + 2], comp[2])){
                offset = i;
                break;
            }
        }
        assertTrue("Triangles do not have common first vertex.", offset != -1);
        // Compare vertices
        // 比较各顶点
        for(int i = 0; i < 3; i++){
            int b = baseOffset + ((offset + i) * stride) % (3 * stride);
            int c = i * stride;
            if(!Mathf.equal(base[b], comp[c]) || !Mathf.equal(base[b + 1], comp[c + 1])
            || !Mathf.equal(base[b + 2], comp[c + 2])){
                return false;
            }
        }
        return true;
    }

}
