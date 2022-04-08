package io.cruder.apt;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(SOURCE)
@Target(TYPE)
@Repeatable(ReplaceTypes.class)
public @interface ReplaceType {

    String regex() default "";

    String replacement() default "";

    Class<?> target() default Object.class;

    Class<?> with() default Object.class;

}
