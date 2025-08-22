package inventory.example.inventory_id.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.repository.CategoryRepo;
import inventory.example.inventory_id.repository.ItemRepo;
import inventory.example.inventory_id.request.ItemRequest;

@Service
public class ItemService {
  @Autowired
  private ItemRepo itemRepository;

  @Autowired
  private CategoryRepo categoryRepository;

  @Value("${system.userid}")
  private int systemUserId;

  public void createItem(Integer userId, ItemRequest itemRequest) {

    List<Category> categoryList = categoryRepository.findByUserIdInAndName(List.of(userId, systemUserId),
        itemRequest.getCategoryName());

    if (categoryList.isEmpty()) {
      throw new IllegalArgumentException("カテゴリーが見つかりません");
    }
    Category cate = categoryList.get(0);

    // 同じ名前のアイテムが存在し、削除されていない場合はエラーを投げる
    cate.getItems().stream()
        .filter(i -> i.getName().equals(itemRequest.getName()) && !i.isDeletedFlag())
        .findAny()
        .ifPresent(i -> {
          throw new IllegalArgumentException(String.format("アイテム名 '%s' は既に存在します", itemRequest.getName()));
        });

    Item item = new Item();
    item.setName(itemRequest.getName());
    item.setUserId(userId);
    item.setCategory(cate);
    item.setQuantity(itemRequest.getQuantity());
    cate.getItems().add(item);
    categoryRepository.save(cate);
  }
}
