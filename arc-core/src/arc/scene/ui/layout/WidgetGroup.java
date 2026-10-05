package arc.scene.ui.layout;

import arc.scene.*;
import arc.struct.*;

/**
 * A {@link Group} that participates in layout and provides a minimum, preferred, and maximum size.
 * <p>
 * The default preferred size of a widget group is 0 and this is almost always overridden by a subclass. The default minimum size
 * returns the preferred size, so a subclass may choose to return 0 for minimum size if it wants to allow itself to be sized
 * smaller than the preferred size. The default maximum size is 0, which means no maximum size.
 * <p>
 * See {@link Layout} for details on how a widget group should participate in layout. A widget group's mutator methods should call
 * {@link #invalidate()} or {@link #invalidateHierarchy()} as needed. By default, invalidateHierarchy is called when child widgets
 * are added and removed.
 * <p>
 * 参与布局并提供最小、首选和最大大小的 {@link Group}。 <p> 控件组的默认首选大小为 0,子类几乎总会重写它。默认最小大小返回首选大小,因此若子类希望允许自身被设置为小于首选大小,可以选择让最小大小返回 0。默认最大大小为 0,表示没有最大大小限制。 <p> 有关控件组应如何参与布局的详细信息,见 {@link Layout}。控件组的修改方法应根据需要调用 {@link #invalidate()} 或 {@link #invalidateHierarchy()}。默认情况下,添加和移除子控件时会调用 invalidateHierarchy。
 * @author Nathan Sweet
 */
public class WidgetGroup extends Group{
    private boolean needsLayout = true;
    private boolean layoutEnabled = true;

    public WidgetGroup(){
    }

    /**
     * Creates a new widget group containing the specified actors.
     * 创建一个包含指定元素的新控件组。
     */
    public WidgetGroup(Element... actors){
        for(Element actor : actors)
            addChild(actor);
    }

    @Override
    public float getMinWidth(){
        return getPrefWidth();
    }

    @Override
    public float getMinHeight(){
        return getPrefHeight();
    }

    @Override
    public float getPrefWidth(){
        return 0;
    }

    @Override
    public float getPrefHeight(){
        return 0;
    }

    @Override
    public void setLayoutEnabled(boolean enabled){
        if(layoutEnabled == enabled) return;
        layoutEnabled = enabled;
        setLayoutEnabled(this, enabled);
    }

    private void setLayoutEnabled(Group parent, boolean enabled){
        SnapshotAr<Element> children = parent.getChildren();
        for(int i = 0, n = children.size; i < n; i++){
            children.get(i).setLayoutEnabled(enabled);
        }
    }

    @Override
    public void validate(){
        if(!layoutEnabled) return;

        Group parent = this.parent;
        if(fillParent && parent != null){
            float parentWidth = parent.getWidth();
            float parentHeight = parent.getHeight();

            if(getWidth() != parentWidth || getHeight() != parentHeight){
                setWidth(parentWidth);
                setHeight(parentHeight);
                invalidate();
            }
        }

        if(!needsLayout) return;
        needsLayout = false;
        layout();
    }

    /**
     * Returns true if the widget's layout has been {@link #invalidate() invalidated}.
     * 若控件的布局已被 {@link #invalidate() 标记为失效} 则返回 true。
     */
    @Override
    public boolean needsLayout(){
        return needsLayout;
    }

    @Override
    public void invalidate(){
        needsLayout = true;
    }

    @Override
    public void invalidateHierarchy(){
        invalidate();
        Group parent = this.parent;
        if(parent != null) parent.invalidateHierarchy();
    }

    @Override
    protected void childrenChanged(){
        invalidateHierarchy();
    }

    @Override
    protected void sizeChanged(){
        invalidate();
    }

    @Override
    public void pack(){
        setSize(getPrefWidth(), getPrefHeight());
        validate();
        //Some situations require another layout. Eg, a wrapped label doesn't know its pref height until it knows its width, so it
        // 某些情况需要再次布局。例如,启用换行的标签在知道宽度之前无法得知其首选高度,因此它会
        //calls invalidateHierarchy() in layout() if its pref height has changed.
        // 在首选高度变化时于 layout() 中调用 invalidateHierarchy()。
        if(needsLayout){
            setSize(getPrefWidth(), getPrefHeight());
            validate();
        }
    }

    @Override
    public void setFillParent(boolean fillParent){
        this.fillParent = fillParent;
    }

    @Override
    public void layout(){
    }

    /**
     * If this method is overridden, the super method or {@link #validate()} should be called to ensure the widget group is laid
     * out.
     * <p>
     * 若重写此方法,应调用超类方法或 {@link #validate()},以确保控件组完成布局。
     */
    @Override
    public void draw(){
        validate();
        super.draw();
    }
}
