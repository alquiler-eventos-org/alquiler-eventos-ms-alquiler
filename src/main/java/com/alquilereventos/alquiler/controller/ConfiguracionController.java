package com.alquilereventos.alquiler.controller;

import com.alquilereventos.alquiler.dto.ConfiguracionAlquilerDto;
import com.alquilereventos.alquiler.dto.LogoDto;
import com.alquilereventos.alquiler.dto.RecordatorioResultado;
import com.alquilereventos.alquiler.service.ConfiguracionService;
import com.alquilereventos.alquiler.service.RecordatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Configuracion del sistema, separada por rol:
 * - Alquiler (tarifa de mora, dias de anticipacion, plantilla de mail): ADMIN y ENCARGADO_DEPOSITO.
 * - Logotipo: lo ve cualquier rol del sistema, solo ADMIN lo cambia.
 * Las categorias de equipos se administran en /categorias (ADMIN y ENCARGADO_COMPRAS).
 */
@RestController
@RequestMapping("/configuracion")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Configuracion", description = "Parametros del sistema por rol")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;
    private final RecordatorioService recordatorioService;

    public ConfiguracionController(ConfiguracionService configuracionService,
                                   RecordatorioService recordatorioService) {
        this.configuracionService = configuracionService;
        this.recordatorioService = recordatorioService;
    }

    @GetMapping("/alquiler")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_DEPOSITO')")
    @Operation(summary = "Parametros de alquiler: tarifa de mora, dias de anticipacion y plantilla de mail")
    public ResponseEntity<ConfiguracionAlquilerDto> obtenerAlquiler() {
        return ResponseEntity.ok(configuracionService.obtenerAlquiler());
    }

    @PutMapping("/alquiler")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_DEPOSITO')")
    @Operation(summary = "Guarda los parametros de alquiler (un valor null vuelve al valor por defecto)")
    public ResponseEntity<ConfiguracionAlquilerDto> guardarAlquiler(@Valid @RequestBody ConfiguracionAlquilerDto dto) {
        return ResponseEntity.ok(configuracionService.guardarAlquiler(dto));
    }

    @GetMapping("/logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_DEPOSITO', 'ENCARGADO_COMPRAS')")
    @Operation(summary = "Logotipo del sistema (data URL) o null si no hay")
    public ResponseEntity<LogoDto> obtenerLogo() {
        return ResponseEntity.ok(new LogoDto(configuracionService.obtenerLogo()));
    }

    @PutMapping("/logo")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambia el logotipo del sistema (solo ADMIN)")
    public ResponseEntity<LogoDto> guardarLogo(@RequestBody LogoDto dto) {
        configuracionService.guardarLogo(dto.logo());
        return ResponseEntity.ok(new LogoDto(configuracionService.obtenerLogo()));
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Quita el logotipo del sistema (solo ADMIN)")
    public ResponseEntity<Void> borrarLogo() {
        configuracionService.borrarLogo();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recordatorios/ejecutar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_DEPOSITO')")
    @Operation(summary = "Ejecuta ahora el recordatorio de devolucion (el mismo que corre el cron diario)")
    public ResponseEntity<RecordatorioResultado> ejecutarRecordatorios() {
        return ResponseEntity.ok(recordatorioService.enviarRecordatorios(LocalDate.now()));
    }
}
