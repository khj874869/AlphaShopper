package com.webjpa.shopping.logging;

public final class LogValues {

    private static final int MAX_TEXT_LENGTH = 160;

    private LogValues() {
    }

    public static String safe(Object value) {
        if (value == null) {
            return "";
        }
        return safe(String.valueOf(value));
    }

    public static String safe(String value) {
        if (value == null) {
            return "";
        }

        String sanitized = value.trim()
                .replaceAll("[\\r\\n\\t]", "_")
                .replaceAll("\\p{Cntrl}", "_");
        if (sanitized.length() <= MAX_TEXT_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_TEXT_LENGTH) + "...";
    }

    public static String maskToken(String value) {
        String sanitized = safe(value);
        if (sanitized.isBlank()) {
            return "";
        }

        if (sanitized.length() <= 10) {
            return "***";
        }

        return sanitized.substring(0, 6) + "..." + sanitized.substring(sanitized.length() - 4);
    }

    public static String maskEmail(String value) {
        String sanitized = safe(value);
        if (sanitized.isBlank()) {
            return "";
        }

        int atIndex = sanitized.indexOf('@');
        if (atIndex <= 0 || atIndex == sanitized.length() - 1) {
            return "***";
        }

        String localPart = sanitized.substring(0, atIndex);
        String domain = sanitized.substring(atIndex + 1);
        String localPrefix = localPart.substring(0, 1);
        return localPrefix + "***@" + domain;
    }
}
