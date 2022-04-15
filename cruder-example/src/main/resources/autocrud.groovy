import com.squareup.javapoet.TypeName
import io.cruder.apt.bean.BeanInfo
import io.cruder.apt.dsl.TypesDSL

import javax.annotation.processing.ProcessingEnvironment

import static javax.lang.model.element.Modifier.*

final ProcessingEnvironment processingEnv = __processingEnv
final BeanInfo beanInfo = __beanInfo
final List<String> args = __args

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

    final theEntity = type(beanInfo.typeElement.qualifiedName.toString())
    final theAddDTO = type("io.cruder.example.generated.dto.${theEntity.simpleName()}AddDTO")
    final theDetailsDTO = type("io.cruder.example.generated.dto.${theEntity.simpleName()}DetailsDTO")
    final theRepository = type('io.cruder.example.generated.repository.UserRepository')
    final theConverter = type('io.cruder.example.generated.converter.UserConverter')
    final theService = type('io.cruder.example.generated.service.UserService')
    final theController = type('io.cruder.example.generated.controller.UserController')

    declClass([PUBLIC], theAddDTO, {
        property(type('java.lang.String'), 'username', {})
        property(type('java.lang.String'), 'password', {})
        property(type('java.lang.String'), 'mobile', {})
        property(type('java.lang.String'), 'email', {})
    })

    declClass([PUBLIC], theDetailsDTO, {
        property(type('java.lang.String'), 'username', {})
        property(type('java.lang.String'), 'mobile', {})
        property(type('java.lang.String'), 'email', {})
        property(TypeName.BOOLEAN.box(), 'locked', {})
    })

    declInterface([PUBLIC], theRepository, {
        superinterface(type(JpaRepository, theEntity, TypeName.LONG.box()))
        annotate(Repository)
    })

    declInterface([PUBLIC], theConverter, {
        annotate(Mapper, { member('componentModel', '$S', 'spring') })
        method([PUBLIC, ABSTRACT], 'convertAddToEntity', {
            parameter(theEntity, 'entity', { annotate(MappingTarget) })
            parameter(theAddDTO, 'dto')
        })
        method([PUBLIC, ABSTRACT], 'convertEntityToDetails', {
            parameter(theEntity, 'entity')
            returns(theDetailsDTO)
        })
    })

    declClass([PUBLIC], theService, {
        annotate(Service)
        field([PRIVATE], theRepository, 'repository', { annotate(Autowired) })
        field([PRIVATE], theConverter, 'converter', { annotate(Autowired) })
        method([PUBLIC], 'add', {
            annotate(Transactional)
            parameter(theAddDTO, 'dto')
            returns(TypeName.LONG)
            body('''
$T entity = new $T();
converter.convertAddToEntity(entity, dto);
repository.save(entity);
return entity.getId();
''', theEntity, theEntity)
        })
        method([PUBLIC], 'getDetails', {
            parameter(TypeName.LONG, 'id')
            returns(theDetailsDTO)
            body('''
$T entity = repository.findById(id)
    .orElseThrow(() -> $T.ENTITY_NOT_FOUND.withMessage("user not exists").toException());
return converter.convertEntityToDetails(entity);
''', theEntity, BusinessErrors)
        })
    })

    declClass([PUBLIC], theController, {
        annotate(RestController)
        field([PRIVATE], theService, 'service', { annotate(Autowired) })
        method([PUBLIC], 'add', {
            annotate(RequestMapping, {
                member('method', '$T.POST', RequestMethod)
                member('path', '$S', '/api/user/add')
            })
            parameter(theAddDTO, 'body', { annotate(RequestBody, Valid) })
            returns(type(ApiReply, TypeName.LONG.box()))
            body('return $T.ok(service.add(body));', ApiReply)
        })
        method([PUBLIC], 'add', {
            annotate(RequestMapping, {
                member('method', '$T.GET', RequestMethod)
                member('path', '$S', '/api/user/details')
            })
            parameter(TypeName.LONG.box(), 'id', {
                annotate(RequestParam, { member('name', '$S', 'id')})
            })
            returns(type(ApiReply, theDetailsDTO))
            body('return $T.ok(service.getDetails(id));', ApiReply)
        })
    })
}) build() values() each { it.writeTo(processingEnv.filer) }