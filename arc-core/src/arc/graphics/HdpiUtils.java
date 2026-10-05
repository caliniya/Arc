package arc.graphics;

import arc.*;
import arc.graphics.gl.*;

/**
 * To deal with HDPI monitors properly, use the glViewport and glScissor functions of this class instead of directly calling
 * OpenGL yourself. The logical coordinate system provided by the operating system may not have the same resolution as the actual
 * drawing surface to which OpenGL draws, also known as the backbuffer. This class will ensure, that you pass the correct values
 * to OpenGL for any function that expects backbuffer coordinates instead of logical coordinates.
 * <p>
 * 要正确处理 HDPI 显示器,请使用本类的 glViewport 和 glScissor 函数,而不是直接调用 OpenGL。操作系统提供的逻辑坐标系分辨率可能与 OpenGL 实际绘制的绘图表面(即后备缓冲区)不同。本类确保在任何需要后备缓冲区坐标而非逻辑坐标的函数调用中,向 OpenGL 传入正确的值。
 * @author badlogic
 */
public class HdpiUtils{
    private static HdpiMode mode = HdpiMode.logical;

    /**
     * Allows applications to override HDPI coordinate conversion for glViewport and glScissor calls.
     * <p>
     * This function can be used to ignore the default behavior, for example when rendering a UI stage
     * to an off-screen framebuffer:
     *
     * <pre>
     * HdpiUtils.setMode(HdpiMode.Pixels);
     * fb.begin();
     * stage.draw();
     * fb.end();
     * HdpiUtils.setMode(HdpiMode.Logical);
     * </pre>
     * <p>
     * 允许应用程序重写 glViewport 和 glScissor 调用的 HDPI 坐标转换。 <p> 此函数可用于忽略默认行为,例如将 UI 舞台渲染到离屏帧缓冲时: <pre> HdpiUtils.setMode(HdpiMode.Pixels); fb.begin(); stage.draw(); fb.end(); HdpiUtils.setMode(HdpiMode.Logical); </pre>
     * @param mode set to HdpiMode.Pixels to ignore HDPI conversion for glViewport and glScissor functions 设为 HdpiMode.Pixels 可让 glViewport 和 glScissor 函数忽略 HDPI 转换
     */
    public static void setMode(HdpiMode mode){
        HdpiUtils.mode = mode;
    }

    /**
     * Calls {@link Gl#scissor(int, int, int, int)}, expecting the coordinates and sizes given in logical coordinates and
     * automatically converts them to backbuffer coordinates, which may be bigger on HDPI screens.
     * <p>
     * 调用 {@link Gl#scissor(int, int, int, int)},期望传入逻辑坐标表示的坐标和尺寸,并自动将其转换为后备缓冲区坐标(HDPI 屏幕上可能更大)。
     */
    public static void glScissor(int x, int y, int width, int height){
        if(mode == HdpiMode.logical && (Core.graphics.getWidth() != Core.graphics.getBackBufferWidth()
        || Core.graphics.getHeight() != Core.graphics.getBackBufferHeight())){
            Gl.scissor(toBackBufferX(x), toBackBufferY(y), toBackBufferX(width), toBackBufferY(height));
        }else{
            Gl.scissor(x, y, width, height);
        }
    }

    /**
     * Calls {@link Gl#viewport(int, int, int, int)}, expecting the coordinates and sizes given in logical coordinates and
     * automatically converts them to backbuffer coordinates, which may be bigger on HDPI screens.
     * <p>
     * 调用 {@link Gl#viewport(int, int, int, int)},期望传入逻辑坐标表示的坐标和尺寸,并自动将其转换为后备缓冲区坐标(HDPI 屏幕上可能更大)。
     */
    public static void glViewport(int x, int y, int width, int height){
        if(mode == HdpiMode.logical && (Core.graphics.getWidth() != Core.graphics.getBackBufferWidth()
        || Core.graphics.getHeight() != Core.graphics.getBackBufferHeight())){
            Gl.viewport(toBackBufferX(x), toBackBufferY(y), toBackBufferX(width), toBackBufferY(height));
        }else{
            Gl.viewport(x, y, width, height);
        }
    }

    /**
     * Converts an x-coordinate given in backbuffer coordinates to
     * logical screen coordinates.
     * <p>
     * 将后备缓冲区坐标表示的 x 坐标转换为逻辑屏幕坐标。
     */
    public static int toLogicalX(int backBufferX){
        return (int)(backBufferX * Core.graphics.getWidth() / (float)Core.graphics.getBackBufferWidth());
    }

    /**
     * Convers an y-coordinate given in backbuffer coordinates to
     * logical screen coordinates
     * <p>
     * 将后备缓冲区坐标表示的 y 坐标转换为逻辑屏幕坐标
     */
    public static int toLogicalY(int backBufferY){
        return (int)(backBufferY * Core.graphics.getHeight() / (float)Core.graphics.getBackBufferHeight());
    }

    /**
     * Converts an x-coordinate given in logical screen coordinates to
     * backbuffer coordinates.
     * <p>
     * 将逻辑屏幕坐标表示的 x 坐标转换为后备缓冲区坐标。
     */
    public static int toBackBufferX(int logicalX){
        return (int)(logicalX * Core.graphics.getBackBufferWidth() / (float)Core.graphics.getWidth());
    }

    /**
     * Convers an y-coordinate given in backbuffer coordinates to
     * logical screen coordinates
     * <p>
     * 将后备缓冲区坐标表示的 y 坐标转换为逻辑屏幕坐标
     */
    public static int toBackBufferY(int logicalY){
        return (int)(logicalY * Core.graphics.getBackBufferHeight() / (float)Core.graphics.getHeight());
    }

    public enum HdpiMode{
        /**
         * mouse coordinates, {@link Graphics#getWidth()} and
         * {@link Graphics#getHeight()} will return logical coordinates
         * according to the system defined HDPI scaling. Rendering will be
         * performed to a backbuffer at raw resolution. Use {@link HdpiUtils}
         * when calling {@link Gl#scissor} or {@link Gl#viewport} which
         * expect raw coordinates.
         * <p>
         * 鼠标坐标,{@link Graphics#getWidth()} 和 {@link Graphics#getHeight()} 会根据系统定义的 HDPI 缩放返回逻辑坐标。渲染将在原始分辨率的后备缓冲区上进行。调用需要原始坐标的 {@link Gl#scissor} 或 {@link Gl#viewport} 时请使用 {@link HdpiUtils}。
         */
        logical,

        /**
         * Mouse coordinates, {@link Graphics#getWidth()} and
         * {@link Graphics#getHeight()} will return raw pixel coordinates
         * irrespective of the system defined HDPI scaling.
         * <p>
         * 鼠标坐标,{@link Graphics#getWidth()} 和 {@link Graphics#getHeight()} 将返回原始像素坐标,不受系统定义的 HDPI 缩放影响。
         */
        pixels
    }
}
