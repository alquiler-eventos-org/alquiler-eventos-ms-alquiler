package com.alquilereventos.alquiler.service;

import com.alquilereventos.alquiler.exception.ReglaNegocioException;
import com.alquilereventos.alquiler.exception.RecursoNoEncontradoException;
import com.alquilereventos.alquiler.mapper.ClienteMapper;
import com.alquilereventos.common.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    @Autowired
    public ClienteServiceImpl(ClienteRepository clienteRepository, ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }

    @Override
    @Transactional
    public com.alquilereventos.common.dto.Cliente crear(com.alquilereventos.common.dto.Cliente dto) {
        if (dto.getDocumento() != null && clienteRepository.findByDocumento(dto.getDocumento()).isPresent()) {
            throw new ReglaNegocioException("Ya existe un cliente con ese documento");
        }
        com.alquilereventos.common.entity.Cliente entity = clienteMapper.toEntity(dto);
        entity.setActivo(true);
        com.alquilereventos.common.entity.Cliente guardado = clienteRepository.save(entity);
        return clienteMapper.toDto(guardado);
    }

    @Override
    @Transactional
    public com.alquilereventos.common.dto.Cliente actualizar(Integer id, com.alquilereventos.common.dto.Cliente dto) {
        com.alquilereventos.common.entity.Cliente entity = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id " + id));

        entity.setNombre(dto.getNombre());
        entity.setApellido(dto.getApellido());
        entity.setTelefono(dto.getTelefono());
        entity.setEmail(dto.getEmail());
        entity.setDocumento(dto.getDocumento());
        entity.setDireccion(dto.getDireccion());
        entity.setNotas(dto.getNotas());

        com.alquilereventos.common.entity.Cliente actualizado = clienteRepository.save(entity);
        return clienteMapper.toDto(actualizado);
    }

    @Override
    public com.alquilereventos.common.dto.Cliente obtenerPorId(Integer id) {
        com.alquilereventos.common.entity.Cliente entity = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id " + id));
        return clienteMapper.toDto(entity);
    }

    @Override
    public Page<com.alquilereventos.common.dto.Cliente> listar(Pageable pageable) {
        return clienteRepository.findAll(pageable)
                .map(clienteMapper::toDto);
    }

    @Override
    public Page<com.alquilereventos.common.dto.Cliente> buscar(String nombre, String apellido, String documento, String email, Pageable pageable) {
        return clienteRepository.buscar(nombre, apellido, documento, email, pageable)
                .map(clienteMapper::toDto);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        com.alquilereventos.common.entity.Cliente entity = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id " + id));
        entity.setActivo(false);
        clienteRepository.save(entity);
    }
}