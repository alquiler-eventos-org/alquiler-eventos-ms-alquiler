package com.alquilereventos.alquiler.exception;

import com.alquilereventos.common.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        log.info("Recurso no encontrado: {}", ex.getMessage());
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> handleReglaNegocio(ReglaNegocioException ex, HttpServletRequest request) {
        log.info("Regla de negocio violada: {}", ex.getMessage());
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String campo = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Datos de entrada invalidos");
        log.info("Validacion fallida: {}", campo);
        return construir(HttpStatus.BAD_REQUEST, campo, request);
    }

    // Un cuerpo mal formado (JSON roto) es un error del CLIENTE (400), no del
    // servidor (500). Sin este handler, cae en el generico como 500.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleJsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.info("Cuerpo de solicitud ilegible (JSON invalido): {}", ex.getMostSpecificCause().getMessage());
        return construir(HttpStatus.BAD_REQUEST, "Cuerpo de solicitud invalido (JSON mal formado)", request);
    }

    // Seguridad: nunca se filtra el detalle tecnico; siempre ApiError limpio.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleNoAutenticado(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Token sin identidad valida: {}", ex.getMessage());
        return construir(HttpStatus.UNAUTHORIZED, "Autenticacion requerida o token invalido", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado a {} {}", request.getMethod(), request.getRequestURI());
        return construir(HttpStatus.FORBIDDEN, "No posee permisos para este recurso", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenerica(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno. Intente nuevamente más tarde.", request);
    }

    private ResponseEntity<ApiError> construir(HttpStatus status, String mensaje, HttpServletRequest request) {
        ApiError error = new ApiError();
        error.setCodigo(status.value());
        error.setMensaje(mensaje);
        error.setRuta(request.getRequestURI());
        error.setFecha(OffsetDateTime.now());
        return ResponseEntity.status(status).body(error);
    }
}