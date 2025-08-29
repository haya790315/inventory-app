package inventory.example.inventory_id.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import inventory.example.inventory_id.service.FirebaseAuthService;
import jakarta.servlet.http.HttpServletRequest;

@Component
public abstract class BaseController {
  @Autowired
  FirebaseAuthService firebaseAuthService;

  @Autowired
  protected HttpServletRequest request;

  protected <T> ResponseEntity<T> response(HttpStatus status) {
    return ResponseEntity.status(status).build();
  }

  protected ResponseEntity<Object> response(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(Collections.singletonMap("message", message));
  }

  protected <T> ResponseEntity<T> response(HttpStatus status, T data) {
    return ResponseEntity.status(status).body(data);
  }

  protected Integer fetchUserIdFromToken() {
    // TODO : get token from request
    String token = ""; // Retrieve the token from the request context
    return firebaseAuthService.getUserIdFromToken(token);
  }
}
