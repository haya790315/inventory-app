package inventory.example.inventory_id.repository;

import inventory.example.inventory_id.model.Category;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
  boolean existsByUserIdAndName(int userId, String name);

  @Query(value = """
      SELECT *
      FROM category
      WHERE user_id IN (:userIds)
      AND deleted_flag = FALSE
      """, nativeQuery = true)
  List<Category> findNotDeleted(List<Integer> userIds);

  @Query(value = """
      SELECT *
      FROM category
      WHERE user_id IN (:userIds)
      AND id = :id
      AND deleted_flag = FALSE
      """, nativeQuery = true)
  Optional<Category> findUserCategory(List<Integer> userIds, UUID id);

  @Query(value = """
      SELECT *
      FROM category
      WHERE user_id IN (:userIds)
      AND name = :name
      AND deleted_flag = FALSE
      """, nativeQuery = true)
  List<Category> findActiveCateByName(List<Integer> userIds, String name);
}
