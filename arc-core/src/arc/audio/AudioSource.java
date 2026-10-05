package arc.audio;

import arc.*;
import arc.util.*;

import static arc.audio.Soloud.*;

public abstract class AudioSource implements Disposable{
    protected long handle;

    protected int maxConcurrent, concurrentGroup;
    protected float minInterruptAbsolute, minInterruptFraction;
    protected float priority;

    public boolean valid(){
        return handle != 0;
    }

    public void setFilter(int index, @Nullable AudioFilter filter){
        if(handle == 0) return;
        sourceFilter(handle, index, filter == null ? 0 : filter.handle);
    }

    public void setFilter(@Nullable AudioFilter filter){
        setFilter(0, filter);
    }

    protected void setParamsAfterLoad(){
        if(priority != 0f) setPriority(priority);
        if(maxConcurrent != 0) setMaxConcurrent(maxConcurrent);
        if(concurrentGroup != 0) setConcurrentGroup(concurrentGroup);
        if(minInterruptFraction != 0f){
            setMinConcurrentInterruptFraction(minInterruptAbsolute, minInterruptFraction);
        }else if(minInterruptAbsolute != 0f){
            setMinConcurrentInterrupt(minInterruptAbsolute);
        }
    }

    /**
     * Sets the priority of this source. Sources with higher priorities will not get cut off by those of lower priorities.
     * 设置此声源的优先级。优先级较高的声源不会被优先级较低的声源中断。
     */
    public void setPriority(float priority){
        this.priority = priority;
        if(handle == 0) return;
        sourcePriority(handle, priority);
    }

    /**
     * Sets the priority of this source. Sources with higher priorities will not get cut off by those of lower priorities.
     * 设置此声源的优先级。优先级较高的声源不会被优先级较低的声源中断。
     */
    public void setMaxConcurrent(int max){
        this.maxConcurrent = max;
        if(handle == 0) return;
        sourceMaxConcurrent(handle, max);
    }

    /**
     * Sets the group ID of this source, for which maxConcurrent will be enforced. If unset, a unique group will be created for this sound.
     * 设置此声源的组 ID,将对该组强制执行 maxConcurrent 限制。如果未设置,将为此音效创建一个唯一的组。
     */
    public void setConcurrentGroup(int group){
        this.concurrentGroup = group;
        if(handle == 0) return;
        sourceConcurrentGroup(handle, group);
    }

    /**
     * Sets the minimum playtime (in seconds) that a sound must have in order to be interrupted when its concurrent limit is reached.
     * 设置音效在达到并发上限时可被中断所需的最短播放时长(秒)。
     */
    public void setMinConcurrentInterrupt(float seconds){
        minInterruptAbsolute = seconds;
        minInterruptFraction = 0f;
        if(handle == 0) return;
        sourceMinConcurrentInterrupt(handle, seconds);
    }

    /**
     * Sets the minimum playtime (in seconds) that a sound must have in order to be interrupted when its concurrent limit is reached. This is a fraction of length.
     * 设置音效在达到并发上限时可被中断所需的最短播放时长(秒)。这是 length 的一个比例。
     */
    public void setMinConcurrentInterruptFraction(float min, float fraction){
        minInterruptFraction = fraction;
        minInterruptAbsolute = min;
        if(handle == 0) return;
        sourceMinConcurrentInterrupt(handle, Math.min(min, getLength() * fraction));
    }

    /**
     * @return number of currently playing instances 当前正在播放的实例数量
     */
    public int countPlaying(){
        if(handle == 0) return  0;
        return Core.audio.countPlaying(this);
    }

    public void setSingleInstance(boolean singleInstance){
        if(handle == 0 || !Core.audio.initialized) return;
        sourceSingleInstance(handle, singleInstance);
    }

    public void stop(){
        if(handle == 0) return;
        sourceStop(handle);
    }

    public abstract float getLength();

    /**
     * @return true if this is a lazily loaded source (only loaded once played) 如果这是延迟加载的声源(仅在播放时才加载)则为 true
     */
    public boolean isLazy(){
        return false;
    }

    @Override
    public void dispose(){
        if(handle != 0) sourceDestroy(handle);
        handle = 0;
    }
}
