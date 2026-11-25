package cl.pokestore.backend.common;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final Pattern INDEXED_FIELD = Pattern.compile("\\[(\\d+)]");

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Set<String> fields = new LinkedHashSet<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fields.add(INDEXED_FIELD.matcher(error.getField()).replaceAll(".$1")));
        String message = fields.isEmpty()
                ? "Datos inválidos"
                : "Datos inválidos: " + String.join(", ", fields);
        return ResponseEntity.unprocessableContent().body(new ApiError(message));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleInvalidRequest(Exception exception) {
        String field = exception instanceof MissingServletRequestParameterException missing
                ? missing.getParameterName()
                : exception instanceof MethodArgumentTypeMismatchException mismatch
                        ? mismatch.getName()
                        : null;
        String message = field == null
                ? "Datos inválidos"
                : "Datos inválidos: " + field;
        return ResponseEntity.unprocessableContent().body(new ApiError(message));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDataAccessException(DataAccessException exception) {
        LOGGER.error("Database operation failed", exception);
        return ResponseEntity.status(HttpStatusCode.valueOf(500))
                .body(new ApiError("No se pudo completar la operación de base de datos"));
    }
}
