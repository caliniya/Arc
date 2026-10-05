package arc;

import arc.func.*;
import arc.input.*;
import arc.input.KeyBind.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;

/**
 * <p>
 * Interface to the input facilities. This allows polling the state of the keyboard, the touch screen and the accelerometer. On
 * some backends (desktop) the touch screen is replaced by mouse input. The accelerometer is of course not available on
 * all backends.
 * </p>
 *
 * <p>
 * The class also offers methods to use (and test for the presence of) other input systems like vibration, compass, on-screen
 * keyboards, and cursor capture. Support for simple input dialogs is also provided.
 * </p>
 * <p>
 * 输入设施的接口。它允许轮询键盘、触摸屏和加速度计的状态。在某些后端(桌面)上,触摸屏由鼠标输入代替。当然,并非所有后端都支持加速度计。
 * </p>
 *
 * <p>
 * 该类还提供了使用(并检测是否存在)其他输入系统的方法,如振动、指南针、屏幕键盘和光标捕获。同时也支持简单的输入对话框。
 * @author mzechner
 */
public abstract class Input{
    /**
     * The default input device (keyboard)
     * 默认输入设备(键盘)
     */
    protected KeyboardDevice keyboard = new KeyboardDevice();
    /**
     * An input multiplexer to handle events.
     * 用于处理事件的输入多路复用器。
     */
    protected InputMultiplexer inputMultiplexer = new InputMultiplexer(keyboard);
    /**
     * List of caught keys for Android.
     * Android 上被捕获的按键列表。
     */
    protected IntSet caughtKeys = new IntSet();
    /**
     * Return Vec2 value for various functions.
     * 供多个方法返回用的 Vec2 值。
     */
    protected Vec2 mouseReturn = new Vec2();
    /**
     * Whether to use keyboard controls on Android.
     * 是否在 Android 上使用键盘控制。
     */
    protected boolean useKeyboard;

    /**
     * Returns the unprojected mouse position (screen -> world).
     * 返回反投影后的鼠标位置(屏幕 -> 世界)。
     */
    public Vec2 mouseWorld(float x, float y){
        return Core.camera.unproject(mouseReturn.set(x, y));
    }

    /**
     * Returns the projected mouse position (world -> screen).
     * 返回投影后的鼠标位置(世界 -> 屏幕)。
     */
    public Vec2 mouseScreen(float x, float y){
        return Core.camera.project(mouseReturn.set(x, y));
    }

    /**
     * @return the unprojected mouse position in the world. 世界中反投影后的鼠标位置。
     */
    public float mouseWorldX(){
        return Core.camera.unproject(mouse()).x;
    }

    /**
     * @return the unprojected mouse position in the world. 世界中反投影后的鼠标位置。
     */
    public float mouseWorldY(){
        return Core.camera.unproject(mouse()).y;
    }

    /**
     * Returns the unprojected mouse position in the world.
     * 返回世界中反投影后的鼠标位置。
     */
    public Vec2 mouseWorld(){
        return Core.camera.unproject(mouse());
    }

    /**
     * Returns the mouse position as a Vec2.
     * 以 Vec2 形式返回鼠标位置。
     */
    public Vec2 mouse(){
        return mouseReturn.set(mouseX(), mouseY());
    }

    public void setUseKeyboard(boolean useKeyboard){
        this.useKeyboard = useKeyboard;
    }

    /**
     * @return whether the keyboard should be preferred for mobile devices - used in text fields. 在移动设备上是否应优先使用键盘 - 用于文本字段。
     */
    public boolean useKeyboard(){
        return useKeyboard;
    }

    /**
     * @return The x coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first
     * pointer in screen coordinates. The screen origin is the top left corner. 触摸屏设备上最后一次触摸的 x 坐标;桌面上的第一个指针则为当前鼠标位置的 x 坐标,均为屏幕坐标。屏幕原点在左上角。
     */
    public abstract int mouseX();

    /**
     * Returns the x coordinate in screen coordinates of the given pointer. Pointers are indexed from 0 to n. The pointer id
     * identifies the order in which the fingers went down on the screen, e.g. 0 is the first finger, 1 is the second and so on.
     * When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on
     * the touch screen the first free index will be used.
     * <p>
     * 返回给定指针在屏幕坐标系中的 x 坐标。指针从 0 到 n 编号。指针 id 表示手指按下的顺序,例如 0 是第一根手指,1 是第二根,依此类推。当两根手指按下且第一根抬起后,第二根会保留其索引。如果另一根手指放到触摸屏上,将使用第一个空闲索引。
     * @param pointer the pointer id. 指针 id。
     * @return the x coordinate x 坐标
     */
    public abstract int mouseX(int pointer);

    /**
     * @return the different between the current pointer location and the last pointer location on the x-axis. 当前指针位置与上一次指针位置在 x 轴上的差值。
     */
    public abstract int deltaX();

    /**
     * @return the different between the current pointer location and the last pointer location on the x-axis. 当前指针位置与上一次指针位置在 x 轴上的差值。
     */
    public abstract int deltaX(int pointer);

    /**
     * @return The y coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first
     * pointer in screen coordinates. The screen origin is the bottom left corner. 触摸屏设备上最后一次触摸的 y 坐标;桌面上的第一个指针则为当前鼠标位置的 y 坐标,均为屏幕坐标。屏幕原点在左下角。
     */
    public abstract int mouseY();

    /**
     * Returns the y coordinate in screen coordinates of the given pointer. Pointers are indexed from 0 to n. The pointer id
     * identifies the order in which the fingers went down on the screen, e.g. 0 is the first finger, 1 is the second and so on.
     * When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on
     * the touch screen the first free index will be used.
     * <p>
     * 返回给定指针在屏幕坐标系中的 y 坐标。指针从 0 到 n 编号。指针 id 表示手指按下的顺序,例如 0 是第一根手指,1 是第二根,依此类推。当两根手指按下且第一根抬起后,第二根会保留其索引。如果另一根手指放到触摸屏上,将使用第一个空闲索引。
     * @param pointer the pointer id. 指针 id。
     * @return the y coordinate y 坐标
     */
    public abstract int mouseY(int pointer);

    /**
     * @return the different between the current pointer location and the last pointer location on the y-axis. 当前指针位置与上一次指针位置在 y 轴上的差值。
     */
    public abstract int deltaY();

    /**
     * @return the different between the current pointer location and the last pointer location on the y-axis. 当前指针位置与上一次指针位置在 y 轴上的差值。
     */
    public abstract int deltaY(int pointer);

    /**
     * @return whether the screen is currently touched. 当前屏幕是否被触摸。
     */
    public abstract boolean isTouched();

    /**
     * @return whether a new touch down event just occurred. 刚刚是否发生了新的触摸按下事件。
     */
    public abstract boolean justTouched();

    public int getTouches(){
        int sum = 0;
        for(int i = 0; i < 10; i++){
            if(isTouched(i)) sum ++;
        }
        return sum;
    }

    /**
     * Whether the screen is currently touched by the pointer with the given index. Pointers are indexed from 0 to n. The pointer
     * id identifies the order in which the fingers went down on the screen, e.g. 0 is the first finger, 1 is the second and so on.
     * When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on
     * the touch screen the first free index will be used.
     * <p>
     * 屏幕当前是否被具有给定索引的指针触摸。指针从 0 到 n 编号。指针 id 表示手指按下的顺序,例如 0 是第一根手指,1 是第二根,依此类推。当两根手指按下且第一根抬起后,第二根会保留其索引。如果另一根手指放到触摸屏上,将使用第一个空闲索引。
     * @param pointer the pointer 指针
     * @return whether the screen is touched by the pointer 该指针是否正在触摸屏幕
     */
    public abstract boolean isTouched(int pointer);

    /**
     * @return the pressure of the first pointer 第一个指针的压力
     */
    public float getPressure(){
        return getPressure(0);
    }

    /**
     * Returns the pressure of the given pointer, where 0 is untouched. On Android it should be
     * up to 1.0, but it can go above that slightly and its not consistent between devices. On iOS 1.0 is the normal touch
     * and significantly more of hard touch. Check relevant manufacturer documentation for details.
     * Check availability with {@link Input#isPeripheralAvailable(Peripheral)}. If not supported, returns 1.0 when touched.
     * <p>
     * 返回给定指针的压力,0 表示未触摸。在 Android 上最高应为 1.0,但可能略微超过,且不同设备之间并不一致。在 iOS 上,1.0 为正常触摸,明显更大则为用力触摸。详情请查阅相关厂商文档。可通过 {@link Input#isPeripheralAvailable(Peripheral)} 检查其可用性。若不支持,触摸时返回 1.0。
     * @param pointer the pointer id. 指针 id。
     * @return the pressure 压力
     */
    public float getPressure(int pointer){
        return isTouched(pointer) ? 1f : 0f;
    }

    /**
     * Returns whether one of the two shift keys is currently pressed.
     * 返回两个 shift 键之一当前是否被按下。
     */
    public boolean shift(){
        return keyDown(KeyCode.shiftLeft) || keyDown(KeyCode.shiftRight);
    }

    /**
     * Returns whether one of the two control keys is currently pressed - or, on Macs, the cmd key.
     * 返回两个 control 键之一当前是否被按下 - 在 Mac 上则是 cmd 键。
     */
    public boolean ctrl(){
        return OS.isMac ? keyDown(KeyCode.sym) : keyDown(KeyCode.controlLeft) || keyDown(KeyCode.controlRight);
    }

    /**
     * Returns whether one of the two alt keys is pressed.
     * 返回两个 alt 键之一是否被按下。
     */
    public boolean alt(){
        return keyDown(KeyCode.altLeft) || keyDown(KeyCode.altRight);
    }

    /**
     * Returns whether the key is pressed.
     * 返回该键当前是否被按下。
     */
    public boolean keyDown(KeyCode key){
        return keyboard.isPressed(key);
    }

    /**
     * Returns whether the key has just been pressed.
     * 返回该键是否刚被按下。
     */
    public boolean keyTap(KeyCode key){
        return keyboard.isTapped(key);
    }

    /**
     * Returns whether the key has just been released.
     * 返回该键是否刚被释放。
     */
    public boolean keyRelease(KeyCode key){
        return keyboard.isReleased(key);
    }

    /**
     * Returns the [-1, 1] axis value of a key.
     * 返回该键的 [-1, 1] 轴值。
     */
    public float axis(KeyCode key){
        return keyboard.getAxis(key);
    }

    /**
     * Returns whether the keybind is pressed.
     * 返回该按键绑定当前是否被按下。
     */
    public boolean keyDown(KeyBind key){
        return key.value.key != null && keyboard.isPressed(key.value.key);
    }

    /**
     * Returns whether the key has just been pressed.
     * 返回该键是否刚被按下。
     */
    public boolean keyTap(KeyBind key){
        return key.value.key != null && keyboard.isTapped(key.value.key);
    }

    /**
     * Returns whether the key has just been released.
     * 返回该键是否刚被释放。
     */
    public boolean keyRelease(KeyBind key){
        return key.value.key != null && keyboard.isReleased(key.value.key);
    }

    /**
     * Returns the [-1, 1] axis value of a key.
     * 返回该键的 [-1, 1] 轴值。
     */
    public float axis(KeyBind key){
        Axis axis = key.value;
        if(axis.key != null){
            return keyboard.getAxis(axis.key);
        }else{
            return keyboard.isPressed(axis.min) && keyboard.isPressed(axis.max) ? 0 :
                    keyboard.isPressed(axis.min) ? -1 : keyboard.isPressed(axis.max) ? 1 : 0;
        }
    }

    /** Returns the [-1, 1] axis value of a key.
     * In the case of keyboard-based axes, this will only return a value if one of the axes was just pressed.
     * <p>
     * 返回该键的 [-1, 1] 轴值。对于基于键盘的轴,只有当其中一个轴刚被按下时才会返回值。
     */
    public float axisTap(KeyBind key){
        Axis axis = key.value;
        if(axis.key != null){
            return keyboard.getAxis(axis.key);
        }else{
            return keyboard.isTapped(axis.min) ? -1 : keyboard.isTapped(axis.max) ? 1 : 0;
        }
    }

    /**
     * System dependent method to input a string of text. A dialog box will be created with the given title and the given text as a
     * message for the user. Once the dialog has been closed the consumer be called on the rendering thread.
     * <p>
     * 输入一段文本的系统相关方法。会创建一个对话框,以给定的标题和文本作为显示给用户的消息。对话框关闭后,将在渲染线程上调用该 consumer。
     */
    public void getTextInput(TextInput input){
    }

    /**
     * @return on mobile, whether text input is currently being fetched. Not implemented on other platforms. 在移动设备上,当前是否正在获取文本输入。其他平台未实现。
     */
    public boolean isShowingTextInput(){
        return false;
    }

    /**
     * Sets the on-screen keyboard visible if available. Only applicable on mobile.
     * <p>
     * 如果可用,设置屏幕键盘的可见性。仅适用于移动设备。
     * @param visible visible or not 是否可见
     */
    public void setOnscreenKeyboardVisible(boolean visible){
    }

    /**
     * Vibrates for the given amount of time. Note that you'll need the permission
     * <code> <uses-permission android:name="android.permission.VIBRATE" /></code> in your manifest file in order for this to work.
     * <p>
     * 振动给定的时间长度。注意,你需要在 manifest 文件中添加权限 <code> <uses-permission android:name="android.permission.VIBRATE" /></code> 才能生效。
     * @param milliseconds the number of milliseconds to vibrate. 振动的毫秒数。
     */
    public void vibrate(int milliseconds){
    }

    /**
     * Vibrate with a given pattern. Pass in an array of ints that are the times at which to turn on or off the vibrator. The first
     * one is how long to wait before turning it on, and then after that it alternates. If you want to repeat, pass the index into
     * the pattern at which to start the repeat.
     * <p>
     * 按给定的模式振动。传入一个 int 数组,表示打开或关闭振动器的时刻。第一个值是开启前等待的时长,之后交替进行。如果想重复,传入模式中开始重复的索引。
     * @param pattern an array of longs of times to turn the vibrator on or off. 控制振动器开/关时刻的 long 数组。
     * @param repeat the index into pattern at which to repeat, or -1 if you don't want to repeat. 模式中开始重复的索引,不需要重复则为 -1。
     */
    public void vibrate(long[] pattern, int repeat){
    }

    /**
     * Stops the vibrator
     * 停止振动器
     */
    public void cancelVibrate(){
    }

    /**
     * @return The acceleration force in m/s^2 applied to the device, including the force of gravity 施加于设备的加速度(单位 m/s^2),包括重力
     */
    public Vec3 getAccelerometer(){
        return Vec3.Zero;
    }

    /**
     * @return The rate of rotation in rad/s. 旋转速率(单位为弧度/秒)。
     */
    public Vec3 getGyroscope(){
        return Vec3.Zero;
    }

    /**
     * @return the device's orientation in degrees in the format (pitch, roll, azimuth) corresponding to x,y,z. 设备方向(角度),格式为 (pitch, roll, azimuth),对应 x,y,z。
     */
    public Vec3 getOrientation(){
        return Vec3.Zero;
    }

    /**
     * Returns the rotation matrix describing the devices rotation as per <a href=
     * "http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[], float[])"
     * >SensorManager#getRotationMatrix(float[], float[], float[], float[])</a>. Does not manipulate the matrix if the platform
     * does not have an accelerometer.
     * <p>
     * 返回描述设备旋转的旋转矩阵,遵循上述 SensorManager#getRotationMatrix(float[], float[], float[], float[]) 文档的约定。若平台没有加速度计,则不会修改该矩阵。
     */
    public void getRotationMatrix(float[] matrix){
    }

    /**
     * @return the time of the event currently reported to the {@link InputProcessor}. 当前上报给 {@link InputProcessor} 的事件的时间。
     */
    public abstract long getCurrentEventTime();

    /**
     * Sets whether the specified button on Android should be caught. This will prevent the app from processing the key. Will have no effect
     * on the desktop.
     * <p>
     * 设置 Android 上指定的按键是否应被捕获。这将阻止应用处理该按键。在桌面上无效。
     * @param c whether to catch the button 是否捕获该按键
     */
    public void setCatch(KeyCode code, boolean c){
        if(c){
            caughtKeys.add(code.ordinal());
        }else{
            caughtKeys.remove(code.ordinal());
        }
    }

    /**
     * @return whether the back button is currently being caught 返回键当前是否被捕获
     */
    public boolean isCatch(KeyCode code){
        return caughtKeys.contains(code.ordinal());
    }

    /**
     * Adds a {@link InputProcessor} that will receive all touch and key input events. It will be called before the
     * {@link ApplicationListener#update()} method each frame.
     * <p>
     * 添加一个 {@link InputProcessor},它将接收所有触摸和按键输入事件。每帧会在 {@link ApplicationListener#update()} 方法之前被调用。
     * @param processor the InputProcessor 该 InputProcessor
     * */
    public void addProcessor(InputProcessor processor){
        inputMultiplexer.addProcessor(processor);
    }

    /**
     * Removes a {@link InputProcessor} from the chain.
     * 从链中移除一个 {@link InputProcessor}。
     */
    public void removeProcessor(InputProcessor processor){
        inputMultiplexer.removeProcessor(processor);
    }

    /**
     * @return the currently set {@link InputProcessor} or null. 当前设置的 {@link InputProcessor},或 null。
     */
    public Ar<InputProcessor> getInputProcessors(){
        return inputMultiplexer.getProcessors();
    }

    public InputMultiplexer getInputMultiplexer(){
        return inputMultiplexer;
    }

    /**
     * Returns the default input device (keyboard).
     * 返回默认输入设备(键盘)。
     */
    public KeyboardDevice getKeyboard(){
        return keyboard;
    }

    /**
     * Queries whether a {@link Peripheral} is currently available. In case of Android and the {@link Peripheral#hardwareKeyboard}
     * this returns the whether the keyboard is currently slid out or not.
     * <p>
     * 查询某个 {@link Peripheral} 当前是否可用。对于 Android 和 {@link Peripheral#hardwareKeyboard},返回的是键盘当前是否滑出。
     * @param peripheral the {@link Peripheral} 该 {@link Peripheral}
     * @return whether the peripheral is available or not. 该外设是否可用。
     */
    public boolean isPeripheralAvailable(Peripheral peripheral){
        return peripheral == Peripheral.hardwareKeyboard;
    }

    /**
     * @return the rotation of the device with respect to its native orientation. 设备相对于其原生方向的旋转角度。
     */
    public int getRotation(){
        return 0;
    }

    /**
     * @return the native orientation of the device. 设备的原生方向。
     */
    public Orientation getNativeOrientation(){
        return Orientation.landscape;
    }

    /**
     * Internal method for looking up a key's name based on keyboard layout. Internal use only - use {@link KeyCode#getName()} instead.
     * 根据键盘布局查找按键名称的内部方法。仅限内部使用 - 请改用 {@link KeyCode#getName()}。
     */
    public String getKeyName(KeyCode code){
        return code.value;
    }

    public enum Orientation{
        landscape, portrait
    }

    /**
     * Enumeration of potentially available peripherals. Use with {@link Input#isPeripheralAvailable(Peripheral)}.
     * 可能可用的外设枚举。与 {@link Input#isPeripheralAvailable(Peripheral)} 配合使用。
     */
    public enum Peripheral{
        hardwareKeyboard, onscreenKeyboard, multitouchScreen, accelerometer, compass, vibrator, gyroscope, rotationVector, pressure
    }

    /**
     * Parameters for text input.
     * 文本输入的参数。
     */
    public static class TextInput{
        public boolean multiline = false;
        public boolean allowEmpty = true;
        public String title = "";
        public String text = "";
        public String message = "";
        public boolean numeric;
        public Cons<String> accepted = s -> { };
        public Runnable canceled = () -> { };
        public int maxLength = -1;
    }
}
