
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

  private Category category1;
  private Category category2;
  private Category category3;
  private int userId1 = 1;
  private int userId2 = 2;
  private String name1 = "本";
  private String name2 = "電子機器";
  private String name3 = "食料品";

  @BeforeEach
  void setUp() {
    categoryRepository.deleteAll();
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

    List<Category> saved = categoryRepository.saveAll(Arrays.asList(c1, c2, c3));
    category1 = saved.get(0);
    category2 = saved.get(1);
    category3 = saved.get(2);
  }

  @Test
  @DisplayName("カテゴリーが存在する場合、existsByUserIdAndNameはtrueを返す")
  void testExistsByUserIdAndNameTrue() {
    boolean exists = categoryRepository.existsByUserIdAndName(userId1, name1);
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("カテゴリーが存在しない場合、existsByUserIdAndNameはfalseを返す")
  void testExistsByUserIdAndNameFalse() {
    boolean exists = categoryRepository.existsByUserIdAndName(userId2, name1);
    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("findByUserIdInは正しいカテゴリを返す")
  void testFindByUserIdIn() {
    List<Category> categories = categoryRepository.findByUserIdIn(Arrays.asList(userId1, userId2));
    assertThat(categories).hasSize(3);
    assertThat(categories).extracting(Category::getName).containsExactlyInAnyOrder(name1, name2, name3);
  }

  @Test
  @DisplayName("findByUserIdAndNameは正しいカテゴリを返す")
  void testFindByUserIdAndName() {
    Optional<Category> found = categoryRepository.findByUserIdAndName(userId1, name2);
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo(name2);
  }

  @Test
  @DisplayName("findByUserIdAndNameは存在しない場合、空を返す")
  void testFindByUserIdAndNameNotFound() {
    Optional<Category> found = categoryRepository.findByUserIdAndName(userId2, name1);
    assertThat(found).isNotPresent();
  }

  @Test
  @DisplayName("findByUserIdAndIdは正しいカテゴリを返す")
  void testFindByUserIdAndId() {
    Optional<Category> found = categoryRepository.findByUserIdAndId(userId1, category1.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo(name1);
  }

  @Test
  @DisplayName("findByUserIdAndIdは存在しない場合、空を返す")
  void testFindByUserIdAndIdNotFound() {
    Optional<Category> found = categoryRepository.findByUserIdAndId(userId2, category1.getId());
    assertThat(found).isNotPresent();
  }

  @Test
  @DisplayName("countByUserIdAndDeletedFlagFalseは正しいカウントを返す")
  void testCountByUserIdAndDeletedFlagFalse() {
    int count = categoryRepository.countByUserIdAndDeletedFlagFalse(userId1);
    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("countByUserIdAndDeletedFlagFalseは削除されたカテゴリのみを持つユーザーに対してゼロを返す")
  void testCountByUserIdAndDeletedFlagFalseZero() {
    int count = categoryRepository.countByUserIdAndDeletedFlagFalse(userId2);
    assertThat(count).isEqualTo(0);
  }

  @Test
  @DisplayName("findByUserIdInAndNameは正しいカテゴリを返す")
  void testFindByUserIdInAndName() {
    List<Category> categories = categoryRepository.findByUserIdInAndName(Arrays.asList(userId1, userId2), name1);
    assertThat(categories).hasSize(1);
    assertThat(categories.get(0).getName()).isEqualTo(name1);
  }
}
