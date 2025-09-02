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

import inventory.example.inventory_id.exception.AuthenticationException;
import inventory.example.inventory_id.response.FirebaseSignUpResponse;
import io.github.cdimascio.dotenv.Dotenv;

public class FirebaseAuthServiceTest {

  private FirebaseAuthService service;
  private Dotenv dotenv;

  @BeforeEach
  void setUp() {
    dotenv = mock(Dotenv.class);
    when(dotenv.get("FIREBASE_API_KEY")).thenReturn("test-api-key");
    service = new FirebaseAuthService() {
      @Override
      public String getApiKey() {
        return dotenv.get("FIREBASE_API_KEY");
      }
    };
  }

  @Test
  @DisplayName("サインアップに成功する")
  void testSignUpReturnsIdToken() {
    FirebaseSignUpResponse mockResponse = mock(FirebaseSignUpResponse.class);
    when(mockResponse.getIdToken()).thenReturn("test-id-token");

    FirebaseAuthService spyService = spy(service);
    doReturn(mockResponse).when(spyService).anonymouslySignUp();

    String idToken = spyService.signUp();
    assertEquals("test-id-token", idToken);
  }

  @Test
  @DisplayName("サインアップに失敗する")
  void testSignUpThrowsAuthenticationException() {
    FirebaseAuthService spyService = spy(service);
    doThrow(new AuthenticationException("登録失敗しました")).when(spyService).anonymouslySignUp();

    AuthenticationException ex = assertThrows(AuthenticationException.class, spyService::signUp);
    assertEquals("登録失敗しました", ex.getMessage());
  }
}
