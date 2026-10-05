package arc.util;

/**
 * Keeps track of X actions in Y units of time.
 * 跟踪 Y 个时间单位内的 X 次操作。
 */
public class Ratekeeper{
    public int occurences;
    public long lastTime;

    /**
     * @return whether an action is allowed. 是否允许执行操作。
     * @param spacing the spacing between action chunks in milliseconds 操作块之间的间隔(毫秒)
     * @param cap the maximum amount of actions per chunk 每个操作块允许的最大操作次数
     * */
    public boolean allow(long spacing, int cap){
        if(Time.timeSinceMillis(lastTime) > spacing){
            occurences = 0;
            lastTime = Time.millis();
        }

        occurences ++;
        return occurences <= cap;
    }

    public void reset(){
        occurences = 0;
        lastTime = 0;
    }
}
