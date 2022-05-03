package io.cruder.autoservice;

import com.squareup.javapoet.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;

@RequiredArgsConstructor
public final class RepositoryComponent implements Component {
    private final @Getter ClassName name;
    private final @Getter EntityDescriptor entity;

    @Override
    public void init(ProcessingContext ctx) {
    }

    @Override
    public JavaFile createJavaFile() {
        TypeSpec type = TypeSpec.interfaceBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get("org.springframework.data.jpa.repository", "JpaRepository"),
                        ClassName.get(entity.getEntityElement()),
                        TypeName.get(entity.getIdField().getType()).box()
                ))
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get("org.springframework.data.jpa.repository", "JpaSpecificationExecutor"),
                        ClassName.get(entity.getEntityElement())
                ))
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Repository"))
                .build();
        return JavaFile.builder(name.packageName(), type).build();
    }
}
