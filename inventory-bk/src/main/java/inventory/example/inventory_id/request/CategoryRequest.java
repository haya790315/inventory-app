package inventory.example.inventory_id.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {
  private final String nonEmptyStringRegex = ".*[^\\s　].*";
  @NotBlank(message = "カテゴリ名は必須")
  @Size(max = 50, message = "カテゴリ名は50文字以内")
  @Pattern(regexp = nonEmptyStringRegex, message = "カテゴリ名は必須")
  private String name;
}
