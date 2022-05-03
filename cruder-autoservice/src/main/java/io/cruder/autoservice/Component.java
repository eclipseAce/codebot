package io.cruder.autoservice;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;

public interface Component {
    void init(ProcessingContext ctx);

    ClassName getName();

    JavaFile createJavaFile();
}
