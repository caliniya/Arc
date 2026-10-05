package arc.backend.android;

import android.hardware.*;
import arc.*;

/**
 * Class defining the configuration of an {@link AndroidApplication}. Allows you to disable the use of the accelerometer to save
 * battery among other things.
 * <p>
 * 定义 {@link AndroidApplication} 配置的类。除其他功能外,允许你禁用加速度计以节省电量。
 * @author mzechner
 */
public class AndroidApplicationConfiguration{
    /**
     * number of bits per color channel
     * 每个颜色通道的位数
     */
    public int r = 8, g = 8, b = 8, a = 0;

    /**
     * number of bits for depth and stencil buffer
     * 深度和模板缓冲的位数
     */
    public int depth = 16, stencil = 0;

    /**
     * number of samples for CSAA/MSAA, 2 is a good value
     * CSAA/MSAA 的采样数,2 是一个不错的值
     */
    public int numSamples = 0;

    /**
     * whether to use the accelerometer. default: false
     * 是否使用加速度计。默认:false
     */
    public boolean useAccelerometer = false;

    /**
     * whether to use the gyroscope. default: false
     * 是否使用陀螺仪。默认:false
     */
    public boolean useGyroscope = false;

    /**
     * Whether to use the compass. The compass enables {@link Input#getRotationMatrix(float[])}, if {@link #useAccelerometer} is also true.
     * <p>
     * If {@link #useRotationVectorSensor} is true and the rotation vector sensor is available, the compass will not be used.
     * <p>
     * Default: false
     * <p>
     * 是否使用罗盘。如果 {@link #useAccelerometer} 也为 true,罗盘将启用 {@link Input#getRotationMatrix(float[])}。<p> 如果 {@link #useRotationVectorSensor} 为 true 且旋转矢量传感器可用,则不会使用罗盘。<p> 默认:false
     **/
    public boolean useCompass = false;

    /**
     * Whether to use Android's rotation vector software sensor, which provides cleaner data than that of {@link #useCompass} for
     * {@link Input#getRotationMatrix(float[])}
     * The rotation vector sensor uses a combination of physical sensors, and it pre-filters and smoothes the data. If true,
     * {@link #useAccelerometer} is not required to enable rotation data.
     * <p>
     * If true and the rotation vector sensor is available, the compass will not be used, regardless of {@link #useCompass}.
     * <p>
     * Default: false
     * <p>
     * 是否使用 Android 的旋转矢量软件传感器。对于 {@link Input#getRotationMatrix(float[])},它提供的数据比 {@link #useCompass} 更干净。旋转矢量传感器组合了多个物理传感器,并对数据进行预过滤和平滑。如果为 true,则无需 {@link #useAccelerometer} 即可获得旋转数据。<p> 如果为 true 且旋转矢量传感器可用,则无论 {@link #useCompass} 如何设置,都不会使用罗盘。<p> 默认:false
     */
    public boolean useRotationVectorSensor = false;

    /**
     * The requested sensor sampling rate in microseconds or one of the {@code SENSOR_DELAY_*} constants in {@link SensorManager}.
     * <p>
     * Default: {@link SensorManager#SENSOR_DELAY_GAME} (20 ms updates).
     * <p>
     * {@link SensorManager} 中的传感器请求采样率(微秒)或 {@code SENSOR_DELAY_*} 常量之一。<p> 默认:{@link SensorManager#SENSOR_DELAY_GAME}(20 ms 更新一次)。
     */
    public int sensorDelay = SensorManager.SENSOR_DELAY_GAME;

    /**
     * whether to keep the screen on and at full brightness or not while running the application. default: false. Uses FLAG_KEEP_SCREEN_ON under the hood.
     * 应用程序运行时是否保持屏幕常亮且全亮度。默认:false。底层使用 FLAG_KEEP_SCREEN_ON。
     */
    public boolean useWakelock = false;

    /**
     * hide status bar buttons on Android 4.x and higher (API 14+). default: true
     * <p>
     * 在 Android 4.x 及更高版本(API 14+)上隐藏状态栏按钮。默认:true
     **/
    public boolean hideStatusBar = true;

    /**
     * whether to disable Android audio support. default: false
     * 是否禁用 Android 音频支持。默认:false
     */
    public boolean disableAudio = false;

    /**
     * set this to true to enable Android 4.4 KitKat's 'Immersive mode'
     * 设为 true 以启用 Android 4.4 KitKat 的"沉浸模式"
     */
    public boolean useImmersiveMode = true;
}
