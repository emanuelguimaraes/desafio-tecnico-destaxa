package com.destaxa.destaxa_api.service;

import com.destaxa.destaxa_api.dto.AuthorizationRequest;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class Iso8583ConverterTest {

    private Iso8583Converter converter;
    private GenericPackager packagerMock;

    @Value("${jpos.packager.path}")
    private String packagerPath;

    @BeforeEach
    public void setup() throws ISOException {
        packagerMock = mock(GenericPackager.class);
        converter = new Iso8583Converter(packagerMock);
    }

    @Test
    void toIso8583_validRequest() throws Exception {
        AuthorizationRequest request = criarRequisicaoValida();
        when(packagerMock.pack(any(ISOMsg.class))).thenReturn(new byte[0]);

        String isoMessage = converter.toIso8583(request);

        assertNotNull(isoMessage);

        ArgumentCaptor<ISOMsg> isoMsgCaptor = ArgumentCaptor.forClass(ISOMsg.class);
        verify(packagerMock, times(1)).pack(isoMsgCaptor.capture());

        ISOMsg capturedIsoMsg = isoMsgCaptor.getValue();

        assertEquals("0200", capturedIsoMsg.getMTI());
        assertEquals(request.getCardNumber(), capturedIsoMsg.getString(2));
        assertEquals("000000001050", capturedIsoMsg.getString(4));
    }

    @Test
    void toIso8583_invalidRequest()  {
        AuthorizationRequest request = new AuthorizationRequest();
        assertThrows(RuntimeException.class, () -> converter.toIso8583(request));
    }

    private AuthorizationRequest criarRequisicaoValida() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setExternalId(UUID.randomUUID().toString());
        request.setValue(new BigDecimal("10.50"));
        request.setCardNumber("1234567890123456");
        request.setCvv("123");
        request.setExpMonth(12);
        request.setExpYear(2025);
        request.setHolderName("João da Silva");
        request.setInstallments(1);
        return request;
    }
}