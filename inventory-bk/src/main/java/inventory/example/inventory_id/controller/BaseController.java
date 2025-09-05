package inventory.example.inventory_id.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.firebase.auth.FirebaseAuthException;

import inventory.example.inventory_id.exception.AuthenticationException;
import inventory.example.inventory_id.service.FirebaseAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public abstract class BaseController {
  @Autowired
  FirebaseAuthService firebaseAuthService;
  @Autowired
  protected HttpServletRequest request;

  private String tokenKey = "firebase-token";

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

  protected String fetchUserIdFromToken() throws AuthenticationException {
    try {

      if (request.getCookies() != null) {
        for (Cookie cookie : request.getCookies()) {
          if (tokenKey.equals(cookie.getName())) {
            return firebaseAuthService.verifyToken(cookie.getValue());
          }
        }
      }
      throw new AuthenticationException("認証トークンが見つかりません");
    } catch (FirebaseAuthException e) {
      throw new AuthenticationException("認証に失敗しました");
    }
  }

  public ResponseCookie setCookie(String tokenValue) throws Exception {
    String sessionCookie = firebaseAuthService.createSessionCookie(tokenValue);
    ResponseCookie cookie = ResponseCookie
        .from(tokenKey, sessionCookie)
        .httpOnly(true)
        .path("/")
        .build();
    return cookie;
  }
}
