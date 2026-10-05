package arc.math;

/**
 * Track properties of a stream of float values. The properties (total value, minimum, etc) are updated as values are
 * {@link #put(float)} into the stream.
 * <p>
 * 跟踪浮点值流的属性。当值被 {@link #put(float)} 加入流时,这些属性(总数值、最小值等)会随之更新。
 * @author xoppa
 */
public class FloatCounter{
    /**
     * Provides access to the WindowedMean if any (can be null)
     * 提供对 WindowedMean 的访问(可为 null)
     */
    public final WindowedMean mean;
    /**
     * The amount of values added
     * 已添加的值的数量
     */
    public int count;
    /**
     * The sum of all values
     * 所有值的总和
     */
    public float total;
    /**
     * The smallest value
     * 最小值
     */
    public float min;
    /**
     * The largest value
     * 最大值
     */
    public float max;
    /**
     * The average value (total / count)
     * 平均值(total / count)
     */
    public float average;
    /**
     * The latest raw value
     * 最近的原始值
     */
    public float latest;
    /**
     * The current windowed mean value
     * 当前窗口均值
     */
    public float value;

    /**
     * Construct a new FloatCounter
     * <p>
     * 构造一个新的 FloatCounter
     * @param windowSize The size of the mean window or 1 or below to not use a windowed mean. 平均值窗口的大小,或 1 及以下表示不使用窗口均值。
     */
    public FloatCounter(int windowSize){
        mean = (windowSize > 1) ? new WindowedMean(windowSize) : null;
        reset();
    }

    /**
     * Add a value and update all fields.
     * <p>
     * 添加一个值并更新所有字段。
     * @param value The value to add 要添加的值
     */
    public void put(float value){
        latest = value;
        total += value;
        count++;
        average = total / count;

        if(mean != null){
            mean.add(value);
            this.value = mean.mean();
        }else
            this.value = latest;

        if(mean == null || mean.hasEnoughData()){
            if(this.value < min) min = this.value;
            if(this.value > max) max = this.value;
        }
    }

    /**
     * Reset all values to their default value.
     * 将所有值重置为默认值。
     */
    public void reset(){
        count = 0;
        total = 0f;
        min = Float.MAX_VALUE;
        max = Float.MIN_VALUE;
        average = 0f;
        latest = 0f;
        value = 0f;
        if(mean != null) mean.clear();
    }
}
