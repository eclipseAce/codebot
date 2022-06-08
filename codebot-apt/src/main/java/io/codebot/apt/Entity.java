package io.codebot.apt;

import io.codebot.apt.model.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Entity {
    private final ProcessingEnvironment processingEnv;

    private final @Getter DeclaredType type;
    private final @Getter String idAttribute;
    private final @Getter TypeMirror idAttributeType;
    private final @Getter ReadMethod idReadMethod;

    public static Entity of(ProcessingEnvironment processingEnv, TypeMirror type) {
        TypeOps typeOps = TypeOps.instanceOf(processingEnv);
        if (!typeOps.isDeclared(type)) {
            throw new IllegalArgumentException("Not declared type for entity: " + type);
        }
        Fields fieldUtils = Fields.instanceOf(processingEnv);
        Methods methodUtils = Methods.instanceOf(processingEnv);
        Annotations annotationUtils = Annotations.instanceOf(processingEnv);
        Field idField = fieldUtils.allOf((DeclaredType) type).stream()
                .filter(it -> annotationUtils.isPresent(it.getElement(), "javax.persistence.Id"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entity " + type + " has no Id attribute"));
        ReadMethod idReadMethod = methodUtils.allOf((DeclaredType) type)
                .findReader(idField.getName(), idField.getType());
        return new Entity(processingEnv, (DeclaredType) type, idField.getName(), idField.getType(), idReadMethod);
    }
}
