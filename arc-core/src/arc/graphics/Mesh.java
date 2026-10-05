package arc.graphics;

import arc.graphics.gl.*;
import arc.util.*;

import java.nio.*;

/**
 * <p>
 * A Mesh holds vertices composed of attributes specified by an array of {@link VertexAttribute} instances. The vertices are held either in
 * VRAM in form of vertex buffer objects.
 * </p>
 *
 * <p>
 * A Mesh consists of vertices and optionally indices which specify which vertices define a triangle. Each vertex is composed of
 * attributes such as position, normal, color or texture coordinate. Note that not all of this attributes must be given, except
 * for position which is non-optional. Each attribute has an alias which is used when rendering a Mesh. The alias
 * is used to bind a specific vertex attribute to a shader attribute. The shader source and the alias of the attribute must match
 * exactly for this to work.
 * </p>
 * <p>
 * <p> Mesh 保存由 {@link VertexAttribute} 实例数组指定属性构成的顶点。顶点保存在 VRAM 中,形式为顶点缓冲区对象。 </p> <p> Mesh 由顶点和可选的索引组成,索引指明哪些顶点构成三角形。每个顶点由位置、法线、颜色或纹理坐标等属性组成。注意,并非所有属性都必须提供,但位置属性是必需的。每个属性都有一个别名,渲染 Mesh 时会用到。别名用于将特定顶点属性绑定到着色器属性。着色器源码与属性别名必须完全一致才能生效。 </p>
 * @author mzechner, Dave Clayton <contact@redskyforge.com>, Xoppa
 */
public class Mesh implements Disposable{
    /**
     * The size of one vertex, in bytes.
     * 单个顶点的大小,以字节为单位。
     */
    public final int vertexSize;
    /**
     * Do not modify.
     * 请勿修改。
     */
    public final VertexAttribute[] attributes;

    public VertexBufferObject vertices;
    public IndexBufferObject indices;

    /**
     * Creates a new Mesh with the given attributes.
     * <p>
     * 使用给定属性创建新的 Mesh。
     * @param isStatic whether this mesh is static or not. Allows for internal optimizations. 此网格是否为静态。可进行内部优化。
     * @param maxVertices the maximum number of vertices this mesh can hold 此网格可容纳的最大顶点数
     * @param maxIndices the maximum number of indices this mesh can hold 此网格可容纳的最大索引数
     */
    public Mesh(boolean isStatic, int maxVertices, int maxIndices, VertexAttribute... attributes){
        int count = 0;
        for(VertexAttribute attribute : attributes){
            count += attribute.size;
        }

        this.vertexSize = count;
        this.attributes = attributes;

        vertices = new VertexBufferObject(isStatic, maxVertices, this);
        indices = new IndexBufferObject(isStatic, maxIndices);
    }

    /**
     * Binds the underlying {@link VertexBufferObject} and {@link IndexBufferObject} if indices where given. Use this with OpenGL
     * ES 2.0 and when auto-bind is disabled.
     * <p>
     * 若提供了索引,则绑定底层 {@link VertexBufferObject} 和 {@link IndexBufferObject}。在 OpenGL ES 2.0 且禁用自动绑定时使用。
     */
    public void bind(final Shader shader){
        vertices.bind(shader);
        if(indices.size() > 0) indices.bind();
    }

    /**
     * Unbinds the underlying {@link VertexBufferObject} and {@link IndexBufferObject} is indices were given. Use this when auto-bind is disabled.
     * <p>
     * 若提供了索引,则解绑底层 {@link VertexBufferObject} 和 {@link IndexBufferObject}。在禁用自动绑定时使用。
     */
    public void unbind(final Shader shader){
        vertices.unbind(shader);
        if(indices.size() > 0) indices.unbind();
    }

    /** @see #render(Shader, int, int, int, boolean) */
    public void render(Shader shader, int primitiveType){
        render(shader, primitiveType, 0, indices.max() > 0 ? getNumIndices() : getNumVertices(), true);
    }

    /** @see #render(Shader, int, int, int, boolean) */
    public void render(Shader shader, int primitiveType, int offset, int count){
        render(shader, primitiveType, offset, count, true);
    }

    /**
     * <p>
     * Renders the mesh using the given primitive type. offset specifies the offset into either the vertex buffer or the index
     * buffer depending on whether indices are defined. count specifies the number of vertices or indices to use thus count /
     * #vertices per primitive primitives are rendered.
     * </p>
     * <p>
     * This method will automatically bind each vertex attribute as specified at construction time to
     * the respective shader attributes. The binding is based on the alias defined for each VertexAttribute.
     * </p>
     * <p>
     * This method must only be called after the {@link Shader#bind()} method has been called!
     * </p>
     * <p>
     * <p> 使用给定的图元类型渲染网格。offset 表示顶点缓冲区或索引缓冲区中的偏移(取决于是否定义了索引)。count 表示使用的顶点或索引数量,因此会渲染 count / 每图元顶点数 个图元。 </p> <p> 此方法会按构造时的指定,自动将每个顶点属性绑定到相应的着色器属性。绑定基于每个 VertexAttribute 定义的别名。 </p> <p> 此方法只能在调用 {@link Shader#bind()} 之后调用! </p>
     * @param shader the shader to be used 要使用的着色器
     * @param primitiveType the primitive type 图元类型
     * @param offset the offset into the vertex or index buffer 顶点或索引缓冲区中的偏移量
     * @param count number of vertices or indices to use 使用的顶点或索引数量
     * @param autoBind overrides the autoBind member of this Mesh 覆盖此 Mesh 的 autoBind 成员
     */
    public void render(Shader shader, int primitiveType, int offset, int count, boolean autoBind){
        if(count == 0) return;

        if(autoBind) bind(shader);

        vertices.render(indices, primitiveType, offset, count);

        if(autoBind) unbind(shader);
    }

    public Mesh setVertices(float[] vertices){
        this.vertices.set(vertices, 0, vertices.length);

        return this;
    }

    public Mesh setVertices(float[] vertices, int offset, int count){
        this.vertices.set(vertices, offset, count);

        return this;
    }

    /**
     * Update (a portion of) the vertices. Does not resize the backing buffer.
     * <p>
     * 更新(部分)顶点数据。不会调整底层缓冲区的大小。
     * @param targetOffset the offset in number of floats of the mesh part. 网格部分的偏移量,以浮点数个数计。
     * @param source the vertex data to update the mesh part with 用于更新网格部分的顶点数据
     */
    public Mesh updateVertices(int targetOffset, float[] source){
        return updateVertices(targetOffset, source, 0, source.length);
    }

    /**
     * Update (a portion of) the vertices. Does not resize the backing buffer.
     * <p>
     * 更新(部分)顶点数据。不会调整底层缓冲区的大小。
     * @param targetOffset the offset in number of floats of the mesh part. 网格部分的偏移量,以浮点数个数计。
     * @param source the vertex data to update the mesh part with 用于更新网格部分的顶点数据
     * @param sourceOffset the offset in number of floats within the source array 源数组中的偏移量,以浮点数个数计
     * @param count the number of floats to update 要更新的浮点数个数
     */
    public Mesh updateVertices(int targetOffset, float[] source, int sourceOffset, int count){
        this.vertices.update(targetOffset, source, sourceOffset, count);
        return this;
    }

    public Mesh setIndices(short[] indices){
        this.indices.set(indices, 0, indices.length);

        return this;
    }

    public Mesh setIndices(short[] indices, int offset, int count){
        this.indices.set(indices, offset, count);

        return this;
    }

    /**
     * @return the number of defined indices 已定义的索引数量
     */
    public int getNumIndices(){
        return indices.size();
    }

    /**
     * @return the number of defined vertices 已定义的顶点数量
     */
    public int getNumVertices(){
        return vertices.size();
    }

    /**
     * @return the maximum number of vertices this mesh can hold 此网格可容纳的最大顶点数
     */
    public int getMaxVertices(){
        return vertices.max();
    }

    /**
     * @return the maximum number of indices this mesh can hold 此网格可容纳的最大索引数
     */
    public int getMaxIndices(){
        return indices.max();
    }

    /**
     * @return the backing FloatBuffer holding the vertices. Does not have to be a direct buffer on Android! 保存顶点的底层 FloatBuffer。在 Android 上不一定是直接缓冲区!
     */
    public FloatBuffer getVertices(){
        return vertices.buffer();
    }

    /**
     * @return the backing shortbuffer holding the indices. Does not have to be a direct buffer on Android! 保存索引的底层 ShortBuffer。在 Android 上不一定是直接缓冲区!
     */
    public ShortBuffer getIndices(){
        return indices.buffer();
    }

    /**
     * Frees all resources associated with this Mesh
     * 释放此 Mesh 关联的所有资源
     */
    @Override
    public void dispose(){
        vertices.dispose();
        indices.dispose();
    }
}
