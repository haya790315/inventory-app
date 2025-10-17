package inventory.example.inventory_id.repository;

import inventory.example.inventory_id.model.ItemRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRecordRepository extends JpaRepository<ItemRecord, Long> {
  /**
   * ユーザーIDとレコードIDでItemRecordを取得
   */
  @Query(
    value = """
    SELECT
      *
    FROM
      item_record ir
    WHERE
      ir.user_id = :userId
      AND ir.id = :id
      AND ir.deleted_flag = FALSE
    """,
    nativeQuery = true
  )
  Optional<ItemRecord> getRecordByUserIdAndId(
    @Param("userId") String userId,
    @Param("id") Long id
  );

  /**
   * 指定の入庫のレコードにまだ出庫していない、残り数量を取得
   */
  @Query(
    value = """
    SELECT
      ir.quantity - COALESCE(SUM(out_ir.quantity), 0) AS remaining_quantity
    FROM
      item_record ir
    LEFT JOIN
      item_record out_ir
        ON out_ir.item_record_id = ir.id
      AND out_ir.transaction_type = 'OUT'
      AND out_ir.deleted_flag = FALSE
    WHERE
      ir.id = :recordId
      AND ir.transaction_type = 'IN'
      AND ir.deleted_flag = FALSE
    GROUP BY
      ir.id
    """,
    nativeQuery = true
  )
  Integer getInrecordRemainQuantity(@Param("recordId") Long recordId);

  /**
   * アイテムIDに紐づく入庫・出庫レコードの合計数量を取得
   * 入庫は正の数、出庫は負の数として計算
   * 存在しないレコードの場合は0を返す
   */
  @Query(
    value = """
    SELECT
      COALESCE(SUM(CASE
          WHEN ir.transaction_type = 'IN' THEN ir.quantity
          WHEN ir.transaction_type = 'OUT' THEN -ir.quantity
          ELSE 0
      END), 0)
    FROM
      item_record ir
    WHERE
      ir.item_id = :itemId
      AND ir.deleted_flag = FALSE
    """,
    nativeQuery = true
  )
  int getItemTotalQuantity(@Param("itemId") UUID itemId);

  @Query(
    value = """
    SELECT
      *
    FROM
      item_record
    WHERE id = :id
      AND user_id = :userId
      AND deleted_flag = FALSE
    """,
    nativeQuery = true
  )
  Optional<ItemRecord> findByIdAndUserId(
    @Param("id") Long id,
    @Param("userId") String userId
  );

  /**
   * ユーザーIDで全レコードを取得
   * 履歴がない時は空リストで返す
   * createdAtの降順でソート
   */
  @Query(
    value = """
    SELECT
    *
    FROM
    item_record
    WHERE
      user_id = :userId
      AND deleted_flag = FALSE
    ORDER BY created_at DESC
    """,
    nativeQuery = true
  )
  List<ItemRecord> findUserItemRecords(@Param("userId") String userId);
}
