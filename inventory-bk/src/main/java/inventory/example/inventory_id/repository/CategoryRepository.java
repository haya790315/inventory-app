package inventory.example.inventory_id.repository;

import inventory.example.inventory_id.model.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository
  extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {
  boolean existsByUserIdAndName(String userId, String name);

  @Query(
    value = """
    SELECT *
    FROM category
    WHERE user_id IN (:userIds)
    AND deleted_flag = FALSE
    """,
    nativeQuery = true
  )
  List<Category> findNotDeleted(List<String> userIds);

  @Query(
    value = """
    SELECT *
    FROM category
    WHERE user_id IN (:userIds)
    AND id = :id
    AND deleted_flag = FALSE
    """,
    nativeQuery = true
  )
  Optional<Category> findUserCategory(List<String> userIds, UUID id);

  @Query(
    value = """
    SELECT *
    FROM category
    WHERE user_id IN (:userIds)
    AND name = :name
    AND deleted_flag = FALSE
    """,
    nativeQuery = true
  )
  List<Category> findActiveCateByName(List<String> userIds, String name);
}
