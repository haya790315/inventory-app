package inventory.example.inventory_id.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import inventory.example.inventory_id.dto.CategoryDto;
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

  private int testUserId = 111;
  private String categoryNotFoundMsg = "カテゴリーが見つかりません";

  @BeforeEach
  void setUp() {
    doReturn(testUserId).when(categoryController).fetchUserIdFromToken();
    mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
  }

  @Test
  @Tag("GET: /api/category")
  @DisplayName("カテゴリー一覧取得-200 OK")
  void getAllCategories_usesStubbedUserId() throws Exception {
    List<CategoryDto> categories = Arrays.asList(new CategoryDto(), new CategoryDto());
    when(categoryService.getAllCategories(anyInt())).thenReturn(categories);

    mockMvc.perform(get("/api/category"))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(categories)));
  }

  @Test
  @Tag("GET: /api/category/items")
  @DisplayName("カテゴリーアイテム取得-200 OK")
  void getCategoryItems_returnsItems() throws Exception {
    UUID categoryId = UUID.randomUUID();
    List<Item> items = Arrays.asList(new Item(), new Item());
    when(categoryService.getCategoryItems(anyInt(), any(UUID.class))).thenReturn(items);
    mockMvc.perform(get("/api/category/items").param("categoryId", categoryId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(items)));
  }

  @Test
  @Tag("POST: /api/category")
  @DisplayName("カテゴリー作成-200 OK")
  void createCategory_success() throws Exception {
    String name = "new";
    CategoryRequest req = new CategoryRequest();
    req.setName(name);
    Category created = new Category(name);
    when(categoryService.createCategory(any(CategoryRequest.class), anyInt())).thenReturn(created);
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
    when(categoryService.createCategory(any(CategoryRequest.class), anyInt()))
        .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
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
    when(categoryService.createCategory(any(CategoryRequest.class), anyInt()))
        .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
            "カテゴリー名はすでに存在します"));
    mockMvc.perform(post("/api/category")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isConflict())
        .andExpect(content().json("{\"message\":\"カテゴリー名はすでに存在します\"}"));
  }

  @Test
  @Tag("PUT: /api/category")
  @DisplayName("カテゴリー更新-200 OK")
  void updateCategory_success() throws Exception {
    UUID categoryId = UUID.randomUUID();
    CategoryRequest req = new CategoryRequest();
    Category updated = new Category(req.getName());
    when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class), anyInt())).thenReturn(updated);
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
    when(categoryService.updateCategory(
        eq(categoryId),
        any(CategoryRequest.class),
        anyInt()))
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
    when(categoryService.updateCategory(
        eq(categoryId),
        any(CategoryRequest.class),
        anyInt()))
        .thenThrow(new IllegalArgumentException("デフォルトカテゴリは編集できません"));
    mockMvc.perform(put("/api/category")
        .param("category_id", categoryId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"message\":\"デフォルトカテゴリは編集できません\"}"));
  }

  @Test
  @Tag("DELETE: /api/category")
  @DisplayName("カテゴリー削除-200 OK")
  void deleteCategory_success() throws Exception {
    UUID categoryId = UUID.randomUUID();
    doNothing().when(categoryService).deleteCategory(
        eq(categoryId),
        anyInt());
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
        .when(categoryService).deleteCategory(eq(categoryId), anyInt());
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
        .when(categoryService).deleteCategory(eq(categoryId), anyInt());
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
        .when(categoryService).deleteCategory(eq(categoryId), anyInt());
    mockMvc.perform(delete("/api/category")
        .param("category_id", categoryId.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"message\":\"アイテムが存在するため削除できません\"}"));
  }
}
