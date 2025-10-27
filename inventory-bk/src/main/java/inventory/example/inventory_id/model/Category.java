package inventory.example.inventory_id.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "category")
public class Category {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(columnDefinition = "UUID")
  private UUID id;

  private String name;
  private String userId;
  private boolean deletedFlag;

  @OneToMany(
    mappedBy = "category",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  private List<Item> items;

  private LocalDateTime updatedAt;

  @PrePersist
  @PreUpdate
  public void updateTimestamp() {
    this.updatedAt = LocalDateTime.now();
  }

  public Category(String name) {
    this.name = name;
  }

  public Category(String name, String userId) {
    this.name = name;
    this.userId = userId;
  }
}
