package com.destaxa.destaxa_api.controller;

import com.destaxa.destaxa_api.dto.AuthorizationRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthorizationController {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationController.class);

    @PostMapping("/authorization")
    public ResponseEntity<Void> authorize(@Valid @RequestBody AuthorizationRequest request) {
        try {
            log.info("Requisição recebida: {}", request);
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        } catch (IllegalArgumentException ex) {
            log.error("Erro de validação: {}", ex.getMessage(), ex);
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("Erro ao processar requisição: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}