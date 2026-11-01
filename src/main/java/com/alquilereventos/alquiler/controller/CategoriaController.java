package com.alquilereventos.alquiler.controller;

import com.alquilereventos.alquiler.dto.CategoriaRequest;
import com.alquilereventos.alquiler.dto.CategoriaResponse;
import com.alquilereventos.alquiler.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Categorias de equipo. Lectura: ADMIN, ENCARGADO_DEPOSITO y ENCARGADO_COMPRAS (para el
 * select del formulario de equipos). Alta, edicion y baja: ADMIN y ENCARGADO_COMPRAS.
 */
@RestController
@RequestMapping("/categorias")
@PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_DEPOSITO', 'ENCARGADO_COMPRAS')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Categorias", description = "Categorias de equipos (configurables)")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @Operation(summary = "Lista las categorias de equipo (id y nombre)")
    public ResponseEntity<List<CategoriaResponse>> listar() {
        return ResponseEntity.ok(categoriaService.listar());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_COMPRAS')")
    @Operation(summary = "Crea una categoria")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_COMPRAS')")
    @Operation(summary = "Renombra una categoria")
    public ResponseEntity<CategoriaResponse> actualizar(@PathVariable Integer id,
                                                        @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(categoriaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_COMPRAS')")
    @Operation(summary = "Elimina una categoria (solo si no tiene equipos)")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
