package arc.assets.loaders;

import arc.assets.*;
import arc.files.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;

/**
 * {@link AssetLoader} for {@link Shader} instances loaded from text files. If the file suffix is ".vert", it is assumed
 * to be a vertex shader, and a fragment shader is found using the same file name with a ".frag" suffix. And vice versa if the
 * file suffix is ".frag". These default suffixes can be changed in the ShaderProgramLoader constructor.
 * <p>
 * For all other file suffixes, the same file is used for both (and therefore should internally distinguish between the programs
 * using preprocessor directives and {@link Shader#prependVertexCode} and {@link Shader#prependFragmentCode}).
 * <p>
 * The above default behavior for finding the files can be overridden by explicitly setting the file names in a
 * {@link ShaderProgramParameter}. The parameter can also be used to prepend code to the programs.
 * <p>
 * 用于从文本文件加载 {@link Shader} 实例的 {@link AssetLoader}。如果文件后缀为 ".vert",则假定其为顶点着色器,并使用相同文件名加 ".frag" 后缀查找片段着色器;后缀为 ".frag" 时则相反。这些默认后缀可以在 ShaderProgramLoader 构造函数中修改。
 * <p>
 * 对于所有其他文件后缀,两个着色器使用同一个文件(因此应在内部通过预处理器指令以及 {@link Shader#prependVertexCode} 和 {@link Shader#prependFragmentCode} 来区分两个程序)。
 * <p>
 * 可以通过在 {@link ShaderProgramParameter} 中显式设置文件名来覆盖上述默认的文件查找行为。该参数还可用于向程序前置代码。
 * @author cypherdare
 */
public class ShaderProgramLoader extends AsynchronousAssetLoader<Shader, ShaderProgramLoader.ShaderProgramParameter>{
    private String vertexFileSuffix = ".vert";
    private String fragmentFileSuffix = ".frag";

    public ShaderProgramLoader(FileHandleResolver resolver){
        super(resolver);
    }

    public ShaderProgramLoader(FileHandleResolver resolver, String vertexFileSuffix, String fragmentFileSuffix){
        super(resolver);
        this.vertexFileSuffix = vertexFileSuffix;
        this.fragmentFileSuffix = fragmentFileSuffix;
    }

    @Override
    public Ar<AssetDescriptor> getDependencies(String fileName, Fi file, ShaderProgramParameter parameter){
        return null;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, Fi file, ShaderProgramParameter parameter){
    }

    @Override
    public Shader loadSync(AssetManager manager, String fileName, Fi file, ShaderProgramParameter parameter){
        String vertFileName = null, fragFileName = null;
        if(parameter != null){
            if(parameter.vertexFile != null) vertFileName = parameter.vertexFile;
            if(parameter.fragmentFile != null) fragFileName = parameter.fragmentFile;
        }
        if(vertFileName == null && fileName.endsWith(fragmentFileSuffix)){
            vertFileName = fileName.substring(0, fileName.length() - fragmentFileSuffix.length()) + vertexFileSuffix;
        }
        if(fragFileName == null && fileName.endsWith(vertexFileSuffix)){
            fragFileName = fileName.substring(0, fileName.length() - vertexFileSuffix.length()) + fragmentFileSuffix;
        }
        Fi vertexFile = vertFileName == null ? file : resolve(vertFileName);
        Fi fragmentFile = fragFileName == null ? file : resolve(fragFileName);
        String vertexCode = vertexFile.readString();
        String fragmentCode = vertexFile.equals(fragmentFile) ? vertexCode : fragmentFile.readString();
        if(parameter != null){
            if(parameter.prependVertexCode != null) vertexCode = parameter.prependVertexCode + vertexCode;
            if(parameter.prependFragmentCode != null) fragmentCode = parameter.prependFragmentCode + fragmentCode;
        }

        Shader shader = new Shader(vertexCode, fragmentCode);
        if((parameter == null || parameter.logOnCompileFailure) && !shader.isCompiled()){
            Log.err("Shader " + fileName + " failed to compile:\n" + shader.getLog());
        }

        return shader;
    }

    public static class ShaderProgramParameter extends AssetLoaderParameters<Shader>{
        /**
         * File name to be used for the vertex program instead of the default determined by the file name used to submit this asset
         * to AssetManager.
         * <p>
         * 顶点程序要使用的文件名,代替由提交该资产到 AssetManager 时所用的文件名确定的默认文件名。
         */
        public String vertexFile;
        /**
         * File name to be used for the fragment program instead of the default determined by the file name used to submit this
         * asset to AssetManager.
         * <p>
         * 片段程序要使用的文件名,代替由提交该资产到 AssetManager 时所用的文件名确定的默认文件名。
         */
        public String fragmentFile;
        /**
         * Whether to log (at the error level) the shader's log if it fails to compile. Default true.
         * 如果着色器编译失败,是否(以错误级别)记录着色器的日志。默认为 true。
         */
        public boolean logOnCompileFailure = true;
        /**
         * Code that is always added to the vertex shader code. This is added as-is, and you should include a newline (`\n`) if
         * needed. {@linkplain Shader#prependVertexCode} is placed before this code.
         * <p>
         * 始终添加到顶点着色器代码中的代码。会按原样添加,如有需要应包含换行符(`\n`)。{@linkplain Shader#prependVertexCode} 会被放置在此代码之前。
         */
        public String prependVertexCode;
        /**
         * Code that is always added to the fragment shader code. This is added as-is, and you should include a newline (`\n`) if
         * needed. {@linkplain Shader#prependFragmentCode} is placed before this code.
         * <p>
         * 始终添加到片段着色器代码中的代码。会按原样添加,如有需要应包含换行符(`\n`)。{@linkplain Shader#prependFragmentCode} 会被放置在此代码之前。
         */
        public String prependFragmentCode;
    }
}
