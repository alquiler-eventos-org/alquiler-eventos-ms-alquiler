package com.alquilereventos.alquiler.controller;

import com.alquilereventos.alquiler.service.ClienteService;
import com.alquilereventos.common.dto.ApiError;
import com.alquilereventos.common.dto.Cliente;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Gestion de clientes: CRUD con paginacion, filtros y borrado logico")
public class ClienteController {

    private final ClienteService clienteService;

    @Autowired
    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @Operation(summary = "Crea un cliente", description = "Registra un nuevo cliente en el sistema (activo por defecto)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente creado", content = @Content(schema = @Schema(implementation = Cliente.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (ej. documento repetido)", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Cliente> crear(@RequestBody Cliente dto) {
        Cliente creado = clienteService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un cliente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente actualizado", content = @Content(schema = @Schema(implementation = Cliente.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Cliente> actualizar(@PathVariable Integer id, @RequestBody Cliente dto) {
        Cliente actualizado = clienteService.actualizar(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un cliente por id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado", content = @Content(schema = @Schema(implementation = Cliente.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Cliente> obtenerPorId(@PathVariable Integer id) {
        Cliente cliente = clienteService.obtenerPorId(id);
        return ResponseEntity.ok(cliente);
    }

    @GetMapping
    @Operation(summary = "Lista clientes paginados", description = "Devuelve una pagina de clientes activos. Paginacion obligatoria (page, size)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagina de clientes", content = @Content(schema = @Schema(implementation = Page.class)))
    })
    public ResponseEntity<Page<Cliente>> listar(Pageable pageable) {
        Page<Cliente> pagina = clienteService.listar(pageable);
        return ResponseEntity.ok(pagina);
    }

    @GetMapping("/buscar")
    @Operation(summary = "Busca clientes con filtros y paginacion",
            description = "Filtros opcionales: nombre, apellido, documento, email")
    @Parameter(name = "nombre", description = "Filtro por nombre (coincidencia parcial)", in = ParameterIn.QUERY)
    @Parameter(name = "apellido", description = "Filtro por apellido (coincidencia parcial)", in = ParameterIn.QUERY)
    @Parameter(name = "documento", description = "Filtro por documento (coincidencia parcial)", in = ParameterIn.QUERY)
    @Parameter(name = "email", description = "Filtro por email (coincidencia parcial)", in = ParameterIn.QUERY)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagina de clientes que cumplen los filtros", content = @Content(schema = @Schema(implementation = Page.class)))
    })
    public ResponseEntity<Page<Cliente>> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String email,
            Pageable pageable) {
        Page<Cliente> pagina = clienteService.buscar(nombre, apellido, documento, email, pageable);
        return ResponseEntity.ok(pagina);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un cliente (borrado logico)", description = "Marca activo = false, no borra la fila")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente desactivado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        clienteService.eliminar(id);
        return ResponseEntity.ok().build();
    }
}