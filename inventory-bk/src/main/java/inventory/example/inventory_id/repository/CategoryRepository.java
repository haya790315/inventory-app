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

  @Query(value = "SELECT *\n"
      + "FROM category\n"
      + "WHERE user_id IN (:userIds)\n"
      + "AND deleted_flag = FALSE", nativeQuery = true)
  List<Category> findNotDeleted(List<Integer> userIds);

  @Query(value = "SELECT *\n"
      + "FROM category\n"
      + "WHERE user_id = :userId\n"
      + "AND id = :id", nativeQuery = true)
  Optional<Category> findUserCategory(int userId, UUID id);

  List<Category> findByUserIdInAndName(List<Integer> userIds, String name);
}
