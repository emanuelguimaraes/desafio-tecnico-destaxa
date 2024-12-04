package com.destaxa.api.controller;

import com.destaxa.api.dto.AuthorizationRequest;
import com.destaxa.api.util.ISO8583Processor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ISO8583Processor processor;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.listener.authorization-queue.queue-name}")
    private String autorizacaoQueue;

    @Test
    void testAuthorize_validRequest() throws Exception {
        AuthorizationRequest request = criarRequisicaoValida();
        String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/authorization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isAccepted());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate, times(1)).convertAndSend(eq(autorizacaoQueue), messageCaptor.capture());
        String isoMessageSent = messageCaptor.getValue();

        assertNotNull(isoMessageSent);
        assertTrue("0200".equals(extractFieldFromIsoMessage(isoMessageSent, 0)));
        assertTrue(isoMessageSent.contains(request.getCardNumber()));
    }

    @Test
    void testAuthorize_invalidRequest_missingFields() throws Exception {
        AuthorizationRequest request = new AuthorizationRequest();
        String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/authorization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testAuthorize_invalidRequest_invalidCardNumber() throws Exception {
        AuthorizationRequest request = criarRequisicaoValida();
        request.setCardNumber("123");
        String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/authorization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    private AuthorizationRequest criarRequisicaoValida() {
        AuthorizationRequest request = new AuthorizationRequest();

        request.setExternalId("ext123");
        request.setValue(new BigDecimal("10.50"));
        request.setCardNumber("1234567890123456");
        request.setCvv("123");
        request.setExpMonth(BigInteger.valueOf(12));
        request.setExpYear(BigInteger.valueOf(25));
        request.setHolderName("João da Silva");

        return request;
    }

    private String extractFieldFromIsoMessage(String isoMessage, int fieldNumber) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(processor.getPackager());
        isoMsg.setHeader("ISO1987".getBytes(StandardCharsets.ISO_8859_1));
        isoMsg.unpack(isoMessage.getBytes(StandardCharsets.ISO_8859_1));

        return isoMsg.hasField(fieldNumber) ? isoMsg.getString(fieldNumber) : null;
    }
}