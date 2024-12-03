package com.destaxa.autorizador.iso8583;

import com.destaxa.autorizador.model.AuthorizationRequest;
import com.destaxa.autorizador.model.AuthorizationResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Getter
@Slf4j
public class ISO8583Processor {

    private final GenericPackager packager;

    public ISO8583Processor() {
        try {
            InputStream is = getClass().getResourceAsStream("/packager.xml");
            packager = new GenericPackager(is);
        } catch (ISOException e) {
            log.error("Erro ao carregar o packager.xml", e);
            throw new RuntimeException(e);
        }
    }

    public AuthorizationRequest fromIso8583(String isoMessage) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(packager);
        isoMsg.setHeader("ISO1987".getBytes());
        isoMsg.unpack(isoMessage.getBytes());

        AuthorizationRequest request = new AuthorizationRequest();
        request.setExternalId(isoMsg.getString(48));
        request.setValue(parseAmount(isoMsg.getString(4)));
        request.setCardNumber(isoMsg.getString(2));
        request.setInstallments(Integer.valueOf(isoMsg.getString(67)));
        request.setExpMonth(Integer.valueOf(isoMsg.getString(14).substring(2)));
        request.setExpYear(Integer.valueOf("20" + isoMsg.getString(14).substring(0,2)));
        request.setHolderName("SeuNome");

        return request;
    }

    public String toIso8583(AuthorizationResponse response) throws ISOException {
        if (response.getResponseCode().equals("999")) {
            return createErrorResponseIso8583(response.getResponseCode());
        }

        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(packager);
        isoMsg.setHeader("ISO1987".getBytes());
        isoMsg.setMTI("0210");

        isoMsg.set(4, formatAmount(response.getValue()));
        isoMsg.set(38, response.getAuthorizationCode());
        isoMsg.set(39, response.getResponseCode());
        isoMsg.set(48, response.getExternalId());
        isoMsg.set(127, response.getPaymentId());

        isoMsg.set(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss")));
        isoMsg.set(11, generateStan());
        isoMsg.set(12, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
        isoMsg.set(13, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd")));

        return new String(isoMsg.pack());
    }

    public String formatAmount(BigDecimal amount) {
        DecimalFormat df = new DecimalFormat("000000000000");

        String formattedValue = df.format(amount.abs().multiply(new BigDecimal("100")).longValue());

        if (amount.signum() < 0) {
            formattedValue = "-" + formattedValue.substring(1);
        }

        return formattedValue;
    }

    public BigDecimal parseAmount(String amount) {
        return new BigDecimal(amount).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private String generateStan() {
        return String.valueOf((int) (Math.random() * 1000000));
    }

    private String createErrorResponseIso8583(String responseCode) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(packager);
        isoMsg.setHeader("ISO1987".getBytes());
        isoMsg.setMTI("0210");
        isoMsg.set(39, responseCode);

        return new String(isoMsg.pack());
    }
}