package com.example.paymentprocessor.service;

import com.example.paymentprocessor.dto.PaymentRequest;
import com.example.paymentprocessor.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final int simulatedFailureRatePercent;

    public PaymentService(@Value("${processor.simulated-failure-rate-percent:0}") int simulatedFailureRatePercent) {
        this.simulatedFailureRatePercent = simulatedFailureRatePercent;
    }

    public PaymentResponse process(PaymentRequest request) {
        if (simulatedFailureRatePercent > 0
                && ThreadLocalRandom.current().nextInt(100) < simulatedFailureRatePercent) {
            log.warn("Simulated processor failure injected for transaction from {} to {}",
                    request.getSource(), request.getDestination());
            throw new IllegalStateException("simulated downstream failure");
        }

        String transactionId = UUID.randomUUID().toString();
        log.info("Processed transaction {} for amount {} {}", transactionId, request.getAmount(), request.getCurrency());

        return new PaymentResponse(
                transactionId,
                "APPROVED",
                "Payment processed successfully",
                "payment-processor",
                request.getAmount(),
                request.getCurrency(),
                Instant.now());
    }
}
