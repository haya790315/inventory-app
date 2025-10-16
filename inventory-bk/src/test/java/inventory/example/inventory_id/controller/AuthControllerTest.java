package inventory.example.inventory_id.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import inventory.example.inventory_id.service.FirebaseAuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock
  private FirebaseAuthService firebaseAuthService;

  @Spy
  @InjectMocks
  private AuthController authController;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
  }

  @Test
  @DisplayName("ユーザー登録成功時のテスト")
  void signUpSuccessSetsCookieAndReturnsOk() throws Exception {
    when(firebaseAuthService.signUp()).thenReturn("test-id-token");

    mockMvc.perform(post("/api/signUp"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("ユーザー登録が完了しました"));
  }

  @Test
  @DisplayName("ユーザー登録失敗時のテスト")
  void signUpFailureReturnsUnauthorized() throws Exception {
    when(firebaseAuthService.signUp()).thenThrow(new RuntimeException("error"));

    mockMvc.perform(post("/api/signUp"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("ユーザー登録に失敗しました"));
  }
}
