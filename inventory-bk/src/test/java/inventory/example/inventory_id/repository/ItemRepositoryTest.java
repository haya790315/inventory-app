package inventory.example.inventory_id.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;

@DataJpaTest
@ActiveProfiles("test")
public class ItemRepositoryTest {
  @Autowired
  private ItemRepository itemRepository;
  @Autowired
  private CategoryRepository categoryRepository;

  private String testUserId = "testUserId";

  private String defaultSystemId = "systemId";

  @Test
  @DisplayName("アクティブなアイテムをカテゴリ名で取得")
  public void testGetActiveByCategoryName() {
    Category categoryPc = new Category("pc");
    categoryPc.setUserId(defaultSystemId);
    categoryRepository.save(categoryPc);
    Item existedNotebook = new Item(
        "Notebook",
        testUserId,
        categoryPc,
        10,
        false);
    existedNotebook.setUpdatedAt(LocalDateTime.now().minusDays(1));
    itemRepository.save(existedNotebook);
    Item existedDesktop = new Item("Desktop",
        testUserId,
        categoryPc,
        5,
        false);
    existedDesktop.setUpdatedAt(LocalDateTime.now());
    itemRepository.save(existedDesktop);

    Item deletedMonitor = new Item(
        "Monitor",
        testUserId,
        categoryPc,
        1,
        true);
    itemRepository.save(deletedMonitor);

    Item anotherUserItem = new Item(
        "Tablet",
        "anotherUserId",
        categoryPc,
        3,
        false);
    itemRepository.save(anotherUserItem);

    List<Item> result = itemRepository
        .getActiveByCategoryName(
            List.of(testUserId, defaultSystemId),
            "pc");

    assertThat(result).hasSize(2);
    // 順番確認
    assertThat(result.get(0)
        .getUpdatedAt()).isAfter(result.get(1).getUpdatedAt());
    // 各アイテムの名前確認
    assertThat(result.get(0)
        .getName()).isEqualTo("Desktop");
    assertThat(result.get(1)
        .getName()).isEqualTo("Notebook");
    // 削除フラグが立っていないこと確認
    assertThat(result.get(0)
        .isDeletedFlag()).isFalse();
    assertThat(result.get(1)
        .isDeletedFlag()).isFalse();
  }

  @Test
  @DisplayName("アクティブなアイテムをカテゴリ名で取得（テストゼロ件）")
  public void testGetActiveByCategoryNameWithZeroResult() {
    List<Item> result = itemRepository.getActiveByCategoryName(
        List.of(testUserId, defaultSystemId),
        "nonexistent");
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("アクティブなアイテムをカテゴリ名で取得失敗（カテゴリーが削除）")
  public void testGetActiveByCategoryNameWithDeletedCategory() {
    Category deletedCategory = new Category("deleted", testUserId);
    deletedCategory.setDeletedFlag(true);
    categoryRepository.save(deletedCategory);

    List<Item> result = itemRepository.getActiveByCategoryName(
        List.of(testUserId, defaultSystemId),
        "deleted");
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("getActiveItemWithId 正しいアイテムを取得できる")
  void testGetActiveItemWithId() {
    // カテゴリのセットアップ
    Category category = new Category("Laptop", defaultSystemId);
    categoryRepository.save(category);

    // ユーザ1アイテムのセットアップ
    Item existedItem = new Item("Notebook", testUserId, category, 5, false);
    itemRepository.save(existedItem);

    // ユーザ2アイテムのセットアップ
    Item notExitedItem = new Item("Macbook", testUserId, category, 1, true);
    itemRepository.save(notExitedItem);

    Item differUserItem = new Item("Notebook", "anotherUserId", category, 10, false);
    differUserItem.setCategory(category);
    differUserItem.setQuantity(10);
    itemRepository.save(differUserItem);

    Optional<Item> resultExisted = itemRepository.getActiveItemWithId(List.of(testUserId, defaultSystemId),
        existedItem.getId());
    assertThat(resultExisted).isPresent();
    assertThat(resultExisted.get().getName()).isEqualTo("Notebook");
    assertThat(resultExisted.get().getCategoryName()).isEqualTo("Laptop");
    assertThat(resultExisted.get().getQuantity()).isEqualTo(5);
    assertThat(resultExisted.get().isDeletedFlag()).isFalse();

    Optional<Item> resultNotExisted = itemRepository.getActiveItemWithId(List.of(testUserId, defaultSystemId),
        notExitedItem.getId());
    assertThat(resultNotExisted).isEmpty();
  }

  @Test
  @DisplayName("getActiveItemWithId（テストゼロ件）")
  void testGetActiveItemWithIdNotFound() {
    Optional<Item> result = itemRepository.getActiveItemWithId(List.of(defaultSystemId), UUID.randomUUID());
    assertThat(result).isEmpty();
  }
}
