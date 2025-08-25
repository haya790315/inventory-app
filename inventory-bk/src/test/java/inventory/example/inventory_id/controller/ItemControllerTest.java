package inventory.example.inventory_id.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import inventory.example.inventory_id.service.ItemService;

@ExtendWith(MockitoExtension.class)
public class ItemControllerTest {

  @Mock
  private ItemService itemService;

  @Spy
  @InjectMocks
  private ItemController itemController;

  private MockMvc mockMvc;

  private int testUserId = 111;
  private String categoryNotFoundMsg = "カテゴリーが見つかりません";

  @BeforeEach
  void setUp() {
    doReturn(testUserId).when(itemController).fetchUserIdFromToken();
    mockMvc = MockMvcBuilders.standaloneSetup(itemController).build();
  }

  @Test
  @Tag("DELETE: /api/item")
  @DisplayName("アイテム削除-202 Accepted")
  void deleteItem_success() throws Exception {
    UUID itemId = UUID.randomUUID();
    doNothing().when(itemService).deleteItem(anyInt(), eq(itemId));
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
    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, categoryNotFoundMsg)).when(itemService).deleteItem(
        anyInt(),
        eq(itemId));
    mockMvc.perform(delete("/api/item")
        .param("item_id", itemId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(content().json("{\"message\":\"" + categoryNotFoundMsg + "\"}"));
  }
}
