package inventory.example.inventory_id.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import inventory.example.inventory_id.dto.ItemDto;
import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.repository.CategoryRepository;
import inventory.example.inventory_id.repository.ItemRepository;
import inventory.example.inventory_id.request.ItemRequest;

@Service
public class ItemService {
  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Value("${system.userid}")
  private String systemUserId;

  private String categoryNotFoundMsg = "カテゴリーが見つかりません";

  private String itemsNotFoundMsg = "アイテムが見つかりません";

  public void createItem(
      String userId,
      ItemRequest itemRequest) {

    List<Category> categoryList = categoryRepository.findActiveCateByName(List.of(userId, systemUserId),
        itemRequest.getCategoryName());

    if (categoryList.isEmpty()) {
      throw new IllegalArgumentException(categoryNotFoundMsg);
    }
    Category cate = categoryList.get(0);

    // 同じ名前のアイテムが存在し、削除されていない場合はエラーを投げる
    cate.getItems().stream()
        .filter(i -> i.getName()
            .equals(itemRequest.getName()) && !i.isDeletedFlag()
            && i.getUserId().equals(userId))
        .findAny()
        .ifPresent(i -> {
          throw new ResponseStatusException(HttpStatus.CONFLICT,
              String.format("アイテム名 '%s' は既に存在します", itemRequest.getName()));
        });

    Item item = new Item(
        itemRequest.getName(),
        userId,
        cate,
        itemRequest.getQuantity(),
        false);
    cate.getItems().add(item);
    categoryRepository.save(cate);
  }

  public List<ItemDto> getItems(
      String userId,
      String categoryName) {
    List<Item> items = itemRepository.getActiveByCategoryName(List.of(userId, systemUserId), categoryName);
    if (items.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, itemsNotFoundMsg);
    }
    return items.stream()
        .map(item -> new ItemDto(
            item.getName(),
            categoryName,
            item.getQuantity()))
        .toList();
  }

  public void updateItem(
      String userId,
      UUID itemId,
      ItemRequest itemRequest) {
    // 自分とデフォルトのカテゴリーアイテムを取得
    List<Item> items = itemRepository.getActiveByCategoryName(
        List.of(userId, systemUserId),
        itemRequest.getCategoryName());
    // 編集したいアイテムを取得
    Optional<Item> matchItem = items.stream()
        .filter(i -> i.getId().equals(itemId))
        .findFirst();
    if (matchItem.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, itemsNotFoundMsg);
    }

    // 編集したい名前は他のアイテムに重複かをチェック
    List<Item> sameNamedItem = items.stream()
        .filter(
            i -> i.getName()
                .equals(itemRequest.getName()) &&
                !i.getId().equals(itemId))
        .toList();
    if (!sameNamedItem.isEmpty()) {
      throw new IllegalArgumentException("アイテム名は既に登録されています");
    }

    Item item = matchItem.get();
    item.setName(itemRequest.getName());
    item.setQuantity(itemRequest.getQuantity());
    itemRepository.save(item);
  }

  public void deleteItem(
      String userId,
      UUID itemId) {
    // 自分とデフォルトのカテゴリーアイテムを取得
    Optional<Item> itemsOpt = itemRepository
        .getActiveItemWithId(List.of(userId, systemUserId), itemId);

    if (itemsOpt.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, itemsNotFoundMsg);
    }

    Item item = itemsOpt.get();
    item.setDeletedFlag(true);
    itemRepository.save(item);
  }
}
