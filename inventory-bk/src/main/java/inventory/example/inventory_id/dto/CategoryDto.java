package inventory.example.inventory_id.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
  @Schema(example = "文房具", description = "カテゴリ名")
  private String name;
}
