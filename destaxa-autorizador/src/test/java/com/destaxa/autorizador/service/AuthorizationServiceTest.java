package com.destaxa.autorizador.service;

import com.destaxa.autorizador.iso8583.ISO8583Processor;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@TestPropertySource(properties = {"spring.rabbitmq.template.default-receive-queue=autorizacao_resposta"})
public class AuthorizationServiceTest {

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ISO8583Processor iso8583Processor;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.template.default-receive-queue}")
    private String responseQueueName;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Test
    void testPositiveValue_approved() throws ISOException {
        String isoMessage = createValidIsoMessage(new BigDecimal("500"));

        authorizationService.processAuthorizationRequest(isoMessage);

        verify(rabbitTemplate).convertAndSend(eq(responseQueueName), messageCaptor.capture());
        String capturedMessage = messageCaptor.getValue();

        assertTrue(capturedMessage.contains("000"));
        assertNotNull(extractFieldFromIsoMessage(capturedMessage, 38));
    }

    @Test
    void testNegativeValue_denied() throws ISOException {
        String isoMessage = createValidIsoMessage(new BigDecimal("-100"));

        authorizationService.processAuthorizationRequest(isoMessage);

        verify(rabbitTemplate).convertAndSend(eq(responseQueueName), messageCaptor.capture());
        String capturedMessage = messageCaptor.getValue();

        assertTrue(capturedMessage.contains("051"));
        assertNull(extractFieldFromIsoMessage(capturedMessage, 38));
    }

    @Test
    void testValueOver1000_timeout() throws ISOException, InterruptedException {
        String isoMessage = createValidIsoMessage(new BigDecimal("1500"));

        authorizationService.processAuthorizationRequest(isoMessage);

        Thread.sleep(6000);

        verify(rabbitTemplate, times(1)).convertAndSend(eq(responseQueueName), messageCaptor.capture());
        String capturedMessage = messageCaptor.getValue();

        assertTrue(capturedMessage.contains("051"));
        assertNull(extractFieldFromIsoMessage(capturedMessage, 38));
    }

    @Test
    void testProcessAuthorizationRequest_invalidIsoMessage() {
        authorizationService.processAuthorizationRequest("Invalid ISO message");

        verify(rabbitTemplate, times(1)).convertAndSend(eq(responseQueueName), messageCaptor.capture());
        String capturedMessage = messageCaptor.getValue();

        assertTrue(capturedMessage.contains("999"));
    }

    private String createValidIsoMessage(BigDecimal value) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(iso8583Processor.getPackager());
        isoMsg.setHeader("ISO1987".getBytes());
        isoMsg.setMTI("0200");

        isoMsg.set(2, "1234567890123456");
        isoMsg.set(4, iso8583Processor.formatAmount(value));
        isoMsg.set(14, "1225");
        isoMsg.set(48, "externalId123");
        isoMsg.set(67, "1");

        return new String(isoMsg.pack());
    }

    private String extractFieldFromIsoMessage(String isoMessage, int fieldNumber) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(iso8583Processor.getPackager());
        isoMsg.setHeader("ISO1987".getBytes());
        isoMsg.unpack(isoMessage.getBytes());

        if (isoMsg.hasField(fieldNumber)) {
            return isoMsg.getString(fieldNumber);
        } else {
            return null;
        }
    }
}