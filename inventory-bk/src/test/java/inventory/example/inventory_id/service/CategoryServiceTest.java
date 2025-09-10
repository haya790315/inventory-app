package inventory.example.inventory_id.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import inventory.example.inventory_id.dto.CategoryDto;
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
  private String testUserId = "testUserId";
  private String defaultSystemId = "systemId";

  private String categoryNotFoundMsg = "カテゴリーが見つかりません";

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    ReflectionTestUtils.setField(categoryService, "systemUserId", defaultSystemId);
  }

  @Test
  @Tag("getCategory")
  @DisplayName("カテゴリー取得- 取得成功")
  void testGetAllCategoriesSuccess() {
    String userId = testUserId;
    Category category1 = new Category();
    category1.setName("CategoryB");
    category1.setId(UUID.randomUUID());
    category1.setUserId(userId);

    Category category2 = new Category();
    category2.setName("CategoryA");
    category2.setId(UUID.randomUUID());
    category2.setUserId(userId);
    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenReturn(List.of(category1, category2));

    List<CategoryDto> result = categoryService.getAllCategories(userId);
    assertFalse(result.isEmpty());
    assertEquals(result.size(), 2);
    assertEquals(result.get(0).getName(), "CategoryA");
    assertEquals(result.get(1).getName(), "CategoryB");
  }

  @Test
  @Tag("getCategory")
  @DisplayName("カテゴリー取得- 件数0件の場合")
  void testGetAllCategoriesNoResults() {
    String userId = testUserId;
    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenReturn(List.of());
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      categoryService.getAllCategories(userId);
    });
    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    assertEquals(categoryNotFoundMsg, exception.getReason());
  }

  @Test
  @Tag("getCategory/items")
  @DisplayName("アイテム取得- アイテムを取得成功")
  void testGetCategoryItemsSuccess() {
    UUID categoryId = UUID.randomUUID();
    String userId = testUserId;
    Category category = new Category();
    category.setId(categoryId);
    category.setUserId(userId);
    category.setItems(List.of(new Item("Item1")));
    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenReturn(List.of(category));

    List<Item> result = categoryService.getCategoryItems(testUserId, categoryId);
    assertFalse(result.isEmpty());
    assertEquals(category.getItems(), result);
  }

  @Test
  @Tag("getCategory/items")
  @DisplayName("アイテム取得- アイテムが空の場合")
  void testGetCategoryItemsEmpty() {
    UUID categoryId = UUID.randomUUID();
    String userId = testUserId;
    Category category = new Category();
    category.setId(categoryId);
    category.setUserId(userId);
    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenReturn(List.of(category));

    List<Item> result = categoryService.getCategoryItems(testUserId, categoryId);
    assertTrue(result.isEmpty());
  }

  @Test
  @Tag("createCategory")
  @DisplayName("カテゴリー作成成功")
  void testCreateCategorySuccess() {
    CategoryRequest request = new CategoryRequest();
    request.setName("TestCategory");
    String userId = testUserId;

    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId))).thenReturn(List.of());

    Category savedCategory = new Category();
    savedCategory.setName(request.getName());
    savedCategory.setUserId(userId);

    when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

    Category result = categoryService.createCategory(request, userId);

    assertEquals(request.getName(), result.getName());
    assertEquals(userId, result.getUserId());
  }

  @Test
  @Tag("createCategory")
  @DisplayName("カテゴリー名がすでに存在する場合のエラー")
  void testCreateCategoryAlreadyExists() {
    CategoryRequest request = new CategoryRequest();
    request.setName("TestCategory");
    String userId = testUserId;
    Category savedCategory = new Category();
    savedCategory.setName(request.getName());
    savedCategory.setUserId(userId);
    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenReturn(List.of(savedCategory));
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      categoryService.createCategory(request, userId);
    });
    assertEquals("カテゴリー名はすでに存在します", exception.getReason());
    assertEquals(409, exception.getStatusCode().value());
  }

  @Test
  @Tag("createCategory")
  @DisplayName("削除済みカテゴリーの再作成")
  void testCreateCategoryAlreadyDeleted() {
    CategoryRequest request = new CategoryRequest();
    request.setName("TestCategory");
    String userId = testUserId;
    Category existedCategory = new Category();
    existedCategory.setName(request.getName());
    existedCategory.setUserId(userId);
    existedCategory.setDeletedFlag(true);

    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenReturn(List.of());

    Category newCategory = new Category();
    newCategory.setName(request.getName());
    newCategory.setUserId(userId);
    when(categoryRepository.save(any(Category.class))).thenReturn(newCategory);
    Category result = assertDoesNotThrow(() -> categoryService.createCategory(request, userId));
    assertFalse(result.isDeletedFlag());
  }

  @Test
  @Tag("createCategory")
  @DisplayName("カテゴリー作成成功- 削除済みカテゴリーと作成したい名前が違う場合")
  void testCreateCategoryNameNotTheSameAsDeletedCategory() {
    CategoryRequest request = new CategoryRequest();
    request.setName("TestCategory");
    String userId = testUserId;
    Category existedCategory = new Category();
    existedCategory.setName("existedCategory");
    existedCategory.setUserId(userId);
    existedCategory.setDeletedFlag(true);

    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenReturn(List.of(existedCategory));

    Category newCategory = new Category();
    newCategory.setName(request.getName());
    newCategory.setUserId(userId);
    when(categoryRepository.save(any(Category.class))).thenReturn(newCategory);

    Category result = categoryService.createCategory(request, userId);
    assertFalse(result.isDeletedFlag());
  }

  @Test
  @Tag("createCategory")
  @DisplayName("登録できるカテゴリの上限に達している場合のエラー")
  void testCreateCategoryLimitExceeded() {
    CategoryRequest request = new CategoryRequest();
    request.setName("TestCategory");
    String userId = testUserId;

    // Create a list with 50 categories for the user
    List<Category> existingCategories = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      Category category = new Category();
      category.setUserId(userId);
      category.setName("Category" + i);
      existingCategories.add(category);
    }
    Category defCategory = new Category("DefaultCategory");
    defCategory.setUserId(defaultSystemId);
    existingCategories.add(defCategory);

    // Mock the repository to return the list
    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenReturn(existingCategories);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      categoryService.createCategory(request, userId);
    });

    assertEquals("登録できるカテゴリの上限に達しています", exception.getReason());
  }

  @Test
  @Tag("updateCategory")
  @DisplayName("カテゴリーアップデート成功")
  void testUpdateCategorySuccess() {
    UUID categoryId = UUID.randomUUID();
    CategoryRequest request = new CategoryRequest();
    request.setName("UpdatedName");
    String userId = testUserId;

    Category category = new Category("OldName", new String(testUserId));

    when(categoryRepository.findUserCategory(List.of(userId, defaultSystemId), categoryId))
        .thenReturn(Optional.of(category));
    when(categoryRepository.save(any(Category.class))).thenReturn(category);

    Category result = categoryService.updateCategory(categoryId, request, userId);
    assertEquals("UpdatedName", result.getName());
  }

  @Test
  @Tag("updateCategory")
  @DisplayName("アップデートしたいデータがない場合のエラー")
  void testUpdateCategoryNotFound() {
    UUID categoryId = UUID.randomUUID();
    CategoryRequest request = new CategoryRequest();
    request.setName("UpdatedName");
    String userId = testUserId;

    when(categoryRepository.findUserCategory(List.of(userId, defaultSystemId), categoryId))
        .thenReturn(Optional.empty());

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      categoryService.updateCategory(categoryId, request, userId);
    });

    assertEquals(categoryNotFoundMsg, exception.getMessage());
  }

  @Test
  @Tag("updateCategory")
  @DisplayName("デフォルトカテゴリのアップデートはできない")
  void testUpdateDefaultCategoryError() {
    UUID categoryId = UUID.randomUUID();
    CategoryRequest request = new CategoryRequest();
    request.setName("Update");
    String userId = testUserId; // Default user ID

    Category category = new Category("target");
    category.setUserId(defaultSystemId);
    when(categoryRepository.findUserCategory(List.of(userId, defaultSystemId), categoryId))
        .thenReturn(Optional.of(category));

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      categoryService.updateCategory(categoryId, request, userId);
    });

    assertEquals("デフォルトカテゴリは編集できません", exception.getMessage());
  }

  @Test
  @Tag("updateCategory")
  @DisplayName("カテゴリー名がすでに存在する場合のエラー")
  void testUpdateCategoryNameExistsError() {
    UUID categoryId = UUID.randomUUID();
    CategoryRequest request = new CategoryRequest();
    request.setName("Update");
    String userId = testUserId; // Default user ID

    Category category = new Category("target", new String(userId));

    when(categoryRepository.findUserCategory(List.of(userId, defaultSystemId), categoryId))
        .thenReturn(Optional.of(category));
    when(categoryRepository.findActiveCateByName(List.of(userId, defaultSystemId), request.getName()))
        .thenReturn(List.of(new Category(request.getName(), new String(userId))));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      categoryService.updateCategory(categoryId, request, userId);
    });

    assertEquals("カテゴリー名はすでに存在します", exception.getReason());
    assertEquals(409, exception.getStatusCode().value());
  }

  @Test
  @Tag("deleteCategory")
  @DisplayName("カテゴリー削除成功")
  void testDeleteCategorySuccess() {
    UUID categoryId = UUID.randomUUID();
    String userId = testUserId;
    Category category = new Category();
    category.setId(categoryId);
    category.setUserId(userId);
    category.setItems(new ArrayList<>());
    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenReturn(List.of(category));

    assertDoesNotThrow(() -> categoryService.deleteCategory(categoryId, userId));
    assertTrue(category.isDeletedFlag());
  }

  @Test
  @Tag("deleteCategory")
  @DisplayName("カテゴリー削除時にアイテムが存在する場合のエラー")
  void testDeleteCategoryHasItems() {
    UUID categoryId = UUID.randomUUID();
    String userId = testUserId;
    Category category = new Category();
    category.setId(categoryId);
    category.setUserId(userId);
    category.setItems(new ArrayList<Item>(List.of(new Item())));
    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenReturn(List.of(category));

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      categoryService.deleteCategory(categoryId, userId);
    });

    assertEquals("アイテムが存在するため削除できません", exception.getMessage());
  }

  @Test
  @Tag("deleteCategory")
  @DisplayName("カテゴリー削除- カテゴリーが見つからない場合のエラー")
  void testDeleteCategoryNotFound() {
    UUID targetCategoryId = UUID.randomUUID();
    String userId = testUserId;
    Category exsitedCategory = new Category();
    exsitedCategory.setId(UUID.randomUUID());
    exsitedCategory.setUserId(userId);

    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenReturn(List.of(exsitedCategory));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
      categoryService.deleteCategory(targetCategoryId, userId);
    });

    assertEquals(categoryNotFoundMsg, ex.getReason());
  }

  @Test
  @Tag("deleteCategory")
  @DisplayName("カテゴリー削除- デフォルトカテゴリを削除場合のエラー")
  void testDeleteDefaultCategoryError() {
    UUID categoryId = UUID.randomUUID();
    String userId = testUserId; // Default user ID
    Category category = new Category();
    category.setId(categoryId);
    category.setUserId(defaultSystemId);
    category.setItems(new ArrayList<>());
    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenReturn(List.of(category));

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      categoryService.deleteCategory(categoryId, userId);
    });

    assertEquals("デフォルトカテゴリは削除できません", exception.getMessage());
  }

  @Test
  @Tag("deleteCategory")
  @DisplayName("カテゴリ削除- カテゴリーリストが空の場合のエラー")
  void testDeleteCategoryEmptyList() {
    String userId = testUserId;
    UUID categoryId = UUID.randomUUID();
    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId))).thenReturn(List.of());

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      categoryService.deleteCategory(categoryId, userId);
    });

    assertEquals(categoryNotFoundMsg, exception.getReason());
  }

  @Test
  @Tag("DB Error")
  @DisplayName("カテゴリー取得失敗 - DBエラー")
  void testGetAllCategoriesDbError() {
    String userId = testUserId;
    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenThrow(new DataAccessException("DBエラー") {
        });

    Exception exception = assertThrows(DataAccessException.class, () -> {
      categoryService.getAllCategories(userId);
    });

    assertEquals("DBエラー", exception.getMessage());
  }

  @Test
  @Tag("DB Error")
  @DisplayName("カテゴリー作成失敗 - DBエラー")
  void testCreateCategoryDbError() {
    UUID categoryId = UUID.randomUUID();
    String userId = testUserId;
    Category category = new Category();
    category.setId(categoryId);
    category.setUserId(userId);
    category.setItems(new ArrayList<>());
    when(categoryRepository.findNotDeleted(List.of(userId, defaultSystemId)))
        .thenReturn(List.of(category));
    when(categoryRepository.save(any(Category.class))).thenThrow(new DataAccessException("DBエラー") {
    });

    Exception exception = assertThrows(DataAccessException.class, () -> {
      categoryService.deleteCategory(categoryId, userId);
    });
    assertEquals("DBエラー", exception.getMessage());
  }
}
