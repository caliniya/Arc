package arc.scene;

import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.struct.Ar;
import arc.struct.SnapshotAr;
import arc.func.Cons;
import arc.func.Boolf;
import arc.math.Affine2;
import arc.math.Mat;
import arc.scene.event.Touchable;
import arc.scene.style.*;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.Table.DrawRect;
import arc.scene.utils.Cullable;

/**
 * 2D scene graph node that may contain other actors.
 * <p>
 * Actors have a z-order equal to the order they were inserted into the group. Actors inserted later will be drawn on top of
 * actors added earlier. Touch events that hit more than one actor are distributed to topmost actors first.
 * <p>
 * 2D 场景图节点,可包含其他元素。 <p> 元素的 z 顺序等于其插入到组中的顺序。后插入的元素将绘制在先添加的元素之上。命中多个元素的触摸事件会优先分发给最上层的元素。
 * @author mzechner
 * @author Nathan Sweet
 */
public abstract class Group extends Element implements Cullable{
    private static final Vec2 tmp = new Vec2();

    protected final SnapshotAr<Element> children = new SnapshotAr<>(true, 4, Element.class);
    private final Affine2 worldTransform = new Affine2();
    private final Mat computedTransform = new Mat();
    private final Mat oldTransform = new Mat();
    protected boolean transform = false;
    protected Rect cullingArea;

    @Override
    public void act(float delta){
        super.act(delta);
        Element[] actors = children.begin();
        for(int i = 0, n = children.size; i < n; i++){
            actors[i].updateVisibility();
            if(actors[i].visible){
                actors[i].act(delta);
            }
        }
        children.end();
    }

    @Override
    public void draw(){
        if(transform) applyTransform(computeTransform());
        drawChildren();
        if(transform) resetTransform();
    }

    protected void drawChildren(){
        parentAlpha *= this.color.a;
        SnapshotAr<Element> children = this.children;
        Element[] actors = children.begin();
        Rect cullingArea = this.cullingArea;
        if(cullingArea != null){
            // Draw children only if inside culling area.
            // 仅在剔除区域内时才绘制子级。
            float cullLeft = cullingArea.x;
            float cullRight = cullLeft + cullingArea.width;
            float cullBottom = cullingArea.y;
            float cullTop = cullBottom + cullingArea.height;
            if(transform){
                for(int i = 0, n = children.size; i < n; i++){
                    Element child = actors[i];
                    child.parentAlpha = parentAlpha;
                    if(!child.visible) continue;
                    float cx = child.x, cy = child.y;
                    child.x += child.translation.x;
                    child.y += child.translation.y;
                    if((cx <= cullRight && cy <= cullTop && cx + child.width >= cullLeft && cy + child.height >= cullBottom) || !child.cullable)
                        child.draw();
                    child.x -= child.translation.x;
                    child.y -= child.translation.y;
                }
            }else{
                // No transform for this group, offset each child.
                // 该组无变换,偏移每个子级。
                float offsetX = x, offsetY = y;
                x = 0;
                y = 0;
                for(int i = 0, n = children.size; i < n; i++){
                    Element child = actors[i];
                    child.parentAlpha = parentAlpha;
                    if(!child.visible) continue;
                    float cx = child.x, cy = child.y;
                    if((cx <= cullRight && cy <= cullTop && cx + child.width >= cullLeft && cy + child.height >= cullBottom) || !child.cullable){
                        child.x = cx + offsetX + child.translation.x;
                        child.y = cy + offsetY + child.translation.y;
                        child.draw();
                        child.x = cx;
                        child.y = cy;
                    }
                }
                x = offsetX;
                y = offsetY;
            }
        }else{
            // No culling, draw all children.
            // 无剔除,绘制所有子级。
            if(transform){
                for(int i = 0, n = children.size; i < n; i++){
                    Element child = actors[i];
                    child.parentAlpha = parentAlpha;
                    if(!child.visible) continue;
                    child.x += child.translation.x;
                    child.y += child.translation.y;
                    child.draw();
                    child.x -= child.translation.x;
                    child.y -= child.translation.y;
                }
            }else{
                // No transform for this group, offset each child.
                // 该组无变换,偏移每个子级。
                float offsetX = x, offsetY = y;
                x = 0;
                y = 0;
                for(int i = 0, n = children.size; i < n; i++){
                    Element child = actors[i];
                    child.parentAlpha = parentAlpha;
                    if(!child.visible) continue;
                    float cx = child.x, cy = child.y;
                    child.x = cx + offsetX + child.translation.x;
                    child.y = cy + offsetY + child.translation.y;
                    child.draw();
                    child.x = cx;
                    child.y = cy;
                }
                x = offsetX;
                y = offsetY;
            }
        }
        children.end();
    }

    /**
     * Returns the transform for this group's coordinate system.
     * 返回该组坐标系的变换矩阵。
     */
    protected Mat computeTransform(){
        Affine2 worldTransform = this.worldTransform;
        float originX = this.originX, originY = this.originY;
        worldTransform.setToTrnRotScl(x + originX, y + originY, rotation, scaleX, scaleY);
        if(originX != 0 || originY != 0) worldTransform.translate(-originX, -originY);

        // Find the first parent that transforms.
        // 查找第一个进行变换的父级。
        Group parentGroup = parent;
        while(parentGroup != null){
            if(parentGroup.transform) break;
            parentGroup = parentGroup.parent;
        }
        if(parentGroup != null) worldTransform.preMul(parentGroup.worldTransform);

        computedTransform.set(worldTransform);
        return computedTransform;
    }

    /**
     * Set the batch's transformation matrix, often with the result of {@link #computeTransform()}. Note this causes the batch to
     * be flushed. {@link #resetTransform()} will restore the transform to what it was before this call.
     * <p>
     * 设置批处理的变换矩阵,通常使用 {@link #computeTransform()} 的结果。注意这会导致批处理被刷新。{@link #resetTransform()} 会将变换恢复到此次调用之前的状态。
     */
    protected void applyTransform(Mat transform){
        oldTransform.set(Draw.trans());
        Draw.trans(transform);
    }

    /**
     * Restores the batch transform to what it was before {@link #applyTransform(Mat)}. Note this causes the batch to
     * be flushed.
     * <p>
     * 将批处理的变换恢复到 {@link #applyTransform(Mat)} 之前的状态。注意这会导致批处理被刷新。
     */
    protected void resetTransform(){
        Draw.trans(oldTransform);
    }

    /**
     * @return May be null. 可为 null。
     * @see #setCullingArea(Rect)
     */
    public Rect getCullingArea(){
        return cullingArea;
    }

    /**
     * Children completely outside of this rectangle will not be drawn. This is only valid for use with unrotated and unscaled
     * actors.
     * <p>
     * 完全位于此矩形之外的子级将不会被绘制。仅对未旋转且未缩放的元素有效。
     * @param cullingArea May be null. 可为 null。
     */
    @Override
    public void setCullingArea(Rect cullingArea){
        this.cullingArea = cullingArea;
    }

    @Override
    public Element hit(float x, float y, boolean touchable){
        if(touchable && this.touchable == Touchable.disabled) return null;
        Vec2 point = tmp;
        Element[] childrenArray = children.items;
        for(int i = children.size - 1; i >= 0; i--){
            Element child = childrenArray[i];
            //TODO: this optimization may be incorrect, needs further testing.
            // TODO:此优化可能不正确,需要进一步测试。
            if(!child.visible || (child.cullable && cullingArea != null && !cullingArea.overlaps(child.x + child.translation.x, child.y + child.translation.y, child.width, child.height))) continue;
            child.parentToLocalCoordinates(point.set(x, y));
            Element hit = child.hit(point.x, point.y, touchable);
            if(hit != null) return hit;
        }
        return super.hit(x, y, touchable);
    }

    /**
     * Called when actors are added to or removed from the group.
     * 当元素被加入或移出该组时调用。
     */
    protected void childrenChanged(){
    }

    /**
     * Recursively iterates through every child of this group.
     * 递归遍历该组的每个子级。
     */
    public void forEach(Cons<Element> cons){
        for(Element e : getChildren()){
            cons.get(e);
            if(e instanceof Group){
                ((Group)e).forEach(cons);
            }
        }
    }

    public Element fill(DrawRect rect){
        Element e = new Element(){
            @Override
            public void draw(){
                rect.draw(0f, 0f, Group.this.width, Group.this.height);
            }
        };
        e.setFillParent(true);
        //usually this area is used for drawing, and should not capture touch events
        // 通常此区域用于绘制,不应捕获触摸事件
        e.touchable = Touchable.disabled;
        addChild(e);
        return e;
    }

    /**
     * Adds and returns a table. This table will fill the whole scene.
     * 添加并返回一个表格。该表格将填满整个场景。
     */
    public void fill(Cons<Table> cons){
        fill(null, cons);
    }

    /**
     * Adds and returns a table. This table will fill the whole scene.
     * 添加并返回一个表格。该表格将填满整个场景。
     */
    public void fill(Drawable background, Cons<Table> cons){
        Table table = background == null ? new Table() : new Table(background);
        table.setFillParent(true);
        addChild(table);
        cons.get(table);
    }

    /**
     * Adds an actor as a child of this group, removing it from its previous parent. If the actor is already a child of this
     * group, no changes are made.
     * <p>
     * 将一个元素作为子级添加到该组,并将其从先前的父级中移除。若该元素已是该组的子级,则不做任何更改。
     */
    public void addChild(Element actor){
        if(actor.parent != null){
            if(actor.parent == this) return;
            actor.parent.removeChild(actor, false);
        }
        children.add(actor);
        actor.parent = this;
        actor.setScene(getScene());
        childrenChanged();
    }

    /**
     * Adds an actor as a child of this group at a specific index, removing it from its previous parent. If the actor is already a
     * child of this group, no changes are made.
     * <p>
     * 将一个元素作为子级添加到该组的指定索引处,并将其从先前的父级中移除。若该元素已是该组的子级,则不做任何更改。
     * @param index May be greater than the number of children. 可以大于子级数量。
     */
    public void addChildAt(int index, Element actor){
        if(actor.parent != null){
            if(actor.parent == this) return;
            actor.parent.removeChild(actor, false);
        }
        if(index >= children.size)
            children.add(actor);
        else
            children.insert(index, actor);
        actor.parent = this;
        actor.setScene(getScene());
        childrenChanged();
    }

    /**
     * Adds an actor as a child of this group immediately before another child actor, removing it from its previous parent. If the
     * actor is already a child of this group, no changes are made.
     * <p>
     * 将一个元素作为子级添加到该组中另一个子级元素之前,并将其从先前的父级中移除。若该元素已是该组的子级,则不做任何更改。
     */
    public void addChildBefore(Element actorBefore, Element actor){
        if(actor.parent != null){
            if(actor.parent == this) return;
            actor.parent.removeChild(actor, false);
        }
        int index = children.indexOf(actorBefore, true);
        children.insert(index, actor);
        actor.parent = this;
        actor.setScene(getScene());
        childrenChanged();
    }

    /**
     * Adds an actor as a child of this group immediately after another child actor, removing it from its previous parent. If the
     * actor is already a child of this group, no changes are made.
     * <p>
     * 将一个元素作为子级添加到该组中另一个子级元素之后,并将其从先前的父级中移除。若该元素已是该组的子级,则不做任何更改。
     */
    public void addChildAfter(Element actorAfter, Element actor){
        if(actor.parent != null){
            if(actor.parent == this) return;
            actor.parent.removeChild(actor, false);
        }
        int index = children.indexOf(actorAfter, true);
        if(index == children.size)
            children.add(actor);
        else
            children.insert(index + 1, actor);
        actor.parent = this;
        actor.setScene(getScene());
        childrenChanged();
    }

    /**
     * Removes an actor from this group and unfocuses it. Calls {@link #removeChild(Element, boolean)} with true.
     * 从该组中移除一个元素并使其失焦。以 true 调用 {@link #removeChild(Element, boolean)}。
     */
    public boolean removeChild(Element actor){
        return removeChild(actor, true);
    }

    /**
     * Removes an actor from this group. If the actor will not be used again and has actions, they should be
     * {@link Element#clearActions() cleared} so the actions will be returned to their
     * {@link Action#setPool(arc.util.pooling.Pool) pool}, if any. This is not done automatically.
     * <p>
     * 从该组中移除一个元素。若该元素不再被使用且有动作,应将其 {@link Element#clearActions() 清除},以便动作被归还到其 {@link Action#setPool(arc.util.pooling.Pool) 池}(如果有)。这不会自动完成。
     * @param unfocus If true, {@link Scene#unfocus(Element)} is called. 若为 true,将调用 {@link Scene#unfocus(Element)}。
     * @return true if the actor was removed from this group. 若该元素已从此组中移除则返回 true。
     */
    public boolean removeChild(Element actor, boolean unfocus){
        if(!children.remove(actor, true)) return false;
        if(unfocus){
            Scene stage = getScene();
            if(stage != null) stage.unfocus(actor);
        }
        actor.parent = null;
        actor.setScene(null);
        childrenChanged();
        return true;
    }

    /**
     * Removes all actors from this group.
     * 移除该组中的所有元素。
     */
    public void clearChildren(){
        Scene stage = getScene();
        Element[] actors = children.begin();
        for(int i = 0, n = children.size; i < n; i++){
            Element child = actors[i];
            if(stage != null) stage.unfocus(child);
            child.setScene(null);
            child.parent = null;
        }
        children.end();
        children.clear();
        childrenChanged();
    }

    /**
     * Removes all children, actions, and listeners from this group.
     * 移除该组的所有子级、动作和监听器。
     */
    @Override
    public void clear(){
        super.clear();
        clearChildren();
    }

    /**
     * Returns the first actor found with the specified name. Note this recursively compares the name of every actor in the
     * group.
     * <p>
     * 返回找到的第一个具有指定名称的元素。注意,这会递归比较组中每个元素的名称。
     */
    @SuppressWarnings("unchecked")
    public <T extends Element> T find(String name){
        Ar<Element> children = this.children;
        for(int i = 0, n = children.size; i < n; i++)
            if(name.equals(children.get(i).name)) return (T)children.get(i);
        for(int i = 0, n = children.size; i < n; i++){
            Element child = children.get(i);
            if(child instanceof Group){
                Element actor = ((Group)child).find(name);
                if(actor != null) return (T)actor;
            }
        }
        return null;
    }

    /**
     * Finds only visible elements.
     * 仅查找可见的元素。
     */
    @SuppressWarnings("unchecked")
    public <T extends Element> T findVisible(String name){
        Ar<Element> children = this.children;
        for(int i = 0, n = children.size; i < n; i++)
            if(name.equals(children.get(i).name) && children.get(i).visible) return (T)children.get(i);
        for(int i = 0, n = children.size; i < n; i++){
            Element child = children.get(i);
            if(child instanceof Group && child.visible){
                Element actor = ((Group)child).findVisible(name);
                if(actor != null) return (T)actor;
            }
        }
        return null;
    }

    /**
     * Find element by a predicate.
     * 通过谓词查找元素。
     */
    @SuppressWarnings("unchecked")
    public <T extends Element> T find(Boolf<Element> pred){
        Ar<Element> children = this.children;
        for(int i = 0, n = children.size; i < n; i++)
            if(pred.get(children.get(i))) return (T)children.get(i);

        for(int i = 0, n = children.size; i < n; i++){
            Element child = children.get(i);
            if(child instanceof Group){
                Element actor = ((Group)child).find(pred);
                if(actor != null) return (T)actor;
            }
        }
        return null;
    }

    @Override
    protected void setScene(Scene stage){
        super.setScene(stage);
        Element[] childrenArray = children.items;
        for(int i = 0, n = children.size; i < n; i++)
            childrenArray[i].setScene(stage); // StackOverflowError here means the group is its own ancestor.
            // 此处出现 StackOverflowError 意味着该组是它自己的祖先。
    }

    /**
     * Swaps two actors by index. Returns false if the swap did not occur because the indexes were out of bounds.
     * 按索引交换两个元素。若因索引越界而未发生交换则返回 false。
     */
    public boolean swapActor(int first, int second){
        int maxIndex = children.size;
        if(first < 0 || first >= maxIndex) return false;
        if(second < 0 || second >= maxIndex) return false;
        children.swap(first, second);
        return true;
    }

    /**
     * Swaps two actors. Returns false if the swap did not occur because the actors are not children of this group.
     * 交换两个元素。若因元素不是该组的子级而未发生交换则返回 false。
     */
    public boolean swapActor(Element first, Element second){
        int firstIndex = children.indexOf(first, true);
        int secondIndex = children.indexOf(second, true);
        if(firstIndex == -1 || secondIndex == -1) return false;
        children.swap(firstIndex, secondIndex);
        return true;
    }

    /**
     * Returns an ordered list of child actors in this group.
     * 返回该组中子级元素的有序列表。
     */
    public SnapshotAr<Element> getChildren(){
        return children;
    }

    public boolean hasChildren(){
        return children.size > 0;
    }

    public boolean isTransform(){
        return transform;
    }

    /**
     * When true (the default), the Batch is transformed so children are drawn in their parent's coordinate system. This has a
     * performance impact because {@link Batch#flush()} must be done before and after the transform. If the actors in a group are
     * not rotated or scaled, then the transform for the group can be set to false. In this case, each child's position will be
     * offset by the group's position for drawing, causing the children to appear in the correct location even though the Batch has
     * not been transformed.
     * <p>
     * 若为 true(默认),会对 Batch 进行变换,使子级在其父级的坐标系中绘制。这会带来性能开销,因为必须在变换前后调用 {@link Batch#flush()}。若组中的元素没有旋转或缩放,则可将该组的 transform 设置为 false。此时,绘制时每个子级的位置会偏移该组的位置,即使 Batch 未被变换,子级也会显示在正确的位置。
     */
    public void setTransform(boolean transform){
        this.transform = transform;
    }

    /**
     * Converts coordinates for this group to those of a descendant actor. The descendant does not need to be a direct child.
     * 将该组的坐标转换为某个后代元素的坐标。该后代不必是直接子级。
     */
    public Vec2 localToDescendantCoordinates(Element descendant, Vec2 localCoords){
        Group parent = descendant.parent;
        if(parent == null) throw new IllegalArgumentException("Child is not a descendant: " + descendant);
        // First convert to the actor's parent coordinates.
        // 先转换到该元素的父级坐标。
        if(parent != this) localToDescendantCoordinates(parent, localCoords);
        // Then from each parent down to the descendant.
        // 然后从每个父级向下到该后代。
        descendant.parentToLocalCoordinates(localCoords);
        return localCoords;
    }

    /**
     * Returns a description of the actor hierarchy, recursively.
     * 递归地返回元素层级的描述。
     */
    @Override
    public String toString(){
        StringBuilder buffer = new StringBuilder(128);
        toString(buffer, 1);
        buffer.setLength(buffer.length() - 1);
        return buffer.toString();
    }

    void toString(StringBuilder buffer, int indent){
        buffer.append(super.toString());
        buffer.append('\n');

        Element[] actors = children.begin();
        for(int i = 0, n = children.size; i < n; i++){
            for(int ii = 0; ii < indent; ii++)
                buffer.append("|  ");
            Element actor = actors[i];
            if(actor instanceof Group)
                ((Group)actor).toString(buffer, indent + 1);
            else{
                buffer.append(actor);
                buffer.append('\n');
            }
        }
        children.end();
    }
}
