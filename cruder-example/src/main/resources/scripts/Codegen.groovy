package scripts

import groovy.transform.BaseScript
import io.cruder.apt.script.JavaBuilder
import io.cruder.apt.script.ProcessingScript

import javax.lang.model.element.TypeElement

class Autocrud {
    def fieldsFrom(TypeElement typeElement) {

    }

    def fieldsHint(Map hints, String ...names) {

    }
}

Autocrud.define {
    fieldsFrom(targetElement)
    fields('id', title: '用户ID')
    fields('username', title: '用户名', nonEmpty: true, length: [6, 20])
    fields('password', title: '密码', nonEmpty: true, length: [8, 16])
    fields('mobile', title: '手机号码', length: [-1, 20])
    fields('email', title: '邮箱', length: [-1, 50])
    fields('locked', title: '是否锁定')
    fields('createdAt', title: '创建时间')
    fields('updatedAt', title: '修改时间')
    createAction('add', title: '创建用户') {
        fields('username,password,mobile,email')
    }
    updateAction('setProfile', title: '修改用户资料') {
        byId
        fields('mobile,email')
    }
    updateAction('setPassword', title: '设置用户密码') {
        byId
        fields('password')
    }
    updateAction('setLocked', title: '设置用户锁定状态') {
        byId
        fields('locked', nonEmpty: true)
    }
    readAction('getDetails', title: '获取用户详情') {
        byId
        fields('id,username,mobile,email,locked,createdAt,updatedAt')
    }
    readAction('getPage', title: '查询用户') {
        byFilter {
            contains('username,mobile')
            range('createdAt')
            matches('locked')
        }
        fields('id,username,mobile,email,locked,createdAt,updatedAt')
        pageable
    }
}

@BaseScript
ProcessingScript theScript

JavaBuilder.build(processingEnv.filer) {
    def entityName = classOf(targetElement).simpleName()
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

            theEntity: classOf(targetElement),
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
