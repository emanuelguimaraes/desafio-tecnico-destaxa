package com.destaxa.api.util;

import com.destaxa.api.dto.AuthorizationRequest;
import com.destaxa.api.dto.AuthorizationResponse;
import com.destaxa.api.exception.ISOFormatException;
import com.destaxa.api.util.formatter.AmountFormatter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@Getter
@Slf4j
public class ISO8583Processor {

    public static final String MTI_AUTHORIZATION_REQUEST = "0200";
    public static final String MTI_AUTHORIZATION_RESPONSE = "0210";
    public static final String ISO_HEADER = "ISO1987";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMddHHmmss");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter ISO_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yy-MM-dd");
    public static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMdd");
    public static final int FIELD_CARD_NUMBER = 2;
    public static final int FIELD_PROCESSING_CODE = 3;
    public static final int FIELD_TRANSACTION_AMOUNT = 4;
    public static final int FIELD_TRANSMISSION_DATE_TIME = 7;
    public static final int FIELD_NSU = 11;
    public static final int FIELD_LOCAL_TRANSACTION_TIME = 12;
    public static final int FIELD_LOCAL_TRANSACTION_DATE = 13;
    public static final int FIELD_EXPIRATION_DATE = 14;
    public static final int FIELD_ENTRY_MODE = 22;
    public static final int FIELD_AUTHORIZATION_ID_RESPONSE = 38;
    public static final int FIELD_RESPONSE_CODE = 39;
    public static final int FIELD_MERCHANT_ID = 42;
    public static final int FIELD_EXTERNAL_ID = 47;
    public static final int FIELD_CVV = 48;
    public static final int FIELD_INSTALLMENTS = 67;
    public static final int FIELD_PAYMENT_ID = 127;

    public static final String PROCESSING_CODE_CASH_CREDIT = "003000";
    public static final String PROCESSING_CODE_INSTALLMENT_CREDIT = "003001";

    private final GenericPackager packager;
    private final AmountFormatter amountFormatter;

    public ISO8583Processor(GenericPackager packager, AmountFormatter amountFormatter) {
        this.packager = packager;
        this.amountFormatter = amountFormatter;
    }

    public AuthorizationResponse fromIso8583(String isoMessage) throws ISOFormatException {
        try {
            ISOMsg isoMsg = unpackIsoMessage(isoMessage, MTI_AUTHORIZATION_RESPONSE);
            return extractAuthorizationResponseFromIsoMsg(isoMsg);
        } catch (ISOException e) {
            throw new ISOFormatException("Erro ao processar mensagem ISO8583: " + e.getMessage(), e);
        }
    }

    public String toIso8583(AuthorizationRequest request) throws ISOException {
        try {
            LocalDateTime now = LocalDateTime.now();
            ISOMsg isoMsg = buildISOMsg(MTI_AUTHORIZATION_REQUEST);

            setField(isoMsg, FIELD_CARD_NUMBER, request.getCardNumber());
            setField(isoMsg, FIELD_PROCESSING_CODE, PROCESSING_CODE_CASH_CREDIT);
            setField(isoMsg, FIELD_TRANSACTION_AMOUNT, amountFormatter.formatDecimal(request.getValue()));
            setField(isoMsg, FIELD_TRANSMISSION_DATE_TIME, now.format(DATE_TIME_FORMATTER));
            setField(isoMsg, FIELD_NSU, generateStan());
            setField(isoMsg, FIELD_LOCAL_TRANSACTION_TIME, now.format(ISO_TIME_FORMATTER));
            setField(isoMsg, FIELD_LOCAL_TRANSACTION_DATE, now.format(ISO_DATE_FORMATTER));
            setField(isoMsg, FIELD_EXPIRATION_DATE, String.format("%s%s", amountFormatter.formatInteger(request.getExpYear()), amountFormatter.formatInteger(request.getExpMonth())));
            setField(isoMsg, FIELD_ENTRY_MODE, "000");
            setField(isoMsg, FIELD_EXTERNAL_ID, request.getExternalId());
            setField(isoMsg, FIELD_CVV, request.getCvv());
            setField(isoMsg, FIELD_INSTALLMENTS, "01");

            return new String(isoMsg.pack(), StandardCharsets.ISO_8859_1);

        } catch (ISOFormatException e) {
            log.error("Erro ao formatar a resposta ISO8583", e);
            throw new ISOException("Erro ao construir mensagem ISO8583 de resposta: " + e.getMessage(), e);
        }
    }

    private ISOMsg unpackIsoMessage(String isoMessage, String expectedMTI) throws ISOException, ISOFormatException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(packager);
        isoMsg.setHeader(ISO_HEADER.getBytes(StandardCharsets.ISO_8859_1));

        try {
            isoMsg.unpack(isoMessage.getBytes(StandardCharsets.ISO_8859_1));
        } catch (ISOException e) {
            throw new ISOFormatException("Formato da mensagem ISO8583 inválido", e);
        }

        validateMTI(isoMsg, expectedMTI);
        return isoMsg;
    }

    private String getField(ISOMsg isoMsg, int fieldNumber) throws ISOFormatException {
        if (isoMsg.hasField(fieldNumber)) {
            return isoMsg.getString(fieldNumber);
        } else {
            throw new ISOFormatException("Campo obrigatório " + fieldNumber + " não encontrado na mensagem ISO8583.");
        }
    }

    private Optional<String> getOptionalField(ISOMsg isoMsg, int fieldNumber) {
        return isoMsg.hasField(fieldNumber) ? Optional.of(isoMsg.getString(fieldNumber)) : Optional.empty();
    }

    private void setField(ISOMsg isoMsg, int fieldNumber, String value) throws ISOFormatException {
        if (value != null) {
            try {
                isoMsg.set(fieldNumber, value);
            } catch (Exception e) {
                throw new ISOFormatException(String.format("Erro ao setar o campo %s na mensagem ISO: %s", fieldNumber, e.getMessage()), e);
            }
        }
    }

    private AuthorizationResponse extractAuthorizationResponseFromIsoMsg(ISOMsg isoMsg) throws ISOFormatException {
        AuthorizationResponse response = new AuthorizationResponse();
        LocalDateTime now = LocalDateTime.now();

        response.setPaymentId(getField(isoMsg, FIELD_PAYMENT_ID));
        response.setValue(amountFormatter.parseDecimal(getOptionalField(isoMsg, FIELD_TRANSACTION_AMOUNT).orElse(null)));
        response.setResponseCode(getField(isoMsg, FIELD_RESPONSE_CODE));
        response.setAuthorizationCode(getOptionalField(isoMsg, FIELD_AUTHORIZATION_ID_RESPONSE).orElse(null));

        response.setTransactionHour(now.toLocalTime());
        response.setTransactionDate(now.toLocalDate());
        response.setExternalId(getField(isoMsg, FIELD_EXTERNAL_ID));

        return response;
    }

    private void validateMTI(ISOMsg isoMsg, String expectedMTI) throws ISOFormatException, ISOException {
        String mti = isoMsg.getMTI();
        if (!mti.equals(expectedMTI)) {
            String errorMessage = String.format("MTI inválido. Esperado '%s', recebido '%s'", expectedMTI, mti);
            log.error(errorMessage);
            throw new ISOFormatException(errorMessage);
        }
    }

    private ISOMsg buildISOMsg(String mti) throws ISOFormatException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(packager);

        try {
            isoMsg.setHeader(ISO_HEADER.getBytes(StandardCharsets.ISO_8859_1));
            isoMsg.setMTI(mti);
        } catch (ISOException e) {
            throw new ISOFormatException("Erro ao criar mensagem ISO8583: " + e.getMessage(), e);
        }
        return isoMsg;
    }

    private String generateStan() {
        return String.valueOf((int) (Math.random() * 1000000));
    }
}