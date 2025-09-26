package inventory.example.inventory_id.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.model.ItemRecord;
import inventory.example.inventory_id.repository.ItemRecordRepository;
import inventory.example.inventory_id.repository.ItemRepository;
import inventory.example.inventory_id.request.ItemRecordRequest;

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
  private UUID testItemRecordId;
  private Item testItem;
  private Category testCategory;
  private ItemRecord testItemRecord;

  private LocalDate timeNow;

  @BeforeEach
  void setUp() {
    testUserId = "testUser";
    testItemId = UUID.randomUUID();
    testItemRecordId = UUID.randomUUID();
    timeNow = LocalDate.now();

    testCategory = new Category(
        "Test Category",
        testUserId);
    testCategory.setId(UUID.randomUUID());

    testItem = new Item("Test Item", testUserId, testCategory, false);
    testItem.setId(testItemId);
    // Create test item record (for OUT operations)
    testItemRecord = new ItemRecord(
        testItem,
        testUserId,
        100,
        50,
        timeNow,
        ItemRecord.Source.IN,
        null);
    testItemRecord.setId(testItemRecordId);
  }

  @Test
  @DisplayName("入庫記録作成 -　正常系")
  void createItemRecord_success_in() {
    ItemRecordRequest request = new ItemRecordRequest(
        testItemId,
        10,
        500,
        timeNow,
        ItemRecordRequest.Source.IN);

    when(itemRepository.getActiveItemWithId(List.of(
        testUserId),
        testItemId))
        .thenReturn(Optional.of(testItem));

    itemRecordService.createItemRecord(testUserId, request);

    ArgumentCaptor<ItemRecord> itemRecordCaptor = ArgumentCaptor.forClass(ItemRecord.class);
    verify(itemRecordRepository).save(itemRecordCaptor.capture());

    ItemRecord savedRecord = itemRecordCaptor.getValue();
    assertThat(savedRecord.getItem()).isEqualTo(testItem);
    assertThat(savedRecord.getUserId()).isEqualTo(testUserId);
    assertThat(savedRecord.getQuantity()).isEqualTo(10);
    assertThat(savedRecord.getPrice()).isEqualTo(500);
    assertThat(savedRecord.getExpirationDate()).isEqualTo(timeNow);
    assertThat(savedRecord.getSource()).isEqualTo(ItemRecord.Source.IN);
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
        ItemRecordRequest.Source.IN);

    when(itemRepository.getActiveItemWithId(List.of(testUserId), testItemId))
        .thenReturn(Optional.of(testItem));

    assertDoesNotThrow(() -> itemRecordService.createItemRecord(testUserId, request));
  }

  @Test
  @DisplayName("入庫記録作成成功 - 最小値での作成（数量1、価格0）")
  void createItemRecord_success_in_with_minimum_values() {
    ItemRecordRequest request = new ItemRecordRequest(
        testItemId,
        1, // 最小価格
        0, // 最小数量
        timeNow,
        ItemRecordRequest.Source.IN);

    when(itemRepository.getActiveItemWithId(List.of(testUserId), testItemId))
        .thenReturn(Optional.of(testItem));
    assertDoesNotThrow(() -> itemRecordService.createItemRecord(testUserId, request));
  }

  @Test
  @DisplayName("入庫記録作成成功 - 有効期限がnullの場合")
  void createItemRecord_success_in_with_null_expiration_date() {
    ItemRecordRequest request = new ItemRecordRequest(
        testItemId,
        500,
        10,
        null, // 有効期限がnull
        ItemRecordRequest.Source.IN);

    when(itemRepository.getActiveItemWithId(List.of(testUserId), testItemId))
        .thenReturn(Optional.of(testItem));
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
    request.setSource(ItemRecordRequest.Source.IN);
    // priceを設定しない（nullのまま）

    when(itemRepository.getActiveItemWithId(List.of(testUserId), testItemId))
        .thenReturn(Optional.of(testItem));
    itemRecordService.createItemRecord(testUserId, request);
    ArgumentCaptor<ItemRecord> itemRecordCaptor = ArgumentCaptor.forClass(ItemRecord.class);
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
        ItemRecordRequest.Source.IN);

    when(itemRepository.getActiveItemWithId(List.of(testUserId), testItemId))
        .thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> itemRecordService.createItemRecord(testUserId, request));

    assertThat(exception.getMessage()).isEqualTo("アイテムが見つかりません");
    verify(itemRecordRepository, times(0)).save(any(ItemRecord.class));
  }

  @Test
  @DisplayName("出庫記録作成成功 - 十分な在庫がある場合")
  void createItemRecord_success_out_with_sufficient_stock() {
    ItemRecordRequest request = new ItemRecordRequest(
        testItemId,
        10, // 出庫数量
        ItemRecordRequest.Source.OUT,
        testItemRecordId // 元の入庫記録ID
    );

    when(itemRepository.getActiveItemWithId(List.of(testUserId),
        testItemId))
        .thenReturn(Optional.of(testItem));
    when(itemRecordRepository.findByUserIdAndId(testUserId, testItemRecordId))
        .thenReturn(Optional.of(testItemRecord));
    when(itemRecordRepository.getRemainingQuantityForInRecord(testItemRecordId))
        .thenReturn(50); // 残り在庫50個

    itemRecordService.createItemRecord(testUserId, request);

    ArgumentCaptor<ItemRecord> itemRecordCaptor = ArgumentCaptor.forClass(ItemRecord.class);
    verify(itemRecordRepository).save(itemRecordCaptor.capture());

    ItemRecord savedRecord = itemRecordCaptor.getValue();
    assertThat(savedRecord.getItem()).isEqualTo(testItem);
    assertThat(savedRecord.getUserId()).isEqualTo(testUserId);
    assertThat(savedRecord.getQuantity()).isEqualTo(10);
    assertThat(savedRecord.getSource()).isEqualTo(ItemRecord.Source.OUT);
    assertThat(savedRecord.getSourceRecord()).isEqualTo(testItemRecord);
  }

  @Test
  @DisplayName("出庫記録作成成功 - 在庫数と同じ数量を出庫する場合")
  void createItemRecord_success_out_with_exact_stock_quantity() {
    ItemRecordRequest request = new ItemRecordRequest(
        testItemId,
        50, // 残り在庫と同じ数量
        ItemRecordRequest.Source.OUT,
        testItemRecordId);

    when(itemRepository.getActiveItemWithId(List.of(testUserId), testItemId))
        .thenReturn(Optional.of(testItem));
    when(itemRecordRepository.findByUserIdAndId(testUserId, testItemRecordId))
        .thenReturn(Optional.of(testItemRecord));
    when(itemRecordRepository.getRemainingQuantityForInRecord(testItemRecordId))
        .thenReturn(50); // 残り在庫50個

    assertDoesNotThrow(() -> itemRecordService.createItemRecord(testUserId, request));

    ArgumentCaptor<ItemRecord> itemRecordCaptor = ArgumentCaptor.forClass(ItemRecord.class);
    verify(itemRecordRepository).save(itemRecordCaptor.capture());

    ItemRecord savedRecord = itemRecordCaptor.getValue();
    assertThat(savedRecord.getQuantity()).isEqualTo(50);
    assertThat(savedRecord.getSource()).isEqualTo(ItemRecord.Source.OUT);
  }

  @Test
  @DisplayName("出庫記録作成失敗 - 在庫数が不足している")
  void createItemRecord_throws_exception_when_insufficient_stock() {
    ItemRecordRequest request = new ItemRecordRequest(
        testItemId,
        60, // 出庫数量60個（在庫50個より多い）
        ItemRecordRequest.Source.OUT,
        testItemRecordId);

    when(itemRepository.getActiveItemWithId(List.of(testUserId), testItemId))
        .thenReturn(Optional.of(testItem));
    when(itemRecordRepository.findByUserIdAndId(testUserId, testItemRecordId))
        .thenReturn(Optional.of(testItemRecord));
    when(itemRecordRepository.getRemainingQuantityForInRecord(testItemRecordId))
        .thenReturn(50); // 残り在庫50個

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> itemRecordService.createItemRecord(testUserId, request));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(exception.getReason()).isEqualTo("在庫数が不足しています。");
    verify(itemRecordRepository, times(0)).save(any(ItemRecord.class));
  }

  @Test
  @DisplayName("出庫記録作成失敗 - sourceRecordが出庫のレコード")
  void createItemRecord_throws_exception_when_item_record_not_found() {
    UUID outRecordId = UUID.randomUUID();
    ItemRecordRequest request = new ItemRecordRequest(
        testItemId,
        10,
        ItemRecordRequest.Source.OUT,
        outRecordId);
    ItemRecord outRecord = new ItemRecord(
        testItem,
        testUserId,
        10,
        100,
        LocalDate.now(),
        ItemRecord.Source.OUT,
        testItemRecord);
    when(itemRepository.getActiveItemWithId(List.of(testUserId), testItemId))
        .thenReturn(Optional.of(testItem));

    when(itemRecordRepository.findByUserIdAndId(testUserId,
        outRecordId))
        .thenReturn(Optional.of(outRecord)); // 出庫レコードを返す

    when(itemRecordRepository.getRemainingQuantityForInRecord(
        outRecordId))
        .thenReturn(null);

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> itemRecordService.createItemRecord(testUserId, request));
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(exception.getReason()).isEqualTo("指定のレコードが存在しません。");
    verify(itemRecordRepository, times(0)).save(any(ItemRecord.class));
  }

  @Test
  @DisplayName("出庫記録作成失敗 - sourceRecordが見つからない")
  void createItemRecord_throws_exception_when_source_record_not_found() {
    ItemRecordRequest request = new ItemRecordRequest(
        testItemId,
        10,
        ItemRecordRequest.Source.OUT,
        testItemRecordId);

    when(itemRepository.getActiveItemWithId(List.of(testUserId), testItemId))
        .thenReturn(Optional.of(testItem));

    when(itemRecordRepository.findByUserIdAndId(testUserId, testItemRecordId))
        .thenReturn(Optional.empty()); // sourceRecordが見つからない

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> itemRecordService.createItemRecord(testUserId, request));

    assertThat(exception.getMessage()).isEqualTo("指定のレコードが存在しません。");
    verify(itemRecordRepository, times(0)).save(any(ItemRecord.class));
  }

  @Test
  @DisplayName("出庫記録作成失敗 - アイテムIDとレコードIDが一致しない場合")
  void createItemRecord_throws_exception_when_itemId_and_recordId_do_not_match() {
    UUID anotherItemId = UUID.randomUUID();
    Item anotherItem = new Item("Another Item", testUserId, testCategory, false);

    ItemRecord outRecord = new ItemRecord(
        testItem,
        testUserId,
        10,
        ItemRecord.Source.OUT,
        testItemRecord);

    ItemRecordRequest request = new ItemRecordRequest(
        anotherItemId, // outRecordと異なるアイテムID
        10,
        ItemRecordRequest.Source.OUT,
        outRecord.getId());

    when(itemRepository.getActiveItemWithId(List.of(testUserId), anotherItemId))
        .thenReturn(Optional.of(anotherItem));
    when(itemRecordRepository.findByUserIdAndId(testUserId, outRecord.getId()))
        .thenReturn(Optional.of(outRecord));

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> itemRecordService.createItemRecord(testUserId, request));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(exception.getReason()).isEqualTo("指定のアイテムIDとレコードIDが一致しません。");
    verify(itemRecordRepository, times(0)).save(any(ItemRecord.class));
  }

  @Test
  @DisplayName("出庫記録作成失敗 - 在庫数より1個多く出庫しようとする場合")
  void createItemRecord_throws_exception_when_one_more_than_stock() {
    ItemRecordRequest request = new ItemRecordRequest(
        testItemId,
        51, // 在庫50個より1個多い
        ItemRecordRequest.Source.OUT,
        testItemRecordId);

    when(itemRepository.getActiveItemWithId(List.of(testUserId), testItemId))
        .thenReturn(Optional.of(testItem));
    when(itemRecordRepository.findByUserIdAndId(testUserId, testItemRecordId))
        .thenReturn(Optional.of(testItemRecord));
    when(itemRecordRepository.getRemainingQuantityForInRecord(testItemRecordId))
        .thenReturn(50); // 残り在庫50個

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> itemRecordService.createItemRecord(testUserId, request));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(exception.getReason()).isEqualTo("在庫数が不足しています。");
  }
}
