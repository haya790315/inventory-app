package inventory.example.inventory_id.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import inventory.example.inventory_id.model.Item;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {
  @Query(value = """
      SELECT i.*
      FROM item i
      JOIN category c ON i.category_id = c.id
      WHERE i.user_id IN (:userIds)
      AND c.name = :categoryName
      AND i.deleted_flag = FALSE
      """, nativeQuery = true)
  List<Item> getItemsByCategoryName(
      List<Integer> userIds,
      String categoryName);
}
