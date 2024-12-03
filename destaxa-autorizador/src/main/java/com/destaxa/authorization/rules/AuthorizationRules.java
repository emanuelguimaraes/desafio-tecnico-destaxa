package com.destaxa.authorization.rules;

import com.destaxa.authorization.model.AuthorizationRequest;
import com.destaxa.authorization.model.AuthorizationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
public class AuthorizationRules {

    private static final BigDecimal TRANSACTION_LIMIT = BigDecimal.valueOf(1000);

    public AuthorizationResponse apply(AuthorizationRequest request, String paymentId) {
        AuthorizationResponse response = new AuthorizationResponse();

        response.setTransmissionDateTime(request.getTransmissionDateTime());
        response.setLocalTransactionTime(request.getTransactionTime());
        response.setLocalTransactionDate(request.getTransactionDate());

        response.setPaymentId(paymentId);
        response.setValue(request.getValue());
        response.setExternalId(request.getExternalId());
        response.setNsu(request.getNsu());

        if (request.getValue().compareTo(BigDecimal.ZERO) > 0) {
            if (request.getValue().compareTo(TRANSACTION_LIMIT) > 0) {
                simulateTimeout(response);
            } else {
                response.setResponseCode("000");
                response.setAuthorizationCode(generateAuthorizationCode());
            }
        } else {
            response.setResponseCode("051");
        }

        return response;
    }

    private void simulateTimeout(AuthorizationResponse response) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Erro durante o timeout simulado: {}", e.getMessage(), e);
        }

        response.setResponseCode("051");
    }


    private String generateAuthorizationCode() {
        return UUID.randomUUID().toString().substring(0, 6);
    }
}