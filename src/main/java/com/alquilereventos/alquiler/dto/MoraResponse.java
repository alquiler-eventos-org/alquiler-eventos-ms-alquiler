package com.alquilereventos.alquiler.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mora de una orden. origen: REGISTRADA (se guardo al devolver/marcar mora),
 * ESTIMADA (calculada hoy, la orden sigue afuera) o SIN_MORA.
 */
public record MoraResponse(String estado,
                           LocalDate fechaDevolucion,
                           long diasAtraso,
                           BigDecimal tarifaDia,
                           BigDecimal montoMora,
                           String origen) {
}
