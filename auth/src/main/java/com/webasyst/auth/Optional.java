package com.webasyst.auth;

import java.util.Objects;

/**
 * Compat {@link java.util.Optional} implementation for pre-24 devices. Functionality is limited.
 */
public class Optional<T> {
    private static final Optional<?> EMPTY = new Optional<>();
    private final T value;

    private Optional() {
        value = null;
    }

    private Optional(T value) {
        this.value = Objects.requireNonNull(value);
    }

    public T get() {
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }

    public static <T> Optional<T> of(T value) {
        return new Optional<>(value);
    }

    public static <T> Optional<T> ofNullable(T value) {
        return null == value ? empty() : Optional.of(value);
    }

    public static<T> Optional<T> empty() {
        @SuppressWarnings("unchecked")
        Optional<T> t = (Optional<T>) EMPTY;
        return t;
    }

    public interface Action<T> {
        void apply(T value);
    }
}
