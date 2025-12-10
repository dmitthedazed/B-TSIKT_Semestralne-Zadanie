package com.combined.frontend.util;

public final class ErrorUtils {

    private ErrorUtils() {
    }

    public static String formatError(String prefix, Throwable throwable) {
        String reason = extractReason(throwable);
        if (reason == null || reason.isBlank()) {
            return prefix;
        }
        return prefix + ": " + reason;
    }

    public static String extractReason(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            message = cause.toString();
        }
        return message == null ? "" : message;
    }
}
