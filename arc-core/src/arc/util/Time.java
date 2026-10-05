package arc.util;

import arc.*;
import arc.func.*;
import arc.struct.*;
import arc.util.Timer.*;

public class Time{
    /**
     * Conversion factors for ticks to other unit values.
     * tick 到其他单位值的换算系数。
     */
    public static final float toSeconds = 60f, toMinutes = 60f * 60f, toHours = 60f * 60f * 60f;

    /**
     * Global delta value. Do not change.
     * 全局增量值。请勿修改。
     */
    public static float delta = 1f;
    /**
     * Global time value. Do not change.
     * 全局时间值。请勿修改。
     */
    public static float globalTime;
    private static double globalTimeRaw;

    public static final long nanosPerMilli = 1000000;

    private static LongAr marks = new LongAr();
    private static Floatp deltaimpl = () -> Math.min(Core.graphics.getDeltaTime() * 60f, 3f);

    /**
     * Runs a task with a delay of several ticks. Unless the application is closed, this task will always complete.
     * 延迟若干 tick 后运行任务。除非应用程序关闭,该任务总会完成。
     */
    public static Task runTask(float delay, Runnable r){
        return Timer.schedule(r, delay / 60f);
    }

    public static void mark(){
        marks.add(nanos());
    }

    /**
     * A value of -1 means mark() wasn't called beforehand.
     * 值为 -1 表示之前没有调用过 mark()。
     */
    public static float elapsed(){
        if(marks.size == 0){
            return -1;
        }else{
            return timeSinceNanos(marks.pop()) / 1000000f;
        }
    }

    public static void updateGlobal(){
        globalTimeRaw += Core.graphics.getDeltaTime()*60f;
        delta = deltaimpl.get();
        globalTime = (float)globalTimeRaw;
    }

    public static void setDeltaProvider(Floatp impl){
        deltaimpl = impl;
        delta = impl.get();
    }

    /** @return The current value of the system timer, in nanoseconds. 系统计时器的当前值,以纳秒为单位。 */
    public static long nanos(){
        return System.nanoTime();
    }

    /** @return the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC. 当前时间与 1970 年 1 月 1 日 UTC 午夜之间相差的毫秒数。 */
    public static long millis(){
        return System.currentTimeMillis();
    }

    /**
     * Convert nanoseconds time to milliseconds
     * <p>
     * 将纳秒时间转换为毫秒
     * @param nanos must be nanoseconds 必须是纳秒
     * @return time value in milliseconds 以毫秒为单位的时间值
     */
    public static long nanosToMillis(long nanos){
        return nanos / nanosPerMilli;
    }

    /**
     * Convert milliseconds time to nanoseconds
     * <p>
     * 将毫秒时间转换为纳秒
     * @param millis must be milliseconds 必须是毫秒
     * @return time value in nanoseconds 以纳秒为单位的时间值
     */
    public static long millisToNanos(long millis){
        return millis * nanosPerMilli;
    }

    /**
     * Get the time in nanos passed since a previous time
     * <p>
     * 获取自上一个时间点以来经过的时间(纳秒)
     * @param prevTime - must be nanoseconds 必须是纳秒
     * @return - time passed since prevTime in nanoseconds 自 prevTime 以来经过的时间(纳秒)
     */
    public static long timeSinceNanos(long prevTime){
        return nanos() - prevTime;
    }

    /**
     * Get the time in millis passed since a previous time
     * <p>
     * 获取自上一个时间点以来经过的时间(毫秒)
     * @param prevTime - must be milliseconds 必须是毫秒
     * @return - time passed since prevTime in milliseconds 自 prevTime 以来经过的时间(毫秒)
     */
    public static long timeSinceMillis(long prevTime){
        return millis() - prevTime;
    }

}
