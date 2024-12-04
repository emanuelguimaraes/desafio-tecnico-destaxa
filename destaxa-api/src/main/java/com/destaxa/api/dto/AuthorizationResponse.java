package com.destaxa.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AuthorizationResponse {

    private String paymentId;
    private BigDecimal value;
    private String responseCode;
    private String authorizationCode;
    private LocalDate transactionDate;
    private LocalTime transactionHour;

    @JsonIgnore
    private String externalId;
}