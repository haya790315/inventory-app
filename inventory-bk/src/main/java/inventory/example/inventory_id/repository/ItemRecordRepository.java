package inventory.example.inventory_id.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import inventory.example.inventory_id.model.ItemRecord;

@Repository
public interface ItemRecordRepository extends JpaRepository<ItemRecord, UUID> {
  /**
   * ユーザーIDとレコードIDでItemRecordを取得
   */
  Optional<ItemRecord> findByUserIdAndId(String userId, UUID id);

  /**
   * 指定の入庫のレコードにまだ出庫していない、残り数量を取得
   */
  @Query(value = """
      SELECT ir.quantity - COALESCE(SUM(out_ir.quantity), 0) AS remaining_quantity
      FROM item_record ir
      LEFT JOIN item_record out_ir ON out_ir.item_record_id = ir.id AND out_ir.source = 'OUT'
      WHERE ir.id = :recordId AND ir.source = 'IN'
      GROUP BY ir.id
      """, nativeQuery = true)
  Integer getRemainingQuantityForInRecord(UUID recordId);

  /**
   * アイテムIDに紐づく入庫・出庫レコードの合計数量を取得
   * 入庫は正の数、出庫は負の数として計算
   * 存在しないレコードの場合は0を返す
   */
  @Query(value = """
      SELECT COALESCE(SUM(CASE WHEN ir.source = 'IN' THEN ir.quantity WHEN ir.source = 'OUT' THEN -ir.quantity ELSE 0 END), 0) FROM item_record ir WHERE ir.item_id = :itemId
      """, nativeQuery = true)
  int getItemTotalQuantity(UUID itemId);

  /**
   * IDとユーザーIDでレコードを取得
   */
  Optional<ItemRecord> findByIdAndUserId(UUID id, String userId);

  /**
   * ユーザーIDで全レコードを取得
   * 履歴がない時は空リストで返す
   * createdAtの降順でソート
   */
  @Query(value = """
      SELECT *
      FROM item_record
      WHERE user_id = :userId
      ORDER BY created_at DESC
      """, nativeQuery = true)
  List<ItemRecord> findUserItemRecords(String userId);
}
