package com.alquilereventos.alquiler.service;

import com.alquilereventos.common.dto.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClienteService {

    Cliente crear(Cliente dto);

    Cliente actualizar(Integer id, Cliente dto);

    Cliente obtenerPorId(Integer id);

    Page<Cliente> listar(Pageable pageable);

    Page<Cliente> buscar(String nombre, String apellido, String documento, String email, Pageable pageable);

    void eliminar(Integer id);
}