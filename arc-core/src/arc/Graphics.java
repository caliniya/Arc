package arc;

import arc.Graphics.Cursor.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.graphics.gl.GLVersion.*;
import arc.math.*;
import arc.util.*;

/**
 * This interface encapsulates communication with the graphics processor. Depending on the available hardware and the current
 * {@link Application} configuration, access to {@link GLProvider} are provided here.
 * <p>
 * If supported by the backend, this interface lets you query the available display modes (graphics resolution and color depth)
 * and change it.
 * <p>
 * This interface can be used to switch between continuous and non-continuous rendering (see
 * {@link #setContinuousRendering(boolean)}), and to explicitly {@link #requestRendering()}.
 * <p>
 * 此接口封装与图形处理器的通信。根据可用硬件和当前 {@link Application} 配置,此处提供了对 {@link GLProvider} 的访问。
 * 如果后端支持,此接口让你能够查询可用的显示模式(图形分辨率和颜色深度)并进行更改。
 * 此接口可用于在连续渲染与非连续渲染之间切换(见 {@link #setContinuousRendering(boolean)}),以及显式调用 {@link #requestRendering()}。
 * @author mzechner
 */
public abstract class Graphics implements Disposable{
    /**
     * The last cursor used. Can be Cursor or SystemCursor.
     * 上次使用的光标。可以是 Cursor 或 SystemCursor。
     */
    private Object lastCursor;

    /**
     * @return whether instancing with glVertexAttribDivisor and glDrawElementsInstanced is available on this platform. 此平台是否支持通过 glVertexAttribDivisor 和 glDrawElementsInstanced 进行实例化渲染。
     */
    public boolean supportsInstancing(){
        //instancing via glDrawElementsInstanced + glVertexAttribDivisor is available on GL ES 3.0; otherwise, only >= Gl 3.3 has glVertexAttribDivisor on desktop
        // 通过 glDrawElementsInstanced + glVertexAttribDivisor 实现实例化在 GL ES 3.0 上可用;否则,在桌面上只有 Gl 3.3 及以上版本才提供 glVertexAttribDivisor
        return getGLVersion().type == GlType.GLES || getGLVersion().atLeast(3, 3);
    }

    /**
     * Clears the color buffer using the specified color.
     * 使用指定颜色清除颜色缓冲区。
     */
    public void clear(float r, float g, float b, float a){
        Gl.clearColor(r, g, b, a);
        Gl.clear(Gl.colorBufferBit);
    }

    /**
     * Clears the color buffer using the specified color.
     * 使用指定颜色清除颜色缓冲区。
     */
    public void clear(Color color){
        clear(color.r, color.g, color.b, color.a);
    }

    /**
     * Returns whether this application is probably in portrait mode, e.g. if width < height.
     * 返回此应用是否可能处于竖屏模式,例如宽度小于高度时。
     */
    public boolean isPortrait(){
        return getWidth() < getHeight();
    }

    /**
     * @return the width of the client area in logical pixels. 客户区的宽度(逻辑像素)。
     */
    public abstract int getWidth();

    /**
     * @return the height of the client area in logical pixels 客户区的高度(逻辑像素)
     */
    public abstract int getHeight();

    /**
     * @return the aspect ratio, e.g. width/height 宽高比,例如 宽度/高度
     */
    public float getAspect(){
        return (float)getWidth() / getHeight();
    }

    /** @return whether the window is 'hidden', e.g. whether width or height of the window is less than 2.
     * This is due to Windows reporting window size as 0 or 1 when minimized. This causes framebuffers to crash if resized. 窗口是否处于“隐藏”状态,例如窗口宽度或高度小于 2 的情况。这是因为 Windows 在最小化时会把窗口大小报告为 0 或 1,此时调整大小会导致帧缓冲崩溃。
     */
    public boolean isHidden(){
        return getWidth() < 2 || getHeight() < 2;
    }

    /**
     * @return the width of the framebuffer in physical pixels 帧缓冲的宽度(物理像素)
     */
    public abstract int getBackBufferWidth();

    /**
     * @return the height of the framebuffer in physical pixels 帧缓冲的高度(物理像素)
     */
    public abstract int getBackBufferHeight();

    /**
     * @return the safe area insets, in the order left-right-top-bottom. 安全区域内边距,顺序为左-右-上-下。
     */
    public int[] getSafeInsets(){
        return new int[4];
    }

    /**
     * Returns the id of the current frame. The general contract of this method is that the id is incremented only when the
     * application is in the running state right before calling the {@link ApplicationListener#update()} method. Also, the id of
     * the first frame is 0; the id of subsequent frames is guaranteed to take increasing values for 2<sup>63</sup>-1 rendering
     * cycles.
     * <p>
     * 返回当前帧的 id。此方法的通用约定是:仅当应用处于运行状态且即将调用 {@link ApplicationListener#update()} 方法时,id 才会递增。
     * 此外,第一帧的 id 为 0;后续帧的 id 保证在 2<sup>63</sup>-1 个渲染周期内取递增的值。
     * @return the id of the current frame 当前帧的 id
     */
    public abstract long getFrameId();

    /**
     * @return the time span between the current frame and the last frame in seconds. Might be smoothed over n frames. 当前帧与上一帧之间的时间间隔(秒)。可能在 n 帧上做了平滑。
     */
    public abstract float getDeltaTime();

    /**
     * @return the average number of frames per second 平均每秒帧数
     */
    public abstract int getFramesPerSecond();

    /**
     * @return the {@link GLVersion} of this Graphics instance 此 Graphics 实例的 {@link GLVersion}
     */
    public abstract GLVersion getGLVersion();

    /**
     * @return the pixels per inch on the x-axis x 轴上每英寸像素数
     */
    public abstract float getPpiX();

    /**
     * @return the pixels per inch on the y-axis y 轴上每英寸像素数
     */
    public abstract float getPpiY();

    /**
     * @return the pixels per centimeter on the x-axis x 轴上每厘米像素数
     */
    public abstract float getPpcX();

    /**
     * @return the pixels per centimeter on the y-axis. y 轴上每厘米像素数。
     */
    public abstract float getPpcY();

    /**
     * This is a scaling factor for the Density Independent Pixel unit, following the same conventions as
     * android.util.DisplayMetrics#density, where one DIP is one pixel on an approximately 160 dpi screen. Thus on a 160dpi screen
     * this density value will be 1; on a 120 dpi screen it would be .75; etc.
     * <p>
     * 这是密度无关像素(DIP)单位的缩放因子,遵循与 android.util.DisplayMetrics#density 相同的约定,即在大约 160 dpi 的屏幕上一个 DIP 等于一个像素。因此在 160dpi 的屏幕上该密度值为 1;在 120 dpi 的屏幕上为 .75;依此类推。
     * @return the logical density of the Display. 显示器的逻辑密度。
     */
    public abstract float getDensity();

    /**
     * Sets the window to full-screen mode.
     * <p>
     * 将窗口设置为全屏模式。
     * @return whether the operation succeeded. 操作是否成功。
     */
    public boolean setFullscreen(boolean fullscreen){
        return false;
    }

    /**
     * Sets the window position on its current monitor.
     * <p>
     * 设置窗口在其当前显示器上的位置。
     */
    public void setWindowPosition(int x, int y){}


    /**
     * Sets the window size in pixels.
     * <p>
     * 设置窗口大小(以像素为单位)。
     */
    public void setWindowSize(int width, int height){}

    /**
     * Sets the title of the window. Ignored on Android.
     * <p>
     * 设置窗口标题。在 Android 上被忽略。
     * @param title the title. 标题。
     */
    public abstract void setTitle(String title);

    /**
     * Enable/Disable vsynching. This is a best-effort attempt which might not work on all platforms.
     * <p>
     * 启用/禁用垂直同步。这是尽力而为的操作,并非在所有平台上都有效。
     * @param vsync vsync enabled or not. 是否启用垂直同步。
     */
    public abstract void setVSync(boolean vsync);

    /**
     * @return the format of the color, depth and stencil buffer in a {@link BufferFormat} instance 以 {@link BufferFormat} 实例表示的颜色、深度和模板缓冲区格式
     */
    public abstract BufferFormat getBufferFormat();

    /**
     * @param extension the extension name 扩展名
     * @return whether the extension is supported 该扩展是否受支持
     */
    public abstract boolean supportsExtension(String extension);

    /**
     * On iOS, sets the preferred frame rate. A value of 0 sets the maximum supported value for this device.
     * 在 iOS 上设置首选帧率。值为 0 表示使用此设备支持的最大值。
     */
    public void setPreferredFPS(int fps){}

    /**
     * @return whether rendering is continuous. 渲染是否为连续的。
     */
    public abstract boolean isContinuousRendering();

    /**
     * Sets whether to render continuously. In case rendering is performed non-continuously, the following events will trigger a
     * redraw:
     *
     * <ul>
     * <li>A call to {@link #requestRendering()}</li>
     * <li>Input events from the touch screen/mouse or keyboard</li>
     * <li>A {@link Runnable} is posted to the rendering thread via {@link Application#post(Runnable)}. In the case
     * of a multi-window app, all windows will request rendering if a runnable is posted to the application. To avoid this,
     * post a runnable to the window instead. </li>
     * </ul>
     * <p>
     * Life-cycle events will also be reported as usual, see {@link ApplicationListener}. This method can be called from any
     * thread.
     * <p>
     * 设置是否进行连续渲染。在非连续渲染的情况下,以下事件将触发重绘:
     *
     * <ul>
     * <li>调用 {@link #requestRendering()}</li>
     * <li>来自触摸屏/鼠标或键盘的输入事件</li>
     * <li>通过 {@link Application#post(Runnable)} 向渲染线程投递了一个 {@link Runnable}。对于多窗口应用,如果有 runnable 被投递到应用,所有窗口都会请求渲染。为避免这种情况,应向窗口投递 runnable。</li>
     * </ul>
     * <p>
     * 生命周期事件仍会照常上报,见 {@link ApplicationListener}。此方法可以在任何线程中调用。
     * @param isContinuous whether the rendering should be continuous or not. 渲染应该连续还是不连续。
     */
    public abstract void setContinuousRendering(boolean isContinuous);

    /**
     * Requests a new frame to be rendered if the rendering mode is non-continuous. This method can be called from any thread.
     * 若渲染模式为非连续,则请求渲染新的一帧。此方法可以在任何线程中调用。
     */
    public abstract void requestRendering();

    /**
     * Whether the app is fullscreen or not
     * 应用是否为全屏
     */
    public abstract boolean isFullscreen();

    /**
     * Create a new cursor represented by the {@link arc.graphics.Pixmap}. The Pixmap must be in RGBA8888 format,
     * width & height must be powers-of-two greater than zero (not necessarily equal) and of a certain minimum size (32x32 is a safe bet),
     * and alpha transparency must be single-bit (i.e., 0x00 or 0xFF only). This function returns a Cursor object that can be set as the
     * system cursor by calling {@link #setCursor(Cursor)} .
     * <p>
     * 用 {@link arc.graphics.Pixmap} 创建一个新光标。Pixmap 必须为 RGBA8888 格式,宽度和高度必须是大于 0 的 2 的幂(不必相等)且不小于某个最小尺寸(32x32 比较稳妥),alpha 透明度必须是单比特(即仅为 0x00 或 0xFF)。此函数返回一个 Cursor 对象,可通过调用 {@link #setCursor(Cursor)} 将其设置为系统光标。
     * @param pixmap the mouse cursor image as a {@link arc.graphics.Pixmap} 作为 {@link arc.graphics.Pixmap} 的鼠标光标图像
     * @param xHotspot the x location of the hotspot pixel within the cursor image (origin top-left corner) 热点像素在光标图像中的 x 位置(原点为左上角)
     * @param yHotspot the y location of the hotspot pixel within the cursor image (origin top-left corner) 热点像素在光标图像中的 y 位置(原点为左上角)
     * @return a cursor object that can be used by calling {@link #setCursor(Cursor)} or null if not supported 一个可通过调用 {@link #setCursor(Cursor)} 使用的光标对象,若不支持则为 null
     */
    public abstract Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot);

    /**
     * Creates a new cursor by scaling a pixmap and adding an outline.
     * <p>
     * 通过缩放 pixmap 并添加轮廓来创建新光标。
     * @param pixmap The base pixmap. Unscaled. 基准 pixmap。未缩放。
     * @param scaling The factor by which to scale the base pixmap. 缩放基准 pixmap 的倍数。
     * @param outlineColor The color of the cursor's outline. 光标轮廓的颜色。
     */
    public Cursor newCursor(Pixmap pixmap, int scaling, Color outlineColor, int outlineThickness){
        Pixmap out = Pixmaps.outline(pixmap, outlineColor, outlineThickness);
        Pixmap out2 = Pixmaps.scale(out, scaling);

        if(!Mathf.isPowerOfTwo(out2.width)){
            Pixmap old = out2;
            out2 = Pixmaps.resize(out2, Mathf.nextPowerOfTwo(out2.width), Mathf.nextPowerOfTwo(out2.width));
            old.dispose();
        }

        out.dispose();
        pixmap.dispose();

        Cursor cur = newCursor(out2, out2.width / 2, out2.height / 2);
        out2.dispose();
        return cur;
    }

    /**
     * Creates a new cursor by file name.
     * <p>
     * 通过文件名创建新光标。
     * @param filename the name of the cursor .png file, found in the internal file "cursors/{name}.png" 光标 .png 文件的名称,位于内部文件 "cursors/{name}.png" 中
     */
    public Cursor newCursor(String filename, int scale){
        if(scale == 1 || OS.isAndroid || OS.isIos) return newCursor(filename);
        Pixmap base = new Pixmap(Core.files.internal("cursors/" + filename + ".png"));
        Pixmap result = Pixmaps.scale(base, base.width * scale, base.height * scale);
        base.dispose();
        Cursor cur = newCursor(result, result.width /2, result.height /2);
        result.dispose();
        return cur;
    }

    /**
     * Creates a new cursor by file name.
     * <p>
     * 通过文件名创建新光标。
     * @param filename the name of the cursor .png file, found in the internal file "cursors/{name}.png" 光标 .png 文件的名称,位于内部文件 "cursors/{name}.png" 中
     */
    public Cursor newCursor(String filename){
        Pixmap p = new Pixmap(Core.files.internal("cursors/" + filename + ".png"));
        Cursor result = newCursor(p, p.width /2, p.height /2);
        p.dispose();
        return result;
    }

    /**
     * Creates a new cursor by file name.
     * <p>
     * 通过文件名创建新光标。
     * @param filename the name of the cursor .png file, found in the internal file "cursors/{name}.png" 光标 .png 文件的名称,位于内部文件 "cursors/{name}.png" 中
     */
    public Cursor newCursor(String filename, int scaling, Color outlineColor, int outlineScaling){
        return newCursor(new Pixmap(Core.files.internal("cursors/" + filename + ".png")), scaling, outlineColor, outlineScaling);
    }

    /**
     * Sets the cursor to the default value, e.g. {@link SystemCursor#arrow}.
     * 将光标设置为默认值,例如 {@link SystemCursor#arrow}。
     */
    public void restoreCursor(){
        cursor(SystemCursor.arrow);
    }

    /**
     * Sets the display cursor.
     * 设置显示的光标。
     */
    public void cursor(Cursor cursor){
        if(lastCursor == cursor) return;

        if(cursor instanceof SystemCursor){
            if(((SystemCursor)cursor).cursor != null){
                setCursor(((SystemCursor)cursor).cursor);
            }else{
                setSystemCursor((SystemCursor)cursor);
            }
        }else{
            setCursor(cursor);
        }

        lastCursor = cursor;
    }

    /**
     * Only viable on desktop and web. Browsers that support cursor:url() and support the png format (the
     * pixmap is converted to a data-url of type image/png) should also support custom cursors. Will set the mouse cursor image to
     * the image represented by the {@link Cursor}. It is recommended to call this function in the main render thread, and maximum one time per frame.
     * Internal use only!
     * <p>
     * 仅在桌面和 web 上有效。支持 cursor:url() 且支持 png 格式(pixmap 会被转换为 image/png 类型的 data-url)的浏览器也应支持自定义光标。会将鼠标光标图像设置为 {@link Cursor} 所表示的图像。建议在主渲染线程中调用此函数,且每帧最多调用一次。仅限内部使用!
     * @param cursor the mouse cursor as a {@link Cursor} 作为 {@link Cursor} 的鼠标光标
     */
    protected abstract void setCursor(Cursor cursor);

    /**Sets one of the predefined {@link SystemCursor}s.
     * Internal use only!
     * <p>
     * 设置一个预定义的 {@link SystemCursor}。仅限内部使用!
     */
    protected abstract void setSystemCursor(SystemCursor systemCursor);

    @Override
    public void dispose(){
        for(SystemCursor cursor : SystemCursor.values()){
            cursor.dispose();
        }
    }

    /**
     * Class describing the bits per pixel, depth buffer precision, stencil precision and number of MSAA samples.
     * 描述每像素位数、深度缓冲精度、模板精度以及 MSAA 采样数的类。
     */
    public static class BufferFormat{
        /* number of bits per color channel 每个颜色通道的位数 */
        public final int r, g, b, a;
        /* number of bits for depth and stencil buffer 深度和模板缓冲的位数 */
        public final int depth, stencil;
        /**
         * number of samples for multi-sample anti-aliasing (MSAA)
         * 多重采样抗锯齿(MSAA)的采样数
         */
        public final int samples;
        /**
         * whether coverage sampling anti-aliasing is used. in that case you have to clear the coverage buffer as well!
         * 是否使用覆盖采样抗锯齿。在这种情况下,你还必须清除覆盖缓冲区!
         */
        public final boolean coverageSampling;

        public BufferFormat(int r, int g, int b, int a, int depth, int stencil, int samples, boolean coverageSampling){
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.depth = depth;
            this.stencil = stencil;
            this.samples = samples;
            this.coverageSampling = coverageSampling;
        }

        public String toString(){
            return "r: " + r + ", g: " + g + ", b: " + b + ", a: " + a + ", depth: " + depth + ", stencil: " + stencil
            + ", num samples: " + samples + ", coverage sampling: " + coverageSampling;
        }
    }

    /**
     * <p>
     * Represents a mouse cursor. Create a cursor via
     * {@link Graphics#newCursor(Pixmap, int, int)}. To
     * set the cursor use {@link Graphics#setCursor(Cursor)}.
     * To use one of the system cursors, call Graphics#setSystemCursor
     * </p>
     * <p>
     * 表示一个鼠标光标。通过 {@link Graphics#newCursor(Pixmap, int, int)} 创建光标。要设置光标,使用 {@link Graphics#setCursor(Cursor)}。要使用系统光标之一,调用 Graphics#setSystemCursor
     **/
    public interface Cursor extends arc.util.Disposable{

        enum SystemCursor implements Cursor{
            arrow,
            ibeam,
            crosshair,
            hand,
            horizontalResize,
            verticalResize;

            /**
             * The override cursor to use when setting this cursor.
             * 设置此光标时要使用的覆盖光标。
             */
            @Nullable Cursor cursor;

            /**
             * Sets the alias for this cursor.
             * 设置此光标的别名。
             */
            public void set(Cursor cursor){
                this.cursor = cursor;
            }

            @Override
            public void dispose(){
                if(cursor != null && !(cursor instanceof SystemCursor)){
                    cursor.dispose();
                    cursor = null;
                }
            }
        }
    }
}
