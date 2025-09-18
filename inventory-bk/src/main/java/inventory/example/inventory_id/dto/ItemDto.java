package inventory.example.inventory_id.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemDto {
  private String name;
  private String categoryName;
  private int quantity;
  private int totalPrice;
}
