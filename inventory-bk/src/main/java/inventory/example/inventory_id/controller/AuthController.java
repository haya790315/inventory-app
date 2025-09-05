package inventory.example.inventory_id.controller;

import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inventory.example.inventory_id.service.FirebaseAuthService;

@RestController
@RequestMapping("/api/signUp")
public class AuthController extends BaseController {

  private final FirebaseAuthService firebaseAuthService;

  public AuthController(FirebaseAuthService firebaseAuthService) {
    this.firebaseAuthService = firebaseAuthService;
  }

  @PostMapping()
  public ResponseEntity<Object> signUp() {
    try {
      String idToken = firebaseAuthService.signUp();

      ResponseCookie cookie = setCookie(idToken);

      return ResponseEntity.status(HttpStatus.OK)
          .header("Set-Cookie", cookie.toString())
          .body(Collections.singletonMap("message", "ユーザー登録が完了しました"));
    } catch (Exception e) {
      return response(HttpStatus.UNAUTHORIZED, "ユーザー登録に失敗しました");
    }
  }
}
