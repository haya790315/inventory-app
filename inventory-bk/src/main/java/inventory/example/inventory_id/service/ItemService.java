package inventory.example.inventory_id.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import inventory.example.inventory_id.dto.ItemDto;
import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.repository.CategoryRepository;
import inventory.example.inventory_id.repository.ItemRepositroy;
import inventory.example.inventory_id.request.ItemRequest;

@Service
public class ItemService {
  @Autowired
  private ItemRepositroy itemRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Value("${system.userid}")
  private int systemUserId;

  private String categoryNotFoundMsg = "カテゴリーが見つかりません";

  public void createItem(Integer userId, ItemRequest itemRequest) {

    List<Category> categoryList = categoryRepository.findByUserIdInAndName(List.of(userId, systemUserId),
        itemRequest.getCategoryName());

    if (categoryList.isEmpty()) {
      throw new IllegalArgumentException("カテゴリーが見つかりません");
    }
    Category cate = categoryList.get(0);

    // 同じ名前のアイテムが存在するかをチェック
    Optional<Item> existingItemOpt = cate.getItems().stream()
        .filter(i -> i.getName().equals(itemRequest.getName()))
        .findFirst();

    if (existingItemOpt.isPresent()) {
      throw new IllegalArgumentException("そのアイテム名は既に登録されています");
    }
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
    List<Category> categoryList = categoryRepository.findByUserIdInAndName(List.of(userId, systemUserId), categoryName);
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
        .map(item -> new ItemDto(
            item.getName(),
            category.getName(),
            item.getQuantity()))
        .toList();

    if (items.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "アイテムが登録されていません");
    }

    return items;
  }
}
