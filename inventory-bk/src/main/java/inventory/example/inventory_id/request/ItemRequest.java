package inventory.example.inventory_id.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemRequest {
  private final String nonEmptyStringRegex = ".*[^\\s　].*";

  @NotBlank(message = "アイテム名は必須です")
  @Pattern(regexp = nonEmptyStringRegex, message = "アイテム名は必須です")
  @Size(max = 50, message = "アイテム名は50文字以内で入力してください")
  @Schema(example = "鉛筆", description = "アイテム名")
  private String name;

  @NotBlank(message = "カテゴリは必須です")
  @Pattern(regexp = nonEmptyStringRegex, message = "カテゴリは必須です")
  @Schema(example = "文房具", description = "カテゴリ名")
  private String categoryName;

  @PositiveOrZero(message = "数量は0以上の整数で入力してください")
  @Schema(example = "10", description = "数量", minimum = "0", type = "integer")
  private int quantity = 0;
}
