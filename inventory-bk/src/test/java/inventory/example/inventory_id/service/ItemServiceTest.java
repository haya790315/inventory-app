package inventory.example.inventory_id.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    int quantity = 5;

    Category category = new Category(categoryName);
    category.setUserId(userId);
    category.setItems(new ArrayList<>());

    ItemRequest request = new ItemRequest(itemName, categoryName, quantity);

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

    ItemRequest request = new ItemRequest("Notebook", categoryName, 5);

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

    Item existingItem = new Item(itemName);

    category.setItems(List.of(existingItem));

    ItemRequest request = new ItemRequest(itemName, categoryName, 5);

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

    ItemRequest request = new ItemRequest(itemName, categoryName, 5);

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

    ItemRequest request = new ItemRequest(itemName, categoryName, 5);

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
        5,
        false);

    Item desktop = new Item(
        "Desktop",
        userId,
        category,
        10,
        false);

    category.setItems(List.of(notebook, desktop));

    when(itemRepository
        .getActiveByCategoryName(List.of(userId, systemUserId), categoryName))
        .thenReturn(category.getItems());

    List<ItemDto> result = assertDoesNotThrow(() -> itemService.getItems(userId, categoryName));

    assertEquals(2, result.size());
  }

  @Test
  @Tag("getItem")
  @DisplayName("アイテム取得失敗 - アイテムが見つかりません")
  void testGetItemsNotExist() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "Food";

    when(itemRepository
        .getActiveByCategoryName(List.of(userId, systemUserId), categoryName))
        .thenReturn(new ArrayList<>());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> itemService.getItems(userId, categoryName));
    assertEquals(itemsNotFoundMsg, ex.getReason());
  }

  @Test
  @Tag("updateItem")
  @DisplayName("アイテム更新成功")
  void testUpdateItemSuccess() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "Laptop";
    String newItemName = "Notebook";
    int newQuantity = 5;
    UUID itemId = UUID.randomUUID();

    Item existingItem = new Item(
        "OldName",
        testUserId,
        null,
        1,
        false);
    existingItem.setId(itemId);

    ItemRequest request = new ItemRequest(newItemName, categoryName, newQuantity);

    List<Item> items = new ArrayList<>();
    items.add(existingItem);

    when(itemRepository.getActiveByCategoryName(
        List.of(userId, systemUserId), categoryName))
        .thenReturn(items);
    when(itemRepository.save(any(Item.class))).thenReturn(existingItem);

    assertDoesNotThrow(() -> itemService.updateItem(userId, itemId, request));
    assertEquals(newItemName, existingItem.getName());
    assertEquals(newQuantity, existingItem.getQuantity());
    verify(itemRepository).save(existingItem);
  }

  @Test
  @Tag("updateItem")
  @DisplayName("アイテム更新失敗 - アイテム名重複")
  void testUpdateItemNameDuplicate() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "Laptop";
    UUID itemId = UUID.randomUUID();

    Item userNotebook = new Item("Notebook");
    userNotebook.setId(itemId);

    Item diffUserNotebook = new Item("Notebook");
    diffUserNotebook.setId(UUID.randomUUID());

    ItemRequest request = new ItemRequest("Notebook", categoryName, 10);

    List<Item> items = List.of(userNotebook, diffUserNotebook);
    when(itemRepository.getActiveByCategoryName(List.of(userId, systemUserId), categoryName))
        .thenReturn(items);

    Exception ex = assertThrows(IllegalArgumentException.class,
        () -> itemService.updateItem(userId, itemId, request));
    assertEquals("アイテム名は既に登録されています", ex.getMessage());
  }

  @Test
  @Tag("updateItem")
  @DisplayName("アイテム更新失敗 - アイテムが見つからない")
  void testUpdateItemNotFound() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "Laptop";
    UUID itemId = UUID.randomUUID();

    ItemRequest request = new ItemRequest("Notebook", categoryName, 10);

    when(itemRepository
        .getActiveByCategoryName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> itemService.updateItem(userId, itemId, request));
    assertEquals(itemsNotFoundMsg, ex.getReason());
  }

  @Test
  @Tag("updateItem")
  @DisplayName("アイテム更新失敗 - アイテムIdが見つからないエラー")
  void testUpdateItemIdNotFound() {
    String userId = testUserId;
    String systemUserId = defaultSystemId;
    String categoryName = "Laptop";
    UUID itemId = UUID.randomUUID();
    Category category = new Category(categoryName);

    ItemRequest request = new ItemRequest("Notebook", categoryName, 10);

    Item existingItem = new Item(
        "Notebook",
        userId,
        category, 5,
        false);
    existingItem.setId(UUID.randomUUID());

    when(itemRepository
        .getActiveByCategoryName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(existingItem));

    Exception ex = assertThrows(ResponseStatusException.class,
        () -> itemService.updateItem(userId, itemId, request));
    assertEquals(itemsNotFoundMsg, ((ResponseStatusException) ex).getReason());
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
