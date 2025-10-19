package inventory.example.inventory_id.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import inventory.example.inventory_id.enums.TransactionType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemRecordDto {

  private Integer id;
  private String itemName;
  private String categoryName;
  private int quantity;
  private int price;
  private TransactionType transactionType;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate expirationDate;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;
}
