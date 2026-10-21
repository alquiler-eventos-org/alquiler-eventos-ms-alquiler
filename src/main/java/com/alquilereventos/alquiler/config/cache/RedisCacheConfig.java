package com.alquilereventos.alquiler.config.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Configura el CacheManager de Redis: una seccion por dominio, TTL por dominio
 * (leido de application.yml) y serializacion JSON de los DTOs.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(prefix = "app.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheConfig {

    @Bean
    @Primary
    CacheManager cacheManager(RedisConnectionFactory connectionFactory, CacheProperties properties) {
        // Los DTOs tienen LocalDate / OffsetDateTime: hace falta el modulo JSR-310
        // (y el de JsonNullable de los DTOs generados). findAndRegisterModules() los
        // registra si estan en el classpath.
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults);

        properties.getDomains().forEach((domain, definition) -> {
            RedisCacheConfiguration configuration = defaults;
            if (definition.getTtlSeconds() > 0) {
                configuration = configuration.entryTtl(Duration.ofSeconds(definition.getTtlSeconds()));
            }
            builder.withCacheConfiguration(definition.getName(), configuration);
        });

        RedisCacheManager redisCacheManager = builder.build();
        // Sin esto el RedisCacheManager envuelto nunca carga las secciones con su TTL
        redisCacheManager.afterPropertiesSet();
        return new LoggingCacheManager(redisCacheManager);
    }
}
