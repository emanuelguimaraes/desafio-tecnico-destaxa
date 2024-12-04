package com.destaxa.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
public class AuthorizationRequest {

    @NotBlank(message = "External ID cannot be blank")
    private String externalId;

    @NotNull(message = "Value cannot be null")
    @Digits(integer = 10, fraction = 2, message = "Value must have at most 10 integer and 2 fraction digits")
    private BigDecimal value;

    @NotBlank(message = "Card number cannot be blank")
    @Pattern(regexp = "\\d{16,19}", message = "Card number must contain only digits and be between 16 and 19 digits long")
    private String cardNumber;

    @NotBlank(message = "CVV cannot be blank")
    @Pattern(regexp = "\\d{3,4}", message = "CVV must contain only digits and be 3 or 4 digits long")
    private String cvv;

    @NotNull(message = "Expiration month cannot be null")
    @Min(value = 1, message = "Invalid expiration month")
    @Max(value = 12, message = "Invalid expiration month")
    private BigInteger expMonth;

    @NotNull(message = "Expiration year cannot be null")
    @Min(value = 24, message = "Invalid expiration year")
    @Max(value = 99, message = "Invalid expiration month")
    private BigInteger expYear;

    @NotBlank(message = "Holder name cannot be blank")
    @Size(max = 255, message = "Holder name must be at most 255 characters")
    private String holderName;
}