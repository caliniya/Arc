package arc.backend.robovm;

import arc.func.*;
import arc.graphics.HdpiUtils.*;
import arc.util.*;
import com.badlogic.gdx.backends.iosrobovm.bindings.metalangle.*;
import org.robovm.apple.uikit.*;

public class IOSApplicationConfiguration{
    /**
     * whether to enable screen dimming.
     * 是否启用屏幕变暗。
     */
    public boolean preventScreenDimming = true;
    /**
     * whether or not portrait orientation is supported.
     * 是否支持竖屏方向。
     */
    public boolean orientationPortrait = true;
    /**
     * whether or not landscape orientation is supported.
     * 是否支持横屏方向。
     */
    public boolean orientationLandscape = true;

    /**
     * whether the status bar should be visible or not
     * 状态栏是否可见
     */
    public boolean statusBarVisible = false;

    public HdpiMode hdpiMode = HdpiMode.pixels;

    /**
     * the color format, RGB565 is the default
     * 颜色格式,默认为 RGB565
     */
    public MGLDrawableColorFormat colorFormat = MGLDrawableColorFormat.RGBA8888;

    /**
     * the depth buffer format, Format16 is default
     * 深度缓冲格式,默认为 Format16
     */
    public MGLDrawableDepthFormat depthFormat = MGLDrawableDepthFormat._16;

    /**
     * the stencil buffer format, None is default
     * 模板缓冲格式,默认为 None
     */
    public MGLDrawableStencilFormat stencilFormat = MGLDrawableStencilFormat.None;

    /**
     * the multisample format, None is default
     * 多重采样格式,默认为 None
     */
    public MGLDrawableMultisample multisample = MGLDrawableMultisample.None;

    /**
     * number of frames per second, 60 is default
     * 每秒帧数,默认为 60
     */
    public int preferredFramesPerSecond = 60;

    /**
     * handles any errors in the main loop.
     * 处理主循环中的任何错误。
     */
    @Nullable
    public Cons<Throwable> errorHandler;

    /**
     * whether to use the accelerometer, default true
     * 是否使用加速度计,默认 true
     */
    public boolean useAccelerometer = false;
    /**
     * the update interval to poll the accelerometer with, in seconds
     * 轮询加速度计的时间间隔(以秒为单位)
     */
    public float accelerometerUpdate = 0.05f;
    /**
     * whether or not the onScreenKeyboard should be closed on return key
     * 按回车键时是否应关闭屏幕键盘
     */
    public boolean keyboardCloseOnReturn = true;

    /**
     * whether the home indicator should be hidden or not
     * 是否隐藏主屏幕指示条(Home 指示器)
     */
    public boolean hideHomeIndicator = true;

    /**
     * Edges where app gestures must be fired over system gestures.
     * Prior to iOS 11, UIRectEdge.All was default behaviour if status bar hidden, see #5110
     * <p>
     * 必须在系统手势之上触发应用手势的屏幕边缘。在 iOS 11 之前,若状态栏隐藏,默认行为是 UIRectEdge.All,见 #5110
     **/
    public UIRectEdge screenEdgesDeferringSystemGestures = UIRectEdge.None;
}
