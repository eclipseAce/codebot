package io.codebot.apt;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(AttributeMappings.class)
public @interface AttributeMapping {
    String to();

    String useMethod() default "";
}
