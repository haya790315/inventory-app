package inventory.example.inventory_id.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import inventory.example.inventory_id.enums.TransactionType;
import inventory.example.inventory_id.exception.AuthenticationException;
import inventory.example.inventory_id.exception.ValidationException;
import inventory.example.inventory_id.request.ItemRecordRequest;
import inventory.example.inventory_id.service.ItemRecordService;

@ExtendWith(MockitoExtension.class)
class ItemRecordControllerTest {

  @Mock
  private ItemRecordService itemRecordService;

  @Spy
  @InjectMocks
  private ItemRecordController itemRecordController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule());

  private String testUserId = "testUserId";
  private String itemNotFoundMsg = "アイテムが見つかりません";
  private String serverErrorMsg = "サーバーエラーが発生しました";
  private static String itemRecordNotFoundMsg = "指定のレコードが存在しません。";

  @BeforeEach
  void setUp() {
    Mockito.lenient().doReturn(testUserId).when(itemRecordController)
        .fetchUserIdFromToken();
    mockMvc = MockMvcBuilders.standaloneSetup(itemRecordController)
        .setControllerAdvice(new ValidationException())
        .build();
  }

  @Test
  @Tag("POST: /api/item-record")
  @DisplayName("アイテム入庫記録作成-201 Created")
  void createItemRecord_success_in() throws Exception {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        10,
        500,
        LocalDate.now(),
        TransactionType.IN);

    doNothing().when(itemRecordService).createItemRecord(anyString(),
        any(ItemRecordRequest.class));

    mockMvc.perform(post("/api/item-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(content().json("{\"message\":\"アイテム入庫しました。\"}"));
  }

  @Test
  @Tag("POST: /api/item-record")
  @DisplayName("アイテム出庫記録作成-201 Created")
  void createItemRecord_success_out() throws Exception {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        5,
        TransactionType.OUT,
        UUID.randomUUID());

    doNothing().when(itemRecordService).createItemRecord(anyString(),
        any(ItemRecordRequest.class));

    mockMvc.perform(post("/api/item-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(content().json("{\"message\":\"アイテム出庫しました。\"}"));
  }

  @Test
  @Tag("POST: /api/item-record")
  @DisplayName("アイテム記録作成-409 在庫数が不足しています。")
  void createItemRecord_responseStatusException_conflict() throws Exception {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        10,
        500,
        LocalDate.now().plusDays(30),
        TransactionType.OUT,
        UUID.randomUUID());

    doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "在庫数が不足しています。"))
        .when(itemRecordService)
        .createItemRecord(anyString(), any(ItemRecordRequest.class));

    mockMvc.perform(post("/api/item-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(content().json("{\"message\":\"在庫数が不足しています。\"}"));
  }

  @Test
  @Tag("POST: /api/item-record")
  @DisplayName("アイテム記録作成-404 指定のレコードが存在しません。")
  void createItemRecord_responseStatusException_notFound() throws Exception {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        5,
        TransactionType.OUT,
        UUID.randomUUID());

    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
        itemRecordNotFoundMsg))
        .when(itemRecordService)
        .createItemRecord(anyString(), any(ItemRecordRequest.class));

    mockMvc.perform(post("/api/item-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(content().json("""
            {"message":"%s"}
            """.formatted(itemRecordNotFoundMsg)));
  }

  @Test
  @Tag("POST: /api/item-record")
  @DisplayName("アイテム記録作成-400 アイテムが見つかりません")
  void createItemRecord_illegalArgumentException() throws Exception {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        5,
        0,
        null,
        TransactionType.IN,
        null);

    doThrow(new IllegalArgumentException(itemNotFoundMsg))
        .when(itemRecordService)
        .createItemRecord(anyString(), any(ItemRecordRequest.class));

    mockMvc.perform(post("/api/item-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("""
            {"message":"%s"}
            """.formatted(itemNotFoundMsg)));
  }

  @Test
  @Tag("POST: /api/item-record")
  @DisplayName("アイテム記録作成-500 サーバーエラーが発生しました")
  void createItemRecord_generalException() throws Exception {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        5,
        0,
        null,
        TransactionType.IN,
        null);

    doThrow(new RuntimeException(
        serverErrorMsg))
        .when(itemRecordService)
        .createItemRecord(anyString(), any(ItemRecordRequest.class));

    mockMvc.perform(post("/api/item-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("""
            {"message":"%s"}
            """.formatted(serverErrorMsg)));
  }

  @Test
  @Tag("POST: /api/item-record")
  @DisplayName("アイテム記録作成-401 認証失敗エラー")
  void createItemRecord_authenticationException() throws Exception {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        5,
        0,
        null,
        TransactionType.IN,
        null);

    doThrow(new AuthenticationException("認証に失敗しました。"))
        .when(itemRecordController).fetchUserIdFromToken();

    mockMvc.perform(post("/api/item-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("""
            {"message":"認証に失敗しました。"}
            """));
  }

  @Test
  @Tag("POST: /api/item-record")
  @DisplayName("アイテム記録作成-400 バリデーション失敗 - アイテムIDが必須")
  void createItemRecord_validation_itemIdRequired() throws Exception {
    ItemRecordRequest request = new ItemRecordRequest(
        null, // アイテムIDがnull
        5,
        0,
        null,
        TransactionType.IN,
        null);

    mockMvc.perform(post("/api/item-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("""
            {"error":"アイテムIDは必須です。"}
            """));
  }

  @Test
  @Tag("POST: /api/item-record")
  @DisplayName("アイテム記録作成-400 Bad Request バリデーション失敗 - 入出庫種別は必須です。")
  void createItemRecord_validation_sourceRequired() throws Exception {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        5,
        0,
        null,
        null, // sourceがnull
        null);

    mockMvc.perform(post("/api/item-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest()).andExpect(content().json("""
            {"error":"入出庫種別は必須です。"}
            """));
  }

  @Test
  @Tag("POST: /api/item-record")
  @DisplayName("アイテム記録作成-400 バリデーション失敗 - 数量は0以上")
  void createItemRecord_validation_quantityPositive() throws Exception {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        -1, // 数量が負の値
        0,
        null,
        TransactionType.IN,
        null);

    mockMvc.perform(post("/api/item-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest()).andExpect(content().json("""
            {"error":"数量は1以上である必要があります。"}
            """));
  }

  @Test
  @Tag("POST: /api/item-record")
  @DisplayName("アイテム記録作成-400 バリデーション失敗 - 数量が必須です。")
  void createItemRecord_validation_quantityRequired() throws Exception {
    ItemRecordRequest request = new ItemRecordRequest();
    request.setItemId(UUID.randomUUID());
    request.setPrice(100);
    request.setTransactionType(TransactionType.IN);
    // quantityを設定しない（nullのまま）

    mockMvc.perform(post("/api/item-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest()).andExpect(content().json("""
            {"error":"数量は1以上である必要があります。"}
            """));
  }

  @Test
  @Tag("POST: /api/item-record")
  @DisplayName("アイテム記録作成-400 バリデーション失敗 - 出庫にはレコードIDが必要です。")
  void createItemRecord_validation_itemRecordIdRequiredForOut() throws Exception {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        10,
        TransactionType.OUT,
        null); // itemRecordIdがnull
    mockMvc.perform(post("/api/item-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest()).andExpect(content().json("""
            {"error":"出庫にはレコードIDが必要です。"}
            """));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "2023/12/31", // US format with slashes
      "31/12/2023", // European format with slashes
      "2023.12.31", // Dot separator
      "31-12-2023", // DD-MM-YYYY format
      "12-31-2023", // MM-DD-YYYY format
      "2023年12月31日", // Japanese format
      "Dec 31, 2023", // Month name format
      "31 Dec 2023", // European month name format
      "2023-13-01", // Invalid month (13)
      "2023-12-32", // Invalid day (32)
      "2023-1-1", // Single digit month/day
      "23-12-31", // Two digit year
      "2023/2/29", // Invalid leap year date
      "", // Empty string
      "null", // String "null"
      "2023-12-31T10:30:00", // ISO datetime instead of date
      "20231231" // No separators
  })
  void testVariousInvalidDateFormats(String invalidDate) throws Exception {
    String invalidJson = """
        {
          "itemId": "%s",
          "quantity": 10,
          "price": 500,
          "expirationDate": "%s",
          "source": "IN"
        }""".formatted(UUID.randomUUID(), invalidDate);

    mockMvc.perform(post("/api/item-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidJson))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("""
            {"error":"有効期限の形式が不正です。yyyy-MM-dd形式で入力してください。"}
            """));
  }
}
