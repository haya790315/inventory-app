package inventory.example.inventory_id.request;

import lombok.Data;

@Data
public class FirebaseSignUpRequest {

  private String email;
  private String password;

  // 常にtrueに設定
  // 参考： https://firebase.google.com/docs/reference/rest/auth#section-create-email-password
  private boolean returnSecureToken = true;

  /**
   * 匿名サインアップ用のコンストラクタ
   */
  public FirebaseSignUpRequest() {
    this.returnSecureToken = true;
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
