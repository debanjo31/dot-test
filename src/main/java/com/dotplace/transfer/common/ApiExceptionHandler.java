package com.dotplace.transfer.common;

import com.dotplace.transfer.transaction.IdempotencyConflictException;
import com.dotplace.transfer.transaction.InvalidTransferException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(IdempotencyConflictException.class)
  ResponseEntity<ProblemDetail> handleIdempotencyConflict(IdempotencyConflictException exception) {
    return problem(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", exception.getMessage());
  }

  @ExceptionHandler(InvalidTransferException.class)
  ResponseEntity<ProblemDetail> handleInvalidTransfer(InvalidTransferException exception) {
    return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
    ProblemDetail detail =
        createProblem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed");
    Map<String, String> errors = new LinkedHashMap<>();
    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
    detail.setProperty("fieldErrors", errors);
    return ResponseEntity.badRequest().body(detail);
  }

  @ExceptionHandler({
    ConstraintViolationException.class,
    MissingRequestHeaderException.class,
    HttpMessageNotReadableException.class
  })
  ResponseEntity<ProblemDetail> handleMalformedRequest(Exception exception) {
    return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
  }

  private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(createProblem(status, code, message));
  }

  private ProblemDetail createProblem(HttpStatus status, String code, String message) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
    detail.setTitle(status.getReasonPhrase());
    detail.setType(URI.create("https://errors.dotplace.example/" + code.toLowerCase()));
    detail.setProperty("code", code);
    detail.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
    return detail;
  }
}
