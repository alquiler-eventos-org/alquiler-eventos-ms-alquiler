package com.alquilereventos.alquiler.service;

import com.alquilereventos.alquiler.dto.MoraResponse;
import com.alquilereventos.alquiler.exception.RecursoNoEncontradoException;
import com.alquilereventos.common.entity.OrdenAlquiler;
import com.alquilereventos.common.entity.OrdenAlquilerHistorial;
import com.alquilereventos.common.entity.enums.EstadoOrdenAlquiler;
import com.alquilereventos.common.repository.OrdenAlquilerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;

/** Consulta de la mora de una orden de alquiler (con la tarifa configurada). */
@Service
public class MoraService {

    private final OrdenAlquilerRepository ordenRepository;
    private final ConfiguracionService configuracion;

    public MoraService(OrdenAlquilerRepository ordenRepository, ConfiguracionService configuracion) {
        this.ordenRepository = ordenRepository;
        this.configuracion = configuracion;
    }

    @Transactional(readOnly = true)
    public MoraResponse consultar(Integer id) {
        OrdenAlquiler orden = ordenRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Orden de alquiler no encontrada con id " + id));
        BigDecimal tarifa = configuracion.tarifaMoraDia().orElse(null);
        EstadoOrdenAlquiler estado = orden.getEstado();
        LocalDate devolucion = orden.getFechaDevolucion();

        if (estado == EstadoOrdenAlquiler.DEVOLUCION || estado == EstadoOrdenAlquiler.MORA) {
            // Si ya se registro mora en el historial, esa es la que vale.
            BigDecimal registrada = orden.getHistorial() == null ? null : orden.getHistorial().stream()
                    .filter(h -> h.getMontoMora() != null)
                    .filter(h -> h.getFecha() != null)
                    .max(Comparator.comparing(OrdenAlquilerHistorial::getFecha))
                    .map(OrdenAlquilerHistorial::getMontoMora)
                    .filter(Objects::nonNull)
                    .orElse(null);
            if (registrada != null && estado == EstadoOrdenAlquiler.DEVOLUCION) {
                return new MoraResponse(estado.name(), devolucion, diasDeAtraso(devolucion), tarifa, registrada, "REGISTRADA");
            }
        }
        if ((estado == EstadoOrdenAlquiler.ENTREGA || estado == EstadoOrdenAlquiler.MORA) && devolucion != null) {
            long dias = diasDeAtraso(devolucion);
            BigDecimal monto = configuracion.montoMora(orden.getTotal(), dias);
            if (monto != null) {
                return new MoraResponse(estado.name(), devolucion, dias, tarifa, monto, "ESTIMADA");
            }
        }
        return new MoraResponse(estado.name(), devolucion, 0, tarifa, null, "SIN_MORA");
    }

    private long diasDeAtraso(LocalDate devolucion) {
        if (devolucion == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(devolucion, LocalDate.now()));
    }
}
