package scripts

import groovy.transform.BaseScript
import io.cruder.apt.script.CrudBuilder
import io.cruder.apt.script.JavaBuilder
import io.cruder.apt.script.ProcessingScript

@BaseScript
ProcessingScript script

CrudBuilder.ofEntity(targetElement) {
    field('username', title: '用户名')
    field('password', title: '密码')
    field('mobile', title: '手机号')
    field('email', title: '邮箱')
    field('locked', title: '是否锁定')
    field('createdAt', title: '创建时间')
    field('updatedAt', title: '更新时间')

    insert('add', api: '/api/user/add', title: '新增用户') {
        field('username,password', nonEmpty: true)
        field('mobile,email')
    }
    update('setProfile', api: '/api/user/setProfile', title: '更新用户资料') {
        field('mobile,email')
    }
    update('setLocked', api: '/api/user/setLocked', title: '修改用户锁定状态') {
        field('locked')
    }
    select('getDetails', api: '/api/user/getDetails', title: '查询用户列表') {
        field('username,password,mobile,email,locked,createdAt,updatedAt')
    }
}

JavaBuilder.build(processingEnv.filer) {
    typeRef(
            'org.springframework.data.jpa.repository.JpaRepository',
            'org.mapstruct.Mapper',
            'org.mapstruct.MappingTarget',
            'org.springframework.stereotype.Repository',
            'org.springframework.stereotype.Service',
            'org.springframework.transaction.annotation.Transactional',
            'org.springframework.beans.factory.annotation.Autowired',
            'org.springframework.web.bind.annotation.RestController',
            'org.springframework.web.bind.annotation.RequestMapping',
            'org.springframework.web.bind.annotation.RequestBody',
            'org.springframework.web.bind.annotation.RequestMethod',
            'org.springframework.web.bind.annotation.RequestParam',
            'javax.validation.Valid',
            'io.cruder.example.core.ApiReply',
            'io.cruder.example.core.BusinessErrors',
            'io.swagger.v3.oas.annotations.media.Schema',

            theEntity: typeOf(targetElement),
            theRepository: 'io.cruder.example.generated.repository.UserRepository',
            theConverter: 'io.cruder.example.generated.converter.UserConverter',
            theService: 'io.cruder.example.generated.service.UserService',
            theController: 'io.cruder.example.generated.controller.UserController',
    )

    defInterface('theRepository', modifiers: 'public', extends: typeOf('JpaRepository', 'theEntity', 'Long')) {
        addAnnotation('Repository')
    }

    defInterface('theConverter', modifiers: 'public') {
        addAnnotation('Mapper', componentModel: 'spring')
    }

    defClass('theService', modifiers: 'public') {
        addAnnotation('Service')
        addField('repository', type: 'theRepository', modifiers: 'private') {
            addAnnotation('Autowired')
        }
        addField('converter', type: 'theConverter', modifiers: 'private') {
            addAnnotation('Autowired')
        }
    }

    defClass('theController', modifiers: 'public') {
        addAnnotation('RestController')
        addField('service', type: 'theService', modifiers: 'private') {
            addAnnotation('Autowired')
        }
    }
}
