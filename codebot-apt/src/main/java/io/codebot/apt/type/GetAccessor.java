package io.codebot.apt.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.type.TypeKind;
import java.util.List;

public class GetAccessor extends ExecutableAccessor {
    private static final String GETTER_PREFIX = "get";
    private static final String BOOLEAN_GETTER_PREFIX = "is";

    GetAccessor(String accessedName, Type accessedType, Executable executable) {
        super(accessedName, accessedType, executable);
    }

    public static List<GetAccessor> gettersOf(Type type) {
        List<GetAccessor> getters = Lists.newArrayList();
        for (Executable method : type.getMethods()) {
            String methodName = method.getSimpleName();
            if (methodName.length() > GETTER_PREFIX.length()
                    && methodName.startsWith(GETTER_PREFIX)
                    && method.getParameters().isEmpty()
                    && !method.getReturnType().isVoid()) {
                String accessedName = StringUtils.uncapitalize(methodName.substring(GETTER_PREFIX.length()));
                getters.add(new GetAccessor(accessedName, method.getReturnType(), method));
            } //
            else if (methodName.length() > BOOLEAN_GETTER_PREFIX.length()
                    && methodName.startsWith(BOOLEAN_GETTER_PREFIX)
                    && method.getParameters().isEmpty()
                    && method.getReturnType().getTypeMirror().getKind() == TypeKind.BOOLEAN) {
                String accessedName = StringUtils.uncapitalize(methodName.substring(BOOLEAN_GETTER_PREFIX.length()));
                getters.add(new GetAccessor(accessedName, method.getReturnType(), method));
            }
        }
        return ImmutableList.copyOf(getters);
    }
}