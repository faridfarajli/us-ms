package az.ingress.common.exception;

import az.ingress.common.exception.exceptions.UnAuthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends DefaultErrorAttributes {

    @SneakyThrows
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handle(MethodArgumentNotValidException exception,
                                                      WebRequest request) {
        log.info("Method argument not valid exception occurred");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("message", "you have not sent some properties correctly");

        List<ConstraintsViolationError> validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ConstraintsViolationError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());
        attributes.put("fieldErrors", validationErrors);
        return ofType(request, HttpStatus.BAD_REQUEST,
                "You are missing some fields", validationErrors);
    }

    @SneakyThrows
    @ExceptionHandler(UnAuthorizedException.class)
    public ResponseEntity<Map<String, Object>> handle(UnAuthorizedException exception,
                                                      WebRequest request) {
        log.trace("Unauthorized exception occurred ",exception);
        return ofType(request, HttpStatus.UNAUTHORIZED,
                "Access is denied", null);
    }

    @SneakyThrows
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handle(HttpMessageNotReadableException exception,
                                                      WebRequest request) {
        log.info("Http message is missing request body");
        return ofType(request, HttpStatus.BAD_REQUEST,
                "You are missing request body", null);
    }


    private ResponseEntity<Map<String, Object>> ofType(WebRequest request, HttpStatus status,
                                                       String message,
                                                       List<ConstraintsViolationError> validationErrors) {
        Map<String, Object> attributes = getErrorAttributes(request, ErrorAttributeOptions.defaults());
        attributes.put("status", status.value());
        attributes.put("error", message);
        attributes.put("fieldErrors", validationErrors);
        attributes.put("path", ((ServletWebRequest) request).getRequest().getRequestURI());
        return new ResponseEntity<>(attributes, status);
    }
}
