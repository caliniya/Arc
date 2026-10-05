package arc.backend.robovm;

/**
 * An IOSViewControllerListener can be added to an {@link IOSApplication} via
 * {@link IOSApplication#addViewControllerListener(IOSViewControllerListener)}. It will receive notification of view events.</p>
 * <p>
 * The methods will be invoked on the UI thread.
 * <p>
 * 可以通过 {@link IOSApplication#addViewControllerListener(IOSViewControllerListener)} 将 IOSViewControllerListener 添加到 {@link IOSApplication}。它将接收视图事件的通知。</p> <p> 这些方法将在 UI 线程上被调用。
 * @author mzechner
 */
public interface IOSViewControllerListener{

    /**
     * Called when the {@link IOSApplication} root ViewController has appeared
     * 当 {@link IOSApplication} 的根 ViewController 显示后调用
     */
    void viewDidAppear(boolean animated);

}
