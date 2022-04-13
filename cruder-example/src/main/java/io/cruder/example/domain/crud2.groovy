package io.cruder.example.domain


import com.squareup.javapoet.TypeName
import io.cruder.apt.bean.BeanInfo
import io.cruder.apt.dsl.TypesDSL

import javax.annotation.processing.ProcessingEnvironment

import static javax.lang.model.element.Modifier.*

final ProcessingEnvironment processingEnv = __processingEnv
final BeanInfo beanInfo = __beanInfo

TypesDSL.decls({
    final JpaRepository = type('org.springframework.data.jpa.repository.JpaRepository')
    final Repository = type('org.springframework.stereotype.Repository')
    final Service = type('org.springframework.stereotype.Service')
    final RestController = type('org.springframework.web.bind.annotation.RestController')
    final Transactional = type('org.springframework.transaction.annotation.Transactional')
    final Autowired = type('org.springframework.beans.factory.annotation.Autowired')
    final RequestMapping = type('org.springframework.web.bind.annotation.RequestMapping')
    final RequestBody = type('org.springframework.web.bind.annotation.RequestBody')
    final RequestMethod = type('org.springframework.web.bind.annotation.RequestMethod')
    final Valid = type('javax.validation.Valid')
    final ApiReply = type('io.cruder.example.core.ApiReply')

    final theEntity = type(beanInfo.typeElement.qualifiedName.toString())
    final theAddDTO = type("io.cruder.example.generated.dto.${theEntity.simpleName()}AddDTO")
    final theRepository = type('io.cruder.example.generated.repository.UserRepository')
    final theService = type('io.cruder.example.generated.service.UserService')
    final theController = type('io.cruder.example.generated.controller.UserController')

    declClass([PUBLIC], theAddDTO, {
        property(type('java.lang.String'), 'username', {})
        property(TypeName.BOOLEAN, 'locked', {})
    })

    declInterface([PUBLIC], theRepository, {
        superinterface(type(JpaRepository, theEntity, TypeName.LONG.box()))
        annotate(Repository)
    })

    declClass([PUBLIC], theService, {
        annotate(Service)
        field([PRIVATE], theRepository, 'repository', {
            annotate(Autowired)
        })
        method([PUBLIC], 'add', {
            annotate(Transactional)
            parameter(TypeName.LONG.box(), 'id')
            returns(TypeName.LONG)
            body('return 0L;')
        })
    })

    declClass([PUBLIC], theController, {
        annotate(RestController)
        annotate(RequestMapping, {
            member('path', '$S', '/api/user')
        })
        field([PRIVATE], theService, 'service', {
            annotate(Autowired)
        })
        method([PUBLIC], 'add', {
            annotate(RequestMapping, {
                member('method', '$T.POST', RequestMethod)
                member('path', '$S', '/add')
            })
            parameter(TypeName.LONG.box(), 'id', {
                annotate(RequestBody)
                annotate(Valid)
            })
            returns(type(ApiReply, TypeName.LONG.box()))
            body('return $T.ok(service.add(null));', ApiReply)
        })
    })
}).files.each { it.writeTo(processingEnv.filer) }