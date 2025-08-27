package inventory.example.inventory_id.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
    // Setup category
    Category category = new Category();
    category.setName("PC");
    category.setUserId(123);
    categoryRepository.save(category);

    // Setup item
    Item notDeleteditem = new Item();
    notDeleteditem.setName("Notebook");
    notDeleteditem.setUserId(123);
    notDeleteditem.setCategory(category);
    notDeleteditem.setQuantity(5);
    itemRepository.save(notDeleteditem);
    Item deleteditem = new Item();
    deleteditem.setName("Desktop");
    deleteditem.setUserId(123);
    deleteditem.setCategory(category);
    deleteditem.setQuantity(5);
    deleteditem.setDeletedFlag(true);
    itemRepository.save(deleteditem);

    // Test
    List<Item> result = itemRepository.getItemsByCategoryName(List.of(123), "PC");
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Notebook");
    assertThat(result.get(0).getCategoryName()).isEqualTo("PC");
  }
}
