package inventory.example.inventory_id.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuthException;
import inventory.example.inventory_id.exception.AuthenticationException;
import inventory.example.inventory_id.request.EmailAuthRequest;
import inventory.example.inventory_id.response.FirebaseSignUpResponse;
import inventory.example.inventory_id.service.FirebaseAuthService;
import inventory.example.inventory_id.service.TokenCacheService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private FirebaseAuthService firebaseAuthService;

  @MockitoBean
  private TokenCacheService tokenCacheService;

  private final String testIdToken = "test-id-token";

  private final ObjectMapper objectMapper = new ObjectMapper();

  private String serverErrorMsg = "サーバーエラーが発生しました";
  private String userRegisterMsg = "ユーザー登録が完了しました";

  @BeforeEach
  void setUp() throws FirebaseAuthException {
    lenient()
      .when(firebaseAuthService.verifyToken(testIdToken))
      .thenReturn("test-user-id");
  }

  @Test
  @Tag("POST /api/auth/signUp")
  @DisplayName("匿名サインアップ - ユーザー登録成功時のテスト")
  void signUpSuccessSetsCookieAndReturnsOk() throws Exception {
    when(firebaseAuthService.anonymouslySignUp()).thenReturn(
      new FirebaseSignUpResponse(
        testIdToken,
        null,
        "test-refresh-token",
        null,
        "test-local-id",
        true
      )
    );

    mockMvc
      .perform(post("/api/auth/signUp"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.message").value(userRegisterMsg))
      .andExpect(cookie().value("firebase-token", testIdToken));
  }

  @Test
  @Tag("POST /api/auth/signUp")
  @DisplayName("匿名サインアップ - ユーザー登録失敗時のテスト")
  void signUpFailureReturnsUnauthorized() throws Exception {
    when(firebaseAuthService.anonymouslySignUp()).thenThrow(
      new RuntimeException("ユーザー登録に失敗しました")
    );

    mockMvc
      .perform(post("/api/auth/signUp"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.message").value("ユーザー登録に失敗しました"));
  }

  @Test
  @Tag("POST /api/auth/signOut")
  @DisplayName("ユーザーサインアウト成功時のテスト")
  void signOutSuccessRemovesCacheAndReturnsOk() throws Exception {
    mockMvc
      .perform(
        post("/api/auth/signOut").cookie(
          new Cookie("firebase-token", testIdToken)
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.message").value("サインアウトが完了しました"));
  }

  @Test
  @Tag("POST /api/auth/signOut")
  @DisplayName("ユーザーサインアウト - サーバーエラー発生時のテスト")
  void signOutWithServerError() throws Exception {
    doThrow(new RuntimeException("サーバーエラーが発生しました"))
      .when(tokenCacheService)
      .removeUserCache(testIdToken);
    mockMvc
      .perform(
        post("/api/auth/signOut").cookie(
          new Cookie("firebase-token", testIdToken)
        )
      )
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.message").value(serverErrorMsg));
  }

  @Test
  @Tag("POST /api/auth/email/signUp")
  @DisplayName("メール・パスワードサインアップ - ユーザー登録成功時のテスト")
  void emailSignUpSuccessSetsCookieAndReturnsOk() throws Exception {
    String testEmail = "test@example.com";
    String testPassword = "password";

    EmailAuthRequest emailAuthRequest = new EmailAuthRequest(
      testEmail,
      testPassword
    );

    when(firebaseAuthService.emailSignUp(testEmail, testPassword)).thenReturn(
      new FirebaseSignUpResponse(
        testIdToken,
        null,
        "test-refresh-token",
        null,
        "test-local-id",
        true
      )
    );

    mockMvc
      .perform(
        post("/api/auth/email/signUp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(emailAuthRequest))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.message").value(userRegisterMsg))
      .andExpect(cookie().value("firebase-token", testIdToken));
  }

  @Test
  @Tag("POST /api/auth/email/signUp")
  @DisplayName("メール・パスワードサインアップ - ユーザー登録失敗時のテスト")
  void emailSignUpFailureReturnsUnauthorized() throws Exception {
    String testEmail = "test@example.com";
    String testPassword = "password";
    EmailAuthRequest emailAuthRequest = new EmailAuthRequest(
      testEmail,
      testPassword
    );

    when(firebaseAuthService.emailSignUp(testEmail, testPassword)).thenThrow(
      new RuntimeException(serverErrorMsg)
    );
    mockMvc
      .perform(
        post("/api/auth/email/signUp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(emailAuthRequest))
      )
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.message").value(serverErrorMsg));
  }

  @Test
  @Tag("POST /api/auth/email/signUp")
  @DisplayName("メール・パスワードサインアップ - emailが空の時のテスト")
  void emailSignUpWithEmptyRequestBodyReturnsBadRequest() throws Exception {
    EmailAuthRequest emailAuthRequest = new EmailAuthRequest("", "123456");
    mockMvc
      .perform(
        post("/api/auth/email/signUp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(emailAuthRequest))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error").value("メールアドレスは必須です"));
  }

  @ParameterizedTest
  @Tag("POST /api/auth/email/signUp")
  @DisplayName(
    "メール・パスワードサインアップ - バリデーションエラー時のテスト"
  )
  @ValueSource(
    strings = {
      "invalid-email",
      "user@.com",
      "　　　　",
      "user@domain..com",
      "@example.com",
      "user@",
    }
  )
  void emailSignUpValidationErrorReturnsBadRequest(String email)
    throws Exception {
    EmailAuthRequest emailAuthRequest = new EmailAuthRequest(email, "123456");
    mockMvc
      .perform(
        post("/api/auth/email/signUp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(emailAuthRequest))
      )
      .andExpect(status().isBadRequest())
      .andExpect(
        jsonPath("$.error").value("有効なメールアドレス形式で入力してください")
      );
  }

  @ParameterizedTest
  @Tag("POST /api/auth/email/signUp")
  @DisplayName(
    "メール・パスワードサインアップ - パスワードが短すぎる時のテスト"
  )
  @ValueSource(strings = { "　　　　", "123", "" })
  void emailSignUpWithShortPasswordReturnsBadRequest(String password)
    throws Exception {
    EmailAuthRequest emailAuthRequest = new EmailAuthRequest(
      "test@example.com",
      password
    );
    mockMvc
      .perform(
        post("/api/auth/email/signUp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(emailAuthRequest))
      )
      .andExpect(status().isBadRequest())
      .andExpect(
        jsonPath("$.error").value("パスワードは6文字以上である必要があります")
      );
  }

  @Test
  @Tag("POST /api/auth/email/signUp")
  @DisplayName("メール・パスワードサインアップ - パスワードが空の時のテスト")
  void emailSignUpWithEmptyPasswordReturnsBadRequest() throws Exception {
    EmailAuthRequest emailAuthRequest = new EmailAuthRequest(
      "test@example.com",
      null
    );
    mockMvc
      .perform(
        post("/api/auth/email/signUp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(emailAuthRequest))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error").value("パスワードは必須です"));
  }

  @Test
  @Tag("POST /api/auth/email/signIn")
  @DisplayName("メール・パスワードサインイン - サインイン成功時のテスト")
  void emailSignInSuccessSetsCookieAndReturnsOk() throws Exception {
    String testEmail = "test@example.com";
    String testPassword = "password123";

    EmailAuthRequest emailAuthRequest = new EmailAuthRequest(
      testEmail,
      testPassword
    );

    when(firebaseAuthService.emailSignIn(testEmail, testPassword)).thenReturn(
      new FirebaseSignUpResponse(
        testIdToken,
        null,
        "test-refresh-token",
        null,
        "test-local-id",
        false
      )
    ); // false for signIn (not new user)

    mockMvc
      .perform(
        post("/api/auth/email/signIn")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(emailAuthRequest))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.message").value("サインインが完了しました"))
      .andExpect(cookie().value("firebase-token", testIdToken));
  }

  @Test
  @Tag("POST /api/auth/email/signIn")
  @DisplayName("メール・パスワードサインイン - 認証失敗時のテスト")
  void emailSignInAuthenticationFailureReturnsUnauthorized() throws Exception {
    String testEmail = "test@example.com";
    String testPassword = "wrongpassword";
    EmailAuthRequest emailAuthRequest = new EmailAuthRequest(
      testEmail,
      testPassword
    );

    when(firebaseAuthService.emailSignIn(testEmail, testPassword)).thenThrow(
      new AuthenticationException("サインインに失敗しました")
    );

    mockMvc
      .perform(
        post("/api/auth/email/signIn")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(emailAuthRequest))
      )
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.message").value("サインインに失敗しました"));
  }

  @Test
  @Tag("POST /api/auth/email/signIn")
  @DisplayName("メール・パスワードサインイン - サーバーエラー時のテスト")
  void emailSignInServerErrorReturnsInternalServerError() throws Exception {
    String testEmail = "test@example.com";
    String testPassword = "password123";
    EmailAuthRequest emailAuthRequest = new EmailAuthRequest(
      testEmail,
      testPassword
    );

    when(firebaseAuthService.emailSignIn(testEmail, testPassword)).thenThrow(
      new RuntimeException("Firebase connection error")
    );

    mockMvc
      .perform(
        post("/api/auth/email/signIn")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(emailAuthRequest))
      )
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.message").value(serverErrorMsg));
  }

  @Test
  @Tag("POST /api/auth/email/signIn")
  @DisplayName("メール・パスワードサインイン - 空のメールアドレスのテスト")
  void emailSignInEmptyEmailReturnsBadRequest() throws Exception {
    EmailAuthRequest emailAuthRequest = new EmailAuthRequest("", "password123");
    mockMvc
      .perform(
        post("/api/auth/email/signIn")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(emailAuthRequest))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error").value("メールアドレスは必須です"));
  }

  @ParameterizedTest
  @Tag("POST /api/auth/email/signIn")
  @DisplayName("メール・パスワードサインイン - 無効なメール形式のテスト")
  @ValueSource(
    strings = {
      "invalid-email",
      "user@.com",
      "　　　　",
      "user@domain..com",
      "@example.com",
      "user@",
    }
  )
  void emailSignInInvalidEmailFormatReturnsBadRequest(String email)
    throws Exception {
    EmailAuthRequest emailAuthRequest = new EmailAuthRequest(
      email,
      "password123"
    );
    mockMvc
      .perform(
        post("/api/auth/email/signIn")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(emailAuthRequest))
      )
      .andExpect(status().isBadRequest())
      .andExpect(
        jsonPath("$.error").value("有効なメールアドレス形式で入力してください")
      );
  }

  @Test
  @Tag("POST /api/auth/email/signIn")
  @DisplayName("メール・パスワードサインイン - 空のパスワードのテスト")
  void emailSignInEmptyPasswordReturnsBadRequest() throws Exception {
    EmailAuthRequest emailAuthRequest = new EmailAuthRequest(
      "test@example.com",
      null
    );
    mockMvc
      .perform(
        post("/api/auth/email/signIn")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(emailAuthRequest))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error").value("パスワードは必須です"));
  }
}
