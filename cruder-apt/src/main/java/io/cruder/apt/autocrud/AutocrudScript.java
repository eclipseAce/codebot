package io.cruder.apt.autocrud;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import io.cruder.apt.PreCompileScript;
import io.cruder.apt.dsl.TypesDSL;

public abstract class AutocrudScript extends PreCompileScript {
    public TypesDSL declTypes(@DelegatesTo(TypesDSL.class) Closure<?> cl) {
        return TypesDSL.declTypes(cl);
    }
}
