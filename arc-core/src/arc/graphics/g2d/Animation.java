package arc.graphics.g2d;

import arc.struct.Ar;
import arc.math.Mathf;

/**
 * <p>
 * An Animation stores a list of objects representing an animated sequence, e.g. for running or jumping. Each
 * object in the Animation is called a key frame, and multiple key frames make up the animation.
 * <p>
 * The animation's type is the class representing a frame of animation. For example, a typical 2D animation could be made
 * up of {@link arc.graphics.g2d.TextureRegion TextureRegions} and would be specified as:
 * <p><code>Animation&lt;TextureRegion&gt; myAnimation = new Animation&lt;TextureRegion&gt;(...);</code>
 * <p>
 * <p> Animation 存储表示一段动画序列的对象列表,例如奔跑或跳跃。Animation 中的每个对象称为关键帧(key frame),多个关键帧构成动画。 <p> 动画的类型即表示动画帧的类。例如,典型的 2D 动画可由 {@link arc.graphics.g2d.TextureRegion TextureRegions} 组成,声明如下: <p><code>Animation&lt;TextureRegion&gt; myAnimation = new Animation&lt;TextureRegion&gt;(...);</code>
 * @author mzechner
 */
public class Animation<T>{
    /**
     * Length must not be modified without updating {@link #animationDuration}. See {@link #setKeyFrames(T[])}.
     * 修改长度时必须同步更新 {@link #animationDuration}。见 {@link #setKeyFrames(T[])}。
     */
    T[] keyFrames;
    private float frameDuration;
    private float animationDuration;
    private int lastFrameNumber;
    private float lastStateTime;
    private PlayMode playMode = PlayMode.normal;

    /**
     * Constructor, storing the frame duration and key frames.
     * <p>
     * 构造函数,保存帧时长和关键帧。
     * @param frameDuration the time between frames in seconds. 帧间隔时间(秒)。
     * @param keyFrames the objects representing the frames. If this Array is type-aware, {@link #getKeyFrames()} can return the
     * correct type of array. Otherwise, it returns an Object[]. 表示各帧的对象。若此 Array 是类型感知的,{@link #getKeyFrames()} 可返回正确类型的数组,否则返回 Object[]。
     */
    @SuppressWarnings("unchecked")
    public Animation(float frameDuration, Ar<? extends T> keyFrames){
        this.frameDuration = frameDuration;
        Class arrayType = keyFrames.items.getClass().getComponentType();
        T[] frames = (T[])java.lang.reflect.Array.newInstance(arrayType, keyFrames.size);
        for(int i = 0, n = keyFrames.size; i < n; i++){
            frames[i] = keyFrames.get(i);
        }
        setKeyFrames(frames);
    }

    /**
     * Constructor, storing the frame duration and key frames.
     * <p>
     * 构造函数,保存帧时长和关键帧。
     * @param frameDuration the time between frames in seconds. 帧间隔时间(秒)。
     * @param keyFrames the objects representing the frames. If this Array is type-aware, {@link #getKeyFrames()} can
     * return the correct type of array. Otherwise, it returns an Object[]. 表示各帧的对象。若此 Array 是类型感知的,{@link #getKeyFrames()} 可返回正确类型的数组,否则返回 Object[]。
     */
    public Animation(float frameDuration, Ar<? extends T> keyFrames, PlayMode playMode){
        this(frameDuration, keyFrames);
        setPlayMode(playMode);
    }

    /**
     * Constructor, storing the frame duration and key frames.
     * <p>
     * 构造函数,保存帧时长和关键帧。
     * @param frameDuration the time between frames in seconds. 帧间隔时间(秒)。
     * @param keyFrames the objects representing the frames. 表示各帧的对象。
     */
    public Animation(float frameDuration, T... keyFrames){
        this.frameDuration = frameDuration;
        setKeyFrames(keyFrames);
    }

    /**
     * Returns a frame based on the so called state time. This is the amount of seconds an object has spent in the
     * state this Animation instance represents, e.g. running, jumping and so on. The mode specifies whether the animation is
     * looping or not.
     * <p>
     * 基于所谓状态时间返回一帧。状态时间是对象处于此 Animation 实例所表示状态(如奔跑、跳跃等)的秒数。mode 指定动画是否循环。
     * @param stateTime the time spent in the state represented by this animation. 处于此动画所表示状态的时长。
     * @param looping whether the animation is looping or not. 动画是否循环。
     * @return the frame of animation for the given state time. 给定状态时间对应的动画帧。
     */
    public T getKeyFrame(float stateTime, boolean looping){
        // we set the play mode by overriding the previous mode based on looping
        // 根据是否循环覆盖之前的模式来设置播放模式
        // parameter value
        // 参数值
        PlayMode oldPlayMode = playMode;
        if(looping && (playMode == PlayMode.normal || playMode == PlayMode.reversed)){
            if(playMode == PlayMode.normal)
                playMode = PlayMode.loop;
            else
                playMode = PlayMode.loopReversed;
        }else if(!looping && !(playMode == PlayMode.normal || playMode == PlayMode.reversed)){
            if(playMode == PlayMode.loopReversed)
                playMode = PlayMode.reversed;
            else
                playMode = PlayMode.loop;
        }

        T frame = getKeyFrame(stateTime);
        playMode = oldPlayMode;
        return frame;
    }

    /**
     * Returns a frame based on the so called state time. This is the amount of seconds an object has spent in the
     * state this Animation instance represents, e.g. running, jumping and so on using the mode specified by
     * {@link #setPlayMode(PlayMode)} method.
     * <p>
     * 基于所谓状态时间返回一帧。状态时间是对象处于此 Animation 实例所表示状态(如奔跑、跳跃等)的秒数,使用 {@link #setPlayMode(PlayMode)} 方法指定的模式。
     * @return the frame of animation for the given state time. 给定状态时间对应的动画帧。
     */
    public T getKeyFrame(float stateTime){
        int frameNumber = getKeyFrameIndex(stateTime);
        return keyFrames[frameNumber];
    }

    /**
     * Returns the current frame number.
     * <p>
     * 返回当前帧号。
     * @return current frame number 当前帧号
     */
    public int getKeyFrameIndex(float stateTime){
        if(keyFrames.length == 1) return 0;

        int frameNumber = (int)(stateTime / frameDuration);
        switch(playMode){
            case normal:
                frameNumber = Math.min(keyFrames.length - 1, frameNumber);
                break;
            case loop:
                frameNumber = frameNumber % keyFrames.length;
                break;
            case loopPingPong:
                frameNumber = frameNumber % ((keyFrames.length * 2) - 2);
                if(frameNumber >= keyFrames.length)
                    frameNumber = keyFrames.length - 2 - (frameNumber - keyFrames.length);
                break;
            case loopRandom:
                int lastFrameNumber = (int)((lastStateTime) / frameDuration);
                if(lastFrameNumber != frameNumber){
                    frameNumber = Mathf.random(keyFrames.length - 1);
                }else{
                    frameNumber = this.lastFrameNumber;
                }
                break;
            case reversed:
                frameNumber = Math.max(keyFrames.length - frameNumber - 1, 0);
                break;
            case loopReversed:
                frameNumber = frameNumber % keyFrames.length;
                frameNumber = keyFrames.length - frameNumber - 1;
                break;
        }

        lastFrameNumber = frameNumber;
        lastStateTime = stateTime;

        return frameNumber;
    }

    /**
     * Returns the keyframes[] array where all the frames of the animation are stored.
     * <p>
     * 返回存储动画所有帧的 keyframes[] 数组。
     * @return The keyframes[] field. This array is an Object[] if the animation was instantiated with an Array that was not
     * type-aware. keyframes[] 字段。若动画是用非类型感知的 Array 实例化的,则该数组为 Object[]。
     */
    public T[] getKeyFrames(){
        return keyFrames;
    }

    protected void setKeyFrames(T... keyFrames){
        this.keyFrames = keyFrames;
        this.animationDuration = keyFrames.length * frameDuration;
    }

    /**
     * Returns the animation play mode.
     * 返回动画播放模式。
     */
    public PlayMode getPlayMode(){
        return playMode;
    }

    /**
     * Sets the animation play mode.
     * <p>
     * 设置动画播放模式。
     * @param playMode The animation {@link PlayMode} to use. 要使用的动画 {@link PlayMode}。
     */
    public void setPlayMode(PlayMode playMode){
        this.playMode = playMode;
    }

    /**
     * Whether the animation would be finished if played without looping (PlayMode#NORMAL), given the state time.
     * <p>
     * 给定状态时间,若以不循环方式(PlayMode#NORMAL)播放,动画是否已结束。
     * @return whether the animation is finished. 动画是否已结束。
     */
    public boolean isAnimationFinished(float stateTime){
        int frameNumber = (int)(stateTime / frameDuration);
        return keyFrames.length - 1 < frameNumber;
    }

    /**
     * @return the duration of a frame in seconds
     * @return the duration of a frame in seconds 单帧时长(秒)
     */
    public float getFrameDuration(){
        return frameDuration;
    }

    /**
     * Sets duration a frame will be displayed.
     * <p>
     * 设置单帧显示时长。
     * @param frameDuration in seconds 以秒为单位
     */
    public void setFrameDuration(float frameDuration){
        this.frameDuration = frameDuration;
        this.animationDuration = keyFrames.length * frameDuration;
    }

    /**
     * @return the duration of the entire animation, number of frames times frame duration, in seconds
     * @return the duration of the entire animation, number of frames times frame duration, in seconds 整个动画的时长,即帧数乘以帧时长(秒)
     */
    public float getAnimationDuration(){
        return animationDuration;
    }

    /**
     * Defines possible playback modes for an {@link Animation}.
     * 定义 {@link Animation} 的可选播放模式。
     */
    public enum PlayMode{
        normal,
        reversed,
        loop,
        loopReversed,
        loopPingPong,
        loopRandom,
    }
}
