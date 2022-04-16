package io.cruder.apt.dsl;

import com.google.common.collect.Maps;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.Collections;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TypesDSL extends DSLSupport {
    private final Map<ClassName, TypeSpec> types = Maps.newHashMap();

    public static TypesDSL declTypes(@DelegatesTo(TypesDSL.class) Closure<?> cl) {
        TypesDSL dsl = new TypesDSL();
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    public TypesDSL declClass(Iterable<Modifier> modifiers, ClassName typeName,
                              @DelegatesTo(TypeDSL.class) Closure<?> cl) {
        types.put(typeName, TypeDSL.declClass(modifiers, typeName.simpleName(), cl).build());
        return this;
    }

    public TypesDSL declInterface(Iterable<Modifier> modifiers, ClassName typeName,
                                  @DelegatesTo(TypeDSL.class) Closure<?> cl) {
        types.put(typeName, TypeDSL.declInterface(modifiers, typeName.simpleName(), cl).build());
        return this;
    }

    public TypesDSL redecl(ClassName typeName,
                           @DelegatesTo(TypeDSL.class) Closure<?> cl) {
        types.computeIfPresent(typeName, (k, type) -> {
            TypeDSL dsl = new TypeDSL(type.toBuilder());
            cl.rehydrate(dsl, cl.getOwner(), dsl).call();
            return dsl.build();
        });
        return this;
    }

    public Map<ClassName, JavaFile> build() {
        Map<ClassName, JavaFile> files = Maps.newHashMap();
        types.forEach((typeName, type) -> {
            files.put(typeName, JavaFile.builder(typeName.packageName(), type).build());
        });
        return Collections.unmodifiableMap(files);
    }
}
