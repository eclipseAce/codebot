package io.cruder.example.domain

import com.squareup.javapoet.*

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

class Actions {
    final ProcessingEnvironment processingEnv
    final TypeElement annotatedElement

    Actions(ProcessingEnvironment processingEnv, TypeElement annotatedElement) {
        this.processingEnv = processingEnv
        this.annotatedElement = annotatedElement
    }

    class Add {
        private String dtoName
        private Set<String> dtoFields

        def dtoName(dtoName) { this.dtoName = dtoName }

        def dtoFields(... dtoFields) { this.dtoFields = dtoFields }
    }

    def make(@DelegatesTo(Actions) Closure cl) {
        cl.delegate = this
        cl()
    }

    def add(@DelegatesTo(Add) Closure cl) {
        var add = new Add()
        cl.rehydrate(add, this, this).call()

        def (packageName, className) = splitQualifiedName(add.dtoName)
        TypeSpec.Builder typeBld = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
        for (ExecutableElement method : ElementFilter.methodsIn(annotatedElement.enclosedElements)) {
            if (!isSetter(method)) {
                continue;
            }
            var fieldName = getFieldName(method, "set")
            if (!add.dtoFields.contains(fieldName)) {
                continue
            }
            var fieldType = method.parameters[0].asType()
            if (fieldType.kind.primitive) {
                fieldType = processingEnv.typeUtils.boxedClass(fieldType).asType()
            }
            FieldSpec field = FieldSpec.builder(TypeName.get(fieldType), fieldName)
                    .addModifiers(Modifier.PRIVATE)
                    .build()
            MethodSpec getter = MethodSpec.methodBuilder("get" + fieldName.capitalize())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(field.type)
                    .addCode("return this.\$N;", field.name)
                    .build()
            MethodSpec setter = MethodSpec.methodBuilder("set" + fieldName.capitalize())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(field.type, field.name)
                    .addCode("this.\$N = \$N;", field.name, field.name)
                    .build()
            typeBld.addField(field).addMethod(getter).addMethod(setter)
        }
        JavaFile.builder(packageName, typeBld.build())
                .build()
                .writeTo(processingEnv.filer)
    }

    static isSetter(ExecutableElement method) {
        return method.simpleName.length() > 3
                && method.simpleName.startsWithAny("set")
                && method.parameters.size() == 1
    }

    static getFieldName(ExecutableElement method, String prefix) {
        var name = method.simpleName.toString()
        if (name.startsWith(prefix)) {
            name = name.substring(prefix.length()).uncapitalize()
        }
        return name.uncapitalize()
    }

    static splitQualifiedName(String name) {
        var dot = name.lastIndexOf('.')
        if (dot > -1) {
            return [name.substring(0, dot), name.substring(dot + 1, name.length())]
        }
        return ['', name]
    }
}

var actions = new Actions(__processingEnv, __annotatedElement)

actions.make {
    add {
        dtoName "io.cruder.example.generated.dto.${annotatedElement.simpleName}AddDTO"
        dtoFields 'username', 'password', 'mobile', 'email'
    }
}
