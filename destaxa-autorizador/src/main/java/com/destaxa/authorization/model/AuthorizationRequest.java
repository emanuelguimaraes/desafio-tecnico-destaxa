package com.destaxa.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizationRequest implements Serializable {

    private String externalId;
    private BigDecimal value;
    private String cardNumber;
    private String processingCode;
    private String transmissionDateTime;
    private String nsu;
    private String transactionTime;
    private String transactionDate;
    private String entryMode;
    private Integer installments;
    private Integer expMonth;
    private Integer expYear;
}