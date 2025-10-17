package inventory.example.inventory_id.enums;

/**
 * 在庫トランザクションのタイプを表すEnum。
 * 入庫（IN）と出庫（OUT）の操作を区別するために使用される。
 */
public enum TransactionType {
  /**
   * 入庫操作 - 在庫への商品追加
   */
  IN,

  /**
   * 出庫操作 - 在庫からの商品取り出し
   */
  OUT,
}
