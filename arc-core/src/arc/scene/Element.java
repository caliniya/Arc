package arc.scene;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.event.InputEvent.*;
import arc.scene.utils.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;

import static arc.util.Align.*;

public class Element{
    public final Color color = new Color(1, 1, 1, 1);
    public float originX, originY, scaleX = 1, scaleY = 1, rotation;
    public String name;
    public boolean fillParent;
    public Vec2 translation = new Vec2(0, 0);
    public boolean visible = true;
    public Object userObject;
    public Touchable touchable = Touchable.enabled;
    public Group parent;
    public Boolp visibility;
    public Prov<Touchable> touchablility;
    public boolean cullable = true;

    private final DelayedRemovalAr<EventListener> listeners = new DelayedRemovalAr<>(0), captureListeners = new DelayedRemovalAr<>(0);
    private final Ar<Action> actions = new Ar<>(0);

    public float x, y;

    /**
     * DO NOT modify without calling sizeChanged.
     * 未经 sizeChanged 调用请勿修改。
     */
    protected float width, height;
    /**
     * Alpha value of the parent. Should be multiplied with the actor's alpha, allowing a parent's alpha to affect all children.
     * 父级的 alpha 值。应与元素的 alpha 相乘,使父级的 alpha 能影响所有子级。
     */
    protected float parentAlpha = 1f;

    private Scene stage;
    private boolean needsLayout = true;
    private boolean layoutEnabled = true;
    private Runnable update;

    /**
     * Draws the element. Does nothing by default.
     * 绘制该元素。默认不做任何事。
     */
    public void draw(){
        validate();
    }

    /**
     * Updates the actor based on time. Typically this is called each frame by {@link Scene#act(float)}.
     * <p>
     * The default implementation calls {@link Action#act(float)} on each action and removes actions that are complete.
     * <p>
     * 基于时间更新该元素。通常由 {@link Scene#act(float)} 每帧调用。 <p> 默认实现对每个动作调用 {@link Action#act(float)},并移除已完成的动作。
     * @param delta Time in seconds since the last frame. 距上一帧的时间,单位为秒。
     */
    public void act(float delta){
        Ar<Action> actions = this.actions;
        if(actions.size > 0){
            if(stage != null && stage.getActionsRequestRendering()) Core.graphics.requestRendering();
            for(int i = 0; i < actions.size; i++){
                Action action = actions.get(i);
                if(action.act(delta) && i < actions.size){
                    Action current = actions.get(i);
                    int actionIndex = current == action ? i : actions.indexOf(action, true);
                    if(actionIndex != -1){
                        actions.remove(actionIndex);
                        action.setActor(null);
                        i--;
                    }
                }
            }
        }

        if(touchablility != null) this.touchable = touchablility.get();
        if(update != null) update.run();
    }

    public void updateVisibility(){
        if(visibility != null) this.visible = visibility.get();
    }

    public boolean hasMouse(){
        Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
        return e == this || (e != null && e.isDescendantOf(this));
    }

    public boolean hasKeyboard(){
        return Core.scene.getKeyboardFocus() == this;
    }

    public boolean hasScroll(){
        return Core.scene.getScrollFocus() == this;
    }

    public void requestKeyboard(){
        Core.scene.setKeyboardFocus(this);
    }

    public void requestScroll(){
        Core.scene.setScrollFocus(this);
    }

    @SuppressWarnings("unchecked")
    public boolean fire(SceneEvent event){
        event.targetActor = this;

        // Collect ancestors so event propagation is unaffected by hierarchy changes.
        // 收集所有祖先,使事件传播不受层级变化的影响。
        Ar<Group> ancestors = Pools.obtain(Ar.class, Ar::new);
        Group parent = this.parent;
        while(parent != null){
            ancestors.add(parent);
            parent = parent.parent;
        }

        try{
            // Notify all parent capture listeners, starting at the root. Ancestors may stop an event before children receive it.
            // 从根节点开始通知所有父级捕获监听器。祖先可以在子级收到事件之前将其停止。
            Object[] ancestorsArray = ancestors.items;
            for(int i = ancestors.size - 1; i >= 0; i--){
                Group currentTarget = (Group)ancestorsArray[i];
                currentTarget.notify(event, true);
                if(event.stopped) return event.cancelled;
            }

            // Notify the target capture listeners.
            // 通知目标的捕获监听器。
            notify(event, true);
            if(event.stopped) return event.cancelled;

            // Notify the target listeners.
            // 通知目标的监听器。
            notify(event, false);
            if(!event.bubbles) return event.cancelled;
            if(event.stopped) return event.cancelled;

            // Notify all parent listeners, starting at the target. Children may stop an event before ancestors receive it.
            // 从目标开始通知所有父级监听器。子级可以在祖先收到事件之前将其停止。
            for(int i = 0, n = ancestors.size; i < n; i++){
                ((Group)ancestorsArray[i]).notify(event, false);
                if(event.stopped) return event.cancelled;
            }

            return event.cancelled;
        }finally{
            ancestors.clear();
            Pools.free(ancestors);
        }
    }

    public boolean notify(SceneEvent event, boolean capture){
        if(event.targetActor == null) throw new IllegalArgumentException("The event target cannot be null.");

        DelayedRemovalAr<EventListener> listeners = capture ? captureListeners : this.listeners;
        if(listeners.size == 0) return event.cancelled;

        event.listenerActor = this;
        event.capture = capture;

        listeners.begin();
        for(int i = 0, n = listeners.size; i < n; i++){
            EventListener listener = listeners.get(i);
            if(listener.handle(event)){
                event.handle();
                if(event instanceof InputEvent){
                    InputEvent inputEvent = (InputEvent)event;
                    if(inputEvent.type == InputEventType.touchDown){
                        getScene().addTouchFocus(listener, this, inputEvent.targetActor, inputEvent.pointer,
                        inputEvent.keyCode);
                    }
                }
            }
        }
        listeners.end();

        return event.cancelled;
    }

    /**
     * Returns the deepest actor that contains the specified point and is {@link #getTouchable() touchable} and
     * {@link #isVisible() visible}, or null if no actor was hit. The point is specified in the actor's local coordinate system
     * (0,0 is the bottom left of the actor and width,height is the upper right).
     * <p>
     * This method is used to delegate touchDown, mouse, and enter/exit events. If this method returns null, those events will not
     * occur on this Actor.
     * <p>
     * The default implementation returns this actor if the point is within this actor's bounds.
     * <p>
     * 返回包含指定点且 {@link #getTouchable() 可触碰} 和 {@link #isVisible() 可见} 的最深层元素,若没有元素被命中则返回 null。该点以元素的局部坐标系指定(0,0 是元素的左下角,width,height 是右上角)。 <p> 此方法用于分发 touchDown、鼠标和进入/离开事件。若此方法返回 null,这些事件就不会发生在该元素上。 <p> 默认实现中,若该点在此元素的边界内则返回该元素本身。
     * @param touchable If true, the hit detection will respect the {@link #touchable(Touchable) touchability}. 若为 true,命中检测将遵循 {@link #touchable(Touchable) 可触碰性}。
     * @see Touchable
     */
    public Element hit(float x, float y, boolean touchable){
        if(touchable && this.touchable != Touchable.enabled) return null;
        Element e = this;
        return x >= e.translation.x && x < width + e.translation.x && y >= e.translation.y && y < height + e.translation.y ? this : null;
    }

    /**
     * Removes this actor from its parent, if it has a parent.
     * <p>
     * 将该元素从其父级中移除(如果它有父级)。
     * @see Group#removeChild(Element)
     */
    public boolean remove(){
        return parent != null && parent.removeChild(this, true);
    }


    /**Adds a listener which listens for drag (touch down and move) events.
     * <p>
     * 添加一个监听拖拽(触摸按下并移动)事件的监听器。结果以正的增量返回。
     * Results are returned in positive deltas.*/
    public void dragged(Floatc2 cons){
        addListener(new InputListener(){
            float lastX, lastY;

            @Override
            public void touchDragged(InputEvent event, float mx, float my, int pointer){
                if(Core.app.isMobile() && pointer != 0) return;

                cons.get(mx - lastX, my - lastY);
                lastX = mx;
                lastY = my;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(Core.app.isMobile() && pointer != 0) return false;

                lastX = x;
                lastY = y;
                return true;
            }
        });
    }

    public void scrolled(Floatc cons){
        addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                cons.get(amountY);
                return true;
            }
        });
    }

    /**
     * Add a listener to receive events that {@link #hit(float, float, boolean) hit} this actor. See {@link #fire(SceneEvent)}.
     * <p>
     * 添加一个监听器,以接收 {@link #hit(float, float, boolean) 命中} 该元素的事件。参见 {@link #fire(SceneEvent)}。
     * @see InputListener
     * @see ClickListener
     */
    public boolean addListener(EventListener listener){
        if(listener == null) throw new IllegalArgumentException("listener cannot be null.");
        if(!listeners.contains(listener, true)){
            listeners.add(listener);
            return true;
        }
        return false;
    }

    public boolean removeListener(EventListener listener){
        if(listener == null) throw new IllegalArgumentException("listener cannot be null.");
        return listeners.remove(listener, true);
    }

    public Ar<EventListener> getListeners(){
        return listeners;
    }

    /**
     * Adds a listener that is only notified during the capture phase.
     * <p>
     * 添加一个仅在捕获阶段被通知的监听器。
     * @see #fire(SceneEvent)
     */
    public boolean addCaptureListener(EventListener listener){
        if(listener == null) throw new IllegalArgumentException("listener cannot be null.");
        if(!captureListeners.contains(listener, true)) captureListeners.add(listener);
        return true;
    }

    public boolean removeCaptureListener(EventListener listener){
        if(listener == null) throw new IllegalArgumentException("listener cannot be null.");
        return captureListeners.remove(listener, true);
    }

    public Ar<EventListener> getCaptureListeners(){
        return captureListeners;
    }

    public void addAction(Action action){
        action.setActor(this);
        actions.add(action);

        if(stage != null && stage.getActionsRequestRendering()) Core.graphics.requestRendering();
    }

    public void actions(Action... actions){
        addAction(Actions.sequence(actions));
    }

    public void removeAction(Action action){
        if(actions.remove(action, true)) action.setActor(null);
    }

    public Ar<Action> getActions(){
        return actions;
    }

    /**
     * Returns true if the actor has one or more actions.
     * 若该元素拥有一个或多个动作则返回 true。
     */
    public boolean hasActions(){
        return actions.size > 0;
    }

    /**
     * Removes all actions on this actor.
     * 移除该元素上的所有动作。
     */
    public void clearActions(){
        for(int i = actions.size - 1; i >= 0; i--)
            actions.get(i).setActor(null);
        actions.clear();
    }

    /**
     * Removes all listeners on this actor.
     * 移除该元素上的所有监听器。
     */
    public void clearListeners(){
        listeners.clear();
        captureListeners.clear();
    }

    /**
     * Removes all actions and listeners on this actor.
     * 移除该元素上的所有动作和监听器。
     */
    public void clear(){
        clearActions();
        clearListeners();
    }

    /**
     * Returns the stage that this actor is currently in, or null if not in a stage.
     * 返回该元素当前所在的舞台,若不在任何舞台中则返回 null。
     */
    public Scene getScene(){
        return stage;
    }

    /**
     * Called by the framework when this actor or any parent is added to a group that is in the stage.
     * <p>
     * 当该元素或其任何父级被加入处于舞台中的组时,由框架调用。
     * @param stage May be null if the actor or any parent is no longer in a stage. 若该元素或任何父级已不在舞台中,则为 null。
     */
    protected void setScene(Scene stage){
        this.stage = stage;
    }

    public boolean isDescendantOf(Boolf<Element> actor){
        Element parent = this;
        while(parent != null){
            if(actor.get(parent)) return true;
            parent = parent.parent;
        }
        return false;
    }

    /**
     * Returns true if this actor is the same as or is the descendant of the specified actor.
     * 若该元素与指定元素相同或是其后代则返回 true。
     */
    public boolean isDescendantOf(Element actor){
        if(actor == null) throw new IllegalArgumentException("actor cannot be null.");
        Element parent = this;
        while(true){
            if(parent == null) return false;
            if(parent == actor) return true;
            parent = parent.parent;
        }
    }

    /**
     * Returns true if this actor is the same as or is the ascendant of the specified actor.
     * 若该元素与指定元素相同或是其祖先则返回 true。
     */
    public boolean isAscendantOf(Element actor){
        if(actor == null) throw new IllegalArgumentException("actor cannot be null.");
        while(true){
            if(actor == null) return false;
            if(actor == this) return true;
            actor = actor.parent;
        }
    }

    /**
     * Returns true if the actor's parent is not null.
     * 若该元素的父级不为 null 则返回 true。
     */
    public boolean hasParent(){
        return parent != null;
    }

    /**
     * Returns true if input events are processed by this actor.
     * 若输入事件由该元素处理则返回 true。
     */
    public boolean isTouchable(){
        return touchable == Touchable.enabled;
    }

    /**
     * Returns the X position of the specified {@link Align alignment}.
     * 返回指定 {@link Align 对齐方式} 的 X 位置。
     */
    public float getX(int alignment){
        float x = this.x;
        if((alignment & right) != 0)
            x += width;
        else if((alignment & left) == 0) //
            x += width / 2;
        return x;
    }

    /**
     * Returns the Y position of the specified {@link Align alignment}.
     * 返回指定 {@link Align 对齐方式} 的 Y 位置。
     */
    public float getY(int alignment){
        float y = this.y;
        if((alignment & top) != 0)
            y += height;
        else if((alignment & bottom) == 0) //
            y += height / 2;
        return y;
    }

    /**
     * Sets the position of the actor's bottom left corner.
     * 设置元素左下角的位置。
     */
    public void setPosition(float x, float y){
        if(this.x != x || this.y != y){
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Sets the position using the specified {@link Align alignment}. Note this may set the position to non-integer
     * coordinates.
     * <p>
     * 使用指定的 {@link Align 对齐方式} 设置位置。注意这可能将位置设置为非整数坐标。
     */
    public void setPosition(float x, float y, int alignment){
        if((alignment & right) != 0)
            x -= width;
        else if((alignment & left) == 0) //
            x -= width / 2;

        if((alignment & top) != 0)
            y -= height;
        else if((alignment & bottom) == 0) //
            y -= height / 2;

        if(this.x != x || this.y != y){
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Add x and y to current position
     * 将 x 和 y 加到当前位置上
     */
    public void moveBy(float x, float y){
        if(x != 0 || y != 0){
            this.x += x;
            this.y += y;
        }
    }

    public float getWidth(){
        return width;
    }

    public void setWidth(float width){
        if(this.width != width){
            this.width = width;
            sizeChanged();
        }
    }

    public float getHeight(){
        return height;
    }

    public void setHeight(float height){
        if(this.height != height){
            this.height = height;
            sizeChanged();
        }
    }

    /**
     * Returns y plus height.
     * 返回 y 加 height。
     */
    public float getTop(){
        return y + height;
    }

    /**
     * Returns x plus width.
     * 返回 x 加 width。
     */
    public float getRight(){
        return x + width;
    }

    /**
     * Called when the actor's size has been changed.
     * 当元素的大小被改变时调用。
     */
    protected void sizeChanged(){
        invalidate();
    }

    /**
     * Called when the actor's rotation has been changed.
     * 当元素的旋转被改变时调用。
     */
    protected void rotationChanged(){
    }

    public void setSize(float size){
        setSize(size, size);
    }

    /**
     * Sets the width and height.
     * 设置宽度和高度。
     */
    public void setSize(float width, float height){
        if(this.width != width || this.height != height){
            this.width = width;
            this.height = height;
            sizeChanged();
        }
    }

    /**
     * Adds the specified size to the current size.
     * 将指定的大小加到当前大小上。
     */
    public void sizeBy(float size){
        if(size != 0){
            width += size;
            height += size;
            sizeChanged();
        }
    }

    /**
     * Adds the specified size to the current size.
     * 将指定的大小加到当前大小上。
     */
    public void sizeBy(float width, float height){
        if(width != 0 || height != 0){
            this.width += width;
            this.height += height;
            sizeChanged();
        }
    }

    /**
     * Set bounds the x, y, width, and height.
     * 设置边界,即 x、y、宽度和高度。
     */
    public void setBounds(float x, float y, float width, float height){
        if(this.x != x || this.y != y){
            this.x = x;
            this.y = y;
        }
        if(this.width != width || this.height != height){
            this.width = width;
            this.height = height;
            sizeChanged();
        }
    }

    /**
     * Sets the origin position which is relative to the actor's bottom left corner.
     * 设置相对于元素左下角的原点位置。
     */
    public void setOrigin(float originX, float originY){
        this.originX = originX;
        this.originY = originY;
    }

    /**
     * Sets the origin position to the specified {@link Align alignment}.
     * 将原点位置设置为指定的 {@link Align 对齐方式}。
     */
    public void setOrigin(int alignment){
        if((alignment & left) != 0)
            originX = 0;
        else if((alignment & right) != 0)
            originX = width;
        else
            originX = width / 2;

        if((alignment & bottom) != 0)
            originY = 0;
        else if((alignment & top) != 0)
            originY = height;
        else
            originY = height / 2;
    }

    /**
     * Sets the scale for both X and Y
     * 同时设置 X 和 Y 的缩放
     */
    public void setScale(float scaleXY){
        this.scaleX = scaleXY;
        this.scaleY = scaleXY;
    }

    /**
     * Sets the scale X and scale Y.
     * 设置 X 缩放和 Y 缩放。
     */
    public void setScale(float scaleX, float scaleY){
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    /**
     * Adds the specified scale to the current scale.
     * 将指定的缩放值加到当前缩放上。
     */
    public void scaleBy(float scale){
        scaleX += scale;
        scaleY += scale;
    }

    /**
     * Adds the specified scale to the current scale.
     * 将指定的缩放值加到当前缩放上。
     */
    public void scaleBy(float scaleX, float scaleY){
        this.scaleX += scaleX;
        this.scaleY += scaleY;
    }

    public float getRotation(){
        return rotation;
    }

    public void setRotation(float degrees){
        if(this.rotation != degrees){
            this.rotation = degrees;
            rotationChanged();
        }
    }

    public void setRotationOrigin(float degrees, int align){
        setOrigin(align);
        if(this.rotation != degrees){
            this.rotation = degrees;
            rotationChanged();
        }
    }

    /**
     * Adds the specified rotation to the current rotation.
     * 将指定的旋转角度加到当前旋转上。
     */
    public void rotateBy(float amountInDegrees){
        if(amountInDegrees != 0){
            rotation += amountInDegrees;
            rotationChanged();
        }
    }

    public void setColor(float r, float g, float b, float a){
        color.set(r, g, b, a);
    }

    public void setColor(Color color){
        this.color.set(color);
    }

    /**
     * Changes the z-order for this actor so it is in front of all siblings.
     * 改变该元素的 z 顺序,使其位于所有同级元素之前。
     */
    public void toFront(){
        setZIndex(Integer.MAX_VALUE);
    }

    /**
     * Changes the z-order for this actor so it is in back of all siblings.
     * 改变该元素的 z 顺序,使其位于所有同级元素之后。
     */
    public void toBack(){
        setZIndex(0);
    }

    /**
     * Returns the z-index of this actor.
     * <p>
     * 返回该元素的 z 索引。
     * @see #setZIndex(int)
     */
    public int getZIndex(){
        Group parent = this.parent;
        if(parent == null) return -1;
        return parent.children.indexOf(this, true);
    }

    /**
     * Sets the z-index of this actor. The z-index is the index into the parent's {@link Group#getChildren() children}, where a
     * lower index is below a higher index. Setting a z-index higher than the number of children will move the child to the front.
     * Setting a z-index less than zero is invalid.
     * <p>
     * 设置该元素的 z 索引。z 索引是元素在父级 {@link Group#getChildren() 子级} 列表中的索引,索引越小越靠下,索引越大越靠上。设置的 z 索引大于子级数量时,会将该子级移到最前面。设置的 z 索引小于零是无效的。
     */
    public void setZIndex(int index){
        if(index < 0) throw new IllegalArgumentException("ZIndex cannot be < 0.");
        Group parent = this.parent;
        if(parent == null) return;
        Ar<Element> children = parent.children;
        if(children.size == 1) return;
        index = Math.min(index, children.size - 1);
        if(children.get(index) == this) return;
        if(!children.remove(this, true)) return;
        children.insert(index, this);
    }

    /**
     * Calls {@link #clipBegin(float, float, float, float)} to clip this actor's bounds.
     * 调用 {@link #clipBegin(float, float, float, float)} 来裁剪该元素的边界。
     */
    public boolean clipBegin(){
        return clipBegin(x, y, width, height);
    }

    /**
     * Clips the specified screen aligned rectangle, specified relative to the transform matrix of the stage's Batch. The
     * transform matrix and the stage's camera must not have rotational components. Calling this method must be followed by a call
     * to {@link #clipEnd()} if true is returned.
     * <p>
     * 裁剪指定的与屏幕对齐的矩形,该矩形相对于舞台 Batch 的变换矩阵指定。变换矩阵和舞台的相机不得含有旋转分量。若返回 true,则调用此方法后必须接着调用 {@link #clipEnd()}。
     * @return false if the clipping area is zero and no drawing should occur. 若裁剪区域为零且不应进行绘制,则返回 false。
     * @see ScissorStack
     */
    public boolean clipBegin(float x, float y, float width, float height){
        if(width <= 0 || height <= 0) return false;
        Rect tableBounds = Rect.tmp;
        tableBounds.x = x;
        tableBounds.y = y;
        tableBounds.width = width;
        tableBounds.height = height;
        Scene stage = this.stage;
        Rect scissorBounds = Pools.obtain(Rect.class, Rect::new);
        stage.calculateScissors(tableBounds, scissorBounds);
        if(ScissorStack.push(scissorBounds)) return true;
        Pools.free(scissorBounds);
        return false;
    }

    /**
     * Ends clipping begun by {@link #clipBegin(float, float, float, float)}.
     * 结束由 {@link #clipBegin(float, float, float, float)} 开始的裁剪。
     */
    public void clipEnd(){
        Pools.free(ScissorStack.pop());
    }

    /**
     * Transforms the specified point in screen coordinates to the actor's local coordinate system.
     * 将屏幕坐标中的指定点转换到该元素的局部坐标系。
     */
    public Vec2 screenToLocalCoordinates(Vec2 screenCoords){
        Scene stage = this.stage;
        if(stage == null) return screenCoords;
        return stageToLocalCoordinates(stage.screenToStageCoordinates(screenCoords));
    }

    /**
     * Transforms the specified point in the stage's coordinates to the actor's local coordinate system.
     * 将舞台坐标系中的指定点转换到该元素的局部坐标系。
     */
    public Vec2 stageToLocalCoordinates(Vec2 stageCoords){
        if(parent != null) parent.stageToLocalCoordinates(stageCoords);
        parentToLocalCoordinates(stageCoords);
        return stageCoords;
    }

    /**
     * Transforms the specified point in the actor's coordinates to be in the stage's coordinates.
     * <p>
     * 将元素坐标系中的指定点转换到舞台坐标系。
     */
    public Vec2 localToStageCoordinates(Vec2 localCoords){
        return localToAscendantCoordinates(null, localCoords);
    }

    /**
     * Transforms the specified point in the actor's coordinates to be in the parent's coordinates.
     * 将元素坐标系中的指定点转换到父级坐标系。
     */
    public Vec2 localToParentCoordinates(Vec2 localCoords){
        final float rotation = -this.rotation;
        final float scaleX = this.scaleX;
        final float scaleY = this.scaleY;
        final float x = this.x + this.translation.x;
        final float y = this.y + this.translation.y;
        if(rotation == 0){
            if(scaleX == 1 && scaleY == 1){
                localCoords.x += x;
                localCoords.y += y;
            }else{
                final float originX = this.originX;
                final float originY = this.originY;
                localCoords.x = (localCoords.x - originX) * scaleX + originX + x;
                localCoords.y = (localCoords.y - originY) * scaleY + originY + y;
            }
        }else{
            final float cos = (float)Math.cos(rotation * Mathf.degreesToRadians);
            final float sin = (float)Math.sin(rotation * Mathf.degreesToRadians);
            final float originX = this.originX;
            final float originY = this.originY;
            final float tox = (localCoords.x - originX) * scaleX;
            final float toy = (localCoords.y - originY) * scaleY;
            localCoords.x = (tox * cos + toy * sin) + originX + x;
            localCoords.y = (tox * -sin + toy * cos) + originY + y;
        }
        return localCoords;
    }

    /**
     * Converts coordinates for this actor to those of a parent actor. The ascendant does not need to be a direct parent.
     * 将该元素的坐标转换为某个父级元素的坐标。该祖先不必是直接父级。
     */
    public Vec2 localToAscendantCoordinates(Element ascendant, Vec2 localCoords){
        Element actor = this;
        while(actor != null){
            actor.localToParentCoordinates(localCoords);
            actor = actor.parent;
            if(actor == ascendant) break;
        }
        return localCoords;
    }

    /**
     * Converts the coordinates given in the parent's coordinate system to this actor's coordinate system.
     * 将在父级坐标系中给出的坐标转换为该元素的坐标系。
     */
    public Vec2 parentToLocalCoordinates(Vec2 parentCoords){
        final float rotation = this.rotation;
        final float scaleX = this.scaleX;
        final float scaleY = this.scaleY;
        final float childX = x + this.translation.x;
        final float childY = y + this.translation.y;
        if(rotation == 0){
            if(scaleX == 1 && scaleY == 1){
                parentCoords.x -= childX;
                parentCoords.y -= childY;
            }else{
                final float originX = this.originX;
                final float originY = this.originY;
                parentCoords.x = (parentCoords.x - childX - originX) / scaleX + originX;
                parentCoords.y = (parentCoords.y - childY - originY) / scaleY + originY;
            }
        }else{
            final float cos = (float)Math.cos(rotation * Mathf.degreesToRadians);
            final float sin = (float)Math.sin(rotation * Mathf.degreesToRadians);
            final float originX = this.originX;
            final float originY = this.originY;
            final float tox = parentCoords.x - childX - originX;
            final float toy = parentCoords.y - childY - originY;
            parentCoords.x = (tox * cos + toy * sin) / scaleX + originX;
            parentCoords.y = (tox * -sin + toy * cos) / scaleY + originY;
        }
        return parentCoords;
    }

    public float getMinWidth(){
        return getPrefWidth();
    }

    public float getMinHeight(){
        return getPrefHeight();
    }

    public float getPrefWidth(){
        return 0;
    }

    public float getPrefHeight(){
        return 0;
    }

    public float getMaxWidth(){
        return 0;
    }

    public float getMaxHeight(){
        return 0;
    }

    public void setLayoutEnabled(boolean enabled){
        layoutEnabled = enabled;
        if(enabled) invalidateHierarchy();
    }

    /** Ensures the actor has been laid out. Calls {@link #layout()} if {@link #invalidate()} has been called since the last time
     * {@link #validate()} was called, or if the actor otherwise needs to be laid out. This method is usually called in
     * <p>
     * 确保元素已完成布局。若自上次调用 {@link #validate()} 以来调用过 {@link #invalidate()},或该元素因其他原因需要布局,则调用 {@link #layout()}。此方法通常由元素自身在 {@link Element#draw()} 中执行绘制之前调用。
     * {@link Element#draw()} by the actor itself before drawing is performed. */
    public void validate(){
        if(!layoutEnabled) return;

        Group parent = this.parent;
        if(fillParent && parent != null){
            setSize(parent.getWidth(), parent.getHeight());
        }

        if(!needsLayout) return;
        needsLayout = false;
        layout();
    }

    /**
     * Returns true if the widget's layout has been {@link #invalidate() invalidated}.
     * 若部件的布局已被 {@link #invalidate() 标记为失效} 则返回 true。
     */
    public boolean needsLayout(){
        return needsLayout;
    }

    /** Invalidates this actor's layout, causing {@link #layout()} to happen the next time {@link #validate()} is called. This
     * method should be called when state changes in the actor that requires a layout but does not change the minimum, preferred,
     * <p>
     * 使该元素的布局失效,导致下次调用 {@link #validate()} 时执行 {@link #layout()}。当元素中需要重新布局但不改变其最小、首选、最大或实际大小的状态发生变化时(即不影响父级元素的布局),应调用此方法。
     * maximum, or actual size of the actor (meaning it does not affect the parent actor's layout). */
    public void invalidate(){
        needsLayout = true;
    }

    /** Invalidates this actor and its ascendants, calling {@link #invalidate()} on each. This method should be called when state
     * changes in the actor that affects the minimum, preferred, maximum, or actual size of the actor (meaning it potentially
     * <p>
     * 使该元素及其所有祖先失效,对每个调用 {@link #invalidate()}。当元素中影响其最小、首选、最大或实际大小的状态发生变化时(即可能影响父级元素的布局),应调用此方法。
     * affects the parent actor's layout). */
    public void invalidateHierarchy(){
        if(!layoutEnabled) return;
        invalidate();
        Group parent = this.parent;
        if(parent != null) parent.invalidateHierarchy();
    }

    /** Sizes this actor to its preferred width and height, then calls {@link #validate()}.
     * <p>
     * Generally this method should not be called in an actor's constructor because it calls {@link #layout()}, which means a
     * subclass would have layout() called before the subclass' constructor. Instead, in constructors simply set the actor's size
     * to {@link #getPrefWidth()} and {@link #getPrefHeight()}. This allows the actor to have a size at construction time for more
     * <p>
     * 将该元素设置为其首选宽度和高度,然后调用 {@link #validate()}。 <p> 通常不应在元素的构造函数中调用此方法,因为它会调用 {@link #layout()},这意味着子类的 layout() 会在子类构造函数之前被调用。作为替代,在构造函数中只需将元素的大小设置为 {@link #getPrefWidth()} 和 {@link #getPrefHeight()}。这使元素在构造时就具有大小,便于与不布局其子级的组配合使用。
     * convenient use with groups that do not layout their children. */
    public void pack(){
        setSize(getPrefWidth(), getPrefHeight());
        validate();
    }

    /** If true, this actor will be sized to the parent in {@link #validate()}. If the parent is the stage, the actor will be sized
     * to the stage. This method is for convenience only when the widget's parent does not set the size of its children (such as
     * <p>
     * 若为 true,该元素将在 {@link #validate()} 中被设置为父级的大小。若父级是舞台,则该元素将被设置为舞台的大小。此方法仅在部件的父级不设置其子级大小时(例如舞台)用于便利。
     * the stage). */
    public void setFillParent(boolean fillParent){
        this.fillParent = fillParent;
    }

    /** Computes and caches any information needed for drawing and, if this actor has children, positions and sizes each child,
     * calls {@link #invalidate()} on any each child whose width or height has changed, and calls {@link #validate()} on each
     * <p>
     * 计算并缓存绘制所需的任何信息;若该元素拥有子级,则定位并设置每个子级的大小,对宽或高发生变化的每个子级调用 {@link #invalidate()},并对每个子级调用 {@link #validate()}。几乎不应直接调用此方法,而应使用 {@link #validate()}。
     * child. This method should almost never be called directly, instead {@link #validate()} should be used. */
    public void layout(){}

    public void keepInStage(){
        if(stage == null) return;
        Camera camera = stage.getCamera();
        float parentWidth = stage.getWidth();
        float parentHeight = stage.getHeight();
        if(getX(Align.right) - camera.position.x > parentWidth / 2)
            setPosition(camera.position.x + parentWidth / 2, getY(Align.right), Align.right);
        if(getX(Align.left) - camera.position.x < -parentWidth / 2)
            setPosition(camera.position.x - parentWidth / 2, getY(Align.left), Align.left);
        if(getY(Align.top) - camera.position.y > parentHeight / 2)
            setPosition(getX(Align.top), camera.position.y + parentHeight / 2, Align.top);
        if(getY(Align.bottom) - camera.position.y < -parentHeight / 2)
            setPosition(getX(Align.bottom), camera.position.y - parentHeight / 2, Align.bottom);
    }

    public void setTranslation(float x, float y){
        translation.x = x;
        translation.y = y;
    }

    public void keyDown(KeyCode key, Runnable l){
        keyDown(k -> {
            if(k == key)
                l.run();
        });
    }

    /**
     * Adds a keydown input listener.
     * 添加一个按键按下输入监听器。
     */
    public void keyDown(Cons<KeyCode> cons){
        addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode){
                cons.get(keycode);
                return true;
            }
        });
    }

    /**
     * Fakes a click event on all ClickListeners.
     * 对所有 ClickListener 模拟触发一个点击事件。
     */
    public void fireClick(){
        for(EventListener listener : getListeners()){
            if(listener instanceof ClickListener){
                ((ClickListener)listener).clicked(new InputEvent(), -1, -1);
            }
        }
    }

    /**
     * Adds a click listener.
     * 添加一个点击监听器。
     */
    public ClickListener clicked(Runnable r){
        return clicked(KeyCode.mouseLeft, r);
    }

    /**
     * Adds a click listener.
     * 添加一个点击监听器。
     */
    public ClickListener clicked(KeyCode button, Runnable r){
        return clicked(l -> l.setButton(button), r);
    }

    /**
     * Adds a click listener.
     * 添加一个点击监听器。
     */
    public ClickListener clicked(Cons<ClickListener> tweaker, Runnable r){
        return clicked(tweaker, e -> r.run());
    }

    public ClickListener clicked(Cons<ClickListener> tweaker, Cons<ClickListener> runner){
        ClickListener click;
        Element elem = this;
        addListener(click = new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                if(runner != null && !(elem instanceof Disableable && ((Disableable)elem).isDisabled())) runner.get(this);
            }
        });
        tweaker.get(click);
        return click;
    }

    /**
     * Adds a touch listener.
     * 添加一个触摸监听器。
     */
    public InputListener tapped(Runnable r){
        InputListener result;

        addListener(result = new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                r.run();
                event.stop();
                return true;
            }
        });

        return result;
    }

    /**
     * Adds a hover/mouse enter listener.
     * 添加一个悬停/鼠标进入监听器。
     */
    public void hovered(Runnable r){
        addListener(new InputListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                r.run();
            }
        });
    }

    /**
     * Adds a hover/mouse exit listener.
     * 添加一个悬停/鼠标离开监听器。
     */
    public void exited(Runnable r){
        addListener(new InputListener(){
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element fromActor){
                r.run();
            }
        });
    }

    /**
     * Adds a mouse up listener.
     * 添加一个鼠标抬起监听器。
     */
    public void released(Runnable r){
        addListener(new InputListener(){
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                return true;
            }
            @Override public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                r.run();
            }
        });
    }

    /**
     * Fires a change event on all listeners.
     * 向所有监听器触发一个 change 事件。
     */
    public void change(){
        fire(new ChangeListener.ChangeEvent());
    }

    /**
     * Adds a click listener.
     * 添加一个点击监听器。
     */
    public void changed(Runnable r){
        Element elem = this;
        addListener(new ChangeListener(){
            @Override
            public void changed(ChangeEvent event, Element actor){
                if(!(elem instanceof Disableable && ((Disableable)elem).isDisabled())) r.run();
            }
        });
    }

    public Element update(Runnable r){
        update = r;
        return this;
    }

    public Element visible(Boolp vis){
        visibility = vis;
        return this;
    }

    public void touchable(Prov<Touchable> touch){
        this.touchablility = touch;
    }

    @Override
    public String toString(){
        String name = this.name;
        if(name == null){
            name = super.toString().split("@")[0];
            int dotIndex = name.lastIndexOf('.');
            if(dotIndex != -1) name = name.substring(dotIndex + 1);
        }
        return name;
    }
}
