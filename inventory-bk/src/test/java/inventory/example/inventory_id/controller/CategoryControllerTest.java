package inventory.example.inventory_id.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import inventory.example.inventory_id.dto.CategoryDto;
import inventory.example.inventory_id.exception.ValidationException;
import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.request.CategoryRequest;
import inventory.example.inventory_id.service.CategoryService;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

  @Mock
  private CategoryService categoryService;

  @Spy
  @InjectMocks
  private CategoryController categoryController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper = new ObjectMapper();

  private String testUserId = "testUserId";
  private String categoryNotFoundMsg = "カテゴリーが見つかりません";
  private String serverErrorMsg = "サーバーエラーが発生しました";

  @BeforeEach
  void setUp() {
    lenient().doReturn(testUserId).when(categoryController).fetchUserIdFromToken();
    mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
        .setControllerAdvice(new ValidationException())
        .build();
  }

  @Test
  @Tag("GET: /api/category")
  @DisplayName("カテゴリー一覧取得-200 OK")
  void fetchAllCategories_ShouldReturn200() throws Exception {
    List<CategoryDto> categories = Arrays.asList(new CategoryDto(), new CategoryDto());
    when(categoryService.getAllCategories(anyString())).thenReturn(categories);

    mockMvc.perform(get("/api/category"))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(categories)));
  }

  @Test
  @Tag("GET: /api/category")
  @DisplayName("カテゴリー一覧取得-404 カテゴリーがゼロ件")
  void fetchAllCategories_throws404() throws Exception {
    when(categoryService.getAllCategories(anyString()))
        .thenThrow(new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            categoryNotFoundMsg));

    mockMvc.perform(get("/api/category"))
        .andExpect(status().isNotFound())
        .andExpect(content().json("{\"message\":\"" + categoryNotFoundMsg + "\"}"));
  }

  @Test
  @Tag("GET: /api/category")
  @DisplayName("カテゴリー一覧取得-500 サーバーエラー")
  void fetchAllCategories_throws500() throws Exception {
    when(categoryService.getAllCategories(anyString())).thenThrow(new RuntimeException(serverErrorMsg));

    mockMvc.perform(get("/api/category"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("{\"message\":\"" + serverErrorMsg + "\"}"));
  }

  @Test
  @Tag("GET: /api/category/items")
  @DisplayName("カテゴリーアイテム取得-200 OK")
  void getCategoryItems_returnsItems() throws Exception {
    UUID categoryId = UUID.randomUUID();
    List<Item> items = Arrays.asList(new Item(), new Item());
    when(categoryService.getCategoryItems(anyString(), any(UUID.class))).thenReturn(items);
    mockMvc.perform(get("/api/category/items").param("categoryId", categoryId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(items)));
  }

  @Test
  @Tag("GET: /api/category/items")
  @DisplayName("カテゴリーアイテム取得-500 サーバーエラー")
  void getCategoryItems_throws500() throws Exception {
    UUID categoryId = UUID.randomUUID();
    when(categoryService.getCategoryItems(anyString(), any(UUID.class)))
        .thenThrow(new RuntimeException(serverErrorMsg));
    mockMvc.perform(get("/api/category/items").param("categoryId", categoryId.toString()))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("{\"message\":\"" + serverErrorMsg + "\"}"));
  }

  @Test
  @Tag("POST: /api/category")
  @DisplayName("カテゴリー作成-200 OK")
  void createCategory_success() throws Exception {
    String name = "new";
    CategoryRequest req = new CategoryRequest();
    req.setName(name);
    Category created = new Category(name);
    when(categoryService.createCategory(any(CategoryRequest.class), anyString())).thenReturn(created);
    mockMvc.perform(post("/api/category")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(content().json("{\"message\":\"カスタムカテゴリの作成が完了しました\"}"));
  }

  @Test
  @Tag("POST: /api/category")
  @DisplayName("カテゴリー作成-失敗 409 カテゴリー上限超え")
  void createCategory_conflict_limited() throws Exception {
    String name = "new";
    CategoryRequest req = new CategoryRequest();
    req.setName(name);
    when(categoryService.createCategory(any(CategoryRequest.class), anyString()))
        .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,
            "登録できるカテゴリの上限に達しています"));
    mockMvc.perform(post("/api/category")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isConflict())
        .andExpect(content().json("{\"message\":\"登録できるカテゴリの上限に達しています\"}"));
  }

  @Test
  @Tag("POST: /api/category")
  @DisplayName("カテゴリー作成-失敗 409 カテゴリー名重複")
  void createCategory_conflict() throws Exception {
    String name = "new";
    CategoryRequest req = new CategoryRequest();
    req.setName(name);
    when(categoryService.createCategory(any(CategoryRequest.class), anyString()))
        .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,
            "カテゴリー名はすでに存在します"));
    mockMvc.perform(post("/api/category")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isConflict())
        .andExpect(content().json("{\"message\":\"カテゴリー名はすでに存在します\"}"));
  }

  @Test
  @Tag("POST: /api/category")
  @DisplayName("カテゴリー作成-失敗 400 カテゴリー名が空文字")
  void createCategory_badRequest_nameEmpty() throws Exception {
    CategoryRequest req = new CategoryRequest();
    req.setName("　　　　　　"); // 空文字
    mockMvc.perform(post("/api/category")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"error\":\"カテゴリ名は必須\"}"));
  }

  @Test
  @Tag("POST: /api/category")
  @DisplayName("カテゴリー作成-失敗 500 サーバーエラー")
  void createCategory_throws500() throws Exception {
    String name = "new";
    CategoryRequest req = new CategoryRequest();
    req.setName(name);
    when(categoryService.createCategory(any(CategoryRequest.class), anyString()))
        .thenThrow(new RuntimeException(serverErrorMsg));
    mockMvc.perform(post("/api/category")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("{\"message\":\"" + serverErrorMsg + "\"}"));
  }

  @Test
  @Tag("PUT: /api/category")
  @DisplayName("カテゴリー更新-200 OK")
  void updateCategory_success() throws Exception {
    UUID categoryId = UUID.randomUUID();
    CategoryRequest req = new CategoryRequest();
    req.setName("categoryName");
    Category updated = new Category(req.getName());
    when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class), anyString())).thenReturn(updated);
    mockMvc.perform(put("/api/category")
        .param("category_id", categoryId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"message\":\"カスタムカテゴリの更新が完了しました\"}"));
  }

  @Test
  @Tag("PUT: /api/category")
  @DisplayName("カテゴリー更新-400 Bad Request カテゴリーが見つからない")
  void updateCategory_badRequest() throws Exception {
    UUID categoryId = UUID.randomUUID();
    CategoryRequest req = new CategoryRequest();
    req.setName("categoryName");
    when(categoryService.updateCategory(
        eq(categoryId),
        any(CategoryRequest.class),
        anyString()))
        .thenThrow(new IllegalArgumentException(categoryNotFoundMsg));
    mockMvc.perform(put("/api/category")
        .param("category_id", categoryId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"message\":\"" + categoryNotFoundMsg + "\"}"));
  }

  @Test
  @Tag("PUT: /api/category")
  @DisplayName("カテゴリー更新-400 Bad Request デフォルトカテゴリーは編集できない")
  void updateCategory_badRequest_edit_default() throws Exception {
    UUID categoryId = UUID.randomUUID();
    CategoryRequest req = new CategoryRequest();
    req.setName("categoryName");
    when(categoryService.updateCategory(
        eq(categoryId),
        any(CategoryRequest.class),
        anyString()))
        .thenThrow(new IllegalArgumentException("デフォルトカテゴリは編集できません"));
    mockMvc.perform(put("/api/category")
        .param("category_id", categoryId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"message\":\"デフォルトカテゴリは編集できません\"}"));
  }

  @Test
  @Tag("PUT: /api/category")
  @DisplayName("カテゴリー更新-500 サーバーエラー")
  void updateCategory_throws500() throws Exception {
    UUID categoryId = UUID.randomUUID();
    CategoryRequest req = new CategoryRequest();
    req.setName("categoryName");
    when(categoryService.updateCategory(
        eq(categoryId),
        any(CategoryRequest.class),
        anyString()))
        .thenThrow(new RuntimeException(serverErrorMsg));
    mockMvc.perform(put("/api/category")
        .param("category_id", categoryId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("{\"message\":\"" + serverErrorMsg + "\"}"));
  }

  @Test
  @Tag("PUT: /api/category")
  @DisplayName("カテゴリー更新-409 Conflict カテゴリー名がすでに存在する")
  void updateCategory_conflict() throws Exception {
    UUID categoryId = UUID.randomUUID();
    CategoryRequest req = new CategoryRequest();
    req.setName("updateName");
    when(categoryService.updateCategory(
        eq(categoryId),
        any(CategoryRequest.class),
        anyString()))
        .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "カテゴリー名はすでに存在します"));
    mockMvc.perform(put("/api/category")
        .param("category_id", categoryId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isConflict())
        .andExpect(content().json("{\"message\":\"カテゴリー名はすでに存在します\"}"));
  }

  @Test
  @Tag("DELETE: /api/category")
  @DisplayName("カテゴリー削除-200 OK")
  void deleteCategory_success() throws Exception {
    UUID categoryId = UUID.randomUUID();
    doNothing().when(categoryService).deleteCategory(
        eq(categoryId),
        anyString());
    mockMvc.perform(delete("/api/category")
        .param("category_id", categoryId.toString()))
        .andExpect(status().isAccepted())
        .andExpect(content().json("{\"message\":\"カスタムカテゴリの削除が完了しました\"}"));
  }

  @Test
  @Tag("DELETE: /api/category")
  @DisplayName("カテゴリー削除-404 Not Found")
  void deleteCategory_notFound() throws Exception {
    UUID categoryId = UUID.randomUUID();
    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, categoryNotFoundMsg))
        .when(categoryService).deleteCategory(eq(categoryId), anyString());
    mockMvc.perform(delete("/api/category")
        .param("category_id", categoryId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(content().json("{\"message\":\"" + categoryNotFoundMsg + "\"}"));
  }

  @Test
  @Tag("DELETE: /api/category")
  @DisplayName("カテゴリー削除-400 Bad Request デフォルトカテゴリは削除できません")
  void deleteCategory_badRequest_delete_default() throws Exception {
    UUID categoryId = UUID.randomUUID();
    doThrow(new IllegalArgumentException("デフォルトカテゴリは削除できません"))
        .when(categoryService).deleteCategory(eq(categoryId), anyString());
    mockMvc.perform(delete("/api/category")
        .param("category_id", categoryId.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"message\":\"デフォルトカテゴリは削除できません\"}"));
  }

  @Test
  @Tag("DELETE: /api/category")
  @DisplayName("カテゴリー削除-400 Bad Request アイテムが存在するため削除できません")
  void deleteCategory_badRequest_delete_with_Items_exist() throws Exception {
    UUID categoryId = UUID.randomUUID();
    doThrow(new IllegalArgumentException("アイテムが存在するため削除できません"))
        .when(categoryService).deleteCategory(eq(categoryId), anyString());
    mockMvc.perform(delete("/api/category")
        .param("category_id", categoryId.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"message\":\"アイテムが存在するため削除できません\"}"));
  }

  @Test
  @Tag("DELETE: /api/category")
  @DisplayName("カテゴリー削除-500 サーバーエラー")
  void deleteCategory_throws500() throws Exception {
    UUID categoryId = UUID.randomUUID();
    doThrow(new RuntimeException(serverErrorMsg))
        .when(categoryService).deleteCategory(eq(categoryId), anyString());
    mockMvc.perform(delete("/api/category")
        .param("category_id", categoryId.toString()))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("{\"message\":\"" + serverErrorMsg + "\"}"));
  }
}
