package arc;

import arc.graphics.*;
import arc.struct.*;
import arc.util.*;

import java.net.*;

public interface Application extends Disposable{

    /**
     * Returns a list of all the application listeners used.
     * 返回所有正在使用的应用监听器的列表。
     */
    Ar<ApplicationListener> getListeners();

    /**
     * Adds a new application listener.
     * 添加一个新的应用监听器。
     */
    default void addListener(ApplicationListener listener){
        synchronized(getListeners()){
            getListeners().add(listener);
        }
    }

    /**
     * Removes an application listener.
     * 移除一个应用监听器。
     */
    default void removeListener(ApplicationListener listener){
        post(() -> {
            synchronized(getListeners()){
                getListeners().remove(listener);
            }
        });
    }

    /**
     * Call this before update() in each backend.
     * 在每个后端中,于 update() 之前调用此方法。
     */
    default void defaultUpdate(){
        Core.settings.autosave();
        Time.updateGlobal();
    }

    /**
     * @return what {@link ApplicationType} this application has, e.g. Android or Desktop 此应用所属的 {@link ApplicationType} 类型,例如 Android 或桌面端
     */
    ApplicationType getType();

    default boolean isDesktop(){
        return getType() == ApplicationType.desktop;
    }

    default boolean isHeadless(){
        return getType() == ApplicationType.headless;
    }

    default boolean isAndroid(){
        return getType() == ApplicationType.android;
    }

    default boolean isIOS(){
        return getType() == ApplicationType.iOS;
    }

    default boolean isMobile(){
        return isAndroid() || isIOS();
    }

    default boolean isWeb(){
        return getType() == ApplicationType.web;
    }

    /**
     * @return the Android API level on Android, the major OS version on iOS (5, 6, 7, ..), or 0 on the desktop. 在 Android 上为 Android API 级别,在 iOS 上为大版本号(5、6、7 等),在桌面上为 0。
     */
    default int getVersion(){
        return 0;
    }

    /**
     * @return the Java heap memory use in bytes. Java 堆内存使用量(字节)。
     */
    default long getJavaHeap(){
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * @return the Native heap memory use in bytes. Only valid on Android. 本地堆内存使用量(字节)。仅在 Android 上有效。
     */
    default long getNativeHeap(){
        return 0;
    }

    /**
     * @return the main graphics thread, upon which all ApplicationListener methods are called. May return null if not initialized. 主图形线程,所有 ApplicationListener 方法都在该线程上调用。若未初始化,可能返回 null。
     */
    default @Nullable Thread getMainThread(){
        return null;
    }

    /**
     * @return whether the currently executing thread is the main thread.
     * If the main thread is not initialized, returns true by default. This is used for error checking purposes. 当前执行线程是否为主线程。若主线程未初始化,默认返回 true。这用于错误检查。
     * */
    default boolean isOnMainThread(){
        Thread thread = getMainThread();
        return thread == null || Thread.currentThread() == thread;
    }

    @Nullable String getClipboardText();

    void setClipboardText(String text);

    default void setClipboardImage(Pixmap pixmap){
        //no-op
        // 空操作
    }

    /** Open a folder in the system's file browser.
     * <p>
     * 在系统的文件浏览器中打开一个文件夹。
     * @return whether this operation was successful. 此操作是否成功。 */
    default boolean openFolder(String file){
        return false;
    }

    /**
     * Launches the default browser to display a URI. If the default browser is not able to handle the specified URI, the
     * application registered for handling URIs of the specified type is invoked. The application is determined from the protocol
     * and path of the URI. A best effort is made to open the given URI; however, since external applications are involved, no guarantee
     * can be made as to whether the URI was actually opened. If it is known that the URI was not opened, false will be returned;
     * otherwise, true will be returned.
     * <p>
     * 启动默认浏览器来显示一个 URI。如果默认浏览器无法处理指定的 URI,则会调用已注册用于处理该类型 URI 的应用。
     * 具体调用哪个应用由 URI 的协议和路径决定。系统会尽最大努力打开给定的 URI;但由于涉及外部应用,
     * 无法保证 URI 确实被打开。如果可以确定 URI 未被打开,则返回 false;否则返回 true。
     * @param URI the URI to be opened. 要打开的 URI。
     * @return false if it is known the uri was not opened, true otherwise. 如果可以确定 URI 未被打开则返回 false,否则返回 true。
     */
    default boolean openURI(String URI){
        return false;
    }

    default void getDnsServers(Ar<InetSocketAddress> out){}

    /**
     * Posts a runnable on the main loop thread.
     * 在主循环线程上投递一个 runnable。
     */
    void post(Runnable runnable);

    /**
     * Schedule an exit from the application. On android, this will cause a call to pause() and dispose() some time in the future,
     * it will not immediately finish your application.
     * On iOS this should be avoided in production as it breaks Apples guidelines.
     * <p>
     * 安排退出应用。在 Android 上,这会在未来的某个时刻引发对 pause() 和 dispose() 的调用,
     * 不会立即结束你的应用。
     * 在 iOS 上,生产环境中应避免使用,因为它违反了 Apple 的规范。
     */
    void exit();

    /**
     * Disposes of core resources.
     * 释放核心资源。
     */
    @Override
    default void dispose(){
        //flush any changes to settings upon dispose
        // 销毁时将所有更改刷入设置
        if(Core.settings != null){
            Core.settings.autosave();
        }

        if(Core.audio != null){
            Core.audio.dispose();
        }
    }

    /**
     * Enumeration of possible {@link Application} types
     * {@link Application} 可能类型的枚举
     */
    enum ApplicationType{
        android, desktop, headless, web, iOS
    }
}
