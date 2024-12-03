package com.destaxa.autorizador.service;

import com.destaxa.autorizador.iso8583.ISO8583Processor;
import com.destaxa.autorizador.model.AuthorizationRequest;
import com.destaxa.autorizador.model.AuthorizationResponse;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AuthorizationService {

    private final ISO8583Processor iso8583Processor;
    private final RabbitTemplate rabbitTemplate;
    private final String responseQueueName;

    public AuthorizationService(ISO8583Processor iso8583Processor, RabbitTemplate rabbitTemplate,
        @Value("${spring.rabbitmq.template.default-receive-queue}") String responseQueueName) {
        this.iso8583Processor = iso8583Processor;
        this.rabbitTemplate = rabbitTemplate;
        this.responseQueueName = responseQueueName;
    }

    public void processAuthorizationRequest(String isoMessage) {
        try {
            AuthorizationRequest request = iso8583Processor.fromIso8583(isoMessage);

            authorizeRequest(request);

        } catch (ISOException e) {
            log.error("Erro ao processar requisição de autorização ISO {}: {}", isoMessage, e.getMessage(), e);

            try {
                AuthorizationResponse response = new AuthorizationResponse();
                response.setResponseCode("999");
                String isoResponse = iso8583Processor.toIso8583(response);
                rabbitTemplate.convertAndSend(responseQueueName, isoResponse);
            } catch (ISOException ex) {
                log.error("Erro ao converter resposta de erro para ISO8583 para a mensagem {}: {}", isoMessage, ex.getMessage(), ex);
            }
        }
    }

    private void authorizeRequest(AuthorizationRequest request) throws ISOException {
        BigDecimal transactionValue = request.getValue();
        String responseCode;
        String authorizationCode = null;

        if (transactionValue.compareTo(BigDecimal.ZERO) > 0) {
            if (transactionValue.compareTo(BigDecimal.valueOf(1000)) > 0) {
                simulateTimeout(request);
                return;
            } else {
                responseCode = "000";
                authorizationCode = generateAuthorizationCode();
            }
        } else {
            responseCode = "051";
        }

        AuthorizationResponse response = new AuthorizationResponse(
            UUID.randomUUID().toString(),
            transactionValue,
            responseCode,
            authorizationCode,
            LocalDateTime.now(),
            request.getExternalId()
        );

        String isoResponse = iso8583Processor.toIso8583(response);
        rabbitTemplate.convertAndSend(responseQueueName, isoResponse);
    }

    private void simulateTimeout(AuthorizationRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000);
                AuthorizationResponse timeoutResponse = new AuthorizationResponse(
                    UUID.randomUUID().toString(),
                    request.getValue(),
                    "051",
                    null,
                    LocalDateTime.now(),
                    request.getExternalId()
                );

                String isoTimeoutResponse = iso8583Processor.toIso8583(timeoutResponse);
                rabbitTemplate.convertAndSend(responseQueueName, isoTimeoutResponse);

            } catch (InterruptedException | ISOException e) {
                log.error("Erro durante o timeout simulado: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        });
    }

    private String generateAuthorizationCode() {
        return UUID.randomUUID().toString().substring(0, 6);
    }
}