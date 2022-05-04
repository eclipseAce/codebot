package io.cruder.autoservice;

import com.squareup.javapoet.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;

@RequiredArgsConstructor
public class RepositoryComponent implements Component {
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
                        ClassNames.SpringData.JpaRepository,
                        entity.getClassName(),
                        TypeName.get(entity.getIdProperty().getType()).box()
                ))
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassNames.SpringData.JpaSpecificationExecutor,
                        entity.getClassName()
                ))
                .addAnnotation(ClassNames.Spring.Repository)
                .build();
        return JavaFile.builder(name.packageName(), type).build();
    }
}
