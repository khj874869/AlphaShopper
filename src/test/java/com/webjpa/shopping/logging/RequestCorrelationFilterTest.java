package com.webjpa.shopping.logging;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void doFilter_reusesValidRequestIdHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader(LoggingContext.REQUEST_ID_HEADER, " client-request-123 ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain filterChain = new CapturingFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(filterChain.requestIdDuringRequest).isEqualTo("client-request-123");
        assertThat(response.getHeader(LoggingContext.REQUEST_ID_HEADER)).isEqualTo("client-request-123");
        assertThat(MDC.get(LoggingContext.REQUEST_ID)).isNull();
    }

    @Test
    void doFilter_generatesRequestIdWhenHeaderIsUnsafe() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.addHeader(LoggingContext.REQUEST_ID_HEADER, "bad\nrequest");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain filterChain = new CapturingFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(filterChain.requestIdDuringRequest).isNotBlank();
        assertThat(filterChain.requestIdDuringRequest).isNotEqualTo("bad\nrequest");
        assertThat(response.getHeader(LoggingContext.REQUEST_ID_HEADER)).isEqualTo(filterChain.requestIdDuringRequest);
        assertThat(MDC.get(LoggingContext.REQUEST_ID)).isNull();
    }

    private static class CapturingFilterChain extends MockFilterChain {

        private String requestIdDuringRequest;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request,
                             jakarta.servlet.ServletResponse response) throws IOException, ServletException {
            this.requestIdDuringRequest = MDC.get(LoggingContext.REQUEST_ID);
            super.doFilter(request, response);
        }
    }
}
