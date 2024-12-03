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
public class AuthorizationRequest implements Serializable {

    private String externalId;
    private BigDecimal value;
    private String cardNumber;
    private String processingCode;
    private LocalDateTime transmissionDateTime;
    private String nsu;
    private LocalTime transactionTime;
    private LocalDate transactionDate;
    private String entryMode;
    private Integer installments;
    private Integer expMonth;
    private Integer expYear;
}