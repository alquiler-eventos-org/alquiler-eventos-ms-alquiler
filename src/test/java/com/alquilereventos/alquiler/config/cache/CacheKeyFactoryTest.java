package com.alquilereventos.alquiler.config.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheKeyFactoryTest {

    private CacheKeyFactory cacheKeys;

    @BeforeEach
    void setUp() {
        CacheProperties properties = new CacheProperties();
        properties.setServiceName("alquiler");
        properties.setKeyPrefix("ae");
        properties.setEnvironment("local");
        properties.setKeyVersion("v1");
        properties.setDomains(new LinkedHashMap<>());

        CacheProperties.Domain clientes = new CacheProperties.Domain();
        clientes.setName("alquiler:clientes");
        clientes.setKeyType("cliente");
        clientes.setTtlSeconds(900);
        properties.getDomains().put("clientes", clientes);

        CacheProperties.Domain ordenes = new CacheProperties.Domain();
        ordenes.setName("alquiler:ordenes-alquiler");
        ordenes.setKeyType("orden-alquiler");
        ordenes.setTtlSeconds(60);
        properties.getDomains().put("ordenes-alquiler", ordenes);

        cacheKeys = new CacheKeyFactory(properties);
    }

    @Test
    @DisplayName("Formato legible ae:<ambiente>:alquiler:<tipo>:v1:<id>")
    void formatoDeClave() {
        assertThat(cacheKeys.cliente(5)).isEqualTo("ae:local:alquiler:cliente:v1:5");
        assertThat(cacheKeys.ordenAlquiler(9)).isEqualTo("ae:local:alquiler:orden-alquiler:v1:9");
    }

    @Test
    @DisplayName("Dominios distintos con el mismo id no colisionan")
    void sinColisiones() {
        assertThat(cacheKeys.cliente(1)).isNotEqualTo(cacheKeys.ordenAlquiler(1));
    }

    @Test
    @DisplayName("Dominio sin configuracion falla rapido")
    void dominioDesconocido() {
        CacheProperties sinDominios = new CacheProperties();
        sinDominios.setServiceName("alquiler");
        sinDominios.setKeyPrefix("ae");
        sinDominios.setEnvironment("local");
        sinDominios.setKeyVersion("v1");
        assertThatThrownBy(() -> new CacheKeyFactory(sinDominios).cliente(1))
                .isInstanceOf(IllegalStateException.class);
    }
}
