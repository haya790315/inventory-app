package inventory.example.inventory_id.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import inventory.example.inventory_id.exception.AuthenticationException;
import inventory.example.inventory_id.request.FirebaseSignUpRequest;
import inventory.example.inventory_id.response.FirebaseSignUpResponse;
import io.github.cdimascio.dotenv.Dotenv;

@Service
public class FirebaseAuthService {
  private final Dotenv dotenv;

  @Value("${firebase.signUpBaseUrl}")
  private String signUpBaseUrl;

  private static final String API_KEY_PARAM = "key";

  private String FIREBASE_API_KEY = "FIREBASE_API_KEY";

  public FirebaseAuthService(Dotenv dotenv) {
    this.dotenv = dotenv;
  }

  public String getApiKey() {
    return dotenv.get(FIREBASE_API_KEY);
  }

  public String verifyToken(String idToken) throws FirebaseAuthException {
    FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
    return token.getUid();
  }

  public FirebaseSignUpResponse anonymouslySignUp() {

    FirebaseSignUpRequest requestBody = new FirebaseSignUpRequest(true);
    try {
      return RestClient.create(signUpBaseUrl)
          .post()
          .uri(uriBuilder -> uriBuilder
              .queryParam(API_KEY_PARAM, getApiKey())
              .build())
          .body(requestBody)
          .retrieve()
          .body(FirebaseSignUpResponse.class);
    } catch (Exception e) {
      throw new AuthenticationException("登録失敗しました");
    }
  }

  public String signUp() {
    FirebaseSignUpResponse response = anonymouslySignUp();
    return response.getIdToken();
  }
}
