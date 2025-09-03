package inventory.example.inventory_id.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    Item item = new Item(
        itemRequest.getName(),
        userId,
        cate,
        itemRequest.getQuantity(),
        false);
    cate.getItems().add(item);
    categoryRepository.save(cate);
  }
}
