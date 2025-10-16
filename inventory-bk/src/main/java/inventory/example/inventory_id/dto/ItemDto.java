package inventory.example.inventory_id.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemDto {
  @Schema(example = "鉛筆", description = "アイテム名")
  private String name;
  @Schema(example = "文房具", description = "カテゴリ名")
  private String categoryName;
  @Schema(example = "10", description = "在庫総数量")
  private int totalQuantity;
  @Schema(example = "500", description = "在庫総金額")
  private int totalPrice;
}
