package inventory.example.inventory_id.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

  private String itemNotFoundMsg = "アイテムが見つかりません";

  private int defaultUserId = 111;

  private int defaultSystemUserId = 999;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    ReflectionTestUtils.setField(itemService, "systemUserId", defaultSystemUserId);
  }

  @Test
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

    ItemRequest request = new ItemRequest();
    request.setName(itemName);
    request.setQuantity(quantity);
    request.setCategoryName(categoryName);

    when(categoryRepository.findByUserIdInAndName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(category));

    when(categoryRepository.save(any(Category.class))).thenReturn(category);

    assertDoesNotThrow(() -> itemService.createItem(userId, request));
    verify(categoryRepository).save(any(Category.class));
  }

  @Test
  @DisplayName("アイテム作成失敗 - カテゴリーが見つからない")
  void testCreateItemCategoryNotFound() {
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "Books";

    ItemRequest request = new ItemRequest();
    request.setName("Notebook");
    request.setQuantity(5);
    request.setCategoryName(categoryName);

    when(categoryRepository.findByUserIdInAndName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of());

    Exception ex = assertThrows(IllegalArgumentException.class, () -> itemService.createItem(userId, request));
    assertEquals(categoryNotFoundMsg, ex.getMessage());
  }

  @Test
  @DisplayName("アイテム作成失敗 - 同じ名前のアイテムが存在する")
  void testCreateItemAlreadyExists() {
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "Laptop";
    String itemName = "Notebook";

    Category category = new Category(categoryName);
    category.setUserId(userId);

    Item existingItem = new Item();
    existingItem.setName(itemName);
    category.setItems(List.of(existingItem));

    ItemRequest request = new ItemRequest();
    request.setName(itemName);
    request.setQuantity(5);
    request.setCategoryName(categoryName);

    when(categoryRepository.findByUserIdInAndName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(category));

    Exception ex = assertThrows(IllegalArgumentException.class, () -> itemService.createItem(userId, request));
    assertEquals("そのアイテム名は既に登録されています", ex.getMessage());
  }

  @Test
  @DisplayName("アイテム取得成功")
  void testGetItemsSortedByUpdatedAtDescending() {
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "Laptop";

    Category category = new Category(categoryName);
    category.setUserId(userId);

    Item item1 = new Item();
    item1.setName("Notebook");
    item1.setQuantity(5);
    item1.setUpdatedAt(LocalDateTime.now());

    Item item2 = new Item();
    item2.setName("PC");
    item2.setQuantity(10);
    item2.setUpdatedAt(LocalDateTime.now().minusDays(1));

    category.setItems(List.of(item1, item2));

    when(categoryRepository.findByUserIdInAndName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(category));

    List<ItemDto> result = itemService.getItems(userId, categoryName);

    assertEquals(2, result.size());
    assertEquals("Notebook", result.get(0).getName());
    assertEquals("PC", result.get(1).getName());
  }

  @Test
  @DisplayName("アイテム取得失敗 - カテゴリーが見つからない")
  void testGetItemsCategoryNotFound() {
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "NonExistent";

    when(categoryRepository.findByUserIdInAndName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of());

    Exception ex = assertThrows(IllegalArgumentException.class, () -> itemService.getItems(userId, categoryName));
    assertEquals(categoryNotFoundMsg, ex.getMessage());
  }

  @Test
  @DisplayName("アイテム取得失敗 - アイテムが登録されていない")
  void testGetCategoryItemsNotExist() {
    int userId = defaultUserId;
    int systemUserId = defaultSystemUserId;
    String categoryName = "Food";
    Category category = new Category(categoryName);

    category.setUserId(userId);
    category.setItems(new ArrayList<>());

    when(categoryRepository.findByUserIdInAndName(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(category));

    Exception ex = assertThrows(ResponseStatusException.class, () -> itemService.getItems(userId, categoryName));
    assertEquals("アイテムが登録されていません", ((ResponseStatusException) ex).getReason());
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

    when(itemRepository.findByUserIdInAndCategory_NameAndDeletedFlagFalse(List.of(userId, systemUserId), categoryName))
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
    when(itemRepository.findByUserIdInAndCategory_NameAndDeletedFlagFalse(List.of(userId, systemUserId), categoryName))
        .thenReturn(items);

    Exception ex = assertThrows(IllegalArgumentException.class, () -> itemService.updateItem(userId, itemId, request));
    assertEquals("そのアイテム名は既に登録されています", ex.getMessage());
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

    when(itemRepository.findByUserIdInAndCategory_NameAndDeletedFlagFalse(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of());

    Exception ex = assertThrows(ResponseStatusException.class, () -> itemService.updateItem(userId, itemId, request));
    assertEquals(itemNotFoundMsg, ((ResponseStatusException) ex).getReason());
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

    when(itemRepository.findByUserIdInAndCategory_NameAndDeletedFlagFalse(List.of(userId, systemUserId), categoryName))
        .thenReturn(List.of(existingItem));

    Exception ex = assertThrows(ResponseStatusException.class, () -> itemService.updateItem(userId, itemId, request));
    assertEquals(itemNotFoundMsg, ((ResponseStatusException) ex).getReason());
  }
}
