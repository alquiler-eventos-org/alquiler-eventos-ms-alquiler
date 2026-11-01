package com.alquilereventos.alquiler.service;

import com.alquilereventos.alquiler.dto.ConfiguracionAlquilerDto;
import com.alquilereventos.alquiler.exception.ReglaNegocioException;
import com.alquilereventos.common.entity.Configuracion;
import com.alquilereventos.common.repository.ConfiguracionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Parametros del sistema guardados en la tabla {@code configuraciones} (clave/valor).
 * Si un parametro no esta cargado se usa el valor por defecto.
 */
@Service
public class ConfiguracionService {

    static final String CLAVE_MORA = "mora.tarifa_dia";
    static final String CLAVE_DIAS = "recordatorio.dias_anticipacion";
    static final String CLAVE_PLANTILLA = "email.plantilla";
    static final String CLAVE_LOGO = "sistema.logo";

    public static final int DIAS_POR_DEFECTO = 2;
    public static final String PLANTILLA_POR_DEFECTO =
            "Hola {cliente}, te recordamos que la devolución de los equipos ({equipos}) es el {fecha}. ¡Gracias!";

    private static final int LOGO_MAX_CARACTERES = 400_000;

    private final ConfiguracionRepository repository;
    private final double moraPorcentajeDia;

    public ConfiguracionService(ConfiguracionRepository repository,
                                @Value("${app.mora.porcentaje-dia:0.05}") double moraPorcentajeDia) {
        this.repository = repository;
        this.moraPorcentajeDia = moraPorcentajeDia;
    }

    @Transactional(readOnly = true)
    public ConfiguracionAlquilerDto obtenerAlquiler() {
        return new ConfiguracionAlquilerDto(tarifaMoraDia().orElse(null), diasAnticipacion(), plantillaEmail());
    }

    @Transactional
    public ConfiguracionAlquilerDto guardarAlquiler(ConfiguracionAlquilerDto dto) {
        guardar(CLAVE_MORA, dto.tarifaMoraDia() == null ? null : dto.tarifaMoraDia().toPlainString());
        guardar(CLAVE_DIAS, dto.diasAnticipacion() == null ? null : dto.diasAnticipacion().toString());
        guardar(CLAVE_PLANTILLA, dto.plantillaEmail());
        return obtenerAlquiler();
    }

    /** Tarifa fija de mora por dia de atraso, si el encargado la cargo. */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> tarifaMoraDia() {
        return valor(CLAVE_MORA).flatMap(v -> {
            try {
                return Optional.<BigDecimal>of(new BigDecimal(v.trim()));
            } catch (NumberFormatException ex) {
                return Optional.<BigDecimal>empty();
            }
        });
    }

    @Transactional(readOnly = true)
    public int diasAnticipacion() {
        return valor(CLAVE_DIAS).map(v -> {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException ex) {
                return DIAS_POR_DEFECTO;
            }
        }).orElse(DIAS_POR_DEFECTO);
    }

    @Transactional(readOnly = true)
    public String plantillaEmail() {
        return valor(CLAVE_PLANTILLA).filter(v -> !v.isBlank()).orElse(PLANTILLA_POR_DEFECTO);
    }

    /**
     * Monto de mora para una orden: dias de atraso x tarifa fija si hay tarifa configurada;
     * si no, el porcentaje por dia sobre el total (comportamiento original).
     */
    @Transactional(readOnly = true)
    public BigDecimal montoMora(BigDecimal totalOrden, long diasAtraso) {
        if (diasAtraso <= 0) {
            return null;
        }
        Optional<BigDecimal> tarifa = tarifaMoraDia();
        if (tarifa.isPresent()) {
            return tarifa.get().multiply(BigDecimal.valueOf(diasAtraso)).setScale(2, RoundingMode.HALF_UP);
        }
        if (totalOrden == null) {
            return null;
        }
        return BigDecimal.valueOf(diasAtraso)
                .multiply(totalOrden)
                .multiply(BigDecimal.valueOf(moraPorcentajeDia))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ---------- Logotipo ----------

    @Transactional(readOnly = true)
    public String obtenerLogo() {
        return valor(CLAVE_LOGO).orElse(null);
    }

    @Transactional
    public void guardarLogo(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:image/")) {
            throw new ReglaNegocioException("El logotipo debe ser una imagen (data:image/...)");
        }
        if (dataUrl.length() > LOGO_MAX_CARACTERES) {
            throw new ReglaNegocioException("La imagen es demasiado grande; usa un logotipo mas liviano");
        }
        guardar(CLAVE_LOGO, dataUrl);
    }

    @Transactional
    public void borrarLogo() {
        guardar(CLAVE_LOGO, null);
    }

    // ---------- Helpers ----------

    private Optional<String> valor(String clave) {
        return repository.findByClave(clave).map(Configuracion::getValor);
    }

    private void guardar(String clave, String valor) {
        if (valor == null || valor.isBlank()) {
            repository.findByClave(clave).ifPresent(repository::delete);
            return;
        }
        Configuracion cfg = repository.findByClave(clave).orElseGet(() -> {
            Configuracion nueva = new Configuracion();
            nueva.setClave(clave);
            return nueva;
        });
        cfg.setValor(valor);
        repository.save(cfg);
    }
}
