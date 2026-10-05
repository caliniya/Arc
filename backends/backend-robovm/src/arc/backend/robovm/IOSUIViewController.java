package arc.backend.robovm;

import arc.*;
import arc.graphics.*;
import com.badlogic.gdx.backends.iosrobovm.bindings.metalangle.*;
import org.robovm.apple.foundation.*;
import org.robovm.apple.uikit.*;

public class IOSUIViewController extends MGLKViewController{
    final IOSApplication app;
    final IOSGraphics graphics;

    protected IOSUIViewController(IOSApplication app, IOSGraphics graphics){
        this.app = app;
        this.graphics = graphics;
    }

    @Override
    public void viewWillAppear(boolean animated){
        super.viewWillAppear(animated);
        // start GLKViewController even though we may only draw a single frame
        // 即使我们可能只绘制一帧,也要启动 GLKViewController
        // (we may be in non-continuous mode)
        // (我们可能处于非连续渲染模式)
        setPaused(false);
    }

    @Override
    public void viewDidAppear(boolean animated){
        super.viewDidAppear(animated);
        getView().setContentScaleFactor(app.uiWindowScene.getScreen().getNativeScale());
        if(app.viewControllerListener != null) app.viewControllerListener.viewDidAppear(animated);
    }

    @Override
    public UIInterfaceOrientationMask getSupportedInterfaceOrientations(){
        long mask = 0;
        if(app.config.orientationLandscape){
            mask |= (1L << UIInterfaceOrientation.LandscapeLeft.value()) | (1L << UIInterfaceOrientation.LandscapeRight.value());
        }
        if(app.config.orientationPortrait){
            mask |= 1L << UIInterfaceOrientation.Portrait.value();
            if(UIDevice.getCurrentDevice().getUserInterfaceIdiom() == UIUserInterfaceIdiom.Pad){
                mask |= 1L << UIInterfaceOrientation.PortraitUpsideDown.value();
            }
        }
        return new UIInterfaceOrientationMask(mask);
    }

    @Override
    public boolean shouldAutorotate(){
        return true;
    }

    @Override
    public UIRectEdge getPreferredScreenEdgesDeferringSystemGestures(){
        return app.config.screenEdgesDeferringSystemGestures;
    }

    @Override
    public void viewDidLayoutSubviews(){
        super.viewDidLayoutSubviews();
        // get the view size and update graphics
        // 获取视图大小并更新图形
        final IOSScreenBounds oldBounds = graphics.screenBounds;
        final IOSScreenBounds newBounds = app.computeBounds();
        graphics.screenBounds = newBounds;
        // Layout may happen without bounds changing, don't trigger resize in that case
        // 布局可能在边界未变化时发生,这种情况下不要触发 resize
        if(newBounds.width != oldBounds.width || newBounds.height != oldBounds.height){
            graphics.makeCurrent();
            graphics.updateSafeInsets();
            Core.glProvider.glViewport(0, 0, newBounds.backBufferWidth, newBounds.backBufferHeight);

            if(graphics.config.hdpiMode == HdpiUtils.HdpiMode.pixels){
                for(ApplicationListener list : app.listeners){
                    list.resize(newBounds.backBufferWidth, newBounds.backBufferHeight);
                }
            }else{
                for(ApplicationListener list : app.listeners){
                    list.resize(newBounds.width, newBounds.height);
                }
            }
        }
    }

    @Override
    public boolean prefersStatusBarHidden(){
        return !app.config.statusBarVisible;
    }

    @Override
    public boolean prefersHomeIndicatorAutoHidden(){
        return app.config.hideHomeIndicator;
    }

    @Override
    public void pressesBegan(NSSet<UIPress> presses, UIPressesEvent event){
        if(presses == null || presses.isEmpty() || !app.input.onKey(presses.getValues().first().getKey(), true)){
            super.pressesBegan(presses, event);
        }
    }

    @Override
    public void pressesEnded(NSSet<UIPress> presses, UIPressesEvent event){
        if(presses == null || presses.isEmpty() || !app.input.onKey(presses.getValues().first().getKey(), false)){
            super.pressesEnded(presses, event);
        }
    }
}