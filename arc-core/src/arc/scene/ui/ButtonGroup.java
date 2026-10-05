package arc.scene.ui;

import arc.struct.Ar;

/**
 * Manages a group of buttons to enforce a minimum and maximum number of checked buttons. This enables "radio button"
 * functionality and more. A button may only be in one group at a time.
 * <p>
 * The {@link #canCheck(Button, boolean)} method can be overridden to control if a button check or uncheck is allowed.
 * <p>
 * 管理一组按钮,强制限制被选中按钮的最小和最大数量。这可以实现"单选按钮"等功能。一个按钮同一时间只能属于一个组。 <p> 可以重写 {@link #canCheck(Button, boolean)} 方法来控制是否允许选中或取消选中某个按钮。
 * @author Nathan Sweet
 */
public class ButtonGroup<T extends Button>{
    private final Ar<T> buttons = new Ar<>();
    private Ar<T> checkedButtons = new Ar<>(1);
    private int minCheckCount, maxCheckCount = 1;
    private boolean uncheckLast = true;
    private T lastChecked;

    public ButtonGroup(){
        minCheckCount = 1;
    }

    public ButtonGroup(T... buttons){
        minCheckCount = 0;
        add(buttons);
        minCheckCount = 1;
    }

    public void add(T button){
        if(button == null) throw new IllegalArgumentException("button cannot be null.");
        button.buttonGroup = null;
        boolean shouldCheck = button.isChecked() || buttons.size < minCheckCount;
        button.setChecked(false);
        button.buttonGroup = this;
        buttons.add(button);
        button.setChecked(shouldCheck);
    }

    public void add(T... buttons){
        if(buttons == null) throw new IllegalArgumentException("buttons cannot be null.");
        for(int i = 0, n = buttons.length; i < n; i++)
            add(buttons[i]);
    }

    public void remove(T button){
        if(button == null) throw new IllegalArgumentException("button cannot be null.");
        button.buttonGroup = null;
        buttons.remove(button, true);
        checkedButtons.remove(button, true);
    }

    public void remove(T... buttons){
        if(buttons == null) throw new IllegalArgumentException("buttons cannot be null.");
        for(int i = 0, n = buttons.length; i < n; i++)
            remove(buttons[i]);
    }

    public void clear(){
        buttons.clear();
        checkedButtons.clear();
    }

    /**
     * Called when a button is checked or unchecked. If overridden, generally changing button checked states should not be done
     * from within this method.
     * <p>
     * 当按钮被选中或取消选中时调用。若重写此方法,通常不应在此方法内部修改按钮的选中状态。
     * @return True if the new state should be allowed. 若应允许新状态则返回 true。
     */
    protected boolean canCheck(T button, boolean newState){
        if(button.isChecked == newState) return false;

        if(!newState){
            // Keep button checked to enforce minCheckCount.
            // 保持按钮为选中状态,以满足 minCheckCount。
            if(checkedButtons.size <= minCheckCount) return false;
            checkedButtons.remove(button, true);
        }else{
            // Keep button unchecked to enforce maxCheckCount.
            // 保持按钮为未选中状态,以满足 maxCheckCount。
            if(maxCheckCount != -1 && checkedButtons.size >= maxCheckCount){
                if(uncheckLast){
                    int old = minCheckCount;
                    minCheckCount = 0;
                    lastChecked.setChecked(false);
                    minCheckCount = old;
                }else
                    return false;
            }
            checkedButtons.add(button);
            lastChecked = button;
        }

        return true;
    }

    /**
     * Sets all buttons' {@link Button#isChecked()} to false, regardless of {@link #setMinCheckCount(int)}.
     * 将所有按钮的 {@link Button#isChecked()} 设为 false,不受 {@link #setMinCheckCount(int)} 限制。
     */
    public void uncheckAll(){
        int old = minCheckCount;
        minCheckCount = 0;
        for(int i = 0, n = buttons.size; i < n; i++){
            T button = buttons.get(i);
            button.setChecked(false);
        }
        minCheckCount = old;
    }

    /** @return The first checked button, or null. 第一个被选中的按钮,若无则返回 null。 */
    public T getChecked(){
        if(checkedButtons.size > 0) return checkedButtons.get(0);
        return null;
    }

    /**
     * Sets the first {@link TextButton} with the specified text to checked.
     * 将第一个具有指定文本的 {@link TextButton} 设为选中。
     */
    public void setChecked(String text){
        if(text == null) throw new IllegalArgumentException("text cannot be null.");
        for(int i = 0, n = buttons.size; i < n; i++){
            T button = buttons.get(i);
            if(button instanceof TextButton && text.contentEquals(((TextButton)button).getText())){
                button.setChecked(true);
                return;
            }
        }
    }

    /** @return The first checked button index, or -1. 第一个被选中按钮的索引,若无则返回 -1。 */
    public int getCheckedIndex(){
        if(checkedButtons.size > 0) return buttons.indexOf(checkedButtons.get(0), true);
        return -1;
    }

    public Ar<T> getAllChecked(){
        return checkedButtons;
    }

    public Ar<T> getButtons(){
        return buttons;
    }

    /**
     * Sets the minimum number of buttons that must be checked. Default is 1.
     * 设置必须被选中的按钮的最小数量。默认为 1。
     */
    public void setMinCheckCount(int minCheckCount){
        this.minCheckCount = minCheckCount;
    }

    /**
     * Sets the maximum number of buttons that can be checked. Set to -1 for no maximum. Default is 1.
     * 设置可被选中的按钮的最大数量。设为 -1 表示无上限。默认为 1。
     */
    public void setMaxCheckCount(int maxCheckCount){
        if(maxCheckCount == 0) maxCheckCount = -1;
        this.maxCheckCount = maxCheckCount;
    }

    /**
     * If true, when the maximum number of buttons are checked and an additional button is checked, the last button to be checked
     * is unchecked so that the maximum is not exceeded. If false, additional buttons beyond the maximum are not allowed to be
     * checked. Default is true.
     * <p>
     * 为 true 时,当已选中的按钮达到最大数量后再选中另一个按钮,最后被选中的按钮会被取消选中,以免超过最大数量。为 false 时,超出最大数量的按钮不允许被选中。默认为 true。
     */
    public void setUncheckLast(boolean uncheckLast){
        this.uncheckLast = uncheckLast;
    }
}
