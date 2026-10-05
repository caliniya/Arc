package arc.util.anno;

import java.lang.annotation.*;

/**
 * Marks an annotation processor for automatic SPI registration.
 *
 * <p>
 * Annotated processors are discovered by {@link AutoProcessor}, which generates the
 * META-INF/services/javax.annotation.processing.Processor file for them - no manual
 * service registration needed.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface AnnoProc{
}
