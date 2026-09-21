package com.alquilereventos.alquiler.service;

import com.alquilereventos.alquiler.exception.RecursoNoEncontradoException;
import com.alquilereventos.alquiler.exception.ReglaNegocioException;
import com.alquilereventos.alquiler.mapper.OrdenAlquilerMapper;
import com.alquilereventos.common.dto.OrdenAlquiler;
import com.alquilereventos.common.dto.OrdenAlquilerCrearRequest;
import com.alquilereventos.common.dto.OrdenAlquilerDetalle;
import com.alquilereventos.common.dto.OrdenAlquilerEstadoRequest;
import com.alquilereventos.common.entity.Cliente;
import com.alquilereventos.common.entity.Equipo;
import com.alquilereventos.common.entity.OrdenAlquilerHistorial;
import com.alquilereventos.common.entity.Usuario;
import com.alquilereventos.common.entity.enums.EstadoEquipo;
import com.alquilereventos.common.entity.enums.EstadoOrdenAlquiler;
import com.alquilereventos.common.repository.ClienteRepository;
import com.alquilereventos.common.repository.EquipoRepository;
import com.alquilereventos.common.repository.OrdenAlquilerHistorialRepository;
import com.alquilereventos.common.repository.OrdenAlquilerRepository;
import com.alquilereventos.common.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrdenAlquilerServiceImpl implements OrdenAlquilerService {

    private static final Logger log = LoggerFactory.getLogger(OrdenAlquilerServiceImpl.class);

    private final OrdenAlquilerRepository ordenAlquilerRepository;
    private final OrdenAlquilerHistorialRepository historialRepository;
    private final EquipoRepository equipoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrdenAlquilerMapper ordenAlquilerMapper;

    /**
     * Porcentaje del total aplicado por cada dia de atraso (configurable via MORA_PORCENTAJE_DIA).
     */
    @Value("${app.mora.porcentaje-dia:0.05}")
    private double moraPorcentajeDia;

    @Autowired
    public OrdenAlquilerServiceImpl(OrdenAlquilerRepository ordenAlquilerRepository,
                                    OrdenAlquilerHistorialRepository historialRepository,
                                    EquipoRepository equipoRepository,
                                    ClienteRepository clienteRepository,
                                    UsuarioRepository usuarioRepository,
                                    OrdenAlquilerMapper ordenAlquilerMapper) {
        this.ordenAlquilerRepository = ordenAlquilerRepository;
        this.historialRepository = historialRepository;
        this.equipoRepository = equipoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.ordenAlquilerMapper = ordenAlquilerMapper;
    }

    @Override
    @Transactional
    public OrdenAlquiler crear(OrdenAlquilerCrearRequest request) {
        validarRequest(request);
        Cliente cliente = obtenerClienteActivo(request.getClienteId());
        Usuario usuario = obtenerUsuario(request.getUsuarioId());
        int dias = calcularDias(request.getFechaEvento(), request.getFechaDevolucion());

        com.alquilereventos.common.entity.OrdenAlquiler orden =
                new com.alquilereventos.common.entity.OrdenAlquiler();
        orden.setCliente(cliente);
        orden.setUsuario(usuario);
        orden.setFechaEvento(request.getFechaEvento());
        orden.setFechaDevolucion(request.getFechaDevolucion());
        orden.setEstado(EstadoOrdenAlquiler.RESERVA);

        BigDecimal total = armarDetalles(request.getDetalles(), orden, dias);
        orden.setTotal(total);

        com.alquilereventos.common.entity.OrdenAlquiler guardada = ordenAlquilerRepository.save(orden);
        registrarHistorial(guardada, "CREACION", EstadoOrdenAlquiler.RESERVA.name(), usuario, null);
        log.info("Orden de alquiler creada: id={}, clienteId={}, dias={}, total={}",
                guardada.getId(), request.getClienteId(), dias, total);
        return ordenAlquilerMapper.toDto(guardada);
    }

    @Override
    @Transactional
    public OrdenAlquiler actualizar(Integer id, OrdenAlquilerCrearRequest request) {
        validarRequest(request);
        com.alquilereventos.common.entity.OrdenAlquiler orden = buscarOrden(id);
        if (orden.getEstado() != EstadoOrdenAlquiler.RESERVA) {
            throw new ReglaNegocioException("Solo pueden modificarse ordenes en estado RESERVA");
        }

        Cliente cliente = obtenerClienteActivo(request.getClienteId());
        Usuario usuario = obtenerUsuario(request.getUsuarioId());
        int dias = calcularDias(request.getFechaEvento(), request.getFechaDevolucion());

        orden.setCliente(cliente);
        orden.setUsuario(usuario);
        orden.setFechaEvento(request.getFechaEvento());
        orden.setFechaDevolucion(request.getFechaDevolucion());

        BigDecimal total = armarDetalles(request.getDetalles(), orden, dias);
        orden.setTotal(total);

        com.alquilereventos.common.entity.OrdenAlquiler actualizada = ordenAlquilerRepository.save(orden);
        log.info("Orden de alquiler actualizada: id={}, clienteId={}, dias={}, total={}",
                id, request.getClienteId(), dias, total);
        return ordenAlquilerMapper.toDto(actualizada);
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenAlquiler obtenerPorId(Integer id) {
        return ordenAlquilerMapper.toDto(buscarOrden(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrdenAlquiler> listar(Pageable pageable) {
        return ordenAlquilerRepository.findAll(pageable).map(ordenAlquilerMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrdenAlquiler> buscar(EstadoOrdenAlquiler estado,
                                      Integer clienteId,
                                      LocalDate fechaDesde,
                                      LocalDate fechaHasta,
                                      Pageable pageable) {
        return ordenAlquilerRepository.buscar(estado, clienteId, fechaDesde, fechaHasta, pageable)
                .map(ordenAlquilerMapper::toDto);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        com.alquilereventos.common.entity.OrdenAlquiler orden = buscarOrden(id);
        if (orden.getEstado() != EstadoOrdenAlquiler.RESERVA) {
            throw new ReglaNegocioException("Solo pueden anularse ordenes en estado RESERVA");
        }
        Usuario usuario = orden.getUsuario();
        orden.setEstado(EstadoOrdenAlquiler.ANULADA);
        ordenAlquilerRepository.save(orden);
        registrarHistorial(orden, EstadoOrdenAlquiler.RESERVA.name(), EstadoOrdenAlquiler.ANULADA.name(), usuario, null);
        log.info("Orden de alquiler anulada (borrado logico): id={}", id);
    }

    @Override
    @Transactional
    public OrdenAlquiler cambiarEstado(Integer id, OrdenAlquilerEstadoRequest request) {
        if (request.getEstado() == null) {
            throw new ReglaNegocioException("El estado es obligatorio");
        }
        com.alquilereventos.common.entity.OrdenAlquiler orden = buscarOrden(id);
        Usuario usuario = obtenerUsuario(request.getUsuarioId());
        EstadoOrdenAlquiler actual = orden.getEstado();
        EstadoOrdenAlquiler nuevo = EstadoOrdenAlquiler.valueOf(request.getEstado().name());

        validarTransicion(actual, nuevo);
        BigDecimal montoMora = null;

        if (nuevo == EstadoOrdenAlquiler.ENTREGA) {
            moverStock(orden, true);
        } else if (nuevo == EstadoOrdenAlquiler.MORA) {
            montoMora = calcularMora(orden);
            if (montoMora == null) {
                throw new ReglaNegocioException("No hay atraso: la orden aun no supero su fecha de devolucion");
            }
        } else if (nuevo == EstadoOrdenAlquiler.DEVOLUCION) {
            moverStock(orden, false);
            montoMora = calcularMora(orden);
        }

        orden.setEstado(nuevo);
        ordenAlquilerRepository.save(orden);
        registrarHistorial(orden, actual.name(), nuevo.name(), usuario, montoMora);
        log.info("Estado de orden {} cambiado: {} -> {} (mora={})", id, actual, nuevo, montoMora);
        return ordenAlquilerMapper.toDto(orden);
    }

    private BigDecimal armarDetalles(List<OrdenAlquilerDetalle> detallesDto,
                                     com.alquilereventos.common.entity.OrdenAlquiler orden,
                                     int dias) {
        List<com.alquilereventos.common.entity.OrdenAlquilerDetalle> detalles = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (OrdenAlquilerDetalle detalleDto : detallesDto) {
            if (detalleDto.getEquipoId() == null) {
                throw new ReglaNegocioException("Cada detalle debe indicar el equipoId");
            }
            if (detalleDto.getCantidad() == null || detalleDto.getCantidad() <= 0) {
                throw new ReglaNegocioException("La cantidad de cada detalle debe ser mayor a cero");
            }
            Equipo equipo = obtenerEquipo(detalleDto.getEquipoId());
            validarStock(equipo, detalleDto.getCantidad());

            BigDecimal subtotal = BigDecimal.valueOf(detalleDto.getCantidad())
                    .multiply(equipo.getPrecioDia())
                    .multiply(BigDecimal.valueOf(dias))
                    .setScale(2, RoundingMode.HALF_UP);
            detalles.add(ordenAlquilerMapper.toDetalleEntity(detalleDto, equipo, orden, subtotal));
            total = total.add(subtotal);
        }
        if (orden.getDetalles() == null) {
            orden.setDetalles(detalles);
        } else {
            orden.getDetalles().clear();
            orden.getDetalles().addAll(detalles);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private void validarRequest(OrdenAlquilerCrearRequest request) {
        if (request == null || request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new ReglaNegocioException("La orden debe incluir al menos un detalle");
        }
        if (request.getFechaEvento() == null) {
            throw new ReglaNegocioException("La fecha del evento es obligatoria");
        }
        if (request.getFechaDevolucion() != null && request.getFechaDevolucion().isBefore(request.getFechaEvento())) {
            throw new ReglaNegocioException("La fecha de devolucion no puede ser anterior a la fecha del evento");
        }
    }

    private Cliente obtenerClienteActivo(Integer clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id " + clienteId));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException("El cliente esta inactivo y no puede utilizarse en ordenes nuevas");
        }
        return cliente;
    }

    private Equipo obtenerEquipo(Integer equipoId) {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado con id " + equipoId));
        if (equipo.getEstado() != EstadoEquipo.DISPONIBLE) {
            throw new ReglaNegocioException("El equipo '" + equipo.getNombre() + "' no esta disponible para alquiler");
        }
        return equipo;
    }

    private void validarStock(Equipo equipo, int cantidad) {
        if (equipo.getStock() < cantidad) {
            throw new ReglaNegocioException("Stock insuficiente del equipo '" + equipo.getNombre()
                    + "': disponible " + equipo.getStock() + ", solicitado " + cantidad);
        }
    }

    private Usuario obtenerUsuario(Integer usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id " + usuarioId));
    }

    private com.alquilereventos.common.entity.OrdenAlquiler buscarOrden(Integer id) {
        return ordenAlquilerRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Orden de alquiler no encontrada con id " + id));
    }

    private int calcularDias(LocalDate fechaEvento, LocalDate fechaDevolucion) {
        if (fechaEvento == null || fechaDevolucion == null) {
            return 1;
        }
        long dias = ChronoUnit.DAYS.between(fechaEvento, fechaDevolucion) + 1;
        return dias < 1 ? 1 : (int) dias;
    }

    private void validarTransicion(EstadoOrdenAlquiler actual, EstadoOrdenAlquiler nuevo) {
        boolean valida;
        switch (actual) {
            case RESERVA -> valida = nuevo == EstadoOrdenAlquiler.ENTREGA || nuevo == EstadoOrdenAlquiler.ANULADA;
            case ENTREGA -> valida = nuevo == EstadoOrdenAlquiler.DEVOLUCION || nuevo == EstadoOrdenAlquiler.MORA;
            case MORA -> valida = nuevo == EstadoOrdenAlquiler.DEVOLUCION;
            default -> valida = false;
        }
        if (!valida) {
            throw new ReglaNegocioException("Transicion de estado invalida: " + actual + " -> " + nuevo);
        }
    }

    /**
     * Mueve el stock de los equipos de la orden.
     * egresar = true  -> ENTREGA: descuenta stock.
     * egresar = false -> DEVOLUCION: devuelve stock.
     */
    private void moverStock(com.alquilereventos.common.entity.OrdenAlquiler orden, boolean egresar) {
        for (com.alquilereventos.common.entity.OrdenAlquilerDetalle detalle : orden.getDetalles()) {
            Equipo equipo = detalle.getEquipo();
            if (equipo == null) {
                continue;
            }
            if (egresar) {
                validarStock(equipo, detalle.getCantidad());
                equipo.setStock(equipo.getStock() - detalle.getCantidad());
            } else {
                equipo.setStock(equipo.getStock() + detalle.getCantidad());
            }
            equipoRepository.save(equipo);
            log.debug("Stock del equipo {} actualizado a {} ", equipo.getId(), equipo.getStock());
        }
    }

    private BigDecimal calcularMora(com.alquilereventos.common.entity.OrdenAlquiler orden) {
        if (orden.getFechaDevolucion() == null || orden.getTotal() == null) {
            return null;
        }
        long diasAtraso = ChronoUnit.DAYS.between(orden.getFechaDevolucion(), LocalDate.now());
        if (diasAtraso <= 0) {
            return null;
        }
        return BigDecimal.valueOf(diasAtraso)
                .multiply(orden.getTotal())
                .multiply(BigDecimal.valueOf(moraPorcentajeDia))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void registrarHistorial(com.alquilereventos.common.entity.OrdenAlquiler orden,
                                    String estadoAnterior,
                                    String estadoNuevo,
                                    Usuario usuario,
                                    BigDecimal montoMora) {
        OrdenAlquilerHistorial historial = new OrdenAlquilerHistorial();
        historial.setOrdenAlquiler(orden);
        historial.setUsuario(usuario);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setMontoMora(montoMora);
        historialRepository.save(historial);
        log.debug("Historial registrado para orden {}: {} -> {} (mora={})",
                orden.getId(), estadoAnterior, estadoNuevo, montoMora);
    }
}