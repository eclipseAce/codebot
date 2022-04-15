import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import io.cruder.apt.dsl.TypesDSL

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import java.lang.reflect.Modifier

import static javax.lang.model.element.Modifier.*

class AutocrudBuilder extends BuilderSupport {
    private final Map bean

    AutocrudBuilder(ProcessingEnvironment processingEnv, TypeElement beanElement) {
        this.bean = scanBean(processingEnv, beanElement)
    }

    @Override
    protected void setParent(Object parent, Object child) {
        var children = parent.computeIfAbsent(child.type, k -> [])
        if (child.type == 'field') {
            (child.value ?: '')
                    .split(',')
                    .each { it.trim() }
                    .findAll { !it.empty && bean.fields.containsKey(it) }
                    .each {
                        children << [
                                type : 'field',
                                value: it,
                                attrs: child.attrs << [type: bean.fields[it].type]
                        ]
                    }
        } else {
            children << child
        }
    }

    @Override
    protected void nodeCompleted(Object parent, Object node) {
        if (node.type == 'define') {
            ['insert', 'update', 'select']
                    .collectMany { node[it] as List ?: [] }
                    .collectMany { it.field as List ?: [] }
                    .each { field ->
                        var attrs = [:]
                        (node.field as List)
                                .findAll { it.value == field.value }
                                .each { attrs << (it.attrs as Map) }
                        attrs << (field.attrs as Map)
                        field.attrs = attrs
                    }
            node.bean = bean
        }
    }

    @Override
    protected Object createNode(Object name) {
        createNode(name, null)
    }

    @Override
    protected Object createNode(Object name, Object value) {
        createNode(name, [:], value)
    }

    @Override
    protected Object createNode(Object name, Map attributes) {
        createNode(name, attributes, null)
    }

    @Override
    protected Object createNode(Object name, Map attributes, Object value) {
        [type: name, value: value, attrs: attributes ?: [:]]
    }

    static scanBean(ProcessingEnvironment processingEnv, TypeElement beanElement) {
        var fields = [:]
        for (var e = beanElement;
             !e.qualifiedName.contentEquals('java.lang.Object');
             e = processingEnv.typeUtils.asElement(e.superclass) as TypeElement) {
            ElementFilter.fieldsIn(e.enclosedElements)
                    .findAll {
                        !it.modifiers.contains(Modifier.STATIC)
                                && !fields.containsKey(it.simpleName.toString())
                    }
                    .each {
                        var name = it.simpleName.toString()
                        var id = it.annotationMirrors.any {
                            (it.annotationType.asElement() as TypeElement)
                                    .qualifiedName.contentEquals('javax.persistence.Id')
                        }
                        fields[name] = [name: name, type: ClassName.get(it.asType()), id: id]
                    }
        }
        [
                type   : TypeName.get(beanElement.asType()),
                fields : fields,
                idField: fields.values().find { it.id }
        ]
    }
}


var define = new AutocrudBuilder(__processingEnv, __element).define {
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
    update('setProfile', findBy: 'findById', api: '/api/user/setProfile', title: '更新用户资料') {
        field('mobile,email')
    }
    update('setLocked', findBy: 'findById', api: '/api/user/setLocked', title: '修改用户锁定状态') {
        field('locked')
    }
    select('getDetails', findBy: 'findById', api: '/api/user/getDetails', title: '查询用户列表') {
        field('username,password,mobile,email,locked,createdAt,updatedAt')
    }
}

println(define)

TypesDSL.decls({
    final JpaRepository = type('org.springframework.data.jpa.repository.JpaRepository')
    final Mapper = type('org.mapstruct.Mapper')
    final MappingTarget = type('org.mapstruct.MappingTarget')
    final Repository = type('org.springframework.stereotype.Repository')
    final Service = type('org.springframework.stereotype.Service')
    final RestController = type('org.springframework.web.bind.annotation.RestController')
    final Transactional = type('org.springframework.transaction.annotation.Transactional')
    final Autowired = type('org.springframework.beans.factory.annotation.Autowired')
    final RequestMapping = type('org.springframework.web.bind.annotation.RequestMapping')
    final RequestBody = type('org.springframework.web.bind.annotation.RequestBody')
    final RequestMethod = type('org.springframework.web.bind.annotation.RequestMethod')
    final RequestParam = type('org.springframework.web.bind.annotation.RequestParam')
    final Valid = type('javax.validation.Valid')
    final ApiReply = type('io.cruder.example.core.ApiReply')
    final BusinessErrors = type('io.cruder.example.core.BusinessErrors')
    final Schema = type('io.swagger.v3.oas.annotations.media.Schema')

    final theEntity = define.bean.type as ClassName
    final theRepository = type('io.cruder.example.generated.repository.UserRepository')
    final theConverter = type('io.cruder.example.generated.converter.UserConverter')
    final theService = type('io.cruder.example.generated.service.UserService')
    final theController = type('io.cruder.example.generated.controller.UserController')

    final basePackage = 'io.cruder.example.generated'

    declInterface([PUBLIC], theRepository, {
        superinterface(type(JpaRepository, theEntity, TypeName.LONG.box()))
        annotate(Repository)
    })

    declInterface([PUBLIC], theConverter, {
        annotate(Mapper, { member('componentModel', '$S', 'spring') })
    })

    declClass([PUBLIC], theService, {
        annotate(Service)
        field([PRIVATE], theRepository, 'repository', { annotate(Autowired) })
        field([PRIVATE], theConverter, 'converter', { annotate(Autowired) })
    })

    declClass([PUBLIC], theController, {
        annotate(RestController)
        field([PRIVATE], theService, 'service', { annotate(Autowired) })
    })

    define.insert.each { insert ->
        final theDTO = type("${basePackage}.dto.${theEntity.simpleName()}${insert.value.capitalize()}DTO")

        declClass([PUBLIC], theDTO, {
            annotate(Schema, { member('description', '$S', insert.attrs.title) })
            insert.field.each { field ->
                property(field.attrs.type, field.value, {
                    annotateField(Schema, { member('description', '$S', field.attrs.title) })
                })
            }
        })

        redecl(theConverter, {
            method([PUBLIC, ABSTRACT], "convert${insert.value.capitalize()}ToEntity", {
                parameter(theEntity, 'entity', { annotate(MappingTarget) })
                parameter(theDTO, 'dto')
            })
        })

        redecl(theService, {
            method([PUBLIC], insert.value as String, {
                annotate(Transactional)
                parameter(theDTO, 'dto')
                returns(define.bean.idField.type)
                body('''
$T entity = new $T();
converter.$L(entity, dto);
repository.save(entity);
return entity.getId();
''', theEntity, theEntity, "convert${insert.value.capitalize()}ToEntity")
            })
        })
    }
}) build() values() each { it.writeTo(__processingEnv.filer) }