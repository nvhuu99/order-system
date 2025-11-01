package com.example.inventory.utils;

public class ErrorUtils {

    public static <T extends Throwable> Throwable unwrapException(Throwable rootException, Class<T> targetType) {
        if (!targetType.isInstance(rootException) && targetType.isInstance(rootException.getCause())) {
            return targetType.cast(rootException.getCause());
        }
        return rootException;
    }

    public static Throwable exceptionCause(Throwable ex) {
        return ex.getCause() != null ? ex.getCause() : ex;
    }
}
