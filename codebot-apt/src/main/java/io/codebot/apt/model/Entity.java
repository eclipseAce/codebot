package io.codebot.apt.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Type;
import io.codebot.apt.util.AnnotationUtils;

import javax.lang.model.element.VariableElement;
import java.util.Optional;

public class Entity {
    private final Type type;
    private final ClassName typeName;
    private final Type idType;
    private final TypeName idTypeName;
    private final String idName;
    private final GetAccessor idGetter;

    public Entity(Type type) {
        VariableElement idField = findIdField(type)
                .orElseThrow(() -> new IllegalArgumentException("Can't determin ID of entity " + type.typeMirror()));

        this.type = type;
        this.typeName = ClassName.get(type.asTypeElement());
        this.idType = type.factory().getType(type.asMember(idField));
        this.idTypeName = TypeName.get(idType.typeMirror());
        this.idName = idField.getSimpleName().toString();
        this.idGetter = type.findGetter(idName, idType).orElse(null);
    }

    public Type getType() {
        return type;
    }

    public ClassName getTypeName() {
        return typeName;
    }

    public Type getIdType() {
        return idType;
    }

    public TypeName getIdTypeName() {
        return idTypeName;
    }

    public String getIdName() {
        return idName;
    }

    public GetAccessor getIdGetter() {
        return idGetter;
    }

    private static Optional<VariableElement> findIdField(Type type) {
        return type.fields().stream()
                .filter(it -> AnnotationUtils.isPresent(it, "javax.persistence.Id"))
                .findFirst();
    }
}
