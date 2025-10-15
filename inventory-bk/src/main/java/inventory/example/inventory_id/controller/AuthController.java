package inventory.example.inventory_id.controller;

import inventory.example.inventory_id.enums.AuthMessage;
import inventory.example.inventory_id.exception.AuthenticationException;
import inventory.example.inventory_id.request.EmailAuthRequest;
import inventory.example.inventory_id.response.FirebaseSignUpResponse;
import inventory.example.inventory_id.service.FirebaseAuthService;
import inventory.example.inventory_id.service.TokenCacheService;
import inventory.example.inventory_id.validategroup.RegisterGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "認証", description = "ユーザー認証操作のためのAPI")
public class AuthController extends BaseController {

  private final FirebaseAuthService firebaseAuthService;
  private final TokenCacheService tokenCacheService;

  private static final String SIGNUP_SUCCEEDED_EXAMPLE =
    "{ \"message\": \"ユーザー登録が完了しました\" }";
  private static final String SIGNUP_FAILED_EXAMPLE =
    "{ \"message\": \"ユーザー登録に失敗しました\" }";
  private static final String SERVER_ERROR_EXAMPLE =
    "{ \"message\": \"サーバーエラーが発生しました\" }";
  private static final String INVALID_REQUEST_EXAMPLE =
    "{ \"message\": \"メールアドレスまたはパスワードが無効です\" }";
  private static final String SIGNOUT_SUCCEEDED_EXAMPLE =
    "{ \"message\": \"サインアウトが完了しました\" }";
  private static final String SIGNIN_SUCCEEDED_EXAMPLE =
    "{ \"message\": \"サインインが完了しました\" }";
  private static final String SIGNIN_FAILED_EXAMPLE =
    "{ \"message\": \"サインインに失敗しました\" }";

  public AuthController(
    FirebaseAuthService firebaseAuthService,
    TokenCacheService tokenCacheService
  ) {
    this.firebaseAuthService = firebaseAuthService;
    this.tokenCacheService = tokenCacheService;
  }

  @PostMapping("/signUp")
  @Operation(
    summary = "ユーザー登録",
    description = "新しいユーザーを登録します"
  )
  @ApiResponses(
    {
      @ApiResponse(
        responseCode = "200",
        description = "ユーザー登録成功",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = SIGNUP_SUCCEEDED_EXAMPLE)
        )
      ),
      @ApiResponse(
        responseCode = "401",
        description = "サーバーエラーが発生しました",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = SIGNUP_FAILED_EXAMPLE)
        )
      ),
      @ApiResponse(
        responseCode = "500",
        description = "サーバーエラーが発生しました",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = SERVER_ERROR_EXAMPLE)
        )
      ),
    }
  )
  public ResponseEntity<Object> signUp(HttpServletResponse response) {
    try {
      FirebaseSignUpResponse firebaseResponse =
        firebaseAuthService.anonymouslySignUp();
      String idToken = firebaseResponse.getIdToken();

      String userId = firebaseAuthService.verifyToken(idToken);
      tokenCacheService.cacheUser(idToken, userId);

      setCookie(response, idToken);
      return response(
        HttpStatus.OK,
        AuthMessage.REGISTER_SUCCEEDED_MSG.getMessage()
      );
    } catch (Exception e) {
      return response(HttpStatus.UNAUTHORIZED, e.getMessage());
    }
  }

  @PostMapping("/signOut")
  @Operation(
    summary = "ユーザーサインアウト",
    description = "ユーザーのセッションを削除します"
  )
  @ApiResponses(
    {
      @ApiResponse(
        responseCode = "200",
        description = "サインアウト成功",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = SIGNOUT_SUCCEEDED_EXAMPLE)
        )
      ),
      @ApiResponse(
        responseCode = "500",
        description = "サーバーエラー",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = SERVER_ERROR_EXAMPLE)
        )
      ),
    }
  )
  public ResponseEntity<Object> signOut(HttpServletResponse response) {
    try {
      String token = getTokenFromRequest();

      if (token != null) {
        tokenCacheService.removeUserCache(token);
      }

      clearCookie(response);

      return response(
        HttpStatus.OK,
        AuthMessage.SIGNOUT_SUCCEEDED_MSG.getMessage()
      );
    } catch (Exception e) {
      return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        AuthMessage.SERVER_ERROR_MSG.getMessage()
      );
    }
  }

  @PostMapping("/email/signUp")
  @Operation(
    summary = "メール/パスワードでユーザー登録",
    description = "メールアドレスとパスワードで新しいユーザーを登録します"
  )
  @ApiResponses(
    {
      @ApiResponse(
        responseCode = "200",
        description = "ユーザー登録成功",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = SIGNUP_SUCCEEDED_EXAMPLE)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "リクエストデータが無効です",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = INVALID_REQUEST_EXAMPLE)
        )
      ),
      @ApiResponse(
        responseCode = "401",
        description = "認証エラー",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = SIGNUP_FAILED_EXAMPLE)
        )
      ),
      @ApiResponse(
        responseCode = "500",
        description = "サーバーエラー",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = SERVER_ERROR_EXAMPLE)
        )
      ),
    }
  )
  public ResponseEntity<Object> emailSignUp(
    @RequestBody @Validated(
      { RegisterGroup.class, Default.class }
    ) EmailAuthRequest emailAuthRequest,
    HttpServletResponse response
  ) {
    try {
      FirebaseSignUpResponse firebaseResponse = firebaseAuthService.emailSignUp(
        emailAuthRequest.getEmail(),
        emailAuthRequest.getPassword()
      );
      String idToken = firebaseResponse.getIdToken();

      String userId = firebaseAuthService.verifyToken(idToken);
      tokenCacheService.cacheUser(idToken, userId);

      setCookie(response, idToken);
      return response(
        HttpStatus.OK,
        AuthMessage.REGISTER_SUCCEEDED_MSG.getMessage()
      );
    } catch (AuthenticationException e) {
      return response(HttpStatus.UNAUTHORIZED, e.getMessage());
    } catch (Exception e) {
      return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        AuthMessage.SERVER_ERROR_MSG.getMessage()
      );
    }
  }

  @PostMapping("/email/signIn")
  @Operation(
    summary = "メール/パスワードでサインイン",
    description = "既存ユーザーのメールアドレスとパスワードでサインインします"
  )
  @ApiResponses(
    {
      @ApiResponse(
        responseCode = "200",
        description = "サインイン成功",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = SIGNIN_SUCCEEDED_EXAMPLE)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "リクエストデータが無効です",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = INVALID_REQUEST_EXAMPLE)
        )
      ),
      @ApiResponse(
        responseCode = "401",
        description = "認証エラー",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = SIGNIN_FAILED_EXAMPLE)
        )
      ),
      @ApiResponse(
        responseCode = "500",
        description = "サーバーエラー",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = SERVER_ERROR_EXAMPLE)
        )
      ),
    }
  )
  public ResponseEntity<Object> emailSignIn(
    @RequestBody @Valid EmailAuthRequest emailAuthRequest,
    HttpServletResponse response
  ) {
    try {
      FirebaseSignUpResponse firebaseResponse = firebaseAuthService.emailSignIn(
        emailAuthRequest.getEmail(),
        emailAuthRequest.getPassword()
      );
      String idToken = firebaseResponse.getIdToken();

      String userId = firebaseAuthService.verifyToken(idToken);
      tokenCacheService.cacheUser(idToken, userId);

      setCookie(response, idToken);
      return response(
        HttpStatus.OK,
        AuthMessage.SIGNIN_SUCCEEDED_MSG.getMessage()
      );
    } catch (AuthenticationException e) {
      return response(HttpStatus.UNAUTHORIZED, e.getMessage());
    } catch (Exception e) {
      return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        AuthMessage.SERVER_ERROR_MSG.getMessage()
      );
    }
  }
}
