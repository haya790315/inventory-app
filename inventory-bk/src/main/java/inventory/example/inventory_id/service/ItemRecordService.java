package inventory.example.inventory_id.service;

import inventory.example.inventory_id.dto.ItemRecordDto;
import inventory.example.inventory_id.enums.TransactionType;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.model.ItemRecord;
import inventory.example.inventory_id.repository.ItemRecordRepository;
import inventory.example.inventory_id.repository.ItemRepository;
import inventory.example.inventory_id.request.ItemRecordRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ItemRecordService {

  private final ItemRecordRepository itemRecordRepository;
  private final ItemRepository itemRepository;
  private static String itemNotFoundMsg = "アイテムが見つかりません";
  private static String itemRecordNotFoundMsg =
    "指定のレコードが存在しません。";

  public ItemRecordService(
    ItemRecordRepository itemRecordRepository,
    ItemRepository itemRepository
  ) {
    this.itemRecordRepository = itemRecordRepository;
    this.itemRepository = itemRepository;
  }

  @Caching(
    evict = {
      @CacheEvict(
        //getUserItemRecordsのキャッシュ削除
        value = "itemRecord",
        key = "#userId"
      ),
      @CacheEvict(
        //getAllRecordsByItemのキャッシュ削除
        value = "itemRecord",
        key = "#userId + ':' + #request.getItemId()"
      ),
      @CacheEvict(value = "items", allEntries = true),
    }
  )
  public String createItemRecord(String userId, ItemRecordRequest request) {
    Item item = itemRepository
      .getActiveItemWithId(List.of(userId), request.getItemId())
      .orElseThrow(() -> new IllegalArgumentException(itemNotFoundMsg));

    if (request.getTransactionType() == TransactionType.OUT) {
      // itemRecordIdとitemIdの組み合わせが正しいかチェック
      ItemRecord itemRecord = itemRecordRepository
        .getRecordByUserIdAndId(userId, request.getItemRecordId())
        .orElseThrow(() -> new IllegalArgumentException(itemRecordNotFoundMsg));
      if (!itemRecord.getItem().getId().equals(request.getItemId())) {
        throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "指定のアイテムIDとレコードIDが一致しません。"
        );
      }
      // 出庫の場合、在庫数をチェック
      Integer currentQuantity = itemRecordRepository.getInrecordRemainQuantity(
        request.getItemRecordId()
      );
      if (currentQuantity == null) {
        throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          itemRecordNotFoundMsg
        );
      }
      // 在庫数が足りない場合はエラー
      if (currentQuantity < request.getQuantity()) {
        throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "在庫数が不足しています。"
        );
      }
    }

    // トランザクションタイプを変換
    TransactionType transactionType = TransactionType.valueOf(
      request.getTransactionType().name()
    );

    // アイテムレコードを作成
    ItemRecord itemRecord;
    if (transactionType == TransactionType.IN) {
      // 入庫の場合
      itemRecord = new ItemRecord(
        item,
        userId,
        request.getQuantity(),
        request.getPrice(),
        request.getExpirationDate(),
        transactionType
      );
      itemRecordRepository.save(itemRecord);

      updateItemSummary(userId, item);

      return """
      %sが入庫しました\
      """.formatted(item.getName());
    }
    // ソースレコードを取得（出庫の場合のみ）
    ItemRecord sourceRecord = null;
    if (request.getItemRecordId() != null) {
      sourceRecord = itemRecordRepository
        .getRecordByUserIdAndId(userId, request.getItemRecordId())
        .orElseThrow(() -> new IllegalArgumentException(itemRecordNotFoundMsg));
    }
    itemRecord = new ItemRecord(
      item,
      userId,
      request.getQuantity(),
      sourceRecord.getPrice(),
      sourceRecord.getExpirationDate(),
      transactionType,
      sourceRecord
    );
    itemRecordRepository.save(itemRecord);

    updateItemSummary(userId, item);

    return """
    %sが出庫しました\
    """.formatted(item.getName());
  }

  @Caching(
    evict = {
      @CacheEvict(value = "itemRecord", allEntries = true),
      @CacheEvict(value = "items", allEntries = true),
    }
  )
  public List<Long> deleteItemRecord(Long id, String userId) {
    ItemRecord itemRecord = itemRecordRepository
      .findByIdAndUserId(id, userId)
      .orElseThrow(() -> new IllegalArgumentException(itemRecordNotFoundMsg));
    itemRecord.setDeletedFlag(true);
    itemRecordRepository.save(itemRecord);

    List<Long> deletedIds = new ArrayList<>(List.of(id));

    if (itemRecord.getTransactionType() == TransactionType.IN) {
      // 入庫レコード削除時は、関連する出庫レコードも削除
      List<ItemRecord> outRecords = itemRecord.getChildRecords();

      if (outRecords != null && !outRecords.isEmpty()) {
        for (ItemRecord outRecord : outRecords) {
          if (outRecord.isDeletedFlag()) {
            continue;
          }
          outRecord.setDeletedFlag(true);
          deletedIds.add(outRecord.getId());
          itemRecordRepository.save(outRecord);
        }
      }
    }

    Item item = itemRecord.getItem();

    updateItemSummary(userId, item);

    return deletedIds;
  }

  @Cacheable(value = "itemRecord", key = "#userId + ':' + #id")
  public ItemRecordDto getItemRecord(Long id, String userId) {
    return itemRecordRepository
      .findByIdAndUserId(id, userId)
      .stream()
      .map(record ->
        new ItemRecordDto(
          record.getItem().getName(),
          record.getItem().getCategory().getName(),
          record.getQuantity(),
          record.getPrice(),
          record.getTransactionType(),
          record.getExpirationDate() != null
            ? record.getExpirationDate()
            : null,
          record.getCreatedAt()
        )
      )
      .findFirst()
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, itemRecordNotFoundMsg)
      );
  }

  @Cacheable(value = "itemRecord", key = "#userId")
  public List<ItemRecordDto> getUserItemRecords(String userId) {
    List<ItemRecord> itemRecords = itemRecordRepository.findUserItemRecords(
      userId
    );
    return itemRecords
      .stream()
      .map(record ->
        new ItemRecordDto(
          record.getItem().getName(),
          record.getItem().getCategoryName(),
          record.getQuantity(),
          record.getPrice(),
          record.getTransactionType(),
          record.getExpirationDate() != null
            ? record.getExpirationDate()
            : null,
          record.getCreatedAt()
        )
      )
      .toList();
  }

  @Cacheable(value = "itemRecord", key = "#userId + ':' + #itemId")
  public List<ItemRecordDto> getAllRecordsByItem(String userId, UUID itemId) {
    Item item = itemRepository
      .getActiveItemWithId(List.of(userId), itemId)
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, itemNotFoundMsg)
      );
    List<ItemRecord> itemRecords =
      itemRecordRepository.getRecordsByItemIdAndUserId(item.getId(), userId);
    return itemRecords
      .stream()
      .map(record ->
        new ItemRecordDto(
          record.getItem().getName(),
          record.getItem().getCategoryName(),
          record.getQuantity(),
          record.getPrice(),
          record.getTransactionType(),
          record.getExpirationDate() != null
            ? record.getExpirationDate()
            : null,
          record.getCreatedAt()
        )
      )
      .toList();
  }

  /**
   * アイテムの管理および在庫集計を行うサービスクラスです。
   * アイテムの在庫数・在庫金額の集計処理を提供します。
   */
  static class ItemSummary {

    int quantity;
    int price;
  }

  /**
   * 指定ユーザー・アイテムの全レコードから在庫数・在庫金額を集計し、アイテム情報を更新します。
   *
   * @param userId ユーザーID
   * @param item 集計対象のアイテム
   */
  public void updateItemSummary(String userId, Item item) {
    List<ItemRecord> itemRecords =
      itemRecordRepository.getRecordsByItemIdAndUserId(item.getId(), userId);

    ItemSummary itemSummary = itemRecords
      .stream()
      .reduce(
        new ItemSummary(),
        (aggregateSummary, record) -> {
          if (record.getTransactionType() == TransactionType.IN) {
            aggregateSummary.quantity += record.getQuantity();
            aggregateSummary.price += record.getQuantity() * record.getPrice();
          } else {
            aggregateSummary.quantity -= record.getQuantity();
            aggregateSummary.price -= record.getQuantity() * record.getPrice();
          }
          return aggregateSummary;
        },
        (reducedSummary, nextSummary) -> {
          reducedSummary.quantity += nextSummary.quantity;
          reducedSummary.price += nextSummary.price;
          return reducedSummary;
        }
      );

    item.setTotalQuantity(itemSummary.quantity);
    item.setTotalPrice(itemSummary.price);
    itemRepository.save(item);
  }
}
