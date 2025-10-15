package inventory.example.inventory_id.handler;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

public class RestTemplateHandler implements ResponseErrorHandler {

  @Override
  public boolean hasError(ClientHttpResponse response) {
    return false; // Never throw exception
  }

  @Override
  public void handleError(ClientHttpResponse response) {
    // No implementation needed as we are not throwing exceptions
  }
}
