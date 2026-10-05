package arc.util;

import java.util.Comparator;

/**
 * Implementation of Tony Hoare's quickselect algorithm. Running time is generally O(n), but worst case is O(n^2) Pivot choice is
 * median of three method, providing better performance than a random pivot for partially sorted data.
 * http://en.wikipedia.org/wiki/Quickselect
 * <p>
 * Tony Hoare 快速选择(quickselect)算法的实现。运行时间通常为 O(n),最坏情况为 O(n^2)。基准选取采用三者取中法,对部分有序的数据比随机基准性能更好。
 * http://en.wikipedia.org/wiki/Quickselect
 * @author Jon Renner
 */
public class QuickSelect<T>{
    private T[] array;
    private Comparator<? super T> comp;

    public int select(T[] items, Comparator<T> comp, int n, int size){
        this.array = items;
        this.comp = comp;
        return recursiveSelect(0, size - 1, n);
    }

    private int partition(int left, int right, int pivot){
        T pivotValue = array[pivot];
        swap(right, pivot);
        int storage = left;
        for(int i = left; i < right; i++){
            if(comp.compare(array[i], pivotValue) < 0){
                swap(storage, i);
                storage++;
            }
        }
        swap(right, storage);
        return storage;
    }

    private int recursiveSelect(int left, int right, int k){
        if(left == right) return left;
        int pivotIndex = medianOfThreePivot(left, right);
        int pivotNewIndex = partition(left, right, pivotIndex);
        int pivotDist = (pivotNewIndex - left) + 1;
        int result;
        if(pivotDist == k){
            result = pivotNewIndex;
        }else if(k < pivotDist){
            result = recursiveSelect(left, pivotNewIndex - 1, k);
        }else{
            result = recursiveSelect(pivotNewIndex + 1, right, k - pivotDist);
        }
        return result;
    }

    /**
     * Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays
     * 三者取中法可能优于随机基准,对部分有序的数组尤其如此
     */
    private int medianOfThreePivot(int leftIdx, int rightIdx){
        T left = array[leftIdx];
        int midIdx = (leftIdx + rightIdx) / 2;
        T mid = array[midIdx];
        T right = array[rightIdx];

        // spaghetti median of three algorithm
        // 朴素的三者取中算法实现
        // does at most 3 comparisons
        // 最多进行 3 次比较
        if(comp.compare(left, mid) > 0){
            if(comp.compare(mid, right) > 0){
                return midIdx;
            }else if(comp.compare(left, right) > 0){
                return rightIdx;
            }else{
                return leftIdx;
            }
        }else{
            if(comp.compare(left, right) > 0){
                return leftIdx;
            }else if(comp.compare(mid, right) > 0){
                return rightIdx;
            }else{
                return midIdx;
            }
        }
    }

    private void swap(int left, int right){
        T tmp = array[left];
        array[left] = array[right];
        array[right] = tmp;
    }
}
