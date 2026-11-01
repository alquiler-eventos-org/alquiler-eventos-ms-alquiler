package com.alquilereventos.alquiler.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Parametros de configuracion que maneja el encargado de alquiler (y el admin):
 * tarifa de mora por dia, dias de anticipacion del recordatorio y plantilla del mail.
 * Un valor null borra el parametro y el sistema vuelve a usar el valor por defecto.
 */
public record ConfiguracionAlquilerDto(@DecimalMin("0.0") BigDecimal tarifaMoraDia,
                                       @Min(0) @Max(365) Integer diasAnticipacion,
                                       @Size(max = 5000) String plantillaEmail) {
}
