package com.example.paymentgateway.client;

import com.example.paymentgateway.dto.PaymentRequest;
import com.example.paymentgateway.dto.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

import static com.example.paymentgateway.filter.RequestIdFilter.MDC_KEY;
import static com.example.paymentgateway.filter.RequestIdFilter.REQUEST_ID_HEADER;

@Component
public class PaymentProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorClient.class);

    private final RestTemplate restTemplate;
    private final String processorUrl;
    private final String apiKey;

    public PaymentProcessorClient(RestTemplate restTemplate,
                                   @Value("${processor.url}") String processorUrl,
                                   @Value("${processor.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.processorUrl = processorUrl;
        this.apiKey = apiKey;
    }

    @CircuitBreaker(name = "processor", fallbackMethod = "fallback")
    @Retry(name = "processor")
    @TimeLimiter(name = "processor")
    public CompletableFuture<PaymentResponse> process(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);
            String requestId = MDC.get(MDC_KEY);
            if (requestId != null) {
                headers.set(REQUEST_ID_HEADER, requestId);
            }
            HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);
            log.info("Forwarding payment request to processor at {}", processorUrl);
            return restTemplate.postForObject(processorUrl + "/process", entity, PaymentResponse.class);
        });
    }

    private CompletableFuture<PaymentResponse> fallback(PaymentRequest request, Throwable throwable) {
        log.error("payment-processor unavailable, returning fallback response: {}", throwable.toString());
        PaymentResponse response = new PaymentResponse();
        response.setStatus("UNAVAILABLE");
        response.setMessage("payment-processor is currently unreachable, please retry later");
        response.setProcessedBy("payment-gateway-fallback");
        return CompletableFuture.completedFuture(response);
    }
}
