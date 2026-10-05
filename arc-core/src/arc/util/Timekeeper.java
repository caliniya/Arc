package arc.util;

/**
 * Keeps track of a time interval.
 * 跟踪一个时间间隔。
 */
public class Timekeeper{
    private final long intervalMs;
    private long lastTime;

    Timekeeper(long ms){
        intervalMs = ms;
    }

    public static Timekeeper ofMillis(long ms){
        return new Timekeeper(ms);
    }

    public static Timekeeper ofTicks(float ticks){
        return ofSeconds(ticks / 60f);
    }

    public static Timekeeper ofSeconds(float seconds){
        return new Timekeeper((long)(seconds * 1000));
    }

    /** @return true if the interval has passed since the last reset(); resets the timer if true 若自上次 reset() 以来间隔已经过去则返回 true;为 true 时重置计时器 */
    public boolean poll(){
        boolean result = get();
        if(result) reset();
        return result;
    }

    /** @return true if the interval has passed since the last reset(). 若自上次 reset() 以来间隔已经过去则返回 true。 */
    public boolean get(){
        return Time.timeSinceMillis(lastTime) > intervalMs;
    }

    /**
     * resets the timer; the interval will need to pass until get() returns true again.
     * 重置计时器;需要再经过一个间隔,get() 才会再次返回 true。
     */
    public void reset(){
        lastTime = Time.millis();
    }
}
