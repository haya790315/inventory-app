package inventory.example.inventory_id.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import inventory.example.inventory_id.exception.AuthenticationException;
import inventory.example.inventory_id.response.FirebaseSignUpResponse;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FirebaseAuthServiceTest {

  private FirebaseAuthService firebaseAuthService;
  private Dotenv dotenv;
  private final String FIREBASE_API_KEY = "FIREBASE_API_KEY";

  private String registerErrMsg = "登録に失敗しました";
  private String signInErrMsg = "サインインに失敗しました";

  @BeforeEach
  void setUp() {
    dotenv = mock(Dotenv.class);
    when(dotenv.get(FIREBASE_API_KEY)).thenReturn("test-api-key");
    firebaseAuthService = spy(
      new FirebaseAuthService(dotenv) {
        @Override
        public String getApiKey() {
          return dotenv.get(FIREBASE_API_KEY);
        }
      }
    );
  }

  @Test
  @DisplayName("サインアップに成功する")
  void testSignUpReturnsIdToken() {
    FirebaseSignUpResponse mockResponse = mock(FirebaseSignUpResponse.class);
    when(mockResponse.getIdToken()).thenReturn("test-id-token");

    doReturn(mockResponse).when(firebaseAuthService).anonymouslySignUp();

    String idToken = firebaseAuthService.anonymouslySignUp().getIdToken();
    assertEquals("test-id-token", idToken);
  }

  @Test
  @DisplayName("サインアップに失敗する")
  void testSignUpThrowsAuthenticationException() {
    doThrow(new AuthenticationException(registerErrMsg))
      .when(firebaseAuthService)
      .anonymouslySignUp();

    AuthenticationException ex = assertThrows(
      AuthenticationException.class,
      firebaseAuthService::anonymouslySignUp
    );
    assertEquals(registerErrMsg, ex.getMessage());
  }

  @Test
  @DisplayName("APIキーが正しく取得できる")
  void testGetApiKeyReturnsCorrectValue() {
    String apiKey = firebaseAuthService.getApiKey();
    assertEquals("test-api-key", apiKey);
  }

  @Test
  @DisplayName("匿名サインアップが成功する")
  void testAnonymouslySignUpSuccess() {
    FirebaseSignUpResponse mockResponse = mock(FirebaseSignUpResponse.class);
    doReturn(mockResponse).when(firebaseAuthService).anonymouslySignUp();
    assertEquals(mockResponse, firebaseAuthService.anonymouslySignUp());
  }

  @Test
  @DisplayName("匿名サインアップが失敗し例外を投げる")
  void testAnonymouslySignUpThrowsException() {
    doThrow(new AuthenticationException(registerErrMsg))
      .when(firebaseAuthService)
      .anonymouslySignUp();
    assertThrows(
      AuthenticationException.class,
      firebaseAuthService::anonymouslySignUp
    );
  }

  @Test
  @DisplayName("メールサインアップが成功する")
  void testEmailSignUpSuccess() {
    String email = "test@example.com";
    String password = "password123";
    FirebaseSignUpResponse mockResponse = mock(FirebaseSignUpResponse.class);
    when(mockResponse.getIdToken()).thenReturn("test-id-token");

    doReturn(mockResponse)
      .when(firebaseAuthService)
      .emailSignUp(email, password);

    FirebaseSignUpResponse response = firebaseAuthService.emailSignUp(
      email,
      password
    );
    assertEquals(mockResponse, response);
    assertEquals("test-id-token", response.getIdToken());
  }

  @Test
  @DisplayName("メールサインアップが失敗し例外を投げる")
  void testEmailSignUpThrowsException() {
    String email = "test.com";
    String password = "password123";

    doThrow(new AuthenticationException(registerErrMsg))
      .when(firebaseAuthService)
      .emailSignUp(email, password);

    AuthenticationException ex = assertThrows(
      AuthenticationException.class,
      () -> firebaseAuthService.emailSignUp(email, password)
    );
    assertEquals(registerErrMsg, ex.getMessage());
  }

  @Test
  @DisplayName("メールサインインが成功する")
  void testEmailSignInSuccess() {
    String email = "test@example.com";
    String password = "password123";
    FirebaseSignUpResponse mockResponse = mock(FirebaseSignUpResponse.class);
    when(mockResponse.getIdToken()).thenReturn("signin-id-token");

    doReturn(mockResponse)
      .when(firebaseAuthService)
      .emailSignIn(email, password);

    FirebaseSignUpResponse response = firebaseAuthService.emailSignIn(
      email,
      password
    );
    assertEquals(mockResponse, response);
    assertEquals("signin-id-token", response.getIdToken());
  }

  @Test
  @DisplayName("メールサインインが失敗し例外を投げる")
  void testEmailSignInThrowsException() {
    String email = "test@example.com";
    String password = "wrongpassword";

    doThrow(new AuthenticationException(signInErrMsg))
      .when(firebaseAuthService)
      .emailSignIn(email, password);

    AuthenticationException ex = assertThrows(
      AuthenticationException.class,
      () -> firebaseAuthService.emailSignIn(email, password)
    );
    assertEquals(signInErrMsg, ex.getMessage());
  }
}
