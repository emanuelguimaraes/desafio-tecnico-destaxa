package com.destaxa.authorization.service;

import com.destaxa.authorization.exception.ISOFormatException;
import com.destaxa.authorization.iso.ISO8583Processor;
import com.destaxa.authorization.model.AuthorizationRequest;
import com.destaxa.authorization.model.AuthorizationResponse;
import com.destaxa.authorization.rules.AuthorizationRules;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.destaxa.authorization.iso.ISO8583Processor.*;

@Service
@Slf4j
public class AuthorizationService {

    private final ISO8583Processor iso8583Processor;
    private final RabbitTemplate rabbitTemplate;
    private final String responseQueueName;
    private final AuthorizationRules authorizationRules;

    public AuthorizationService(ISO8583Processor iso8583Processor, RabbitTemplate rabbitTemplate,
        @Value("${spring.rabbitmq.template.default-receive-queue}") String responseQueueName,
        AuthorizationRules authorizationRules) {
        this.iso8583Processor = iso8583Processor;
        this.rabbitTemplate = rabbitTemplate;
        this.responseQueueName = responseQueueName;
        this.authorizationRules = authorizationRules;
    }

    public void processAuthorizationRequest(String isoMessage) {
        try {
            AuthorizationRequest request = iso8583Processor.fromIso8583(isoMessage);

            String paymentId = UUID.randomUUID().toString();

            AuthorizationResponse response = authorizationRules.apply(request, paymentId);

            String isoResponse = iso8583Processor.toIso8583(response);
            rabbitTemplate.convertAndSend(responseQueueName, isoResponse);

        } catch (ISOFormatException | ISOException e) {
            log.error("Erro ao processar requisição de autorização ISO {}: {}", isoMessage, e.getMessage(), e);
            sendErrorResponse();
        }
    }

    private void sendErrorResponse() {
        try {
            LocalDateTime now = LocalDateTime.now();
            AuthorizationResponse errorResponse = new AuthorizationResponse();
            errorResponse.setResponseCode("999");
            errorResponse.setTransmissionDateTime(now.format(DATE_TIME_FORMATTER));
            errorResponse.setLocalTransactionTime(now.format(TIME_FORMATTER));
            errorResponse.setLocalTransactionDate(now.format(DATE_FORMATTER));

            errorResponse.setPaymentId(UUID.randomUUID().toString());
            errorResponse.setValue(BigDecimal.ZERO);
            errorResponse.setExternalId(" ");
            errorResponse.setNsu(" ");

            String isoErrorResponse = iso8583Processor.toIso8583(errorResponse);

            rabbitTemplate.convertAndSend(responseQueueName, isoErrorResponse);

        } catch (ISOException ex) {
            log.error("Erro ao converter/enviar resposta de erro para ISO8583: {}", ex.getMessage(), ex);
        }
    }
}