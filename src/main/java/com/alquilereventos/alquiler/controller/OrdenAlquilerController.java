package com.alquilereventos.alquiler.controller;

import com.alquilereventos.alquiler.service.OrdenAlquilerService;
import com.alquilereventos.common.dto.EstadoOrdenAlquiler;
import com.alquilereventos.common.dto.OrdenAlquiler;
import com.alquilereventos.common.dto.OrdenAlquilerCrearRequest;
import com.alquilereventos.common.dto.OrdenAlquilerEstadoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/ordenes-alquiler")
@Tag(name = "Ordenes-Alquiler", description = "Gestion de ordenes de alquiler de equipos: CRUD, filtros y cambio de estado")
public class OrdenAlquilerController {

    private final OrdenAlquilerService ordenAlquilerService;

    @Autowired
    public OrdenAlquilerController(OrdenAlquilerService ordenAlquilerService) {
        this.ordenAlquilerService = ordenAlquilerService;
    }

    @PostMapping
    @Operation(summary = "Crea una orden de alquiler",
            description = "Valida cliente, equipos y stock; calcula subtotales (cantidad x precioDia x dias) y el total; registra el historial inicial en estado RESERVA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Orden de alquiler creada", content = @Content(schema = @Schema(implementation = OrdenAlquiler.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o regla de negocio violada", content = @Content(schema = @Schema(implementation = com.alquilereventos.common.dto.ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Cliente, usuario o equipo no encontrado", content = @Content(schema = @Schema(implementation = com.alquilereventos.common.dto.ApiError.class)))
    })
    public ResponseEntity<OrdenAlquiler> crear(@RequestBody OrdenAlquilerCrearRequest request) {
        OrdenAlquiler creada = ordenAlquilerService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una orden de alquiler",
            description = "Solo permite modificar ordenes en estado RESERVA. Recalcula subtotales y total, y reemplaza los detalles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden de alquiler actualizada", content = @Content(schema = @Schema(implementation = OrdenAlquiler.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o la orden no esta en RESERVA", content = @Content(schema = @Schema(implementation = com.alquilereventos.common.dto.ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Orden, cliente, usuario o equipo no encontrado", content = @Content(schema = @Schema(implementation = com.alquilereventos.common.dto.ApiError.class)))
    })
    public ResponseEntity<OrdenAlquiler> actualizar(@PathVariable Integer id,
                                                    @RequestBody OrdenAlquilerCrearRequest request) {
        OrdenAlquiler actualizada = ordenAlquilerService.actualizar(id, request);
        return ResponseEntity.ok(actualizada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una orden de alquiler por id", description = "Incluye los detalles de la orden")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden de alquiler encontrada", content = @Content(schema = @Schema(implementation = OrdenAlquiler.class))),
            @ApiResponse(responseCode = "404", description = "Orden de alquiler no encontrada", content = @Content(schema = @Schema(implementation = com.alquilereventos.common.dto.ApiError.class)))
    })
    public ResponseEntity<OrdenAlquiler> obtenerPorId(@PathVariable Integer id) {
        OrdenAlquiler orden = ordenAlquilerService.obtenerPorId(id);
        return ResponseEntity.ok(orden);
    }

    @GetMapping
    @Operation(summary = "Lista ordenes de alquiler paginadas", description = "Devuelve una pagina de ordenes. Paginacion obligatoria (page, size)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagina de ordenes de alquiler", content = @Content(schema = @Schema(implementation = Page.class)))
    })
    public ResponseEntity<Page<OrdenAlquiler>> listar(Pageable pageable) {
        Page<OrdenAlquiler> pagina = ordenAlquilerService.listar(pageable);
        return ResponseEntity.ok(pagina);
    }

    @GetMapping("/buscar")
    @Operation(summary = "Busca ordenes de alquiler con filtros y paginacion",
            description = "Filtros opcionales: estado, clienteId, fechaDesde, fechaHasta")
    @Parameter(name = "estado", description = "Filtro por estado de la orden (RESERVA, ENTREGA, DEVOLUCION, MORA, ANULADA)", in = ParameterIn.QUERY)
    @Parameter(name = "clienteId", description = "Filtro por cliente", in = ParameterIn.QUERY)
    @Parameter(name = "fechaDesde", description = "Filtro por fecha del evento desde (yyyy-MM-dd)", in = ParameterIn.QUERY)
    @Parameter(name = "fechaHasta", description = "Filtro por fecha del evento hasta (yyyy-MM-dd)", in = ParameterIn.QUERY)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagina de ordenes que cumplen los filtros", content = @Content(schema = @Schema(implementation = Page.class)))
    })
    public ResponseEntity<Page<OrdenAlquiler>> buscar(
            @RequestParam(required = false) EstadoOrdenAlquiler estado,
            @RequestParam(required = false) Integer clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable pageable) {
        com.alquilereventos.common.entity.enums.EstadoOrdenAlquiler estadoEntity =
                estado != null ? com.alquilereventos.common.entity.enums.EstadoOrdenAlquiler.valueOf(estado.name()) : null;
        Page<OrdenAlquiler> pagina = ordenAlquilerService.buscar(estadoEntity, clienteId, fechaDesde, fechaHasta, pageable);
        return ResponseEntity.ok(pagina);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Anula una orden de alquiler (borrado logico)",
            description = "Cambia el estado a ANULADA. Solo permitido si la orden esta en RESERVA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden de alquiler anulada"),
            @ApiResponse(responseCode = "400", description = "La orden no esta en RESERVA", content = @Content(schema = @Schema(implementation = com.alquilereventos.common.dto.ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Orden de alquiler no encontrada", content = @Content(schema = @Schema(implementation = com.alquilereventos.common.dto.ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        ordenAlquilerService.eliminar(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambia el estado de una orden de alquiler",
            description = "Transiciones validas: RESERVA->ENTREGA|ANULADA, ENTREGA->DEVOLUCION|MORA, MORA->DEVOLUCION. " +
                    "ENTREGA descuenta stock, DEVOLUCION lo devuelve, MORA calcula el monto de mora. Cada cambio queda en el historial")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado", content = @Content(schema = @Schema(implementation = OrdenAlquiler.class))),
            @ApiResponse(responseCode = "400", description = "Transicion invalida o regla de negocio violada", content = @Content(schema = @Schema(implementation = com.alquilereventos.common.dto.ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Orden de alquiler o usuario no encontrado", content = @Content(schema = @Schema(implementation = com.alquilereventos.common.dto.ApiError.class)))
    })
    public ResponseEntity<OrdenAlquiler> cambiarEstado(@PathVariable Integer id,
                                                       @RequestBody OrdenAlquilerEstadoRequest request) {
        OrdenAlquiler actualizada = ordenAlquilerService.cambiarEstado(id, request);
        return ResponseEntity.ok(actualizada);
    }
}