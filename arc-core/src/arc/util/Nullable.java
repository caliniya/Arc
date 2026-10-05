package arc.util;

import java.lang.annotation.*;

/**
 * Indicates that a method return or field can be null.
 * 表示方法返回值或字段可以为 null。
 */
@Documented
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable{

}
