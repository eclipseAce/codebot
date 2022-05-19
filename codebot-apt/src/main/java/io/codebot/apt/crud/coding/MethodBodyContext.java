package io.codebot.apt.crud.coding;

import com.google.common.collect.Lists;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.Variable;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MethodBodyContext {
    private final MethodSpec.Builder builder;
    private final List<LocalVariable> localVariables;
    private final NameAllocator nameAllocator;

    public MethodBodyContext(Executable method) {
        this.builder = MethodSpec.overriding(
                method.getElement(),
                service.getType().asDeclaredType(),
                service.getType().getFactory().getTypeUtils()
        );
        this.localVariables = Lists.newArrayList();
        this.nameAllocator = new NameAllocator();

        for (Variable param : method.getParameters()) {
            this.nameAllocator.newName(param.getSimpleName());
            this.localVariables.add(new LocalVariable(param.getSimpleName(), param.getType()));
        }
    }

    public NameAllocator getNameAllocator() {
        return nameAllocator;
    }

    public List<LocalVariable> getLocalVariables() {
        return Collections.unmodifiableList(localVariables);
    }

    public List<LocalVariable> findLocalVariables(Predicate<LocalVariable> filter) {
        return getLocalVariables().stream().filter(filter).collect(Collectors.toList());
    }
}
