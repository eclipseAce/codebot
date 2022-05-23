package io.codebot.apt.crud;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.codebot.apt.type.Field;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Type;

public class Entity {
    private final Type type;
    private final ClassName typeName;
    private final Field idField;
    private final String idName;
    private final Type idType;
    private final TypeName idTypeName;
    private final GetAccessor idGetter;

    public Entity(Type type) {
        this.type = type;
        this.typeName = ClassName.get(type.asTypeElement());
        this.idField = type.getFields().stream()
                .filter(it -> it.isAnnotationPresent("javax.persistence.Id"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't determin @Id of entity " + type));
        this.idName = idField.getSimpleName();
        this.idType = idField.getType();
        this.idTypeName = TypeName.get(idType.getTypeMirror());
        this.idGetter = type.findGetter(idName, idType.getTypeMirror())
                .orElseThrow(() -> new IllegalArgumentException("Entity id must be readable"));
    }

    public Type getType() {
        return type;
    }

    public ClassName getTypeName() {
        return typeName;
    }

    public Field getIdField() {
        return idField;
    }

    public String getIdName() {
        return idName;
    }

    public Type getIdType() {
        return idType;
    }

    public TypeName getIdTypeName() {
        return idTypeName;
    }

    public GetAccessor getIdGetter() {
        return idGetter;
    }
}
