package inventory.example.inventory_id.repository;

import static org.assertj.core.api.Assertions.assertThat;

import inventory.example.inventory_id.enums.TransactionType;
import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.model.ItemRecord;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
public class ItemRecordRepositoryTest {

  @Autowired
  private ItemRecordRepository itemRecordRepository;

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  private String testUserId = "testUserId";
  private String otherUserId = "otherUserId";
  private ItemRecord testItemInRecord;
  private ItemRecord testItemOutRecord;
  private ItemRecord deletedInRecord;
  private Item testUserItem;
  private Item otherUserItem;

  @BeforeEach
  void setUp() {
    // データベースをクリーンアップ
    itemRecordRepository.deleteAll();
    itemRepository.deleteAll();
    categoryRepository.deleteAll();

    // テスト用のカテゴリーを作成
    Category testCategory = new Category("testCategory", "System");
    categoryRepository.save(testCategory);

    // テスト用のアイテムを作成
    testUserItem = new Item("Test Item", testUserId, testCategory, false);
    itemRepository.save(testUserItem);

    // 別ユーザーの同じ名前とカテゴリのアイテムを作成
    otherUserItem = new Item("Test Item", otherUserId, testCategory, false);
    itemRepository.save(otherUserItem);

    // testUser入庫レコードを作成(10個、単価1000)
    testItemInRecord = new ItemRecord(
      testUserItem,
      testUserId,
      10,
      1000,
      null,
      TransactionType.IN
    );
    itemRecordRepository.save(testItemInRecord);

    // testUser出庫レコードを作成(5個出庫、残り5個)
    testItemOutRecord = new ItemRecord(
      testUserItem,
      testUserId,
      5,
      testItemInRecord.getPrice(),
      testItemInRecord.getExpirationDate(),
      TransactionType.OUT,
      testItemInRecord
    );
    itemRecordRepository.save(testItemOutRecord);

    // 削除フラグが立っているレコードを作成（対象外）
    deletedInRecord = new ItemRecord(
      testUserItem,
      testUserId,
      3,
      1000,
      null,
      TransactionType.IN
    );
    deletedInRecord.setDeletedFlag(true);
    itemRecordRepository.save(deletedInRecord);

    ItemRecord deletedOutRecord = new ItemRecord(
      testUserItem,
      testUserId,
      2,
      TransactionType.OUT,
      testItemInRecord
    );
    deletedOutRecord.setDeletedFlag(true);
    itemRecordRepository.save(deletedOutRecord);

    // otherUser入庫レコードを作成（20個、単価2000）
    ItemRecord otherUserRecord = new ItemRecord(
      otherUserItem,
      otherUserId,
      20,
      2000,
      null,
      TransactionType.IN
    );
    itemRecordRepository.save(otherUserRecord);

    // otherUser出庫レコードを作成（20個出庫、残り0個）
    ItemRecord otherUserOutRecord = new ItemRecord(
      otherUserItem,
      otherUserId,
      20,
      otherUserRecord.getPrice(),
      otherUserRecord.getExpirationDate(),
      TransactionType.OUT,
      otherUserRecord
    );
    itemRecordRepository.save(otherUserOutRecord);
  }

  @Test
  @Tag("getRecordByUserIdAndId")
  @DisplayName("正しいユーザーIDとIDでアイテムレコードを取得できる")
  void testFindByUserIdAndId_Success() {
    Optional<ItemRecord> result = itemRecordRepository.getRecordByUserIdAndId(
      testUserId,
      testItemInRecord.getId()
    );

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(testItemInRecord.getId());
    assertThat(result.get().getUserId()).isEqualTo(testUserId);
    assertThat(result.get().getQuantity()).isEqualTo(10);
    assertThat(result.get().getPrice()).isEqualTo(1000);
    assertThat(result.get().getTransactionType()).isEqualTo(TransactionType.IN);

    Optional<ItemRecord> resultOut =
      itemRecordRepository.getRecordByUserIdAndId(
        testUserId,
        testItemOutRecord.getId()
      );
    assertThat(resultOut).isPresent();
    assertThat(resultOut.get().getId()).isEqualTo(testItemOutRecord.getId());
    assertThat(resultOut.get().getUserId()).isEqualTo(testUserId);
    assertThat(resultOut.get().getQuantity()).isEqualTo(5);
    assertThat(resultOut.get().getTransactionType()).isEqualTo(
      TransactionType.OUT
    );
  }

  @Test
  @Tag("getRecordByUserIdAndId")
  @DisplayName("削除フラグが立っている場合は取得しない")
  void testFindByUserIdAndId_DeletedFlag() {
    // 削除フラグが立っているレコードを取得しようとする
    Optional<ItemRecord> result = itemRecordRepository.getRecordByUserIdAndId(
      testUserId,
      deletedInRecord.getId()
    );
    assertThat(result).isNotPresent();
  }

  @Test
  @Tag("getRecordByUserIdAndId")
  @DisplayName("正しいユーザーIDとIDでアイテムレコードを取得- ゼロ件の場合")
  void testFindByUserIdAndId_ZeroRecords() {
    Optional<ItemRecord> result = itemRecordRepository.getRecordByUserIdAndId(
      testUserId,
      Long.MAX_VALUE
    );

    assertThat(result).isNotPresent();
  }

  @Test
  @Tag("getRecordByUserIdAndId")
  @DisplayName("存在しないIDでアイテムレコードを検索すると空のOptionalを返す")
  void testFindByUserIdAndId_NotFound() {
    Long nonExistId = Long.MAX_VALUE;

    Optional<ItemRecord> result = itemRecordRepository.getRecordByUserIdAndId(
      testUserId,
      nonExistId
    );

    assertThat(result).isNotPresent();
  }

  @Test
  @Tag("getRecordByUserIdAndId")
  @DisplayName("ユーザーIDとレコードのユーザIDが異なる場合は空のOptionalを返す")
  void testFindByUserIdAndId_DifferentUserRecord() {
    Optional<ItemRecord> result = itemRecordRepository.getRecordByUserIdAndId(
      otherUserId,
      testItemInRecord.getId()
    );
    assertThat(result).isNotPresent();
  }

  @Test
  @Tag("getInrecordRemainQuantity")
  @DisplayName("入庫レコードの残り数量を正しく計算する")
  void testGetRemainingQuantityForInRecord_NoOutRecords() {
    Integer remainingQuantity = itemRecordRepository.getInrecordRemainQuantity(
      testItemInRecord.getId()
    );
    assertThat(remainingQuantity).isEqualTo(5);
  }

  @Test
  @Tag("getInrecordRemainQuantity")
  @DisplayName("入庫レコードの残り数量を正しく計算する - 複数の出庫がある場合")
  void testGetRemainingQuantityForInRecord_WithMultipleOut() {
    // 複数の出庫レコードを作成
    ItemRecord outRecord1 = new ItemRecord(
      testUserItem,
      testUserId,
      3,
      TransactionType.OUT,
      testItemInRecord
    );
    itemRecordRepository.save(outRecord1);

    Integer remainingQuantity = itemRecordRepository.getInrecordRemainQuantity(
      testItemInRecord.getId()
    );

    assertThat(remainingQuantity).isEqualTo(2);
  }

  @Test
  @Tag("getInrecordRemainQuantity")
  @DisplayName("入庫レコードの残り数量を正しく計算する - 完全に出庫された場合")
  void testGetRemainingQuantityForInRecord_FullyOut() {
    ItemRecord outRecord = new ItemRecord(
      testUserItem,
      testUserId,
      5,
      TransactionType.OUT,
      testItemInRecord
    );
    itemRecordRepository.save(outRecord);

    Integer remainingQuantity = itemRecordRepository.getInrecordRemainQuantity(
      testItemInRecord.getId()
    );

    assertThat(remainingQuantity).isEqualTo(0);
  }

  @Test
  @Tag("getInrecordRemainQuantity")
  @DisplayName("レコードが存在しない場合の残り数量はnullを返す")
  void testGetRemainingQuantityForInRecord_NoInRecords() {
    Integer remainingQuantity = itemRecordRepository.getInrecordRemainQuantity(
      Long.MAX_VALUE
    );
    assertThat(remainingQuantity).isNull();
  }

  @Test
  @Tag("getInrecordRemainQuantity")
  @DisplayName("出庫レコードIDを指定した場合はnullを返す")
  void testGetRemainingQuantityForInRecord_WithOutRecord() {
    Integer remainingQuantity = itemRecordRepository.getInrecordRemainQuantity(
      testItemOutRecord.getId()
    );
    assertThat(remainingQuantity).isNull();
  }

  @Test
  @Tag("getItemTotalQuantity")
  @DisplayName("アイテムの合計数量を正しく計算する")
  void testGetItemTotalQuantity_InOnly() {
    Integer totalQuantity = itemRecordRepository.getItemTotalQuantity(
      testUserItem.getId()
    );

    assertThat(totalQuantity).isEqualTo(5);
  }

  @Test
  @Tag("getItemTotalQuantity")
  @DisplayName("存在しないアイテムIDの合計数量は0を返す")
  void testGetItemTotalQuantity_NonExistentItem() {
    UUID nonExistentItemId = UUID.randomUUID();

    Integer totalQuantity = itemRecordRepository.getItemTotalQuantity(
      nonExistentItemId
    );

    assertThat(totalQuantity).isEqualTo(0);
  }

  @Test
  @DisplayName("入力履歴がない場合、アイテムの合計数量は0を返す")
  void testGetItemTotalQuantity_NonExistentItemRecord() {
    Category newCategory = new Category("newCategory", testUserId);
    categoryRepository.save(newCategory);
    Item newItem = new Item("New Item", testUserId, newCategory, false);

    int totalQuantity = itemRecordRepository.getItemTotalQuantity(
      newItem.getId()
    );

    assertThat(totalQuantity).isEqualTo(0);
  }

  @Test
  @DisplayName("IDとユーザーIDでレコードを取得成功")
  void testFindByIdAndUserId_Success() {
    Optional<ItemRecord> result = itemRecordRepository.findByIdAndUserId(
      testItemInRecord.getId(),
      testUserId
    );
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(testItemInRecord);
  }

  @Test
  @DisplayName("IDとユーザーIDでレコードを取得失敗- ユーザーIDが異なる場合")
  void testFindByIdAndUserId_Failure_DifferentUser() {
    Optional<ItemRecord> result = itemRecordRepository.findByIdAndUserId(
      testItemInRecord.getId(),
      otherUserId
    );
    assertThat(result).isNotPresent();
  }

  @Test
  @Tag("findUserItemRecords")
  @DisplayName("ユーザーIDで全レコードを取得成功 - 削除されていないレコードのみ、順番はcreatedAtの降順")
  void testFindUserItemRecords_Success() {
    ItemRecord latestInRecord = new ItemRecord(
      testUserItem,
      testUserId,
      15,
      1500,
      null,
      TransactionType.IN
    );
    itemRecordRepository.save(latestInRecord);
    var results = itemRecordRepository.findUserItemRecords(testUserId);
    assertThat(results).hasSize(3);
    assertThat(results).containsExactly(
      latestInRecord,
      testItemOutRecord,
      testItemInRecord
    );
  }

  @Test
  @Tag("findUserItemRecords")
  @DisplayName("ユーザーIDで全レコードを取得成功(履歴なし) ")
  void testFindUserItemRecords_Empty() {
    var results = itemRecordRepository.findUserItemRecords("noRecordUser");
    assertThat(results).isEmpty();
  }

  @Test
  @Tag("findAllByItemIdAndUserId")
  @DisplayName("アイテムIDとユーザーIDで全レコードを取得成功 - 削除されていないレコードのみ、順番はcreatedAtの降順")
  void testFindAllByItemIdAndUserId_Success() {
    var results = itemRecordRepository.getRecordsByItemIdAndUserId(testUserItem.getId(), testUserId);
    assertThat(results).hasSize(2);
    assertThat(results).containsExactly(testItemOutRecord, testItemInRecord);
  }

  @Test
  @Tag("findAllByItemIdAndUserId")
  @DisplayName("アイテムIDとユーザーIDで全レコードを取得成功 - ゼロ件場合")
  void testFindAllByItemIdAndUserId_Empty() {
    Category newCategory = new Category("Category", testUserId);
    categoryRepository.save(newCategory);
    Item newItem = new Item("Test Item", testUserId, newCategory, false);
    itemRepository.save(newItem);
    var results = itemRecordRepository.getRecordsByItemIdAndUserId(newItem.getId(), testUserId);
    assertThat(results).isEmpty();
  }

  @Test
  @Tag("getItemTotalPriceAndQuantity")
  @DisplayName("アイテムの合計数量と合計金額を正しく集計する - INとOUT混在")
  void testGetItemTotalPriceAndQuantity_SumsInAndOut() {
    // 現在の setup では testUserItem に入庫10*1000 と 出庫5*1000 の組み合わせがある (合計数量=5, 合計金額=10*1000 - 5*1000 = 5000)
    // ここに追加で IN 5*200 を追加し OUT 4*200 を追加して合算を検証する
    var anotherIn = new ItemRecord(
      testUserItem,
      testUserId,
      5,
      200,
      null,
      TransactionType.IN
    );
    itemRecordRepository.save(anotherIn);

    var anotherOut = new ItemRecord(
      testUserItem,
      testUserId,
      4,
      anotherIn.getPrice(),
      anotherIn.getExpirationDate(),
      TransactionType.OUT,
      anotherIn
    );
    itemRecordRepository.save(anotherOut);

    var itemSummary = itemRecordRepository.getItemTotalPriceAndQuantity(
      testUserId,
      testUserItem.getId()
    );

    assertThat(itemSummary.getTotalQuantity()).isEqualTo(6);
    assertThat(itemSummary.getTotalPrice()).isEqualTo(5200);
  }

  @Test
  @Tag("getItemTotalPriceAndQuantity")
  @DisplayName("削除フラグが立っているレコードは集計から除外される")
  void testGetItemTotalPriceAndQuantity_IgnoresDeleted() {
    var deletedRecord = new ItemRecord(
      testUserItem,
      testUserId,
      7,
      500,
      null,
      TransactionType.IN
    );
    deletedRecord.setDeletedFlag(true);
    itemRecordRepository.save(deletedRecord);

    var itemSummary = itemRecordRepository.getItemTotalPriceAndQuantity(
      testUserId,
      testUserItem.getId()
    );

    assertThat(itemSummary.getTotalQuantity()).isEqualTo(5);
    assertThat(itemSummary.getTotalPrice()).isEqualTo(5000);
  }

  @Test
  @Tag("getItemTotalPriceAndQuantity")
  @DisplayName("レコードが存在しないアイテムIDの場合はゼロを返す")
  void testGetItemTotalPriceAndQuantity_NoRecords() {
    UUID nonExist = UUID.randomUUID();
    var itemSummary = itemRecordRepository.getItemTotalPriceAndQuantity(
      testUserId,
      nonExist
    );
    assertThat(itemSummary.getTotalQuantity()).isEqualTo(0);
    assertThat(itemSummary.getTotalPrice()).isEqualTo(0);
  }
}
