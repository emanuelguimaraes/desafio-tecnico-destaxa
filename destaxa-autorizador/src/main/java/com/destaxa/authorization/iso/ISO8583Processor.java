package com.destaxa.authorization.iso;

import com.destaxa.authorization.exception.ISOFormatException;
import com.destaxa.authorization.iso.formatter.AmountFormatter;
import com.destaxa.authorization.model.AuthorizationRequest;
import com.destaxa.authorization.model.AuthorizationResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Component
@Getter
@Slf4j
public class ISO8583Processor {

    public static final String MTI_AUTHORIZATION_REQUEST = "0200";
    public static final String MTI_AUTHORIZATION_RESPONSE = "0210";
    public static final String ISO_HEADER = "ISO1987";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmm");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
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
    public static final int FIELD_EXTERNAL_ID = 48;
    public static final int FIELD_INSTALLMENTS = 67;
    public static final int FIELD_NSU_HOST = 127;

    private final GenericPackager packager;
    private final AmountFormatter amountFormatter;

    public ISO8583Processor(GenericPackager packager, AmountFormatter amountFormatter) {
        this.packager = packager;
        this.amountFormatter = amountFormatter;
    }

    public AuthorizationRequest fromIso8583(String isoMessage) throws ISOFormatException {
        try {
            ISOMsg isoMsg = unpackIsoMessage(isoMessage, MTI_AUTHORIZATION_REQUEST);
            return extractAuthorizationRequestFromIsoMsg(isoMsg);
        } catch (ISOException e) {
            throw new ISOFormatException("Erro ao processar mensagem ISO8583: " + e.getMessage(), e);
        }
    }

    public String toIso8583(AuthorizationResponse response) throws ISOException {
        try {
            ISOMsg isoMsg = buildISOMsg(response, MTI_AUTHORIZATION_RESPONSE);

            setField(isoMsg, FIELD_TRANSACTION_AMOUNT, amountFormatter.format(response.getValue()));
            setField(isoMsg, FIELD_TRANSMISSION_DATE_TIME, response.getTransmissionDateTime().format(DATE_TIME_FORMATTER));
            setField(isoMsg, FIELD_NSU, response.getNsu());
            setField(isoMsg, FIELD_LOCAL_TRANSACTION_TIME, response.getLocalTransactionTime().format(TIME_FORMATTER));
            setField(isoMsg, FIELD_LOCAL_TRANSACTION_DATE, response.getLocalTransactionDate().format(DATE_FORMATTER));
            setField(isoMsg, FIELD_AUTHORIZATION_ID_RESPONSE, response.getAuthorizationCode());
            setField(isoMsg, FIELD_RESPONSE_CODE, response.getResponseCode());
            setField(isoMsg, FIELD_MERCHANT_ID, response.getExternalId());
            setField(isoMsg, FIELD_NSU_HOST, response.getPaymentId());

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

    private AuthorizationRequest extractAuthorizationRequestFromIsoMsg(ISOMsg isoMsg) throws ISOFormatException {
        AuthorizationRequest request = new AuthorizationRequest();

        request.setExternalId(getOptionalField(isoMsg, FIELD_EXTERNAL_ID).orElse(null));
        request.setValue(amountFormatter.parse(getOptionalField(isoMsg, FIELD_TRANSACTION_AMOUNT).orElse(null)));
        request.setCardNumber(getOptionalField(isoMsg, FIELD_CARD_NUMBER).orElse(null));
        request.setInstallments(getOptionalField(isoMsg, FIELD_INSTALLMENTS).map(Integer::parseInt).orElse(0));

        try {
            request.setProcessingCode(getField(isoMsg, FIELD_PROCESSING_CODE));
            request.setTransmissionDateTime(LocalDateTime.parse(getField(isoMsg, FIELD_TRANSMISSION_DATE_TIME), DATE_TIME_FORMATTER));
            request.setNsu(getField(isoMsg, FIELD_NSU));
            request.setTransactionTime(LocalTime.parse(getField(isoMsg, FIELD_LOCAL_TRANSACTION_TIME), TIME_FORMATTER));
            request.setTransactionDate(LocalDate.parse(getField(isoMsg, FIELD_LOCAL_TRANSACTION_DATE), DATE_FORMATTER));
            request.setEntryMode(getField(isoMsg, FIELD_ENTRY_MODE));

            String expiryDate = getField(isoMsg, FIELD_EXPIRATION_DATE);
            request.setExpMonth(Integer.parseInt(expiryDate.substring(2, 4)));
            request.setExpYear(Integer.parseInt("20" + expiryDate.substring(0, 2)));

        } catch (DateTimeParseException e) {
            throw new ISOFormatException("Erro ao fazer o parse de datas na mensagem ISO8583: " + e.getMessage(), e);
        }

        return request;
    }

    private void validateMTI(ISOMsg isoMsg, String expectedMTI) throws ISOFormatException, ISOException {
        String mti = isoMsg.getMTI();
        if (!mti.equals(expectedMTI)) {
            String errorMessage = String.format("MTI inválido. Esperado '%s', recebido '%s'", expectedMTI, mti);
            log.error(errorMessage);
            throw new ISOFormatException(errorMessage);
        }
    }

    private ISOMsg buildISOMsg(AuthorizationResponse response, String mti) throws ISOFormatException {
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
}