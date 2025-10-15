package inventory.example.inventory_id.dto;

import inventory.example.inventory_id.model.ItemRecord;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemRecordDto {

  private UUID id;
  private String itemName;
  private String categoryName;
  private int quantity;
  private int price;
  private ItemRecord.Source source;
  private String expirationDate;
  private String createdAt;

  public ItemRecordDto(
    UUID id,
    String itemName,
    String categoryName,
    int quantity,
    int price,
    ItemRecord.Source source,
    String expirationDate
  ) {
    this.id = id;
    this.itemName = itemName;
    this.categoryName = categoryName;
    this.quantity = quantity;
    this.price = price;
    this.source = source;
    this.expirationDate = expirationDate;
  }
}
