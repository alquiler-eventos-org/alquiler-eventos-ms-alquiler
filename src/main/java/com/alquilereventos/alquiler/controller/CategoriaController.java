package com.alquilereventos.alquiler.controller;

import com.alquilereventos.alquiler.dto.CategoriaResponse;
import com.alquilereventos.alquiler.service.EquipoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catalogo de categorias de equipo, para el select del formulario de equipos. */
@RestController
@RequestMapping("/categorias")
@PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_DEPOSITO', 'ENCARGADO_COMPRAS')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Equipos", description = "Consulta, alta, edicion y baja de equipos")
public class CategoriaController {

    private final EquipoService equipoService;

    public CategoriaController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @GetMapping
    @Operation(summary = "Lista las categorias de equipo (id y nombre)")
    public ResponseEntity<List<CategoriaResponse>> listar() {
        return ResponseEntity.ok(equipoService.categorias());
    }
}
