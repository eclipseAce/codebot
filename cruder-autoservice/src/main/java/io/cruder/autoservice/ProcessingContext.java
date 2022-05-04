package io.cruder.autoservice;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.squareup.javapoet.ClassName;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.Collection;
import java.util.Map;

public final class ProcessingContext {
    public final ProcessingEnvironment processingEnv;
    public final ProcessingUtils utils;

    private final Map<ClassName, RepositoryComponent> repositories = Maps.newLinkedHashMap();
    private final Map<ClassName, ServiceImplComponent> serviceImpls = Maps.newLinkedHashMap();
    private final Map<ClassName, MapperComponent> serviceMappers = Maps.newLinkedHashMap();
    private final Map<ClassName, ServiceControllerComponent> serviceControllers = Maps.newLinkedHashMap();

    public ProcessingContext(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.utils = new ProcessingUtils(processingEnv);
    }

    public Collection<? extends Component> getComponents() {
        return Lists.newArrayList(Iterables.concat(
                repositories.values(),
                serviceImpls.values(),
                serviceMappers.values(),
                serviceControllers.values()));
    }

    public RepositoryComponent getRepositoryComponent(EntityDescriptor entity) {
        ClassName entityName = ClassName.get(entity.getBeanElement());
        return repositories.computeIfAbsent(entityName, k -> {
            ClassName repositoryName = ClassName.get(
                    getSiblingPackage(entityName.packageName(), "repository"),
                    entityName.simpleName() + "Repository");
            RepositoryComponent c = new RepositoryComponent(repositoryName, entity);
            c.init(this);
            return c;
        });
    }

    public ServiceImplComponent getServiceImplComponent(ServiceDescriptor service) {
        ClassName serviceName = ClassName.get(service.getServiceElement());
        return serviceImpls.computeIfAbsent(serviceName, k -> {
            ServiceImplComponent c = new ServiceImplComponent(
                    ClassName.get(serviceName.packageName(), serviceName.simpleName() + "Impl"),
                    service
            );
            c.init(this);
            return c;
        });
    }

    public MapperComponent getServiceMapperComponent(ServiceDescriptor service) {
        ClassName serviceName = ClassName.get(service.getServiceElement());
        return serviceMappers.computeIfAbsent(serviceName, k -> {
            MapperComponent c = new MapperComponent(
                    ClassName.get(serviceName.packageName(), serviceName.simpleName() + "Mapper")
            );
            c.init(this);
            return c;
        });
    }

    public ServiceControllerComponent getServiceControllerComponent(ServiceDescriptor service) {
        ClassName serviceName = ClassName.get(service.getServiceElement());
        return serviceControllers.computeIfAbsent(serviceName, k -> {
            ClassName serviceControllerName = ClassName.get(
                    getSiblingPackage(serviceName.packageName(), "controller"),
                    serviceName.simpleName() + "Controller");
            ServiceControllerComponent c = new ServiceControllerComponent(serviceControllerName, service);
            c.init(this);
            return c;
        });
    }

    private String getSiblingPackage(String pkg, String sibling) {
        int sepIndex = pkg.lastIndexOf('.');
        if (sepIndex > -1) {
            pkg = pkg.substring(0, sepIndex) + "." + sibling;
        }
        return pkg;
    }
}
