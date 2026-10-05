package arc.scene.ui;

import arc.*;
import arc.struct.*;
import arc.func.Boolp;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.ChangeListener.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.util.pooling.*;

import static arc.Core.*;

/**
 * A button is a {@link Table} with a checked state and additional {@link ButtonStyle style} fields for pressed, unpressed, and
 * checked. Each time a button is clicked, the checked state is toggled. Being a table, a button can contain any other actors.<br>
 * <br>
 * The button's padding is set to the background drawable's padding when the background changes, overwriting any padding set
 * manually. Padding can still be set on the button's table cells.
 * <p>
 * {@link ChangeEvent} is fired when the button is clicked. Cancelling the event will restore the checked button state to what is
 * was previously.
 * <p>
 * The preferred size of the button is determined by the background and the button contents.
 * <p>
 * 按钮是一个带有选中(checked)状态的 {@link Table},并包含用于按下、未按下和选中状态的额外 {@link ButtonStyle 样式} 字段。按钮每次被点击时,选中状态都会切换。作为表格,按钮可以包含任何其他元素。<br> <br> 当背景改变时,按钮的内边距会被设置为背景可绘制对象的内边距,并覆盖手动设置的内边距。内边距仍可以在按钮的表格单元格上设置。 <p> 按钮被点击时会触发 {@link ChangeEvent}。取消该事件会将按钮的选中状态恢复为之前的状态。 <p> 按钮的首选大小由背景和按钮内容决定。
 * @author Nathan Sweet
 */
public class Button extends Table implements Disableable{
    boolean isChecked, isDisabled;
    ButtonGroup buttonGroup;
    Boolp disabledProvider;
    private ButtonStyle style;
    private ClickListener clickListener;
    private boolean programmaticChangeEvents;

    public Button(ButtonStyle style){
        initialize();
        setStyle(style);
        setSize(getPrefWidth(), getPrefHeight());
    }

    /**
     * Creates a button without setting the style or size. At least a style must be set before using this button.
     * 创建按钮,但不设置样式和大小。使用此按钮前至少必须设置一个样式。
     */
    public Button(){
        initialize();
        this.style = scene.getStyle(ButtonStyle.class);

        Drawable background;
        if(isPressed() && !isDisabled()){
            background = style.down == null ? style.up : style.down;
        }else{
            if(isDisabled() && style.disabled != null)
                background = style.disabled;
            else if(isChecked && style.checked != null)
                background = (isOver() && style.checkedOver != null) ? style.checkedOver : style.checked;
            else if(isOver() && style.over != null)
                background = style.over;
            else
                background = style.up;
        }
        setBackground(background);
    }

    public Button(Drawable up){
        this(new ButtonStyle(up, null, null));
    }

    public Button(Drawable up, Drawable down){
        this(new ButtonStyle(up, down, null));
    }

    public Button(Drawable up, Drawable down, Drawable checked){
        this(new ButtonStyle(up, down, checked));
    }

    @Override
    public void act(float delta){
        super.act(delta);

        if(disabledProvider != null){
            setDisabled(disabledProvider.get());
        }
    }

    private void initialize(){
        this.touchable = Touchable.enabled;
        addListener(clickListener = new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                if(isDisabled()) return;
                setChecked(!isChecked, true);
            }
        });
        addListener(new HandCursorListener());
    }

    @SuppressWarnings("unchecked")
    void setChecked(boolean isChecked, boolean fireEvent){
        if(this.isChecked == isChecked) return;
        if(buttonGroup != null && !buttonGroup.canCheck(this, isChecked)) return;
        this.isChecked = isChecked;

        if(fireEvent){
            ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class, ChangeEvent::new);
            if(fire(changeEvent)) this.isChecked = !isChecked;
            Pools.free(changeEvent);
        }
    }

    /**
     * Toggles the checked state. This method changes the checked state, which fires a {@link ChangeEvent} (if programmatic change
     * events are enabled), so can be used to simulate a button click.
     * <p>
     * 切换选中状态。此方法会改变选中状态并触发 {@link ChangeEvent}(若程序化变更事件已启用),因此可用于模拟按钮点击。
     */
    public void toggle(){
        setChecked(!isChecked);
    }

    public boolean isChecked(){
        return isChecked;
    }

    public void setChecked(boolean isChecked){
        setChecked(isChecked, programmaticChangeEvents);
    }

    public boolean isPressed(){
        return clickListener.isVisualPressed();
    }

    public boolean isOver(){
        return clickListener.isOver();
    }

    public ClickListener getClickListener(){
        return clickListener;
    }

    @Override
    public boolean isDisabled(){
        return isDisabled;
    }

    public void setDisabled(Boolp prov){
        this.disabledProvider = prov;
    }

    /**
     * When true, the button will not toggle {@link #isChecked()} when clicked and will not fire a {@link ChangeEvent}.
     * 为 true 时,按钮被点击时不会切换 {@link #isChecked()},也不会触发 {@link ChangeEvent}。
     */
    @Override
    public void setDisabled(boolean isDisabled){
        this.isDisabled = isDisabled;
    }

    public boolean childrenPressed(){
        boolean[] b = {false};
        Vec2 v = new Vec2();

        forEach(element -> {
            element.stageToLocalCoordinates(v.set(input.mouseX(), input.mouseY()));
            if(element instanceof Button && (((Button)element).getClickListener().isOver(element, v.x, v.y))){
                b[0] = true;
            }
        });

        return b[0];
    }

    /**
     * If false, {@link #setChecked(boolean)} and {@link #toggle()} will not fire {@link ChangeEvent}, event will be fired only
     * when user clicked the button
     * <p>
     * 为 false 时,{@link #setChecked(boolean)} 和 {@link #toggle()} 不会触发 {@link ChangeEvent},只有当用户点击按钮时才会触发该事件。
     */
    public void setProgrammaticChangeEvents(boolean programmaticChangeEvents){
        this.programmaticChangeEvents = programmaticChangeEvents;
    }

    /**
     * Returns the button's style. Modifying the returned style may not have an effect until {@link #setStyle(ButtonStyle)} is
     * called.
     * <p>
     * 返回按钮的样式。在调用 {@link #setStyle(ButtonStyle)} 之前,修改返回的样式可能不会生效。
     */
    public ButtonStyle getStyle(){
        return style;
    }

    public void setStyle(ButtonStyle style){
        if(style == null) throw new IllegalArgumentException("style cannot be null.");
        this.style = style;

        Drawable background;
        if(isPressed() && !isDisabled()){
            background = style.down == null ? style.up : style.down;
        }else{
            if(isDisabled() && style.disabled != null)
                background = style.disabled;
            else if(isChecked && style.checked != null)
                background = (isOver() && style.checkedOver != null) ? style.checkedOver : style.checked;
            else if(isOver() && style.over != null)
                background = style.over;
            else
                background = style.up;
        }
        setBackground(background);
    }

    /** @return May be null. 可以为 null。 */
    public ButtonGroup getButtonGroup(){
        return buttonGroup;
    }

    @Override
    public void draw(){
        validate();

        boolean isDisabled = isDisabled();
        boolean isPressed = isPressed();
        boolean isChecked = isChecked();
        boolean isOver = isOver();

        Drawable background = null;
        if(isDisabled && style.disabled != null)
            background = style.disabled;
        else if(isPressed && style.down != null)
            background = style.down;
        else if(isChecked && style.checked != null)
            background = (style.checkedOver != null && isOver) ? style.checkedOver : style.checked;
        else if(isOver && style.over != null){
            background = style.over;
        }else if(style.up != null)
            background = style.up;

        setBackground(background);

        float offsetX, offsetY;
        if(isPressed && !isDisabled){
            offsetX = style.pressedOffsetX;
            offsetY = style.pressedOffsetY;
        }else if(isChecked && !isDisabled){
            offsetX = style.checkedOffsetX;
            offsetY = style.checkedOffsetY;
        }else{
            offsetX = style.unpressedOffsetX;
            offsetY = style.unpressedOffsetY;
        }

        Ar<Element> children = getChildren();
        for(int i = 0; i < children.size; i++)
            children.get(i).moveBy(offsetX, offsetY);
        super.draw();
        for(int i = 0; i < children.size; i++)
            children.get(i).moveBy(-offsetX, -offsetY);

        Scene stage = getScene();
        if(stage != null && stage.getActionsRequestRendering() && isPressed != clickListener.isPressed())
            Core.graphics.requestRendering();
    }

    @Override
    public float getPrefWidth(){
        float width = super.getPrefWidth();
        if(style.up != null) width = Math.max(width, style.up.getMinWidth());
        if(style.down != null) width = Math.max(width, style.down.getMinWidth());
        if(style.checked != null) width = Math.max(width, style.checked.getMinWidth());
        return width;
    }

    @Override
    public float getPrefHeight(){
        float height = super.getPrefHeight();
        if(style.up != null) height = Math.max(height, style.up.getMinHeight());
        if(style.down != null) height = Math.max(height, style.down.getMinHeight());
        if(style.checked != null) height = Math.max(height, style.checked.getMinHeight());
        return height;
    }

    @Override
    public float getMinWidth(){
        return getPrefWidth();
    }

    @Override
    public float getMinHeight(){
        return getPrefHeight();
    }

    /**
     * The style for a button, see {@link Button}.
     * <p>
     * 按钮的样式,见 {@link Button}。
     * @author mzechner
     */
    public static class ButtonStyle extends Style{
        /**
         * Optional.
         * 可选。
         */
        public Drawable up, down, over, checked, checkedOver, disabled;
        /**
         * Optional.
         * 可选。
         */
        public float pressedOffsetX, pressedOffsetY, unpressedOffsetX,
        unpressedOffsetY, checkedOffsetX, checkedOffsetY;

        public ButtonStyle(){
        }

        public ButtonStyle(Drawable up, Drawable down, Drawable checked){
            this.up = up;
            this.down = down;
            this.checked = checked;
        }

        public ButtonStyle(ButtonStyle style){
            this.up = style.up;
            this.down = style.down;
            this.over = style.over;
            this.checked = style.checked;
            this.checkedOver = style.checkedOver;
            this.disabled = style.disabled;
            this.pressedOffsetX = style.pressedOffsetX;
            this.pressedOffsetY = style.pressedOffsetY;
            this.unpressedOffsetX = style.unpressedOffsetX;
            this.unpressedOffsetY = style.unpressedOffsetY;
            this.checkedOffsetX = style.checkedOffsetX;
            this.checkedOffsetY = style.checkedOffsetY;
        }
    }
}
