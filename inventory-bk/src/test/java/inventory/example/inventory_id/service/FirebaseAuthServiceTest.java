package inventory.example.inventory_id.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
// ...existing imports...

import inventory.example.inventory_id.exception.AuthenticationException;
import inventory.example.inventory_id.response.FirebaseSignUpResponse;
import io.github.cdimascio.dotenv.Dotenv;
// ...existing imports...

public class FirebaseAuthServiceTest {
  private FirebaseAuthService firebaseAuthService;
  private Dotenv dotenv;
  private final String FIREBASE_API_KEY = "FIREBASE_API_KEY";

  @BeforeEach
  void setUp() {
    dotenv = mock(Dotenv.class);
    when(dotenv.get(FIREBASE_API_KEY)).thenReturn("test-api-key");
    firebaseAuthService = spy(new FirebaseAuthService(dotenv) {
      @Override
      public String getApiKey() {
        return dotenv.get(FIREBASE_API_KEY);
      }
    });
  }

  @Test
  @DisplayName("サインアップに成功する")
  void testSignUpReturnsIdToken() {
    FirebaseSignUpResponse mockResponse = mock(FirebaseSignUpResponse.class);
    when(mockResponse.getIdToken()).thenReturn("test-id-token");

    doReturn(mockResponse).when(firebaseAuthService).anonymouslySignUp();

    String idToken = firebaseAuthService.signUp();
    assertEquals("test-id-token", idToken);
  }

  @Test
  @DisplayName("サインアップに失敗する")
  void testSignUpThrowsAuthenticationException() {
    doThrow(new AuthenticationException("登録失敗しました")).when(firebaseAuthService).anonymouslySignUp();

    AuthenticationException ex = assertThrows(AuthenticationException.class, firebaseAuthService::signUp);
    assertEquals("登録失敗しました", ex.getMessage());
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
    doThrow(new AuthenticationException("登録失敗しました")).when(firebaseAuthService).anonymouslySignUp();
    assertThrows(AuthenticationException.class, firebaseAuthService::anonymouslySignUp);
  }
}
