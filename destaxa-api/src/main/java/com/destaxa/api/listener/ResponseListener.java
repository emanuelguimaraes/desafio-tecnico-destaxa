package com.destaxa.api.listener;

import com.destaxa.api.dto.AuthorizationResponse;
import com.destaxa.api.util.ISO8583Processor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseListener {

    private final ISO8583Processor iso8583Processor;
    private final Map<String, Runnable> callbacks = new ConcurrentHashMap<>();
    private final Map<String, AuthorizationResponse> responses = new ConcurrentHashMap<>();

    @RabbitListener(queues = "${spring.rabbitmq.template.default-receive-queue}")
    public void onMessage(String isoMessage) {
        log.info("Mensagem recebida da fila autorizacao_resposta: {}", isoMessage);

        try {
            AuthorizationResponse response = iso8583Processor.fromIso8583(isoMessage);
            responses.put(response.getExternalId(), response);

            if (response.getExternalId() != null) {
                Runnable callback = callbacks.remove(response.getExternalId());

                if (callback != null) {
                    callback.run();
                }
            }

            log.info("Resposta de autorização recebida: {}", response);

        } catch (Exception e) {
            log.error("Erro ao processar mensagem de resposta", e);
        }
    }

    public void registerCallback(String externalId, Runnable callback) {
        callbacks.put(externalId, callback);
    }

    public AuthorizationResponse getResponse(String externalId) {
        return responses.remove(externalId);
    }
}