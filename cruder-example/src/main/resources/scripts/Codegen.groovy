package scripts


import groovy.transform.BaseScript
import io.cruder.apt.script.CrudBuilder
import io.cruder.apt.script.JavaBuilder
import io.cruder.apt.script.ProcessingScript

CrudBuilder.of(processingEnv, element) {
    fields {
        field('id', label: '用户ID')
        field('username', label: '用户名', nonEmpty: true, length: [6, 20])
        field('password', label: '密码', nonEmpty: true, length: [8, 16])
        field('mobile', label: '手机号码', length: [-1, 20])
        field('email', label: '邮箱', length: [-1, 50])
        field('locked', label: '是否锁定')
        field('createdAt', label: '创建时间')
        field('updatedAt', label: '修改时间')
    }

    actions {
        create('add', label: '创建用户') {
            field('username,password,mobile,email')
        }
        update('setProfile', label: '修改用户资料') {
            field('mobile,email')
        }
        update('setPassword', label: '设置用户密码') {
            field('password')
        }
        update('setLocked', label: '设置用户锁定状态') {
            field('locked', nonEmpty: true)
        }
        read('getDetails', label: '获取用户详情') {
            field('id,username,mobile,email,locked,createdAt,updatedAt')
        }
        read('getPage', label: '获取用户列表') {
            findByFilter {
                contains('username,mobile')
                range('createdAt')
                matches('locked')
            }
            field('id,username,mobile,email,locked,createdAt,updatedAt')
        }
    }
}

@BaseScript
ProcessingScript theScript

JavaBuilder.build(processingEnv.filer) {
    def entityName = classOf(element).simpleName()
    typeRef(
            'org.springframework.data.jpa.repository.JpaRepository',
            'org.springframework.data.jpa.repository.JpaSpecificationExecutor',
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

            theEntity: classOf(element),
            theRepository: "io.cruder.example.generated.repository.${entityName}Repository",
            theConverter: "io.cruder.example.generated.converter.${entityName}Converter",
            theService: "io.cruder.example.generated.service.${entityName}Service",
            theController: "io.cruder.example.generated.controller.${entityName}Controller",
    )

    defInterface('theRepository', modifiers: 'public', extends: [
            typeOf('JpaRepository', 'theEntity', 'Long'),
            typeOf('JpaSpecificationExecutor', 'theEntity'),
    ]) {
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
