package inventory.example.inventory_id.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {
  @Bean
  public GroupedOpenApi publicApi() {
    return GroupedOpenApi.builder()
        .group("public")
        .pathsToMatch("/**")
        .addOpenApiCustomizer(openApi -> openApi.info(new Info()
            .title("在庫システムAPI")
            .description("在庫システムのAPIドキュメント")
            .version("v1.0")))
        .build();
  }
}