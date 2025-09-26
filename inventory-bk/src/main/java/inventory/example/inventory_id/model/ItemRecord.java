package inventory.example.inventory_id.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.firebase.database.annotations.NotNull;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@Table(name = "item_record")
@ToString()
public class ItemRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "item_id", nullable = false)
  @JsonIgnore
  private Item item;

  String userId;

  @PositiveOrZero
  private int quantity;
  @PositiveOrZero
  private int price;

  @CreationTimestamp
  private LocalDateTime createdAt;

  private LocalDate expirationDate;

  public static enum Source {
    IN, OUT
  }

  @Enumerated(EnumType.STRING)
  @NotNull
  private Source source;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "item_record_id")
  @JsonIgnore
  private ItemRecord sourceRecord;

  @OneToMany(mappedBy = "sourceRecord", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private List<ItemRecord> childRecords;

  @JsonProperty("itemName")
  public String getItemName() {
    return item.getName();
  }

  // コンストラクタ(フル版)
  public ItemRecord(
      Item item,
      String userId,
      int quantity,
      int price,
      LocalDate expirationDate,
      Source source,
      ItemRecord sourceRecord) {
    this.item = item;
    this.userId = userId;
    this.quantity = quantity;
    this.price = price;
    this.expirationDate = expirationDate;
    this.source = source;
    this.sourceRecord = sourceRecord;
  }

  // 入庫用のコンストラクタ（sourceRecord = null）
  public ItemRecord(
      Item item,
      String userId,
      int quantity,
      int price,
      LocalDate expirationDate,
      Source source) {
    this(item, userId, quantity, price, expirationDate, source, null);
  }

  // 出庫用のコンストラクタ（sourceRecordあり）
  public ItemRecord(
      Item item,
      String userId,
      int quantity,
      Source source,
      ItemRecord sourceRecord) {
    this(item, userId, quantity, 0, null, source, sourceRecord);
  }
}
