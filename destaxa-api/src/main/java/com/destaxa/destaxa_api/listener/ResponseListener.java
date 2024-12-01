package com.destaxa.destaxa_api.listener;

import com.destaxa.destaxa_api.dto.AuthorizationResponse;
import com.destaxa.destaxa_api.service.Iso8583Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ResponseListener {

    private final Iso8583Converter iso8583Converter;
    private final Map<String, Runnable> callbacks = new HashMap<>();
    private final Map<String, AuthorizationResponse> responses = new HashMap<>();

    @Autowired
    public ResponseListener(Iso8583Converter iso8583Converter) {
        this.iso8583Converter = iso8583Converter;
    }

    @RabbitListener(queues = "autorizacao_resposta")
    public void onMessage(String isoMessage) {
        log.info("Mensagem recebida da fila autorizacao_resposta: {}", isoMessage);

        try {
            AuthorizationResponse response = iso8583Converter.fromIso8583(isoMessage);
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