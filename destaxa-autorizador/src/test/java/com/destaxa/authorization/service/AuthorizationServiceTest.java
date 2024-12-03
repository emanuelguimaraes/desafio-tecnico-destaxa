package com.destaxa.authorization.service;

import com.destaxa.authorization.exception.ISOFormatException;
import com.destaxa.authorization.iso.ISO8583Processor;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.destaxa.authorization.iso.ISO8583Processor.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
        String isoMessage = createValidIsoMessage(new BigDecimal("500.00"));

        authorizationService.processAuthorizationRequest(isoMessage);

        verify(rabbitTemplate).convertAndSend(eq(responseQueueName), messageCaptor.capture());
        String capturedMessage = messageCaptor.getValue();

        assertAuthorizationResponse(capturedMessage, "000", true);
    }

    @Test
    void testNegativeValue_denied() throws ISOException {
        String isoMessage = createValidIsoMessage(new BigDecimal("-100.00"));

        authorizationService.processAuthorizationRequest(isoMessage);

        verify(rabbitTemplate).convertAndSend(eq(responseQueueName), messageCaptor.capture());
        String capturedMessage = messageCaptor.getValue();

        assertAuthorizationResponse(capturedMessage, "051", false);
    }

    @Test
    void testValueOver1000_timeout() throws ISOException {
        String isoMessage = createValidIsoMessage(new BigDecimal("1500.00"));

        authorizationService.processAuthorizationRequest(isoMessage);

        verify(rabbitTemplate, times(1)).convertAndSend(eq(responseQueueName), messageCaptor.capture());
        String capturedMessage = messageCaptor.getValue();

        assertAuthorizationResponse(capturedMessage, "051", false);
    }

    @Test
    void testProcessAuthorizationRequest_invalidIsoMessage() throws ISOException {
        String invalidIsoMessage = "Invalid ISO message";

        authorizationService.processAuthorizationRequest(invalidIsoMessage);

        verify(rabbitTemplate, times(1)).convertAndSend(eq(responseQueueName), messageCaptor.capture());
        assertAuthorizationResponse(messageCaptor.getValue(), "999", false);
    }

    private String createValidIsoMessage(BigDecimal value) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(iso8583Processor.getPackager());
        isoMsg.setHeader(ISO_HEADER.getBytes(StandardCharsets.ISO_8859_1));
        isoMsg.setMTI(MTI_AUTHORIZATION_REQUEST);

        setField(isoMsg, FIELD_CARD_NUMBER, "1234567890123456");
        setField(isoMsg, FIELD_PROCESSING_CODE, "003000");
        setField(isoMsg, FIELD_TRANSACTION_AMOUNT, iso8583Processor.getAmountFormatter().format(value));
        setField(isoMsg, FIELD_TRANSMISSION_DATE_TIME, LocalDateTime.now().format(DATE_TIME_FORMATTER));
        setField(isoMsg, FIELD_NSU, "123456");
        setField(isoMsg, FIELD_LOCAL_TRANSACTION_TIME, LocalTime.now().format(TIME_FORMATTER));
        setField(isoMsg, FIELD_LOCAL_TRANSACTION_DATE, LocalDate.now().format(DATE_FORMATTER));
        setField(isoMsg, FIELD_EXPIRATION_DATE, "1225");
        setField(isoMsg, FIELD_ENTRY_MODE, "123");
        setField(isoMsg, FIELD_EXTERNAL_ID, "externalId123");
        setField(isoMsg, FIELD_INSTALLMENTS, "1");

        return new String(isoMsg.pack(), StandardCharsets.ISO_8859_1);
    }

    private void setField(ISOMsg isoMsg, int fieldNumber, String value) {
        try {
            if (value != null) {
                isoMsg.set(fieldNumber, value);
            }
        } catch (Exception e) {
            fail("Erro ao setar o campo " + fieldNumber + ": " + e.getMessage());
        }
    }

    private String extractFieldFromIsoMessage(String isoMessage, int fieldNumber) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(iso8583Processor.getPackager());
        isoMsg.setHeader("ISO1987".getBytes(StandardCharsets.ISO_8859_1));
        isoMsg.unpack(isoMessage.getBytes(StandardCharsets.ISO_8859_1));

        return isoMsg.hasField(fieldNumber) ? isoMsg.getString(fieldNumber) : null;
    }

    private void assertAuthorizationResponse(String capturedMessage, String expectedResponseCode, boolean expectAuthorizationCode) throws ISOException {
        assertNotNull(capturedMessage);

        String responseCode = extractFieldFromIsoMessage(capturedMessage, FIELD_RESPONSE_CODE);
        assertEquals(expectedResponseCode, responseCode, "Código de resposta incorreto");

        String authorizationCode = extractFieldFromIsoMessage(capturedMessage, FIELD_AUTHORIZATION_ID_RESPONSE);
        if (expectAuthorizationCode) {
            assertNotNull(authorizationCode, "Código de autorização deve estar presente");
        } else {
            assertNull(authorizationCode, "Código de autorização não deve estar presente");
        }

        assertNotNull(extractFieldFromIsoMessage(capturedMessage, FIELD_TRANSACTION_AMOUNT));
        assertNotNull(extractFieldFromIsoMessage(capturedMessage, FIELD_TRANSMISSION_DATE_TIME));
        assertNotNull(extractFieldFromIsoMessage(capturedMessage, FIELD_NSU));
        assertNotNull(extractFieldFromIsoMessage(capturedMessage, FIELD_LOCAL_TRANSACTION_TIME));
        assertNotNull(extractFieldFromIsoMessage(capturedMessage, FIELD_LOCAL_TRANSACTION_DATE));
        assertNotNull(extractFieldFromIsoMessage(capturedMessage, FIELD_MERCHANT_ID));
        assertNotNull(extractFieldFromIsoMessage(capturedMessage, FIELD_NSU_HOST));
    }
}