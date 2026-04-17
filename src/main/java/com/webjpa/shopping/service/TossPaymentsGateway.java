package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.domain.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "toss")
public class TossPaymentsGateway implements PaymentGateway {

    private final RestClient restClient;
    private final String frontendBaseUrl;

    public TossPaymentsGateway(RestClient.Builder restClientBuilder,
                               @Value("${app.payment.toss.api-base-url:https://api.tosspayments.com}") String apiBaseUrl,
                               @Value("${app.payment.toss.secret-key}") String secretKey,
                               @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodeSecretKey(secretKey))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.frontendBaseUrl = trimTrailingSlash(frontendBaseUrl);
    }

    @Override
    public CheckoutStartResult startCheckout(PaymentMethod paymentMethod, BigDecimal amount, String providerOrderId, String orderName) {
        TossCreatePaymentRequest request = TossCreatePaymentRequest.from(
                paymentMethod,
                amount,
                providerOrderId,
                orderName,
                frontendBaseUrl + "/payments/toss/success",
                frontendBaseUrl + "/payments/toss/fail"
        );

        TossPaymentResponse response = post("/v1/payments", request, TossPaymentResponse.class);
        if (response == null || response.checkout() == null || response.checkout().url() == null || response.checkout().url().isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Toss checkout URL is missing.");
        }

        return new CheckoutStartResult(response.checkout().url());
    }

    @Override
    public PaymentResult authorize(PaymentMethod paymentMethod, BigDecimal amount, String paymentReference, String providerOrderId) {
        TossPaymentResponse response = post("/v1/payments/confirm",
                new TossConfirmPaymentRequest(paymentReference, providerOrderId, amount),
                TossPaymentResponse.class);

        if (response == null || response.paymentKey() == null || response.paymentKey().isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Toss payment confirmation did not return a payment key.");
        }

        return new PaymentResult(true, response.paymentKey(), response.status());
    }

    @Override
    public boolean refund(String transactionKey, BigDecimal amount, String reason) {
        TossPaymentResponse response = post("/v1/payments/" + transactionKey + "/cancel",
                new TossCancelPaymentRequest(reason, amount),
                TossPaymentResponse.class);
        return response != null && response.paymentKey() != null && !response.paymentKey().isBlank();
    }

    @Override
    public PaymentLookupResult getPayment(String transactionKey) {
        TossPaymentResponse response = get("/v1/payments/" + transactionKey, TossPaymentResponse.class);
        if (response == null || response.paymentKey() == null || response.paymentKey().isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Toss payment lookup did not return a payment key.");
        }

        return new PaymentLookupResult(
                response.paymentKey(),
                response.orderId(),
                response.status(),
                response.totalAmount(),
                response.resolveReason()
        );
    }

    private <T> T get(String uri, Class<T> responseType) {
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Toss payment lookup failed: " + ex.getMessage());
        }
    }

    private <T> T post(String uri, Object body, Class<T> responseType) {
        try {
            return restClient.post()
                    .uri(uri)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Toss payment request failed: " + ex.getMessage());
        }
    }

    private static String encodeSecretKey(String secretKey) {
        return Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record TossCreatePaymentRequest(
            String method,
            BigDecimal amount,
            String currency,
            String orderId,
            String orderName,
            String successUrl,
            String failUrl,
            String flowMode,
            String easyPay
    ) {
        private static TossCreatePaymentRequest from(PaymentMethod paymentMethod,
                                                     BigDecimal amount,
                                                     String orderId,
                                                     String orderName,
                                                     String successUrl,
                                                     String failUrl) {
            return switch (paymentMethod) {
                case CARD -> new TossCreatePaymentRequest("CARD", amount, "KRW", orderId, orderName, successUrl, failUrl, "DEFAULT", null);
                case BANK_TRANSFER -> new TossCreatePaymentRequest("TRANSFER", amount, "KRW", orderId, orderName, successUrl, failUrl, "DEFAULT", null);
                case KAKAO_PAY -> new TossCreatePaymentRequest("CARD", amount, "KRW", orderId, orderName, successUrl, failUrl, "DIRECT", "KAKAOPAY");
                case NAVER_PAY -> new TossCreatePaymentRequest("CARD", amount, "KRW", orderId, orderName, successUrl, failUrl, "DIRECT", "NAVERPAY");
            };
        }
    }

    private record TossConfirmPaymentRequest(
            String paymentKey,
            String orderId,
            BigDecimal amount
    ) {
    }

    private record TossCancelPaymentRequest(
            String cancelReason,
            BigDecimal cancelAmount
    ) {
    }

    private record TossPaymentResponse(
            String paymentKey,
            String orderId,
            String status,
            BigDecimal totalAmount,
            TossCheckout checkout,
            TossFailure failure,
            java.util.List<TossCancel> cancels,
            String lastTransactionKey
    ) {
        private String resolveReason() {
            if (failure != null) {
                if (failure.code() != null && !failure.code().isBlank() && failure.message() != null && !failure.message().isBlank()) {
                    return failure.code() + ": " + failure.message();
                }
                if (failure.message() != null && !failure.message().isBlank()) {
                    return failure.message();
                }
                if (failure.code() != null && !failure.code().isBlank()) {
                    return failure.code();
                }
            }

            if (cancels != null) {
                for (TossCancel cancel : cancels) {
                    if (cancel.cancelReason() != null && !cancel.cancelReason().isBlank()) {
                        return cancel.cancelReason();
                    }
                }
            }

            if (lastTransactionKey != null && !lastTransactionKey.isBlank()) {
                return lastTransactionKey;
            }

            return status == null || status.isBlank() ? "Toss payment lookup result" : "Toss payment status=" + status;
        }
    }

    private record TossCheckout(String url) {
    }

    private record TossFailure(String code, String message) {
    }

    private record TossCancel(String cancelReason) {
    }
}
