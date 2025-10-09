package inventory.example.inventory_id.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import inventory.example.inventory_id.exception.AuthenticationException;
import inventory.example.inventory_id.service.FirebaseAuthService;
import inventory.example.inventory_id.service.TokenCacheService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public abstract class BaseController {
  @Autowired
  FirebaseAuthService firebaseAuthService;

  @Autowired
  TokenCacheService tokenCacheService;

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
    String token = getTokenFromRequest();
    if (token == null) {
      throw new AuthenticationException("認証トークンが見つかりません");
    }

    // RedisキャッシュからユーザーIDを取得
    String cachedUserId = tokenCacheService.getUserIdFromCache(token);
    if (cachedUserId == null) {
      throw new AuthenticationException(
          "時間が経過したため、再度サインインしてください");
    }

    // セッションのタイムアウトを更新
    tokenCacheService.refreshUserCache(token);
    return cachedUserId;
  }

  protected String getTokenFromRequest() {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if (tokenKey.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  protected void setCookie(HttpServletResponse response, String tokenValue) {
    Cookie cookie = new Cookie(tokenKey, tokenValue);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    response.addCookie(cookie);
  }

  protected void clearCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie(tokenKey, null);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }
}
