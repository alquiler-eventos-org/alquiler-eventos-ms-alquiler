package com.alquilereventos.alquiler.mapper;

public interface Mapper<E, D> {

    D toDto(E entity);

    E toEntity(D dto);
}