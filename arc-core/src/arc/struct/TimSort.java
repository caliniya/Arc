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

import java.util.Arrays;
import java.util.Comparator;

/**
 * A stable, adaptive, iterative mergesort that requires far fewer than n lg(n) comparisons when running on partially sorted
 * arrays, while offering performance comparable to a traditional mergesort when run on random arrays. Like all proper mergesorts,
 * this sort is stable and runs O(n log n) time (worst case). In the worst case, this sort requires temporary storage space for
 * n/2 object references; in the best case, it requires only a small constant amount of space.
 * <p>
 * This implementation was adapted from Tim Peters's list sort for Python, which is described in detail here:
 * <p>
 * http://svn.python.org/projects/python/trunk/Objects/listsort.txt
 * <p>
 * Tim's C code may be found here:
 * <p>
 * http://svn.python.org/projects/python/trunk/Objects/listobject.c
 * <p>
 * The underlying techniques are described in this paper (and may have even earlier origins):
 * <p>
 * "Optimistic Sorting and Information Theoretic Complexity" Peter McIlroy SODA (Fourth Annual ACM-SIAM Symposium on Discrete
 * Algorithms), pp 467-474, Austin, Texas, 25-27 January 1993.
 * <p>
 * While the API to this class consists solely of static methods, it is (privately) instantiable; a TimSort instance holds the
 * state of an ongoing sort, assuming the input array is large enough to warrant the full-blown TimSort. Small arrays are sorted
 * in place, using a binary insertion sort.
 * <p>
 * 一种稳定的自适应迭代归并排序,在部分有序的数组上运行时所需的比较次数远少于 n lg(n),而在随机数组上运行时性能可与传统归并排序媲美。与所有正规的归并排序一样,该排序是稳定的,时间复杂度为 O(n log n)(最坏情况)。最坏情况下,该排序需要 n/2 个对象引用的临时存储空间;最好情况下,只需要很小的常量空间。<p> 本实现改编自 Tim Peters 为 Python 编写的列表排序,详细描述见:<p> http://svn.python.org/projects/python/trunk/Objects/listsort.txt <p> Tim 的 C 代码见:<p> http://svn.python.org/projects/python/trunk/Objects/listobject.c <p> 底层技术在以下论文中描述(可能起源更早):<p> "Optimistic Sorting and Information Theoretic Complexity" Peter McIlroy SODA (Fourth Annual ACM-SIAM Symposium on Discrete Algorithms), pp 467-474, Austin, Texas, 25-27 January 1993. <p> 虽然该类的 API 仅由静态方法组成,但它(在私有层面)是可以实例化的;TimSort 实例保存一次正在进行的排序的状态,前提是输入数组足够大,值得使用完整的 TimSort。小数组使用二分插入排序就地排序。
 */
class TimSort<T>{
    /**
     * This is the minimum sized sequence that will be merged. Shorter sequences will be lengthened by calling binarySort. If the
     * entire array is less than this length, no merges will be performed.
     * <p>
     * This constant should be a power of two. It was 64 in Tim Peter's C implementation, but 32 was empirically determined to work
     * better in this implementation. In the unlikely event that you set this constant to be a number that's not a power of two,
     * you'll need to change the {@link #minRunLength} computation.
     * <p>
     * If you decrease this constant, you must change the stackLen computation in the TimSort constructor, or you risk an
     * ArrayOutOfBounds exception. See listsort.txt for a discussion of the minimum stack length required as a function of the
     * length of the array being sorted and the minimum merge sequence length.
     * <p>
     * 这是要合并的最小序列长度。更短的序列将通过调用 binarySort 加长。如果整个数组小于此长度,则不会执行合并。<p> 此常量应为 2 的幂。在 Tim Peter 的 C 实现中它是 64,但经实验确定 32 在此实现中效果更好。万一你将此常量设为非 2 的幂的数,则需要更改 {@link #minRunLength} 的计算。<p> 如果你减小此常量,必须更改 TimSort 构造函数中的 stackLen 计算,否则会有 ArrayOutOfBounds 异常的风险。关于所需最小栈长度与待排序数组长度及最小合并序列长度之间关系的讨论,见 listsort.txt。
     */
    private static final int MIN_MERGE = 32;
    /**
     * When we get into galloping mode, we stay there until both runs win less often than MIN_GALLOP consecutive times.
     * 进入飞奔(galloping)模式后,我们会一直保持该模式,直到两个区段连续获胜的次数都少于 MIN_GALLOP 次。
     */
    private static final int MIN_GALLOP = 7;
    /**
     * Maximum initial size of tmp array, which is used for merging. The array can grow to accommodate demand.
     * <p>
     * Unlike Tim's original C version, we do not allocate this much storage when sorting smaller arrays. This change was required
     * for performance.
     * <p>
     * 用于归并的 tmp 数组的最大初始大小。该数组可以增长以满足需求。<p> 与 Tim 的原始 C 版本不同,排序较小的数组时我们不会分配这么多存储空间。此改动是出于性能考虑。
     */
    private static final int INITIAL_TMP_STORAGE_LENGTH = 256;
    /**
     * Asserts have been placed in if-statements for performance. To enable them, set this field to true and enable them in VM with
     * a command line flag. If you modify this class, please do test the asserts!
     * <p>
     * 出于性能考虑,断言被放在 if 语句中。要启用它们,请将此字段设为 true,并在 VM 中通过命令行标志开启。如果你修改了这个类,请务必测试这些断言!
     */
    private static final boolean DEBUG = false;
    private final int[] runBase;
    private final int[] runLen;
    /**
     * The array being sorted.
     * 正在排序的数组。
     */
    private T[] a;
    /**
     * The comparator for this sort.
     * 此排序所用的比较器。
     */
    private Comparator<? super T> c;
    /**
     * This controls when we get *into* galloping mode. It is initialized to MIN_GALLOP. The mergeLo and mergeHi methods nudge it
     * higher for random data, and lower for highly structured data.
     * <p>
     * 控制何时*进入*飞奔(galloping)模式。初始化为 MIN_GALLOP。mergeLo 和 mergeHi 方法会针对随机数据将其调高,针对高度结构化的数据将其调低。
     */
    private int minGallop = MIN_GALLOP;
    /**
     * Temp storage for merges.
     * 用于归并的临时存储。
     */
    private T[] tmp; // Actual runtime type will be Object[], regardless of T
    // 无论 T 是什么,实际运行时类型都是 Object[]
    private int tmpCount;
    /**
     * A stack of pending runs yet to be merged. Run i starts at address base[i] and extends for len[i] elements. It's always true
     * (so long as the indices are in bounds) that:
     * <p>
     * runBase[i] + runLen[i] == runBase[i + 1]
     * <p>
     * so we could cut the storage for this, but it's a minor amount, and keeping all the info explicit simplifies the code.
     * <p>
     * 待合并区段(pending runs)的栈。区段 i 从地址 base[i] 开始,延伸 len[i] 个元素。只要索引在边界内,以下等式总是成立:<p> runBase[i] + runLen[i] == runBase[i + 1] <p> 因此本可以削减其存储,但那只是很小的空间,保留所有显式信息可以简化代码。
     */
    private int stackSize = 0; // Number of pending runs on stack
    // 栈上待处理区段的数量

    TimSort(){
        tmp = (T[])new Object[INITIAL_TMP_STORAGE_LENGTH];
        runBase = new int[40];
        runLen = new int[40];
    }

    /**
     * Creates a TimSort instance to maintain the state of an ongoing sort.
     * <p>
     * 创建一个 TimSort 实例,以维护一次正在进行的排序的状态。
     * @param a the array to be sorted 要排序的数组
     * @param c the comparator to determine the order of the sort 决定排序顺序的比较器
     */
    private TimSort(T[] a, Comparator<? super T> c){
        this.a = a;
        this.c = c;

        // Allocate temp storage (which may be increased later if necessary)
        // 分配临时存储(之后必要时可以增加)
        int len = a.length;
        tmp = (T[])new Object[len < 2 * INITIAL_TMP_STORAGE_LENGTH ? len >>> 1 : INITIAL_TMP_STORAGE_LENGTH];

        /*
         * Allocate runs-to-be-merged stack (which cannot be expanded). The stack length requirements are described in listsort.txt.
         * The C version always uses the same stack length (85), but this was measured to be too expensive when sorting "mid-sized"
         * arrays (e.g., 100 elements) in Java. Therefore, we use smaller (but sufficiently large) stack lengths for smaller arrays.
         * The "magic numbers" in the computation below must be changed if MIN_MERGE is decreased. See the MIN_MERGE declaration
         * above for more information.
         * 分配待合并区段(runs)的栈(不可扩容)。栈长度的需求在 listsort.txt 中有描述。C 版本总是使用相同的栈长度(85),但实测在 Java 中排序“中等规模”的数组(如 100 个元素)时代价过高。因此,对较小的数组使用更小(但足够大)的栈长度。如果减小 MIN_MERGE,下方计算中的“魔法数字”必须随之修改。更多信息见上方 MIN_MERGE 的声明。
         */
        int stackLen = (len < 120 ? 5 : len < 1542 ? 10 : len < 119151 ? 19 : 40);
        runBase = new int[stackLen];
        runLen = new int[stackLen];
    }

    static <T> void sort(T[] a, Comparator<? super T> c){
        sort(a, 0, a.length, c);
    }

    /*
     * The next two methods (which are package private and static) constitute the entire API of this class. Each of these methods
     * obeys the contract of the public method with the same signature in java.util.Arrays.
     * 接下来的两个方法(包私有且静态)构成了这个类的全部 API。每个方法都遵守 java.util.Arrays 中相同签名的公共方法的约定。
     */

    static <T> void sort(T[] a, int lo, int hi, Comparator<? super T> c){
        if(c == null){
            Arrays.sort(a, lo, hi);
            return;
        }

        rangeCheck(a.length, lo, hi);
        int nRemaining = hi - lo;
        if(nRemaining < 2) return; // Arrays of size 0 and 1 are always sorted
        // 大小为 0 和 1 的数组总是有序的

        // If array is small, do a "mini-TimSort" with no merges
        // 如果数组很小,则执行不带合并的“迷你 TimSort”
        if(nRemaining < MIN_MERGE){
            int initRunLen = countRunAndMakeAscending(a, lo, hi, c);
            binarySort(a, lo, hi, lo + initRunLen, c);
            return;
        }

        /** March over the array once, left to right, finding natural runs, extending short natural runs to minRun elements, and
         * merging runs to maintain stack invariant. 
         * <p>
         * 从左到右对数组进行一次扫描,找出自然区段,将短的自然区段扩展到 minRun 个元素,并合并区段以维持栈不变式。
         */
        TimSort<T> ts = new TimSort<>(a, c);
        int minRun = minRunLength(nRemaining);
        do{
            // Identify next run
            // 确定下一个区段
            int runLen = countRunAndMakeAscending(a, lo, hi, c);

            // If run is short, extend to min(minRun, nRemaining)
            // 如果区段较短,则扩展到 min(minRun, nRemaining)
            if(runLen < minRun){
                int force = nRemaining <= minRun ? nRemaining : minRun;
                binarySort(a, lo, lo + force, lo + runLen, c);
                runLen = force;
            }

            // Push run onto pending-run stack, and maybe merge
            // 将区段压入待合并区段栈,并可能进行合并
            ts.pushRun(lo, runLen);
            ts.mergeCollapse();

            // Advance to find next run
            // 继续前进以找出下一个区段
            lo += runLen;
            nRemaining -= runLen;
        }while(nRemaining != 0);

        // Merge all remaining runs to complete sort
        // 合并所有剩余区段以完成排序
        assert !DEBUG || lo == hi;
        ts.mergeForceCollapse();
        assert !DEBUG || ts.stackSize == 1;
    }

    /**
     * Sorts the specified portion of the specified array using a binary insertion sort. This is the best method for sorting small
     * numbers of elements. It requires O(n log n) compares, but O(n^2) data movement (worst case).
     * <p>
     * If the initial part of the specified range is already sorted, this method can take advantage of it: the method assumes that
     * the elements from index {@code lo}, inclusive, to {@code start}, exclusive are already sorted.
     * <p>
     * 使用二分插入排序对指定数组的指定部分进行排序。这是对少量元素排序的最佳方法。它需要 O(n log n) 次比较,但数据移动为 O(n^2)(最坏情况)。<p> 如果指定范围的初始部分已经有序,此方法可以利用这一点:该方法假定从索引 {@code lo}(含)到 {@code start}(不含)的元素已经有序。
     * @param a the array in which a range is to be sorted 要在其中排序的数组
     * @param lo the index of the first element in the range to be sorted 要排序范围的第一个元素的索引
     * @param hi the index after the last element in the range to be sorted 要排序范围的最后一个元素之后的索引
     * @param start the index of the first element in the range that is not already known to be sorted (@code lo <= start <= hi} 范围内第一个尚未确定有序的元素的索引(@code lo <= start <= hi}
     * @param c comparator to used for the sort 用于排序的比较器
     */
    @SuppressWarnings("fallthrough")
    private static <T> void binarySort(T[] a, int lo, int hi, int start, Comparator<? super T> c){
        assert !DEBUG || lo <= start && start <= hi;
        if(start == lo) start++;
        for(; start < hi; start++){
            T pivot = a[start];

            // Set left (and right) to the index where a[start] (pivot) belongs
            // 将 left(和 right)设为 a[start](即 pivot)所属的索引
            int left = lo;
            int right = start;
            assert !DEBUG || left <= right;
            /*
             * Invariants: pivot >= all in [lo, left). pivot < all in [right, start).
             * 不变式:pivot >= [lo, left) 中的所有元素;pivot < [right, start) 中的所有元素。
             */
            while(left < right){
                int mid = (left + right) >>> 1;
                if(c.compare(pivot, a[mid]) < 0)
                    right = mid;
                else
                    left = mid + 1;
            }
            assert !DEBUG || left == right;

            /*
             * The invariants still hold: pivot >= all in [lo, left) and pivot < all in [left, start), so pivot belongs at left. Note
             * that if there are elements equal to pivot, left points to the first slot after them -- that's why this sort is stable.
             * Slide elements over to make room for pivot.
             * 不变式仍然成立:pivot >= [lo, left) 中的所有元素且 pivot < [left, start) 中的所有元素,因此 pivot 应位于 left 处。注意,如果存在与 pivot 相等的元素,left 指向它们之后的第一个槽位——这正是该排序稳定的原因。滑动元素为 pivot 腾出空间。
             */
            int n = start - left; // The number of elements to move
            // 要移动的元素数量
            // Switch is just an optimization for arraycopy in default case
            // switch 只是默认情况下对 arraycopy 的一个优化
            switch(n){
                case 2:
                    a[left + 2] = a[left + 1];
                case 1:
                    a[left + 1] = a[left];
                    break;
                default:
                    System.arraycopy(a, left, a, left + 1, n);
            }
            a[left] = pivot;
        }
    }

    /**
     * Returns the length of the run beginning at the specified position in the specified array and reverses the run if it is
     * descending (ensuring that the run will always be ascending when the method returns).
     * <p>
     * A run is the longest ascending sequence with:
     * <p>
     * a[lo] <= a[lo + 1] <= a[lo + 2] <= ...
     * <p>
     * or the longest descending sequence with:
     * <p>
     * a[lo] > a[lo + 1] > a[lo + 2] > ...
     * <p>
     * For its intended use in a stable mergesort, the strictness of the definition of "descending" is needed so that the call can
     * safely reverse a descending sequence without violating stability.
     * <p>
     * 返回指定数组中从指定位置开始的区段(run)的长度,如果该区段是降序的则将其反转(确保方法返回时该区段总是升序)。<p> 区段是最长的升序序列:<p> a[lo] <= a[lo + 1] <= a[lo + 2] <= ... <p> 或最长的降序序列:<p> a[lo] > a[lo + 1] > a[lo + 2] > ... <p> 为了在稳定归并排序中的预期用途,需要“降序”定义的严格性,以便调用可以安全地反转降序序列而不破坏稳定性。
     * @param a the array in which a run is to be counted and possibly reversed 要统计区段并可能反转的数组
     * @param lo index of the first element in the run 区段中第一个元素的索引
     * @param hi index after the last element that may be contained in the run. It is required that @code{lo < hi}. 区段中可能包含的最后一个元素之后的索引。要求 @code{lo < hi}。
     * @param c the comparator to used for the sort 用于排序的比较器
     * @return the length of the run beginning at the specified position in the specified array 指定数组中从指定位置开始的区段的长度
     */
    private static <T> int countRunAndMakeAscending(T[] a, int lo, int hi, Comparator<? super T> c){
        assert !DEBUG || lo < hi;
        int runHi = lo + 1;
        if(runHi == hi) return 1;

        // Find end of run, and reverse range if descending
        // 找出区段的末尾,如果为降序则反转该范围
        if(c.compare(a[runHi++], a[lo]) < 0){ // Descending
        // 降序
            while(runHi < hi && c.compare(a[runHi], a[runHi - 1]) < 0)
                runHi++;
            reverseRange(a, lo, runHi);
        }else{ // Ascending
        // 升序
            while(runHi < hi && c.compare(a[runHi], a[runHi - 1]) >= 0)
                runHi++;
        }

        return runHi - lo;
    }

    /**
     * Reverse the specified range of the specified array.
     * <p>
     * 反转指定数组的指定范围。
     * @param a the array in which a range is to be reversed 要反转其中范围的数组
     * @param lo the index of the first element in the range to be reversed 要反转范围的第一个元素的索引
     * @param hi the index after the last element in the range to be reversed 要反转范围的最后一个元素之后的索引
     */
    private static void reverseRange(Object[] a, int lo, int hi){
        hi--;
        while(lo < hi){
            Object t = a[lo];
            a[lo++] = a[hi];
            a[hi--] = t;
        }
    }

    /**
     * Returns the minimum acceptable run length for an array of the specified length. Natural runs shorter than this will be
     * extended with {@link #binarySort}.
     * <p>
     * Roughly speaking, the computation is:
     * <p>
     * If n < MIN_MERGE, return n (it's too small to bother with fancy stuff). Else if n is an exact power of 2, return
     * MIN_MERGE/2. Else return an int k, MIN_MERGE/2 <= k <= MIN_MERGE, such that n/k is close to, but strictly less than, an
     * exact power of 2.
     * <p>
     * For the rationale, see listsort.txt.
     * <p>
     * 返回指定长度的数组可接受的最小区段长度。短于该值的自然区段将用 {@link #binarySort} 扩展。<p> 粗略地说,计算如下:<p> 若 n < MIN_MERGE,返回 n(数组太小,不值得使用复杂的处理)。否则若 n 恰好是 2 的幂,返回 MIN_MERGE/2。否则返回一个 int k,满足 MIN_MERGE/2 <= k <= MIN_MERGE,使 n/k 接近但严格小于一个 2 的幂。<p> 理由见 listsort.txt。
     * @param n the length of the array to be sorted 要排序数组的长度
     * @return the length of the minimum run to be merged 要合并的最小区段的长度
     */
    private static int minRunLength(int n){
        assert !DEBUG || n >= 0;
        int r = 0; // Becomes 1 if any 1 bits are shifted off
        // 如果有任何 1 位被移出,则变为 1
        while(n >= MIN_MERGE){
            r |= (n & 1);
            n >>= 1;
        }
        return n + r;
    }

    /**
     * Locates the position at which to insert the specified key into the specified sorted range; if the range contains an element
     * equal to key, returns the index of the leftmost equal element.
     * <p>
     * 在指定的有序范围内定位指定 key 的插入位置;如果范围内存在等于 key 的元素,返回最左侧相等元素的索引。
     * @param key the key whose insertion point to search for 要搜索其插入点的键
     * @param a the array in which to search 要在其中搜索的数组
     * @param base the index of the first element in the range 范围内第一个元素的索引
     * @param len the length of the range; must be > 0 范围的长度;必须 > 0
     * @param hint the index at which to begin the search, 0 <= hint < n. The closer hint is to the result, the faster this method 开始搜索的索引,0 <= hint < n。hint 越接近结果,此方法越快
     * will run.
     * @param c the comparator used to order the range, and to search 用于确定范围顺序并进行搜索的比较器
     * @return the int k, 0 <= k <= n such that a[b + k - 1] < key <= a[b + k], pretending that a[b - 1] is minus infinity and a[b 满足 0 <= k <= n 且 a[b + k - 1] < key <= a[b + k] 的 int k,假定 a[b - 1] 为负无穷且 a[b
     * + n] is infinity. In other words, key belongs at index b + k; or in other words, the first k elements of a should
     * precede key, and the last n - k should follow it.
     */
    private static <T> int gallopLeft(T key, T[] a, int base, int len, int hint, Comparator<? super T> c){
        assert !DEBUG || hint >= 0 && hint < len;
        int lastOfs = 0;
        int ofs = 1;
        if(c.compare(key, a[base + hint]) > 0){
            // Gallop right until a[base+hint+lastOfs] < key <= a[base+hint+ofs]
            // 向右飞奔,直到 a[base+hint+lastOfs] < key <= a[base+hint+ofs]
            int maxOfs = len - hint;
            while(ofs < maxOfs && c.compare(key, a[base + hint + ofs]) > 0){
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if(ofs <= 0) // int overflow
                // int 溢出
                    ofs = maxOfs;
            }
            if(ofs > maxOfs) ofs = maxOfs;

            // Make offsets relative to base
            // 使偏移量相对于 base
            lastOfs += hint;
            ofs += hint;
        }else{ // key <= a[base + hint]
            // Gallop left until a[base+hint-ofs] < key <= a[base+hint-lastOfs]
            // 向左飞奔,直到 a[base+hint-ofs] < key <= a[base+hint-lastOfs]
            final int maxOfs = hint + 1;
            while(ofs < maxOfs && c.compare(key, a[base + hint - ofs]) <= 0){
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if(ofs <= 0) // int overflow
                // int 溢出
                    ofs = maxOfs;
            }
            if(ofs > maxOfs) ofs = maxOfs;

            // Make offsets relative to base
            // 使偏移量相对于 base
            int tmp = lastOfs;
            lastOfs = hint - ofs;
            ofs = hint - tmp;
        }
        assert !DEBUG || -1 <= lastOfs && lastOfs < ofs && ofs <= len;

        /*
         * Now a[base+lastOfs] < key <= a[base+ofs], so key belongs somewhere to the right of lastOfs but no farther right than ofs.
         * Do a binary search, with invariant a[base + lastOfs - 1] < key <= a[base + ofs].
         * 此时 a[base+lastOfs] < key <= a[base+ofs],因此 key 位于 lastOfs 右侧、但不超过 ofs 的某处。进行二分查找,保持不变式 a[base + lastOfs - 1] < key <= a[base + ofs]。
         */
        lastOfs++;
        while(lastOfs < ofs){
            int m = lastOfs + ((ofs - lastOfs) >>> 1);

            if(c.compare(key, a[base + m]) > 0)
                lastOfs = m + 1; // a[base + m] < key
            else
                ofs = m; // key <= a[base + m]
        }
        assert !DEBUG || lastOfs == ofs; // so a[base + ofs - 1] < key <= a[base + ofs]
        // 因此 a[base + ofs - 1] < key <= a[base + ofs]
        return ofs;
    }

    /**
     * Like gallopLeft, except that if the range contains an element equal to key, gallopRight returns the index after the
     * rightmost equal element.
     * <p>
     * 与 gallopLeft 类似,不同之处在于:如果范围内存在等于 key 的元素,gallopRight 返回最右侧相等元素之后的索引。
     * @param key the key whose insertion point to search for 要搜索其插入点的键
     * @param a the array in which to search 要在其中搜索的数组
     * @param base the index of the first element in the range 范围内第一个元素的索引
     * @param len the length of the range; must be > 0 范围的长度;必须 > 0
     * @param hint the index at which to begin the search, 0 <= hint < n. The closer hint is to the result, the faster this method 开始搜索的索引,0 <= hint < n。hint 越接近结果,此方法越快
     * will run.
     * @param c the comparator used to order the range, and to search 用于确定范围顺序并进行搜索的比较器
     * @return the int k, 0 <= k <= n such that a[b + k - 1] <= key < a[b + k] 满足 0 <= k <= n 且 a[b + k - 1] <= key < a[b + k] 的 int k
     */
    private static <T> int gallopRight(T key, T[] a, int base, int len, int hint, Comparator<? super T> c){
        assert !DEBUG || hint >= 0 && hint < len;

        int ofs = 1;
        int lastOfs = 0;
        if(c.compare(key, a[base + hint]) < 0){
            // Gallop left until a[b+hint - ofs] <= key < a[b+hint - lastOfs]
            // 向左飞奔,直到 a[b+hint - ofs] <= key < a[b+hint - lastOfs]
            int maxOfs = hint + 1;
            while(ofs < maxOfs && c.compare(key, a[base + hint - ofs]) < 0){
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if(ofs <= 0) // int overflow
                // int 溢出
                    ofs = maxOfs;
            }
            if(ofs > maxOfs) ofs = maxOfs;

            // Make offsets relative to b
            // 使偏移量相对于 b
            int tmp = lastOfs;
            lastOfs = hint - ofs;
            ofs = hint - tmp;
        }else{ // a[b + hint] <= key
            // Gallop right until a[b+hint + lastOfs] <= key < a[b+hint + ofs]
            // 向右飞奔,直到 a[b+hint + lastOfs] <= key < a[b+hint + ofs]
            int maxOfs = len - hint;
            while(ofs < maxOfs && c.compare(key, a[base + hint + ofs]) >= 0){
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if(ofs <= 0) // int overflow
                // int 溢出
                    ofs = maxOfs;
            }
            if(ofs > maxOfs) ofs = maxOfs;

            // Make offsets relative to b
            // 使偏移量相对于 b
            lastOfs += hint;
            ofs += hint;
        }
        assert !DEBUG || -1 <= lastOfs && lastOfs < ofs && ofs <= len;

        /*
         * Now a[b + lastOfs] <= key < a[b + ofs], so key belongs somewhere to the right of lastOfs but no farther right than ofs.
         * Do a binary search, with invariant a[b + lastOfs - 1] <= key < a[b + ofs].
         * 此时 a[b + lastOfs] <= key < a[b + ofs],因此 key 位于 lastOfs 右侧、但不超过 ofs 的某处。进行二分查找,保持不变式 a[b + lastOfs - 1] <= key < a[b + ofs]。
         */
        lastOfs++;
        while(lastOfs < ofs){
            int m = lastOfs + ((ofs - lastOfs) >>> 1);

            if(c.compare(key, a[base + m]) < 0)
                ofs = m; // key < a[b + m]
            else
                lastOfs = m + 1; // a[b + m] <= key
        }
        assert !DEBUG || lastOfs == ofs; // so a[b + ofs - 1] <= key < a[b + ofs]
        // 因此 a[b + ofs - 1] <= key < a[b + ofs]
        return ofs;
    }

    /**
     * Checks that fromIndex and toIndex are in range, and throws an appropriate exception if they aren't.
     * <p>
     * 检查 fromIndex 和 toIndex 是否在范围内,如果不在则抛出相应的异常。
     * @param arrayLen the length of the array 数组的长度
     * @param fromIndex the index of the first element of the range 范围内第一个元素的索引
     * @param toIndex the index after the last element of the range 范围内最后一个元素之后的索引
     * @throws IllegalArgumentException if fromIndex > toIndex 如果 fromIndex > toIndex
     * @throws ArrayIndexOutOfBoundsException if fromIndex < 0 or toIndex > arrayLen 如果 fromIndex < 0 或 toIndex > arrayLen
     */
    private static void rangeCheck(int arrayLen, int fromIndex, int toIndex){
        if(fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        if(fromIndex < 0) throw new ArrayIndexOutOfBoundsException(fromIndex);
        if(toIndex > arrayLen) throw new ArrayIndexOutOfBoundsException(toIndex);
    }

    public void doSort(T[] a, Comparator<T> c, int lo, int hi){
        stackSize = 0;
        rangeCheck(a.length, lo, hi);
        int nRemaining = hi - lo;
        if(nRemaining < 2) return; // Arrays of size 0 and 1 are always sorted
        // 大小为 0 和 1 的数组总是有序的

        // If array is small, do a "mini-TimSort" with no merges
        // 如果数组很小,则执行不带合并的“迷你 TimSort”
        if(nRemaining < MIN_MERGE){
            int initRunLen = countRunAndMakeAscending(a, lo, hi, c);
            binarySort(a, lo, hi, lo + initRunLen, c);
            return;
        }

        this.a = a;
        this.c = c;
        tmpCount = 0;

        /** March over the array once, left to right, finding natural runs, extending short natural runs to minRun elements, and
         * merging runs to maintain stack invariant. 
         * <p>
         * 从左到右对数组进行一次扫描,找出自然区段,将短的自然区段扩展到 minRun 个元素,并合并区段以维持栈不变式。
         */
        int minRun = minRunLength(nRemaining);
        do{
            // Identify next run
            // 确定下一个区段
            int runLen = countRunAndMakeAscending(a, lo, hi, c);

            // If run is short, extend to min(minRun, nRemaining)
            // 如果区段较短,则扩展到 min(minRun, nRemaining)
            if(runLen < minRun){
                int force = nRemaining <= minRun ? nRemaining : minRun;
                binarySort(a, lo, lo + force, lo + runLen, c);
                runLen = force;
            }

            // Push run onto pending-run stack, and maybe merge
            // 将区段压入待合并区段栈,并可能进行合并
            pushRun(lo, runLen);
            mergeCollapse();

            // Advance to find next run
            // 继续前进以找出下一个区段
            lo += runLen;
            nRemaining -= runLen;
        }while(nRemaining != 0);

        // Merge all remaining runs to complete sort
        // 合并所有剩余区段以完成排序
        assert !DEBUG || lo == hi;
        mergeForceCollapse();
        assert !DEBUG || stackSize == 1;

        this.a = null;
        this.c = null;
        T[] tmp = this.tmp;
        for(int i = 0, n = tmpCount; i < n; i++)
            tmp[i] = null;
    }

    /**
     * Pushes the specified run onto the pending-run stack.
     * <p>
     * 将指定区段压入待合并区段栈。
     * @param runBase index of the first element in the run 区段中第一个元素的索引
     * @param runLen the number of elements in the run 区段中的元素数量
     */
    private void pushRun(int runBase, int runLen){
        this.runBase[stackSize] = runBase;
        this.runLen[stackSize] = runLen;
        stackSize++;
    }

    /**
     * Examines the stack of runs waiting to be merged and merges adjacent runs until the stack invariants are reestablished:
     * <p>
     * 1. runLen[n - 2] > runLen[n - 1] + runLen[n] 2. runLen[n - 1] > runLen[n]
     * <p>
     * where n is the index of the last run in runLen.
     * <p>
     * This method has been formally verified to be correct after checking the last 4 runs.
     * Checking for 3 runs results in an exception for large arrays.
     * (Source: http://envisage-project.eu/proving-android-java-and-python-sorting-algorithm-is-broken-and-how-to-fix-it/)
     * <p>
     * This method is called each time a new run is pushed onto the stack, so the invariants are guaranteed to hold for i <
     * stackSize upon entry to the method.
     * <p>
     * 检查等待合并的区段栈,并合并相邻区段,直到重新建立栈不变式:<p> 1. runLen[n - 2] > runLen[n - 1] + runLen[n] 2. runLen[n - 1] > runLen[n] <p> 其中 n 是 runLen 中最后一个区段的索引。<p> 该方法在检查最后 4 个区段之后已被形式化验证为正确。只检查 3 个区段会在大数组上导致异常。(来源:http://envisage-project.eu/proving-android-java-and-python-sorting-algorithm-is-broken-and-how-to-fix-it/)<p> 每当有新区段入栈时都会调用此方法,因此进入方法时可以保证对 i < stackSize 不变式成立。
     */
    private void mergeCollapse(){
        while(stackSize > 1){
            int n = stackSize - 2;
            if((n >= 1 && runLen[n - 1] <= runLen[n] + runLen[n + 1]) || (n >= 2 && runLen[n - 2] <= runLen[n] + runLen[n - 1])){
                if(runLen[n - 1] < runLen[n + 1]) n--;
            }else if(runLen[n] > runLen[n + 1]){
                break; // Invariant is established
                // 不变式已建立
            }
            mergeAt(n);
        }
    }

    /**
     * Merges all runs on the stack until only one remains. This method is called once, to complete the sort.
     * 合并栈上的所有区段,直到只剩一个。此方法只被调用一次,用于完成排序。
     */
    private void mergeForceCollapse(){
        while(stackSize > 1){
            int n = stackSize - 2;
            if(n > 0 && runLen[n - 1] < runLen[n + 1]) n--;
            mergeAt(n);
        }
    }

    /**
     * Merges the two runs at stack indices i and i+1. Run i must be the penultimate or antepenultimate run on the stack. In other
     * words, i must be equal to stackSize-2 or stackSize-3.
     * <p>
     * 合并栈上索引 i 和 i+1 处的两个区段。区段 i 必须是栈上的倒数第二个或倒数第三个区段。换句话说,i 必须等于 stackSize-2 或 stackSize-3。
     * @param i stack index of the first of the two runs to merge 要合并的两个区段中第一个区段的栈索引
     */
    private void mergeAt(int i){
        assert !DEBUG || stackSize >= 2;
        assert !DEBUG || i >= 0;
        assert !DEBUG || i == stackSize - 2 || i == stackSize - 3;

        int base1 = runBase[i];
        int len1 = runLen[i];
        int base2 = runBase[i + 1];
        int len2 = runLen[i + 1];
        assert !DEBUG || len1 > 0 && len2 > 0;
        assert !DEBUG || base1 + len1 == base2;

        /*
         * Record the length of the combined runs; if i is the 3rd-last run now, also slide over the last run (which isn't involved
         * in this merge). The current run (i+1) goes away in any case.
         * 记录合并后区段的长度;如果 i 现在是倒数第三个区段,还要把最后一个区段(不参与本次合并)滑动过来。当前区段(i+1)无论如何都会消失。
         */
        runLen[i] = len1 + len2;
        if(i == stackSize - 3){
            runBase[i + 1] = runBase[i + 2];
            runLen[i + 1] = runLen[i + 2];
        }
        stackSize--;

        /*
         * Find where the first element of run2 goes in run1. Prior elements in run1 can be ignored (because they're already in
         * place).
         * 找出 run2 的第一个元素在 run1 中的位置。run1 中位于其之前的元素可以忽略(因为它们已就位)。
         */
        int k = gallopRight(a[base2], a, base1, len1, 0, c);
        assert !DEBUG || k >= 0;
        base1 += k;
        len1 -= k;
        if(len1 == 0) return;

        /*
         * Find where the last element of run1 goes in run2. Subsequent elements in run2 can be ignored (because they're already in
         * place).
         * 找出 run1 的最后一个元素在 run2 中的位置。run2 中位于其之后的元素可以忽略(因为它们已就位)。
         */
        len2 = gallopLeft(a[base1 + len1 - 1], a, base2, len2, len2 - 1, c);
        assert !DEBUG || len2 >= 0;
        if(len2 == 0) return;

        // Merge remaining runs, using tmp array with min(len1, len2) elements
        // 合并剩余区段,使用具有 min(len1, len2) 个元素的临时数组
        if(len1 <= len2)
            mergeLo(base1, len1, base2, len2);
        else
            mergeHi(base1, len1, base2, len2);
    }

    /**
     * Merges two adjacent runs in place, in a stable fashion. The first element of the first run must be greater than the first
     * element of the second run (a[base1] > a[base2]), and the last element of the first run (a[base1 + len1-1]) must be greater
     * than all elements of the second run.
     * <p>
     * For performance, this method should be called only when len1 <= len2; its twin, mergeHi should be called if len1 >= len2.
     * (Either method may be called if len1 == len2.)
     * <p>
     * 就地、稳定地合并两个相邻区段。第一个区段的首元素必须大于第二个区段的首元素(a[base1] > a[base2]),且第一个区段的末元素(a[base1 + len1-1])必须大于第二个区段的所有元素。<p> 出于性能考虑,仅当 len1 <= len2 时才应调用此方法;若 len1 >= len2,应调用其孪生方法 mergeHi。(len1 == len2 时两个方法都可以调用。)
     * @param base1 index of first element in first run to be merged 要合并的第一个区段中第一个元素的索引
     * @param len1 length of first run to be merged (must be > 0) 要合并的第一个区段的长度(必须 > 0)
     * @param base2 index of first element in second run to be merged (must be aBase + aLen) 要合并的第二个区段中第一个元素的索引(必须是 aBase + aLen)
     * @param len2 length of second run to be merged (must be > 0) 要合并的第二个区段的长度(必须 > 0)
     */
    private void mergeLo(int base1, int len1, int base2, int len2){
        assert !DEBUG || len1 > 0 && len2 > 0 && base1 + len1 == base2;

        // Copy first run into temp array
        // 将第一个区段复制到临时数组
        T[] a = this.a; // For performance
        // 出于性能考虑
        T[] tmp = ensureCapacity(len1);
        System.arraycopy(a, base1, tmp, 0, len1);

        int cursor1 = 0; // Indexes into tmp array
        // tmp 数组的索引
        int cursor2 = base2; // Indexes int a
        // a 的索引
        int dest = base1; // Indexes int a
        // a 的索引

        // Move first element of second run and deal with degenerate cases
        // 移动第二个区段的第一个元素并处理退化情况
        a[dest++] = a[cursor2++];
        if(--len2 == 0){
            System.arraycopy(tmp, cursor1, a, dest, len1);
            return;
        }
        if(len1 == 1){
            System.arraycopy(a, cursor2, a, dest, len2);
            a[dest + len2] = tmp[cursor1]; // Last elt of run 1 to end of merge
            // 区段 1 的最后一个元素到归并结束
            return;
        }

        Comparator<? super T> c = this.c; // Use local variable for performance
        // 使用局部变量以提升性能
        int minGallop = this.minGallop; // "    " "     " "
        outer:
        while(true){
            int count1 = 0; // Number of times in a row that first run won
            // 第一个区段连续获胜的次数
            int count2 = 0; // Number of times in a row that second run won
            // 第二个区段连续获胜的次数

            /*
             * Do the straightforward thing until (if ever) one run starts winning consistently.
             * 在(如果有的话)某个区段开始持续胜出之前,先按最直接的方式处理。
             */
            do{
                assert !DEBUG || len1 > 1 && len2 > 0;
                if(c.compare(a[cursor2], tmp[cursor1]) < 0){
                    a[dest++] = a[cursor2++];
                    count2++;
                    count1 = 0;
                    if(--len2 == 0) break outer;
                }else{
                    a[dest++] = tmp[cursor1++];
                    count1++;
                    count2 = 0;
                    if(--len1 == 1) break outer;
                }
            }while((count1 | count2) < minGallop);

            /*
             * One run is winning so consistently that galloping may be a huge win. So try that, and continue galloping until (if
             * ever) neither run appears to be winning consistently anymore.
             * 某个区段持续胜出的程度非常高,飞奔(galloping)模式可能带来巨大收益。因此尝试该模式,并持续飞奔,直到(如果有的话)两个区段都不再持续胜出。
             */
            do{
                assert !DEBUG || len1 > 1 && len2 > 0;
                count1 = gallopRight(a[cursor2], tmp, cursor1, len1, 0, c);
                if(count1 != 0){
                    System.arraycopy(tmp, cursor1, a, dest, count1);
                    dest += count1;
                    cursor1 += count1;
                    len1 -= count1;
                    if(len1 <= 1) // len1 == 1 || len1 == 0
                        break outer;
                }
                a[dest++] = a[cursor2++];
                if(--len2 == 0) break outer;

                count2 = gallopLeft(tmp[cursor1], a, cursor2, len2, 0, c);
                if(count2 != 0){
                    System.arraycopy(a, cursor2, a, dest, count2);
                    dest += count2;
                    cursor2 += count2;
                    len2 -= count2;
                    if(len2 == 0) break outer;
                }
                a[dest++] = tmp[cursor1++];
                if(--len1 == 1) break outer;
                minGallop--;
            }while(count1 >= MIN_GALLOP | count2 >= MIN_GALLOP);
            if(minGallop < 0) minGallop = 0;
            minGallop += 2; // Penalize for leaving gallop mode
            // 因离开飞奔模式而加以惩罚
        } // End of "outer" loop
        // “outer”循环结束
        this.minGallop = minGallop < 1 ? 1 : minGallop; // Write back to field
        // 写回字段

        if(len1 == 1){
            assert !DEBUG || len2 > 0;
            System.arraycopy(a, cursor2, a, dest, len2);
            a[dest + len2] = tmp[cursor1]; // Last elt of run 1 to end of merge
            // 区段 1 的最后一个元素到归并结束
        }else if(len1 == 0){
            throw new IllegalArgumentException("Comparison method violates its general contract!");
        }else{
            assert !DEBUG || len2 == 0;
            assert !DEBUG || len1 > 1;
            System.arraycopy(tmp, cursor1, a, dest, len1);
        }
    }

    /**
     * Like mergeLo, except that this method should be called only if len1 >= len2; mergeLo should be called if len1 <= len2.
     * (Either method may be called if len1 == len2.)
     * <p>
     * 与 mergeLo 类似,但仅在 len1 >= len2 时才应调用此方法;若 len1 <= len2,应调用 mergeLo。(len1 == len2 时两个方法都可以调用。)
     * @param base1 index of first element in first run to be merged 要合并的第一个区段中第一个元素的索引
     * @param len1 length of first run to be merged (must be > 0) 要合并的第一个区段的长度(必须 > 0)
     * @param base2 index of first element in second run to be merged (must be aBase + aLen) 要合并的第二个区段中第一个元素的索引(必须是 aBase + aLen)
     * @param len2 length of second run to be merged (must be > 0) 要合并的第二个区段的长度(必须 > 0)
     */
    private void mergeHi(int base1, int len1, int base2, int len2){
        assert !DEBUG || len1 > 0 && len2 > 0 && base1 + len1 == base2;

        // Copy second run into temp array
        // 将第二个区段复制到临时数组
        T[] a = this.a; // For performance
        // 出于性能考虑
        T[] tmp = ensureCapacity(len2);
        System.arraycopy(a, base2, tmp, 0, len2);

        int cursor1 = base1 + len1 - 1; // Indexes into a
        // a 的索引
        int cursor2 = len2 - 1; // Indexes into tmp array
        // tmp 数组的索引
        int dest = base2 + len2 - 1; // Indexes into a
        // a 的索引

        // Move last element of first run and deal with degenerate cases
        // 移动第一个区段的最后一个元素并处理退化情况
        a[dest--] = a[cursor1--];
        if(--len1 == 0){
            System.arraycopy(tmp, 0, a, dest - (len2 - 1), len2);
            return;
        }
        if(len2 == 1){
            dest -= len1;
            cursor1 -= len1;
            System.arraycopy(a, cursor1 + 1, a, dest + 1, len1);
            a[dest] = tmp[cursor2];
            return;
        }

        Comparator<? super T> c = this.c; // Use local variable for performance
        // 使用局部变量以提升性能
        int minGallop = this.minGallop; // "    " "     " "
        outer:
        while(true){
            int count1 = 0; // Number of times in a row that first run won
            // 第一个区段连续获胜的次数
            int count2 = 0; // Number of times in a row that second run won
            // 第二个区段连续获胜的次数

            /*
             * Do the straightforward thing until (if ever) one run appears to win consistently.
             * 在(如果有的话)某个区段开始持续胜出之前,先按最直接的方式处理。
             */
            do{
                assert !DEBUG || len1 > 0 && len2 > 1;
                if(c.compare(tmp[cursor2], a[cursor1]) < 0){
                    a[dest--] = a[cursor1--];
                    count1++;
                    count2 = 0;
                    if(--len1 == 0) break outer;
                }else{
                    a[dest--] = tmp[cursor2--];
                    count2++;
                    count1 = 0;
                    if(--len2 == 1) break outer;
                }
            }while((count1 | count2) < minGallop);

            /*
             * One run is winning so consistently that galloping may be a huge win. So try that, and continue galloping until (if
             * ever) neither run appears to be winning consistently anymore.
             * 某个区段持续胜出的程度非常高,飞奔(galloping)模式可能带来巨大收益。因此尝试该模式,并持续飞奔,直到(如果有的话)两个区段都不再持续胜出。
             */
            do{
                assert !DEBUG || len1 > 0 && len2 > 1;
                count1 = len1 - gallopRight(tmp[cursor2], a, base1, len1, len1 - 1, c);
                if(count1 != 0){
                    dest -= count1;
                    cursor1 -= count1;
                    len1 -= count1;
                    System.arraycopy(a, cursor1 + 1, a, dest + 1, count1);
                    if(len1 == 0) break outer;
                }
                a[dest--] = tmp[cursor2--];
                if(--len2 == 1) break outer;

                count2 = len2 - gallopLeft(a[cursor1], tmp, 0, len2, len2 - 1, c);
                if(count2 != 0){
                    dest -= count2;
                    cursor2 -= count2;
                    len2 -= count2;
                    System.arraycopy(tmp, cursor2 + 1, a, dest + 1, count2);
                    if(len2 <= 1) // len2 == 1 || len2 == 0
                        break outer;
                }
                a[dest--] = a[cursor1--];
                if(--len1 == 0) break outer;
                minGallop--;
            }while(count1 >= MIN_GALLOP | count2 >= MIN_GALLOP);
            if(minGallop < 0) minGallop = 0;
            minGallop += 2; // Penalize for leaving gallop mode
            // 因离开飞奔模式而加以惩罚
        } // End of "outer" loop
        // “outer”循环结束
        this.minGallop = minGallop < 1 ? 1 : minGallop; // Write back to field
        // 写回字段

        if(len2 == 1){
            assert !DEBUG || len1 > 0;
            dest -= len1;
            cursor1 -= len1;
            System.arraycopy(a, cursor1 + 1, a, dest + 1, len1);
            a[dest] = tmp[cursor2]; // Move first elt of run2 to front of merge
            // 将 run2 的第一个元素移到归并的前端
        }else if(len2 == 0){
            throw new IllegalArgumentException("Comparison method violates its general contract!");
        }else{
            assert !DEBUG || len1 == 0;
            assert !DEBUG || len2 > 0;
            System.arraycopy(tmp, 0, a, dest - (len2 - 1), len2);
        }
    }

    /**
     * Ensures that the external array tmp has at least the specified number of elements, increasing its size if necessary. The
     * size increases exponentially to ensure amortized Linear time complexity.
     * <p>
     * 确保外部数组 tmp 至少拥有指定数量的元素,必要时增加其大小。大小按指数增长,以确保均摊线性时间复杂度。
     * @param minCapacity the minimum required capacity of the tmp array tmp 数组所需的最小容量
     * @return tmp, whether or not it grew tmp,无论其是否增长了
     */
    private T[] ensureCapacity(int minCapacity){
        tmpCount = Math.max(tmpCount, minCapacity);
        if(tmp.length < minCapacity){
            // Compute smallest power of 2 > minCapacity
            // 计算大于 minCapacity 的最小的 2 的幂
            int newSize = minCapacity;
            newSize |= newSize >> 1;
            newSize |= newSize >> 2;
            newSize |= newSize >> 4;
            newSize |= newSize >> 8;
            newSize |= newSize >> 16;
            newSize++;

            if(newSize < 0) // Not bloody likely!
            // 根本不可能发生!
                newSize = minCapacity;
            else
                newSize = Math.min(newSize, a.length >>> 1);

            tmp = (T[])new Object[newSize];
        }
        return tmp;
    }
}
