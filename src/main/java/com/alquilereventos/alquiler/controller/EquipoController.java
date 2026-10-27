package com.alquilereventos.alquiler.controller;

import com.alquilereventos.alquiler.dto.EquipoRequest;
import com.alquilereventos.alquiler.dto.EquipoResponse;
import com.alquilereventos.alquiler.service.EquipoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

/**
 * Equipos. Lectura para ADMIN, ENCARGADO_DEPOSITO y ENCARGADO_COMPRAS
 * (compras necesita elegir equipos al armar una orden de compra).
 * Alta, edicion y baja: solo ADMIN y ENCARGADO_DEPOSITO.
 */
@RestController
@RequestMapping("/equipos")
@PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_DEPOSITO', 'ENCARGADO_COMPRAS')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Equipos", description = "Consulta, alta, edicion y baja de equipos")
public class EquipoController {

    private static final Logger log = LoggerFactory.getLogger(EquipoController.class);

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @GetMapping
    @Operation(summary = "Lista equipos paginados", description = "Incluye categoria, stock, precio por dia y estado")
    public ResponseEntity<Page<EquipoResponse>> listar(@ParameterObject Pageable pageable) {
        log.info("GET /equipos - listar (page={}, size={})", pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(equipoService.listar(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un equipo por id")
    public ResponseEntity<EquipoResponse> obtener(@PathVariable Integer id) {
        log.info("GET /equipos/{} - obtener equipo", id);
        return ResponseEntity.ok(equipoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_DEPOSITO')")
    @Operation(summary = "Crea un equipo")
    public ResponseEntity<EquipoResponse> crear(@Valid @RequestBody EquipoRequest request) {
        log.info("POST /equipos - crear equipo '{}'", request.nombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(equipoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_DEPOSITO')")
    @Operation(summary = "Actualiza un equipo")
    public ResponseEntity<EquipoResponse> actualizar(@PathVariable Integer id,
                                                     @Valid @RequestBody EquipoRequest request) {
        log.info("PUT /equipos/{} - actualizar equipo", id);
        return ResponseEntity.ok(equipoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_DEPOSITO')")
    @Operation(summary = "Da de baja un equipo (baja logica: estado BAJA)")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        log.info("DELETE /equipos/{} - baja logica", id);
        equipoService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}
