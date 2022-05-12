package io.codebot.apt.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.type.TypeKind;
import java.util.List;

public class GetAccessor implements Accessor {
    private final String accessedName;
    private final Type accessedType;
    private final Executable executable;

    GetAccessor(String accessedName, Type accessedType, Executable executable) {
        this.accessedName = accessedName;
        this.accessedType = accessedType;
        this.executable = executable;
    }

    @Override
    public String accessedName() {
        return accessedName;
    }

    @Override
    public Type accessedType() {
        return accessedType;
    }

    public Executable executable() {
        return executable;
    }

    private static final String GETTER_PREFIX = "get";
    private static final String BOOLEAN_GETTER_PREFIX = "is";

    public static List<GetAccessor> from(Type type) {
        List<GetAccessor> getters = Lists.newArrayList();
        for (Executable method : type.methods()) {
            String methodName = method.simpleName();
            if (methodName.length() > GETTER_PREFIX.length()
                    && methodName.startsWith(GETTER_PREFIX)
                    && method.parameters().isEmpty()
                    && !method.returnType().isVoid()) {
                String accessedName = StringUtils.uncapitalize(methodName.substring(GETTER_PREFIX.length()));
                getters.add(new GetAccessor(accessedName, method.returnType(), method));
            } //
            else if (methodName.length() > BOOLEAN_GETTER_PREFIX.length()
                    && methodName.startsWith(BOOLEAN_GETTER_PREFIX)
                    && method.parameters().isEmpty()
                    && method.returnType().typeMirror().getKind() == TypeKind.BOOLEAN) {
                String accessedName = StringUtils.uncapitalize(methodName.substring(BOOLEAN_GETTER_PREFIX.length()));
                getters.add(new GetAccessor(accessedName, method.returnType(), method));
            }
        }
        return ImmutableList.copyOf(getters);
    }
}