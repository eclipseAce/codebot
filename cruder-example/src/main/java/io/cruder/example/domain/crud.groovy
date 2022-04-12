package io.cruder.example.domain

import com.squareup.javapoet.*
import io.cruder.apt.bean.BeanInfo

import javax.lang.model.element.Modifier

def findFields(Collection<BeanInfo.Property> props, String ...names) {
    props.findAll { prop -> names.contains(prop.name) }.collect { prop ->
        FieldSpec.builder(TypeName.get(prop.type).box(), prop.name, Modifier.PRIVATE).build()
    }
}

def writableFields(String ...names) {
    findFields(__beanInfo.writableProperties.values(), names)
}

def readableFields(String ...names) {
    findFields(__beanInfo.readableProperties.values(), names)
}

def beanClass(String name, List<FieldSpec> fields) {
    TypeSpec.Builder typeBld = TypeSpec.classBuilder(name)
            .addModifiers(Modifier.PUBLIC)
    fields.each { field ->
        typeBld.addField(field)
                .addMethod(MethodSpec.methodBuilder("get" + field.name.capitalize())
                        .addModifiers(Modifier.PUBLIC)
                        .returns(field.type)
                        .addCode("return this.\$N;", field.name)
                        .build())
                .addMethod(MethodSpec.methodBuilder("set" + field.name.capitalize())
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(field.type, field.name)
                        .addCode("this.\$N = \$N;", field.name, field.name)
                        .build())
    }
    typeBld.build()
}

var namePrefix = __beanInfo.typeElement.simpleName
var dtoTypes = [
        beanClass("${namePrefix}AddDTO",
                writableFields('username', 'password', 'mobile', 'email')),
        beanClass("${namePrefix}SetProfileDTO",
                readableFields('id') + writableFields('username', 'password', 'mobile', 'email')),
        beanClass("${namePrefix}SetLockedDTO",
                readableFields('id') + writableFields('locked')),
        beanClass("${namePrefix}DetailsDTO",
                readableFields('id', 'username', 'locked', 'mobile', 'email', 'createdAt', 'updatedAt')),
        beanClass("${namePrefix}SummaryDTO",
                readableFields('id', 'username', 'createdAt', 'updatedAt')),
]

dtoTypes.each { type ->
    JavaFile.builder("io.cruder.example.generated.dto", type)
            .build()
            .writeTo(__processingEnv.filer)
}