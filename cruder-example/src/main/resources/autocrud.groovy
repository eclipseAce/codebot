

import groovy.transform.BaseScript
import io.cruder.apt.script.CrudBuilder
import io.cruder.apt.script.JavaBuilder
import io.cruder.apt.script.ProcessingScript

@BaseScript
ProcessingScript script

//CrudBuilder.ofEntity(targetElement) {
//    fields {
//        field('username', title: '用户名')
//        field('password', title: '密码')
//        field('mobile', title: '手机号')
//        field('email', title: '邮箱')
//        field('locked', title: '是否锁定')
//        field('createdAt', title: '创建时间')
//        field('updatedAt', title: '更新时间')
//    }
//
//    insert('add', title: '新增用户', path: '/api/user/add') {
//        field('username,password', nonEmpty: true)
//        field('mobile,email')
//    }
//    update('setProfile', title: '更新用户资料', path: '/api/user/setProfile') {
//        field('mobile,email')
//    }
//    update('setLocked', title: '修改用户锁定状态', path: '/api/user/setLocked') {
//        field('locked')
//    }
//    select('getDetails', title: '查询用户列表', path: '/api/user/getDetails') {
//        field('username,password,mobile,email,locked,createdAt,updatedAt')
//    }
//}

def shell = new GroovyShell()


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
