package com.alquilereventos.alquiler.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de equipos. Lectura para ADMIN, ENCARGADO_DEPOSITO y ENCARGADO_COMPRAS
 * (compras necesita elegir equipos al armar una orden de compra).
 */
@RestController
@RequestMapping("/equipos")
@PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_DEPOSITO', 'ENCARGADO_COMPRAS')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Equipos", description = "Consulta de equipos y su stock (solo lectura)")
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
}
