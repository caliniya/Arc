package arc;


import arc.struct.Ar;
import arc.struct.ObjectMap;
import arc.func.Cons;

/**
 * Simple global event listener system.
 * 简单的全局事件监听器系统。
 */
@SuppressWarnings("unchecked")
public class Events{
    private static final ObjectMap<Object, Ar<Cons<?>>> events = new ObjectMap<>();

    /**
     * Handle an event by class.
     * 按类处理一个事件。
     */
    public static <T> void on(Class<T> type, Cons<T> listener){
        events.get(type, () -> new Ar<>(Cons.class)).add(listener);
    }

    /**
     * Handle an event by enum trigger.
     * 按枚举触发器处理一个事件。
     */
    public static void run(Object type, Runnable listener){
        events.get(type, () -> new Ar<>(Cons.class)).add(e -> listener.run());
    }

    /**
     * Only use this method if you have the reference to the exact listener object that was used.
     * 仅当你持有当时所用的监听器对象的确切引用时才使用此方法。
     */
    public static <T> boolean remove(Class<T> type, Cons<T> listener){
        return events.get(type, () -> new Ar<>(Cons.class)).remove(listener);
    }

    /**
     * Fires an enum trigger.
     * 触发一个枚举事件。
     */
    public static <T extends Enum<T>> void fire(Enum<T> type){
        Ar<Cons<?>> listeners = events.get(type);

        if(listeners != null){
            int len = listeners.size;
            Cons[] items = listeners.items;
            for(int i = 0; i < len; i++){
                items[i].get(type);
            }
        }
    }

    /**
     * Fires a non-enum event by class.
     * 按类触发一个非枚举事件。
     */
    public static <T> void fire(T type){
        fire(type.getClass(), type);
    }

    public static <T> void fire(Class<?> ctype, T type){
        Ar<Cons<?>> listeners = events.get(ctype);

        if(listeners != null){
            int len = listeners.size;
            Cons[] items = listeners.items;
            for(int i = 0; i < len; i++){
                items[i].get(type);
            }
        }
    }

    /**
     * Don't do this.
     * 不要这样做。
     */
    public static void clear(){
        events.clear();
    }
}
