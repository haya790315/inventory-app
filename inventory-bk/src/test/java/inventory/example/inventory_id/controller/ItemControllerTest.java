package inventory.example.inventory_id.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import inventory.example.inventory_id.dto.ItemDto;
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

  private int testUserId = 111;
  private String categoryNotFoundMsg = "カテゴリーが見つかりません";
  private String serverErrorMsg = "サーバーエラーが発生しました";

  @BeforeEach
  void setUp() {
    doReturn(testUserId).when(itemController).fetchUserIdFromToken();
    mockMvc = MockMvcBuilders.standaloneSetup(itemController).build();
  }

  @Test
  @Tag("GET: /api/item")
  @DisplayName("アイテム一覧取得-200 OK")
  void getItems_success() throws Exception {
    List<ItemDto> items = Arrays.asList(new ItemDto(), new ItemDto());
    when(itemService.getItems(anyInt(), anyString())).thenReturn(items);
    mockMvc.perform(get("/api/item").param("category_name", "test"))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(items)));
  }

  @Test
  @Tag("GET: /api/item")
  @DisplayName("アイテム一覧取得-400 不正な引数")
  void getItems_throws400() throws Exception {
    when(itemService.getItems(anyInt(), anyString()))
        .thenThrow(new IllegalArgumentException(categoryNotFoundMsg));
    mockMvc.perform(get("/api/item").param("category_name", "test"))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"message\":\"" + categoryNotFoundMsg + "\"}"));
  }

  @Test
  @Tag("GET: /api/item")
  @DisplayName("アイテム一覧取得-404 アイテムが見つからない")
  void getItems_throws404() throws Exception {
    when(itemService.getItems(anyInt(), anyString()))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "アイテムが登録されていません"));
    mockMvc.perform(get("/api/item").param("category_name", "test"))
        .andExpect(status().isNotFound())
        .andExpect(content().json("{\"message\":\"アイテムが登録されていません\"}"));
  }

  @Test
  @Tag("GET: /api/item")
  @DisplayName("アイテム一覧取得-500 サーバーエラー")
  void getItems_throws500() throws Exception {
    when(itemService.getItems(anyInt(), anyString()))
        .thenThrow(new RuntimeException(serverErrorMsg));
    mockMvc.perform(get("/api/item").param("category_name", "test"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("{\"message\":\"" + serverErrorMsg + "\"}"));
  }
}
