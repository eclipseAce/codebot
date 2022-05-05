package io.cruder.autoservice.model;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Map;

public class Bean {
    private TypeElement beanElement;
    private Map<String, Property> properties;

    public static class Property {
        private VariableElement fieldElement;
        private ExecutableElement getterElement;
        private ExecutableElement setterElement;
    }

    public static Bean of(ProcessingEnvironment env, TypeElement element) {
        return null;
    }
}
