package com.alquilereventos.alquiler.controller;

import com.alquilereventos.alquiler.dto.MoraResponse;
import com.alquilereventos.alquiler.service.MoraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ordenes-alquiler")
@PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_DEPOSITO')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "OrdenesAlquiler", description = "Mora de una orden de alquiler")
public class MoraController {

    private final MoraService moraService;

    public MoraController(MoraService moraService) {
        this.moraService = moraService;
    }

    @GetMapping("/{id}/mora")
    @Operation(summary = "Mora de la orden: dias de atraso, tarifa por dia aplicada y monto")
    public ResponseEntity<MoraResponse> mora(@PathVariable Integer id) {
        return ResponseEntity.ok(moraService.consultar(id));
    }
}
