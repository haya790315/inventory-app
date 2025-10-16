package inventory.example.inventory_id.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inventory.example.inventory_id.dto.ItemRecordDto;
import inventory.example.inventory_id.enums.TransactionType;
import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.model.ItemRecord;
import inventory.example.inventory_id.model.ItemRecord;
import inventory.example.inventory_id.repository.ItemRecordRepository;
import inventory.example.inventory_id.repository.ItemRecordRepository;
import inventory.example.inventory_id.repository.ItemRepository;
import inventory.example.inventory_id.repository.ItemRepository;
import inventory.example.inventory_id.request.ItemRecordRequest;
import inventory.example.inventory_id.request.ItemRecordRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemRecordService Tests")
public class ItemRecordServiceTest {

  @Mock
  private ItemRecordRepository itemRecordRepository;

  @Mock
  private ItemRepository itemRepository;

  @InjectMocks
  private ItemRecordService itemRecordService;

  private String testUserId;
  private UUID testItemId;
  private Long testItemRecordId;
  private Item testItem;
  private Category testCategory;
  private ItemRecord testItemRecord;

  private LocalDate timeNow;

  private static String itemNotFoundMsg = "アイテムが見つかりません";
  private static String itemRecordNotFoundMsg =
    "指定のレコードが存在しません。";

  @BeforeEach
  void setUp() {
    testUserId = "testUser";
    testItemId = UUID.randomUUID();
    testItemRecordId = 1L;
    timeNow = LocalDate.now();

    testCategory = new Category("Test Category", testUserId);
    testCategory.setId(UUID.randomUUID());

    testItem = new Item("Test Item", testUserId, testCategory, false);
    testItem.setId(testItemId);

    testItemRecord = new ItemRecord(
      testItem,
      testUserId,
      100,
      50,
      timeNow,
      TransactionType.IN,
      null
    );
    testItemRecord.setId(testItemRecordId);
  }

  @Test
  @DisplayName("入庫記録作成 - 正常系")
  void createItemRecord_success_in() {
    ItemRecordRequest request = new ItemRecordRequest(
      testItemId,
      10,
      500,
      timeNow,
      TransactionType.IN
    );

    when(
      itemRepository.getActiveItemWithId(List.of(testUserId), testItemId)
    ).thenReturn(Optional.of(testItem));

    itemRecordService.createItemRecord(testUserId, request);

    ArgumentCaptor<ItemRecord> itemRecordCaptor = ArgumentCaptor.forClass(
      ItemRecord.class
    );
    verify(itemRecordRepository).save(itemRecordCaptor.capture());

    ItemRecord savedRecord = itemRecordCaptor.getValue();
    assertThat(savedRecord.getItem()).isEqualTo(testItem);
    assertThat(savedRecord.getUserId()).isEqualTo(testUserId);
    assertThat(savedRecord.getQuantity()).isEqualTo(10);
    assertThat(savedRecord.getPrice()).isEqualTo(500);
    assertThat(savedRecord.getExpirationDate()).isEqualTo(timeNow);
    assertThat(savedRecord.getTransactionType()).isEqualTo(TransactionType.IN);
    assertThat(savedRecord.getSourceRecord()).isNull();
  }

  @Test
  @DisplayName("入庫記録作成成功 - itemRecordIdがnullの場合")
  void createItemRecord_success_in_with_null_itemRecordId() {
    ItemRecordRequest request = new ItemRecordRequest(
      testItemId,
      500,
      10,
      timeNow,
      TransactionType.IN
    );

    when(
      itemRepository.getActiveItemWithId(List.of(testUserId), testItemId)
    ).thenReturn(Optional.of(testItem));

    assertDoesNotThrow(() ->
      itemRecordService.createItemRecord(testUserId, request)
    );
  }

  @Test
  @DisplayName("入庫記録作成成功 - 最小値での作成（数量1、価格0）")
  void createItemRecord_success_in_with_minimum_values() {
    ItemRecordRequest request = new ItemRecordRequest(
      testItemId,
      1, // 最小価格
      0, // 最小数量
      timeNow,
      TransactionType.IN
    );

    when(
      itemRepository.getActiveItemWithId(List.of(testUserId), testItemId)
    ).thenReturn(Optional.of(testItem));
    assertDoesNotThrow(() ->
      itemRecordService.createItemRecord(testUserId, request)
    );
  }

  @Test
  @DisplayName("入庫記録作成成功 - 有効期限がnullの場合")
  void createItemRecord_success_in_with_null_expiration_date() {
    ItemRecordRequest request = new ItemRecordRequest(
      testItemId,
      500,
      10,
      null, // 有効期限がnull
      TransactionType.IN
    );

    when(
      itemRepository.getActiveItemWithId(List.of(testUserId), testItemId)
    ).thenReturn(Optional.of(testItem));
    assertDoesNotThrow(() -> {
      itemRecordService.createItemRecord(testUserId, request);
    });
    verify(itemRecordRepository).save(any(ItemRecord.class));
  }

  @Test
  @DisplayName("入庫記録作成成功 - 単価がnullの場合は0が設定される")
  void createItemRecord_success_in_with_null_price() {
    ItemRecordRequest request = new ItemRecordRequest();
    request.setItemId(testItemId);
    request.setQuantity(10);
    request.setTransactionType(TransactionType.IN);
    // priceを設定しない（nullのまま）

    when(
      itemRepository.getActiveItemWithId(List.of(testUserId), testItemId)
    ).thenReturn(Optional.of(testItem));
    itemRecordService.createItemRecord(testUserId, request);
    ArgumentCaptor<ItemRecord> itemRecordCaptor = ArgumentCaptor.forClass(
      ItemRecord.class
    );
    verify(itemRecordRepository).save(itemRecordCaptor.capture());

    ItemRecord savedRecord = itemRecordCaptor.getValue();
    assertThat(savedRecord.getPrice()).isEqualTo(0);
  }

  @Test
  @DisplayName("入庫記録作成失敗 - アイテムが見つからない")
  void createItemRecord_throws_exception_when_item_not_found() {
    ItemRecordRequest request = new ItemRecordRequest(
      testItemId,
      500,
      10,
      LocalDate.now().plusDays(30),
      TransactionType.IN
    );

    when(
      itemRepository.getActiveItemWithId(List.of(testUserId), testItemId)
    ).thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> itemRecordService.createItemRecord(testUserId, request)
    );

    assertThat(exception.getMessage()).isEqualTo(itemNotFoundMsg);
    verify(itemRecordRepository, times(0)).save(any(ItemRecord.class));
  }

  @Test
  @DisplayName("出庫記録作成成功 - 十分な在庫がある場合")
  void createItemRecord_success_out_with_sufficient_stock() {
    ItemRecordRequest request = new ItemRecordRequest(
      testItemId,
      10, // 出庫数量
      TransactionType.OUT,
      testItemRecordId // 元の入庫記録ID
    );

    when(
      itemRepository.getActiveItemWithId(List.of(testUserId), testItemId)
    ).thenReturn(Optional.of(testItem));
    when(
      itemRecordRepository.getRecordByUserIdAndId(testUserId, testItemRecordId)
    ).thenReturn(Optional.of(testItemRecord));
    when(
      itemRecordRepository.getInrecordRemainQuantity(testItemRecordId)
    ).thenReturn(50); // 残り在庫50個

    itemRecordService.createItemRecord(testUserId, request);

    ArgumentCaptor<ItemRecord> itemRecordCaptor = ArgumentCaptor.forClass(
      ItemRecord.class
    );
    verify(itemRecordRepository).save(itemRecordCaptor.capture());

    ItemRecord savedRecord = itemRecordCaptor.getValue();
    assertThat(savedRecord.getItem()).isEqualTo(testItem);
    assertThat(savedRecord.getUserId()).isEqualTo(testUserId);
    assertThat(savedRecord.getQuantity()).isEqualTo(10);
    assertThat(savedRecord.getTransactionType()).isEqualTo(TransactionType.OUT);
    assertThat(savedRecord.getSourceRecord()).isEqualTo(testItemRecord);
  }

  @Test
  @DisplayName("出庫記録作成成功 - 在庫数と同じ数量を出庫する場合")
  void createItemRecord_success_out_with_exact_stock_quantity() {
    ItemRecordRequest request = new ItemRecordRequest(
      testItemId,
      50, // 残り在庫と同じ数量
      TransactionType.OUT,
      testItemRecordId
    );

    when(
      itemRepository.getActiveItemWithId(List.of(testUserId), testItemId)
    ).thenReturn(Optional.of(testItem));
    when(
      itemRecordRepository.getRecordByUserIdAndId(testUserId, testItemRecordId)
    ).thenReturn(Optional.of(testItemRecord));
    when(
      itemRecordRepository.getInrecordRemainQuantity(testItemRecordId)
    ).thenReturn(50); // 残り在庫50個

    assertDoesNotThrow(() ->
      itemRecordService.createItemRecord(testUserId, request)
    );

    ArgumentCaptor<ItemRecord> itemRecordCaptor = ArgumentCaptor.forClass(
      ItemRecord.class
    );
    verify(itemRecordRepository).save(itemRecordCaptor.capture());

    ItemRecord savedRecord = itemRecordCaptor.getValue();
    assertThat(savedRecord.getQuantity()).isEqualTo(50);
    assertThat(savedRecord.getTransactionType()).isEqualTo(TransactionType.OUT);
  }

  @Test
  @DisplayName("出庫記録作成失敗 - 在庫数が不足している")
  void createItemRecord_throws_exception_when_insufficient_stock() {
    ItemRecordRequest request = new ItemRecordRequest(
      testItemId,
      60, // 出庫数量60個（在庫50個より多い）
      TransactionType.OUT,
      testItemRecordId
    );

    when(
      itemRepository.getActiveItemWithId(List.of(testUserId), testItemId)
    ).thenReturn(Optional.of(testItem));
    when(
      itemRecordRepository.getRecordByUserIdAndId(testUserId, testItemRecordId)
    ).thenReturn(Optional.of(testItemRecord));
    when(
      itemRecordRepository.getInrecordRemainQuantity(testItemRecordId)
    ).thenReturn(50); // 残り在庫50個

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> itemRecordService.createItemRecord(testUserId, request)
    );

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getReason()).isEqualTo("在庫数が不足しています。");
    verify(itemRecordRepository, times(0)).save(any(ItemRecord.class));
  }

  @Test
  @DisplayName("出庫記録作成失敗 - sourceRecordが出庫のレコード")
  void createItemRecord_throws_exception_when_item_record_not_found() {
    Long outRecordId = 2L;
    ItemRecordRequest request = new ItemRecordRequest(
      testItemId,
      10,
      TransactionType.OUT,
      outRecordId
    );
    ItemRecord outRecord = new ItemRecord(
      testItem,
      testUserId,
      10,
      100,
      LocalDate.now(),
      TransactionType.OUT,
      testItemRecord
    );
    when(
      itemRepository.getActiveItemWithId(List.of(testUserId), testItemId)
    ).thenReturn(Optional.of(testItem));

    when(
      itemRecordRepository.getRecordByUserIdAndId(testUserId, outRecordId)
    ).thenReturn(Optional.of(outRecord)); // 出庫レコードを返す

    when(
      itemRecordRepository.getInrecordRemainQuantity(outRecordId)
    ).thenReturn(null);

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> itemRecordService.createItemRecord(testUserId, request)
    );
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(exception.getReason()).isEqualTo(itemRecordNotFoundMsg);
    verify(itemRecordRepository, times(0)).save(any(ItemRecord.class));
  }

  @Test
  @DisplayName("出庫記録作成失敗 - sourceRecordが見つからない")
  void createItemRecord_throws_exception_when_source_record_not_found() {
    ItemRecordRequest request = new ItemRecordRequest(
      testItemId,
      10,
      TransactionType.OUT,
      testItemRecordId
    );

    when(
      itemRepository.getActiveItemWithId(List.of(testUserId), testItemId)
    ).thenReturn(Optional.of(testItem));

    when(
      itemRecordRepository.getRecordByUserIdAndId(testUserId, testItemRecordId)
    ).thenReturn(Optional.empty()); // sourceRecordが見つからない

    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> itemRecordService.createItemRecord(testUserId, request)
    );

    assertThat(exception.getMessage()).isEqualTo(itemRecordNotFoundMsg);
    verify(itemRecordRepository, times(0)).save(any(ItemRecord.class));
  }

  @Test
  @DisplayName("出庫記録作成失敗 - アイテムIDとレコードIDが一致しない場合")
  void createItemRecord_throws_exception_when_itemId_and_recordId_do_not_match() {
    UUID anotherItemId = UUID.randomUUID();
    Item anotherItem = new Item(
      "Another Item",
      testUserId,
      testCategory,
      false
    );

    ItemRecord outRecord = new ItemRecord(
      testItem,
      testUserId,
      10,
      TransactionType.OUT,
      testItemRecord
    );

    ItemRecordRequest request = new ItemRecordRequest(
      anotherItemId, // outRecordと異なるアイテムID
      10,
      TransactionType.OUT,
      outRecord.getId()
    );

    when(
      itemRepository.getActiveItemWithId(List.of(testUserId), anotherItemId)
    ).thenReturn(Optional.of(anotherItem));
    when(
      itemRecordRepository.getRecordByUserIdAndId(testUserId, outRecord.getId())
    ).thenReturn(Optional.of(outRecord));

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> itemRecordService.createItemRecord(testUserId, request)
    );

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(exception.getReason()).isEqualTo(
      "指定のアイテムIDとレコードIDが一致しません。"
    );
    verify(itemRecordRepository, times(0)).save(any(ItemRecord.class));
  }

  @Test
  @DisplayName("出庫記録作成失敗 - 在庫数より1個多く出庫しようとする場合")
  void createItemRecord_throws_exception_when_one_more_than_stock() {
    ItemRecordRequest request = new ItemRecordRequest(
      testItemId,
      51, // 在庫50個より1個多い
      TransactionType.OUT,
      testItemRecordId
    );

    when(
      itemRepository.getActiveItemWithId(List.of(testUserId), testItemId)
    ).thenReturn(Optional.of(testItem));
    when(
      itemRecordRepository.getRecordByUserIdAndId(testUserId, testItemRecordId)
    ).thenReturn(Optional.of(testItemRecord));
    when(
      itemRecordRepository.getInrecordRemainQuantity(testItemRecordId)
    ).thenReturn(50); // 残り在庫50個

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> itemRecordService.createItemRecord(testUserId, request)
    );

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getReason()).isEqualTo("在庫数が不足しています。");
  }

  @Test
  @DisplayName("履歴削除成功 - 正常系")
  void deleteItemRecord_success() {
    when(
      itemRecordRepository.findByIdAndUserId(testItemRecordId, testUserId)
    ).thenReturn(Optional.of(testItemRecord));
    assertDoesNotThrow(() ->
      itemRecordService.deleteItemRecord(testItemRecordId, testUserId)
    );
    verify(itemRecordRepository, times(1)).save(testItemRecord);
    assertThat(testItemRecord.isDeletedFlag()).isEqualTo(true);
  }

  @Test
  @DisplayName("履歴削除成功 - 関連する子レコードは削除される")
  void deleteItemRecord_throws_exception_when_child_records_exist() {
    ItemRecord outRecord1 = new ItemRecord(
      testItem,
      testUserId,
      10,
      TransactionType.OUT,
      testItemRecord
    );
    ItemRecord outRecord2 = new ItemRecord(
      testItem,
      testUserId,
      5,
      TransactionType.OUT,
      testItemRecord
    );
    testItemRecord.setChildRecords(List.of(outRecord1, outRecord2));
    when(
      itemRecordRepository.findByIdAndUserId(testItemRecordId, testUserId)
    ).thenReturn(Optional.of(testItemRecord));

    assertDoesNotThrow(() ->
      itemRecordService.deleteItemRecord(testItemRecordId, testUserId)
    );
    verify(itemRecordRepository, times(1)).save(testItemRecord);

    for (ItemRecord childRecord : testItemRecord.getChildRecords()) {
      verify(itemRecordRepository, times(1)).save(childRecord);
      assertThat(childRecord.isDeletedFlag()).isEqualTo(true);
    }
  }

  @Test
  @DisplayName("履歴削除失敗 - ユーザIDと履歴の所有者が異なる場合")
  void deleteItemRecord_throws_exception_when_record_not_found() {
    String otherUserId = "other-user-ID";
    when(
      itemRecordRepository.findByIdAndUserId(testItemRecordId, otherUserId)
    ).thenReturn(Optional.empty());
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> itemRecordService.deleteItemRecord(testItemRecordId, otherUserId)
    );
    assertThat(exception.getMessage()).isEqualTo(itemRecordNotFoundMsg);

    verify(itemRecordRepository, times(0)).delete(any(ItemRecord.class));
  }

  @Test
  @DisplayName("履歴取得 - 正常系（入庫）")
  void getItemRecord_success() {
    when(
      itemRecordRepository.findByIdAndUserId(testItemRecordId, testUserId)
    ).thenReturn(Optional.of(testItemRecord));

    ItemRecordDto result = itemRecordService.getItemRecord(
      testItemRecordId,
      testUserId
    );

    assertThat(result).isNotNull();
    assertThat(result.getItemName()).isEqualTo(testItem.getName());
    assertThat(result.getCategoryName()).isEqualTo(testItem.getCategoryName());
    assertThat(result.getQuantity()).isEqualTo(testItemRecord.getQuantity());
    assertThat(result.getPrice()).isEqualTo(testItemRecord.getPrice());
    assertThat(result.getSource()).isEqualTo(testItemRecord.getSource());
    assertThat(result.getExpirationDate()).isEqualTo(
      testItemRecord.getExpirationDate().toString()
    );
  }

  @Test
  @DisplayName("履歴取得 - 正常系（出庫）")
  void getItemRecord_success_out() {
    ItemRecord outRecord = new ItemRecord(
      testItem,
      testUserId,
      5,
      ItemRecord.Source.OUT,
      testItemRecord
    );

    when(
      itemRecordRepository.findByIdAndUserId(outRecord.getId(), testUserId)
    ).thenReturn(Optional.of(outRecord));

    ItemRecordDto result = itemRecordService.getItemRecord(
      outRecord.getId(),
      testUserId
    );

    assertThat(result).isNotNull();
    assertThat(result.getItemName()).isEqualTo(outRecord.getItemName());
    assertThat(result.getCategoryName()).isEqualTo(
      outRecord.getItem().getCategoryName()
    );
    assertThat(result.getQuantity()).isEqualTo(outRecord.getQuantity());
    assertThat(result.getSource()).isEqualTo(outRecord.getSource());
    assertThat(result.getExpirationDate()).isNull();
  }

  @Test
  @DisplayName("履歴取得失敗400 - 存在しない履歴を取得しようとした場合")
  void getItemRecord_throws_exception_when_record_not_found() {
    when(
      itemRecordRepository.findByIdAndUserId(testItemRecordId, testUserId)
    ).thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> itemRecordService.getItemRecord(testItemRecordId, testUserId)
    );
    assertThat(exception.getMessage()).isEqualTo(itemRecordNotFoundMsg);
  }
}
