package arc.scene;

import arc.scene.actions.DelayAction;
import arc.scene.actions.TemporalAction;
import arc.util.pooling.Pool;
import arc.util.pooling.Pool.Poolable;

/**
 * Actions attach to an {@link Element} and perform some task, often over time.
 * <p>
 * 动作附加到 {@link Element} 上并执行某些任务,通常是随时间执行。
 * @author Nathan Sweet
 */
abstract public class Action implements Poolable{
    /**
     * The actor this action is attached to, or null if it is not attached.
     * 此动作附加到的元素,若未附加则为 null。
     */
    protected Element actor;

    /**
     * The actor this action targets, or null if a target has not been set.
     * 此动作作用的目标元素,若尚未设置目标则为 null。
     */
    protected Element target;

    private Pool<Action> pool;

    /**
     * Updates the action based on time. Typically this is called each frame by {@link Element#act(float)}.
     * <p>
     * 基于时间更新该动作。通常由 {@link Element#act(float)} 每帧调用。
     * @param delta Time in seconds since the last frame. 距上一帧的时间,单位为秒。
     * @return true if the action is done. This method may continue to be called after the action is done. 若动作已完成则返回 true。动作完成后此方法仍可能被继续调用。
     */
    abstract public boolean act(float delta);

    /**
     * Sets the state of the action so it can be run again.
     * 设置动作的状态,使其可以再次运行。
     */
    public void restart(){
    }

    /** @return null if the action is not attached to an actor. 若 action 未附加到元素则返回 null。 */
    public Element getActor(){
        return actor;
    }

    /**
     * Sets the actor this action is attached to. This also sets the {@link #setTarget(Element) target} actor if it is null. This
     * method is called automatically when an action is added to an actor. This method is also called with null when an action is
     * removed from an actor.
     * <p>
     * When set to null, if the action has a {@link #setPool(Pool) pool} then the action is {@link Pool#free(Object) returned} to
     * the pool (which calls {@link #reset()}) and the pool is set to null. If the action does not have a pool, {@link #reset()} is
     * not called.
     * <p>
     * This method is not typically a good place for an action subclass to query the actor's state because the action may not be
     * executed for some time, eg it may be {@link DelayAction delayed}. The actor's state is best queried in the first call to
     * {@link #act(float)}. For a {@link TemporalAction}, use TemporalAction#begin().
     * <p>
     * 设置此动作附加到的元素。若其为 null,这也会设置 {@link #setTarget(Element) 目标} 元素。当动作被添加到元素时会自动调用此方法。当动作从元素中移除时,也会以 null 调用此方法。 <p> 当设置为 null 时,若此动作拥有 {@link #setPool(Pool) 池},则动作会被 {@link Pool#free(Object) 归还} 到池中(该操作会调用 {@link #reset()}),并将池设为 null。若动作没有池,则不会调用 {@link #reset()}。 <p> 对动作子类而言,此方法通常不适合查询元素的状态,因为动作可能一段时间后才执行,例如它可能被 {@link DelayAction 延迟}。元素状态最好在第一次调用 {@link #act(float)} 时查询。对于 {@link TemporalAction},请使用 TemporalAction#begin()。
     */
    public void setActor(Element actor){
        this.actor = actor;
        if(target == null) setTarget(actor);
        if(actor == null){
            if(pool != null){
                pool.free(this);
                pool = null;
            }
        }
    }

    /** @return null if the action has no target. 若 action 无目标则返回 null。 */
    public Element getTarget(){
        return target;
    }

    /**
     * Sets the actor this action will manipulate. If no target actor is set, {@link #setActor(Element)} will set the target actor
     * when the action is added to an actor.
     * <p>
     * 设置此动作将要操作的目标元素。若未设置目标元素,则当动作被添加到元素时,{@link #setActor(Element)} 会设置目标元素。
     */
    public void setTarget(Element target){
        this.target = target;
    }

    /**
     * Resets the optional state of this action to as if it were newly created, allowing the action to be pooled and reused. State
     * required to be set for every usage of this action or computed during the action does not need to be reset.
     * <p>
     * The default implementation calls {@link #restart()}.
     * <p>
     * If a subclass has optional state, it must override this method, call super, and reset the optional state.
     * <p>
     * 将此动作的可选状态重置为如同刚创建的状态,使动作可以被池化并复用。每次使用该动作都必须设置的状态或在动作执行期间计算出的状态无需重置。 <p> 默认实现调用 {@link #restart()}。 <p> 若子类拥有可选状态,则必须重写此方法,调用 super,并重置可选状态。
     */
    @Override
    public void reset(){
        actor = null;
        target = null;
        pool = null;
        restart();
    }

    public Pool<Action> getPool(){
        return pool;
    }

    /**
     * Sets the pool that the action will be returned to when removed from the actor.
     * <p>
     * 设置动作从元素上移除时将被归还到的池。
     * @param pool May be null. 可为 null。
     * @see #setActor(Element)
     */
    @SuppressWarnings("unchecked")
    public void setPool(Pool pool){
        this.pool = pool;
    }

    @Override
    public String toString(){
        String name = super.toString().split("@")[0];
        int dotIndex = name.lastIndexOf('.');
        if(dotIndex != -1) name = name.substring(dotIndex + 1);
        if(name.endsWith("Action")) name = name.substring(0, name.length() - 6);
        return name;
    }
}
