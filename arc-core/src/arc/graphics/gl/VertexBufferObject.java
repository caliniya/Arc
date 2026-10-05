package arc.graphics.gl;

import arc.graphics.*;
import arc.struct.*;
import arc.util.*;

import java.nio.*;

/**
 * <p>
 * A VertexData implementation that uses vertex buffer objects and vertex array objects.
 * (This is required for OpenGL 3.0+ core profiles. In particular, the default VAO has been
 * deprecated, as has the use of client memory for passing vertex attributes.) Use of VAOs should
 * give a slight performance benefit since you don't have to bind the attributes on every draw
 * anymore.
 * </p>
 *
 * <p>
 * VertexBufferObjectWithVAO objects must be disposed via the {@link #dispose()} method when no longer needed
 * </p>
 * <p>
 * <p>
 * <p> 使用顶点缓冲区对象和顶点数组对象的 VertexData 实现。(OpenGL 3.0+ core profile 需要此实现。特别是默认 VAO 已被弃用,使用客户端内存传递顶点属性的方式同样如此。)使用 VAO 应能带来轻微的性能提升,因为不必在每次绘制时都绑定属性。 </p> <p> VertexBufferObjectWithVAO 对象不再使用时必须通过 {@link #dispose()} 方法释放 </p> <p>
 * @author mzechner, Dave Clayton <contact@redskyforge.com>, Nate Austin <nate.austin gmail>
 */
public class VertexBufferObject implements Disposable{
    final static IntBuffer tmpHandle = Buffers.newIntBuffer(1);

    final Mesh mesh;
    final FloatBuffer buffer;
    final ByteBuffer byteBuffer;
    final boolean isStatic;
    final int usage;
    int bufferHandle;
    boolean isDirty = false;
    boolean isBound = false;
    int vaoHandle = -1;
    IntAr cachedLocations = new IntAr();
    boolean created;

    /**
     * Constructs a new interleaved VertexBufferObjectWithVAO.
     * <p>
     * 构造新的交错式 VertexBufferObjectWithVAO。
     * @param isStatic whether the vertex data is static. 顶点数据是否为静态。
     * @param numVertices the maximum number of vertices 最大顶点数
     */
    public VertexBufferObject(boolean isStatic, int numVertices, Mesh mesh){
        this.isStatic = isStatic;
        this.mesh = mesh;

        byteBuffer = Buffers.newUnsafeByteBuffer(this.mesh.vertexSize * numVertices);
        buffer = byteBuffer.asFloatBuffer();
        buffer.flip();
        byteBuffer.flip();
        usage = isStatic ? Gl.staticDraw: Gl.streamDraw;
    }

    public void render(IndexBufferObject indices, int primitiveType, int offset, int count){

        if(indices.size() > 0){
            if(count + offset > indices.max()){
                throw new ArcRuntimeException("Mesh attempting to access memory outside of the index buffer (count: "
                + count + ", offset: " + offset + ", max: " + indices.max() + ")");
            }

            Gl.drawElements(primitiveType, count, Gl.unsignedShort, offset * 2);
        }else{
            Gl.drawArrays(primitiveType, offset, count);
        }
    }

    public int size(){
        return buffer.limit() * 4 / mesh.vertexSize;
    }

    public int max(){
        return byteBuffer.capacity() / mesh.vertexSize;
    }

    public FloatBuffer buffer(){
        isDirty = true;
        return buffer;
    }

    private void upload(){
        Gl.bufferData(Gl.arrayBuffer, byteBuffer.limit(), byteBuffer, usage);
    }

    private void bufferChanged(){
        if(isBound){
            upload();
            isDirty = false;
        }
    }

    public void set(float[] vertices, int offset, int count){
        isDirty = true;
        Buffers.copy(vertices, byteBuffer, count, offset);
        buffer.position(0);
        buffer.limit(count);
        bufferChanged();
    }

    public void update(int targetOffset, float[] vertices, int sourceOffset, int count){
        isDirty = true;
        final int pos = byteBuffer.position();
        byteBuffer.position(targetOffset * 4);
        Buffers.copy(vertices, sourceOffset, count, byteBuffer);
        byteBuffer.position(pos);
        buffer.position(0);
        bufferChanged();
    }

    public void bind(Shader shader){
        if(!created){
            bufferHandle = Gl.genBuffer();
            tmpHandle.clear();
            Gl.genVertexArrays(1, tmpHandle);
            vaoHandle = tmpHandle.get();
            created = true;
        }

        Gl.bindVertexArray(vaoHandle);

        bindAttributes(shader);

        //if our data has changed upload it
        // 若数据已变化则上传
        bindData();

        isBound = true;
    }

    private void bindAttributes(Shader shader){
        boolean stillValid = this.cachedLocations.size != 0;

        if(stillValid){
            for(int i = 0; stillValid && i < mesh.attributes.length; i++){
                VertexAttribute attribute = mesh.attributes[i];
                int location = shader.getAttributeLocation(attribute.alias);
                stillValid = location == this.cachedLocations.get(i);
            }
        }

        if(!stillValid){
            Gl.bindBuffer(Gl.arrayBuffer, bufferHandle);
            unbindAttributes(shader);
            this.cachedLocations.clear();

            int offset = 0;
            for(int i = 0; i < mesh.attributes.length; i++){
                VertexAttribute attribute = mesh.attributes[i];
                this.cachedLocations.add(shader.getAttributeLocation(attribute.alias));
                int aoffset = offset;
                offset += attribute.size;

                int location = this.cachedLocations.get(i);
                if(location < 0){
                    continue;
                }

                Gl.enableVertexAttribArray(location);
                Gl.vertexAttribPointer(location, attribute.components, attribute.type, attribute.normalized, mesh.vertexSize, aoffset);
            }
        }
    }

    private void unbindAttributes(Shader shader){
        if(cachedLocations.size == 0){
            return;
        }

        for(int i = 0; i < mesh.attributes.length; i++){
            int location = cachedLocations.get(i);
            if(location < 0){
                continue;
            }
            Gl.disableVertexAttribArray(location);
        }
    }

    private void bindData(){
        if(isDirty){
            Gl.bindBuffer(Gl.arrayBuffer, bufferHandle);
            byteBuffer.limit(buffer.limit() * 4);
            upload();
            isDirty = false;
        }
    }

    public void unbind(Shader shader){
        Gl.bindVertexArray(0);
        isBound = false;
    }

    @Override
    public void dispose(){
        Gl.bindBuffer(Gl.arrayBuffer, 0);
        Gl.deleteBuffer(bufferHandle);
        bufferHandle = 0;
        Buffers.disposeUnsafeByteBuffer(byteBuffer);
        deleteVAO();
    }

    private void deleteVAO(){
        if(vaoHandle != -1){
            tmpHandle.clear();
            tmpHandle.put(vaoHandle);
            tmpHandle.flip();
            Gl.deleteVertexArrays(1, tmpHandle);
            vaoHandle = -1;
        }
    }
}
