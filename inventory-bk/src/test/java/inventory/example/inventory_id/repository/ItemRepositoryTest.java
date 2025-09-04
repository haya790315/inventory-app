package inventory.example.inventory_id.repository;

import java.time.LocalDateTime;
import java.util.List;

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

  private int userId = 111;

  private int systemUserId = 999;

  @Test
  @DisplayName("アクティブなアイテムをカテゴリ名で取得")
  public void testGetActiveByCategoryName() {
    Category categoryPc = new Category("pc");
    categoryPc.setUserId(systemUserId);
    categoryRepository.save(categoryPc);
    Item existedNotebook = new Item(
        "Notebook",
        userId,
        categoryPc,
        10,
        false);
    existedNotebook.setUpdatedAt(LocalDateTime.now().minusDays(1));
    itemRepository.save(existedNotebook);
    Item existedDesktop = new Item("Desktop",
        userId,
        categoryPc,
        5,
        false);
    existedDesktop.setUpdatedAt(LocalDateTime.now());
    itemRepository.save(existedDesktop);

    Item deletedMonitor = new Item(
        "Monitor",
        userId,
        categoryPc,
        1,
        true);
    itemRepository.save(deletedMonitor);

    Item anotherUserItem = new Item(
        "Tablet",
        222,
        categoryPc,
        3,
        false);
    itemRepository.save(anotherUserItem);

    List<Item> result = itemRepository.getActiveByCategoryName(
        List.of(userId, systemUserId),
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
        List.of(userId, systemUserId),
        "nonexistent");
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("アクティブなアイテムをカテゴリ名で取得失敗（カテゴリーが削除）")
  public void testGetActiveByCategoryNameWithDeletedCategory() {
    Category deletedCategory = new Category("deleted", userId);
    deletedCategory.setDeletedFlag(true);
    categoryRepository.save(deletedCategory);

    List<Item> result = itemRepository.getActiveByCategoryName(
        List.of(userId, systemUserId),
        "deleted");
    assertThat(result).isEmpty();
  }
}
