/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package arc.struct;

import arc.util.*;

import java.util.Comparator;

/**
 * Provides methods to sort arrays of objects. Sorting requires working memory and this class allows that memory to be reused to
 * avoid allocation. The sorting is otherwise identical to the Arrays.sort methods (uses timsort).<br>
 * <br>
 * Note that sorting primitive arrays with the Arrays.sort methods does not allocate memory (unless sorting large arrays of char,
 * short, or byte).
 * <p>
 * 提供排序对象数组的方法。排序需要工作内存,该类允许复用这部分内存以避免分配。除此之外,排序与 Arrays.sort 方法完全相同(使用 timsort)。<br> <br> 注意,用 Arrays.sort 方法对基本类型数组排序不会分配内存(除非排序大型 char、short 或 byte 数组)。
 * @author Nathan Sweet
 */
public class Sort{
    private static ThreadLocal<Sort> instance = Threads.local(Sort::new);

    private TimSort timSort;
    private ComparableTimSort comparableTimSort;

    /**
     * Returns a Sort instance for convenience. Multiple threads must not use this instance at the same time.
     * 为方便起见返回一个 Sort 实例。多个线程不得同时使用该实例。
     */
    public static Sort instance(){
        return instance.get();
    }

    public <T> void sort(Ar<T> a){
        if(comparableTimSort == null) comparableTimSort = new ComparableTimSort();
        comparableTimSort.doSort(a.items, 0, a.size);
    }

    public <T> void sort(T[] a){
        if(comparableTimSort == null) comparableTimSort = new ComparableTimSort();
        comparableTimSort.doSort(a, 0, a.length);
    }

    public <T> void sort(T[] a, int fromIndex, int toIndex){
        if(comparableTimSort == null) comparableTimSort = new ComparableTimSort();
        comparableTimSort.doSort(a, fromIndex, toIndex);
    }

    public <T> void sort(Ar<T> a, Comparator<? super T> c){
        if(timSort == null) timSort = new TimSort();
        timSort.doSort(a.items, c, 0, a.size);
    }

    public <T> void sort(T[] a, Comparator<? super T> c){
        if(timSort == null) timSort = new TimSort();
        timSort.doSort(a, c, 0, a.length);
    }

    public <T> void sort(T[] a, Comparator<? super T> c, int fromIndex, int toIndex){
        if(timSort == null) timSort = new TimSort();
        timSort.doSort(a, c, fromIndex, toIndex);
    }
}
