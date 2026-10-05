package arc.util.anno;

import java.lang.annotation.*;

/**
 * Marks an annotation processor for automatic SPI registration.
 *
 * <p>
 * Annotated processors are discovered by {@link AutoProcessor}, which generates the
 * META-INF/services/javax.annotation.processing.Processor file for them - no manual
 * service registration needed.
 * <p>
 * 标记注解处理器以进行自动 SPI 注册。
 * <p>
 * 被标记的处理器由 {@link AutoProcessor} 发现,并由它为这些处理器生成 META-INF/services/javax.annotation.processing.Processor 文件,无需手动注册服务。
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface AnnoProc{
}
