package io.cruder.apt.dsl;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyDSL {
    private final FieldSpec.Builder fieldBuilder;
    private final MethodSpec.Builder getterBuilder;
    private final MethodSpec.Builder setterBuilder;
    private boolean noGetter;
    private boolean noSetter;

    public static PropertyDSL property(TypeName typeName, String name,
                                       @DelegatesTo(PropertyDSL.class) Closure<?> cl) {
        PropertyDSL dsl = new PropertyDSL(
                FieldSpec.builder(typeName, name, Modifier.PRIVATE),
                createGetterBuilder(typeName, name),
                createSetterBuilder(typeName, name)
        );
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    private static MethodSpec.Builder createGetterBuilder(TypeName typeName, String name) {
        String prefix = TypeName.BOOLEAN.equals(typeName) ? "is" : "get";
        return MethodSpec.methodBuilder(prefix + StringUtils.capitalize(name))
                .addModifiers(Arrays.asList(Modifier.PUBLIC))
                .returns(typeName)
                .addCode("return this.$N;", name);
    }

    private static MethodSpec.Builder createSetterBuilder(TypeName typeName, String name) {
        return MethodSpec.methodBuilder("set" + StringUtils.capitalize(name))
                .addModifiers(Arrays.asList(Modifier.PUBLIC))
                .addParameter(typeName, name)
                .addCode("this.$N = $N;", name, name);
    }

    public PropertyDSL noGetter() {
        noGetter = true;
        return this;
    }

    public PropertyDSL noSetter() {
        noSetter = true;
        return this;
    }

    public PropertyDSL annotateField(ClassName typeName,
                             @DelegatesTo(AnnotationDSL.class) Closure<?> cl) {
        fieldBuilder.addAnnotation(AnnotationDSL.annotate(typeName, cl).build());
        return this;
    }

    public PropertyDSL annotateField(ClassName ...typeNames) {
        for (ClassName typeName : typeNames) {
            fieldBuilder.addAnnotation(typeName);
        }
        return this;
    }

    public FieldSpec buildField() {
        return fieldBuilder.build();
    }

    public List<MethodSpec> buildAccessors() {
        List<MethodSpec> accessors = Lists.newArrayListWithCapacity(2);
        if (!noGetter) {
            accessors.add(getterBuilder.build());
        }
        if (!noSetter) {
            accessors.add(setterBuilder.build());
        }
        return Collections.unmodifiableList(accessors);
    }
}
