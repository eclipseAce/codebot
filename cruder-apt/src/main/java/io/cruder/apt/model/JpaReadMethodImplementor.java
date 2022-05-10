package io.cruder.apt.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.TypeSpec;
import io.cruder.apt.type.Accessor;
import io.cruder.apt.type.Type;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JpaReadMethodImplementor implements MethodImplementor {
    private static final String SPECIFICATION_FQN = "org.springframework.data.jpa.domain.Specification";
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";

    @Override
    public void implement(Service service, TypeSpec.Builder serviceBuilder,
                          Method method, MethodSpec.Builder methodBuilder,
                          NameAllocator nameAlloc) {
        if (!method.getSimpleName().startsWith("find")) {
            return;
        }
        Entity entity = service.getEntity();
        List<MethodParameter> directValues = Lists.newArrayList();
        List<MethodParameter> specificationValues = Lists.newArrayList();
        List<MethodParameter> pageableValues = Lists.newArrayList();
        List<Map.Entry<MethodParameter, Accessor>> nestedValues = Lists.newArrayList();
        for (MethodParameter param : method.getParameters()) {
            Optional<Accessor> directGetter = entity.getType()
                    .findReadAccessor(param.getName(), param.getType().asTypeMirror());
            if (directGetter.isPresent()) {
                directValues.add(param);
                continue;
            }
            if (param.getType().isSubtype(SPECIFICATION_FQN, entity.getType().asTypeMirror())) {
                specificationValues.add(param);
                continue;
            }
            if (param.getType().isSubtype(PAGEABLE_FQN)) {
                pageableValues.add(param);
                continue;
            }
            for (Accessor getter : param.getType().findReadAccessors()) {
                Optional<Accessor> entityGetter = entity.getType()
                        .findReadAccessor(getter.getAccessedName(), getter.getAccessedType());
                if (entityGetter.isPresent()) {
                    nestedValues.add(new AbstractMap.SimpleImmutableEntry<>(param, getter));
                }
            }
        }
    }
}
