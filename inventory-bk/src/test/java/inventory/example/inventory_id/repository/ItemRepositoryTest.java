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
        false);
    existedNotebook.setUpdatedAt(LocalDateTime.now().minusDays(1));
    itemRepository.save(existedNotebook);
    Item existedDesktop = new Item("Desktop",
        testUserId,
        categoryPc,
        false);
    existedDesktop.setUpdatedAt(LocalDateTime.now());
    itemRepository.save(existedDesktop);

    Item deletedMonitor = new Item(
        "Monitor",
        testUserId,
        categoryPc,
        true);
    itemRepository.save(deletedMonitor);

    Item anotherUserItem = new Item(
        "Tablet",
        "anotherUserId",
        categoryPc,
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
    Item existedItem = new Item("Notebook", testUserId, category, false);
    itemRepository.save(existedItem);

    // ユーザ2アイテムのセットアップ
    Item notExitedItem = new Item("Macbook", testUserId, category, true);
    itemRepository.save(notExitedItem);

    Item differUserItem = new Item("Notebook", "anotherUserId", category, false);
    differUserItem.setCategory(category);
    itemRepository.save(differUserItem);

    Optional<Item> resultExisted = itemRepository.getActiveItemWithId(List.of(testUserId, defaultSystemId),
        existedItem.getId());
    assertThat(resultExisted).isPresent();
    assertThat(resultExisted.get().getName()).isEqualTo("Notebook");
    assertThat(resultExisted.get().getCategoryName()).isEqualTo("Laptop");
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

  @Test
  @DisplayName("同じ名前とカテゴリのアクティブなアイテムを取得できる, 削除済みアイテムは除外される, ユーザお互いにのアイテムは影響しない")
  void testGetActiveWithSameNameAndCategory() {
    String sameCategoryName = "SameCategory";
    String sameItemName = "SameItemName";
    Category userCategory = new Category(sameCategoryName, testUserId);
    Category anotherUserCategory = new Category(sameCategoryName, "anotherUserId");
    categoryRepository.saveAll(List.of(userCategory, anotherUserCategory));
    Item userExistingItem = new Item(sameItemName, testUserId, userCategory, false);
    Item userDeletedItem = new Item(sameItemName, testUserId, userCategory, true);
    Item anotherUserItem = new Item(sameItemName, "anotherUserId", anotherUserCategory, false);

    itemRepository.saveAll(List.of(userExistingItem, userDeletedItem, anotherUserItem));

    Optional<Item> result = itemRepository.getActiveWithSameNameAndCategory(
        List.of(testUserId, defaultSystemId),
        sameItemName,
        userCategory.getId());
    assertThat(result).isPresent();
    assertThat(result.get().getUserId()).isEqualTo(testUserId);
    assertThat(result.get().getName()).isEqualTo(sameItemName);
    assertThat(result.get().getCategoryName()).isEqualTo(sameCategoryName);
    assertThat(result.get().isDeletedFlag()).isFalse();
  }

  @Test
  @DisplayName("同じ名前とカテゴリのアクティブなアイテムを取得できる, デフォルトのカテゴリ使用するもユーザお互いに影響しない")
  void testGetActiveWithSameNameAndDefaultCategory() {
    Category defaultCategory = new Category("DefaultCategory", defaultSystemId);
    categoryRepository.save(defaultCategory);
    String sameItemName = "sameItemName";
    Item userExistingItem = new Item(sameItemName, testUserId, defaultCategory, false);
    Item userDeletedItem = new Item(sameItemName, testUserId, defaultCategory, true);
    Item anotherUserItem = new Item(sameItemName, "anotherUserId", defaultCategory, false);

    itemRepository.saveAll(List.of(userExistingItem, userDeletedItem, anotherUserItem));

    Optional<Item> result = itemRepository.getActiveWithSameNameAndCategory(
        List.of(testUserId, defaultSystemId),
        sameItemName,
        defaultCategory.getId());
    assertThat(result).isPresent();
    assertThat(result.get().getUserId()).isEqualTo(testUserId);
    assertThat(result.get().getName()).isEqualTo(sameItemName);
    assertThat(result.get().getCategoryName()).isEqualTo(defaultCategory.getName());
    assertThat(result.get().isDeletedFlag()).isFalse();
  }

  @Test
  @DisplayName("同じ名前とカテゴリのアクティブなアイテムを取得できる- ゼロ件の場合")
  void testGetActiveWithSameNameAndCategory_NoItem() {
    Category defaultCategory = new Category("DefaultCategory", defaultSystemId);
    categoryRepository.save(defaultCategory);

    Optional<Item> result = itemRepository.getActiveWithSameNameAndCategory(
        List.of(testUserId, defaultSystemId),
        "nonexistentItemName",
        defaultCategory.getId());
    assertThat(result).isEmpty();
  }
}
