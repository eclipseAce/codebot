package io.cruder.autoservice;

import com.google.common.collect.Sets;
import com.squareup.javapoet.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MapperComponent implements Component {
    private final @Getter ClassName name;

    private final Set<Map.Entry<ClassName, ClassName>> conversions = Sets.newLinkedHashSet();

    private ProcessingContext ctx;

    @Override
    public void init(ProcessingContext ctx) {
        this.ctx = ctx;
    }

    public String mapping(ClassName from, ClassName to) {
        conversions.add(new AbstractMap.SimpleImmutableEntry<>(from, to));
        return String.format("map%sTo%s", from.simpleName(), to.simpleName());
    }

    public String mapping(TypeElement from, TypeElement to) {
        return mapping(ClassName.get(from), ClassName.get(to));
    }

    public String mapping(EntityDescriptor from, TypeElement to) {
        return mapping(from.getBeanElement(), to);
    }

    public String mapping(TypeElement from, EntityDescriptor to) {
        return mapping(from, to.getBeanElement());
    }

    public String mapping(VariableElement from, TypeElement to) {
        return mapping(ctx.utils.asTypeElement(from.asType()), to);
    }

    public String mapping(VariableElement from, EntityDescriptor to) {
        return mapping(from, to.getBeanElement());
    }

    @Override
    public JavaFile createJavaFile() {
        TypeSpec type = TypeSpec.interfaceBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.mapstruct", "Mapper"))
                .addMethods(conversions.stream()
                        .map(conv -> MethodSpec
                                .methodBuilder(mapping(conv.getKey(), conv.getValue()))
                                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                .addParameter(conv.getKey(), "from")
                                .addParameter(ParameterSpec.builder(conv.getValue(), "to")
                                        .addAnnotation(ClassName.get("org.mapstruct", "MappingTarget"))
                                        .build())
                                .build())
                        .collect(Collectors.toList()))
                .build();
        return JavaFile.builder(name.packageName(), type).build();
    }
}