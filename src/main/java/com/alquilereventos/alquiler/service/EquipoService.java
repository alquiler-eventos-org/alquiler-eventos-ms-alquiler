package com.alquilereventos.alquiler.service;

import com.alquilereventos.alquiler.dto.EquipoResponse;
import com.alquilereventos.alquiler.exception.RecursoNoEncontradoException;
import com.alquilereventos.common.entity.Equipo;
import com.alquilereventos.common.repository.EquipoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de equipos (solo lectura). El stock lo modifican las ordenes de
 * alquiler al pasar a ENTREGA y DEVOLUCION; aca solo se expone.
 */
@Service
public class EquipoService {

    private final EquipoRepository equipoRepository;

    public EquipoService(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    @Transactional(readOnly = true)
    public Page<EquipoResponse> listar(Pageable pageable) {
        return equipoRepository.findAll(pageable).map(this::aRespuesta);
    }

    @Transactional(readOnly = true)
    public EquipoResponse obtener(Integer id) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado con id " + id));
        return aRespuesta(equipo);
    }

    private EquipoResponse aRespuesta(Equipo equipo) {
        Integer categoriaId = equipo.getCategoria() != null ? equipo.getCategoria().getId() : null;
        String categoria = equipo.getCategoria() != null ? equipo.getCategoria().getNombre() : null;
        String estado = equipo.getEstado() != null ? equipo.getEstado().name() : null;
        return new EquipoResponse(equipo.getId(), equipo.getNombre(), categoriaId, categoria,
                equipo.getStock(), equipo.getPrecioDia(), estado);
    }
}
