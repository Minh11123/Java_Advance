package Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares how a field should be mapped to a database column.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
    /**
     * Optional explicit column name. When left empty the field name is used.
     */
    String name() default "";

    /**
     * Whether the column allows null values.
     */
    boolean nullable() default true;

    /**
     * Optional length hint for text based columns.
     */
    int length() default 255;
}
