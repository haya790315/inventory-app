package inventory.example.inventory_id.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

  private int testUserId = 111;
  private String categoryNotFoundMsg = "カテゴリーが見つかりません";
  private String serverErrorMsg = "サーバーエラーが発生しました";

  @BeforeEach
  void setUp() {
    doReturn(testUserId).when(itemController).fetchUserIdFromToken();
    mockMvc = MockMvcBuilders.standaloneSetup(itemController).build();
  }

  @Test
  @Tag("POST: /api/item")
  @DisplayName("アイテム作成-201 Created")
  void createItem_success() throws Exception {
    ItemRequest req = new ItemRequest();
    req.setName("itemName");
    req.setCategoryName("category");
    req.setQuantity(1);
    doNothing().when(itemService).createItem(anyInt(), any(ItemRequest.class));
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
    ItemRequest req = new ItemRequest();
    req.setName("itemName");
    req.setCategoryName("category");
    req.setQuantity(1);
    doThrow(new IllegalArgumentException(categoryNotFoundMsg)).when(itemService).createItem(anyInt(),
        any(ItemRequest.class));
    mockMvc.perform(post("/api/item")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"message\":\"" + categoryNotFoundMsg + "\"}"));
  }

  @Test
  @Tag("POST: /api/item")
  @DisplayName("アイテム作成-400 Bad Request アイテム名が重複")
  void createItem_badRequest_itemNameDuplicate() throws Exception {
    ItemRequest req = new ItemRequest();
    req.setName("itemName");
    req.setCategoryName("category");
    req.setQuantity(1);
    doThrow(new ResponseStatusException(HttpStatus.CONFLICT, String.format("アイテム名 '%s' は既に存在します", req.getName())))
        .when(itemService)
        .createItem(anyInt(),
            any(ItemRequest.class));
    mockMvc.perform(post("/api/item")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isConflict())
        .andExpect(content().json("{\"message\":\"" + String.format("アイテム名 '%s' は既に存在します", req.getName()) + "\"}"));
  }

  @Test
  @Tag("POST: /api/item")
  @DisplayName("アイテム作成-500 サーバーエラー")
  void createItem_throws500() throws Exception {
    ItemRequest req = new ItemRequest();
    req.setName("itemName");
    req.setCategoryName("category");
    doThrow(new RuntimeException(
        serverErrorMsg))
        .when(itemService)
        .createItem(anyInt(),
            any(ItemRequest.class));
    mockMvc.perform(post("/api/item")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("{\"message\":\"" + serverErrorMsg + "\"}"));
  }
}
