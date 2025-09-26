package inventory.example.inventory_id.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.model.ItemRecord;
import inventory.example.inventory_id.repository.ItemRecordRepository;
import inventory.example.inventory_id.repository.ItemRepository;
import inventory.example.inventory_id.request.ItemRecordRequest;

@Service
public class ItemRecordService {

  private final ItemRecordRepository itemRecordRepository;
  private final ItemRepository itemRepository;
  private static String itemNotFoundMsg = "アイテムが見つかりません";
  private static String itemRecordNotFoundMsg = "指定のレコードが存在しません。";

  public ItemRecordService(ItemRecordRepository itemRecordRepository, ItemRepository itemRepository) {
    this.itemRecordRepository = itemRecordRepository;
    this.itemRepository = itemRepository;
  }

  public void createItemRecord(String userId, ItemRecordRequest request) {

    Item item = itemRepository.getActiveItemWithId(List.of(userId), request.getItemId())
        .orElseThrow(() -> new IllegalArgumentException(itemNotFoundMsg));

    if (request.getSource() == ItemRecordRequest.Source.OUT) {
      // itemRecordIdとitemIdの組み合わせが正しいかチェック
      ItemRecord itemRecord = itemRecordRepository.findByUserIdAndId(userId,
          request.getItemRecordId())
          .orElseThrow(() -> new IllegalArgumentException(itemRecordNotFoundMsg));
      if (!itemRecord.getItem().getId().equals(request.getItemId())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
            "指定のアイテムIDとレコードIDが一致しません。");
      }
      // 出庫の場合、在庫数をチェック
      Integer currentQuantity = itemRecordRepository.getRemainingQuantityForInRecord(request.getItemRecordId());
      if (currentQuantity == null) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, itemRecordNotFoundMsg);
      }
      // 在庫数が足りない場合はエラー
      if (currentQuantity < request.getQuantity()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "在庫数が不足しています。");
      }
    }

    ItemRecord itemRecord = new ItemRecord(
        item,
        userId,
        request.getQuantity(),
        request.getPrice(),
        request.getExpirationDate() != null ? request.getExpirationDate() : null,
        ItemRecord.Source.valueOf(request.getSource().name()),
        request.getItemRecordId() != null
            ? itemRecordRepository.findByUserIdAndId(userId, request.getItemRecordId())
                .orElseThrow(() -> new IllegalArgumentException(itemRecordNotFoundMsg))
            : null);
    itemRecordRepository.save(itemRecord);
  }

  public void deleteItemRecord(UUID id) {
    ItemRecord itemRecord = itemRecordRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException(itemRecordNotFoundMsg));
    itemRecordRepository.delete(itemRecord);
  }
}
