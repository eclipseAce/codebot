package io.cruder.apt.dsl;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.Field;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FieldDsl {
    private final FieldSpec.Builder builder;

    public static FieldSpec decl(TypeName type, String name, @DelegatesTo(FieldDsl.class) Closure<?> cl) {
        FieldDsl dsl = new FieldDsl(FieldSpec.builder(type, name));
        cl.setDelegate(dsl);
        cl.call();
        return dsl.builder.build();
    }
}
