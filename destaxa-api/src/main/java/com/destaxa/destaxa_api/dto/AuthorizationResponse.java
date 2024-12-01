package com.destaxa.destaxa_api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AuthorizationResponse {

    private String paymentId;

    private BigDecimal value;

    private String responseCode;

    private String authorizationCode;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime transactionDateTime;

    private String externalId;
}