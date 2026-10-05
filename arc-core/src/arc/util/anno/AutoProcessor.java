package arc.util.anno;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.Writer;
import java.util.*;

/**
 * Bootstraps {@link AnnoProc}-annotated annotation processors: emits the
 * META-INF/services/javax.annotation.processing.Processor file listing every class
 * marked with {@link AnnoProc} in the current compilation, so processors never need
 * manual SPI registration.
 *
 * <p>
 * This processor registers itself through a hand-written service file in the same
 * resources folder (the one bootstrap step that can't be automated).
 * <p>
 * 为标注了 {@link AnnoProc} 的注解处理器提供引导:生成 META-INF/services/javax.annotation.processing.Processor 文件,列出当前编译中所有标注了 {@link AnnoProc} 的类,使处理器无需手动进行 SPI 注册。
 * <p>
 * 此处理器自身通过同一资源文件夹中手写的服务文件注册(这是唯一无法自动化的引导步骤)。
 */
@SupportedAnnotationTypes("arc.util.anno.AnnoProc")
public class AutoProcessor extends AbstractProcessor{

    private Filer filer;
    private Messager messager;

    @Override
    public SourceVersion getSupportedSourceVersion(){
        // 不写死版本:arc-core 面向 Android 用较低的 source level 编译,latestSupported 跟随运行时的 javac
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment env){
        super.init(env);
        filer = env.getFiler();
        messager = env.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
        Set<? extends Element> processorClasses = roundEnv.getElementsAnnotatedWith(AnnoProc.class);

        if(processorClasses.isEmpty()) return false;

        List<String> processorNames = new ArrayList<>();

        for(Element element : processorClasses){
            String className = ((TypeElement)element).getQualifiedName().toString();
            processorNames.add(className);
            messager.printMessage(Diagnostic.Kind.NOTE, "AnnotationProcessor: " + className);
        }

        generateServiceFile(processorNames);
        return true;
    }

    private void generateServiceFile(List<String> processorNames){
        try{
            String path = "META-INF/services/javax.annotation.processing.Processor";
            Writer writer = filer.createResource(StandardLocation.CLASS_OUTPUT, "", path).openWriter();

            for(String name : processorNames){
                writer.write(name + "\n");
            }
            writer.close();
        }catch(Exception e){
            messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate SPI: " + e.getMessage());
        }
    }
}
