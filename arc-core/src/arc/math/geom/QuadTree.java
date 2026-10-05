package arc.math.geom;

import arc.func.*;
import arc.math.geom.QuadTree.*;
import arc.struct.*;

/**
 * A basic quad tree.
 * <p>
 * This class represents any node, but you will likely only interact with the root node.
 *
 * <p>
 * 一个基础四叉树。 <p> 此类表示任意节点,但你通常只会与根节点交互。
 * @param <T> The type of object this quad tree should contain. An object only requires some way of getting rough bounds. 此四叉树应包含的对象类型。对象只需提供获取粗略边界的方式即可。
 * @author xSke
 * @author Anuke
 */
public class QuadTree<T extends QuadTreeObject>{
    protected final Rect tmp = new Rect();
    //if many objects are stacked on a point, it may split infinitely, so floor the size
    // 如果许多对象堆叠在一个点上,可能会无限分裂,因此对尺寸取下限
    protected static final float minNodeSize = 10f;
    protected static final int maxObjectsPerNode = 5;

    public Rect bounds;
    public Ar<T> objects = new Ar<>(false);
    public QuadTree<T> botLeft, botRight, topLeft, topRight;
    public boolean leaf = true;
    public int totalObjects;

    //scratch partitioning lists reused across fill() calls to avoid allocating every rebuild
    // 在多次 fill() 调用间复用的临时分区列表,避免每次重建都重新分配
    private Ar<T> fillBL, fillBR, fillTL, fillTR;

    public QuadTree(Rect bounds){
        this.bounds = bounds;
    }

    protected void split(){
        if(!leaf || bounds.width <= minNodeSize || bounds.height <= minNodeSize) return;

        float subW = bounds.width / 2;
        float subH = bounds.height / 2;

        if(botLeft == null){
            botLeft = newChild(new Rect(bounds.x, bounds.y, subW, subH));
            botRight = newChild(new Rect(bounds.x + subW, bounds.y, subW, subH));
            topLeft = newChild(new Rect(bounds.x, bounds.y + subH, subW, subH));
            topRight = newChild(new Rect(bounds.x + subW, bounds.y + subH, subW, subH));
        }
        leaf = false;

        Object[] items = objects.items;

        // Transfer objects to children if they fit entirely in one
        // 若对象能完全放入某个子节点,则将其转移到该子节点
        for(int i = 0; i < objects.size; i++){
            T obj = (T)items[i];
            hitbox(obj);
            QuadTree<T> child = getFittingChild(tmp);
            if(child != null){
                child.insert(obj);
                objects.size --;
                items[i] = items[objects.size];
                items[objects.size] = null;
                i --;
            }
        }
    }

    protected void unsplit(){
        if(leaf) return;
        objects.addAll(botLeft.objects);
        objects.addAll(botRight.objects);
        objects.addAll(topLeft.objects);
        objects.addAll(topRight.objects);
        botLeft.clear();
        botRight.clear();
        topLeft.clear();
        topRight.clear();
        leaf = true;
    }

    /**
     * Inserts an object into this node or its child nodes. This will split a leaf node if it exceeds the object limit.
     * 将对象插入此节点或其子节点。若叶节点超过对象上限,将进行分裂。
     */
    public void insert(T obj){
        hitbox(obj);
        if(!bounds.overlaps(tmp)){
            // New object not in quad tree, ignoring
            // 不在四叉树中的新对象,忽略
            // throw an exception?
            // 抛出异常?
            return;
        }

        totalObjects ++;

        if(leaf && objects.size + 1 > maxObjectsPerNode) split();

        if(leaf){
            // Leaf, so no need to add to children, just add to root
            // 叶节点,无需加入子节点,直接加入根节点
            objects.add(obj);
        }else{
            hitbox(obj);
            // Add to relevant child, or root if can't fit completely in a child
            // 添加到合适的子节点,若无法完全放入任何子节点则添加到根节点
            QuadTree<T> child = getFittingChild(tmp);
            if(child != null){
                child.insert(obj);
            }else{
                objects.add(obj);
            }
        }
    }

    /**
     * Rebuilds this tree from scratch using the given list of objects.
     * 使用给定的对象列表从头重建此树。
     */
    public void fill(Ar<T> list){
        clear();
        totalObjects = list.size;

        if(list.size <= maxObjectsPerNode || bounds.width <= minNodeSize || bounds.height <= minNodeSize){
            objects.addAll(list);
            return;
        }

        if(botLeft == null){
            float subW = bounds.width / 2;
            float subH = bounds.height / 2;
            botLeft = newChild(new Rect(bounds.x, bounds.y, subW, subH));
            botRight = newChild(new Rect(bounds.x + subW, bounds.y, subW, subH));
            topLeft = newChild(new Rect(bounds.x, bounds.y + subH, subW, subH));
            topRight = newChild(new Rect(bounds.x + subW, bounds.y + subH, subW, subH));
        }
        leaf = false;

        if(fillBL == null){
            fillBL = new Ar<>(false);
            fillBR = new Ar<>(false);
            fillTL = new Ar<>(false);
            fillTR = new Ar<>(false);
        }
        fillBL.clear();
        fillBR.clear();
        fillTL.clear();
        fillTR.clear();

        Object[] items = list.items;
        int size = list.size;

        //single partitioning pass instead of one split()-check per insert
        // 用单次分区遍历代替每次插入时的 split() 检查
        for(int i = 0; i < size; i++){
            T obj = (T)items[i];
            hitbox(obj);
            QuadTree<T> child = getFittingChild(tmp);

            if(child == botLeft) fillBL.add(obj);
            else if(child == botRight) fillBR.add(obj);
            else if(child == topLeft) fillTL.add(obj);
            else if(child == topRight) fillTR.add(obj);
            else objects.add(obj); //doesn't fit any quadrant, stays in this node
            // 不适合任何象限,留在此节点中
        }

        botLeft.fill(fillBL);
        botRight.fill(fillBR);
        topLeft.fill(fillTL);
        topRight.fill(fillTR);
    }

    /**
     * Removes an object from this node or its child nodes.
     * 从此节点或其子节点中移除对象。
     */
    public boolean remove(T obj){
        boolean result;
        if(leaf){
            // Leaf, no children, remove from root
            // 叶节点,无子节点,从根节点移除
            result = objects.remove(obj, true);
        }else{
            // Remove from relevant child
            // 从合适的子节点中移除
            hitbox(obj);
            QuadTree<T> child = getFittingChild(tmp);

            if(child != null){
                result = child.remove(obj);
            }else{
                // Or root if object doesn't fit in a child
                // 若对象不适合任何子节点,则放入根节点
                result = objects.remove(obj, true);
            }

            if(totalObjects <= maxObjectsPerNode) unsplit();
        }
        if(result){
            totalObjects --;
        }
        return result;
    }

    /**
     * Removes all objects.
     * 移除所有对象。
     */
    public void clear(){
        objects.clear();
        totalObjects = 0;
        if(!leaf){
            topLeft.clear();
            topRight.clear();
            botLeft.clear();
            botRight.clear();
        }
        leaf = true;
    }

    protected QuadTree<T> getFittingChild(Rect boundingBox){
        float verticalMidpoint = bounds.x + (bounds.width / 2);
        float horizontalMidpoint = bounds.y + (bounds.height / 2);

        // Object can completely fit within the top quadrants
        // 对象可完全放入上方的象限
        boolean topQuadrant = boundingBox.y > horizontalMidpoint;
        // Object can completely fit within the bottom quadrants
        // 对象可完全放入下方的象限
        boolean bottomQuadrant = boundingBox.y < horizontalMidpoint && (boundingBox.y + boundingBox.height) < horizontalMidpoint;

        // Object can completely fit within the left quadrants
        // 对象可完全放入左侧的象限
        if(boundingBox.x < verticalMidpoint && boundingBox.x + boundingBox.width < verticalMidpoint){
            if(topQuadrant){
                return topLeft;
            }else if(bottomQuadrant){
                return botLeft;
            }
        }else if(boundingBox.x > verticalMidpoint){ // Object can completely fit within the right quadrants
        // 对象可完全放入右侧的象限
            if(topQuadrant){
                return topRight;
            }else if(bottomQuadrant){
                return botRight;
            }
        }

        // Else, object needs to be in parent cause it can't fit completely in a quadrant
        // 否则,对象无法完全放入任何象限,需要放入父节点
        return null;
    }

    /**
     * Processes objects that may intersect the given rectangle.
     * <p>
     * This will never result in false positives.
     * <p>
     * 处理可能与给定矩形相交的对象。 <p> 这绝不会产生误报。
     */
    public void intersect(float x, float y, float width, float height, Cons<T> out){
        if(!leaf){
            if(topLeft.bounds.overlaps(x, y, width, height)) topLeft.intersect(x, y, width, height, out);
            if(topRight.bounds.overlaps(x, y, width, height)) topRight.intersect(x, y, width, height, out);
            if(botLeft.bounds.overlaps(x, y, width, height)) botLeft.intersect(x, y, width, height, out);
            if(botRight.bounds.overlaps(x, y, width, height)) botRight.intersect(x, y, width, height, out);
        }

        Ar<?> objects = this.objects;

        for(int i = 0; i < objects.size; i++){
            T item = (T)objects.items[i];
            hitbox(item);
            if(tmp.overlaps(x, y, width, height)){
                out.get(item);
            }
        }
    }

    /**
     * Processes objects that may intersect the given rectangle. Returning true will break out of the function.
     * <p>
     * This will never result in false positives.
     * <p>
     * 处理可能与给定矩形相交的对象。返回 true 将中断函数。 <p> 这绝不会产生误报。
     */
    public boolean intersect(float x, float y, float width, float height, Boolf<T> out){
        if(!leaf){
            if(topLeft.bounds.overlaps(x, y, width, height) && topLeft.intersect(x, y, width, height, out)) return true;
            if(topRight.bounds.overlaps(x, y, width, height) && topRight.intersect(x, y, width, height, out)) return true;
            if(botLeft.bounds.overlaps(x, y, width, height) && botLeft.intersect(x, y, width, height, out)) return true;
            if(botRight.bounds.overlaps(x, y, width, height)&& botRight.intersect(x, y, width, height, out)) return true;
        }

        Ar<?> objects = this.objects;

        for(int i = 0; i < objects.size; i++){
            T item = (T)objects.items[i];
            hitbox(item);
            if(tmp.overlaps(x, y, width, height) && out.get(item)){
                return true;
            }
        }
        return false;
    }

    /**
     * Tries to find any object matching the predicate in this tree.
     * <p>
     * This will never result in false positives.
     * <p>
     * 尝试在此树中查找任何符合谓词的对象。 <p> 这绝不会产生误报。
     */
    public T find(float x, float y, float width, float height, Boolf<T> out){
        if(!leaf){
            T result;
            if(topLeft.bounds.overlaps(x, y, width, height) && (result = topLeft.find(x, y, width, height, out)) != null) return result;
            if(topRight.bounds.overlaps(x, y, width, height) && (result = topRight.find(x, y, width, height, out)) != null) return result;
            if(botLeft.bounds.overlaps(x, y, width, height) && (result = botLeft.find(x, y, width, height, out)) != null) return result;
            if(botRight.bounds.overlaps(x, y, width, height)&& (result = botRight.find(x, y, width, height, out)) != null) return result;
        }

        Ar<?> objects = this.objects;

        for(int i = 0; i < objects.size; i++){
            T item = (T)objects.items[i];
            hitbox(item);
            if(tmp.overlaps(x, y, width, height) && out.get(item)){
                return item;
            }
        }
        return null;
    }

    /**
     * @return whether an object overlaps this rectangle. 是否有对象与此矩形重叠。
     * This will never result in false positives.
     */
    public boolean any(float x, float y, float width, float height){
        if(!leaf){
            if(topLeft.bounds.overlaps(x, y, width, height) && topLeft.any(x, y, width, height)) return true;
            if(topRight.bounds.overlaps(x, y, width, height) && topRight.any(x, y, width, height)) return true;
            if(botLeft.bounds.overlaps(x, y, width, height) && botLeft.any(x, y, width, height)) return true;
            if(botRight.bounds.overlaps(x, y, width, height) && botRight.any(x, y, width, height))return true;
        }

        Ar<?> objects = this.objects;

        for(int i = 0; i < objects.size; i++){
            T item = (T)objects.items[i];
            hitbox(item);
            if(tmp.overlaps(x, y, width, height)){
                return true;
            }
        }
        return false;
    }

    /**
     * Processes objects that may intersect the given rectangle.
     * <p>
     * This will never result in false positives.
     * <p>
     * 处理可能与给定矩形相交的对象。 <p> 这绝不会产生误报。
     */
    public void intersect(Rect rect, Cons<T> out){
        intersect(rect.x, rect.y, rect.width, rect.height, out);
    }

    /**
     * Fills the out parameter with any objects that may intersect the given rectangle.
     * <p>
     * This will result in false positives, but never a false negative.
     * <p>
     * 用可能与给定矩形相交的对象填充 out 参数。 <p> 这可能产生误报,但绝不会漏报。
     */
    public void intersect(Rect toCheck, Ar<T> out){
        intersect(toCheck.x, toCheck.y, toCheck.width, toCheck.height, out);
    }

    /**
     * Fills the out parameter with any objects that may intersect the given rectangle.
     * 用可能与给定矩形相交的对象填充 out 参数。
     */
    public void intersect(float x, float y, float width, float height, Ar<T> out){
        if(!leaf){
            if(topLeft.bounds.overlaps(x, y, width, height)) topLeft.intersect(x, y, width, height, out);
            if(topRight.bounds.overlaps(x, y, width, height)) topRight.intersect(x, y, width, height, out);
            if(botLeft.bounds.overlaps(x, y, width, height)) botLeft.intersect(x, y, width, height, out);
            if(botRight.bounds.overlaps(x, y, width, height)) botRight.intersect(x, y, width, height, out);
        }

        Ar<?> objects = this.objects;

        for(int i = 0; i < objects.size; i++){
            T item = (T)objects.items[i];
            hitbox(item);
            if(tmp.overlaps(x, y, width, height)){
                out.add(item);
            }
        }
    }

    /**
     * Adds all quadtree objects to the specified Ar.
     * 将所有四叉树对象添加到指定的 Ar 中。
     */
    public void getObjects(Ar<T> out){
        out.addAll(objects);

        if(!leaf){
            topLeft.getObjects(out);
            topRight.getObjects(out);
            botLeft.getObjects(out);
            botRight.getObjects(out);
        }
    }

    protected QuadTree<T> newChild(Rect rect){
        return new QuadTree<>(rect);
    }

    protected void hitbox(T t){
        t.hitbox(tmp);
    }

    /**
     * Represents an object in a QuadTree.
     * 表示四叉树中的一个对象。
     */
    public interface QuadTreeObject{
        /**
         * Fills the out parameter with this element's rough bounding box. This should never be smaller than the actual object, but may be larger.
         * 用此元素的粗略包围盒填充 out 参数。它不应小于实际对象,但可能更大。
         */
        void hitbox(Rect out);
    }
}