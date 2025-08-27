package inventory.example.inventory_id.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ItemRequest {
  @NotBlank(message = "アイテム名は必須です")
  @Size(max = 50, message = "アイテム名は50文字以内で入力してください")
  private String name;

  @PositiveOrZero(message = "数量は0以上の整数で入力してください")
  private int quantity = 0;

  @NotBlank(message = "カテゴリは必須です")
  private String categoryName;
}
