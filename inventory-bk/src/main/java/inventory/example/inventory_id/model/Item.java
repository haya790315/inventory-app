package inventory.example.inventory_id.model;

import java.util.UUID;
import java.time.LocalDateTime;

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
  private long userId;
  @ManyToOne
  @JoinColumn(name = "category_id")
  @JsonIgnore
  private Category category;
  private int quantity;
  private boolean deletedFlag;

  private LocalDateTime updatedAt;

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
      long userId,
      Category category,
      int quantity,
      boolean deletedFlag) {
    this.name = name;
    this.userId = userId;
    this.category = category;
    this.quantity = quantity;
    this.deletedFlag = deletedFlag;
  }
}
