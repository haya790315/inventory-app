package inventory.example.inventory_id.handler;

import inventory.example.inventory_id.exception.AuthenticationException;
import java.io.IOException;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

public class RestTemplateHandler implements ResponseErrorHandler {

  @Override
  public boolean hasError(ClientHttpResponse response) throws IOException {
    if (response.getStatusCode().value() == 401) {
      throw new AuthenticationException("認証エラーが発生しました。");
    }
    return false;
  }
}
