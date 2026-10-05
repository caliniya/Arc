package arc.math;

import arc.struct.Ar;

/**
 * This class represents a cumulative distribution.
 * It can be used in scenarios where there are values with different probabilities
 * and it's required to pick one of those respecting the probability.
 * For example one could represent the frequency of the alphabet letters using a cumulative distribution
 * and use it to randomly pick a letter respecting their probabilities (useful when generating random words).
 * Another example could be point generation on a mesh surface: one could generate a cumulative distribution using
 * triangles areas as interval size, in this way triangles with a large area will be picked more often than triangles with a smaller one.
 * See <a href="http://en.wikipedia.org/wiki/Cumulative_distribution_function">Wikipedia</a> for a detailed explanation.
 * <p>
 * 此类表示累积分布。可用于存在不同概率的值且需要按概率选取其中一个的场景。例如可以用累积分布表示字母表中字母的频率,并据此按概率随机选取字母(生成随机单词时很有用)。另一个例子是网格表面上的点生成:可以用三角形面积作为区间大小生成累积分布,这样面积大的三角形会比面积小的更常被选中。详细说明参见 <a href="http://en.wikipedia.org/wiki/Cumulative_distribution_function">Wikipedia</a>。
 * @author Inferno
 */
public class CumulativeDistribution<T>{
    private Ar<CumulativeValue> values;

    public CumulativeDistribution(){
        values = new Ar<>(false, 10, CumulativeValue.class);
    }

    /**
     * Adds a value with a given interval size to the distribution
     * 向分布中添加一个具有给定区间大小的值
     */
    public void add(T value, float intervalSize){
        values.add(new CumulativeValue(value, 0, intervalSize));
    }

    /**
     * Adds a value with interval size equal to zero to the distribution
     * 向分布中添加一个区间大小为零的值
     */
    public void add(T value){
        values.add(new CumulativeValue(value, 0, 0));
    }

    /**
     * Generate the cumulative distribution
     * 生成累积分布
     */
    public void generate(){
        float sum = 0;
        for(int i = 0; i < values.size; ++i){
            sum += values.items[i].interval;
            values.items[i].frequency = sum;
        }
    }

    /**
     * Generate the cumulative distribution in [0,1] where each interval will get a frequency between [0,1]
     * 生成 [0,1] 范围内的累积分布,每个区间获得 [0,1] 之间的频率
     */
    public void generateNormalized(){
        float sum = 0;
        for(int i = 0; i < values.size; ++i){
            sum += values.items[i].interval;
        }
        float intervalSum = 0;
        for(int i = 0; i < values.size; ++i){
            intervalSum += values.items[i].interval / sum;
            values.items[i].frequency = intervalSum;
        }
    }

    /**
     * Generate the cumulative distribution in [0,1] where each value will have the same frequency and interval size
     * 生成 [0,1] 范围内的累积分布,每个值具有相同的频率和区间大小
     */
    public void generateUniform(){
        float freq = 1f / values.size;
        for(int i = 0; i < values.size; ++i){
            //reset the interval to the normalized frequency
            // 将区间重置为归一化频率
            values.items[i].interval = freq;
            values.items[i].frequency = (i + 1) * freq;
        }
    }

    /**
     * Finds the value whose interval contains the given probability
     * Binary search algorithm is used to find the value.
     * <p>
     * 查找其区间包含给定概率的值 使用二分查找算法查找该值。
     * @return the value whose interval contains the probability 其区间包含该概率的值
     */
    public T value(float probability){
        CumulativeValue value;
        int imax = values.size - 1, imin = 0, imid;
        while(imin <= imax){
            imid = imin + ((imax - imin) / 2);
            value = values.items[imid];
            if(probability < value.frequency)
                imax = imid - 1;
            else if(probability > value.frequency)
                imin = imid + 1;
            else break;
        }

        return values.items[imin].value;
    }

    /**
     * @return the value whose interval contains a random probability in [0,1]
     * 其区间包含 [0,1] 内随机概率的值
     */
    public T value(){
        return value(Mathf.random());
    }

    /**
     * @return the amount of values
     * 值的数量
     */
    public int size(){
        return values.size;
    }

    /**
     * @return the interval size for the value at the given position
     * 给定位置处值的区间大小
     */
    public float getInterval(int index){
        return values.items[index].interval;
    }

    /**
     * @return the value at the given position
     * 给定位置处的值
     */
    public T getValue(int index){
        return values.items[index].value;
    }

    /**
     * Set the interval size on the passed in object.
     * The object must be present in the distribution.
     * <p>
     * 设置传入对象的区间大小。该对象必须已存在于分布中。
     */
    public void setInterval(T obj, float intervalSize){
        for(CumulativeValue value : values)
            if(value.value == obj){
                value.interval = intervalSize;
                return;
            }
    }

    /**
     * Sets the interval size for the value at the given index
     * 设置给定索引处值的区间大小
     */
    public void setInterval(int index, float intervalSize){
        values.items[index].interval = intervalSize;
    }

    /**
     * Removes all the values from the distribution
     * 移除分布中的所有值
     */
    public void clear(){
        values.clear();
    }

    public class CumulativeValue{
        public T value;
        public float frequency;
        public float interval;

        public CumulativeValue(T value, float frequency, float interval){
            this.value = value;
            this.frequency = frequency;
            this.interval = interval;
        }
    }
}
