package com.alquilereventos.alquiler.service;

import com.alquilereventos.common.dto.EstadoOrdenAlquiler;
import com.alquilereventos.common.dto.OrdenAlquilerEstadoRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Pasa a MORA, sin intervencion manual, las ordenes en ENTREGA cuya fecha de devolucion ya
 * vencio (fechaDevolucion &lt; hoy). Lo dispara MoraAutomaticaScheduler.
 *
 * <p>Reutiliza {@link OrdenAlquilerService#cambiarEstado}: asi se aplican las mismas reglas
 * que al hacerlo a mano (valida la transicion, calcula el monto de mora con la tarifa
 * configurada, guarda el historial y refresca la cache). El historial exige un usuario: se
 * atribuye al usuario que registro el ultimo cambio de esa orden (el que la entrego).</p>
 */
@Service
public class MoraAutomaticaService {

    private static final Logger log = LoggerFactory.getLogger(MoraAutomaticaService.class);

    @PersistenceContext
    private EntityManager em;

    private final OrdenAlquilerService ordenAlquilerService;

    public MoraAutomaticaService(OrdenAlquilerService ordenAlquilerService) {
        this.ordenAlquilerService = ordenAlquilerService;
    }

    /** @return cantidad de ordenes que pasaron a MORA en esta corrida. */
    public int marcarVencidas(LocalDate hoy) {
        List<Integer> vencidas = em.createQuery(
                        "select o.id from OrdenAlquiler o "
                                + "where o.estado = com.alquilereventos.common.entity.enums.EstadoOrdenAlquiler.ENTREGA "
                                + "and o.fechaDevolucion < :hoy", Integer.class)
                .setParameter("hoy", hoy)
                .getResultList();

        int marcadas = 0;
        for (Integer ordenId : vencidas) {
            List<Integer> actores = em.createQuery(
                            "select h.usuario.id from OrdenAlquilerHistorial h "
                                    + "where h.ordenAlquiler.id = :id order by h.id desc", Integer.class)
                    .setParameter("id", ordenId)
                    .setMaxResults(1)
                    .getResultList();
            if (actores.isEmpty()) {
                log.warn("Mora automatica omitida: la orden {} no tiene historial con usuario", ordenId);
                continue;
            }
            try {
                OrdenAlquilerEstadoRequest pedido = new OrdenAlquilerEstadoRequest();
                pedido.setEstado(EstadoOrdenAlquiler.MORA);
                ordenAlquilerService.cambiarEstado(ordenId, pedido, actores.get(0));
                marcadas++;
                log.info("Orden {} paso a MORA automaticamente (devolucion vencida)", ordenId);
            } catch (Exception ex) {
                log.warn("No se pudo pasar la orden {} a MORA: {}", ordenId, ex.getMessage());
            }
        }
        log.info("Mora automatica {}: vencidas={}, marcadas={}", hoy, vencidas.size(), marcadas);
        return marcadas;
    }
}
