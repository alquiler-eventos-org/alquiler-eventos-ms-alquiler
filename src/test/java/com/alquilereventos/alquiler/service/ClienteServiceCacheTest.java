package com.alquilereventos.alquiler.service;

import com.alquilereventos.alquiler.config.cache.CacheKeyFactory;
import com.alquilereventos.alquiler.config.cache.CacheProperties;
import com.alquilereventos.alquiler.mapper.ClienteMapper;
import com.alquilereventos.common.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica el comportamiento cache-aside (MISS -> BD, HIT -> sin BD) y la
 * invalidacion, con un CacheManager en memoria (sin Redis).
 */
@SpringJUnitConfig({ClienteServiceCacheTest.Config.class, com.alquilereventos.alquiler.config.cache.CacheResolverConfig.class})
@TestPropertySource(properties = {
        "app.cache.service-name=alquiler",
        "app.cache.key-prefix=ae",
        "app.cache.environment=test",
        "app.cache.key-version=v1",
        "app.cache.domains.clientes.name=alquiler:clientes",
        "app.cache.domains.clientes.key-type=cliente",
        "app.cache.domains.clientes.ttl-seconds=900"
})
class ClienteServiceCacheTest {

    @Configuration
    @EnableCaching
    @EnableConfigurationProperties(CacheProperties.class)
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        CacheKeyFactory cacheKeys(CacheProperties properties) {
            return new CacheKeyFactory(properties);
        }

        @Bean
        ClienteMapper clienteMapper() {
            return new ClienteMapper();
        }

        @Bean
        ClienteService clienteService(ClienteRepository repository, ClienteMapper mapper,
                                      CacheManager cacheManager, CacheKeyFactory cacheKeys,
                                      @Value("${app.cache.domains.clientes.name}") String cacheName) {
            return new ClienteServiceImpl(repository, mapper, cacheManager, cacheKeys, cacheName);
        }
    }

    @MockitoBean
    private ClienteRepository clienteRepository;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private CacheManager cacheManager;

    private com.alquilereventos.common.entity.Cliente entidad;

    @BeforeEach
    void setUp() {
        cacheManager.getCache("alquiler:clientes").clear();
        reset(clienteRepository);
        entidad = new com.alquilereventos.common.entity.Cliente();
        entidad.setId(1);
        entidad.setNombre("Ana");
        entidad.setApellido("Gomez");
        entidad.setDocumento("123");
        entidad.setActivo(true);
    }

    @Test
    @DisplayName("Primer GET: MISS (consulta la BD). Segundo GET: HIT (no vuelve a la BD)")
    void getIndividualUsaCache() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(entidad));

        clienteService.obtenerPorId(1);
        clienteService.obtenerPorId(1);

        verify(clienteRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Baja logica expulsa el objeto: el siguiente GET vuelve a la BD")
    void bajaLogicaInvalidaCache() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(entidad));

        clienteService.obtenerPorId(1);      // BD (1) + SET
        clienteService.eliminar(1);          // BD (2) + DELETE
        clienteService.obtenerPorId(1);      // BD (3) + SET

        verify(clienteRepository, times(3)).findById(1);
    }
}
