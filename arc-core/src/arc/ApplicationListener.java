package arc;

import arc.files.Fi;

/**
 * <p>
 * An <code>ApplicationListener</code> is called when the {@link Application} is created, resumed, rendering, paused or destroyed.
 * All methods are called in a thread that has the OpenGL context current. You can thus safely create and manipulate graphics
 * resources.
 * </p>
 *
 * <p>
 * The <code>ApplicationListener</code> interface follows the standard Android activity life-cycle and is emulated on the desktop
 * accordingly.
 * </p>
 * <p>
 * 当 {@link Application} 被创建、恢复、渲染、暂停或销毁时,会调用 <code>ApplicationListener</code>。
 * 所有方法都在持有 OpenGL 上下文的线程中调用,因此你可以安全地创建和操作图形资源。
 * </p>
 *
 * <p>
 * <code>ApplicationListener</code> 接口遵循标准的 Android activity 生命周期,并在桌面上进行相应模拟。
 * @author mzechner
 */
public interface ApplicationListener{
    /**
     * Called when the {@link Application} is first created.
     * Only gets called if the application is created before the listener is added.
     * <p>
     * 当 {@link Application} 首次创建时调用。
     * 仅当应用在此监听器被添加之前创建时才会调用。
     */
    default void init(){
    }

    /**
     * Called when the {@link Application} is resized. This can happen at any point during a non-paused state but will never happen
     * before a call to {@link #init()}.
     * <p>
     * 当 {@link Application} 被调整大小时调用。这可能发生在非暂停状态的任何时刻,但绝不会发生在 {@link #init()} 调用之前。
     * @param width the new width in pixels 新宽度(像素)
     * @param height the new height in pixels 新高度(像素)
     */
    default void resize(int width, int height){
    }

    /**
     * Called when the {@link Application} should update itself.
     * 当 {@link Application} 应当更新自身时调用。
     */
    default void update(){
    }

    /**
     * Called when the {@link Application} is paused, usually when it's not active or visible on screen. An Application is also
     * paused before it is destroyed.
     * <p>
     * 当 {@link Application} 被暂停时调用,通常是它未处于活动状态或不在屏幕上显示时。应用在销毁之前也会被暂停。
     */
    default void pause(){
    }

    /**
     * Called when the {@link Application} is resumed from a paused state, usually when it regains focus.
     * 当 {@link Application} 从暂停状态恢复时调用,通常是在它重新获得焦点时。
     */
    default void resume(){
    }

    /**
     * Called when the {@link Application} is destroyed. Preceded by a call to {@link #pause()}.
     * 当 {@link Application} 被销毁时调用。在此之前会先调用 {@link #pause()}。
     */
    default void dispose(){
    }

    /**
     * Called when the applications exits gracefully, either through `Core.app.exit()` or through a window closing.
     * Never called after a crash, unlike dispose().
     * <p>
     * 当应用正常退出时调用,无论是通过 `Core.app.exit()` 还是通过关闭窗口。
     * 与 dispose() 不同,崩溃后绝不会调用。
     * */
    default void exit(){

    }

    /**
     * Called when an external file is dropped into the window, e.g from the desktop.
     * <p>
     * 当外部文件被拖放进窗口时调用,例如从桌面拖入。
     */
    default void fileDropped(Fi file){
    }
}
