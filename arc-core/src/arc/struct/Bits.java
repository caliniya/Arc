package arc.struct;

/**
 * A bitset, without size limitation, allows comparison via bitwise operators to other bitfields.
 * <p>
 * 一种没有大小限制的位集(bitset),可通过按位运算符与其他位域进行比较。
 * @author mzechner
 * @author jshapcott
 */
public class Bits{
    long[] bits = {0};

    public Bits(){
    }

    /**
     * Creates a bit set whose initial size is large enough to explicitly represent bits with indices in the range 0 through
     * nbits-1.
     * <p>
     * 创建一个位集,其初始大小足以显式表示索引范围 0 到 nbits-1 内的位。
     * @param nbits the initial size of the bit set 位集的初始大小
     */
    public Bits(int nbits){
        checkCapacity(nbits >>> 6);
    }

    /**
     * Sets this bits to have the same bits as another. Both sets should have the same length.
     * 将此位集设置为与另一位集具有相同的位。两个位集的长度应当相同。
     */
    public void set(Bits other){
        int length = Math.min(bits.length, other.bits.length);
        System.arraycopy(other.bits, 0, bits, 0, length);
    }

    /**
     * @param index the index of the bit 位的索引
     * @return whether the bit is set 该位是否置位
     * @throws ArrayIndexOutOfBoundsException if index < 0 如果 index < 0
     */
    public boolean get(int index){
        final int word = index >>> 6;
        //TODO why does the original source to index & 0x3F, and why doesn't java.util.BitSet?
        // TODO 为什么原始源码用的是 index & 0x3F,而 java.util.BitSet 却不这样做?
        return word < bits.length && (bits[word] & (1L << (index))) != 0L;
    }

    /**
     * Returns the bit at the given index and clears it in one go.
     * <p>
     * 一次性返回给定索引处的位并将其清除。
     * @param index the index of the bit 位的索引
     * @return whether the bit was set before invocation 调用前该位是否置位
     * @throws ArrayIndexOutOfBoundsException if index < 0 如果 index < 0
     */
    public boolean getAndClear(int index){
        final int word = index >>> 6;
        if(word >= bits.length) return false;
        long oldBits = bits[word];
        bits[word] &= ~(1L << (index & 0x3F));
        return bits[word] != oldBits;
    }

    /**
     * Returns the bit at the given index and sets it in one go.
     * <p>
     * 一次性返回给定索引处的位并将其置位。
     * @param index the index of the bit 位的索引
     * @return whether the bit was set before invocation 调用前该位是否置位
     * @throws ArrayIndexOutOfBoundsException if index < 0 如果 index < 0
     */
    public boolean getAndSet(int index){
        final int word = index >>> 6;
        checkCapacity(word);
        long oldBits = bits[word];
        bits[word] |= 1L << (index & 0x3F);
        return bits[word] == oldBits;
    }

    public void set(int index, boolean value){
        if(value){
            set(index);
        }else{
            clear(index);
        }
    }

    /**
     * @param index the index of the bit to set 要置位的位的索引
     * @throws ArrayIndexOutOfBoundsException if index < 0 如果 index < 0
     */
    public void set(int index){
        final int word = index >>> 6;
        checkCapacity(word);
        bits[word] |= 1L << (index & 0x3F);
    }

    /**
     * @param from index to start from, inclusive. 起始索引(含)。
     * @param to index to end at, exclusive. 结束索引(不含)。
     * */
    public void set(int from, int to){
        if(from == to) return;

        int startWordIndex = from >>> 6;
        int endWordIndex = (to - 1) >>> 6;
        checkCapacity(endWordIndex);

        long mask = 0xffffffffffffffffL;
        long firstWordMask = mask << from;
        long lastWordMask = mask >>> -to;

        if(startWordIndex == endWordIndex){
            // Case 1: One word
            // 情况 1:单个字(word)
            bits[startWordIndex] |= (firstWordMask & lastWordMask);
        }else{
            // Case 2: Multiple words
            // 情况 2:多个字(word)
            // Handle first word
            // 处理第一个字(word)
            bits[startWordIndex] |= firstWordMask;

            // Handle intermediate words, if any
            // 处理中间的字(word),如果有的话
            for(int i = startWordIndex + 1; i < endWordIndex; i++)
                bits[i] = mask;

            // Handle last word (restores invariants)
            // 处理最后一个字(word)(恢复不变式)
            bits[endWordIndex] |= lastWordMask;
        }
    }

    /**
     * @param index the index of the bit to flip 要翻转的位的索引
     */
    public void flip(int index){
        final int word = index >>> 6;
        checkCapacity(word);
        bits[word] ^= 1L << (index & 0x3F);
    }

    private void checkCapacity(int len){
        if(len >= bits.length){
            long[] newBits = new long[len + 1];
            System.arraycopy(bits, 0, newBits, 0, bits.length);
            bits = newBits;
        }
    }

    /**
     * @param index the index of the bit to clear 要清除的位的索引
     * @throws ArrayIndexOutOfBoundsException if index < 0 如果 index < 0
     */
    public void clear(int index){
        final int word = index >>> 6;
        if(word >= bits.length) return;
        bits[word] &= ~(1L << (index & 0x3F));
    }

    /**
     * Clears the entire bitset
     * 清除整个位集
     */
    public void clear(){
        long[] bits = this.bits;
        int length = bits.length;
        for(int i = 0; i < length; i++){
            bits[i] = 0L;
        }
    }

    /**
     * @return the number of bits currently stored, <b>not</b> the highset set bit! 当前存储的位数,<b>不是</b>最高的置位位!
     */
    public int numBits(){
        return bits.length << 6;
    }

    /**
     * Returns the "logical size" of this bitset: the index of the highest set bit in the bitset plus one. Returns zero if the
     * bitset contains no set bits.
     * <p>
     * 返回此位集的“逻辑大小”:位集中最高置位位的索引加一。如果位集没有置位位,返回零。
     * @return the logical size of this bitset 此位集的逻辑大小
     */
    public int length(){
        long[] bits = this.bits;
        for(int word = bits.length - 1; word >= 0; --word){
            long bitsAtWord = bits[word];
            if(bitsAtWord != 0){
                for(int bit = 63; bit >= 0; --bit){
                    if((bitsAtWord & (1L << (bit & 0x3F))) != 0L){
                        return (word << 6) + bit + 1;
                    }
                }
            }
        }
        return 0;
    }

    /**
     * @return true if this bitset contains no bits that are set to true 如果此位集没有置为 true 的位,则为 true
     */
    public boolean isEmpty(){
        long[] bits = this.bits;
        int length = bits.length;
        for(int i = 0; i < length; i++){
            if(bits[i] != 0L){
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the index of the first bit that is set to true that occurs on or after the specified starting index. If no such bit
     * exists then -1 is returned.
     * <p>
     * 返回在指定起始索引或之后第一个置为 true 的位的索引。如果不存在这样的位,则返回 -1。
     */
    public int nextSetBit(int fromIndex){
        long[] bits = this.bits;
        int word = fromIndex >>> 6;
        int bitsLength = bits.length;
        if(word >= bitsLength) return -1;
        long bitsAtWord = bits[word];
        if(bitsAtWord != 0){
            for(int i = fromIndex & 0x3f; i < 64; i++){
                if((bitsAtWord & (1L << (i & 0x3F))) != 0L){
                    return (word << 6) + i;
                }
            }
        }
        for(word++; word < bitsLength; word++){
            if(word != 0){
                bitsAtWord = bits[word];
                if(bitsAtWord != 0){
                    for(int i = 0; i < 64; i++){
                        if((bitsAtWord & (1L << (i & 0x3F))) != 0L){
                            return (word << 6) + i;
                        }
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Returns the index of the first bit that is set to false that occurs on or after the specified starting index.
     * 返回在指定起始索引或之后第一个置为 false 的位的索引。
     */
    public int nextClearBit(int fromIndex){
        long[] bits = this.bits;
        int word = fromIndex >>> 6;
        int bitsLength = bits.length;
        if(word >= bitsLength) return bits.length << 6;
        long bitsAtWord = bits[word];
        for(int i = fromIndex & 0x3f; i < 64; i++){
            if((bitsAtWord & (1L << (i & 0x3F))) == 0L){
                return (word << 6) + i;
            }
        }
        for(word++; word < bitsLength; word++){
            if(word == 0){
                return word << 6;
            }
            bitsAtWord = bits[word];
            for(int i = 0; i < 64; i++){
                if((bitsAtWord & (1L << (i & 0x3F))) == 0L){
                    return (word << 6) + i;
                }
            }
        }
        return bits.length << 6;
    }

    /**
     * Performs a logical <b>AND</b> of this target bit set with the argument bit set. This bit set is modified so that each bit in
     * it has the value true if and only if it both initially had the value true and the corresponding bit in the bit set argument
     * also had the value true.
     * <p>
     * 对此目标位集与参数位集执行逻辑 <b>AND</b> 运算。此位集被修改为:其中的每一位当且仅当它最初为 true 且参数位集中的对应位也为 true 时才为 true。
     * @param other a bit set 一个位集
     */
    public void and(Bits other){
        int commonWords = Math.min(bits.length, other.bits.length);
        for(int i = 0; commonWords > i; i++){
            bits[i] &= other.bits[i];
        }

        if(bits.length > commonWords){
            for(int i = commonWords, s = bits.length; s > i; i++){
                bits[i] = 0L;
            }
        }
    }

    /**
     * Clears all of the bits in this bit set whose corresponding bit is set in the specified bit set.
     * <p>
     * 清除此位集中所有在指定位集中被置位的对应位。
     * @param other a bit set 一个位集
     */
    public void andNot(Bits other){
        for(int i = 0, j = bits.length, k = other.bits.length; i < j && i < k; i++){
            bits[i] &= ~other.bits[i];
        }
    }

    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set argument. This bit set is modified so that a bit in it has the
     * value true if and only if it either already had the value true or the corresponding bit in the bit set argument has the
     * value true.
     * <p>
     * 对此位集与参数位集执行逻辑 <b>OR</b> 运算。此位集被修改为:其中的每一位当且仅当它最初为 true 或参数位集中的对应位为 true 时才为 true。
     * @param other a bit set 一个位集
     */
    public void or(Bits other){
        int commonWords = Math.min(bits.length, other.bits.length);
        for(int i = 0; commonWords > i; i++){
            bits[i] |= other.bits[i];
        }

        if(commonWords < other.bits.length){
            checkCapacity(other.bits.length);
            for(int i = commonWords, s = other.bits.length; s > i; i++){
                bits[i] = other.bits[i];
            }
        }
    }

    /**
     * Performs a logical <b>XOR</b> of this bit set with the bit set argument. This bit set is modified so that a bit in it has
     * the value true if and only if one of the following statements holds:
     * <ul>
     * <li>The bit initially has the value true, and the corresponding bit in the argument has the value false.</li>
     * <li>The bit initially has the value false, and the corresponding bit in the argument has the value true.</li>
     * </ul>
     * <p>
     * 对此位集与参数位集执行逻辑 <b>XOR</b> 运算。此位集被修改为:其中的每一位当且仅当以下陈述之一成立时才为 true:<ul> <li>该位最初为 true,而参数中的对应位为 false。</li> <li>该位最初为 false,而参数中的对应位为 true。</li> </ul>
     */
    public void xor(Bits other){
        int commonWords = Math.min(bits.length, other.bits.length);

        for(int i = 0; commonWords > i; i++){
            bits[i] ^= other.bits[i];
        }

        if(commonWords < other.bits.length){
            checkCapacity(other.bits.length);
            for(int i = commonWords, s = other.bits.length; s > i; i++){
                bits[i] = other.bits[i];
            }
        }
    }

    /**
     * Returns true if the specified BitSet has any bits set to true that are also set to true in this BitSet.
     * <p>
     * 如果指定的 BitSet 有任何置为 true 的位在此 BitSet 中也为 true,返回 true。
     * @param other a bit set 一个位集
     * @return boolean indicating whether this bit set intersects the specified bit set 布尔值,指示此位集是否与指定的位集相交
     */
    public boolean intersects(Bits other){
        long[] bits = this.bits;
        long[] otherBits = other.bits;
        for(int i = Math.min(bits.length, otherBits.length) - 1; i >= 0; i--){
            if((bits[i] & otherBits[i]) != 0){
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this bit set is a super set of the specified set, i.e. it has all bits set to true that are also set to true
     * in the specified BitSet.
     * <p>
     * 如果此位集是指定集合的超集,即指定 BitSet 中所有置为 true 的位在此位集中也置为 true,返回 true。
     * @param other a bit set 一个位集
     * @return boolean indicating whether this bit set is a super set of the specified set 布尔值,指示此位集是否为指定集合的超集
     */
    public boolean containsAll(Bits other){
        long[] bits = this.bits;
        long[] otherBits = other.bits;
        int otherBitsLength = otherBits.length;
        int bitsLength = bits.length;

        for(int i = bitsLength; i < otherBitsLength; i++){
            if(otherBits[i] != 0){
                return false;
            }
        }
        for(int i = Math.min(bitsLength, otherBitsLength) - 1; i >= 0; i--){
            if((bits[i] & otherBits[i]) != otherBits[i]){
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode(){
        final int word = length() >>> 6;
        int hash = 0;
        for(int i = 0; word >= i; i++){
            hash = 127 * hash + (int)(bits[i] ^ (bits[i] >>> 32));
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;

        Bits other = (Bits)obj;
        long[] otherBits = other.bits;

        int commonWords = Math.min(bits.length, otherBits.length);
        for(int i = 0; commonWords > i; i++){
            if(bits[i] != otherBits[i])
                return false;
        }

        if(bits.length == otherBits.length)
            return true;

        return length() == other.length();
    }
}
