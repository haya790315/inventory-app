package inventory.example.inventory_id.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {
  @NotBlank(message = "カテゴリ名は必須")
  @Size(max = 50, message = "カテゴリ名は50文字以内")
  @Schema(example = "文房具", description = "カテゴリ名")
  @Pattern(regexp = ".*[^\\s　].*", message = "カテゴリ名は必須")
  private String name;
}
