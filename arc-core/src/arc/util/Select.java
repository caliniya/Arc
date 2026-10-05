package arc.util;

import java.util.Comparator;

/**
 * This class is for selecting a ranked element (kth ordered statistic) from an unordered list in faster time than sorting the
 * whole array. Typical applications include finding the nearest enemy unit(s), and other operations which are likely to run as
 * often as every x frames. Certain values of k will result in a partial sorting of the Array.
 * <p>
 * The lowest ranking element starts at 1, not 0. 1 = first, 2 = second, 3 = third, etc. calling with a value of zero will result
 * in a {@link ArcRuntimeException}
 * </p>
 * <p>
 * This class uses very minimal extra memory, as it makes no copies of the array. The underlying algorithms used are a naive
 * single-pass for k=min and k=max, and Hoare's quickselect for values in between.
 * </p>
 * <p>
 * 此类用于从无序列表中选出按排名的元素(第 k 顺序统计量),比对整个数组排序更快。典型应用包括查找最近的敌方单位,以及其他可能每 x 帧就要运行一次的操作。某些 k 值会导致数组被部分排序。
 * <p>
 * 最低排名从 1 开始,而不是 0。1 = 第一,2 = 第二,3 = 第三,依此类推。传入 0 会抛出 {@link ArcRuntimeException}。
 * </p>
 * <p>
 * 此类只使用极少的额外内存,因为它不复制数组。所用的底层算法为:k 取 min 或 max 时使用朴素的单趟选择,介于两者之间时使用 Hoare 快速选择。
 * </p>
 * @author Jon Renner
 */
public class Select{
    private static Select instance;
    private QuickSelect quickSelect;

    /**
     * Provided for convenience
     * 为方便使用而提供。
     */
    public static Select instance(){
        if(instance == null) instance = new Select();
        return instance;
    }

    public <T> T select(T[] items, Comparator<T> comp, int kthLowest, int size){
        int idx = selectIndex(items, comp, kthLowest, size);
        return items[idx];
    }

    @SuppressWarnings("unchecked")
    public <T> int selectIndex(T[] items, Comparator<T> comp, int kthLowest, int size){
        if(size < 1){
            throw new ArcRuntimeException("cannot select from empty array (size < 1)");
        }else if(kthLowest > size){
            throw new ArcRuntimeException("Kth rank is larger than size. k: " + kthLowest + ", size: " + size);
        }
        int idx;
        // naive partial selection sort almost certain to outperform quickselect where n is min or max
        // 当 n 为 min 或 max 时,朴素的部分选择排序几乎必定优于快速选择
        if(kthLowest == 1){
            // find min
            // 查找最小值
            idx = fastMin(items, comp, size);
        }else if(kthLowest == size){
            // find max
            // 查找最大值
            idx = fastMax(items, comp, size);
        }else{
            // quickselect a better choice for cases of k between min and max
            // 当 k 介于 min 和 max 之间时,快速选择是更好的选择
            if(quickSelect == null) quickSelect = new QuickSelect();
            idx = quickSelect.select(items, comp, kthLowest, size);
        }
        return idx;
    }

    /**
     * Faster than quickselect for n = min
     * 当 n = min 时比快速选择更快。
     */
    private <T> int fastMin(T[] items, Comparator<T> comp, int size){
        int lowestIdx = 0;
        for(int i = 1; i < size; i++){
            int comparison = comp.compare(items[i], items[lowestIdx]);
            if(comparison < 0){
                lowestIdx = i;
            }
        }
        return lowestIdx;
    }

    /**
     * Faster than quickselect for n = max
     * 当 n = max 时比快速选择更快。
     */
    private <T> int fastMax(T[] items, Comparator<T> comp, int size){
        int highestIdx = 0;
        for(int i = 1; i < size; i++){
            int comparison = comp.compare(items[i], items[highestIdx]);
            if(comparison > 0){
                highestIdx = i;
            }
        }
        return highestIdx;
    }
}
