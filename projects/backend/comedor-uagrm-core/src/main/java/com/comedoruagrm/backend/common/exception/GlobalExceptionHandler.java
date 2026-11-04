package com.comedoruagrm.backend.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.comedoruagrm.backend.common.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Los datos enviados no son válidos");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("ERR_SYS_01", message));
    }

    // * Excepciones del propio framework: sin estos handlers caerian en el
    // * catch-all y una ruta inexistente o un JSON mal formado darian 500.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleRutaInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("ERR_SYS_03", "El recurso solicitado no existe"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMetodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("ERR_SYS_04", "Método HTTP no permitido para este recurso"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Object>> handleRequestMalformado(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("ERR_SYS_05", "La solicitud está mal formada o tiene parámetros inválidos"));
    }

    // * La falta de rol (@PreAuthorize) NO se responde acá: se vuelve a lanzar
    // * para que suba al filtro de seguridad, que la entrega a
    // * ApiAccessDeniedHandler (403 ERR_AUTH_07 en el sobre). Sin este handler
    // * el catch-all de abajo la convertiria en un 500. Va acá y no se arma el
    // * 403 a mano porque `common/` no puede importar de `security/`.
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccesoDenegado(AccessDeniedException ex) {
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpectedException(Exception ex) {
        LOG.error("Error interno no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("ERR_SYS_00", "Ocurrió un error inesperado"));
    }
}
