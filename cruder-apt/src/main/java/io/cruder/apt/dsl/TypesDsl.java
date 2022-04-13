package io.cruder.apt.dsl;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TypesDsl extends DslSupport {
    private final List<JavaFile> files = Lists.newArrayList();

    public static void decls(Filer filer,
                             @DelegatesTo(TypesDsl.class) Closure<?> cl) throws IOException {
        TypesDsl dsl = new TypesDsl();
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        for (JavaFile file : dsl.files) {
            file.writeTo(filer);
        }
    }

    public void declClass(List<Modifier> modifiers, ClassName name,
                          @DelegatesTo(TypeDsl.class) Closure<?> cl) {
        TypeSpec spec = TypeDsl.declClass(modifiers, name.simpleName(), cl);
        JavaFile file = JavaFile.builder(name.packageName(), spec).build();
        files.add(file);
    }

    public void declInterface(List<Modifier> modifiers, ClassName name,
                              @DelegatesTo(TypeDsl.class) Closure<?> cl) {
        TypeSpec spec = TypeDsl.declInterface(modifiers, name.simpleName(), cl);
        JavaFile file = JavaFile.builder(name.packageName(), spec).build();
        files.add(file);
    }
}
