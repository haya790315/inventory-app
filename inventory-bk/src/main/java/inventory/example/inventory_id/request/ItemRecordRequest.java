package inventory.example.inventory_id.request;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import inventory.example.inventory_id.validation.CustomLocalDateDeserializer;
import inventory.example.inventory_id.validation.ValidItemRecordRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
  private Source source;

  public enum Source {
    IN, OUT
  }

  private UUID itemRecordId;

  // 入庫時のリクエスト
  public ItemRecordRequest(
      UUID itemId,
      int quantity,
      int price,
      LocalDate expirationDate,
      Source source) {
    this.itemId = itemId;
    this.quantity = quantity;
    this.price = price;
    this.expirationDate = expirationDate;
    this.source = source;
  }

  // 出庫時のリクエスト
  public ItemRecordRequest(
      UUID itemId,
      int quantity,
      Source source,
      UUID itemRecordId) {
    this.itemId = itemId;
    this.quantity = quantity;
    this.source = source;
    this.itemRecordId = itemRecordId;
  }
}
