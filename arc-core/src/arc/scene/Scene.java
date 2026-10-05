package arc.scene;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.event.FocusListener.*;
import arc.scene.event.InputEvent.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.Pool.*;
import arc.util.pooling.*;
import arc.util.viewport.*;

import static arc.Core.*;


public class Scene implements InputProcessor{
    public final Group root;
    /**
     * Margins for fill layouts.
     * 填充布局的边距。
     */
    public float marginLeft, marginRight, marginTop, marginBottom;

    private final ObjectMap<Class, Object> styleDefaults = new ObjectMap<>();
    private final Vec2 tempCoords = new Vec2();
    private final Element[] pointerOverActors = new Element[20];
    private final boolean[] pointerTouched = new boolean[20];
    private final int[] pointerScreenX = new int[20];
    private final int[] pointerScreenY = new int[20];
    private final SnapshotAr<TouchFocus> touchFocuses = new SnapshotAr<>(true, 4, TouchFocus.class);
    private Viewport viewport;
    private int mouseScreenX, mouseScreenY;
    private Element mouseOverElement;
    private Element keyboardFocus, scrollFocus;
    private boolean actionsRequestRendering = true;

    public Scene(){
        this.viewport = new ScreenViewport(){
            @Override
            public void calculateScissors(Mat batchTransform, Rect area, Rect scissor){
                ScissorStack.calculateScissors(
                getCamera(), getScreenX(), getScreenY(), getScreenWidth(), getScreenHeight(), batchTransform, area, scissor);
            }
        };

        root = new Group(){
            @Override
            public float getHeight(){
                return Scene.this.getHeight() - marginTop - marginBottom;
            }

            @Override
            public float getWidth(){
                return Scene.this.getWidth() - marginLeft - marginRight;
            }
        };
        root.touchable = Touchable.childrenOnly;
        root.setScene(this);

        viewport.update(graphics.getWidth(), graphics.getHeight(), true);
    }

    public Scene(Viewport viewport){
        this();
        this.viewport = viewport;
    }

    @SuppressWarnings("unchecked")
    public <T> T getStyle(Class<T> type){
        return (T)styleDefaults.getThrow(type, () -> new IllegalArgumentException("No default style for type: " + type.getSimpleName()));
    }

    public <T> boolean hasStyle(Class<T> type){
        return styleDefaults.containsKey(type);
    }

    public <T> void addStyle(Class<T> type, T style){
        styleDefaults.put(type, style);
    }

    public void registerStyles(Class<?> type){
        Ar.with(type.getFields()).each(f -> f.getName().startsWith("default"), f -> addStyle(f.getType(), Reflect.get(f)));
    }

    public void registerStyles(Object obj){
        Ar.with(obj.getClass().getFields())
        .each(f -> f.getName().startsWith("default"), f -> addStyle(f.getType(), Reflect.get(obj, f)));
    }

    public @Nullable Element getHoverElement(){
        return mouseOverElement;
    }

    public boolean hasField(){
        return getKeyboardFocus() instanceof TextField;
    }

    public boolean hasMouse(){
        return getHoverElement() != null;
    }

    public boolean hasMouse(float mousex, float mousey){
        return hit(mousex, mousey, true) != null;
    }

    public boolean hasDialog(){
        return getScrollFocus() instanceof Dialog || (getKeyboardFocus() != null && getKeyboardFocus().isDescendantOf(e -> e instanceof Dialog));
    }

    public boolean hasKeyboard(){
        return getKeyboardFocus() != null;
    }

    public boolean hasScroll(){
        return getScrollFocus() != null;
    }

    public Dialog getDialog(){
        if(getKeyboardFocus() instanceof Dialog){
            return (Dialog)getKeyboardFocus();
        }else if(getScrollFocus() instanceof Dialog){
            return (Dialog)getScrollFocus();
        }
        return null;
    }

    public void draw(){
        Camera camera = viewport.getCamera();
        camera.update();

        if(!root.visible) return;

        Draw.proj(camera);

        root.draw();
        Draw.flush();
    }

    /**
     * Calls {@link #act(float)} with {@link Graphics#getDeltaTime()}.
     * 以 {@link Graphics#getDeltaTime()} 调用 {@link #act(float)}。
     */
    public void act(){
        act(graphics.getDeltaTime());
    }

    /**
     * Calls the {@link Element#act(float)} method on each actor in the stage. Typically called each frame. This method also fires
     * enter and exit events.
     * <p>
     * 对舞台中的每个元素调用 {@link Element#act(float)} 方法。通常每帧调用。此方法还会触发进入和离开事件。
     * @param delta Time in seconds since the last frame. 距上一帧的时间,单位为秒。
     */
    public void act(float delta){
        root.y = marginBottom;
        root.x = marginLeft;
        root.height = getHeight() - marginBottom - marginTop;
        root.width = getWidth() - marginLeft - marginRight;

        // Update over actors. Done in act() because actors may change position, which can fire enter/exit without an input event.
        // 更新悬停元素。在 act() 中进行,因为元素可能改变位置,从而在没有输入事件的情况下触发进入/离开事件。
        for(int pointer = 0, n = pointerOverActors.length; pointer < n; pointer++){
            Element overLast = pointerOverActors[pointer];
            // Check if pointer is gone.
            // 检查指针是否已离开。
            if(!pointerTouched[pointer]){
                if(overLast != null){
                    pointerOverActors[pointer] = null;
                    screenToStageCoordinates(tempCoords.set(pointerScreenX[pointer], pointerScreenY[pointer]));
                    // Exit over last.
                    // 离开上一个悬停元素。
                    InputEvent event = Pools.obtain(InputEvent.class, InputEvent::new);
                    event.type = (InputEventType.exit);
                    event.stageX = (tempCoords.x);
                    event.stageY = (tempCoords.y);
                    event.relatedActor = (overLast);
                    event.pointer = (pointer);
                    overLast.fire(event);
                    Pools.free(event);
                }
                continue;
            }
            // Update over actor for the pointer.
            // 为该指针更新悬停元素。
            pointerOverActors[pointer] = fireEnterAndExit(overLast, pointerScreenX[pointer], pointerScreenY[pointer], pointer);
        }
        // Update over element for the mouse on the desktop.
        // 在桌面端为鼠标更新悬停元素。
        if(Core.app.isDesktop() || Core.app.isWeb()){
            mouseOverElement = fireEnterAndExit(mouseOverElement, mouseScreenX, mouseScreenY, -1);
        }else{
            mouseOverElement = hit(mouseScreenX, mouseScreenY, true);
        }

        if(scrollFocus != null && (!scrollFocus.visible || scrollFocus.getScene() == null)) scrollFocus = null;
        if(keyboardFocus != null && (!keyboardFocus.visible || keyboardFocus.getScene() == null)) keyboardFocus = null;

        if(scrollFocus != null){
            Element curr = scrollFocus;
            while(curr.parent != null){
                if(!curr.visible){
                    scrollFocus = null;
                    break;
                }
                curr = curr.parent;
            }
        }

        root.act(delta);
    }

    public Element find(String name){
        return root.find(name);
    }

    public Element findVisible(String name){
        return root.findVisible(name);
    }

    public Element find(Boolf<Element> pred){
        return root.find(pred);
    }

    /**
     * Adds and returns a table. This table will fill the whole scene.
     * 添加并返回一个表格。该表格将填满整个场景。
     */
    public Table table(){
        Table table = new Table();
        table.setFillParent(true);
        add(table);
        return table;
    }

    /**
     * Adds and returns a table. This table will fill the whole scene.
     * 添加并返回一个表格。该表格将填满整个场景。
     */
    public Table table(Cons<Table> cons){
        Table table = new Table();
        table.setFillParent(true);
        add(table);
        cons.get(table);
        return table;
    }

    /**
     * Adds and returns a table. This table will fill the whole scene.
     * 添加并返回一个表格。该表格将填满整个场景。
     */
    public Table table(Drawable style, Cons<Table> cons){
        Table table = new Table(style);
        table.setFillParent(true);
        add(table);
        cons.get(table);
        return table;
    }

    private Element fireEnterAndExit(Element overLast, int screenX, int screenY, int pointer){
        // Find the actor under the point.
        // 查找该点下的元素。
        screenToStageCoordinates(tempCoords.set(screenX, screenY));
        Element over = hit(tempCoords.x, tempCoords.y, true);
        if(over == overLast) return overLast;

        // Exit overLast.
        // 离开上一个悬停元素(overLast)。
        if(overLast != null){
            InputEvent event = Pools.obtain(InputEvent.class, InputEvent::new);
            event.stageX = (tempCoords.x);
            event.stageY = (tempCoords.y);
            event.pointer = (pointer);
            event.type = (InputEventType.exit);
            event.relatedActor = (over);
            overLast.fire(event);
            Pools.free(event);
        }
        // Enter over.
        // 进入悬停元素。
        if(over != null){
            InputEvent event = Pools.obtain(InputEvent.class, InputEvent::new);
            event.stageX = (tempCoords.x);
            event.stageY = (tempCoords.y);
            event.pointer = (pointer);
            event.type = (InputEventType.enter);
            event.relatedActor = (overLast);
            over.fire(event);
            Pools.free(event);
        }
        return over;
    }

    /**
     * Applies a touch down event to the stage and returns true if an actor in the scene {@link SceneEvent#handle() handled} the
     * event.
     * <p>
     * 将触摸按下事件应用到舞台,若场景中有元素 {@link SceneEvent#handle() 处理} 了该事件则返回 true。
     */
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button){
        if(!isInsideViewport(screenX, screenY)) return false;

        pointerTouched[pointer] = true;
        pointerScreenX[pointer] = screenX;
        pointerScreenY[pointer] = screenY;

        screenToStageCoordinates(tempCoords.set(screenX, screenY));

        InputEvent event = Pools.obtain(InputEvent.class, InputEvent::new);
        event.type = (InputEventType.touchDown);
        event.stageX = (tempCoords.x);
        event.stageY = (tempCoords.y);
        event.pointer = (pointer);
        event.keyCode = (button);

        Element target = hit(tempCoords.x, tempCoords.y, true);
        if(target == null){
            if(root.touchable == Touchable.enabled) root.fire(event);
        }else{
            target.fire(event);
        }

        boolean handled = event.handled;
        Pools.free(event);
        return handled;
    }

    /**
     * Applies a touch moved event to the stage and returns true if an actor in the scene {@link SceneEvent#handle() handled} the
     * event. Only {@link InputListener listeners} that returned true for touchDown will receive this event.
     * <p>
     * 将触摸移动事件应用到舞台,若场景中有元素 {@link SceneEvent#handle() 处理} 了该事件则返回 true。只有对 touchDown 返回 true 的 {@link InputListener 监听器} 才会收到此事件。
     */
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer){
        pointerScreenX[pointer] = screenX;
        pointerScreenY[pointer] = screenY;
        mouseScreenX = screenX;
        mouseScreenY = screenY;

        if(touchFocuses.size == 0) return false;

        screenToStageCoordinates(tempCoords.set(screenX, screenY));

        InputEvent event = Pools.obtain(InputEvent.class, InputEvent::new);
        event.type = (InputEventType.touchDragged);
        event.stageX = (tempCoords.x);
        event.stageY = (tempCoords.y);
        event.pointer = (pointer);

        SnapshotAr<TouchFocus> touchFocuses = this.touchFocuses;
        TouchFocus[] focuses = touchFocuses.begin();
        for(int i = 0, n = touchFocuses.size; i < n; i++){
            TouchFocus focus = focuses[i];
            if(focus.pointer != pointer) continue;
            if(!touchFocuses.contains(focus, true)) continue; // Touch focus already gone.
            // 触摸焦点已不存在。
            event.targetActor = focus.target;
            event.listenerActor = focus.listenerActor;
            if(focus.listener.handle(event)) event.handle();
        }
        touchFocuses.end();

        boolean handled = event.handled;
        Pools.free(event);
        return handled;
    }

    /**
     * Applies a touch up event to the stage and returns true if an actor in the scene {@link SceneEvent#handle() handled} the event.
     * Only {@link InputListener listeners} that returned true for touchDown will receive this event.
     * <p>
     * 将触摸抬起事件应用到舞台,若场景中有元素 {@link SceneEvent#handle() 处理} 了该事件则返回 true。只有对 touchDown 返回 true 的 {@link InputListener 监听器} 才会收到此事件。
     */
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, KeyCode button){
        pointerTouched[pointer] = false;
        pointerScreenX[pointer] = screenX;
        pointerScreenY[pointer] = screenY;

        if(touchFocuses.size == 0) return false;

        screenToStageCoordinates(tempCoords.set(screenX, screenY));

        InputEvent event = Pools.obtain(InputEvent.class, InputEvent::new);
        event.type = (InputEventType.touchUp);
        event.stageX = (tempCoords.x);
        event.stageY = (tempCoords.y);
        event.pointer = (pointer);
        event.keyCode = (button);

        SnapshotAr<TouchFocus> touchFocuses = this.touchFocuses;
        TouchFocus[] focuses = touchFocuses.begin();
        for(int i = 0, n = touchFocuses.size; i < n; i++){
            TouchFocus focus = focuses[i];
            if(focus.pointer != pointer || focus.button != button) continue;
            if(!touchFocuses.remove(focus, true)) continue; // Touch focus already gone.
            // 触摸焦点已不存在。
            event.targetActor = focus.target;
            event.listenerActor = focus.listenerActor;
            if(focus.listener.handle(event)) event.handle();
            Pools.free(focus);
        }
        touchFocuses.end();

        boolean handled = event.handled;
        Pools.free(event);
        return handled;
    }

    /**
     * Applies a mouse moved event to the stage and returns true if an actor in the scene {@link SceneEvent#handle() handled} the
     * event. This event only occurs on the desktop.
     * <p>
     * 将鼠标移动事件应用到舞台,若场景中有元素 {@link SceneEvent#handle() 处理} 了该事件则返回 true。此事件仅在桌面端发生。
     */
    @Override
    public boolean mouseMoved(int screenX, int screenY){
        if(!isInsideViewport(screenX, screenY)) return false;

        mouseScreenX = screenX;
        mouseScreenY = screenY;

        screenToStageCoordinates(tempCoords.set(screenX, screenY));

        InputEvent event = Pools.obtain(InputEvent.class, InputEvent::new);
        event.type = (InputEventType.mouseMoved);
        event.stageX = (tempCoords.x);
        event.stageY = (tempCoords.y);

        Element target = hit(tempCoords.x, tempCoords.y, true);
        if(target == null) target = root;

        target.fire(event);
        boolean handled = event.handled;
        Pools.free(event);
        return handled;
    }

    /**
     * Applies a mouse scroll event to the stage and returns true if an actor in the scene {@link SceneEvent#handle() handled} the
     * event. This event only occurs on the desktop.
     * <p>
     * 将鼠标滚轮事件应用到舞台,若场景中有元素 {@link SceneEvent#handle() 处理} 了该事件则返回 true。此事件仅在桌面端发生。
     */
    @Override
    public boolean scrolled(float amountX, float amountY){
        Element target = scrollFocus == null ? root : scrollFocus;

        screenToStageCoordinates(tempCoords.set(mouseScreenX, mouseScreenY));

        InputEvent event = Pools.obtain(InputEvent.class, InputEvent::new);
        event.type = (InputEventType.scrolled);
        event.scrollAmountX = amountX;
        event.scrollAmountY = amountY;
        event.stageX = (tempCoords.x);
        event.stageY = (tempCoords.y);
        target.fire(event);
        boolean handled = event.handled;
        Pools.free(event);
        return handled;
    }

    /**
     * Applies a key down event to the actor that has {@link Scene#setKeyboardFocus(Element) keyboard focus}, if any, and returns
     * true if the event was {@link SceneEvent#handle() handled}.
     * <p>
     * 将按键按下事件应用到拥有 {@link Scene#setKeyboardFocus(Element) 键盘焦点} 的元素(如果有),若该事件被 {@link SceneEvent#handle() 处理} 则返回 true。
     */
    @Override
    public boolean keyDown(KeyCode keyCode){
        Element target = keyboardFocus == null ? root : keyboardFocus;
        InputEvent event = Pools.obtain(InputEvent.class, InputEvent::new);
        event.type = (InputEventType.keyDown);
        event.keyCode = keyCode;
        target.fire(event);
        boolean handled = event.handled;
        Pools.free(event);
        return handled;
    }

    /**
     * Applies a key up event to the actor that has {@link Scene#setKeyboardFocus(Element) keyboard focus}, if any, and returns true
     * if the event was {@link SceneEvent#handle() handled}.
     * <p>
     * 将按键抬起事件应用到拥有 {@link Scene#setKeyboardFocus(Element) 键盘焦点} 的元素(如果有),若该事件被 {@link SceneEvent#handle() 处理} 则返回 true。
     */
    @Override
    public boolean keyUp(KeyCode keyCode){
        Element target = keyboardFocus == null ? root : keyboardFocus;
        InputEvent event = Pools.obtain(InputEvent.class, InputEvent::new);
        event.type = (InputEventType.keyUp);
        event.keyCode = keyCode;
        target.fire(event);
        boolean handled = event.handled;
        Pools.free(event);
        return handled;
    }

    /**
     * Applies a key typed event to the actor that has {@link Scene#setKeyboardFocus(Element) keyboard focus}, if any, and returns
     * true if the event was {@link SceneEvent#handle() handled}.
     * <p>
     * 将按键输入事件应用到拥有 {@link Scene#setKeyboardFocus(Element) 键盘焦点} 的元素(如果有),若该事件被 {@link SceneEvent#handle() 处理} 则返回 true。
     */
    @Override
    public boolean keyTyped(char character){
        Element target = keyboardFocus == null ? root : keyboardFocus;
        InputEvent event = Pools.obtain(InputEvent.class, InputEvent::new);
        event.type = (InputEventType.keyTyped);
        event.character = character;
        target.fire(event);
        boolean handled = event.handled;
        Pools.free(event);
        return handled;
    }

    /**
     * Adds the listener to be notified for all touchDragged and touchUp events for the specified pointer and button.
     * 添加监听器,以便接收指定指针和按钮的所有 touchDragged 和 touchUp 事件通知。
     */
    public void addTouchFocus(EventListener listener, Element listenerActor, Element target, int pointer, KeyCode button){
        TouchFocus focus = Pools.obtain(TouchFocus.class, TouchFocus::new);
        focus.listenerActor = listenerActor;
        focus.target = target;
        focus.listener = listener;
        focus.pointer = pointer;
        focus.button = button;
        touchFocuses.add(focus);
    }

    /**
     * Removes the listener from being notified for all touchDragged and touchUp events for the specified pointer and button. Note
     * the listener may never receive a touchUp event if this method is used.
     * <p>
     * 移除监听器,使其不再接收指定指针和按钮的所有 touchDragged 和 touchUp 事件通知。注意,使用此方法后该监听器可能永远不会收到 touchUp 事件。
     */
    public void removeTouchFocus(EventListener listener, Element listenerActor, Element target, int pointer, KeyCode button){
        SnapshotAr<TouchFocus> touchFocuses = this.touchFocuses;
        for(int i = touchFocuses.size - 1; i >= 0; i--){
            TouchFocus focus = touchFocuses.get(i);
            if(focus.listener == listener && focus.listenerActor == listenerActor && focus.target == target
            && focus.pointer == pointer && focus.button == button){
                touchFocuses.remove(i);
                Pools.free(focus);
            }
        }
    }

    /**
     * Cancels touch focus for the specified actor.
     * <p>
     * 取消指定元素的触摸焦点。
     * @see #cancelTouchFocus()
     */
    public void cancelTouchFocus(Element actor){
        InputEvent event = Pools.obtain(InputEvent.class, InputEvent::new);
        event.type = (InputEventType.touchUp);
        event.stageX = (Integer.MIN_VALUE);
        event.stageY = (Integer.MIN_VALUE);

        // Cancel all current touch focuses for the specified listener, allowing for concurrent modification, and never cancel the
        // 取消指定监听器所有当前的触摸焦点,允许并发修改,且绝不取消
        // same focus twice.
        // 同一焦点两次。
        SnapshotAr<TouchFocus> touchFocuses = this.touchFocuses;
        TouchFocus[] items = touchFocuses.begin();
        for(int i = 0, n = touchFocuses.size; i < n; i++){
            TouchFocus focus = items[i];
            if(focus.listenerActor != actor) continue;
            if(!touchFocuses.remove(focus, true)) continue; // Touch focus already gone.
            // 触摸焦点已不存在。
            event.targetActor = focus.target;
            event.listenerActor = focus.listenerActor;
            event.pointer = (focus.pointer);
            event.keyCode = (focus.button);
            focus.listener.handle(event);
            // Cannot return TouchFocus to pool, as it may still be in use (eg if cancelTouchFocus is called from touchDragged).
            // 不能将 TouchFocus 归还到池中,因为它可能仍在使用中(例如在 touchDragged 中调用了 cancelTouchFocus)。
        }
        touchFocuses.end();

        Pools.free(event);
    }

    /**
     * Sends a touchUp event to all listeners that are registered to receive touchDragged and touchUp events and removes their
     * touch focus. This method removes all touch focus listeners, but sends a touchUp event so that the state of the listeners
     * remains consistent (listeners typically expect to receive touchUp eventually). The location of the touchUp is
     * Integer#MIN_VALUE. Listeners can use {@link InputEvent#isTouchFocusCancel()} to ignore this event if needed.
     * <p>
     * 向所有注册接收 touchDragged 和 touchUp 事件的监听器发送 touchUp 事件,并移除它们的触摸焦点。此方法会移除所有触摸焦点监听器,但会发送一个 touchUp 事件以保持监听器状态一致(监听器通常预期最终会收到 touchUp)。touchUp 的位置为 Integer#MIN_VALUE。监听器可在需要时使用 {@link InputEvent#isTouchFocusCancel()} 忽略此事件。
     */
    public void cancelTouchFocus(){
        cancelTouchFocusExcept(null, null);
    }

    /**
     * Cancels touch focus for all listeners except the specified listener.
     * <p>
     * 取消除指定监听器外所有监听器的触摸焦点。
     * @see #cancelTouchFocus()
     */
    public void cancelTouchFocusExcept(EventListener exceptListener, Element exceptActor){
        InputEvent event = Pools.obtain(InputEvent.class, InputEvent::new);
        event.type = (InputEventType.touchUp);
        event.stageX = (Integer.MIN_VALUE);
        event.stageY = (Integer.MIN_VALUE);

        // Cancel all current touch focuses except for the specified listener, allowing for concurrent modification, and never
        // 取消除指定监听器外所有当前的触摸焦点,允许并发修改,且绝不
        // cancel the same focus twice.
        // 取消同一焦点两次。
        SnapshotAr<TouchFocus> touchFocuses = this.touchFocuses;
        TouchFocus[] items = touchFocuses.begin();
        for(int i = 0, n = touchFocuses.size; i < n; i++){
            TouchFocus focus = items[i];
            if(focus.listener == exceptListener && focus.listenerActor == exceptActor) continue;
            if(!touchFocuses.remove(focus, true)) continue; // Touch focus already gone.
            // 触摸焦点已不存在。
            event.targetActor = focus.target;
            event.listenerActor = focus.listenerActor;
            event.pointer = (focus.pointer);
            event.keyCode = (focus.button);
            focus.listener.handle(event);
            // Cannot return TouchFocus to pool, as it may still be in use (eg if cancelTouchFocus is called from touchDragged).
            // 不能将 TouchFocus 归还到池中,因为它可能仍在使用中(例如在 touchDragged 中调用了 cancelTouchFocus)。
        }
        touchFocuses.end();

        Pools.free(event);
    }

    /**
     * Adds an actor to the root of the stage.
     * <p>
     * 将一个元素添加到舞台的根节点。
     * @see Group#addChild(Element)
     */
    public void add(Element actor){
        root.addChild(actor);
    }

    /**
     * Adds an action to the root of the stage.
     * <p>
     * 向舞台的根节点添加一个动作。
     * @see Group#addAction(Action)
     */
    public void addAction(Action action){
        root.addAction(action);
    }

    /**
     * Returns the root's child actors.
     * <p>
     * 返回根节点的子级元素。
     * @see Group#getChildren()
     */
    public Ar<Element> getElements(){
        return root.children;
    }

    /**
     * Adds a listener to the root.
     * <p>
     * 向根节点添加一个监听器。
     * @see Element#addListener(EventListener)
     */
    public boolean addListener(EventListener listener){
        return root.addListener(listener);
    }

    /**
     * Removes a listener from the root.
     * <p>
     * 从根节点移除一个监听器。
     * @see Element#removeListener(EventListener)
     */
    public boolean removeListener(EventListener listener){
        return root.removeListener(listener);
    }

    /**
     * Adds a capture listener to the root.
     * <p>
     * 向根节点添加一个捕获监听器。
     * @see Element#addCaptureListener(EventListener)
     */
    public boolean addCaptureListener(EventListener listener){
        return root.addCaptureListener(listener);
    }

    /**
     * Removes a listener from the root.
     * <p>
     * 从根节点移除一个监听器。
     * @see Element#removeCaptureListener(EventListener)
     */
    public boolean removeCaptureListener(EventListener listener){
        return root.removeCaptureListener(listener);
    }

    /**
     * Removes the root's children, actions, and listeners.
     * 移除根节点的子级、动作和监听器。
     */
    public void clear(){
        unfocusAll();
        root.clear();
    }

    /**
     * Removes the touch, keyboard, and scroll focused actors.
     * 移除触摸、键盘和滚动的焦点元素。
     */
    public void unfocusAll(){
        setScrollFocus(null);
        setKeyboardFocus(null);
        cancelTouchFocus();
    }

    /**
     * Removes the touch, keyboard, and scroll focus for the specified actor and any descendants.
     * 移除指定元素及其所有后代的触摸、键盘和滚动焦点。
     */
    public void unfocus(Element actor){
        cancelTouchFocus(actor);
        if(scrollFocus != null && scrollFocus.isDescendantOf(actor)) setScrollFocus(null);
        if(keyboardFocus != null && keyboardFocus.isDescendantOf(actor)) setKeyboardFocus(null);
    }

    /**
     * Sets the actor that will receive key events.
     * <p>
     * 设置将接收按键事件的元素。
     * @param actor May be null. 可为 null。
     * @return true if the unfocus and focus events were not cancelled by a {@link FocusListener}. 若失焦和聚焦事件未被 {@link FocusListener} 取消则返回 true。
     */
    public boolean setKeyboardFocus(Element actor){
        if(keyboardFocus == actor) return true;

        FocusEvent event = Pools.obtain(FocusEvent.class, FocusEvent::new);
        event.type = (FocusEvent.Type.keyboard);
        Element oldKeyboardFocus = keyboardFocus;
        if(oldKeyboardFocus != null){
            event.focused = false;
            event.relatedActor = (actor);
            oldKeyboardFocus.fire(event);
        }
        boolean success = !event.cancelled;
        if(success){
            keyboardFocus = actor;
            if(actor != null){
                event.focused = true;
                event.relatedActor = (oldKeyboardFocus);
                actor.fire(event);
                success = !event.cancelled;
                if(!success) setKeyboardFocus(oldKeyboardFocus);
            }
        }
        Pools.free(event);
        return success;
    }

    /**
     * Gets the actor that will receive key events.
     * <p>
     * 获取将接收按键事件的元素。
     * @return May be null. 可为 null。
     */
    public Element getKeyboardFocus(){
        return keyboardFocus;
    }

    /**
     * Sets the actor that will receive scroll events.
     * <p>
     * 设置将接收滚动事件的元素。
     * @param actor May be null. 可为 null。
     * @return true if the unfocus and focus events were not cancelled by a {@link FocusListener}. 若失焦和聚焦事件未被 {@link FocusListener} 取消则返回 true。
     */
    public boolean setScrollFocus(Element actor){
        if(scrollFocus == actor) return true;
        FocusEvent event = Pools.obtain(FocusEvent.class, FocusEvent::new);
        event.type = (FocusEvent.Type.scroll);
        Element oldScrollFocus = scrollFocus;
        if(oldScrollFocus != null){
            event.focused = false;
            event.relatedActor = (actor);
            oldScrollFocus.fire(event);
        }
        boolean success = !event.cancelled;
        if(success){
            scrollFocus = actor;
            if(actor != null){
                event.focused = true;
                event.relatedActor = (oldScrollFocus);
                actor.fire(event);
                success = !event.cancelled;
                if(!success) setScrollFocus(oldScrollFocus);
            }
        }
        Pools.free(event);
        return success;
    }

    /**
     * Gets the actor that will receive scroll events.
     * <p>
     * 获取将接收滚动事件的元素。
     * @return May be null. 可为 null。
     */
    public Element getScrollFocus(){
        return scrollFocus;
    }

    public Viewport getViewport(){
        return viewport;
    }

    public void setViewport(Viewport viewport){
        this.viewport = viewport;
    }

    /**
     * The viewport's world width.
     * 视口的世界宽度。
     */
    public float getWidth(){
        return viewport.getWorldWidth();
    }

    /**
     * The viewport's world height.
     * 视口的世界高度。
     */
    public float getHeight(){
        return viewport.getWorldHeight();
    }

    /**
     * The viewport's camera.
     * 视口的相机。
     */
    public Camera getCamera(){
        return viewport.getCamera();
    }

    /**
     * Returns the {@link Element} at the specified location in stage coordinates. Hit testing is performed in the order the actors
     * were inserted into the stage, last inserted actors being tested first. To get stage coordinates from screen coordinates, use
     * {@link #screenToStageCoordinates(Vec2)}.
     * <p>
     * 返回舞台坐标中指定位置处的 {@link Element}。命中测试按元素插入舞台的顺序进行,后插入的元素先被测试。要将屏幕坐标转换为舞台坐标,请使用 {@link #screenToStageCoordinates(Vec2)}。
     * @param touchable If true, the hit detection will respect the touchability. 若为 true,命中检测将遵循可触碰性。
     * @return May be null if no actor was hit. 若没有元素被命中则为 null。
     */
    public Element hit(float stageX, float stageY, boolean touchable){
        root.parentToLocalCoordinates(tempCoords.set(stageX, stageY));
        return root.hit(tempCoords.x, tempCoords.y, touchable);
    }

    /**
     * Transforms the screen coordinates to stage coordinates.
     * <p>
     * 将屏幕坐标转换为舞台坐标。
     * @param screenCoords Input screen coordinates and output for resulting stage coordinates. 输入屏幕坐标,输出为转换后的舞台坐标。
     */
    public Vec2 screenToStageCoordinates(Vec2 screenCoords){
        viewport.unproject(screenCoords);
        return screenCoords;
    }

    /**
     * Transforms the stage coordinates to screen coordinates.
     * <p>
     * 将舞台坐标转换为屏幕坐标。
     * @param stageCoords Input stage coordinates and output for resulting screen coordinates. 输入舞台坐标,输出为转换后的屏幕坐标。
     */
    public Vec2 stageToScreenCoordinates(Vec2 stageCoords){
        viewport.project(stageCoords);
        stageCoords.y = viewport.getScreenHeight() - stageCoords.y;
        return stageCoords;
    }

    /**
     * Transforms the coordinates to screen coordinates. The coordinates can be anywhere in the stage since the transform matrix
     * describes how to convert them.
     * <p>
     * 将坐标转换为屏幕坐标。由于变换矩阵描述了转换方式,坐标可以位于舞台的任意位置。
     * @see Element#localToStageCoordinates(Vec2)
     */
    public Vec2 toScreenCoordinates(Vec2 coords, Mat transformMatrix){
        return viewport.toScreenCoordinates(coords, transformMatrix);
    }

    /**
     * Calculates window scissor coordinates from local coordinates using the batch's current transformation matrix.
     * 使用批处理当前的变换矩阵,从局部坐标计算窗口剪裁坐标。
     */
    public void calculateScissors(Rect localRect, Rect scissorRect){
        Mat transformMatrix = Draw.trans();
        viewport.calculateScissors(transformMatrix, localRect, scissorRect);
    }

    public boolean getActionsRequestRendering(){
        return actionsRequestRendering;
    }

    /**
     * If true, any actions executed during a call to {@link #act()}) will result in a call to {@link Graphics#requestRendering()}
     * . Widgets that animate or otherwise require additional rendering may check this setting before calling
     * {@link Graphics#requestRendering()}. Default is true.
     * <p>
     * 若为 true,在调用 {@link #act()} 期间执行的任何动作都会导致调用 {@link Graphics#requestRendering()}。有动画或其他额外渲染需求的部件可在调用 {@link Graphics#requestRendering()} 前检查此设置。默认为 true。
     */
    public void setActionsRequestRendering(boolean actionsRequestRendering){
        this.actionsRequestRendering = actionsRequestRendering;
    }

    /**
     * Check if screen coordinates are inside the viewport's screen area.
     * 检查屏幕坐标是否位于视口的屏幕区域内。
     */
    protected boolean isInsideViewport(int screenX, int screenY){
        int x0 = viewport.getScreenX();
        int x1 = x0 + viewport.getScreenWidth();
        int y0 = viewport.getScreenY();
        int y1 = y0 + viewport.getScreenHeight();
        screenY = graphics.getHeight() - screenY;
        return screenX >= x0 && screenX < x1 && screenY >= y0 && screenY < y1;
    }

    /**
     * Updates the viewport.
     * 更新视口。
     */
    public void resize(int width, int height){
        viewport.update(width, height, true);
    }

    /**
     * Internal class for managing touch focus.
     * <p>
     * 用于管理触摸焦点的内部类。
     * @author Nathan Sweet
     */
    private static final class TouchFocus implements Poolable{
        EventListener listener;
        Element listenerActor, target;
        int pointer;
        KeyCode button;

        @Override
        public void reset(){
            listenerActor = null;
            listener = null;
            target = null;
        }
    }
}
