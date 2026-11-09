package com.alquilereventos.alquiler.scheduling;

import com.alquilereventos.alquiler.service.MoraAutomaticaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Pasa a MORA las ordenes en ENTREGA cuyo plazo de devolucion vencio. Corre al arrancar el
 * servicio (por si estuvo apagado) y luego cada 30 minutos; el cron se cambia con
 * {@code app.mora.cron} (variable MORA_CRON) sin tocar el codigo.
 */
@Component
public class MoraAutomaticaScheduler {

    private static final Logger log = LoggerFactory.getLogger(MoraAutomaticaScheduler.class);

    private final MoraAutomaticaService moraAutomaticaService;
    private final ZoneId zona;

    public MoraAutomaticaScheduler(MoraAutomaticaService moraAutomaticaService,
                                   @Value("${app.recordatorios.zone:America/Asuncion}") String zona) {
        this.moraAutomaticaService = moraAutomaticaService;
        this.zona = ZoneId.of(zona);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        ejecutar();
    }

    @Scheduled(cron = "${app.mora.cron:0 */30 * * * *}", zone = "${app.recordatorios.zone:America/Asuncion}")
    public void ejecutar() {
        try {
            moraAutomaticaService.marcarVencidas(LocalDate.now(zona));
        } catch (Exception ex) {
            log.error("Fallo la corrida de mora automatica", ex);
        }
    }
}
