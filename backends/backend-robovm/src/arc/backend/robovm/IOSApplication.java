package arc.backend.robovm;

import org.robovm.apple.coregraphics.*;
import org.robovm.apple.dispatch.*;
import static org.robovm.apple.foundation.NSPathUtilities.*;
import org.robovm.apple.foundation.*;
import static org.robovm.apple.foundation.NSPathUtilities.*;
import org.robovm.apple.uikit.*;
import org.robovm.rt.bro.*;

import arc.*;
import arc.audio.*;
import arc.backend.robovm.custom.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.struct.*;
import arc.util.*;

public class IOSApplication implements Application {

    UIApplication uiApp;
    UIWindowScene uiWindowScene;
    UIWindow uiWindow;
    IOSViewControllerListener viewControllerListener;
    IOSApplicationConfiguration config;
    IOSGraphics graphics;
    IOSInput input;
    Thread mainThread;
    @Nullable
    IOSDevice device;
    float pixelsPerPoint;

    private IOSScreenBounds lastScreenBounds = null;

    final Ar<ApplicationListener> listeners = new Ar<>();
    final Ar<Runnable> runnables = new Ar<>(), executedRunnables = new Ar<>();

    public IOSApplication(ApplicationListener listener, IOSApplicationConfiguration config) {
        addListener(listener);
        this.config = config;
    }

    final boolean didFinishLaunching(UIApplication uiApp, UIApplicationLaunchOptions options) {
        Core.app = this;
        this.uiApp = uiApp;

        // enable or disable screen dimming
        // 启用或禁用屏幕变暗
        UIApplication.getSharedApplication().setIdleTimerDisabled(config.preventScreenDimming);

        Log.info("[IOSApplication] iOS version: " + UIDevice.getCurrentDevice().getSystemVersion());
        Log.info("[IOSApplication] Running in " + (Bro.IS_64BIT ? "64-bit" : "32-bit") + " mode");

        this.input = new IOSInput(this);
        Core.audio = new Audio();
        Core.settings = new Settings();
        Core.files = new IOSFiles();
        Core.input = this.input;

        device = IOSDevice.getDevice(HWMachine.getMachineString());

        Log.info("[IOSApplication] created");
        return true;
    }

    /**
     * called once a UIWindowScene has connected; this is where graphics, window
     * and listeners are set up 在 UIWindowScene 连接后调用;图形、窗口和监听器都在这里设置
     */
    final void handleSceneConnection(UIWindowScene scene) {
        this.uiWindowScene = scene;
        this.uiWindow = new UIWindow(scene);
        this.uiWindow.makeKeyAndVisible();
        ((UIWindowSceneDelegate) scene.getDelegate()).setWindow(uiWindow);

        pixelsPerPoint = (float) uiWindowScene.getScreen().getNativeScale();

        this.graphics = new IOSGraphics(this, config, input);
        Core.graphics = this.graphics;

        this.uiWindow.setRootViewController(this.graphics.viewController);
        this.input.setupPeripherals();
        this.graphics.updateSafeInsets();
        // Trigger first render, special case that is caught and returned
        // 触发首次渲染,这是会被捕获并返回的特殊情况
        this.graphics.view.display();
        for (ApplicationListener list : listeners) {
            list.init();
        }
        for (ApplicationListener list : listeners) {
            list.resize(graphics.getWidth(), graphics.getHeight());
        }
        // make sure the OpenGL view has contents before displaying it
        // 确保 OpenGL 视图在显示前已有内容
        this.graphics.view.display();
    }

    /**
     * Return the UI view controller of IOSApplication
     * <p>
     * 返回 IOSApplication 的 UI 视图控制器
     *
     * @return the view controller of IOSApplication IOSApplication 的视图控制器
     */
    public UIViewController getUIViewController() {
        return graphics.viewController;
    }

    /**
     * Return the UI Window of IOSApplication
     * <p>
     * 返回 IOSApplication 的 UI 窗口
     *
     * @return the window 窗口
     */
    public UIWindow getUIWindow() {
        return uiWindow;
    }

    /**
     * GL View spans whole screen, that is, even under the status bar. iOS can
     * also rotate the screen, which is not handled consistently over iOS
     * versions. This method returns, in pixels, rectangle in which Arc draws.
     * <p>
     * GL 视图覆盖整个屏幕,即包括状态栏下方。iOS 还会旋转屏幕,而各 iOS 版本对此处理不一致。此方法返回 Arc
     * 绘制的矩形区域(以像素为单位)。
     *
     * @return dimensions of space we draw to, adjusted for device orientation
     * 绘制区域的大小,已根据设备方向调整
     */
    protected IOSScreenBounds computeBounds() {
        CGRect screenBounds = uiWindow.getBounds();
        double statusBarHeight = 0.0;
        UIStatusBarManager uiStatusBarManager = uiWindowScene.getStatusBarManager();
        if (uiStatusBarManager != null) {
            statusBarHeight = uiStatusBarManager.getStatusBarFrame().getHeight();
        }
        double screenWidth = screenBounds.getWidth();
        double screenHeight = screenBounds.getHeight();
        if (statusBarHeight != 0.0) {
            Log.debug("IOSApplication", "Status bar is visible (height = " + statusBarHeight + ")");
            screenHeight -= statusBarHeight;
        } else {
            Log.debug("IOSApplication", "Status bar is not visible");
        }
        int offsetX = 0;
        int offsetY = (int) Math.round(statusBarHeight);
        int width = (int) Math.round(screenWidth);
        int height = (int) Math.round(screenHeight);
        int backBufferWidth = (int) Math.round(screenWidth * pixelsPerPoint);
        int backBufferHeight = (int) Math.round(screenHeight * pixelsPerPoint);
        Log.debug("IOSApplication", "Computed bounds are x=" + offsetX + " y=" + offsetY + " w=" + width + " h=" + height + " bbW= "
                + backBufferWidth + " bbH= " + backBufferHeight);
        return lastScreenBounds = new IOSScreenBounds(offsetX, offsetY, width, height, backBufferWidth, backBufferHeight);
    }

    /**
     * @return area of screen in UIKit points on which Arc draws, with 0,0 being
     * upper left corner Arc 绘制的屏幕区域(以 UIKit 点为单位),左上角为 0,0
     */
    public IOSScreenBounds getScreenBounds() {
        return lastScreenBounds == null ? computeBounds() : lastScreenBounds;
    }

    /**
     * Returns device ppi using a best guess approach when device is unknown.
     * Overwrite to customize strategy. 设备未知时,以最佳猜测的方式返回设备 ppi。可重写此方法以自定义策略。
     */
    protected int guessUnknownPpi() {
        return UIDevice.getCurrentDevice().getUserInterfaceIdiom() == UIUserInterfaceIdiom.Pad
                ? 132 * (int) pixelsPerPoint : 164 * (int) pixelsPerPoint;
    }

    final void didBecomeActive(UIScene uiScene) {
        Log.info("[IOSApplication] resumed");
        graphics.makeCurrent();
        graphics.resume();
        input.resumeAccelerometer();
    }

    final void willEnterForeground(UIScene uiScene) {
    }

    final void willResignActive(UIScene uiScene) {
        Log.info("[IOSApplication] paused");
        graphics.makeCurrent();
        graphics.pause();
        Gl.finish();
        input.pauseAccelerometer();
    }

    final void willTerminate(UIApplication uiApp) {
        Log.info("[IOSApplication] disposed");
        // willTerminate can be called before a scene is connected and graphics initialized
        // willTerminate 可能在场景连接、图形初始化之前被调用
        if (graphics != null) {
            graphics.makeCurrent();
        }
        input.disposeAccelerometer();
        Ar<ApplicationListener> listeners = this.listeners;
        synchronized (listeners) {
            for (ApplicationListener listener : listeners) {
                listener.pause();
            }
            for (ApplicationListener listener : listeners) {
                listener.exit();
            }
        }
        if (graphics != null) {
            Gl.finish();
        }
    }

    @Override
    public Thread getMainThread() {
        return mainThread;
    }

    @Override
    public ApplicationType getType() {
        return ApplicationType.iOS;
    }

    @Override
    public int getVersion() {
        return (int) NSProcessInfo.getSharedProcessInfo().getOperatingSystemVersion().getMajorVersion();
    }

    @Override
    public boolean openURI(String URI) {
        NSURL url = new NSURL(URI);
        if (uiApp.canOpenURL(url)) {
            try {
                DispatchQueue.getMainQueue().async(() -> {
                    uiApp.openURL(url, new UIApplicationOpenURLOptions(), null);
                });
                return true;
            } catch (Throwable t) {
                Log.err(t);
                return false;
            }
        }
        return false;
    }

    @Override
    public void post(Runnable runnable) {
        synchronized (runnables) {
            runnables.add(runnable);
            if (Core.graphics != null) {
                Core.graphics.requestRendering();
            }
        }
    }

    public void processRunnables() {
        synchronized (runnables) {
            executedRunnables.clear();
            executedRunnables.addAll(runnables);
            runnables.clear();
        }
        for (int i = 0; i < executedRunnables.size; i++) {
            executedRunnables.get(i).run();
        }
    }

    @Override
    public void exit() {
        NSThread.exit();
    }

    @Override
    public String getClipboardText() {
        return UIPasteboard.getGeneralPasteboard().getString();
    }

    @Override
    public void setClipboardText(String text) {
        UIPasteboard.getGeneralPasteboard().setString(text);
    }

    @Override
    public void setClipboardImage(Pixmap pixmap) {
        try {
            UIPasteboard.getGeneralPasteboard().setData(new NSData(PixmapIO.writePngBytes(pixmap)), "public.png");
        } catch (Throwable ignored) {
        }
    }

    @Override
    public Ar<ApplicationListener> getListeners() {
        return listeners;
    }

    /**
     * Add a listener to handle events from the root view controller
     * <p>
     * 添加监听器以处理来自根视图控制器的事件
     *
     * @param listener The {#link IOSViewControllerListener} to add 要添加的 {#link
     * IOSViewControllerListener}
     */
    public void addViewControllerListener(IOSViewControllerListener listener) {
        viewControllerListener = listener;
    }

    public static abstract class Delegate extends UIApplicationDelegateAdapter {

        private IOSApplication app;

        protected abstract IOSApplication createApplication();

        @Override
        public boolean didFinishLaunching(UIApplication application, UIApplicationLaunchOptions options) {
            // TODO remove once MobiVM ships @CustomClass "preload"; forces RoboVM to preload the scene delegate class
            // TODO 待 MobiVM 提供 @CustomClass "preload" 后移除;强制 RoboVM 预加载场景委托类
            try {
                Class.forName(IOSSceneDelegate.class.getName());
            } catch (ClassNotFoundException ignored) {
            }
            application.addStrongRef(this); // Prevent this from being GCed until the ObjC UIApplication is deallocated
            // 防止此对象在 ObjC UIApplication 释放前被 GC
            this.app = createApplication();

            boolean result = app.didFinishLaunching(application, options);
            if (options != null && options.has(UIApplicationLaunchOptions.Keys.URL())) {
                openURL(((NSURL) options.get(UIApplicationLaunchOptions.Keys.URL())));
            }
            return result;
        }

        @Override
        public void willTerminate(UIApplication application) {
            app.willTerminate(application);
        }

        @Override
        public UISceneConfiguration getConfigurationForConnectingSceneSession(UIApplication application,
                UISceneSession connectingSceneSession, UISceneConnectionOptions options) {
            // ignore screen mirroring/external display sessions, see https://developer.apple.com/forums/thread/815376
            // 忽略屏幕镜像/外接显示器会话,参见 https://developer.apple.com/forums/thread/815376
            if (Foundation.getMajorSystemVersion() < 16) {
                if (connectingSceneSession.getRole() == UISceneSessionRole.ExternalDisplay) {
                    return null;
                }
            } else {
                if (connectingSceneSession.getRole() == UISceneSessionRole.ExternalDisplayNonInteractive) {
                    return null;
                }
            }
            UISceneConfiguration config = new UISceneConfiguration(null, connectingSceneSession.getRole());
            config.setDelegateClass(IOSSceneDelegate.class);
            return config;
        }

        public void willConnect(UIScene scene, UISceneSession session, UISceneConnectionOptions connectionOptions) {
        }

        public void sceneDidBecomeActive(UIScene scene) {
        }

        public void sceneWillResignActive(UIScene scene) {
        }

        public void sceneWillEnterForeground(UIScene scene) {
        }

        public void sceneDidEnterBackground(UIScene scene) {
        }

        public void sceneDidDisconnect(UIScene scene) {
        }

        @Override
        public boolean openURL(UIApplication app, NSURL url, UIApplicationOpenURLOptions options) {
            openURL(url);
            return false;
        }

        void openURL(NSURL url) {
            if (Core.app == null) {
                return;
            }
            Core.app.post(() -> {
                for (ApplicationListener list : Core.app.getListeners()) {
                    list.fileDropped(Core.files.absolute(getDocumentsDirectory()).child(url.getLastPathComponent()));
                }
            });
        }
    }
}
