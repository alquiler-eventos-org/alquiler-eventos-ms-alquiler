package com.alquilereventos.alquiler.scheduling;

import com.alquilereventos.alquiler.service.RecordatorioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Tarea programada (cron) que envia los recordatorios de devolucion una vez por dia.
 * El horario sale de {@code app.recordatorios.cron} (por defecto todos los dias a las 08:00
 * en America/Asuncion); se cambia con RECORDATORIOS_CRON sin tocar el codigo.
 */
@Component
public class RecordatorioScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioScheduler.class);

    private final RecordatorioService recordatorioService;
    private final ZoneId zona;

    public RecordatorioScheduler(RecordatorioService recordatorioService,
                                 @Value("${app.recordatorios.zone:America/Asuncion}") String zona) {
        this.recordatorioService = recordatorioService;
        this.zona = ZoneId.of(zona);
    }

    @Scheduled(cron = "${app.recordatorios.cron:0 0 8 * * *}", zone = "${app.recordatorios.zone:America/Asuncion}")
    public void enviarRecordatoriosDiarios() {
        log.info("Cron de recordatorios: iniciando corrida");
        try {
            recordatorioService.enviarRecordatorios(LocalDate.now(zona));
        } catch (Exception ex) {
            log.error("Fallo la corrida de recordatorios", ex);
        }
    }
}
