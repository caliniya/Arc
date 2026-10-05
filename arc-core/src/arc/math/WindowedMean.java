package arc.math;

import java.util.*;

/**
 * A simple class keeping track of the mean of a stream of values within a certain window. the WindowedMean will only return a
 * value in case enough data has been sampled. After enough data has been sampled the oldest sample will be replaced by the newest
 * in case a new sample is added.
 * <p>
 * 一个跟踪一定窗口内值流均值的简单类。只有在采样了足够数据后 WindowedMean 才会返回值。采样足够数据后,添加新样本时最旧的样本将被最新样本替换。
 * @author badlogicgames@gmail.com
 */
public final class WindowedMean{
    float[] values;
    int addedValues = 0;
    int lastValue;
    float mean = 0;
    boolean dirty = true;

    /**
     * constructor, windowSize specifies the number of samples we will continuously get the mean and variance from. the class
     * will only return meaning full values if at least windowSize values have been added.
     * <p>
     * 构造函数,windowSize 指定持续获取均值和方差的样本数量。只有在至少添加了 windowSize 个值后,该类才会返回有意义的结果。
     * @param windowSize size of the sample window 采样窗口的大小
     */
    public WindowedMean(int windowSize){
        values = new float[windowSize];
    }

    public void reset(){
        addedValues = 0;
        lastValue = 0;
        mean = 0;
    }

    public float get(int index){
        return values[Mathf.mod(index + lastValue, values.length)];
    }

    /**
     * @return whether the value returned will be meaningful
     * 返回的值是否有意义
     */
    public boolean hasEnoughData(){
        return addedValues >= values.length;
    }

    /**
     * clears this WindowedMean. The class will only return meaningful values after enough data has been added again.
     * 清空此 WindowedMean。只有在再次添加足够数据后,该类才会返回有意义的值。
     */
    public void clear(){
        addedValues = 0;
        lastValue = 0;
        Arrays.fill(values, 0);
        dirty = true;
    }

    public void fill(float value){
        dirty = true;
        Arrays.fill(values, value);
        addedValues = values.length;
    }

    /**
     * adds a new sample to this mean. In case the window is full the oldest value will be replaced by this new value.
     * <p>
     * 向此均值添加一个新样本。若窗口已满,最旧的值将被此新值替换。
     * @param value The value to add 要添加的值
     */
    public void add(float value){
        if(addedValues < values.length) addedValues++;
        values[lastValue++] = value;
        if(lastValue > values.length - 1) lastValue = 0;
        dirty = true;
    }

    /**
     * returns the mean of the samples added to this instance. Only returns meaningful results when at least window_size samples
     * as specified in the constructor have been added.
     * <p>
     * 返回添加到此实例的样本的均值。只有添加了至少构造函数中指定的 window_size 个样本后,才会返回有意义的结果。
     * @return the mean 均值
     */
    public float mean(){
        if(hasEnoughData()){
            if(dirty){
                float mean = 0;
                for(int i = 0; i < values.length; i++)
                    mean += values[i];

                this.mean = mean / values.length;
                dirty = false;
            }
            return this.mean;
        }else return 0;
    }

    /**
     * @return raw mean; can be used before this window has enough data.
     * 原始均值;可在窗口数据不足时使用。
     */
    public float rawMean(){
        if(hasEnoughData()){
            return mean();
        }else if(addedValues == 0){
            return 0;
        }else{
            float sum = 0f;
            for(int i = 0; i < lastValue; i++){
                sum += values[i];
            }
            return sum / addedValues;
        }
    }

    /**
     * @return the oldest value in the window
     * 窗口中最早的值
     */
    public float oldest(){
        return addedValues < values.length ? values[0] : values[lastValue];
    }

    /**
     * @return the value last added
     * 最后添加的值
     */
    public float latest(){
        return values[lastValue - 1 == -1 ? values.length - 1 : lastValue - 1];
    }

    /**
     * @return The standard deviation
     * 标准差
     */
    public float standardDeviation(){
        if(!hasEnoughData()) return 0;

        float mean = mean();
        float sum = 0;
        for(int i = 0; i < values.length; i++){
            sum += (values[i] - mean) * (values[i] - mean);
        }

        return (float)Math.sqrt(sum / values.length);
    }

    public float lowest(){
        float lowest = Float.MAX_VALUE;
        for(int i = 0; i < values.length; i++)
            lowest = Math.min(lowest, values[i]);
        return lowest;
    }

    public float highest(){
        float lowest = Float.MIN_NORMAL;
        for(int i = 0; i < values.length; i++)
            lowest = Math.max(lowest, values[i]);
        return lowest;
    }

    public int getCount(){
        return addedValues;
    }

    public int getWindowSize(){
        return values.length;
    }

    /**
     * @return A new <code>float[]</code> containing all values currently in the window of the stream, in order from oldest to 一个新的 <code>float[]</code>,按从旧到新的顺序包含当前流窗口中的所有值
     * latest. The length of the array is smaller than the window size if not enough data has been added.
     */
    public float[] getWindowValues(){
        float[] windowValues = new float[addedValues];
        if(hasEnoughData()){
            for(int i = 0; i < windowValues.length; i++){
                windowValues[i] = values[(i + lastValue) % values.length];
            }
        }else{
            System.arraycopy(values, 0, windowValues, 0, addedValues);
        }
        return windowValues;
    }
}
