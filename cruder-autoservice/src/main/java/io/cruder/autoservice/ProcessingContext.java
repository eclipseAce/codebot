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
    private final Map<ClassName, ServiceMapperComponent> serviceMappers = Maps.newLinkedHashMap();

    public ProcessingContext(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.utils = new ProcessingUtils(processingEnv);
    }

    public Collection<? extends Component> getComponents() {
        return Lists.newArrayList(Iterables.concat(
                repositories.values(),
                serviceImpls.values(),
                serviceMappers.values()));
    }

    public RepositoryComponent getRepositoryComponent(EntityDescriptor entity) {
        ClassName entityName = ClassName.get(entity.getEntityElement());
        return repositories.computeIfAbsent(entityName, k -> {
            String pkg = entityName.packageName();
            int sepIndex = pkg.lastIndexOf('.');
            if (sepIndex > -1) {
                pkg = pkg.substring(0, sepIndex) + ".repository";
            }
            RepositoryComponent c = new RepositoryComponent(
                    ClassName.get(pkg, entityName.simpleName() + "Repository"),
                    entity
            );
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

    public ServiceMapperComponent getServiceMapperComponent(ServiceDescriptor service) {
        ClassName serviceName = ClassName.get(service.getServiceElement());
        return serviceMappers.computeIfAbsent(serviceName, k -> {
            ServiceMapperComponent c = new ServiceMapperComponent(
                    ClassName.get(serviceName.packageName(), serviceName.simpleName() + "Mapper")
            );
            c.init(this);
            return c;
        });
    }
}
