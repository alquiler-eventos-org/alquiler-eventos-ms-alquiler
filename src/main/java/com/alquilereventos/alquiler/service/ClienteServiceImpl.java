package com.alquilereventos.alquiler.service;

import com.alquilereventos.alquiler.config.cache.CacheKeyFactory;
import com.alquilereventos.alquiler.exception.ReglaNegocioException;
import com.alquilereventos.alquiler.exception.RecursoNoEncontradoException;
import com.alquilereventos.alquiler.mapper.ClienteMapper;
import com.alquilereventos.common.repository.ClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica de negocio de clientes.
 *
 * <p>Cache (Redis, seccion {@code alquiler:clientes}, TTL 15 min): solo el
 * objeto Cliente individual por id. GET /{id} = cache-aside ({@code @Cacheable});
 * crear/actualizar = {@code @CachePut}; baja logica = {@code @CacheEvict}.
 * Las paginas y busquedas NO se guardan como coleccion: unicamente se
 * "calientan" sus elementos individuales via CacheManager programatico.</p>
 */
@Service
public class ClienteServiceImpl implements ClienteService {

    private static final Logger log = LoggerFactory.getLogger(ClienteServiceImpl.class);

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final CacheManager cacheManager;
    private final CacheKeyFactory cacheKeys;
    private final String clientesCacheName;

    @Autowired
    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              ClienteMapper clienteMapper,
                              CacheManager cacheManager,
                              CacheKeyFactory cacheKeys,
                              @Value("${app.cache.domains.clientes.name}") String clientesCacheName) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
        this.cacheManager = cacheManager;
        this.cacheKeys = cacheKeys;
        this.clientesCacheName = clientesCacheName;
    }

    /** Constructor para pruebas unitarias sin infraestructura Redis. */
    public ClienteServiceImpl(ClienteRepository clienteRepository, ClienteMapper clienteMapper) {
        this(clienteRepository, clienteMapper, null, null, null);
    }

    @Override
    @Transactional
    @CachePut(cacheNames = "${app.cache.domains.clientes.name}", key = "@cacheKeys.cliente(#result.id)")
    public com.alquilereventos.common.dto.Cliente crear(com.alquilereventos.common.dto.Cliente dto) {
        if (dto.getDocumento() != null && clienteRepository.findByDocumento(dto.getDocumento()).isPresent()) {
            throw new ReglaNegocioException("Ya existe un cliente con ese documento");
        }
        com.alquilereventos.common.entity.Cliente entity = clienteMapper.toEntity(dto);
        entity.setActivo(true);
        com.alquilereventos.common.entity.Cliente guardado = clienteRepository.save(entity);
        log.debug("Cliente creado con id {}", guardado.getId());
        return clienteMapper.toDto(guardado);
    }

    @Override
    @Transactional
    @CachePut(cacheNames = "${app.cache.domains.clientes.name}", key = "@cacheKeys.cliente(#id)")
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
        log.debug("Cliente {} actualizado", id);
        return clienteMapper.toDto(actualizado);
    }

    @Override
    @Cacheable(cacheNames = "${app.cache.domains.clientes.name}", key = "@cacheKeys.cliente(#id)")
    public com.alquilereventos.common.dto.Cliente obtenerPorId(Integer id) {
        log.debug("Buscando cliente id {} en la base de datos", id);
        com.alquilereventos.common.entity.Cliente entity = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id " + id));
        return clienteMapper.toDto(entity);
    }

    @Override
    public Page<com.alquilereventos.common.dto.Cliente> listar(Pageable pageable) {
        // Usa la query con filtros del repo: excluye los de baja logica (activo=false).
        // GET /{id} SI devuelve los inactivos, para evidenciar el borrado logico.
        Page<com.alquilereventos.common.dto.Cliente> pagina = clienteRepository.buscar(null, null, null, null, pageable)
                .map(clienteMapper::toDto);
        calentarClientes(pagina);
        return pagina;
    }

    @Override
    public Page<com.alquilereventos.common.dto.Cliente> buscar(String nombre, String apellido, String documento, String email, Pageable pageable) {
        Page<com.alquilereventos.common.dto.Cliente> pagina = clienteRepository.buscar(nombre, apellido, documento, email, pageable)
                .map(clienteMapper::toDto);
        calentarClientes(pagina);
        return pagina;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "${app.cache.domains.clientes.name}", key = "@cacheKeys.cliente(#id)")
    public void eliminar(Integer id) {
        com.alquilereventos.common.entity.Cliente entity = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id " + id));
        entity.setActivo(false);
        clienteRepository.save(entity);
        log.debug("Cliente {} marcado inactivo (borrado logico)", id);
    }

    /** No cachea la pagina: solamente sus objetos individuales ya procesados. */
    private void calentarClientes(Page<com.alquilereventos.common.dto.Cliente> pagina) {
        if (cacheManager == null || cacheKeys == null) {
            return;
        }
        Cache cache = cacheManager.getCache(clientesCacheName);
        if (cache != null) {
            pagina.getContent().forEach(cliente -> cache.put(cacheKeys.cliente(cliente.getId()), cliente));
        }
    }
}
