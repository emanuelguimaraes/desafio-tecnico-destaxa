package com.destaxa.authorization.listener;

import com.destaxa.authorization.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthorizationRequestListener {

    private final AuthorizationService authorizationService;

    @Autowired
    public AuthorizationRequestListener(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @RabbitListener(queues = "${spring.rabbitmq.listener.authorization-queue.queue-name}")
    public void onMessage(Message isoMessage) {
        String messageContent = new String(isoMessage.getBody());
        log.info("Mensagem ISO8583 recebida: {}", messageContent);

        authorizationService.processAuthorizationRequest(messageContent);
    }
}
