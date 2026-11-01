package com.alquilereventos.alquiler.service;

import com.alquilereventos.alquiler.dto.RecordatorioResultado;
import com.alquilereventos.common.entity.Cliente;
import com.alquilereventos.common.entity.OrdenAlquiler;
import com.alquilereventos.common.entity.OrdenAlquilerDetalle;
import com.alquilereventos.common.entity.enums.EstadoOrdenAlquiler;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Recordatorio de devolucion por correo. Busca las ordenes en ENTREGA cuya fecha de
 * devolucion es "hoy + dias de anticipacion" (parametro configurable) y le escribe a cada
 * cliente con la plantilla de mail. Lo dispara el cron diario (RecordatorioScheduler) o
 * el boton "Enviar recordatorios ahora" de Configuracion.
 *
 * <p>Si no hay SMTP configurado (variables SPRING_MAIL_*), no falla: registra en el log el
 * mensaje que se habria enviado y lo cuenta como "simulado".</p>
 */
@Service
public class RecordatorioService {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @PersistenceContext
    private EntityManager em;

    private final ConfiguracionService configuracion;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remitente;

    public RecordatorioService(ConfiguracionService configuracion,
                               ObjectProvider<JavaMailSender> mailSender,
                               @Value("${app.mail.from:no-reply@alquilereventos.local}") String remitente) {
        this.configuracion = configuracion;
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Transactional(readOnly = true)
    public RecordatorioResultado enviarRecordatorios(LocalDate hoy) {
        int dias = configuracion.diasAnticipacion();
        LocalDate objetivo = hoy.plusDays(dias);
        String plantilla = configuracion.plantillaEmail();
        JavaMailSender sender = mailSender.getIfAvailable();

        List<OrdenAlquiler> ordenes = em.createQuery(
                        "select distinct o from OrdenAlquiler o join fetch o.cliente "
                                + "where o.estado = :estado and o.fechaDevolucion = :fecha", OrdenAlquiler.class)
                .setParameter("estado", EstadoOrdenAlquiler.ENTREGA)
                .setParameter("fecha", objetivo)
                .getResultList();

        int enviados = 0;
        int simulados = 0;
        int sinEmail = 0;
        int errores = 0;
        for (OrdenAlquiler orden : ordenes) {
            Cliente cliente = orden.getCliente();
            if (cliente.getEmail() == null || cliente.getEmail().isBlank()) {
                sinEmail++;
                log.warn("Recordatorio omitido: el cliente {} {} no tiene email", cliente.getNombre(), cliente.getApellido());
                continue;
            }
            String texto = armarMensaje(plantilla, orden, dias);
            if (sender == null) {
                simulados++;
                log.info("[RECORDATORIO SIMULADO - sin SMTP] para {}: {}", cliente.getEmail(), texto);
                continue;
            }
            try {
                SimpleMailMessage mensaje = new SimpleMailMessage();
                mensaje.setFrom(remitente);
                mensaje.setTo(cliente.getEmail());
                mensaje.setSubject("Recordatorio de devolución de equipos");
                mensaje.setText(texto);
                sender.send(mensaje);
                enviados++;
                log.info("Recordatorio enviado a {}", cliente.getEmail());
            } catch (Exception ex) {
                errores++;
                log.warn("No se pudo enviar el recordatorio a {}: {}", cliente.getEmail(), ex.getMessage());
            }
        }
        log.info("Recordatorios para devoluciones del {}: ordenes={}, enviados={}, simulados={}, sinEmail={}, errores={}",
                objetivo, ordenes.size(), enviados, simulados, sinEmail, errores);
        return new RecordatorioResultado(objetivo, ordenes.size(), enviados, simulados, sinEmail, errores, sender != null);
    }

    private String armarMensaje(String plantilla, OrdenAlquiler orden, int dias) {
        Cliente cliente = orden.getCliente();
        String nombre = (cliente.getNombre() + " " + (cliente.getApellido() == null ? "" : cliente.getApellido())).trim();
        String equipos = orden.getDetalles() == null ? "" : orden.getDetalles().stream()
                .map((OrdenAlquilerDetalle d) -> d.getEquipo().getNombre() + " x" + d.getCantidad())
                .collect(Collectors.joining(", "));
        return plantilla
                .replace("{cliente}", nombre)
                .replace("{fecha}", orden.getFechaDevolucion().format(FECHA))
                .replace("{equipos}", equipos)
                .replace("{dias}", String.valueOf(dias));
    }
}
