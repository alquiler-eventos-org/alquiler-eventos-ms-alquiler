package com.alquilereventos.alquiler.service;

import com.alquilereventos.common.dto.EstadoOrdenAlquiler;
import com.alquilereventos.common.dto.OrdenAlquilerEstadoRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MoraAutomaticaServiceTest {

    @SuppressWarnings("unchecked")
    private TypedQuery<Integer> consulta(List<Integer> resultado) {
        TypedQuery<Integer> q = mock(TypedQuery.class);
        when(q.setParameter(anyString(), any())).thenReturn(q);
        when(q.setMaxResults(anyInt())).thenReturn(q);
        when(q.getResultList()).thenReturn(resultado);
        return q;
    }

    private MoraAutomaticaService servicio(OrdenAlquilerService ordenes, EntityManager em) {
        MoraAutomaticaService servicio = new MoraAutomaticaService(ordenes);
        ReflectionTestUtils.setField(servicio, "em", em);
        return servicio;
    }

    @Test
    @DisplayName("marcarVencidas: pasa a MORA cada orden vencida usando el usuario de su ultimo historial")
    void marcaLasVencidas() {
        OrdenAlquilerService ordenes = mock(OrdenAlquilerService.class);
        EntityManager em = mock(EntityManager.class);
        TypedQuery<Integer> vencidas = consulta(List.of(7));  // ordenes vencidas
        TypedQuery<Integer> actor = consulta(List.of(3));     // usuario actor de la orden 7
        when(em.createQuery(anyString(), eq(Integer.class))).thenReturn(vencidas, actor);

        int marcadas = servicio(ordenes, em).marcarVencidas(LocalDate.of(2026, 11, 10));

        assertEquals(1, marcadas);
        ArgumentCaptor<OrdenAlquilerEstadoRequest> pedido = ArgumentCaptor.forClass(OrdenAlquilerEstadoRequest.class);
        verify(ordenes).cambiarEstado(eq(7), pedido.capture(), eq(3));
        assertEquals(EstadoOrdenAlquiler.MORA, pedido.getValue().getEstado());
    }

    @Test
    @DisplayName("marcarVencidas: si una orden falla, sigue con las demas")
    void unaFallaNoDetieneLasDemas() {
        OrdenAlquilerService ordenes = mock(OrdenAlquilerService.class);
        EntityManager em = mock(EntityManager.class);
        TypedQuery<Integer> vencidas = consulta(List.of(7, 8));
        TypedQuery<Integer> actor7 = consulta(List.of(3));
        TypedQuery<Integer> actor8 = consulta(List.of(3));
        when(em.createQuery(anyString(), eq(Integer.class))).thenReturn(vencidas, actor7, actor8);
        doThrow(new RuntimeException("sin atraso")).when(ordenes).cambiarEstado(eq(7), any(), anyInt());

        int marcadas = servicio(ordenes, em).marcarVencidas(LocalDate.of(2026, 11, 10));

        assertEquals(1, marcadas);
        verify(ordenes).cambiarEstado(eq(8), any(), eq(3));
    }

    @Test
    @DisplayName("marcarVencidas: sin ordenes vencidas no hace nada")
    void sinVencidas() {
        OrdenAlquilerService ordenes = mock(OrdenAlquilerService.class);
        EntityManager em = mock(EntityManager.class);
        TypedQuery<Integer> vacia = consulta(List.of());
        when(em.createQuery(anyString(), eq(Integer.class))).thenReturn(vacia);

        assertEquals(0, servicio(ordenes, em).marcarVencidas(LocalDate.now()));
        verify(ordenes, never()).cambiarEstado(anyInt(), any(), anyInt());
    }
}
