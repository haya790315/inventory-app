package inventory.example.inventory_id.service;

import inventory.example.inventory_id.enums.TransactionType;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.model.ItemRecord;
import inventory.example.inventory_id.repository.ItemRecordRepository;
import inventory.example.inventory_id.repository.ItemRepository;
import inventory.example.inventory_id.request.ItemRecordRequest;
import java.util.List;
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

    // ソースレコードを取得（出庫の場合のみ）
    ItemRecord sourceRecord = null;
    if (request.getItemRecordId() != null) {
      sourceRecord = itemRecordRepository
        .getRecordByUserIdAndId(userId, request.getItemRecordId())
        .orElseThrow(() -> new IllegalArgumentException(itemRecordNotFoundMsg));
    }

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
      return """
      %sが入庫しました\
      """.formatted(item.getName());
    }
    // 出庫の場合
    itemRecord = new ItemRecord(
      item,
      userId,
      request.getQuantity(),
      transactionType,
      sourceRecord
    );
    itemRecordRepository.save(itemRecord);
    return """
    %sが出庫しました\
    """.formatted(item.getName());
  }

  public void deleteItemRecord(Long id, String userId) {
    ItemRecord itemRecord = itemRecordRepository
      .findByIdAndUserId(id, userId)
      .orElseThrow(() -> new IllegalArgumentException(itemRecordNotFoundMsg));
    itemRecord.setDeletedFlag(true);
    itemRecordRepository.save(itemRecord);

    if (itemRecord.getTransactionType() == TransactionType.IN) {
      // 入庫レコード削除時は、関連する出庫レコードも削除
      List<ItemRecord> outRecords = itemRecord.getChildRecords();

      if (outRecords != null && !outRecords.isEmpty()) {
        for (ItemRecord outRecord : outRecords) {
          if (outRecord.isDeletedFlag()) {
            continue;
          }
          outRecord.setDeletedFlag(true);
          itemRecordRepository.save(outRecord);
        }
      }
    }
  }
}
