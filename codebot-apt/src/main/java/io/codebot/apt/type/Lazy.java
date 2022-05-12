package io.codebot.apt.type;

import java.util.function.Supplier;

@FunctionalInterface
public interface Lazy<T> {
    T get();

    static <T> Lazy<T> constant(T value) {
        return () -> value;
    }

    static <T> Lazy<T> of(Supplier<T> loader) {
        return new Lazy<T>() {
            private volatile T value;

            @Override
            public T get() {
                if (value == null) {
                    synchronized (this) {
                        if (value == null) {
                            value = loader.get();
                        }
                    }
                }
                return value;
            }
        };
    }
}
