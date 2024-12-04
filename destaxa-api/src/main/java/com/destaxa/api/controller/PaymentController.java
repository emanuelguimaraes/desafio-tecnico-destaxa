package com.destaxa.api.controller;

import com.destaxa.api.dto.AuthorizationRequest;
import com.destaxa.api.dto.AuthorizationResponse;
import com.destaxa.api.listener.ResponseListener;
import com.destaxa.api.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final ResponseListener responseListener;

    @PostMapping("/authorization")
    public ResponseEntity<Map<String, String>> authorize(@Valid @RequestBody AuthorizationRequest request) {
        try {
            paymentService.authorize(request);

            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("externalId", request.getExternalId());
            responseBody.put("message", "Solicitação de autorização enviada com sucesso.");

            return new ResponseEntity<>(responseBody, HttpStatus.ACCEPTED);

        } catch (Exception e) {
            log.error("Erro ao processar requisição de autorização", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/authorization/{externalId}")
    public ResponseEntity<AuthorizationResponse> getAuthorizationStatus(@PathVariable String externalId) {
        try {
            AuthorizationResponse response = responseListener.getResponse(externalId);

            if (response == null) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao buscar status da autorização", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}