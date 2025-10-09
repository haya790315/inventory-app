package inventory.example.inventory_id.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FirebaseSignUpRequest {

  private String email;
  private String password;
  private boolean returnSecureToken = true;

  /**
   * 匿名サインアップ用のコンストラクタ
   *
   * @param returnSecureToken セキュアトークンを返すかどうか（通常はtrue）
   */
  public FirebaseSignUpRequest(boolean returnSecureToken) {
    this.returnSecureToken = returnSecureToken;
  }

  /**
   * メール/パスワードサインアップ用のコンストラクタ
   *
   * @param email    ユーザーのメールアドレス
   * @param password ユーザーのパスワード
   */
  public FirebaseSignUpRequest(String email, String password) {
    this.email = email;
    this.password = password;
    this.returnSecureToken = true;
  }
}
