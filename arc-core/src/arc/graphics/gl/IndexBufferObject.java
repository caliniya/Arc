package arc.graphics.gl;

import arc.util.*;

import java.nio.*;

/**
 * <p>
 * In IndexBufferObject wraps OpenGL's index buffer functionality to be used in conjunction with VBOs.
 * </p>
 *
 * <p>
 * Uses indirect Buffers on Android 1.5/1.6 to fix GC invocation due to leaking PlatformAddress instances.
 * </p>
 *
 * <p>
 * You can also use this to store indices for vertex arrays. Do not call {@link #bind()} or {@link #unbind()} in this case but
 * rather use {@link #buffer()} to use the buffer directly with glDrawElements. You must also create the IndexBufferObject with
 * the second constructor and specify isDirect as true as glDrawElements in conjunction with vertex arrays needs direct buffers.
 * </p>
 *
 * <p>
 * VertexBufferObjects must be disposed via the {@link #dispose()} method when no longer needed.
 * </p>
 * <p>
 * <p> IndexBufferObject 封装 OpenGL 的索引缓冲区功能,可与 VBO 配合使用。 </p> <p> 在 Android 1.5/1.6 上使用间接缓冲区,以修复 PlatformAddress 实例泄漏导致的 GC 触发问题。 </p> <p> 也可以用它为顶点数组存储索引。此时不要调用 {@link #bind()} 或 {@link #unbind()},而应使用 {@link #buffer()} 直接将缓冲区配合 glDrawElements 使用。还必须用第二个构造函数创建 IndexBufferObject 并将 isDirect 指定为 true,因为 glDrawElements 配合顶点数组需要直接缓冲区。 </p> <p> VertexBufferObjects 不再使用时必须通过 {@link #dispose()} 方法释放。 </p>
 * @author mzechner, Thorsten Schleinzer
 */
public class IndexBufferObject implements Disposable{
    final ShortBuffer buffer;
    final ByteBuffer byteBuffer;
    final boolean isDirect;
    final int usage;
    // used to work around bug: https://android-review.googlesource.com/#/c/73175/
    // 用于规避 bug:https://android-review.googlesource.com/#/c/73175/
    final boolean empty;
    int bufferHandle;
    boolean dirty = true;
    boolean bound = false;
    boolean created;

    /**
     * Creates a new static IndexBufferObject to be used with vertex arrays.
     * <p>
     * 创建用于顶点数组的新的静态 IndexBufferObject。
     * @param maxIndices the maximum number of indices this buffer can hold 此缓冲区可容纳的最大索引数
     */
    public IndexBufferObject(int maxIndices){
        this(true, maxIndices);
    }

    /**
     * Creates a new IndexBufferObject.
     * <p>
     * 创建新的 IndexBufferObject。
     * @param isStatic whether the index buffer is static 索引缓冲区是否为静态
     * @param maxIndices the maximum number of indices this buffer can hold 此缓冲区可容纳的最大索引数
     */
    public IndexBufferObject(boolean isStatic, int maxIndices){
        empty = maxIndices == 0;
        if(empty){
            maxIndices = 1; // avoid allocating a zero-sized buffer because of bug in Android's ART < Android 5.0
            // 避免分配零长度缓冲区,因为 Android ART < Android 5.0 存在 bug
        }

        byteBuffer = Buffers.newUnsafeByteBuffer(maxIndices * 2);
        isDirect = true;

        buffer = byteBuffer.asShortBuffer();
        buffer.flip();
        byteBuffer.flip();
        usage = isStatic ? Gl.staticDraw : Gl.dynamicDraw;
    }

    /**
     * @return the number of indices currently stored in this buffer 此缓冲区当前存储的索引数量
     */
    public int size(){
        return empty ? 0 : buffer.limit();
    }

    /**
     * @return the maximum number of indices this IndexBufferObject can store. 此 IndexBufferObject 可存储的最大索引数。
     */
    public int max(){
        return empty ? 0 : buffer.capacity();
    }

    public void set(short[] indices){
        set(indices, 0, indices.length);
    }

    /**
     * <p>
     * Sets the indices of this IndexBufferObject, discarding the old indices. The count must equal the number of indices to be
     * copied to this IndexBufferObject.
     * </p>
     *
     * <p>
     * This can be called in between calls to {@link #bind()} and {@link #unbind()}. The index data will be updated instantly.
     * </p>
     * <p>
     * <p> 设置此 IndexBufferObject 的索引,丢弃旧索引。count 必须等于要复制到此 IndexBufferObject 的索引数量。 </p> <p> 可以在 {@link #bind()} 和 {@link #unbind()} 调用之间调用此方法。索引数据会立即更新。 </p>
     * @param indices the vertex data 顶点数据
     * @param offset the offset to start copying the data from 开始复制数据的偏移量
     * @param count the number of shorts to copy 要复制的 short 数量
     */
    public void set(short[] indices, int offset, int count){
        dirty = true;
        buffer.clear();
        buffer.put(indices, offset, count);
        buffer.flip();
        byteBuffer.position(0);
        byteBuffer.limit(count << 1);

        if(bound){
            Gl.bufferData(Gl.elementArrayBuffer, byteBuffer.limit(), byteBuffer, usage);
            dirty = false;
        }
    }

    public void set(ShortBuffer indices){
        dirty = true;
        int pos = indices.position();
        buffer.clear();
        buffer.put(indices);
        buffer.flip();
        indices.position(pos);
        byteBuffer.position(0);
        byteBuffer.limit(buffer.limit() << 1);

        if(bound){
            Gl.bufferData(Gl.elementArrayBuffer, byteBuffer.limit(), byteBuffer, usage);
            dirty = false;
        }
    }

    public void update(int targetOffset, short[] indices, int offset, int count){
        dirty = true;
        final int pos = byteBuffer.position();
        byteBuffer.position(targetOffset * 2);
        Buffers.copy(indices, offset, byteBuffer, count);
        byteBuffer.position(pos);
        buffer.position(0);

        if(bound){
            Gl.bufferData(Gl.elementArrayBuffer, byteBuffer.limit(), byteBuffer, usage);
            dirty = false;
        }
    }

    /**
     * <p>
     * Returns the underlying ShortBuffer. If you modify the buffer contents they wil be uploaded on the call to {@link #bind()}.
     * If you need immediate uploading use {@link #set(short[], int, int)}.
     * </p>
     * <p>
     * <p> 返回底层 ShortBuffer。若修改缓冲区内容,会在调用 {@link #bind()} 时上传。若需要立即上传,请使用 {@link #set(short[], int, int)}。 </p>
     * @return the underlying short buffer. 底层的 short 缓冲区。
     */
    public ShortBuffer buffer(){
        dirty = true;
        return buffer;
    }

    /**
     * Binds this IndexBufferObject for rendering with glDrawElements.
     * 绑定此 IndexBufferObject 以配合 glDrawElements 进行渲染。
     */
    public void bind(){
        if(!created){
            bufferHandle = Gl.genBuffer();
            created = true;
        }
        if(bufferHandle == 0) throw new ArcRuntimeException("No buffer allocated!");

        Gl.bindBuffer(Gl.elementArrayBuffer, bufferHandle);
        if(dirty){
            byteBuffer.limit(buffer.limit() * 2);
            Gl.bufferData(Gl.elementArrayBuffer, byteBuffer.limit(), byteBuffer, usage);
            dirty = false;
        }
        bound = true;
    }

    /**
     * Unbinds this IndexBufferObject.
     * 解绑此 IndexBufferObject。
     */
    public void unbind(){
        Gl.bindBuffer(Gl.elementArrayBuffer, 0);
        bound = false;
    }

    /**
     * Disposes this IndexBufferObject and all its associated OpenGL resources.
     * 释放此 IndexBufferObject 及其所有关联的 OpenGL 资源。
     */
    @Override
    public void dispose(){
        Gl.bindBuffer(Gl.elementArrayBuffer, 0);
        Gl.deleteBuffer(bufferHandle);
        bufferHandle = 0;

        Buffers.disposeUnsafeByteBuffer(byteBuffer);
    }
}
