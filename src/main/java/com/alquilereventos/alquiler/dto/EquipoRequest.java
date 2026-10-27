package com.alquilereventos.alquiler.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cuerpo para crear o editar un equipo. La categoria se elige por id
 * (catalogo GET /categorias). El estado es opcional: si no viene, DISPONIBLE.
 */
public record EquipoRequest(@NotBlank @Size(max = 255) String nombre,
                            @NotNull Integer categoriaId,
                            @NotNull @Min(0) Integer stock,
                            @NotNull @DecimalMin("0.0") BigDecimal precioDia,
                            String estado) {
}
