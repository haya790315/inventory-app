package inventory.example.inventory_id.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import inventory.example.inventory_id.model.Item;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {
  @Query(value = "SELECT i.*\n"
      + "FROM item i\n"
      + "JOIN category c ON i.category_id = c.id\n"
      + "WHERE i.user_id IN (:userIds)\n"
      + "AND c.name = :categoryName\n"
      + "AND i.deleted_flag = FALSE", nativeQuery = true)
  List<Item> findUserActiveItemWithCategory(
      List<Integer> userIds,
      String categoryName);

  @Query(value = "SELECT *\n"
      + "FROM item\n"
      + "WHERE user_id IN (:userIds)\n"
      + "  AND id = :itemId\n"
      + "  AND deleted_flag = FALSE", nativeQuery = true)
  Optional<Item> findUserActiveItem(
      List<Integer> userIds,
      UUID itemId);
}
