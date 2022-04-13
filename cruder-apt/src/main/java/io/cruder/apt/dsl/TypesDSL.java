package io.cruder.apt.dsl;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TypesDSL extends DSLSupport {
    private final List<JavaFile> files = Lists.newArrayList();

    public static TypesDSL decls(@DelegatesTo(TypesDSL.class) Closure<?> cl) {
        TypesDSL dsl = new TypesDSL();
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    public List<JavaFile> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public void declClass(List<Modifier> modifiers, ClassName name,
                          @DelegatesTo(TypeDSL.class) Closure<?> cl) {
        TypeDSL dsl = TypeDSL.declClass(modifiers, name.simpleName(), cl);
        JavaFile file = JavaFile.builder(name.packageName(), dsl.getBuilder().build()).build();
        files.add(file);
    }

    public void declInterface(List<Modifier> modifiers, ClassName name,
                              @DelegatesTo(TypeDSL.class) Closure<?> cl) {
        TypeDSL dsl = TypeDSL.declInterface(modifiers, name.simpleName(), cl);
        JavaFile file = JavaFile.builder(name.packageName(), dsl.getBuilder().build()).build();
        files.add(file);
    }
}
