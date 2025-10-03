package inventory.example.inventory_id.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.model.ItemRecord;

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
        ItemRecord.Source.IN);
    itemRecordRepository.save(testItemInRecord);

    // testUser出庫レコードを作成(5個出庫、残り5個)
    testItemOutRecord = new ItemRecord(
        testUserItem,
        testUserId,
        5,
        ItemRecord.Source.OUT,
        testItemInRecord);
    itemRecordRepository.save(testItemOutRecord);

    // 削除フラグが立っているレコードを作成（対象外）
    deletedInRecord = new ItemRecord(
        testUserItem,
        testUserId,
        3,
        1000,
        null,
        ItemRecord.Source.IN);
    deletedInRecord.setDeletedFlag(true);
    itemRecordRepository.save(deletedInRecord);

    ItemRecord deletedOutRecord = new ItemRecord(
        testUserItem,
        testUserId,
        2,
        ItemRecord.Source.OUT,
        testItemInRecord);
    deletedOutRecord.setDeletedFlag(true);
    itemRecordRepository.save(deletedOutRecord);

    // otherUser入庫レコードを作成（20個、単価2000）
    ItemRecord otherUserRecord = new ItemRecord(
        otherUserItem,
        otherUserId,
        20,
        2000,
        null,
        ItemRecord.Source.IN);
    itemRecordRepository.save(otherUserRecord);

    // otherUser出庫レコードを作成（20個出庫、残り0個）
    ItemRecord otherUserOutRecord = new ItemRecord(
        otherUserItem,
        otherUserId,
        20,
        ItemRecord.Source.OUT,
        otherUserRecord);
    itemRecordRepository.save(otherUserOutRecord);
  }

  @Test
  @Tag("getRecordByUserIdAndId")
  @DisplayName("正しいユーザーIDとIDでアイテムレコードを取得できる")
  void testFindByUserIdAndId_Success() {
    Optional<ItemRecord> result = itemRecordRepository.getRecordByUserIdAndId(testUserId,
        testItemInRecord.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(testItemInRecord.getId());
    assertThat(result.get().getUserId()).isEqualTo(testUserId);
    assertThat(result.get().getQuantity()).isEqualTo(10);
    assertThat(result.get().getPrice()).isEqualTo(1000);
    assertThat(result.get().getSource()).isEqualTo(ItemRecord.Source.IN);

    Optional<ItemRecord> resultOut = itemRecordRepository.getRecordByUserIdAndId(testUserId,
        testItemOutRecord.getId());
    assertThat(resultOut).isPresent();
    assertThat(resultOut.get().getId()).isEqualTo(testItemOutRecord.getId());
    assertThat(resultOut.get().getUserId()).isEqualTo(testUserId);
    assertThat(resultOut.get().getQuantity()).isEqualTo(5);
    assertThat(resultOut.get().getSource()).isEqualTo(ItemRecord.Source.OUT);
  }

  @Test
  @Tag("getRecordByUserIdAndId")
  @DisplayName("削除フラグが立っている場合は取得しない")
  void testFindByUserIdAndId_DeletedFlag() {
    // 削除フラグが立っているレコードを取得しようとする
    Optional<ItemRecord> result = itemRecordRepository.getRecordByUserIdAndId(testUserId,
        deletedInRecord.getId());
    assertThat(result).isNotPresent();
  }

  @Test
  @Tag("getRecordByUserIdAndId")
  @DisplayName("正しいユーザーIDとIDでアイテムレコードを取得- ゼロ件の場合")
  void testFindByUserIdAndId_ZeroRecords() {
    Optional<ItemRecord> result = itemRecordRepository.getRecordByUserIdAndId(testUserId,
        UUID.randomUUID());

    assertThat(result).isNotPresent();
  }

  @Test
  @Tag("getRecordByUserIdAndId")
  @DisplayName("存在しないIDでアイテムレコードを検索すると空のOptionalを返す")
  void testFindByUserIdAndId_NotFound() {
    UUID nonExistId = UUID.randomUUID();

    Optional<ItemRecord> result = itemRecordRepository.getRecordByUserIdAndId(testUserId, nonExistId);

    assertThat(result).isNotPresent();
  }

  @Test
  @Tag("getRecordByUserIdAndId")
  @DisplayName("ユーザーIDとレコードのユーザIDが異なる場合は空のOptionalを返す")
  void testFindByUserIdAndId_DifferentUserRecord() {
    Optional<ItemRecord> result = itemRecordRepository.getRecordByUserIdAndId(testUserId, otherUserItem.getId());
    assertThat(result).isNotPresent();
  }

  @Test
  @Tag("getInrecordRemainQuantity")
  @DisplayName("入庫レコードの残り数量を正しく計算する")
  void testGetRemainingQuantityForInRecord_NoOutRecords() {
    Integer remainingQuantity = itemRecordRepository.getInrecordRemainQuantity(testItemInRecord.getId());
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
        ItemRecord.Source.OUT,
        testItemInRecord);
    itemRecordRepository.save(outRecord1);

    Integer remainingQuantity = itemRecordRepository.getInrecordRemainQuantity(testItemInRecord.getId());

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
        ItemRecord.Source.OUT,
        testItemInRecord);
    itemRecordRepository.save(outRecord);

    Integer remainingQuantity = itemRecordRepository.getInrecordRemainQuantity(testItemInRecord.getId());

    assertThat(remainingQuantity).isEqualTo(0);
  }

  @Test
  @Tag("getInrecordRemainQuantity")
  @DisplayName("レコードが存在しない場合の残り数量はnullを返す")
  void testGetRemainingQuantityForInRecord_NoInRecords() {
    Integer remainingQuantity = itemRecordRepository.getInrecordRemainQuantity(UUID.randomUUID());
    assertThat(remainingQuantity).isNull();
  }

  @Test
  @Tag("getInrecordRemainQuantity")
  @DisplayName("出庫レコードIDを指定した場合はnullを返す")
  void testGetRemainingQuantityForInRecord_WithOutRecord() {
    Integer remainingQuantity = itemRecordRepository.getInrecordRemainQuantity(testItemOutRecord.getId());
    assertThat(remainingQuantity).isNull();
  }

  @Test
  @Tag("getItemTotalQuantity")
  @DisplayName("アイテムの合計数量を正しく計算する")
  void testGetItemTotalQuantity_InOnly() {
    Integer totalQuantity = itemRecordRepository.getItemTotalQuantity(testUserItem.getId());

    assertThat(totalQuantity).isEqualTo(5);
  }

  @Test
  @Tag("getItemTotalQuantity")
  @DisplayName("存在しないアイテムIDの合計数量は0を返す")
  void testGetItemTotalQuantity_NonExistentItem() {
    UUID nonExistentItemId = UUID.randomUUID();

    Integer totalQuantity = itemRecordRepository.getItemTotalQuantity(nonExistentItemId);

    assertThat(totalQuantity).isEqualTo(0);
  }

  @Test
  @DisplayName("入力履歴がない場合、アイテムの合計数量は0を返す")
  void testGetItemTotalQuantity_NonExistentItemRecord() {
    Category newCategory = new Category("newCategory", testUserId);
    categoryRepository.save(newCategory);
    Item newItem = new Item("New Item", testUserId, newCategory, false);

    int totalQuantity = itemRecordRepository.getItemTotalQuantity(newItem.getId());

    assertThat(totalQuantity).isEqualTo(0);
  }
}
