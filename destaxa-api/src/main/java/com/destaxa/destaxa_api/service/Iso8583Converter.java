package com.destaxa.destaxa_api.service;

import com.destaxa.destaxa_api.dto.AuthorizationRequest;
import lombok.extern.slf4j.Slf4j;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@Slf4j
@Service
public class Iso8583Converter {

    private final GenericPackager packager;

    @Autowired
    public Iso8583Converter(GenericPackager packager) {
        this.packager = packager;
    }

    public String toIso8583(AuthorizationRequest request) {
        try {
            ISOMsg isoMsg = new ISOMsg();
            isoMsg.setPackager(packager);
            isoMsg.setMTI("0200");

            isoMsg.set(2, request.getCardNumber());
            isoMsg.set(3, "000000");
            isoMsg.set(4, formatAmount(request.getValue()));
            isoMsg.set(11, generateStan());
            isoMsg.set(14, request.getExpYear().toString() + request.getExpMonth().toString().substring(request.getExpMonth().toString().length() - 1));
            isoMsg.set(22, "051");
            isoMsg.set(25, "00");
            isoMsg.set(35, request.getCardNumber());
            isoMsg.set(41, "12345678");
            isoMsg.set(42, "123456789012345");
            isoMsg.set(49, "986");
            if (request.getInstallments() != null) {
                isoMsg.set(54, request.getInstallments().toString());
            }
            isoMsg.set(123, "000");

            log.debug("Mensagem ISO8583 gerada: {}", isoMsg);

            return new String(isoMsg.pack());
        } catch (ISOException e) {
            log.error("Erro ao converter para ISO8583", e);
            throw new RuntimeException("Erro ao converter para ISO8583", e);
        }
    }

    private String formatAmount(BigDecimal amount) {
        DecimalFormat df = new DecimalFormat("000000000000");
        return df.format(amount.multiply(new BigDecimal("100")).longValue());
    }

    private String generateStan() {
        return String.valueOf((int) (Math.random() * 1000000));
    }
}