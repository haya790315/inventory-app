package inventory.example.inventory_id.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FirebaseSignUpRequest {
  private boolean returnSecureToken;
}
