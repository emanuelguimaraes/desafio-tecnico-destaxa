package com.destaxa.authorization.config;

import com.destaxa.authorization.iso.formatter.AmountFormatter;
import org.jpos.iso.ISOException;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ISO8583Config {

    @Value("${jpos.packager.path}")
    private String packagerPath;

    @Bean
    public GenericPackager genericPackager() throws ISOException {
        return new GenericPackager(getClass().getResourceAsStream(packagerPath));
    }

    @Bean
    public AmountFormatter amountFormatter() {
        return new AmountFormatter();
    }
}
