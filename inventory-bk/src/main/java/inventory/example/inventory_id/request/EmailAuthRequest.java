package inventory.example.inventory_id.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailAuthRequest {

  @NotBlank(message = "メールアドレスは必須です")
  @Email(message = "有効なメールアドレス形式で入力してください")
  private String email;

  @NotNull(message = "パスワードは必須です")
  @Size(min = 6, message = "パスワードは6文字以上である必要があります")
  private String password;
}
