package arc.graphics;

import arc.graphics.gl.*;

/**
 * A single vertex attribute defined by its number of components and its shader alias. The number of components
 * defines how many components the attribute has. The alias defines to which shader attribute this attribute should bind. The alias
 * is used by a {@link Mesh} when drawing with a {@link Shader}.
 * <p>
 * 单个顶点属性,由其分量数和着色器别名定义。分量数定义该属性有多少个分量。别名定义该属性应绑定到哪个着色器属性。使用 {@link Shader} 绘制时,{@link Mesh} 会用到该别名。
 * @author mzechner
 */
public final class VertexAttribute{

    public static final VertexAttribute

    position = new VertexAttribute(2, Shader.positionAttribute),
    position3 = new VertexAttribute(3, Shader.positionAttribute),
    packedPosition = new VertexAttribute(2, Gl.unsignedShort, true, Shader.positionAttribute),
    texCoords = new VertexAttribute(2, Shader.texcoordAttribute + "0"),
    packedTexCoords = new VertexAttribute(2, Gl.unsignedShort, true, Shader.texcoordAttribute + "0"),
    depthCoords = new VertexAttribute(1, "a_depth"),
    texCoords3 = new VertexAttribute(3, Shader.texcoordAttribute + "0"),
    normal = new VertexAttribute(3, Shader.normalAttribute),
    packedNormal = new VertexAttribute(4, Gl.int2101010Rev, true, Shader.normalAttribute),
    color = new VertexAttribute(4, Gl.unsignedByte, true, Shader.colorAttribute),
    mixColor = new VertexAttribute(4, Gl.unsignedByte, true, Shader.mixColorAttribute);

    /**
     * the number of components this attribute has *
     * 此属性拥有的分量数 *
     */
    public final int components;
    /**
     * For fixed types, whether the values are normalized to either -1f and +1f (signed) or 0f and +1f (unsigned)
     * 对于定点类型,指示值是否归一化到 -1f 与 +1f(有符号)或 0f 与 +1f(无符号)
     */
    public final boolean normalized;
    /**
     * the OpenGL type of each component, e.g. {@link Gl#floatV} or {@link Gl#unsignedByte}
     * 每个分量的 OpenGL 类型,例如 {@link Gl#floatV} 或 {@link Gl#unsignedByte}
     */
    public final int type;
    /**
     * the alias for the attribute used in a {@link Shader} *
     * 在 {@link Shader} 中使用的属性别名 *
     */
    public final String alias;
    /**
     * the size (in bytes) of this attribute
     * 此属性的大小(字节)
     */
    public final int size;

    /**
     * Constructs a new VertexAttribute. The GL data type is automatically selected based on the usage.
     * <p>
     * 构造新的 VertexAttribute。GL 数据类型会根据用途自动选择。
     * @param components the number of components of this attribute, must be between 1 and 4. 此属性的分量数,必须在 1 到 4 之间。
     * @param alias the alias used in a shader for this attribute. Can be changed after construction. 此属性在着色器中使用的别名。构造后可修改。
     */
    public VertexAttribute(int components, String alias){
        this(components, Gl.floatV, false, alias);
    }

    /**
     * Constructs a new VertexAttribute.
     * <p>
     * 构造新的 VertexAttribute。
     * @param components the number of components of this attribute, must be between 1 and 4. 此属性的分量数,必须在 1 到 4 之间。
     * @param type the OpenGL type of each component, e.g. {@link Gl#floatV} or {@link Gl#unsignedByte}. Since {@link Mesh}
     * stores vertex data in 32bit floats, the total size of this attribute (type size times number of components) must be a
     * multiple of four bytes. 每个分量的 OpenGL 类型,例如 {@link Gl#floatV} 或 {@link Gl#unsignedByte}。由于 {@link Mesh} 以 32 位浮点数存储顶点数据,此属性的总大小(类型大小乘以分量数)必须是 4 字节的倍数。
     * @param normalized For fixed types, whether the values are normalized to either -1f and +1f (signed) or 0f and +1f (unsigned) 对于定点类型,指示值是否归一化到 -1f 与 +1f(有符号)或 0f 与 +1f(无符号)
     * @param alias The alias used in a shader for this attribute. Can be changed after construction. 此属性在着色器中使用的别名。构造后可修改。
     */
    public VertexAttribute(int components, int type, boolean normalized, String alias){
        this.components = components;
        this.type = type;
        this.normalized = normalized;
        this.alias = alias;

        //calculate final size based on components & type
        // 根据分量数与类型计算最终大小
        int realSize = 0;
        switch(type){
            case Gl.floatV:
            case Gl.fixed:
                realSize = 4 * components;
                break;
            case Gl.int2101010Rev:
            case Gl.unsignedInt2101010Rev:
            case Gl.unsignedByte:
            case Gl.byteV:
                realSize = components;
                break;
            case Gl.unsignedShort:
            case Gl.halfFloat:
            case Gl.shortV:
                realSize = 2 * components;
                break;
        }
        size = realSize;
    }
}
