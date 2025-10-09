package inventory.example.inventory_id.exception;

import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fasterxml.jackson.databind.JsonMappingException;

@ControllerAdvice
public class ValidationException {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException ex) {
    String errorMessage = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> error.getDefaultMessage())
        .findFirst()
        .orElse("Invalid input");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Collections.singletonMap("error", errorMessage));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Object> handleJsonParseException(HttpMessageNotReadableException ex) {
    String errorMessage = "JSON エラー";

    Throwable cause = ex.getCause();
    if (cause instanceof JsonMappingException) {
      JsonMappingException jsonMappingException = (JsonMappingException) cause;
      Throwable rootCause = jsonMappingException.getCause();
      if (rootCause instanceof IllegalArgumentException) {
        errorMessage = rootCause.getMessage();
      }
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Collections.singletonMap("error", errorMessage));
  }
}
