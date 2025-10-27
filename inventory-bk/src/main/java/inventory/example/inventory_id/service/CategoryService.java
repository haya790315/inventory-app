package inventory.example.inventory_id.service;

import inventory.example.inventory_id.dto.CategoryDto;
import inventory.example.inventory_id.dto.ItemDto;
import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.repository.CategoryRepository;
import inventory.example.inventory_id.request.CategoryRequest;
import inventory.example.inventory_id.spec.CategorySpecs;
import inventory.example.inventory_id.spec.ItemSpecs;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoryService {

  @Autowired
  private CategoryRepository categoryRepository;

  @Value("${system.userid}")
  private String systemUserId;

  private String categoryNotFoundMsg = "カテゴリーが見つかりません";

  @Cacheable(
    value = "categories",
    key = "#userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()"
  )
  public Page<CategoryDto> getAllCategories(Pageable pageable, String userId) {
    Specification<Category> spec = Specification.unrestricted();
    spec = spec
      .and(CategorySpecs.belongsToUser(List.of(userId, systemUserId)))
      .and(CategorySpecs.isNotDeleted());
    // ユーザとデフォルトのカテゴリを取得

    Page<Category> categories = categoryRepository.findAll(spec, pageable);

    return categories.map(category ->
      new CategoryDto(
        category.getId(),
        category.getName(),
        (int) category
          .getItems()
          .stream()
          .filter(
            item -> !item.isDeletedFlag() && item.getUserId().equals(userId)
          )
          .count(),
        category.getUpdatedAt()
      )
    );
    // List<CategoryDto> dtoList = categories
    //   .getContent()
    //   .stream()
    //   .sorted(Comparator.comparing(Category::getName))
    //   .map(category ->
    //     new CategoryDto(
    //       category.getId(),
    //       category.getName(),
    //       (int) category.getItems().stream().count(),
    //       category.getUpdatedAt()
    //     )
    //   )
    //   .toList();
    // return new PageImpl<>(dtoList, pageable, categories.getTotalElements());
  }

  public List<CategoryDto> getAllCategories(String userId) {
    List<Category> categories = categoryRepository.findNotDeleted(
      List.of(userId, systemUserId)
    );
    return categories
      .stream()
      .sorted(Comparator.comparing(Category::getName))
      .map(category ->
        new CategoryDto(
          category.getId(),
          category.getName(),
          (int) category
            .getItems()
            .stream()
            .filter(
              item -> !item.isDeletedFlag() && item.getUserId().equals(userId)
            )
            .count(),
          category.getUpdatedAt()
        )
      )
      .toList();
  }

  @Cacheable(value = "categoryItems", key = "#userId + ':' + #categoryId")
  public List<ItemDto> getCategoryItems(String userId, UUID categoryId) {
    List<Category> categories = categoryRepository.findNotDeleted(
      List.of(userId, systemUserId)
    );
    // ユーザのカテゴリを取得
    Optional<Category> categoryOpt = categories
      .stream()
      .filter(category -> category.getId().equals(categoryId))
      .findFirst();

    if (categoryOpt.isEmpty()) {
      return List.of();
    }

    Category category = categoryOpt.get();
    // ユーザのアイテムを更新日時の降順で取得
    List<Item> sortedUserItems = category
      .getItems()
      .stream()
      .filter(item -> item.getUserId().equals(userId) && !item.isDeletedFlag())
      .sorted(Comparator.comparing(Item::getUpdatedAt).reversed())
      .toList();
    // アイテムDTOのリストを作成
    List<ItemDto> itemDtos = sortedUserItems
      .stream()
      .map(item ->
        new ItemDto(
          item.getId(),
          item.getName(),
          item.getCategoryName(),
          0,
          0,
          item.getUpdatedAt()
        )
      )
      .toList();

    return itemDtos;
  }

  @CacheEvict(value = "categories", key = "#userId", allEntries = true)
  public Category createCategory(
    CategoryRequest categoryRequest,
    String userId
  ) {
    List<Category> categoryList = categoryRepository.findNotDeleted(
      List.of(userId, systemUserId)
    );

    List<Category> userCategories = categoryList
      .stream()
      .filter(category -> category.getUserId().equals(userId))
      .toList();

    // ユーザカテゴリーの上限は50件まで
    if (userCategories.size() >= 50) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "登録できるカテゴリの上限に達しています"
      );
    }
    boolean isNameExist = categoryList
      .stream()
      .anyMatch(category -> category.getName().equals(categoryRequest.getName())
      );
    if (isNameExist) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "カテゴリー名はすでに存在します"
      );
    }

    Category category = new Category(categoryRequest.getName());
    category.setUserId(userId);
    return categoryRepository.save(category);
  }

  @CacheEvict(value = "categories", key = "#userId", allEntries = true)
  public Category updateCategory(
    UUID categoryId,
    CategoryRequest categoryRequest,
    String userId
  ) {
    Optional<Category> categoryOpt = categoryRepository.findUserCategory(
      List.of(userId, systemUserId),
      categoryId
    );
    if (!categoryOpt.isPresent()) {
      throw new IllegalArgumentException(categoryNotFoundMsg);
    }
    Category category = categoryOpt.get();
    if (!category.getUserId().equals(userId)) {
      throw new IllegalArgumentException("デフォルトカテゴリは編集できません");
    }
    List<Category> exsitCategory = categoryRepository.findActiveCateByName(
      List.of(userId, systemUserId),
      categoryRequest.getName()
    );

    if (!exsitCategory.isEmpty()) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "カテゴリー名はすでに存在します"
      );
    }

    category.setName(categoryRequest.getName());
    return categoryRepository.save(category);
  }

  @CacheEvict(value = "categories", key = "#userId", allEntries = true)
  public void deleteCategory(UUID id, String userId) {
    List<Category> categoryList = categoryRepository.findNotDeleted(
      List.of(userId, systemUserId)
    );
    if (categoryList.isEmpty()) {
      throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        categoryNotFoundMsg
      );
    }
    Category category = categoryList
      .stream()
      .filter(cat -> cat.getId().equals(id))
      .findFirst()
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, categoryNotFoundMsg)
      );

    if (!category.getUserId().equals(userId)) {
      throw new IllegalArgumentException("デフォルトカテゴリは削除できません");
    }
    if (
      category.getItems().isEmpty() ||
      category.getItems().stream().allMatch(item -> item.isDeletedFlag())
    ) {
      // アイテムが存在しない場合のみ削除フラグを立てる
      category.setDeletedFlag(true);
      categoryRepository.save(category);
    } else {
      throw new IllegalArgumentException(
        "アイテムが存在するため削除できません"
      );
    }
  }
}
