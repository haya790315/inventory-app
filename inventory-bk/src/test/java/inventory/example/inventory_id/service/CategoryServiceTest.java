package inventory.example.inventory_id.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.repository.CategoryRepository;
import inventory.example.inventory_id.request.CategoryRequest;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

  @Mock
  private CategoryRepository categoryRepository;

  @InjectMocks
  private CategoryService categoryService;

  @Test
  @DisplayName("カテゴリー作成成功")
  void testCreateCategorySuccess() {
    CategoryRequest request = new CategoryRequest();
    request.setName("TestCategory");
    int userId = 1;

    when(categoryRepository.existsByUserIdAndName(userId, "TestCategory")).thenReturn(false);

    Category savedCategory = new Category();
    savedCategory.setName("TestCategory");
    savedCategory.setUserId(userId);

    when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

    Category result = categoryService.createCategory(request, userId);

    assertEquals("TestCategory", result.getName());
    assertEquals(userId, result.getUserId());
  }

  @Test
  @DisplayName("カテゴリー名がすでに存在する場合のエラー")
  void testCreateCategoryAlreadyExists() {
    CategoryRequest request = new CategoryRequest();
    request.setName("TestCategory");
    int userId = 1;

    when(categoryRepository.existsByUserIdAndName(userId, "TestCategory")).thenReturn(true);
    when(categoryRepository.findByUserIdAndName(userId, "TestCategory")).thenReturn(Optional.of(new Category()));

    Exception exception = assertThrows(ResponseStatusException.class, () -> {
      categoryService.createCategory(request, userId);
    });

    assertEquals("カテゴリー名はすでに存在します", ((ResponseStatusException) exception).getReason());
  }

  @Test
  @DisplayName("削除済みカテゴリーの再作成")
  void testCreateCategoryAlreadyDeleted() {
    CategoryRequest request = new CategoryRequest();
    request.setName("TestCategory");
    int userId = 1;

    when(categoryRepository.existsByUserIdAndName(userId, "TestCategory")).thenReturn(true);
    Category existingCategory = new Category("TestCategory");
    existingCategory.setDeletedFlag(true);
    when(categoryRepository.findByUserIdAndName(userId, "TestCategory")).thenReturn(Optional.of(existingCategory));
    when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);

    Category result = categoryService.createCategory(request, userId);
    assertFalse(result.isDeletedFlag());
  }

  @Test
  @DisplayName("登録できるカテゴリの上限に達している場合のエラー")
  void testCreateCategoryLimitExceeded() {
    CategoryRequest request = new CategoryRequest();
    request.setName("TestCategory");
    int userId = 1;

    when(categoryRepository.countByUserIdAndDeletedFlagFalse(userId)).thenReturn(50);

    Exception exception = assertThrows(ResponseStatusException.class, () -> {
      categoryService.createCategory(request, userId);
    });

    assertEquals("登録できるカテゴリの上限に達しています", ((ResponseStatusException) exception).getReason());
  }

  @Test
  @DisplayName("カテゴリーアップデート成功")
  void testUpdateCategorySuccess() {
    UUID categoryId = UUID.randomUUID();
    CategoryRequest request = new CategoryRequest();
    request.setName("UpdatedName");
    int userId = 1;

    Category category = new Category("OldName");
    category.setUserId(userId);
    when(categoryRepository.findByUserIdAndId(userId, categoryId)).thenReturn(Optional.of(category));
    when(categoryRepository.save(any(Category.class))).thenReturn(category);

    Category result = categoryService.updateCategory(categoryId, request, userId);
    assertEquals("UpdatedName", result.getName());
  }

  @Test
  @DisplayName("アップデートしたいデータがない場合のエラー")
  void testUpdateCategoryNotFound() {
    UUID categoryId = UUID.randomUUID();
    CategoryRequest request = new CategoryRequest();
    request.setName("UpdatedName");
    int userId = 1;

    when(categoryRepository.findByUserIdAndId(userId, categoryId)).thenReturn(Optional.empty());

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      categoryService.updateCategory(categoryId, request, userId);
    });

    assertEquals("カテゴリーが見つかりません", exception.getMessage());
  }

  @Test
  @DisplayName("カテゴリー削除成功")
  void testDeleteCategorySuccess() {
    UUID categoryId = UUID.randomUUID();
    int userId = 1;
    Category category = new Category();
    category.setUserId(userId);
    category.setItems(new ArrayList<>());
    when(categoryRepository.findByUserIdAndId(userId, categoryId)).thenReturn(Optional.of(category));
    when(categoryRepository.save(any(Category.class))).thenReturn(category);

    assertDoesNotThrow(() -> categoryService.deleteCategory(categoryId, userId));
    assertTrue(category.isDeletedFlag());
  }

  @Test
  @DisplayName("カテゴリー削除時にアイテムが存在する場合のエラー")
  void testDeleteCategoryHasItems() {
    UUID categoryId = UUID.randomUUID();
    int userId = 1;
    Category category = new Category();
    category.setUserId(userId);
    ArrayList<Item> items = new ArrayList<>();
    items.add(new Item());
    category.setItems(items);
    when(categoryRepository.findByUserIdAndId(userId, categoryId)).thenReturn(Optional.of(category));

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      categoryService.deleteCategory(categoryId, userId);
    });

    assertEquals("アイテムが存在するため削除できません", exception.getMessage());
  }
}