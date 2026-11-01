package com.alquilereventos.alquiler.service;

import com.alquilereventos.alquiler.dto.CategoriaRequest;
import com.alquilereventos.alquiler.dto.CategoriaResponse;
import com.alquilereventos.alquiler.exception.RecursoNoEncontradoException;
import com.alquilereventos.alquiler.exception.ReglaNegocioException;
import com.alquilereventos.common.entity.CategoriaEquipo;
import com.alquilereventos.common.repository.CategoriaEquipoRepository;
import com.alquilereventos.common.repository.EquipoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Categorias de equipos (Sonido, Mesas, Sillas, Carpas...): alta, edicion y baja. */
@Service
public class CategoriaService {

    private final CategoriaEquipoRepository categoriaRepository;
    private final EquipoRepository equipoRepository;

    public CategoriaService(CategoriaEquipoRepository categoriaRepository, EquipoRepository equipoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.equipoRepository = equipoRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll(Sort.by("nombre")).stream().map(this::aRespuesta).toList();
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = request.nombre().trim();
        validarNombreLibre(nombre, null);
        CategoriaEquipo categoria = new CategoriaEquipo();
        categoria.setNombre(nombre);
        return aRespuesta(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Integer id, CategoriaRequest request) {
        CategoriaEquipo categoria = buscar(id);
        String nombre = request.nombre().trim();
        validarNombreLibre(nombre, id);
        categoria.setNombre(nombre);
        return aRespuesta(categoriaRepository.save(categoria));
    }

    @Transactional
    public void eliminar(Integer id) {
        CategoriaEquipo categoria = buscar(id);
        int enUso = equipoRepository.findByCategoriaId(id).size();
        if (enUso > 0) {
            throw new ReglaNegocioException("No se puede eliminar la categoria '" + categoria.getNombre()
                    + "': tiene " + enUso + " equipo(s). Cambia primero la categoria de esos equipos.");
        }
        categoriaRepository.delete(categoria);
    }

    private CategoriaEquipo buscar(Integer id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria no encontrada con id " + id));
    }

    private void validarNombreLibre(String nombre, Integer idActual) {
        boolean repetida = categoriaRepository.findAll().stream()
                .anyMatch(c -> c.getNombre().equalsIgnoreCase(nombre) && !c.getId().equals(idActual));
        if (repetida) {
            throw new ReglaNegocioException("Ya existe una categoria llamada '" + nombre + "'");
        }
    }

    private CategoriaResponse aRespuesta(CategoriaEquipo categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre());
    }
}
