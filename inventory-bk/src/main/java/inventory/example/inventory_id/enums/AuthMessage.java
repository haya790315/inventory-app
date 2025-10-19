package inventory.example.inventory_id.enums;

public enum AuthMessage {
  REGISTER_SUCCEEDED_MSG("ユーザー登録が完了しました"),
  REGISTER_ERROR_MSG("ユーザー登録に失敗しました"),
  SIGNOUT_SUCCEEDED_MSG("サインアウトが完了しました"),
  SIGNIN_SUCCEEDED_MSG("サインインが完了しました"),
  SIGNIN_FAILED_MSG("サインインに失敗しました"),
  SERVER_ERROR_MSG("サーバーエラーが発生しました"),
  AUTHTOKEN_NOT_FOUND("認証トークンが見つかりません"),
  IDLE_TIMEOUT("時間が経過したため、再度サインインしてください");

  private final String message;

  AuthMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
