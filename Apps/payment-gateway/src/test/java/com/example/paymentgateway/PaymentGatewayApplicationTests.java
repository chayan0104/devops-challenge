package com.example.paymentgateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "processor.url=http://localhost:9999"
})
class PaymentGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
