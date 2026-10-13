package com.utn.atenea.compartido.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ProblemDetail recursoDuplicado(RecursoDuplicadoException ex) {
        return problema(HttpStatus.CONFLICT, "Recurso duplicado", ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail reglaNegocio(ReglaNegocioException ex) {
        return problema(HttpStatus.CONFLICT, "Regla de negocio no cumplida", ex.getMessage());
    }

    // Red de seguridad: la base rechazó algo (UNIQUE, FK o CHECK) que el service no validó antes,
    // por ejemplo dos requests simultáneos que pasaron el mismo existsBy. El mensaje de la base no
    // se devuelve porque expone nombres de tablas y columnas.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridadDeDatos(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflicto con datos existentes",
                "La operación entra en conflicto con datos existentes.");
    }

    // Los 401 y 403 que genera Spring Security en sus filtros también llegan acá
    // (ver SecurityConfig), así que salen con el mismo formato.
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail noAutenticado(AuthenticationException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "No autenticado",
                "Credenciales inválidas o sesión vencida.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accesoDenegado(AccessDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "Acceso denegado",
                "No tenés permiso para realizar esta operación.");
    }

    // Cualquier otra cosa es un bug: se loguea completo y al cliente no se le muestra el detalle.
    @ExceptionHandler(Exception.class)
    public ProblemDetail errorInesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado.");
    }

    // @Valid falló: se agrega la lista de campos con su error.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorCampo> errores = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.add(new ErrorCampo(error.getField(), error.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errores.add(new ErrorCampo(null, error.getDefaultMessage())));

        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Datos inválidos",
                "El request tiene campos inválidos.");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
