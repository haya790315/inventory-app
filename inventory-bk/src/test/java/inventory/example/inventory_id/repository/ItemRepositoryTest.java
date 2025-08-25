package inventory.example.inventory_id.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;

@DataJpaTest
class ItemRepositoryTest {

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Test
  @DisplayName("findByUserIdInAndCategory_Name 正しいアイテムを取得できる")
  void testFindByUserIdInAndCategoryName() {
    // カテゴリのセットアップ
    Category category = new Category();
    category.setName("Laptop");
    category.setUserId(123);
    categoryRepository.save(category);

    // アイテムのセットアップ
    Item item = new Item();
    item.setName("Notebook");
    item.setUserId(123);
    item.setCategory(category);
    item.setQuantity(5);
    itemRepository.save(item);

    Optional<List<Item>> result = itemRepository.findByUserIdInAndCategory_Name(List.of(123), "Laptop");
    assertThat(result).isPresent();
    assertThat(result.get()).hasSize(1);
    assertThat(result.get().get(0).getName()).isEqualTo("Notebook");
    assertThat(result.get().get(0).getCategoryName()).isEqualTo("Laptop");
  }

  @Test
  @DisplayName("findByUserIdInAndIdAndDeletedFlagFalse 正しいアイテムを取得できる")
  void testFindByUserIdInAndIdAndDeletedFlagFalse() {
    // カテゴリのセットアップ
    Category category = new Category();
    category.setName("Laptop");
    category.setUserId(999);
    categoryRepository.save(category);

    // ユーザ1アイテムのセットアップ
    Item item = new Item();
    item.setName("Notebook");
    item.setUserId(123);
    item.setCategory(category);
    item.setQuantity(5);
    itemRepository.save(item);

    // ユーザ2アイテムのセットアップ
    Item item2 = new Item();
    item2.setName("Macbook");
    item2.setUserId(123);
    item2.setCategory(category);
    item2.setQuantity(1);
    item2.setDeletedFlag(true);
    itemRepository.save(item2);

    Item item3 = new Item();
    item3.setName("Notebook");
    item3.setUserId(321);
    item3.setCategory(category);
    item3.setQuantity(10);
    itemRepository.save(item3);

    Optional<Item> result = itemRepository.findByUserIdInAndIdAndDeletedFlagFalse(List.of(123, 999), item.getId());
    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Notebook");
    assertThat(result.get().getCategoryName()).isEqualTo("Laptop");
    assertThat(result.get().getQuantity()).isEqualTo(5);
  }

  @Test
  @DisplayName("findByUserIdInAndIdAndDeletedFlagFalse 存在しない場合は空")
  void testFindByUserIdInAndIdAndDeletedFlagFalseNotFound() {
    Optional<Item> result = itemRepository.findByUserIdInAndIdAndDeletedFlagFalse(List.of(999), UUID.randomUUID());
    assertThat(result).isEmpty();
  }
}
