package com.alquilereventos.alquiler.service;

import com.alquilereventos.common.dto.OrdenAlquiler;
import com.alquilereventos.common.dto.OrdenAlquilerCrearRequest;
import com.alquilereventos.common.dto.OrdenAlquilerEstadoRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Logica de negocio de ordenes de alquiler.
 *
 * <p>Entrega 3: el usuario que realiza la accion (<b>usuarioActorId</b>) llega
 * siempre desde el claim {@code uid} del JWT autenticado; nunca desde el body.</p>
 */
public interface OrdenAlquilerService {

    OrdenAlquiler crear(OrdenAlquilerCrearRequest request, Integer usuarioActorId);

    OrdenAlquiler actualizar(Integer id, OrdenAlquilerCrearRequest request, Integer usuarioActorId);

    OrdenAlquiler obtenerPorId(Integer id);

    Page<OrdenAlquiler> listar(Pageable pageable);

    Page<OrdenAlquiler> buscar(com.alquilereventos.common.entity.enums.EstadoOrdenAlquiler estado,
                               Integer clienteId,
                               LocalDate fechaDesde,
                               LocalDate fechaHasta,
                               Pageable pageable);

    void eliminar(Integer id, Integer usuarioActorId);

    OrdenAlquiler cambiarEstado(Integer id, OrdenAlquilerEstadoRequest request, Integer usuarioActorId);
}
