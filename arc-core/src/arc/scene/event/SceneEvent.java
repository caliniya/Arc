package arc.scene.event;

import arc.scene.Element;
import arc.scene.Scene;
import arc.util.pooling.Pool.Poolable;

/**
 * The base class for all events.
 * <p>
 * By default an event will "bubble" up through an element's parent's handlers.
 * <p>
 * An element's capture listeners can {@link #stop()} an event to prevent child elements from seeing it.
 * <p>
 * An Event may be marked as "handled" which will end its propagation outside of the Stage (see {@link #handle()}). The default
 * {@link Element#fire(SceneEvent)} will mark events handled if an {@link EventListener} returns true.
 * <p>
 * A cancelled event will be stopped and handled. Additionally, many elements will undo the side-effects of a canceled event. (See
 * {@link #cancel()}.)
 * <p>
 * 所有事件的基类。默认情况下,事件会沿着元素的父级处理器逐层向上“冒泡”(bubble)。元素的捕获监听器可以对事件调用 {@link #stop()},以阻止子元素看到它。事件可以被标记为“已处理”(handled),这会结束其在舞台之外的传播(参见 {@link #handle()})。如果某个 {@link EventListener} 返回 true,默认的 {@link Element#fire(SceneEvent)} 会将事件标记为已处理。被取消的事件会被停止并标记为已处理。此外,许多元素会撤销被取消事件产生的副作用。(参见 {@link #cancel()}。)
 * @see InputEvent
 * @see Element#fire(SceneEvent)
 */
public class SceneEvent implements Poolable{
    public Element targetActor;
    public Element listenerActor;

    public boolean capture; // true means event occurred during the capture phase
    // true 表示事件发生在捕获阶段
    public boolean bubbles = true; // true means propagate to target's parents
    // true 表示向目标元素的父级传播
    public boolean handled; // true means the event was handled (the stage will eat the input)
    // true 表示事件已被处理(舞台将吞掉该输入)
    public boolean stopped; // true means event propagation was stopped
    // true 表示事件的传播已被停止
    public boolean cancelled; // true means propagation was stopped and any action that this event would cause should not happen
    // true 表示传播已被停止,且此事件本应引发的动作不应发生

    /**
     * Marks this event as handled. This does not affect event propagation inside scene2d, but causes the {@link Scene} event
     * methods to return true, which will eat the event so it is not passed on to the application under the stage.
     * <p>
     * 将此事件标记为已处理。这不会影响 scene2d 内部的事件传播,但会使 {@link Scene} 的事件方法返回 true,从而吞掉该事件,使其不会被继续传递给舞台之下的应用程序。
     */
    public void handle(){
        handled = true;
    }

    /**
     * Marks this event cancelled. This {@link #handle() handles} the event and {@link #stop() stops} the event propagation. It
     * also cancels any default action that would have been taken by the code that fired the event. Eg, if the event is for a
     * checkbox being checked, cancelling the event could uncheck the checkbox.
     * <p>
     * 将此事件标记为已取消。这会 {@link #handle() handled 处理} 该事件并 {@link #stop() stopped 停止} 事件的传播。它还会取消触发事件的代码原本会执行的默认动作。例如,如果事件对应于复选框被勾选,取消该事件就可以取消勾选该复选框。
     */
    public void cancel(){
        cancelled = true;
        stopped = true;
        handled = true;
    }

    /**
     * Marks this event has being stopped. This halts event propagation.
     * 将此事件标记为已停止。这会中止事件的传播。
     */
    public void stop(){
        stopped = true;
    }

    @Override
    public void reset(){
        targetActor = null;
        listenerActor = null;
        capture = false;
        bubbles = true;
        handled = false;
        stopped = false;
        cancelled = false;
    }

}
