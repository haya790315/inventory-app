package inventory.example.inventory_id.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inventory.example.inventory_id.service.FirebaseAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/signUp")
@Tag(name = "認証", description = "ユーザー認証操作のためのAPI")
public class AuthController extends BaseController {

  private final FirebaseAuthService firebaseAuthService;

  public AuthController(FirebaseAuthService firebaseAuthService) {
    this.firebaseAuthService = firebaseAuthService;
  }

  @PostMapping()
  @Operation(summary = "ユーザー登録", description = "新しいユーザーを登録します")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "ユーザー登録成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"ユーザー登録が完了しました\" }"))),
      @ApiResponse(responseCode = "401", description = "サーバーエラーが発生しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"ユーザー登録に失敗しました\" }"))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
  })
  public ResponseEntity<Object> signUp(HttpServletResponse response) {
    try {
      String idToken = firebaseAuthService.signUp();
      setCookie(response, idToken);
      return response(HttpStatus.OK, "ユーザー登録が完了しました");
    } catch (Exception e) {
      return response(HttpStatus.UNAUTHORIZED, "ユーザー登録に失敗しました");
    }
  }
}
