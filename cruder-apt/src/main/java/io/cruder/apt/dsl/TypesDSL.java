package io.cruder.apt.dsl;

import com.google.common.collect.Maps;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TypesDSL extends DSLSupport {
    private final Map<TypeName, JavaFile.Builder> builders = Maps.newHashMap();

    public static TypesDSL decls(@DelegatesTo(TypesDSL.class) Closure<?> cl) {
        TypesDSL dsl = new TypesDSL();
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    public TypesDSL declClass(Iterable<Modifier> modifiers, ClassName typeName,
                              @DelegatesTo(TypeDSL.class) Closure<?> cl) {
        TypeSpec type = TypeDSL.declClass(modifiers, typeName.simpleName(), cl).build();
        builders.put(typeName, JavaFile.builder(typeName.packageName(), type));
        return this;
    }

    public TypesDSL declInterface(Iterable<Modifier> modifiers, ClassName typeName,
                                  @DelegatesTo(TypeDSL.class) Closure<?> cl) {
        TypeSpec type = TypeDSL.declInterface(modifiers, typeName.simpleName(), cl).build();
        builders.put(typeName, JavaFile.builder(typeName.packageName(), type));
        return this;
    }

    public Map<? extends TypeName, JavaFile> build() {
        Map<TypeName, JavaFile> files = Maps.newHashMap();
        builders.forEach((typeName, builder) -> files.put(typeName, builder.build()));
        return Collections.unmodifiableMap(files);
    }
}
