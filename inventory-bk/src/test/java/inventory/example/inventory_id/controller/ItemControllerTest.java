package inventory.example.inventory_id.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import inventory.example.inventory_id.dto.ItemDto;
import inventory.example.inventory_id.exception.ValidationException;
import inventory.example.inventory_id.request.ItemRequest;
import inventory.example.inventory_id.service.ItemService;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {

  @Mock
  private ItemService itemService;

  @Spy
  @InjectMocks
  private ItemController itemController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper = new ObjectMapper();

  private String testUserId = "testUserId";
  private String categoryNotFoundMsg = "カテゴリーが見つかりません";
  private String itemNotFoundMsg = "アイテムが見つかりません";
  private String serverErrorMsg = "サーバーエラーが発生しました";

  @BeforeEach
  void setUp() {
    Mockito.lenient().doReturn(testUserId).when(itemController).fetchUserIdFromToken();
    mockMvc = MockMvcBuilders.standaloneSetup(itemController)
        .setControllerAdvice(new ValidationException())
        .build();
  }

  @Test
  @Tag("POST: /api/item")
  @DisplayName("アイテム作成-201 Created")
  void createItem_success() throws Exception {
    ItemRequest req = new ItemRequest("itemName", "category", 1);
    doNothing().when(itemService).createItem(anyString(), any(ItemRequest.class));
    mockMvc.perform(post("/api/item")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(content().json("{\"message\":\"アイテムの作成が完了しました\"}"));
  }

  @Test
  @Tag("POST: /api/item")
  @DisplayName("アイテム作成-201 数量が入力されていない場合も作成できる")
  void createItem_badRequest_quantityMissing() throws Exception {
    ItemRequest req = new ItemRequest();
    req.setCategoryName("category");
    req.setName("itemName");
    mockMvc.perform(post("/api/item")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(content().json("{\"message\":\"アイテムの作成が完了しました\"}"));
  }

  @Test
  @Tag("POST: /api/item")
  @DisplayName("アイテム作成-400 Bad Request カテゴリーが見つからない")
  void createItem_badRequest_categoryNotFound() throws Exception {
    ItemRequest req = new ItemRequest("itemName", "category", 1);
    doThrow(new IllegalArgumentException(categoryNotFoundMsg))
        .when(itemService)
        .createItem(anyString(), any(ItemRequest.class));
    mockMvc.perform(post("/api/item")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"message\":\"" + categoryNotFoundMsg + "\"}"));
  }

  @Test
  @Tag("POST: /api/item")
  @DisplayName("アイテム作成-409 Conflict アイテム名が重複")
  void createItem_conflict_itemNameDuplicate() throws Exception {
    ItemRequest req = new ItemRequest("itemName", "category", 1);
    doThrow(new ResponseStatusException(
        HttpStatus.CONFLICT,
        String.format("アイテム名 '%s' は既に存在します", req.getName())))
        .when(itemService)
        .createItem(anyString(), any(ItemRequest.class));
    mockMvc.perform(post("/api/item")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isConflict())
        .andExpect(content()
            .json("{\"message\":\"" +
                String.format("アイテム名 '%s' は既に存在します", req.getName()) + "\"}"));
  }

  @Test
  @Tag("POST: /api/item")
  @DisplayName("アイテム作成-400 Bad Request アイテム名入力がない")
  void createItem_badRequest_itemNameMissing() throws Exception {
    ItemRequest req = new ItemRequest("", "category", 1);
    mockMvc.perform(post("/api/item")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"error\":\"アイテム名は必須です\"}"));
  }

  @Test
  @Tag("POST: /api/item")
  @DisplayName("アイテム作成-400 Bad Request カテゴリー名入力がない")
  void createItem_badRequest_categoryNameMissing() throws Exception {
    ItemRequest req = new ItemRequest("test", "", 1);
    mockMvc.perform(post("/api/item")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"error\":\"カテゴリは必須です\"}"));
  }

  @Test
  @Tag("POST: /api/item")
  @DisplayName("アイテム作成-400 Bad Request　数量がマイナス")
  void createItem_badRequest_quantityNegative() throws Exception {
    ItemRequest req = new ItemRequest("itemName", "category", -1);
    mockMvc.perform(post("/api/item")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"error\":\"数量は0以上の整数で入力してください\"}"));
  }

  @Test
  @Tag("POST: /api/item")
  @DisplayName("アイテム作成-500 サーバーエラー")
  void createItem_throws500() throws Exception {
    ItemRequest req = new ItemRequest("itemName", "category", 1);
    doThrow(new RuntimeException(
        serverErrorMsg))
        .when(itemService)
        .createItem(anyString(), any(ItemRequest.class));
    mockMvc.perform(post("/api/item")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("{\"message\":\"" + serverErrorMsg + "\"}"));
  }

  @Test
  @Tag("GET: /api/item")
  @DisplayName("アイテム一覧取得-200 OK")
  void getItems_success() throws Exception {
    List<ItemDto> items = Arrays.asList(new ItemDto(), new ItemDto());
    when(itemService.getItems(anyString(), anyString())).thenReturn(items);
    mockMvc.perform(get("/api/item")
        .param("category_name", "test"))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(items)));
  }

  @Test
  @Tag("GET: /api/item")
  @DisplayName("アイテム一覧取得-400 不正な引数")
  void getItems_throws400() throws Exception {
    when(itemService.getItems(anyString(), anyString()))
        .thenThrow(new IllegalArgumentException(categoryNotFoundMsg));
    mockMvc.perform(get("/api/item")
        .param("category_name", "test"))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"message\":\"" + categoryNotFoundMsg + "\"}"));
  }

  @Test
  @Tag("GET: /api/item")
  @DisplayName("アイテム一覧取得-404 アイテムが見つからない")
  void getItems_throws404() throws Exception {
    when(itemService.getItems(anyString(), anyString()))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, itemNotFoundMsg));
    mockMvc.perform(get("/api/item")
        .param("category_name", "test"))
        .andExpect(status().isNotFound())
        .andExpect(content().json("{\"message\":\"" + itemNotFoundMsg + "\"}"));
  }

  @Test
  @Tag("GET: /api/item")
  @DisplayName("アイテム一覧取得-500 サーバーエラー")
  void getItems_throws500() throws Exception {
    when(itemService.getItems(anyString(), anyString()))
        .thenThrow(new RuntimeException(serverErrorMsg));
    mockMvc.perform(get("/api/item")
        .param("category_name", "test"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("{\"message\":\"" + serverErrorMsg + "\"}"));
  }

  @Test
  @Tag("PUT: /api/item")
  @DisplayName("アイテム更新-200 OK")
  void updateItem_success() throws Exception {
    UUID itemId = UUID.randomUUID();
    ItemRequest req = new ItemRequest("itemName", "category", 1);
    doNothing().when(itemService).updateItem(
        anyString(),
        eq(itemId),
        any(ItemRequest.class));
    mockMvc.perform(put("/api/item")
        .param("item_id", itemId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"message\":\"アイテムの更新が完了しました\"}"));
  }

  @Test
  @Tag("PUT: /api/item")
  @DisplayName("アイテム更新-404 Not Found アイテムが見つかりません")
  void updateItem_notFound() throws Exception {
    UUID itemId = UUID.randomUUID();
    ItemRequest req = new ItemRequest("itemName", "category", 1);
    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, itemNotFoundMsg))
        .when(itemService).updateItem(
            anyString(),
            eq(itemId),
            any(ItemRequest.class));
    mockMvc.perform(put("/api/item")
        .param("item_id", itemId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isNotFound())
        .andExpect(content().json("{\"message\":\"" + itemNotFoundMsg + "\"}"));
  }

  @Test
  @Tag("PUT: /api/item")
  @DisplayName("アイテム更新-400 Bad Request アイテム名は既に登録されています")
  void updateItem_badRequest() throws Exception {
    UUID itemId = UUID.randomUUID();
    ItemRequest req = new ItemRequest("itemName", "category", 1);
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "アイテム名は既に登録されています"))
        .when(itemService)
        .updateItem(
            anyString(),
            eq(itemId),
            any(ItemRequest.class));
    mockMvc.perform(put("/api/item")
        .param("item_id", itemId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"message\":\"アイテム名は既に登録されています\"}"));
  }

  @Test
  @Tag("PUT: /api/item")
  @DisplayName("アイテム更新-400 Bad Request アイテム名がない")
  void updateItem_badRequest_itemNameMissed() throws Exception {
    UUID itemId = UUID.randomUUID();
    ItemRequest req = new ItemRequest("", "category", 1);
    mockMvc.perform(put("/api/item")
        .param("item_id", itemId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"error\":\"アイテム名は必須です\"}"));
  }

  @Test
  @Tag("PUT: /api/item")
  @DisplayName("アイテム更新-400 Bad Request カテゴリ名がない")
  void updateItem_badRequest_categoryNameMissed() throws Exception {
    UUID itemId = UUID.randomUUID();
    ItemRequest req = new ItemRequest("itemName", "", 1);
    mockMvc.perform(put("/api/item")
        .param("item_id", itemId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"error\":\"カテゴリは必須です\"}"));
  }

  @Test
  @Tag("PUT: /api/item")
  @DisplayName("アイテム更新-500 サーバーエラー")
  void updateItem_throws500() throws Exception {
    UUID itemId = UUID.randomUUID();
    ItemRequest req = new ItemRequest("itemName", "category", 1);
    doThrow(new RuntimeException(serverErrorMsg)).when(itemService).updateItem(
        anyString(),
        eq(itemId),
        any(ItemRequest.class));
    mockMvc.perform(put("/api/item")
        .param("item_id", itemId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("{\"message\":\"" + serverErrorMsg + "\"}"));
  }

  @Test
  @Tag("DELETE: /api/item")
  @DisplayName("アイテム削除-202 Accepted")
  void deleteItem_success() throws Exception {
    UUID itemId = UUID.randomUUID();
    doNothing().when(itemService).deleteItem(anyString(), eq(itemId));
    mockMvc.perform(delete("/api/item")
        .param("item_id", itemId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"message\":\"アイテムの削除が完了しました\"}"));
  }

  @Test
  @Tag("DELETE: /api/item")
  @DisplayName("アイテム削除-404 Not Found")
  void deleteItem_notFound() throws Exception {
    UUID itemId = UUID.randomUUID();
    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, itemNotFoundMsg))
        .when(itemService).deleteItem(
            anyString(),
            eq(itemId));
    mockMvc.perform(delete("/api/item")
        .param("item_id", itemId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(content().json("{\"message\":\"" + itemNotFoundMsg + "\"}"));
  }

  @Test
  @Tag("DELETE: /api/item")
  @DisplayName("アイテム削除-500 サーバーエラー")
  void deleteItem_throws500() throws Exception {
    UUID itemId = UUID.randomUUID();
    doThrow(new RuntimeException(serverErrorMsg))
        .when(itemService)
        .deleteItem(
            anyString(),
            eq(itemId));
    mockMvc.perform(delete("/api/item")
        .param("item_id", itemId.toString()))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("{\"message\":\"" + serverErrorMsg + "\"}"));
  }
}
