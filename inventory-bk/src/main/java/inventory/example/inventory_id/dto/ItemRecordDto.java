package inventory.example.inventory_id.dto;

import inventory.example.inventory_id.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemRecordDto {

  private String itemName;
  private String categoryName;
  private int quantity;
  private int price;
  private TransactionType transactionType;
  private String expirationDate;
}
