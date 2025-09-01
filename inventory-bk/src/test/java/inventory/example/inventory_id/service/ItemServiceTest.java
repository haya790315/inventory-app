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
  private int defaultUserId = 111;

  private int defaultSystemUserId = 999;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    ReflectionTestUtils.setField(itemService, "systemUserId", defaultSystemUserId);
  }

  @Test
  @Tag("createItem")
  @DisplayName("アイテム作成成功")
  void testCreateItemSuccess() {
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "Laptop";
    String itemName = "Notebook";
    int quantity = 5;

    Category category = new Category(categoryName);
    category.setUserId(userId);
    category.setItems(new ArrayList<>());

    ItemRequest request = new ItemRequest(itemName, categoryName, quantity);

    when(categoryRepository.findActiveCateByName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(category));

    when(categoryRepository.save(any(Category.class))).thenReturn(category);

    assertDoesNotThrow(() -> itemService.createItem(userId, request));
    verify(categoryRepository).save(any(Category.class));
  }

  @Test
  @Tag("createItem")
  @DisplayName("アイテム作成失敗- カテゴリーが見つからない")
  void testCreateItemCategoryNotFound() {
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "Books";

    ItemRequest request = new ItemRequest("Notebook", categoryName, 5);

    when(categoryRepository.findActiveCateByName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of());

    Exception ex = assertThrows(IllegalArgumentException.class, () -> itemService.createItem(userId, request));
    assertEquals(categoryNotFoundMsg, ex.getMessage());
  }

  @Test
  @Tag("createItem")
  @DisplayName("アイテム作成失敗 - 同じ名前のアイテムが存在する")
  void testCreateItemAlreadyExists() {
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "Laptop";
    String itemName = "Notebook";

    Category category = new Category(categoryName);
    category.setUserId(userId);

    Item existingItem = new Item(itemName);

    category.setItems(List.of(existingItem));

    ItemRequest request = new ItemRequest(itemName, categoryName, 5);

    when(categoryRepository.findActiveCateByName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(category));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> itemService.createItem(userId, request));
    assertEquals(String.format("アイテム名 '%s' は既に存在します", itemName), ex.getReason());
  }

  @Test
  @Tag("createItem")
  @DisplayName("アイテム作成 - 同じ名前のアイテムが存在するが削除フラグが立っている場合")
  void testCreateItemThatHasDeletedFlag() {
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "Laptop";
    String itemName = "Notebook";

    Category category = new Category(categoryName);
    category.setUserId(userId);

    Item existingItem = new Item();
    existingItem.setName(itemName);
    existingItem.setDeletedFlag(true);

    category.setItems(new ArrayList<>(List.of(existingItem)));

    ItemRequest request = new ItemRequest(itemName, categoryName, 5);

    when(categoryRepository.findActiveCateByName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(category));

    assertDoesNotThrow(() -> itemService.createItem(userId, request));
    verify(categoryRepository).save(any(Category.class));
  }

  @Test
  @Tag("createItem")
  @DisplayName("アイテム作成 - 名前が違うアイテムが存在するが削除フラグが立っている場合")
  void testCreateItemThatHasDeletedFlagButDifferentName() {
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
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

    when(categoryRepository.findActiveCateByName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(category));

    assertDoesNotThrow(() -> itemService.createItem(userId, request));
    verify(categoryRepository).save(any(Category.class));
  }

  @Test
  @Tag("getItem")
  @DisplayName("アイテム取得成功")
  void testGetItemsSortedByUpdatedAtDescending() {
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "pc";

    Category category = new Category(categoryName);
    category.setUserId(userId);

    Item notebook = new Item("Notebook", userId, category, 5, false);

    Item desktop = new Item("Desktop", userId, category, 10, false);

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
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
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
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "Laptop";
    String newItemName = "Notebook";
    int newQuantity = 5;
    UUID itemId = UUID.randomUUID();

    Item existingItem = new Item();
    existingItem.setId(itemId);
    existingItem.setUserId(defaultUserId);
    existingItem.setName("OldName");
    existingItem.setQuantity(1);

    ItemRequest request = new ItemRequest();
    request.setName(newItemName);
    request.setQuantity(newQuantity);
    request.setCategoryName(categoryName);

    List<Item> items = new ArrayList<>();
    items.add(existingItem);

    when(itemRepository.getActiveByCategoryName(List.of(userId, systemUserId), categoryName))
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
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "Laptop";
    UUID itemId = UUID.randomUUID();

    Item item1 = new Item();
    item1.setId(itemId);
    item1.setName("Notebook");

    Item item2 = new Item();
    item2.setId(UUID.randomUUID());
    item2.setName("Notebook");

    ItemRequest request = new ItemRequest();
    request.setName("Notebook");
    request.setQuantity(10);
    request.setCategoryName(categoryName);

    List<Item> items = List.of(item1, item2);
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
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "Laptop";
    UUID itemId = UUID.randomUUID();

    ItemRequest request = new ItemRequest();
    request.setName("Notebook");
    request.setQuantity(10);
    request.setCategoryName(categoryName);

    when(itemRepository.getActiveByCategoryName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> itemService.updateItem(userId, itemId, request));
    assertEquals(itemsNotFoundMsg, ex.getReason());
  }

  @Test
  @Tag("updateItem")
  @DisplayName("アイテム更新失敗 - アイテムIdが見つからないエラー")
  void testUpdateItemIdNotFound() {
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "Laptop";
    UUID itemId = UUID.randomUUID();

    ItemRequest request = new ItemRequest();
    request.setName("Notebook");
    request.setQuantity(10);
    request.setCategoryName(categoryName);

    Item existingItem = new Item();
    existingItem.setId(UUID.randomUUID());
    existingItem.setName("Notebook");
    existingItem.setQuantity(5);

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
    int userId = defaultUserId;
    UUID itemId = UUID.randomUUID();
    Item item = new Item();
    item.setId(itemId);
    item.setUserId(userId);

    when(itemRepository.getActiveItemWithId(List.of(userId, defaultSystemUserId), itemId))
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
    int userId = defaultUserId;
    UUID itemId = UUID.randomUUID();
    when(itemRepository.getActiveItemWithId(List.of(userId, defaultSystemUserId), itemId))
        .thenReturn(Optional.empty());
    Exception ex = assertThrows(ResponseStatusException.class, () -> itemService.deleteItem(userId, itemId));
    assertEquals("アイテムが見つかりません", ((ResponseStatusException) ex).getReason());
  }

  @Test
  @Tag("DB error")
  @DisplayName("アイテム取得失敗 - DBエラー")
  void selectItem_dbError() {
    int userId = defaultUserId;
    UUID itemId = UUID.randomUUID();
    Item item = new Item();
    item.setId(itemId);
    item.setUserId(userId);
    when(itemRepository.getActiveItemWithId(anyList(), any(UUID.class)))
        .thenThrow(new DataAccessException("DB error") {
        });
    Exception ex = assertThrows(DataAccessException.class, () -> itemService.deleteItem(userId, itemId));
    assertEquals("DB error", ex.getMessage());
  }

  @Test
  @Tag("DB error")
  @DisplayName("アイテム保存失敗 - DBエラー")
  void saveItem_dbError() {
    int userId = defaultUserId;
    UUID itemId = UUID.randomUUID();
    Item item = new Item();
    item.setId(itemId);
    item.setUserId(userId);

    when(itemRepository.getActiveItemWithId(List.of(userId, defaultSystemUserId), itemId))
        .thenReturn(Optional.of(item));
    when(itemRepository.save(any(Item.class))).thenThrow(new DataAccessException("DB エラー") {
    });

    Exception ex = assertThrows(DataAccessException.class, () -> itemService.deleteItem(userId, itemId));
    assertEquals("DB エラー", ex.getMessage());
  }
}
