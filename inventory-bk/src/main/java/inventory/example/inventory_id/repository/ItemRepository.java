package inventory.example.inventory_id.repository;

import inventory.example.inventory_id.model.Item;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository
  extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {
  @Query(
    value = """
    SELECT i.*
    FROM item i
    JOIN category c ON i.category_id = c.id
    WHERE i.user_id IN (:userIds)
      AND c.name = :categoryName
      AND c.deleted_flag = FALSE
      AND i.deleted_flag = FALSE
    ORDER BY i.updated_at DESC
    """,
    nativeQuery = true
  )
  List<Item> getActiveByCategoryName(
    @Param("userIds") List<String> userIds,
    @Param("categoryName") String categoryName
  );

  @Query(
    value = """
    SELECT *
    FROM item
    WHERE user_id IN (:userIds)
    AND id = :itemId
    AND deleted_flag = FALSE
    """,
    nativeQuery = true
  )
  Optional<Item> getActiveItemWithId(List<String> userIds, UUID itemId);

  @Query(
    value = """
    SELECT *
    FROM item
    WHERE user_id IN (:userIds)
    AND name = :name
    AND category_id = :categoryId
    AND deleted_flag = FALSE
    """,
    nativeQuery = true
  )
  Optional<Item> getActiveWithSameNameAndCategory(
    List<String> userIds,
    String name,
    UUID categoryId
  );

  // @Query(
  //   value = """
  //   SELECT *
  //   FROM item
  //   WHERE deleted_flag = FALSE
  //   AND user_id = :userId
  //   """,
  //   countQuery = """
  //   SELECT count(id)
  //   FROM item
  //   WHERE deleted_flag = FALSE
  //   AND user_id = :userId
  //   """,
  //   nativeQuery = true
  // )
  // Page<Item> findAllActive(String userId, Pageable pageable);

  @Query(
    value = """
    SELECT *
    FROM item
    WHERE deleted_flag = FALSE
    AND user_id = :userId
    """,
    countQuery = """
    SELECT count(id)
    FROM item
    WHERE deleted_flag = FALSE
    AND user_id = :userId
    """,
    nativeQuery = true
  )
  Page<Item> findAllActive(
    String userId,
    Specification<Item> spec,
    Pageable pageable
  );
}
