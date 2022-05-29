package io.codebot.apt;

import com.google.common.collect.Lists;
import io.codebot.apt.coding.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Entity {
    private final ProcessingEnvironment processingEnv;

    private final @Getter DeclaredType type;
    private final @Getter String idAttribute;
    private final @Getter TypeMirror idAttributeType;

    public static Entity of(ProcessingEnvironment processingEnv, TypeMirror type) {
        TypeOps typeOps = TypeOps.instanceOf(processingEnv);
        if (!typeOps.isDeclared(type)) {
            throw new IllegalArgumentException("Not declared type for entity: " + type);
        }
        Fields fieldUtils = Fields.instanceOf(processingEnv);
        Annotations annotationUtils = Annotations.instanceOf(processingEnv);
        Field idField = fieldUtils.allOf((DeclaredType) type).stream()
                .filter(it -> annotationUtils.isPresent(it.getElement(), "javax.persistence.Id"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entity " + type + " has no Id attribute"));
        return new Entity(processingEnv, (DeclaredType) type, idField.getName(), idField.getType());
    }

    private Map<String, WriteMapping> findWriteMappings(List<? extends Variable> variables) {
        TypeOps typeOps = TypeOps.instanceOf(processingEnv);
        Methods methodUtils = Methods.instanceOf(processingEnv);

        List<WriteMapping> mappings = Lists.newArrayList();
        MethodCollection entityMethods = methodUtils.allOf(type);
        for (Variable variable : variables) {
            WriteMethod attrWriter = entityMethods.findWriter(variable.getName(), variable.getType());
            if (attrWriter != null) {
                mappings.add(new WriteMapping(
                        attrWriter.getWriteName(), attrWriter,
                        variable, null
                ));
                continue;
            }
            if (typeOps.isDeclared(variable.getType())) {
                for (ReadMethod varReader : methodUtils.allOf((DeclaredType) variable.getType()).readers()) {
                    attrWriter = entityMethods.findWriter(variable.getName(), variable.getType());
                    if (attrWriter != null) {
                        mappings.add(new WriteMapping(
                                attrWriter.getWriteName(), attrWriter,
                                variable, varReader
                        ));
                    }
                }
            }
        }
        return mappings.stream().collect(Collectors.toMap(
                it -> it.attribute,
                it -> it,
                (a, b) -> a
        ));
    }

    @RequiredArgsConstructor
    private static class WriteMapping {
        public final String attribute;
        public final WriteMethod attributeWriteMethod;
        public final Variable variable;
        public final ReadMethod variableReadMethod;
    }
}
