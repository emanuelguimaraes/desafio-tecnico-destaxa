package com.destaxa.api.util.formatter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class AmountFormatter {

    public String formatDecimal(BigDecimal amount) {
        if (amount == null) {
            return null;
        }

        DecimalFormat df = new DecimalFormat("000000000000");
        String formattedValue = df.format(amount.abs().multiply(new BigDecimal("100")).longValue());

        if (amount.signum() < 0) {
            formattedValue = "-" + formattedValue.substring(1);
        }

        return formattedValue;
    }

    public String formatInteger(BigInteger value) {
        DecimalFormat df = new DecimalFormat("00");

        return df.format(value);
    }

    public BigDecimal parseDecimal(String amount) {
        if (amount == null || amount.isBlank()) {
            return null;
        }

        String numericAmount = amount.replaceAll("[^\\d.-]", "");

        return new BigDecimal(numericAmount).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
