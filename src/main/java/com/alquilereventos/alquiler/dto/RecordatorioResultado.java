package com.alquilereventos.alquiler.dto;

import java.time.LocalDate;

/**
 * Resumen de una corrida de recordatorios.
 * enviados = correos realmente enviados por SMTP; simulados = no hay SMTP configurado y solo se registro en el log.
 */
public record RecordatorioResultado(LocalDate fechaObjetivo,
                                    int ordenes,
                                    int enviados,
                                    int simulados,
                                    int sinEmail,
                                    int errores,
                                    boolean smtpConfigurado) {
}
