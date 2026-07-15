package com.example.paymentgateway.controller;

import com.example.paymentgateway.client.PaymentProcessorClient;
import com.example.paymentgateway.dto.PaymentRequest;
import com.example.paymentgateway.dto.PaymentResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.TimeUnit;

@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentProcessorClient processorClient;

    public PaymentController(PaymentProcessorClient processorClient) {
        this.processorClient = processorClient;
    }

    @PostMapping(value = "/pay")
    public DeferredResult<ResponseEntity<PaymentResponse>> pay(@Valid @RequestBody PaymentRequest request) {
        DeferredResult<ResponseEntity<PaymentResponse>> deferredResult = new DeferredResult<>(5_000L);

        processorClient.process(request)
                .orTimeout(5, TimeUnit.SECONDS)
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        log.error("Unexpected error while processing payment", throwable);
                        deferredResult.setErrorResult(
                                ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody(throwable.getMessage())));
                        return;
                    }
                    HttpStatus status = "UNAVAILABLE".equals(response.getStatus())
                            ? HttpStatus.SERVICE_UNAVAILABLE
                            : HttpStatus.OK;
                    deferredResult.setResult(ResponseEntity.status(status).body(response));
                });

        return deferredResult;
    }

    private PaymentResponse errorBody(String message) {
        PaymentResponse response = new PaymentResponse();
        response.setStatus("ERROR");
        response.setMessage(message);
        response.setProcessedBy("payment-gateway");
        return response;
    }
}
