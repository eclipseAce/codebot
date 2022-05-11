package io.codebot.apt.model.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.model.Entity;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.SetAccessor;
import io.codebot.apt.type.Type;

import java.util.List;

public class CodeUtils {
    public static final String PAGE_FQN = "org.springframework.data.domain.Page";

    public static List<CodeBlock> map(Type fromType, String fromVar,
                                      Type toType, String toVar) {
        List<CodeBlock> statements = Lists.newArrayList();
        for (GetAccessor fromGetter : fromType.getters()) {
            toType.findSetter(fromGetter.accessedName(), fromGetter.accessedType())
                    .ifPresent(toSetter -> statements.add(map(fromVar, fromGetter, toVar, toSetter)));
        }
        return statements;
    }

    public static CodeBlock map(String fromVar, GetAccessor fromGetter,
                                String toVar, SetAccessor toSetter) {
        return CodeBlock.of(
                "$[$1N.$2N($3N.$4N());\n$]",
                toVar, toSetter.simpleName(), fromVar, fromGetter.simpleName()
        );
    }

    public static List<CodeBlock> newAndMap(Type fromType, String fromVar, Type toType, String toVar) {
        List<CodeBlock> statements = Lists.newArrayList();
        statements.add(CodeBlock.of("$[$1T $2N = new $1T();\n$]", toType.asTypeMirror(), toVar));
        statements.addAll(map(fromType, fromVar, toType, toVar));
        return statements;
    }

    public static List<CodeBlock> newAndMapAndReturn(Type fromType, String fromVar, Type toType,
                                                     NameAllocator nameAlloc) {
        List<CodeBlock> statements = Lists.newArrayList();
        String tempVar = nameAlloc.newName("temp");
        statements.addAll(newAndMap(fromType, fromVar, toType, tempVar));
        statements.add(CodeBlock.of("$[return $N;\n$]", tempVar));
        return statements;
    }

    public static List<CodeBlock> mapFromEntityAndReturn(Type fromType, String fromVar, Type toType,
                                                         Entity entity, NameAllocator nameAlloc) {
        if (fromType.isSubtype(PAGE_FQN, entity.getType().asTypeMirror())
                && toType.erasure().isAssignableTo(PAGE_FQN)) {
            NameAllocator scopeNameAlloc = nameAlloc.clone();
            String itVar = scopeNameAlloc.newName("it");
            CodeBlock body = CodeBlock.join(mapFromEntityAndReturnInternal(
                    itVar, toType.typeArguments().get(0), entity, scopeNameAlloc
            ), "");
            return ImmutableList.of(CodeBlock.of(
                    "return $1N.map($2N -> {$>\n$3L$<});\n",
                    fromVar, itVar, body
            ));
        }
        return mapFromEntityAndReturnInternal(fromVar, toType, entity, nameAlloc);
    }

    private static List<CodeBlock> mapFromEntityAndReturnInternal(String entityVar, Type toType,
                                                                  Entity entity, NameAllocator nameAlloc) {
        List<CodeBlock> statements = Lists.newArrayList();
        if (toType.isAssignableFrom(entity.getIdType()) && entity.getIdReadAccessor() != null) {
            statements.add(CodeBlock.of("$[return $1N.$2N();\n$]", entityVar, entity.getIdReadAccessor().simpleName()));
        } //
        else if (toType.isDeclared()) {
            statements.addAll(CodeUtils.newAndMapAndReturn(entity.getType(), entityVar, toType, nameAlloc));
        } //
        else if (!toType.isVoid()){
            throw new IllegalArgumentException("Can't handle result type " + toType.asTypeMirror());
        }
        return statements;
    }
}
