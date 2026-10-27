package com.alquilereventos.alquiler.dto;

import java.math.BigDecimal;

/**
 * Vista de solo lectura de un equipo y su stock, para la UI y para armar
 * ordenes (de alquiler y de compra). No expone relaciones lazy de la entidad.
 */
public record EquipoResponse(Integer id,
                             String nombre,
                             Integer categoriaId,
                             String categoria,
                             Integer stock,
                             BigDecimal precioDia,
                             String estado) {
}
