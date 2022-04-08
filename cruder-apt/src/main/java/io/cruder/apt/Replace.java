package io.cruder.apt;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;

@Retention(SOURCE)
public @interface Replace {
    String type();
    
    String[] args();
}
