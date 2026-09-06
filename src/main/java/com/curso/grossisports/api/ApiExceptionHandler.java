package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.ApiError;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> tratarNaoEncontrado(
        RecursoNaoEncontradoException exception,
        HttpServletRequest request) {

        return criarResposta(
            HttpStatus.NOT_FOUND,
            exception.getMessage(),
            request.getRequestURI(),
            null);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ApiError> tratarDuplicado(
        RecursoDuplicadoException exception,
        HttpServletRequest request) {

        return criarResposta(
            HttpStatus.CONFLICT,
            exception.getMessage(),
            request.getRequestURI(),
            null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> tratarValidacao(
        MethodArgumentNotValidException exception,
        HttpServletRequest request) {

        Map<String, String> fields = new LinkedHashMap<>();

        exception.getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                fields.put(
                    error.getField(),
                    error.getDefaultMessage()));

        return criarResposta(
            HttpStatus.BAD_REQUEST,
            "Dados inválidos",
            request.getRequestURI(),
            fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> tratarJsonInvalido(
        HttpMessageNotReadableException exception,
        HttpServletRequest request) {

        return criarResposta(
            HttpStatus.BAD_REQUEST,
            "JSON inválido",
            request.getRequestURI(),
            null);
    }

    private ResponseEntity<ApiError> criarResposta(
        HttpStatus status,
        String message,
        String path,
        Map<String, String> fields) {

        ApiError error = new ApiError(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            path,
            fields);

        return ResponseEntity
            .status(status)
            .body(error);
    }
}
