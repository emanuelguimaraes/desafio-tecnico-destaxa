package com.destaxa.destaxa_api.controller;

import com.destaxa.destaxa_api.dto.AuthorizationRequest;
import com.destaxa.destaxa_api.dto.AuthorizationResponse;
import com.destaxa.destaxa_api.listener.ResponseListener;
import com.destaxa.destaxa_api.service.Iso8583Converter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class AuthorizationController {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationController.class);

    private final Iso8583Converter iso8583Converter;
    private final RabbitTemplate rabbitTemplate;
    private final ResponseListener responseListener;

    @Autowired
    public AuthorizationController(Iso8583Converter iso8583Converter, RabbitTemplate rabbitTemplate, ResponseListener responseListener) {
        this.iso8583Converter = iso8583Converter;
        this.rabbitTemplate = rabbitTemplate;
        this.responseListener = responseListener;
    }

    @PostMapping("/authorization")
    public ResponseEntity<Void> authorize(@Valid @RequestBody AuthorizationRequest request) {
        try {
            log.info("Requisição recebida: {}", request);

            String isoMessage = iso8583Converter.toIso8583(request);

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

            rabbitTemplate.convertAndSend("autorizacao", isoMessage);

            return ResponseEntity.accepted().build();

        } catch (AmqpException ex) {
            log.error("Erro ao enviar mensagem para o RabbitMQ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception ex) {
            log.error("Erro ao processar requisição: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}