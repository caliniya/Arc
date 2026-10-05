package arc.graphics.g2d;

import arc.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;

/**
 * A stack of {@link Rect} objects to be used for clipping via glScissor. When a new
 * Rectangle is pushed onto the stack, it will be merged with the current top of stack. The minimum area of overlap is then set as
 * the real top of the stack.
 * <p>
 * 用于通过 glScissor 裁剪的 {@link Rect} 对象栈。新矩形入栈时会与当前栈顶合并,取最小重叠区域作为实际栈顶。
 * @author mzechner
 */
public class ScissorStack{
    static final Rect viewport = new Rect();
    static Vec2 tmp = new Vec2();
    private static Ar<Rect> scissors = new Ar<>();

    /**
     * Pushes a new scissor {@link Rect} onto the stack, merging it with the current top of the stack. The minimal area of
     * overlap between the top of stack rectangle and the provided rectangle is pushed onto the stack. This will invoke
     * glScissor with the final top of stack rectangle. In case no scissor is yet on the stack
     * this will also enable glScissorTest automatically.
     * <p>
     * Any drawing should be flushed before pushing scissors.
     * <p>
     * 将新的剪裁 {@link Rect} 压入栈,并与当前栈顶合并。栈顶矩形与给定矩形的最小重叠区域被压入栈,并以最终栈顶矩形调用 glScissor。若栈中还没有剪裁区域,也会自动启用 glScissorTest。 <p> 推入剪裁区域前应刷新所有绘制。
     * @return true if the scissors were pushed. false if the scissor area was zero, in this case the scissors were not pushed and
     * no drawing should occur. 若剪裁区域已推入则为 true;若剪裁区域为零则为 false,此时不会推入,也不应进行任何绘制。
     */
    public static boolean push(Rect scissor){
        fix(scissor);

        if(scissors.size == 0){
            if(scissor.width < 1 || scissor.height < 1) return false;
            Draw.flush();
            Gl.enable(Gl.scissorTest);
        }else{
            // merge scissors
            // 合并剪裁区域
            Rect parent = scissors.get(scissors.size - 1);
            float minX = Math.max(parent.x, scissor.x);
            float maxX = Math.min(parent.x + parent.width, scissor.x + scissor.width);
            if(maxX - minX < 1) return false;

            float minY = Math.max(parent.y, scissor.y);
            float maxY = Math.min(parent.y + parent.height, scissor.y + scissor.height);
            if(maxY - minY < 1) return false;

            Draw.flush();
            scissor.x = minX;
            scissor.y = minY;
            scissor.width = maxX - minX;
            scissor.height = Math.max(1, maxY - minY);
        }

        scissors.add(scissor);
        HdpiUtils.glScissor((int)scissor.x, (int)scissor.y, (int)scissor.width, (int)scissor.height);
        return true;
    }

    /**
     * Pops the current scissor rectangle from the stack and sets the new scissor area to the new top of stack rectangle. In case
     * no more rectangles are on the stack, glScissorTest is disabled.
     * <p>
     * Any drawing should be flushed before popping scissors.
     * <p>
     * 从栈中弹出当前剪裁矩形,并将剪裁区域设置为新的栈顶矩形。若栈中不再有矩形,则禁用 glScissorTest。 <p> 弹出剪裁区域前应刷新所有绘制。
     */
    public static Rect pop(){
        Draw.flush();
        Rect old = scissors.pop();
        if(scissors.size == 0)
            Gl.disable(Gl.scissorTest);
        else{
            Rect scissor = scissors.peek();
            HdpiUtils.glScissor((int)scissor.x, (int)scissor.y, (int)scissor.width, (int)scissor.height);
        }
        return old;
    }

    public static boolean pushWorld(Rect scissorWorld){
        calculateScissors(Core.camera, Tmp.m1.idt(), Tmp.r1.set(scissorWorld), scissorWorld);
        return push(scissorWorld);
    }

    public static Rect peek(){
        return scissors.peek();
    }

    private static void fix(Rect rect){
        rect.x = Math.round(rect.x);
        rect.y = Math.round(rect.y);
        rect.width = Math.round(rect.width);
        rect.height = Math.round(rect.height);
        if(rect.width < 0){
            rect.width = -rect.width;
            rect.x -= rect.width;
        }
        if(rect.height < 0){
            rect.height = -rect.height;
            rect.y -= rect.height;
        }
    }

    /**
     * Calculates a scissor rectangle using 0,0,Core.graphics.getWidth(),Core.graphics.getHeight() as the viewport.
     * <p>
     * 使用 0,0,Core.graphics.getWidth(),Core.graphics.getHeight() 作为视口计算剪裁矩形。
     * @see #calculateScissors(Camera, float, float, float, float, Mat, Rect, Rect)
     */
    public static void calculateScissors(Camera camera, Mat batchTransform, Rect area, Rect scissor){
        calculateScissors(camera, 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight(), batchTransform, area, scissor);
    }

    /**
     * Calculates a scissor rectangle in OpenGL ES window coordinates from a {@link Camera}, a transformation {@link Mat} and
     * an axis aligned {@link Rect}. The rectangle will get transformed by the camera and transform matrices and is then
     * projected to screen coordinates. Note that only axis aligned rectangles will work with this method. If either the Camera or
     * the Matrix4 have rotational components, the output of this method will not be suitable for glScissor.
     * <p>
     * 根据 {@link Camera}、变换 {@link Mat} 和轴对齐的 {@link Rect} 计算 OpenGL ES 窗口坐标下的剪裁矩形。矩形经相机和变换矩阵变换后投影到屏幕坐标。注意,只有轴对齐的矩形适用于此方法。若 Camera 或 Matrix4 带有旋转分量,则此方法的输出不适合 glScissor。
     * @param camera the {@link Camera} 相机
     * @param batchTransform the transformation {@link Mat} 变换 {@link Mat}
     * @param area the {@link Rect} to transform to window coordinates 要变换到窗口坐标的 {@link Rect}
     * @param scissor the Rectangle to store the result in 存储结果的矩形
     */
    public static void calculateScissors(Camera camera, float viewportX, float viewportY, float viewportWidth,
                                         float viewportHeight, Mat batchTransform, Rect area, Rect scissor){
        tmp.set(area.x, area.y);
        tmp.mul(batchTransform);
        camera.project(tmp, viewportX, viewportY, viewportWidth, viewportHeight);
        scissor.x = tmp.x;
        scissor.y = tmp.y;

        tmp.set(area.x + area.width, area.y + area.height);
        tmp.mul(batchTransform);
        camera.project(tmp, viewportX, viewportY, viewportWidth, viewportHeight);
        scissor.width = tmp.x - scissor.x;
        scissor.height = tmp.y - scissor.y;
    }

    /**
     * @return the current viewport in OpenGL ES window coordinates based on the currently applied scissor
     * @return the current viewport in OpenGL ES window coordinates based on the currently applied scissor 基于当前剪裁区域的 OpenGL ES 窗口坐标视口
     */
    public static Rect getViewport(){
        if(scissors.size == 0){
            viewport.set(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
            return viewport;
        }else{
            Rect scissor = scissors.peek();
            viewport.set(scissor);
            return viewport;
        }
    }
}
