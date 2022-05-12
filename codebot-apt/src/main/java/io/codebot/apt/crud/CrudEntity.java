package io.codebot.apt.crud;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.Variable;

public class CrudEntity {
    public final Type type;
    public final ClassName typeName;
    public final Variable idField;
    public final GetAccessor idGetter;
    public final Type idType;
    public final TypeName idTypeName;
    public final String idName;

    public CrudEntity(Type type) {
        this.type = type;
        this.typeName = ClassName.get(type.asTypeElement());
        this.idType = ;
        this.idTypeName = idTypeName;
        this.idName = idName;
        this.idGetter = idGetter;
    }
}
