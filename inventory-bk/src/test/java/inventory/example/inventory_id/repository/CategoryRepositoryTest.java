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
  private CategoryRepository categoryRepository;

  private Category existedCategory;
  private Category deletedCategory;
  private Category sameNameDiffUser;
  private int userId1 = 1;
  private int userId2 = 2;
  private String book = "book";
  private String electronics = "electronics";

  @BeforeEach
  void setUp() {
    categoryRepository.deleteAll();
    Category category1 = new Category();
    category1.setUserId(userId1);
    category1.setName(book);
    category1.setDeletedFlag(false);

    Category category2 = new Category();
    category2.setUserId(userId1);
    category2.setName(electronics);
    category2.setDeletedFlag(true);

    Category category3 = new Category();
    category3.setUserId(userId2);
    category3.setName(electronics);
    category3.setDeletedFlag(false);

    List<Category> saved = categoryRepository.saveAll(Arrays.asList(category1, category2, category3));
    existedCategory = saved.get(0);
    deletedCategory = saved.get(1);
    sameNameDiffUser = saved.get(2);
  }

  @Test
  @DisplayName("カテゴリーが存在する場合、existsByUserIdAndNameはtrueを返す")
  void testExistsByUserIdAndNameTrue() {
    boolean exists = categoryRepository.existsByUserIdAndName(userId1, book);
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("カテゴリーが存在しない場合、existsByUserIdAndNameはfalseを返す")
  void testExistsByUserIdAndNameFalse() {
    boolean exists = categoryRepository.existsByUserIdAndName(userId2, book);
    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("findNotDeletedは正しいカテゴリを返す")
  void testFindByUserIdIn() {
    List<Category> categories = categoryRepository.findNotDeleted(Arrays.asList(userId1, userId2));
    assertThat(categories).hasSize(2);
    assertThat(categories).extracting(Category::getName).containsExactlyInAnyOrder(book, electronics);
  }

  @Test
  @DisplayName("findUserCategoryは正しいカテゴリを返す")
  void testFindByUserIdAndId() {
    Optional<Category> found = categoryRepository.findUserCategory(List.of(userId1, userId2), existedCategory.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo(book);
  }

  @Test
  @DisplayName("findUserCategoryは削除したものを返さない")
  void testFindByUserIdAndIdWithDeletedId() {
    Optional<Category> found = categoryRepository.findUserCategory(List.of(userId1, userId2), deletedCategory.getId());
    assertThat(found).isNotPresent();
  }

  @Test
  @DisplayName("findUserCategoryは存在しない場合、空を返す")
  void testFindByUserIdAndIdNotFound() {
    Optional<Category> found = categoryRepository.findUserCategory(List.of(userId2), existedCategory.getId());
    assertThat(found).isNotPresent();
  }

  @Test
  @DisplayName("findByUserIdInAndNameは正しいカテゴリを返す")
  void testFindByUserIdInAndName() {
    List<Category> categories = categoryRepository.findActiveCateByName(Arrays.asList(userId1, userId2), book);
    assertThat(categories).hasSize(1);
    assertThat(categories.get(0).getName()).isEqualTo(book);
  }

  @Test
  @DisplayName("findByUserIdInAndNameは削除したものを返さない")
  void testFindByUserIdInAndNameWithDeleted() {
    List<Category> categories = categoryRepository.findActiveCateByName(Arrays.asList(userId1, userId2),
        electronics);
    assertThat(categories).hasSize(1);
    assertThat(categories.get(0).getName()).isEqualTo(electronics);
    assertThat(categories.get(0).getUserId()).isEqualTo(userId2);
  }
}
