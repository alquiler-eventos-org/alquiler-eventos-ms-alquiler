package com.alquilereventos.alquiler.config.cache;

import org.springframework.stereotype.Component;

/**
 * Genera claves de negocio legibles y consistentes:
 * {@code <prefijo>:<ambiente>:<servicio>:<tipo>:<version>:<id>}
 * (ej. {@code ae:local:alquiler:cliente:v1:5}).
 */
@Component("cacheKeys")
public class CacheKeyFactory {

    private final CacheProperties properties;

    public CacheKeyFactory(CacheProperties properties) {
        this.properties = properties;
    }

    public String cliente(Integer id) {
        return key("clientes", id);
    }

    public String ordenAlquiler(Integer id) {
        return key("ordenes-alquiler", id);
    }

    private String key(String domain, Integer id) {
        CacheProperties.Domain definition = properties.getDomains().get(domain);
        if (definition == null || definition.getKeyType() == null) {
            throw new IllegalStateException("No existe configuracion de clave para el dominio " + domain);
        }
        return String.join(":", properties.getKeyPrefix(), properties.getEnvironment(),
                properties.getServiceName(), definition.getKeyType(), properties.getKeyVersion(),
                String.valueOf(id));
    }
}
