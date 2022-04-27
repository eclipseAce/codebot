package io.cruder.example.codegen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface AutoService {
    Class<?> value();

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    @interface Creating {

    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    @interface Updating {

    }
}
