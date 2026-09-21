package com.alquilereventos.alquiler.mapper;

import com.alquilereventos.common.dto.OrdenAlquiler;
import com.alquilereventos.common.dto.OrdenAlquilerDetalle;
import com.alquilereventos.common.entity.Equipo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrdenAlquilerMapper implements Mapper<com.alquilereventos.common.entity.OrdenAlquiler, OrdenAlquiler> {

    @Override
    public OrdenAlquiler toDto(com.alquilereventos.common.entity.OrdenAlquiler entity) {
        Integer clienteId = entity.getCliente() != null ? entity.getCliente().getId() : null;
        String clienteNombre = null;
        if (entity.getCliente() != null) {
            clienteNombre = entity.getCliente().getNombre() + " " + entity.getCliente().getApellido();
        }
        OffsetDateTime createdAt = entity.getCreatedAt() != null
                ? entity.getCreatedAt().atOffset(ZoneOffset.UTC)
                : null;

        OrdenAlquiler dto = new OrdenAlquiler(
                entity.getId(),
                clienteNombre,
                entity.getTotal() != null ? entity.getTotal().doubleValue() : null,
                createdAt);
        dto.setClienteId(clienteId);
        dto.setUsuarioId(entity.getUsuario() != null ? entity.getUsuario().getId() : null);
        dto.setFechaEvento(entity.getFechaEvento());
        dto.setFechaDevolucion(entity.getFechaDevolucion());
        if (entity.getEstado() != null) {
            dto.setEstado(com.alquilereventos.common.dto.EstadoOrdenAlquiler.valueOf(entity.getEstado().name()));
        }

        if (entity.getDetalles() != null) {
            int dias = calcularDias(entity.getFechaEvento(), entity.getFechaDevolucion());
            List<OrdenAlquilerDetalle> detallesDto = new ArrayList<>();
            for (com.alquilereventos.common.entity.OrdenAlquilerDetalle detalle : entity.getDetalles()) {
                detallesDto.add(toDetalleDto(detalle, dias));
            }
            dto.setDetalles(detallesDto);
        }
        return dto;
    }

    public OrdenAlquilerDetalle toDetalleDto(com.alquilereventos.common.entity.OrdenAlquilerDetalle entity, int dias) {
        String equipoNombre = entity.getEquipo() != null ? entity.getEquipo().getNombre() : null;
        OrdenAlquilerDetalle dto = new OrdenAlquilerDetalle(
                entity.getId(),
                equipoNombre,
                dias,
                entity.getSubtotal() != null ? entity.getSubtotal().doubleValue() : null);
        dto.setEquipoId(entity.getEquipo() != null ? entity.getEquipo().getId() : null);
        dto.setCantidad(entity.getCantidad());
        dto.setPrecioDia(entity.getPrecioUnitario() != null ? entity.getPrecioUnitario().doubleValue() : null);
        return dto;
    }

    public com.alquilereventos.common.entity.OrdenAlquilerDetalle toDetalleEntity(
            OrdenAlquilerDetalle detalleDto,
            Equipo equipo,
            com.alquilereventos.common.entity.OrdenAlquiler orden,
            BigDecimal subtotal) {
        com.alquilereventos.common.entity.OrdenAlquilerDetalle entity =
                new com.alquilereventos.common.entity.OrdenAlquilerDetalle();
        entity.setOrdenAlquiler(orden);
        entity.setEquipo(equipo);
        entity.setCantidad(detalleDto.getCantidad());
        entity.setPrecioUnitario(equipo.getPrecioDia());
        entity.setSubtotal(subtotal);
        return entity;
    }

    @Override
    public com.alquilereventos.common.entity.OrdenAlquiler toEntity(OrdenAlquiler dto) {
        com.alquilereventos.common.entity.OrdenAlquiler entity = new com.alquilereventos.common.entity.OrdenAlquiler();
        entity.setId(dto.getId());
        entity.setFechaEvento(dto.getFechaEvento());
        entity.setFechaDevolucion(dto.getFechaDevolucion());
        if (dto.getEstado() != null) {
            entity.setEstado(com.alquilereventos.common.entity.enums.EstadoOrdenAlquiler.valueOf(dto.getEstado().name()));
        }
        if (dto.getTotal() != null) {
            entity.setTotal(BigDecimal.valueOf(dto.getTotal()));
        }
        return entity;
    }

    private int calcularDias(java.time.LocalDate fechaEvento, java.time.LocalDate fechaDevolucion) {
        if (fechaEvento == null) {
            return 1;
        }
        if (fechaDevolucion == null) {
            return 1;
        }
        long dias = java.time.temporal.ChronoUnit.DAYS.between(fechaEvento, fechaDevolucion) + 1;
        return dias < 1 ? 1 : (int) dias;
    }
}