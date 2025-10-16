package inventory.example.inventory_id;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(info = @Info(title = "在庫管理システムAPI仕様書", version = "1.0", description = "在庫管理APIの説明"))
@SpringBootApplication
public class InventoryIdApplication {

  public static void main(String[] args) {
    SpringApplication.run(InventoryIdApplication.class, args);
  }

}
