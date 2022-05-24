package io.codebot.apt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Exposed {
    boolean value() default true;

    String title() default "";

    String path() default "";

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.PARAMETER)
    @interface Body {
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.PARAMETER)
    @interface Param {
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.PARAMETER)
    @interface Path {
    }
}
