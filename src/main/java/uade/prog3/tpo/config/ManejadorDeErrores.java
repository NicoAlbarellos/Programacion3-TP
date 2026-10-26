package uade.prog3.tpo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uade.prog3.tpo.algorithm.PendienteDeImplementar;

import java.util.Map;

/**
 * Traduce excepciones a respuestas HTTP con un cuerpo entendible.
 *
 * Lo importante para el trabajo: un algoritmo sin implementar devuelve
 * 501 Not Implemented con un mensaje claro, en vez de un stack trace de 500.
 * Asi la aplicacion arranca y es demostrable desde el primer dia.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

    @ExceptionHandler(PendienteDeImplementar.class)
    public ResponseEntity<Map<String, String>> pendiente(PendienteDeImplementar e) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("estado", "PENDIENTE", "detalle", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("estado", "ERROR", "detalle", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> estadoInvalido(IllegalStateException e) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("estado", "ERROR", "detalle", e.getMessage()));
    }

    /** Falta un parametro obligatorio, por ejemplo /dijkstra sin destino. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> parametroFaltante(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("estado", "ERROR", "detalle", "Falta el parametro: " + e.getParameterName()));
    }

    /** Un parametro con tipo incorrecto, por ejemplo capacidad=abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> tipoInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("estado", "ERROR", "detalle",
                        "Valor invalido para " + e.getName() + ": " + e.getValue()));
    }

    /**
     * Cualquier otro error (por ejemplo, Neo4j caido): 500 con mensaje, nunca stack trace.
     * Los errores HTTP propios de Spring (ruta inexistente 404, metodo no permitido 405)
     * conservan su codigo.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> errorInesperado(Exception e) {
        if (e instanceof ErrorResponse errorHttp) {
            return ResponseEntity.status(errorHttp.getStatusCode())
                    .body(Map.of("estado", "ERROR", "detalle", errorHttp.getBody().getTitle()));
        }
        log.error("Error inesperado", e);
        return ResponseEntity.internalServerError()
                .body(Map.of("estado", "ERROR", "detalle", "Error interno: " + e.getClass().getSimpleName()));
    }
}
