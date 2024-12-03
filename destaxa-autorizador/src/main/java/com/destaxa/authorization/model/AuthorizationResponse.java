package com.destaxa.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizationResponse implements Serializable {

    private String paymentId;
    private BigDecimal value;
    private String responseCode;
    private String authorizationCode;
    private String nsu;
    private LocalDateTime transmissionDateTime;
    private LocalTime localTransactionTime;
    private LocalDate localTransactionDate;
    private String externalId;
}