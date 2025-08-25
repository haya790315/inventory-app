package inventory.example.inventory_id.repository;

import inventory.example.inventory_id.model.Category;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
  boolean existsByUserIdAndName(int userId, String name);

  List<Category> findByUserIdIn(List<Integer> userIds);

  Optional<Category> findByUserIdAndName(int userId, String name);

  Optional<Category> findByUserIdAndId(int userId, UUID id);

  List<Category> findByUserIdInAndName(List<Integer> userIds, String name);

  int countByUserIdAndDeletedFlagFalse(int userId);

}