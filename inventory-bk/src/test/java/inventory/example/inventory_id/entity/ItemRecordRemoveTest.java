package inventory.example.inventory_id.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.model.ItemRecord;
import inventory.example.inventory_id.repository.ItemRecordRepository;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ItemRecord OrphanRemoval Tests")
public class ItemRecordRemoveTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ItemRecordRepository itemRecordRepository;

  private String testUserId;
  private Category testCategory;
  private Item testItem;
  private ItemRecord testParentInRecord;
  private ItemRecord testChildrenOutRecord1;
  private ItemRecord testChildrenOutRecord2;

  @BeforeEach
  void setUp() {
    testUserId = "test-user";

    testCategory = new Category("category", testUserId);
    entityManager.persistAndFlush(testCategory);

    testItem = new Item("parent", testUserId, testCategory, false);
    entityManager.persistAndFlush(testItem);

    // 入庫履歴を作成
    testParentInRecord = new ItemRecord(
        testItem, testUserId, 100, 1000,
        LocalDate.now().plusDays(30),
        ItemRecord.Source.IN);
    testParentInRecord.setChildRecords(new ArrayList<>());
    entityManager.persistAndFlush(testParentInRecord);

    // 出庫履歴を作成
    testChildrenOutRecord1 = new ItemRecord(
        testItem, testUserId, 10,
        ItemRecord.Source.OUT, testParentInRecord);
    testChildrenOutRecord2 = new ItemRecord(
        testItem, testUserId, 20,
        ItemRecord.Source.OUT, testParentInRecord);

    testParentInRecord.getChildRecords().add(testChildrenOutRecord1);
    testParentInRecord.getChildRecords().add(testChildrenOutRecord2);

    entityManager.persistAndFlush(testParentInRecord);
    entityManager.clear();
  }

  @Test
  @DisplayName("子レコードを明示的に削除する")
  void testExplicitDeletion_whenChildDeleted_shouldRemoveFromDatabase() {
    // チェック初期状態
    List<ItemRecord> allRecords = itemRecordRepository.findAll();
    assertThat(allRecords).hasSize(3);

    ItemRecord parentRecord = itemRecordRepository.findById(testParentInRecord.getId()).get();
    assertThat(parentRecord.getChildRecords()).hasSize(2);

    // 子レコードを取得
    ItemRecord childToDelete = parentRecord.getChildRecords().get(0);
    UUID childToDeleteId = childToDelete.getId();

    // 削除作業
    parentRecord.getChildRecords().remove(childToDelete);
    itemRecordRepository.delete(childToDelete);

    // 検証
    // データベースに残っているレコードは2件
    List<ItemRecord> remainingRecords = itemRecordRepository.findAll();
    assertThat(remainingRecords).hasSize(2);

    // 削除された子レコードが存在しないことを確認
    Optional<ItemRecord> deletedChild = itemRecordRepository.findById(childToDeleteId);
    assertThat(deletedChild).isEmpty();

    // 親レコードは1件の子レコードを持つことを確認
    ItemRecord finalParent = itemRecordRepository.findById(testParentInRecord.getId()).get();
    assertThat(finalParent.getChildRecords()).hasSize(1);
  }

  @Test
  @DisplayName("親レコードを削除すると全ての子も削除される")
  void testOrphanRemoval_whenParentDeleted_shouldDeleteAllChildren() {
    // チェック初期状態
    assertThat(itemRecordRepository.findAll()).hasSize(3);

    itemRecordRepository.deleteById(testParentInRecord.getId());

    // 検証
    // データベースに残っているレコードは0件
    List<ItemRecord> remainingRecords = itemRecordRepository.findAll();
    assertThat(remainingRecords).isEmpty();
  }
}
