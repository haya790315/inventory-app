package inventory.example.inventory_id.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ItemDto {
  @Schema(example = "鉛筆")
  private String name;
  @Schema(example = "10")
  private int quantity;
  @Schema(example = "文房具")
  private String categoryName;
}
