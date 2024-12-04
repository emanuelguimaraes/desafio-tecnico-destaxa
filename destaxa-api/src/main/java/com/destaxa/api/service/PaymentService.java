package com.destaxa.api.service;

import com.destaxa.api.dto.AuthorizationRequest;
import com.destaxa.api.dto.AuthorizationResponse;
import com.destaxa.api.exception.AuthorizationException;
import com.destaxa.api.listener.ResponseListener;
import com.destaxa.api.util.ISO8583Processor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ISO8583Processor iso8583Processor;
    private final RabbitTemplate rabbitTemplate;
    private final ResponseListener responseListener;

    @Value("${spring.rabbitmq.listener.authorization-queue.queue-name}")
    private String autorizacaoQueue;

    public void authorize(AuthorizationRequest request) {
        try {
            String isoMessage = iso8583Processor.toIso8583(request);
            log.debug("Mensagem ISO8583: {}", isoMessage);

            String externalId = request.getExternalId();

            CompletableFuture<AuthorizationResponse> future = new CompletableFuture<>();
            responseListener.registerCallback(externalId, () -> {
                try {
                    AuthorizationResponse response = responseListener.getResponse(externalId);

                    if (response != null) {
                        future.complete(response);
                        log.info("Notificação enviada para o cliente: {}", response);

                    } else {
                        future.completeExceptionally(new RuntimeException("Resposta não encontrada para externalId: " + externalId));
                    }

                } catch (Exception e) {
                    future.completeExceptionally(new RuntimeException("Erro ao processar resposta", e));

                }
            });

            rabbitTemplate.convertAndSend(autorizacaoQueue, isoMessage);

        } catch (Exception e) {
            log.error("Erro ao processar autorização", e);
            throw new AuthorizationException("Erro ao processar autorização", e);
        }
    }
}
