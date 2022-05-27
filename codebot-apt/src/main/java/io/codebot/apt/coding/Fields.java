package io.codebot.apt.coding;


import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Fields {
    private final Types typeUtils;

    public static Fields instanceOf(ProcessingEnvironment processingEnv) {
        return new Fields(processingEnv.getTypeUtils());
    }

    public Field of(DeclaredType containingType, VariableElement element) {
        return new FieldImpl(
                containingType, element,
                typeUtils.asMemberOf(containingType, element)
        );
    }

    public List<Field> allOf(DeclaredType containingType) {
        List<VariableElement> fields = Lists.newArrayList();
        collectFieldsInHierarchy(containingType, fields);
        return fields.stream()
                .map(it -> of(containingType, it))
                .collect(Collectors.toList());
    }

    private static void collectFieldsInHierarchy(DeclaredType declaredType,
                                                 List<VariableElement> collected) {
        TypeElement element = (TypeElement) declaredType.asElement();
        if (!Object.class.getName().contentEquals(element.getQualifiedName())) {
            ElementFilter.fieldsIn(element.getEnclosedElements()).stream()
                    .filter(field -> !field.getModifiers().contains(Modifier.STATIC))
                    .forEach(collected::add);
            if (element.getSuperclass().getKind() != TypeKind.NONE) {
                collectFieldsInHierarchy((DeclaredType) element.getSuperclass(), collected);
            }
        }
    }

    private static class FieldImpl implements Field {
        private final @Getter DeclaredType containingType;
        private final @Getter VariableElement element;
        private final @Getter TypeMirror type;

        FieldImpl(DeclaredType containingType, VariableElement element, TypeMirror type) {
            this.containingType = containingType;
            this.element = element;
            this.type = type;
        }

        @Override
        public String getName() {
            return element.getSimpleName().toString();
        }
    }
}
