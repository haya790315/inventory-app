package inventory.example.inventory_id.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.firebase.database.annotations.NotNull;

import inventory.example.inventory_id.enums.TransactionType;
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

  @NotNull
  private String userId;

  @PositiveOrZero
  private int quantity;
  @PositiveOrZero
  private int price;

  private boolean deletedFlag = false;

  @CreationTimestamp
  private LocalDateTime createdAt;

  private LocalDate expirationDate;

  @Enumerated(EnumType.STRING)
  @NotNull
  private TransactionType transactionType;

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
      TransactionType transactionType,
      ItemRecord sourceRecord) {
    this.item = item;
    this.userId = userId;
    this.quantity = quantity;
    this.price = price;
    this.expirationDate = expirationDate;
    this.transactionType = transactionType;
    this.sourceRecord = sourceRecord;
  }

  /**
   * 入庫トランザクション用のコンストラクタ（在庫受入）。
   * ソースレコードなしで在庫に新しいアイテムを追加する際に使用。
   * 
   * @param item            記録対象のアイテム
   * @param userId          トランザクションを実行するユーザーのID
   * @param quantity        在庫に追加されるアイテムの数量
   * @param price           アイテムの単価
   * @param expirationDate  アイテムの有効期限
   * @param transactionType トランザクションタイプ（入庫操作にはINを指定）
   */
  public ItemRecord(
      Item item,
      String userId,
      int quantity,
      int price,
      LocalDate expirationDate,
      TransactionType transactionType) {
    this(item,
        userId,
        quantity,
        price,
        expirationDate,
        transactionType,
        null);
  }

  /**
   * 出庫トランザクション用のコンストラクタ（在庫払出）。
   * ソースレコードを参照して在庫からアイテムを除去する際に使用。
   * 
   * @param item            記録対象のアイテム
   * @param userId          トランザクションを実行するユーザーのID
   * @param quantity        在庫から除去されるアイテムの数量
   * @param transactionType トランザクションタイプ（出庫操作にはOUTを指定）
   * @param sourceRecord    消費される元の入庫レコードへの参照
   */
  public ItemRecord(
      Item item,
      String userId,
      int quantity,
      TransactionType transactionType,
      ItemRecord sourceRecord) {
    this(item,
        userId,
        quantity,
        0,
        null,
        transactionType,
        sourceRecord);
  }
}
