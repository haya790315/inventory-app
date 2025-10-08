package inventory.example.inventory_id.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import inventory.example.inventory_id.enums.TransactionType;
import inventory.example.inventory_id.validation.CustomLocalDateDeserializer;
import inventory.example.inventory_id.validation.ValidItemRecordRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ValidItemRecordRequest
public class ItemRecordRequest {

  @NotNull(message = "アイテムIDは必須です。")
  private UUID itemId;

  @Positive(message = "数量は1以上である必要があります。")
  private int quantity;

  @PositiveOrZero(message = "価格は0以上である必要があります。")
  private int price = 0;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @JsonDeserialize(using = CustomLocalDateDeserializer.class)
  private LocalDate expirationDate;

  @NotNull(message = "入出庫種別は必須です。")
  private TransactionType transactionType;

  private UUID itemRecordId;

  /**
   * 入庫時のリクエストコンストラクタ
   *
   * @param itemId          アイテムID
   * @param quantity        数量
   * @param price           価格
   * @param expirationDate  有効期限
   * @param transactionType 入出庫種別（IN）
   */
  public ItemRecordRequest(
    UUID itemId,
    int quantity,
    int price,
    LocalDate expirationDate,
    TransactionType transactionType
  ) {
    this.itemId = itemId;
    this.quantity = quantity;
    this.price = price;
    this.expirationDate = expirationDate;
    this.transactionType = transactionType;
  }

  /**
   * 出庫時のリクエストコンストラクタ
   *
   * @param itemId          アイテムID
   * @param quantity        数量
   * @param transactionType 入出庫種別（OUT）
   * @param itemRecordId    出庫対象のレコードID
   */
  public ItemRecordRequest(
    UUID itemId,
    int quantity,
    TransactionType transactionType,
    UUID itemRecordId
  ) {
    this.itemId = itemId;
    this.quantity = quantity;
    this.transactionType = transactionType;
    this.itemRecordId = itemRecordId;
  }
}
