package inventory.example.inventory_id.service;

import java.util.Comparator;
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
  private int systemUserId;

  private String categoryNotFoundMsg = "カテゴリーが見つかりません";

  public void createItem(
      Integer userId,
      ItemRequest itemRequest) {

    List<Category> categoryList = categoryRepository.findActiveCateByName(List.of(userId, systemUserId),
        itemRequest.getCategoryName());

    if (categoryList.isEmpty()) {
      throw new IllegalArgumentException(categoryNotFoundMsg);
    }
    Category cate = categoryList.get(0);

    // 同じ名前のアイテムが存在し、削除されていない場合はエラーを投げる
    cate.getItems().stream()
        .filter(i -> i.getName().equals(itemRequest.getName()) && !i.isDeletedFlag())
        .findAny()
        .ifPresent(i -> {
          throw new ResponseStatusException(HttpStatus.CONFLICT,
              String.format("アイテム名 '%s' は既に存在します", itemRequest.getName()));
        });

    Item item = new Item();
    item.setName(itemRequest.getName());
    item.setUserId(userId);
    item.setCategory(cate);
    item.setQuantity(itemRequest.getQuantity());
    cate.getItems().add(item);
    categoryRepository.save(cate);
  }

  public List<ItemDto> getItems(
      Integer userId,
      String categoryName) {
    List<Category> categoryList = categoryRepository.findActiveCateByName(List.of(userId, systemUserId), categoryName);
    List<Category> notDeletedCategoryList = categoryList.stream()
        .filter(category -> !category.isDeletedFlag())
        .toList();
    if (notDeletedCategoryList.isEmpty()) {
      throw new IllegalArgumentException(categoryNotFoundMsg);
    }

    Category category = notDeletedCategoryList.get(0);
    // カテゴリーに紐づくアイテムを取得
    List<ItemDto> items = category.getItems().stream()
        .filter(item -> !item.isDeletedFlag())
        // 更新日時でソートし、DTOに変換
        .sorted(Comparator.comparing(Item::getUpdatedAt).reversed())
        .map(item -> {
          ItemDto dto = new ItemDto();
          dto.setName(item.getName());
          dto.setQuantity(item.getQuantity());
          return dto;
        }).toList();

    if (items.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "アイテムが登録されていません");
    }

    return items;
  }

  public void updateItem(
      Integer userId,
      UUID itemId,
      ItemRequest itemRequest) {
    // 自分とデフォルトのカテゴリーアイテムを取得
    List<Item> items = itemRepository.findUserActiveItemWithCategory(
        List.of(userId, systemUserId),
        itemRequest.getCategoryName());
    // 編集したいアイテムを取得
    Optional<Item> match = items.stream()
        .filter(i -> i.getId().equals(itemId))
        .findFirst();
    if (match.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "アイテムが見つかりません");
    }

    // 編集したい名前は他のアイテムに重複かをチェック
    List<Item> sameNamed = items.stream()
        .filter(i -> i.getName().equals(itemRequest.getName()) && !i.getId().equals(itemId))
        .toList();
    if (!sameNamed.isEmpty()) {
      throw new IllegalArgumentException("アイテム名は既に登録されています");
    }

    Item item = match.get();
    item.setName(itemRequest.getName());
    item.setQuantity(itemRequest.getQuantity());
    itemRepository.save(item);
  }

  public void deleteItem(
      Integer userId,
      UUID itemId) {
    // 自分とデフォルトのカテゴリーアイテムを取得
    Optional<Item> itemsOpt = itemRepository.findUserActiveItem(List.of(userId, systemUserId),
        itemId);
    if (itemsOpt.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "アイテムが見つかりません");
    }

    Item item = itemsOpt.get();
    item.setDeletedFlag(true);
    itemRepository.save(item);
  }
}
