package com.alquilereventos.alquiler.service;

import com.alquilereventos.alquiler.dto.CategoriaResponse;
import com.alquilereventos.alquiler.dto.EquipoRequest;
import com.alquilereventos.alquiler.dto.EquipoResponse;
import com.alquilereventos.alquiler.exception.RecursoNoEncontradoException;
import com.alquilereventos.alquiler.exception.ReglaNegocioException;
import com.alquilereventos.common.entity.CategoriaEquipo;
import com.alquilereventos.common.entity.Equipo;
import com.alquilereventos.common.entity.enums.EstadoEquipo;
import com.alquilereventos.common.repository.CategoriaEquipoRepository;
import com.alquilereventos.common.repository.EquipoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Equipos: consulta, alta, edicion y baja logica (estado BAJA). Ademas, el stock
 * lo modifican las ordenes de alquiler al pasar a ENTREGA y DEVOLUCION.
 */
@Service
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final CategoriaEquipoRepository categoriaRepository;

    public EquipoService(EquipoRepository equipoRepository, CategoriaEquipoRepository categoriaRepository) {
        this.equipoRepository = equipoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public Page<EquipoResponse> listar(Pageable pageable) {
        return equipoRepository.findAll(pageable).map(this::aRespuesta);
    }

    @Transactional(readOnly = true)
    public EquipoResponse obtener(Integer id) {
        return aRespuesta(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> categorias() {
        return categoriaRepository.findAll(Sort.by("nombre")).stream()
                .map(c -> new CategoriaResponse(c.getId(), c.getNombre()))
                .toList();
    }

    @Transactional
    public EquipoResponse crear(EquipoRequest request) {
        Equipo equipo = new Equipo();
        aplicar(equipo, request);
        return aRespuesta(equipoRepository.save(equipo));
    }

    @Transactional
    public EquipoResponse actualizar(Integer id, EquipoRequest request) {
        Equipo equipo = buscar(id);
        aplicar(equipo, request);
        return aRespuesta(equipoRepository.save(equipo));
    }

    /** Baja logica: el equipo queda en estado BAJA y deja de poder alquilarse. */
    @Transactional
    public void darDeBaja(Integer id) {
        Equipo equipo = buscar(id);
        equipo.setEstado(EstadoEquipo.BAJA);
        equipoRepository.save(equipo);
    }

    private Equipo buscar(Integer id) {
        return equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado con id " + id));
    }

    private void aplicar(Equipo equipo, EquipoRequest request) {
        CategoriaEquipo categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoria no encontrada con id " + request.categoriaId()));
        equipo.setNombre(request.nombre().trim());
        equipo.setCategoria(categoria);
        equipo.setStock(request.stock());
        equipo.setPrecioDia(request.precioDia());
        equipo.setEstado(estado(request.estado()));
    }

    private EstadoEquipo estado(String valor) {
        if (valor == null || valor.isBlank()) {
            return EstadoEquipo.DISPONIBLE;
        }
        try {
            return EstadoEquipo.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ReglaNegocioException("Estado de equipo invalido: " + valor
                    + " (use DISPONIBLE, MANTENIMIENTO o BAJA)");
        }
    }

    private EquipoResponse aRespuesta(Equipo equipo) {
        Integer categoriaId = equipo.getCategoria() != null ? equipo.getCategoria().getId() : null;
        String categoria = equipo.getCategoria() != null ? equipo.getCategoria().getNombre() : null;
        String estado = equipo.getEstado() != null ? equipo.getEstado().name() : null;
        return new EquipoResponse(equipo.getId(), equipo.getNombre(), categoriaId, categoria,
                equipo.getStock(), equipo.getPrecioDia(), estado);
    }
}
