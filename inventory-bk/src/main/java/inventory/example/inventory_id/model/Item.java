package inventory.example.inventory_id.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@Table(name = "item")
@ToString(exclude = "category")
public class Item {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;
  private String name;
  private String userId;
  @ManyToOne
  @JoinColumn(name = "category_id")
  @JsonIgnore
  private Category category;
  private boolean deletedFlag = false;
  private LocalDateTime updatedAt;

  int totalQuantity = 0;
  int totalPrice = 0;

  @PrePersist
  @PreUpdate
  public void updateTimestamp() {
    this.updatedAt = LocalDateTime.now();
  }

  @JsonProperty("categoryName")
  public String getCategoryName() {
    return category != null ? category.getName() : null;
  }

  public Item(String name) {
    this.name = name;
  }

  public Item(
      String name,
      String userId,
      Category category,
      boolean deletedFlag) {
    this.name = name;
    this.userId = userId;
    this.category = category;
    this.deletedFlag = deletedFlag;
  }

  public Item(
      String name,
      String userId,
      Category category,
      int total_quantity,
      int total_price,
      boolean deletedFlag) {
    this.name = name;
    this.userId = userId;
    this.category = category;
    this.totalQuantity = total_quantity;
    this.totalPrice = total_price;
    this.deletedFlag = deletedFlag;
  }

  public Item(
      String name,
      String userId,
      Category category,
      boolean deletedFlag,
      LocalDateTime updatedAt) {
    this.name = name;
    this.userId = userId;
    this.category = category;
    this.deletedFlag = deletedFlag;
    this.updatedAt = updatedAt;
  }
}
