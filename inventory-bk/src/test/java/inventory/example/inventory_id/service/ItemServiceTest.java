package inventory.example.inventory_id.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import inventory.example.inventory_id.dto.ItemDto;
import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.repository.CategoryRepository;
import inventory.example.inventory_id.repository.ItemRepository;
import inventory.example.inventory_id.request.ItemRequest;

class ItemServiceTest {

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private ItemRepository itemRepository;

  @InjectMocks
  private ItemService itemService;

  private String categoryNotFoundMsg = "カテゴリーが見つかりません";
  private String itemsNotFoundMsg = "アイテムが見つかりません";
  private String testUserId = "testUserId";
  private String defaultSystemId = "systemId";

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    ReflectionTestUtils.setField(itemService, "systemUserId", defaultSystemId);
  }

  @Test
  @Tag("createItem")
  @DisplayName("アイテム作成成功")
  void testCreateItemSuccess() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "Laptop";
    String itemName = "Notebook";

    Category category = new Category(categoryName);
    category.setUserId(userId);
    category.setItems(new ArrayList<>());

    ItemRequest request = new ItemRequest(itemName, categoryName);

    when(categoryRepository
        .findActiveCateByName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(category));

    when(categoryRepository.save(any(Category.class))).thenReturn(category);

    assertDoesNotThrow(() -> itemService.createItem(userId, request));
    verify(categoryRepository).save(any(Category.class));
  }

  @Test
  @Tag("createItem")
  @DisplayName("アイテム作成失敗- カテゴリーが見つからない")
  void testCreateItemCategoryNotFound() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "Books";

    ItemRequest request = new ItemRequest("Notebook", categoryName);

    when(categoryRepository
        .findActiveCateByName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of());

    Exception ex = assertThrows(IllegalArgumentException.class,
        () -> itemService.createItem(userId, request));
    assertEquals(categoryNotFoundMsg, ex.getMessage());
  }

  @Test
  @Tag("createItem")
  @DisplayName("アイテム作成失敗 - 同じ名前のアイテムが存在する")
  void testCreateItemAlreadyExists() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "Laptop";
    String itemName = "Notebook";

    Category category = new Category(categoryName);
    category.setUserId(userId);

    Item existingItem = new Item(itemName, userId, category, false);

    category.setItems(List.of(existingItem));

    ItemRequest request = new ItemRequest(itemName, categoryName);

    when(categoryRepository
        .findActiveCateByName(List.of(userId,
            systemUserId), categoryName))
        .thenReturn(List.of(category));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> itemService.createItem(userId, request));
    assertEquals(String.format("アイテム名 '%s' は既に存在します", itemName), ex.getReason());
  }

  @Test
  @Tag("createItem")
  @DisplayName("アイテム作成成功 - 別ユーザがデフォルトのカテゴリに同じ名前のアイテムを持っている場合")
  void testCreateItemWhenOtherUserHasTheSameNameInDefaultCategory() {
    String userId = testUserId;
    String otherUserId = "otherUserId";
    String systemUserId = defaultSystemId;
    String categoryName = "Laptop";
    String itemName = "Notebook";

    Category category = new Category(categoryName, systemUserId);

    Item otherUserItem = new Item(itemName, otherUserId, category, false);

    category.setItems(new ArrayList<>(List.of(otherUserItem)));

    ItemRequest request = new ItemRequest(itemName, categoryName);

    when(categoryRepository
        .findActiveCateByName(List.of(userId,
            systemUserId), categoryName))
        .thenReturn(List.of(category));

    assertDoesNotThrow(() -> itemService.createItem(userId, request));
    verify(categoryRepository).save(any(Category.class));
  }

  @Test
  @Tag("createItem")
  @DisplayName("アイテム作成 - 同じ名前のアイテムが存在するが削除フラグが立っている場合")
  void testCreateItemThatHasDeletedFlag() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "Laptop";
    String itemName = "Notebook";

    Category category = new Category(categoryName);
    category.setUserId(userId);

    Item existingItem = new Item();
    existingItem.setName(itemName);
    existingItem.setDeletedFlag(true);

    category.setItems(new ArrayList<>(List.of(existingItem)));

    ItemRequest request = new ItemRequest(itemName, categoryName);

    when(categoryRepository
        .findActiveCateByName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(category));

    assertDoesNotThrow(() -> itemService.createItem(userId, request));
    verify(categoryRepository).save(any(Category.class));
  }

  @Test
  @Tag("createItem")
  @DisplayName("アイテム作成 - 名前が違うアイテムが存在するが削除フラグが立っている場合")
  void testCreateItemThatHasDeletedFlagButDifferentName() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "Laptop";
    String itemName = "Notebook";
    String differentItemName = "Tablet";

    Category category = new Category(categoryName);
    category.setUserId(userId);

    Item existingItem = new Item();
    existingItem.setName(differentItemName);
    existingItem.setDeletedFlag(true);

    category.setItems(new ArrayList<>(List.of(existingItem)));

    ItemRequest request = new ItemRequest(itemName, categoryName);

    when(categoryRepository
        .findActiveCateByName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(category));

    assertDoesNotThrow(() -> itemService.createItem(userId, request));
    verify(categoryRepository).save(any(Category.class));
  }

  @Test
  @Tag("getItem")
  @DisplayName("アイテム取得成功")
  void testGetItemsSortedByUpdatedAtDescending() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "pc";

    Category category = new Category(categoryName);
    category.setUserId(userId);

    Item notebook = new Item(
        "Notebook",
        userId,
        category,
        false);

    Item desktop = new Item(
        "Desktop",
        userId,
        category,
        false);

    category.setItems(List.of(notebook, desktop));

    when(categoryRepository
        .findActiveCateByName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(category));

    when(itemRepository
        .getActiveByCategoryName(List.of(userId, systemUserId), categoryName))
        .thenReturn(category.getItems());

    List<ItemDto> result = assertDoesNotThrow(() -> itemService.getItems(userId, categoryName));

    assertEquals(2, result.size());
  }

  @Test
  @Tag("getItem")
  @DisplayName("アイテム取得成功 - アイテムがない場合は空のリストを返す")
  void testGetItemsNotExist() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "Food";

    when(categoryRepository
        .findActiveCateByName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(new Category(categoryName)));

    when(itemRepository
        .getActiveByCategoryName(List.of(userId, systemUserId), categoryName))
        .thenReturn(new ArrayList<>());

    List<ItemDto> result = assertDoesNotThrow(() -> itemService.getItems(userId, categoryName));
    assertTrue(result.isEmpty());
  }

  @Test
  @Tag("getItem")
  @DisplayName("アイテム取得失敗 - カテゴリーが見つからない")
  void testGetItemsCategoryNotFound() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "notexits";

    when(categoryRepository
        .findActiveCateByName(List.of(userId, systemUserId), categoryName))
        .thenThrow(new IllegalArgumentException("カテゴリーが見つかりません"));

    Exception ex = assertThrows(IllegalArgumentException.class,
        () -> itemService.getItems(userId, categoryName));
    assertEquals("カテゴリーが見つかりません", ex.getMessage());
  }

  @Test
  @Tag("updateItem")
  @DisplayName("アイテム更新成功- 正常系")
  void testUpdateItemSuccess() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String oldCategoryName = "oldCategoryName";
    Category oldCategory = new Category(oldCategoryName, userId);
    String existingName = "existingItem";
    UUID existingItemId = UUID.randomUUID();
    Category newCategory = new Category("newCategory", userId);

    Item existingItem = new Item(
        existingName,
        testUserId,
        oldCategory,
        false);
    existingItem.setId(existingItemId);

    ItemRequest request = new ItemRequest("newItemName", "newCategory");

    when(itemRepository.getActiveItemWithId(List.of(userId, systemUserId), existingItemId))
        .thenReturn(Optional.of(existingItem));

    when(categoryRepository.findActiveCateByName(List.of(userId, systemUserId),
        "newCategory"))
        .thenReturn(List.of(newCategory));

    when(itemRepository.getActiveWithSameNameAndCategory(
        List.of(userId, systemUserId), existingName, newCategory.getId())).thenReturn(Optional.empty());

    when(itemRepository.save(any(Item.class))).thenReturn(existingItem);

    assertDoesNotThrow(() -> itemService.updateItem(userId, existingItemId, request));
    assertEquals("newItemName", existingItem.getName());
    assertEquals(newCategory, existingItem.getCategory());
    verify(itemRepository).save(existingItem);
  }

  @Test
  @Tag("updateItem")
  @DisplayName("アイテム更新成功- 正常系(他のユーザがデフォルトカテゴリに同じ名前のアイテムを持っている場合)")
  void testUpdateItemSuccess_WithOtherUser() {
    String userId = testUserId;
    String anotherUserId = "anotherUserId";
    String systemUserId = defaultSystemId;
    String oldCategoryName = "oldCategoryName";
    String oldItemName = "oldItemName";
    String newItemName = "newItemName";
    UUID testItemId = UUID.randomUUID();
    Category oldCategory = new Category(oldCategoryName, userId);
    Category newCategory = new Category("newCategory", userId);

    Item existingItem = new Item(
        oldItemName,
        testUserId,
        oldCategory,
        false);
    existingItem.setId(testItemId);

    Item otherUserItemInNewCategory = new Item(
        newItemName,
        anotherUserId,
        newCategory,
        false);
    newCategory.setItems(List.of(otherUserItemInNewCategory));

    ItemRequest request = new ItemRequest("newItemName", "newCategory");

    when(itemRepository.getActiveItemWithId(List.of(userId, systemUserId), testItemId))
        .thenReturn(Optional.of(existingItem));

    when(categoryRepository.findActiveCateByName(List.of(userId, systemUserId),
        "newCategory"))
        .thenReturn(List.of(newCategory));

    when(itemRepository.getActiveWithSameNameAndCategory(
        List.of(userId, systemUserId), oldItemName, newCategory.getId())).thenReturn(Optional.empty());

    when(itemRepository.save(any(Item.class))).thenReturn(existingItem);

    assertDoesNotThrow(() -> itemService.updateItem(userId, testItemId, request));
    assertEquals("newItemName", existingItem.getName());
    assertEquals(newCategory, existingItem.getCategory());
    verify(itemRepository).save(existingItem);
  }

  @Test
  @Tag("updateItem")
  @DisplayName("アイテム更新失敗 - アイテム名変更時、アイテム名重複")
  void testUpdateItemNameDuplicate() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "Laptop";

    Category LaptopCategory = new Category(
        categoryName, systemUserId);
    UUID targetItemId = UUID.randomUUID();

    Item userNotebook = new Item("Notebook",
        userId, LaptopCategory, false);
    userNotebook.setId(UUID.randomUUID());

    Item targetItem = new Item(
        "OldName",
        userId,
        LaptopCategory,
        false);
    targetItem.setId(targetItemId);

    ItemRequest request = new ItemRequest("Notebook", categoryName);

    when(itemRepository.getActiveItemWithId(
        List.of(userId, systemUserId), targetItemId))
        .thenReturn(Optional.of(targetItem));

    when(categoryRepository.findActiveCateByName(
        List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(LaptopCategory));

    when(itemRepository.getActiveWithSameNameAndCategory(
        List.of(userId, systemUserId),
        request.getName(),
        LaptopCategory.getId()))
        .thenReturn(Optional.of(userNotebook));

    Exception ex = assertThrows(IllegalArgumentException.class,
        () -> itemService.updateItem(userId, targetItemId, request));
    assertEquals("アイテム名は既に登録されています", ex.getMessage());
  }

  @Test
  @Tag("updateItem")
  @DisplayName("アイテム更新失敗 - カテゴリ変更時、アイテム名重複")
  void testUpdateItemCategoryDuplicate() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String oldCategoryName = "oldCategoryName";
    String newCategoryName = "newCategoryName";
    String itemName = "itemName";
    UUID targetItemId = UUID.randomUUID();
    Category oldCategory = new Category(oldCategoryName, systemUserId);
    Category newCategory = new Category(newCategoryName, systemUserId);

    Item itemInOldCategory = new Item(itemName, userId, oldCategory, false);
    itemInOldCategory.setId(targetItemId);

    Item itemInNewCategory = new Item(itemName, userId, newCategory, false);
    itemInNewCategory.setId(UUID.randomUUID());

    ItemRequest request = new ItemRequest(itemName,
        "newCategoryName");

    when(itemRepository.getActiveItemWithId(List.of(userId, systemUserId), targetItemId))
        .thenReturn(Optional.of(itemInOldCategory));

    when(categoryRepository.findActiveCateByName(List.of(userId, systemUserId),
        newCategoryName))
        .thenReturn(List.of(newCategory));

    when(itemRepository.getActiveWithSameNameAndCategory(List.of(userId, systemUserId),
        request.getName(), newCategory.getId()))
        .thenReturn(Optional.of(itemInNewCategory));

    Exception ex = assertThrows(IllegalArgumentException.class,
        () -> itemService.updateItem(userId, targetItemId, request));
    assertEquals("アイテム名は既に登録されています", ex.getMessage());
  }

  @Test
  @Tag("updateItem")
  @DisplayName("アイテム更新失敗 - アイテムが見つからない")
  void testUpdateItemNotFound() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    UUID itemId = UUID.randomUUID();

    ItemRequest request = new ItemRequest("Notebook", "Laptop");

    when(itemRepository
        .getActiveItemWithId(List.of(userId, systemUserId), itemId))
        .thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> itemService.updateItem(userId, itemId, request));
    assertEquals(itemsNotFoundMsg, ex.getReason());
  }

  @Test
  @Tag("updateItem")
  @DisplayName("アイテム更新失敗 - カテゴリが見つからない")
  void testUpdateItemCategoryNotFound() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    UUID itemId = UUID.randomUUID();

    ItemRequest request = new ItemRequest("Notebook",
        "NotExistCategory");

    when(itemRepository
        .getActiveItemWithId(List.of(userId, systemUserId),
            itemId))
        .thenReturn(Optional.of(new Item(
            "Notebook",
            userId,
            new Category(
                "Laptop"),
            false)));

    when(categoryRepository
        .findActiveCateByName(List.of(userId,
            systemUserId), request.getCategoryName()))
        .thenReturn(List.of());

    Exception ex = assertThrows(
        IllegalArgumentException.class,
        () -> itemService.updateItem(userId, itemId, request));
    assertEquals(categoryNotFoundMsg, ex.getMessage());
  }

  @Test
  @Tag("deleteItem")
  @DisplayName("アイテム削除成功")
  void testDeleteItemSuccess() {
    String userId = testUserId;
    UUID itemId = UUID.randomUUID();
    Item item = new Item();
    item.setId(itemId);
    item.setUserId(userId);

    when(itemRepository
        .getActiveItemWithId(List.of(userId, defaultSystemId), itemId))
        .thenReturn(Optional.of(item));

    when(itemRepository.save(any(Item.class))).thenReturn(item);

    assertDoesNotThrow(() -> itemService.deleteItem(userId, itemId));
    assertEquals(true, item.isDeletedFlag());
    verify(itemRepository).save(item);
  }

  @Test
  @Tag("deleteItem")
  @DisplayName("アイテム削除失敗 - アイテムが見つからない")
  void testDeleteItemNotFound() {
    String userId = testUserId;
    UUID itemId = UUID.randomUUID();
    when(itemRepository
        .getActiveItemWithId(List.of(userId, defaultSystemId), itemId))
        .thenReturn(Optional.empty());

    Exception ex = assertThrows(ResponseStatusException.class,
        () -> itemService.deleteItem(userId, itemId));
    assertEquals("アイテムが見つかりません",
        ((ResponseStatusException) ex).getReason());
  }

  @Test
  @Tag("DB error")
  @DisplayName("アイテム取得失敗 - DBエラー")
  void selectItem_dbError() {
    String userId = testUserId;
    UUID itemId = UUID.randomUUID();
    Item item = new Item();
    item.setId(itemId);
    item.setUserId(userId);
    when(itemRepository.getActiveItemWithId(anyList(), any(UUID.class)))
        .thenThrow(new DataAccessException("DB error") {
        });
    Exception ex = assertThrows(DataAccessException.class,
        () -> itemService.deleteItem(userId, itemId));
    assertEquals("DB error", ex.getMessage());
  }

  @Test
  @Tag("DB error")
  @DisplayName("アイテム保存失敗 - DBエラー")
  void saveItem_dbError() {
    String userId = testUserId;
    UUID itemId = UUID.randomUUID();
    Item item = new Item();
    item.setId(itemId);
    item.setUserId(userId);

    when(itemRepository
        .getActiveItemWithId(List.of(userId, defaultSystemId), itemId))
        .thenReturn(Optional.of(item));
    when(itemRepository.save(any(Item.class)))
        .thenThrow(new DataAccessException("DB エラー") {
        });

    Exception ex = assertThrows(DataAccessException.class,
        () -> itemService.deleteItem(userId, itemId));
    assertEquals("DB エラー", ex.getMessage());
  }
}
