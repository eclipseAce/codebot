package io.codebot.apt.type;

public abstract class ExecutableAccessor implements Accessor {
    private final String accessedName;
    private final Type accessedType;
    private final Executable executable;

    ExecutableAccessor(String accessedName, Type accessedType, Executable executable) {
        this.accessedName = accessedName;
        this.accessedType = accessedType;
        this.executable = executable;
    }

    @Override
    public String getAccessedName() {
        return accessedName;
    }

    @Override
    public Type getAccessedType() {
        return accessedType;
    }

    public Executable getExecutable() {
        return executable;
    }

    public String getExecutableName() {
        return executable.getSimpleName();
    }
}
