package inventory.example.inventory_id.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FirebaseSignUpResponse {
  private String idToken;
  private String email;
  private String refreshToken;
  private String expiresIn;
  private String localId;
  private boolean registered;
}
