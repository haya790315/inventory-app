package inventory.example.inventory_id.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import inventory.example.inventory_id.model.Category;

@DataJpaTest
@ActiveProfiles("test")
public class CategoryRepositoryTest {

  @Autowired
  private CategoryRepository categoryRepo;

  private Category category1;
  private Category category2;
  private Category category3;
  private int userId1 = 1;
  private int userId2 = 2;
  private String name1 = "book";
  private String name2 = "electronics";
  private String name3 = "groceries";

  @BeforeEach
  void setUp() {
    categoryRepo.deleteAll();
    Category c1 = new Category();
    c1.setUserId(userId1);
    c1.setName(name1);
    c1.setDeletedFlag(false);

    Category c2 = new Category();
    c2.setUserId(userId1);
    c2.setName(name2);
    c2.setDeletedFlag(false);

    Category c3 = new Category();
    c3.setUserId(userId2);
    c3.setName(name3);
    c3.setDeletedFlag(true);

    List<Category> saved = categoryRepo.saveAll(Arrays.asList(c1, c2, c3));
    category1 = saved.get(0);
    category2 = saved.get(1);
    category3 = saved.get(2);
  }

  @Test
  @DisplayName("カテゴリーが存在する場合、existsByUserIdAndNameはtrueを返す")
  void testExistsByUserIdAndNameTrue() {
    boolean exists = categoryRepo.existsByUserIdAndName(userId1, name1);
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("カテゴリーが存在しない場合、existsByUserIdAndNameはfalseを返す")
  void testExistsByUserIdAndNameFalse() {
    boolean exists = categoryRepo.existsByUserIdAndName(userId2, name1);
    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("findByUserIdInは正しいカテゴリを返す")
  void testFindByUserIdIn() {
    List<Category> categories = categoryRepo.findNotDeleted(Arrays.asList(userId1, userId2));
    assertThat(categories).hasSize(2);
    assertThat(categories).extracting(Category::getName).containsExactlyInAnyOrder(name1, name2);
  }

  @Test
  @DisplayName("findByUserIdAndIdは正しいカテゴリを返す")
  void testFindByUserIdAndId() {
    Optional<Category> found = categoryRepo.findUserCategory(userId1, category1.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo(name1);
  }

  @Test
  @DisplayName("findByUserIdAndIdは存在しない場合、空を返す")
  void testFindByUserIdAndIdNotFound() {
    Optional<Category> found = categoryRepo.findUserCategory(userId2, category1.getId());
    assertThat(found).isNotPresent();
  }

  @Test
  @DisplayName("findByUserIdInAndNameは正しいカテゴリを返す")
  void testFindByUserIdInAndName() {
    List<Category> categories = categoryRepo.findByUserIdInAndName(Arrays.asList(userId1, userId2), name1);
    assertThat(categories).hasSize(1);
    assertThat(categories.get(0).getName()).isEqualTo(name1);
  }
}
