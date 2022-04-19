package io.cruder.example.core;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.TYPE})
@Repeatable(CRUDs.class)
public @interface CRUD {
    String[] value();
}
