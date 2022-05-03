package io.cruder.autoservice;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.squareup.javapoet.ClassName;
import lombok.Getter;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BeanDescriptor {
    private final @Getter TypeElement beanElement;
    private final @Getter Map<String, PropertyDescriptor> properties;

    public BeanDescriptor(ProcessingContext ctx, TypeElement bean) {
        this.beanElement = bean;

        Map<String, PropertyDescriptor> properties = Maps.newLinkedHashMap();
        TypeElement e = bean;
        while (e.getKind() == ElementKind.CLASS
                && !ctx.utils.isTypeOfName(e.asType(), "java.lang.Object")) {
            properties.putAll(ElementFilter.fieldsIn(e.getEnclosedElements()).stream()
                    .filter(field -> !field.getModifiers().contains(Modifier.STATIC))
                    .map(field -> new PropertyDescriptor(ctx, field))
                    .collect(Collectors.toMap(PropertyDescriptor::getName, Function.identity())));
            e = ctx.utils.asTypeElement(e.getSuperclass());
        }
        this.properties = ImmutableMap.copyOf(properties);
    }

    public ClassName getClassName() {
        return ClassName.get(beanElement);
    }

    public List<PropertyDescriptor> findProperties(Predicate<PropertyDescriptor> condition) {
        return properties.values().stream().filter(condition).collect(Collectors.toList());
    }

    public Optional<PropertyDescriptor> findUniqueProperty(Predicate<PropertyDescriptor> condition) {
        List<PropertyDescriptor> all = findProperties(condition);
        return all.size() == 1 ? Optional.of(all.get(0)) : Optional.empty();
    }

    public Optional<PropertyDescriptor> findFirstProperty(Predicate<PropertyDescriptor> condition) {
        return findProperties(condition).stream().findFirst();
    }
}
