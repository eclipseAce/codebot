package io.cruder.example.domain

import com.squareup.javapoet.*
import io.cruder.apt.dsl.TypeDsl

import static javax.lang.model.element.Modifier.*

TypeDsl.classDecl([PUBLIC], 'UserService', {
    method([PUBLIC], 'add', {

    })
})