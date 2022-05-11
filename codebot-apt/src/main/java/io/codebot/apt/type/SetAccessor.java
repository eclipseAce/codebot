package io.codebot.apt.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class SetAccessor implements Accessor {
    private final String accessedName;
    private final Type accessedType;
    private final Executable executable;

    protected SetAccessor(String accessedName, Type accessedType, Executable executable) {
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

    private static final String SETTER_PREFIX = "set";

    public static List<SetAccessor> from(Type type) {
        List<SetAccessor> setters = Lists.newArrayList();
        for (Executable method : type.methods()) {
            String methodName = method.simpleName();
            if (methodName.length() > SETTER_PREFIX.length()
                    && methodName.startsWith(SETTER_PREFIX)
                    && method.parameters().size() == 1) {
                String accessedName = StringUtils.uncapitalize(methodName.substring(SETTER_PREFIX.length()));
                setters.add(new SetAccessor(accessedName, method.parameters().get(0).type(), method));
            }
        }
        return ImmutableList.copyOf(setters);
    }
}