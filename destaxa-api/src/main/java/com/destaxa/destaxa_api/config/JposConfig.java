package com.destaxa.destaxa_api.config;

import org.jpos.iso.ISOException;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JposConfig {

    @Value("${jpos.packager.path}")
    private String packagerPath;

    @Bean
    public GenericPackager genericPackager() throws ISOException {
        return new GenericPackager(packagerPath);
    }
}
