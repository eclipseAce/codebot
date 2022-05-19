package io.codebot.apt.crud.coding;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.crud.query.Expression;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Map;

public class CodingContext {
    private final Types typeUtils;
    private final Elements elementUtils;
    private final NameAllocator nameAllocator;
    private final CodeBlock.Builder codeBuilder;
    private final List<Variable> variables;

    public CodingContext(Types typeUtils, Elements elementUtils) {
        this.typeUtils = typeUtils;
        this.elementUtils = elementUtils;
        this.nameAllocator = new NameAllocator();
        this.codeBuilder = CodeBlock.builder();
        this.variables = Lists.newArrayList();
    }

    public CodingContext(Types typeUtils, Elements elementUtils, ExecutableElement method, DeclaredType containing) {
        this(typeUtils, elementUtils);
        ExecutableType methodType = (ExecutableType) typeUtils.asMemberOf(containing, method);
        List<? extends VariableElement> params = method.getParameters();
        List<? extends TypeMirror> paramTypes = methodType.getParameterTypes();
        for (int i = 0; i < params.size(); i++) {
            String paramName = params.get(0).getSimpleName().toString();
            this.nameAllocator.newName(paramName);
            this.variables.add(new Variable(paramName, paramTypes.get(0)));
        }
    }

    public NameAllocator getNameAllocator() {
        return nameAllocator;
    }

    public CodeBlock.Builder getCodeBuilder() {
        return codeBuilder;
    }

    public static class Variable {
        private String name;
        private TypeMirror type;

        Variable(String name, TypeMirror type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public TypeMirror getType() {
            return type;
        }
    }
}
