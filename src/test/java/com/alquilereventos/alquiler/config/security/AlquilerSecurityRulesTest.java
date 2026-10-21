package com.alquilereventos.alquiler.config.security;

import com.alquilereventos.alquiler.controller.ClienteController;
import com.alquilereventos.alquiler.controller.OrdenAlquilerController;
import com.alquilereventos.alquiler.exception.ApiExceptionHandler;
import com.alquilereventos.alquiler.service.ClienteService;
import com.alquilereventos.alquiler.service.OrdenAlquilerService;
import com.alquilereventos.common.dto.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la cadena real de Spring Security (401 / 403 / 200) de ms-alquiler.
 * El secreto es solo de prueba. Se fijan las clases del slice con
 * {@code @ContextConfiguration} para no arrastrar el {@code @EnableJpaRepositories}
 * de la aplicacion.
 */
@WebMvcTest
@ContextConfiguration(classes = {
        ClienteController.class,
        OrdenAlquilerController.class,
        AlquilerSecurityConfig.class,
        SecurityErrorHandler.class,
        ApiExceptionHandler.class
})
@TestPropertySource(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.jwt.issuer=alquiler-eventos-seguridad"
})
class AlquilerSecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private OrdenAlquilerService ordenAlquilerService;

    @Test
    @DisplayName("Sin token -> 401 con ApiError")
    void sinToken() throws Exception {
        mockMvc.perform(get("/clientes/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));
    }

    @Test
    @DisplayName("Rol ENCARGADO_COMPRAS sobre alquiler -> 403 con ApiError")
    void rolIncorrecto() throws Exception {
        mockMvc.perform(get("/clientes/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ENCARGADO_COMPRAS"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403));
    }

    @Test
    @DisplayName("Rol ENCARGADO_DEPOSITO -> 200")
    void rolDeposito() throws Exception {
        when(clienteService.obtenerPorId(1)).thenReturn(new Cliente(1, true));

        mockMvc.perform(get("/clientes/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ENCARGADO_DEPOSITO"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Rol ADMIN -> 200 (un usuario con multiples roles accede a todo)")
    void rolAdmin() throws Exception {
        when(clienteService.obtenerPorId(1)).thenReturn(new Cliente(1, true));

        mockMvc.perform(get("/clientes/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Token valido sin claim uid al crear orden -> 401 (no 400 ni 500)")
    void tokenSinUid() throws Exception {
        mockMvc.perform(post("/ordenes-alquiler")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ENCARGADO_DEPOSITO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"fechaEvento\":\"2026-10-20\","
                                + "\"detalles\":[{\"equipoId\":1,\"cantidad\":1}]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));
    }
}
