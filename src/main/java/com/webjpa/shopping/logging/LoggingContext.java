package com.webjpa.shopping.logging;

import org.slf4j.MDC;

public final class LoggingContext {

    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private LoggingContext() {
    }

    public static String currentRequestId() {
        return MDC.get(REQUEST_ID);
    }

    public static void putRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            MDC.remove(REQUEST_ID);
            return;
        }
        MDC.put(REQUEST_ID, requestId);
    }

    public static void clearRequestId() {
        MDC.remove(REQUEST_ID);
    }
}
