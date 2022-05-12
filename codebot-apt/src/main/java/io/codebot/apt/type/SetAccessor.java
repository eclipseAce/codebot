package io.codebot.apt.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class SetAccessor extends ExecutableAccessor {
    private static final String SETTER_PREFIX = "set";

    SetAccessor(String accessedName, Type accessedType, Executable executable) {
        super(accessedName, accessedType, executable);
    }

    public static List<SetAccessor> settersOf(Type type) {
        List<SetAccessor> setters = Lists.newArrayList();
        for (Executable method : type.getMethods()) {
            String methodName = method.getSimpleName();
            if (methodName.length() > SETTER_PREFIX.length()
                    && methodName.startsWith(SETTER_PREFIX)
                    && method.getParameters().size() == 1) {
                String accessedName = StringUtils.uncapitalize(methodName.substring(SETTER_PREFIX.length()));
                setters.add(new SetAccessor(accessedName, method.getParameters().get(0).getType(), method));
            }
        }
        return ImmutableList.copyOf(setters);
    }
}