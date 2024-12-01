package com.destaxa.destaxa_api.controller;

import com.destaxa.destaxa_api.dto.AuthorizationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testAuthorize_validRequest() throws Exception {
        AuthorizationRequest request = criarRequisicaoValida();
        String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/authorization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isAccepted());
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
        request.setExpMonth(12);
        request.setExpYear(25);
        request.setHolderName("João da Silva");
        request.setInstallments(1);
        return request;
    }
}