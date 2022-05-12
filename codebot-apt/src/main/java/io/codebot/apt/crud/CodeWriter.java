package io.codebot.apt.crud;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.SetAccessor;
import io.codebot.apt.type.Type;

public class CodeWriter {
    private final CodeBlock.Builder codeBuilder;
    private final NameAllocator nameAllocator;

    public CodeWriter(Executable method) {
        this.codeBuilder = CodeBlock.builder();
        this.nameAllocator = new NameAllocator();
        method.getParameters().forEach(it -> nameAllocator.newName(it.getSimpleName()));
    }

    public CodeBlock getCode() {
        return codeBuilder.build();
    }

    public void copyProperty(String fromVar, GetAccessor fromGetter,
                             String toVar, SetAccessor toSetter) {
        codeBuilder.add("$1N.$2N($3N.$4N());\n",
                toVar, toSetter.getExecutable().getSimpleName(),
                fromVar, fromGetter.getExecutable().getSimpleName()
        );
    }

    public void copyProperties(String fromVar, Type fromType,
                               String toVar, Type toType) {
        for (GetAccessor fromGetter : fromType.getGetters()) {
            toType.findSetter(fromGetter.getAccessedName(), fromGetter.getAccessedType())
                    .ifPresent(toSetter -> copyProperty(fromVar, fromGetter, toVar, toSetter));
        }
    }

    public void setProperty(String valueVar, String targetVar, SetAccessor targetSetter) {
        codeBuilder.add("$1N.$2N($3N);\n")
    }

    public void getProperty(String targetVar, GetAccessor targetGetter) {
        codeBuilder.add("$1N.$2N()")
    }

    public String newInstance(String newVar, Type newType) {
        newVar = nameAllocator.newName(newVar);
        codeBuilder.add("$1T $2N = new $1T();\n", newType.asTypeElement(), newVar);
        return newVar;
    }
}
