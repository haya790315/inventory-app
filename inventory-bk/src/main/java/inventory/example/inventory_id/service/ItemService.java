package inventory.example.inventory_id.service;

import inventory.example.inventory_id.dto.ItemDto;
import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.repository.CategoryRepository;
import inventory.example.inventory_id.repository.ItemRepository;
import inventory.example.inventory_id.request.ItemRequest;
import inventory.example.inventory_id.spec.ItemSpecs;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ItemService {

  private static final Logger log = LoggerFactory.getLogger(ItemService.class);

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Value("${system.userid}")
  private String systemUserId;

  private String categoryNotFoundMsg = "カテゴリーが見つかりません";

  private String itemsNotFoundMsg = "アイテムが見つかりません";

  @Caching(
    evict = {
      @CacheEvict(
        value = "items",
        key = "#userId + ':' + #itemRequest.categoryName"
      ),
      @CacheEvict(value = "categories", allEntries = true),
    }
  )
  public void createItem(String userId, ItemRequest itemRequest) {
    List<Category> categoryList = categoryRepository.findActiveCateByName(
      List.of(userId, systemUserId),
      itemRequest.getCategoryName()
    );

    if (categoryList.isEmpty()) {
      throw new IllegalArgumentException(categoryNotFoundMsg);
    }
    Category cate = categoryList.get(0);

    // 同じ名前のアイテムが存在し、削除されていない場合はエラーを投げる
    cate
      .getItems()
      .stream()
      .filter(
        i ->
          i.getName().equals(itemRequest.getName()) &&
          !i.isDeletedFlag() &&
          i.getUserId().equals(userId)
      )
      .findAny()
      .ifPresent(i -> {
        throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          String.format(
            "アイテム名 '%s' は既に存在します",
            itemRequest.getName()
          )
        );
      });

    Item item = new Item(itemRequest.getName(), userId, cate, false);
    cate.getItems().add(item);
    categoryRepository.save(cate);
  }

  @Cacheable(value = "items", key = "#userId + ':' + #categoryName")
  public Page<ItemDto> getItems(
    Pageable pageable,
    String userId,
    String categoryName
  ) {
    Specification<Item> spec = Specification.unrestricted();
    spec = spec
      .and(ItemSpecs.belongsToUser(userId))
      .and(ItemSpecs.isNotDeleted());

    if (categoryName != null) {
      String decodedCategory = URLDecoder.decode(
        categoryName,
        StandardCharsets.UTF_8
      );

      spec = spec.and(ItemSpecs.hasCategoryName(decodedCategory));
    }

    Page<Item> items = itemRepository.findAll(spec, pageable);

    return items.map(item ->
      new ItemDto(
        item.getId(),
        item.getName(),
        item.getCategory().getName(),
        item.getTotalQuantity(),
        item.getTotalPrice(),
        item.getUpdatedAt()
      )
    );
  }

  @CacheEvict(value = "items", allEntries = true)
  public void updateItem(String userId, UUID itemId, ItemRequest itemRequest) {
    // 編集するアイテムを取得
    Item item = itemRepository
      .getActiveItemWithId(List.of(userId, systemUserId), itemId)
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, itemsNotFoundMsg)
      );

    // リクエストのカテゴリー名からカテゴリーを取得
    Category category = categoryRepository
      .findActiveCateByName(
        List.of(userId, systemUserId),
        itemRequest.getCategoryName()
      )
      .stream()
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException(categoryNotFoundMsg));

    // 同じ名前のアイテムが存在し、削除されていない場合はエラーを投げる
    Optional<Item> sameNameItem =
      itemRepository.getActiveWithSameNameAndCategory(
        List.of(userId, systemUserId),
        itemRequest.getName(),
        category.getId()
      );

    // 同じ名前のアイテムが存在し、かつそれが自分自身でない場合はエラー
    if (
      sameNameItem.isPresent() &&
      !sameNameItem.get().getId().equals(item.getId())
    ) {
      throw new IllegalArgumentException("アイテム名は既に登録されています");
    }

    // アイテムの名前とカテゴリーを更新して保存
    item.setName(itemRequest.getName());
    item.setCategory(category);
    itemRepository.save(item);
  }

  @CacheEvict(value = "items", allEntries = true)
  public void deleteItem(String userId, UUID itemId) {
    // 自分とデフォルトのカテゴリーアイテムを取得
    Optional<Item> itemsOpt = itemRepository.getActiveItemWithId(
      List.of(userId, systemUserId),
      itemId
    );

    if (itemsOpt.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, itemsNotFoundMsg);
    }

    Item item = itemsOpt.get();
    item.setDeletedFlag(true);
    itemRepository.save(item);
  }
}
