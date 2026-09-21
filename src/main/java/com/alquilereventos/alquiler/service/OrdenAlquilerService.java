package com.alquilereventos.alquiler.service;

import com.alquilereventos.common.dto.OrdenAlquiler;
import com.alquilereventos.common.dto.OrdenAlquilerCrearRequest;
import com.alquilereventos.common.dto.OrdenAlquilerEstadoRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface OrdenAlquilerService {

    OrdenAlquiler crear(OrdenAlquilerCrearRequest request);

    OrdenAlquiler actualizar(Integer id, OrdenAlquilerCrearRequest request);

    OrdenAlquiler obtenerPorId(Integer id);

    Page<OrdenAlquiler> listar(Pageable pageable);

    Page<OrdenAlquiler> buscar(com.alquilereventos.common.entity.enums.EstadoOrdenAlquiler estado,
                               Integer clienteId,
                               LocalDate fechaDesde,
                               LocalDate fechaHasta,
                               Pageable pageable);

    void eliminar(Integer id);

    OrdenAlquiler cambiarEstado(Integer id, OrdenAlquilerEstadoRequest request);
}