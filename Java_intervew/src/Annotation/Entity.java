package Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a persistable entity that can be handled by the pseudo JPA layer.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Entity {
    /**
     * Optional explicit entity name. When absent the simple class name is used.
     */
    String value() default "";
}
