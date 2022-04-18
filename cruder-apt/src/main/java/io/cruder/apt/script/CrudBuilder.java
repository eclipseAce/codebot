package io.cruder.apt.script;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.util.BuilderSupport;
import lombok.RequiredArgsConstructor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;

public class CrudBuilder extends BuilderSupport {
    private final Map<String, FieldInfo> fields = Maps.newLinkedHashMap();

    private final Map<String, ActionInfo> actions = Maps.newLinkedHashMap();

    private CrudBuilder(ProcessingEnvironment processingEnv, TypeElement element) {
        initFields(processingEnv, element);
    }

    public static CrudBuilder ofEntity(ProcessingEnvironment processingEnv,
                                       TypeElement entityElement,
                                       @DelegatesTo(CrudBuilder.class) Closure<?> cl) {
        CrudBuilder builder = new CrudBuilder(processingEnv, entityElement);
        cl.rehydrate(builder, cl.getOwner(), builder).call();
        return builder;
    }

    @Override
    protected void setParent(Object parent, Object child) {
    }

    @Override
    protected Object createNode(Object name) {
        return createNode(name, Collections.emptyMap(), null);
    }

    @Override
    protected Object createNode(Object name, Object value) {
        return createNode(name, Collections.emptyMap(), value);
    }

    @Override
    protected Object createNode(Object name, Map attributes) {
        return createNode(name, attributes, null);
    }

    @Override
    protected Object createNode(Object name, Map attributes, Object value) {
       return null;
    }

    private void initFields(ProcessingEnvironment processingEnv, TypeElement element) {
        for (TypeElement e = element;
             !e.getQualifiedName().contentEquals("java.lang.Object");
             e = (TypeElement) processingEnv.getTypeUtils().asElement(e.getSuperclass())) {
            for (VariableElement ve : ElementFilter.fieldsIn(e.getEnclosedElements())) {
                if (ve.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }
                FieldInfo info = new FieldInfo(ve.getSimpleName().toString(), TypeName.get(ve.asType()));
                if (!fields.containsKey(info.fieldName)) {
                    fields.put(info.fieldName, info);
                }
            }
        }
    }

    private static boolean hasAnnotation(Element element, String annotationType) {
        return element.getAnnotationMirrors().stream()
                .map(it -> ((TypeElement) it.getAnnotationType().asElement()).getQualifiedName())
                .anyMatch(it -> it.contentEquals(annotationType));
    }

    @RequiredArgsConstructor
    private static class FieldInfo {
        private final String fieldName;

        private final TypeName fieldType;

        private final Map<String, Object> attributes = Maps.newLinkedHashMap();
    }

    @RequiredArgsConstructor
    private static class ActionInfo {
        private final String actionName;

        private final String actionType;

        private final Map<String, FieldInfo> fields = Maps.newLinkedHashMap();

        private final Map<String, Object> attributes = Maps.newLinkedHashMap();
    }
}
