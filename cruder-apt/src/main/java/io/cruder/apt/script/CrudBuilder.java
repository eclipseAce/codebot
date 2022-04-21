package io.cruder.apt.script;

import com.google.common.collect.Maps;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.util.BuilderSupport;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CrudBuilder extends BuilderSupport {
    private final Map<String, FieldNode> entityFields = Maps.newLinkedHashMap();
    private final Map<String, CreateActionNode> createActions = Maps.newLinkedHashMap();

    public static CrudBuilder of(ProcessingEnvironment processingEnv, TypeElement entityElement,
                                 @DelegatesTo(CrudBuilder.class) Closure<?> cl) {
        CrudBuilder crud = new CrudBuilder();
        TypeElement e = entityElement;
        while (!e.getQualifiedName().contentEquals("java.lang.Object")) {
            ElementFilter.fieldsIn(e.getEnclosedElements()).stream()
                    .filter(it -> !it.getModifiers().contains(Modifier.STATIC))
                    .forEach(field -> {
                        String name = field.getSimpleName().toString();
                        crud.entityFields.computeIfAbsent(name, k -> new FieldNode(name, TypeName.get(field.asType())));
                    });
            e = (TypeElement) processingEnv.getTypeUtils().asElement(e.getSuperclass());
        }
        cl.rehydrate(crud, cl.getOwner(), crud).call();
        return crud;
    }

    @Override
    protected void setParent(Object parent, Object child) {
    }

    @Override
    protected Object createNode(Object name) {
        return createNode(name, Maps.newHashMap(), null);
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
        switch ((String) name) {
            case "field": {
                return resolveFieldName(value)entityFields.get((String) value).clone(attributes);
            }
            case "create": {
                return createActions.put((String) value,
                        new CreateActionNode(((String) value)).putAttrs(attributes));
            }
        }
        return name;
    }

    @Override
    protected void nodeCompleted(Object parent, Object node) {
        if (node instanceof FieldNode) {
            FieldNode fieldNode = (FieldNode) node;
            if (parent instanceof CreateActionNode) {
                ((CreateActionNode) parent).putField(fieldNode);
            } else if ("fields".equals(parent)) {
                entityFields.computeIfPresent(fieldNode.getFieldName(),
                        (k, v) -> v.putAttrs(fieldNode.getFieldAttrs()));
            }
        } else if ("actions".equals(parent)) {
            if (node instanceof CreateActionNode) {
                CreateActionNode actionNode = (CreateActionNode) node;
                createActions.put(actionNode.getActionName(), actionNode);
            }
        }
    }

    public void debug() {
        System.out.println(entityFields);
    }

    private List<String> resolveFieldName(Object name) {
        return name == null ? Collections.emptyList() : Stream.of(((String) name).split(","))
                .map(StringUtils::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    @Getter
    @RequiredArgsConstructor
    private static class CreateActionNode {
        private final String actionName;
        private final Map<Object, Object> actionAttrs = Maps.newLinkedHashMap();
        private final Map<String, FieldNode> actionFields = Maps.newLinkedHashMap();

        public CreateActionNode putField(FieldNode field) {
            FieldNode existing = actionFields.putIfAbsent(field.getFieldName(), field);
            if (existing != null) {
                existing.putAttrs(field.getFieldAttrs());
            }
            return this;
        }

        public CreateActionNode putAttrs(Map<?, ?> attrs) {
            actionAttrs.putAll(attrs);
            return this;
        }
    }

    @Getter
    @RequiredArgsConstructor
    private static class FieldNode {
        private final String fieldName;
        private final TypeName fieldType;
        private final Map<Object, Object> fieldAttrs = Maps.newLinkedHashMap();

        public FieldNode clone(Map<?, ?> extraAttrs) {
            FieldNode node = new FieldNode(fieldName, fieldType);
            node.fieldAttrs.putAll(fieldAttrs);
            node.fieldAttrs.putAll(extraAttrs);
            return node;
        }

        public FieldNode putAttrs(Map<?, ?> attrs) {
            fieldAttrs.putAll(attrs);
            return this;
        }
    }

    private
}
