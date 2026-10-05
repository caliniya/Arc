package arc.util;

import arc.struct.*;

import java.nio.*;

/**
 * Class with static helper methods to increase the speed of array/direct buffer and direct buffer/direct buffer transfers
 * <p>
 * 包含静态辅助方法的类,用于提高数组/直接缓冲区以及直接缓冲区/直接缓冲区之间传输的速度。
 * @author mzechner, xoppa
 */
public final class Buffers{
    static final Ar<ByteBuffer> unsafeBuffers = new Ar<>();
    static int allocatedUnsafe = 0;

    /**
     * Copies numFloats floats from src starting at offset to dst. Dst is assumed to be a direct {@link Buffer}. The method will
     * crash if that is not the case. The position and limit of the buffer are ignored, the copy is placed at position 0 in the
     * buffer. After the copying process the position of the buffer is set to 0 and its limit is set to numFloats * 4 if it is a
     * ByteBuffer and numFloats if it is a FloatBuffer. In case the Buffer is neither a ByteBuffer nor a FloatBuffer the limit is
     * not set. This is an expert method, use at your own risk.
     * <p>
     * 从 src 的 offset 处开始复制 numFloats 个浮点数到 dst。dst 被假定为一个直接的 {@link Buffer},否则该方法会崩溃。缓冲区的 position 和 limit 被忽略,复制的内容放置在缓冲区的 position 0 处。复制完成后,缓冲区的 position 被设为 0,若它是 ByteBuffer 则 limit 被设为 numFloats * 4,若它是 FloatBuffer 则设为 numFloats;若 Buffer 既不是 ByteBuffer 也不是 FloatBuffer,则不设置 limit。这是专家级方法,使用风险自负。
     * @param src the source array 源数组
     * @param dst the destination buffer, has to be a direct Buffer 目标缓冲区,必须是直接缓冲区
     * @param numFloats the number of floats to copy 要复制的浮点数个数
     * @param offset the offset in src to start copying from src 中开始复制的偏移量
     */
    public static void copy(float[] src, Buffer dst, int numFloats, int offset){
        if(dst instanceof ByteBuffer)
            dst.limit(numFloats << 2);
        else if(dst instanceof FloatBuffer) dst.limit(numFloats);

        copyJni(src, dst, numFloats, offset);
        dst.position(0);
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer}
     * instance's {@link Buffer#position()} is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed</b>.
     * <p>
     * 将 src 的内容从 src[srcOffset] 开始复制 numElements 个元素到 dst。{@link Buffer} 实例的 {@link Buffer#position()} 用于定义相对于 Buffer 本身的偏移量。position 保持不变,limit 将被设置为 position + numElements。<b>Buffer 必须是使用本机字节序的直接 Buffer,不执行任何错误检查</b>。
     * @param src the source array. 源数组。
     * @param srcOffset the offset into the source array. 源数组中的偏移量。
     * @param dst the destination Buffer, its position is used as an offset. 目标 Buffer,其 position 被用作偏移量。
     * @param numElements the number of elements to copy. 要复制的元素个数。
     */
    public static void copy(byte[] src, int srcOffset, Buffer dst, int numElements){
        dst.limit(dst.position() + bytesToElements(dst, numElements));
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements);
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer}
     * instance's {@link Buffer#position()} is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed</b>.
     * <p>
     * 将 src 的内容从 src[srcOffset] 开始复制 numElements 个元素到 dst。{@link Buffer} 实例的 {@link Buffer#position()} 用于定义相对于 Buffer 本身的偏移量。position 保持不变,limit 将被设置为 position + numElements。<b>Buffer 必须是使用本机字节序的直接 Buffer,不执行任何错误检查</b>。
     * @param src the source array. 源数组。
     * @param srcOffset the offset into the source array. 源数组中的偏移量。
     * @param dst the destination Buffer, its position is used as an offset. 目标 Buffer,其 position 被用作偏移量。
     * @param numElements the number of elements to copy. 要复制的元素个数。
     */
    public static void copy(short[] src, int srcOffset, Buffer dst, int numElements){
        dst.limit(dst.position() + bytesToElements(dst, numElements << 1));
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 1);
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer}
     * instance's {@link Buffer#position()} is used to define the offset into the Buffer itself. The position and limit will stay
     * the same. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
     * <p>
     * 将 src 的内容从 src[srcOffset] 开始复制 numElements 个元素到 dst。{@link Buffer} 实例的 {@link Buffer#position()} 用于定义相对于 Buffer 本身的偏移量。position 和 limit 均保持不变。<b>Buffer 必须是使用本机字节序的直接 Buffer,不执行任何错误检查</b>。
     * @param src the source array. 源数组。
     * @param srcOffset the offset into the source array. 源数组中的偏移量。
     * @param numElements the number of elements to copy. 要复制的元素个数。
     * @param dst the destination Buffer, its position is used as an offset. 目标 Buffer,其 position 被用作偏移量。
     */
    public static void copy(float[] src, int srcOffset, int numElements, Buffer dst){
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2);
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer}
     * instance's {@link Buffer#position()} is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed</b>.
     * <p>
     * 将 src 的内容从 src[srcOffset] 开始复制 numElements 个元素到 dst。{@link Buffer} 实例的 {@link Buffer#position()} 用于定义相对于 Buffer 本身的偏移量。position 保持不变,limit 将被设置为 position + numElements。<b>Buffer 必须是使用本机字节序的直接 Buffer,不执行任何错误检查</b>。
     * @param src the source array. 源数组。
     * @param srcOffset the offset into the source array. 源数组中的偏移量。
     * @param dst the destination Buffer, its position is used as an offset. 目标 Buffer,其 position 被用作偏移量。
     * @param numElements the number of elements to copy. 要复制的元素个数。
     */
    public static void copy(int[] src, int srcOffset, Buffer dst, int numElements){
        dst.limit(dst.position() + bytesToElements(dst, numElements << 2));
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2);
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer}
     * instance's {@link Buffer#position()} is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed</b>.
     * <p>
     * 将 src 的内容从 src[srcOffset] 开始复制 numElements 个元素到 dst。{@link Buffer} 实例的 {@link Buffer#position()} 用于定义相对于 Buffer 本身的偏移量。position 保持不变,limit 将被设置为 position + numElements。<b>Buffer 必须是使用本机字节序的直接 Buffer,不执行任何错误检查</b>。
     * @param src the source array. 源数组。
     * @param srcOffset the offset into the source array. 源数组中的偏移量。
     * @param dst the destination Buffer, its position is used as an offset. 目标 Buffer,其 position 被用作偏移量。
     * @param numElements the number of elements to copy. 要复制的元素个数。
     */
    public static void copy(float[] src, int srcOffset, Buffer dst, int numElements){
        dst.limit(dst.position() + bytesToElements(dst, numElements << 2));
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2);
    }

    /**
     * Copies the contents of src to dst, starting from the current position of src, copying numElements elements (using the data
     * type of src, no matter the datatype of dst). The dst {@link Buffer#position()} is used as the writing offset. The position
     * of both Buffers will stay the same. The limit of the src Buffer will stay the same. The limit of the dst Buffer will be set
     * to dst.position() + numElements, where numElements are translated to the number of elements appropriate for the dst Buffer
     * data type. <b>The Buffers must be direct Buffers with native byte order. No error checking is performed</b>.
     * <p>
     * 从 src 的当前 position 开始复制 numElements 个元素到 dst(使用 src 的数据类型,与 dst 的数据类型无关)。dst 的 {@link Buffer#position()} 被用作写入偏移量。两个 Buffer 的 position 均保持不变,src Buffer 的 limit 保持不变,dst Buffer 的 limit 将被设置为 dst.position() + numElements,其中 numElements 会换算成与 dst Buffer 数据类型相应的元素个数。<b>Buffer 必须是使用本机字节序的直接 Buffer,不执行任何错误检查</b>。
     * @param src the source Buffer. 源 Buffer。
     * @param dst the destination Buffer. 目标 Buffer。
     * @param numElements the number of elements to copy. 要复制的元素个数。
     */
    public static void copy(Buffer src, Buffer dst, int numElements){
        int numBytes = elementsToBytes(src, numElements);
        dst.limit(dst.position() + bytesToElements(dst, numBytes));
        copyJni(src, positionInBytes(src), dst, positionInBytes(dst), numBytes);
    }

    private static int positionInBytes(Buffer dst){
        return dst.position() << elementShift(dst);
    }

    private static int bytesToElements(Buffer dst, int bytes){
        return bytes >>> elementShift(dst);
    }

    private static int elementsToBytes(Buffer dst, int elements){
        return elements << elementShift(dst);
    }

    private static int elementShift(Buffer dst){
        if(dst instanceof ByteBuffer)
            return 0;
        else if(dst instanceof ShortBuffer || dst instanceof CharBuffer)
            return 1;
        else if(dst instanceof IntBuffer)
            return 2;
        else if(dst instanceof LongBuffer)
            return 3;
        else if(dst instanceof FloatBuffer)
            return 2;
        else if(dst instanceof DoubleBuffer)
            return 3;
        else
            throw new ArcRuntimeException("Can't copy to a " + dst.getClass().getName() + " instance");
    }

    public static FloatBuffer newFloatBuffer(int numFloats){
        ByteBuffer buffer = ByteBuffer.allocateDirect(numFloats * 4);
        buffer.order(ByteOrder.nativeOrder());
        return buffer.asFloatBuffer();
    }

    public static ShortBuffer newShortBuffer(int numShorts){
        ByteBuffer buffer = ByteBuffer.allocateDirect(numShorts * 2);
        buffer.order(ByteOrder.nativeOrder());
        return buffer.asShortBuffer();
    }

    public static ByteBuffer newByteBuffer(int numBytes){
        ByteBuffer buffer = ByteBuffer.allocateDirect(numBytes);
        buffer.order(ByteOrder.nativeOrder());
        return buffer;
    }

    public static IntBuffer newIntBuffer(int numInts){
        ByteBuffer buffer = ByteBuffer.allocateDirect(numInts * 4);
        buffer.order(ByteOrder.nativeOrder());
        return buffer.asIntBuffer();
    }

    public static void disposeUnsafeByteBuffer(ByteBuffer buffer){
        int size = buffer.capacity();
        synchronized(unsafeBuffers){
            if(!unsafeBuffers.remove(buffer, true))
                throw new IllegalArgumentException("buffer not allocated with newUnsafeByteBuffer or already disposed");
        }
        allocatedUnsafe -= size;
        freeMemory(buffer);
    }

    public static boolean isUnsafeByteBuffer(ByteBuffer buffer){
        synchronized(unsafeBuffers){
            return unsafeBuffers.contains(buffer, true);
        }
    }

    /**
     * Allocates a new direct ByteBuffer from native heap memory using the native byte order. Needs to be disposed with
     * {@link #disposeUnsafeByteBuffer(ByteBuffer)}.
     * <p>
     * 使用本机字节序从本机堆内存分配一个新的直接 ByteBuffer。需要通过 {@link #disposeUnsafeByteBuffer(ByteBuffer)} 释放。
     */
    public static ByteBuffer newUnsafeByteBuffer(int numBytes){
        ByteBuffer buffer = newDisposableByteBuffer(numBytes);
        buffer.order(ByteOrder.nativeOrder());
        allocatedUnsafe += numBytes;
        synchronized(unsafeBuffers){
            unsafeBuffers.add(buffer);
        }
        return buffer;
    }

    /**
     * Returns the address of the Buffer, it assumes it is an unsafe buffer.
     * <p>
     * 返回 Buffer 的地址,假定它是一个 unsafe 缓冲区。
     * @param buffer The Buffer to ask the address for. 要查询地址的 Buffer。
     * @return the address of the Buffer. Buffer 的地址。
     */
    public static long getUnsafeBufferAddress(Buffer buffer){
        return getBufferAddress(buffer) + buffer.position();
    }

    /**
     * Registers the given ByteBuffer as an unsafe ByteBuffer. The ByteBuffer must have been allocated in native code, pointing to
     * a memory region allocated via malloc. Needs to be disposed with {@link #disposeUnsafeByteBuffer(ByteBuffer)}.
     * <p>
     * 将给定的 ByteBuffer 注册为 unsafe ByteBuffer。该 ByteBuffer 必须是在本机代码中分配的、指向通过 malloc 分配的内存区域。需要通过 {@link #disposeUnsafeByteBuffer(ByteBuffer)} 释放。
     * @param buffer the {@link ByteBuffer} to register 要注册的 {@link ByteBuffer}
     * @return the ByteBuffer passed to the method 传入该方法的 ByteBuffer
     */
    public static ByteBuffer newUnsafeByteBuffer(ByteBuffer buffer){
        allocatedUnsafe += buffer.capacity();
        synchronized(unsafeBuffers){
            unsafeBuffers.add(buffer);
        }
        return buffer;
    }

    /** @return the number of bytes allocated with {@link #newUnsafeByteBuffer(int)} 通过 {@link #newUnsafeByteBuffer(int)} 分配的字节数 */
    public static int getAllocatedBytesUnsafe(){
        return allocatedUnsafe;
    }


	/*JNI
	#include <stdio.h>
	#include <stdlib.h>
	#include <string.h>
	*/

    /**
     * Frees the memory allocated for the ByteBuffer, which MUST have been allocated via {@link #newUnsafeByteBuffer(ByteBuffer)}
     * or in native code.
     * <p>
     * 释放为 ByteBuffer 分配的内存,该 ByteBuffer 必须是通过 {@link #newUnsafeByteBuffer(ByteBuffer)} 或在本机代码中分配的。
     */
    private static native void freeMemory(ByteBuffer buffer); /*
		free(buffer);
	 */

    private static native ByteBuffer newDisposableByteBuffer(int numBytes); /*
		return env->NewDirectByteBuffer((char*)malloc(numBytes), numBytes);
	*/

    private static native long getBufferAddress(Buffer buffer); /*
	    return (jlong) buffer;
	*/

    /**
     * Writes the specified number of zeros to the buffer. This is generally faster than reallocating a new buffer.
     * 向缓冲区写入指定数量的零。这通常比重新分配一个新缓冲区更快。
     */
    private static native void clear(ByteBuffer buffer, int numBytes); /*
		memset(buffer, 0, numBytes);
	*/

    private native static void copyJni(float[] src, Buffer dst, int numFloats, int offset); /*
		memcpy(dst, src + offset, numFloats << 2 );
	*/

    private native static void copyJni(byte[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/

    private native static void copyJni(short[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	 */

    private native static void copyJni(int[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/

    private native static void copyJni(float[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/

    public native static void copyJni(Buffer src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
}
