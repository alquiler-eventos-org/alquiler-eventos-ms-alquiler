package com.alquilereventos.alquiler.mapper;

import org.springframework.stereotype.Component;

@Component
public class ClienteMapper implements Mapper<com.alquilereventos.common.entity.Cliente, com.alquilereventos.common.dto.Cliente> {

    @Override
    public com.alquilereventos.common.dto.Cliente toDto(com.alquilereventos.common.entity.Cliente entity) {
        com.alquilereventos.common.dto.Cliente dto =
                new com.alquilereventos.common.dto.Cliente(entity.getId(), entity.getActivo());
        dto.setNombre(entity.getNombre());
        dto.setApellido(entity.getApellido());
        dto.setTelefono(entity.getTelefono());
        dto.setEmail(entity.getEmail());
        dto.setDocumento(entity.getDocumento());
        dto.setDireccion(entity.getDireccion());
        dto.setNotas(entity.getNotas());
        return dto;
    }

    @Override
    public com.alquilereventos.common.entity.Cliente toEntity(com.alquilereventos.common.dto.Cliente dto) {
        com.alquilereventos.common.entity.Cliente entity = new com.alquilereventos.common.entity.Cliente();
        entity.setNombre(dto.getNombre());
        entity.setApellido(dto.getApellido());
        entity.setTelefono(dto.getTelefono());
        entity.setEmail(dto.getEmail());
        entity.setDocumento(dto.getDocumento());
        entity.setDireccion(dto.getDireccion());
        entity.setNotas(dto.getNotas());
        return entity;
    }
}