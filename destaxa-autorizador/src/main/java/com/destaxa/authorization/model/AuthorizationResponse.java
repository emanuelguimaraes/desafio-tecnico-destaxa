package com.destaxa.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizationResponse implements Serializable {

    private String paymentId;
    private BigDecimal value;
    private String responseCode;
    private String authorizationCode;
    private String nsu;
    private String transmissionDateTime;
    private String localTransactionTime;
    private String localTransactionDate;
    private String externalId;
}