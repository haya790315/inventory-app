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

import inventory.example.inventory_id.dto.CategoryDto;
import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.repository.CategoryRepository;
import inventory.example.inventory_id.request.CategoryRequest;

@Service
public class CategoryService {
  @Autowired
  private CategoryRepository categoryRepo;

  @Value("${system.userid}")
  private int systemUserId;

  private String categoryNotFoundMsg = "カテゴリーが見つかりません";

  public List<CategoryDto> getAllCategories(int userId) {
    // ユーザとデフォルトのカテゴリを取得
    return categoryRepo.findNotDeleted(List.of(userId, systemUserId)).stream()
        .sorted(Comparator.comparing(Category::getName))
        .map(category -> new CategoryDto(category.getName(), category.getItems()))
        .toList();
  }

  public List<Item> getCategoryItems(int userId, UUID categoryId) {
    List<Category> categories = categoryRepo.findNotDeleted(List.of(userId, systemUserId));
    return categories.stream()
        .filter(category -> category.getId().equals(categoryId))
        .findFirst()
        .map(Category::getItems)
        .orElse(List.of()); // アイテムが見つからない場合は空リストを返す
  }

  public Category createCategory(CategoryRequest categoryRequest, int userId) {

    List<Category> categoryList = categoryRepo.findNotDeleted(List.of(userId, systemUserId));

    List<Category> userCategories = categoryList.stream()
        .filter(category -> category.getUserId() == userId)
        .toList();

    // ユーザカテゴリーの上限は50件まで
    if (userCategories.size() >= 50) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "登録できるカテゴリの上限に達しています");
    }
    boolean isNameExist = categoryList.stream()
        .anyMatch(category -> category.getName().equals(categoryRequest.getName()));
    if (isNameExist) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "カテゴリー名はすでに存在します");
    }

    Category category = new Category(categoryRequest.getName());
    category.setUserId(userId);
    return categoryRepo.save(category);
  }

  public Category updateCategory(UUID categoryId, CategoryRequest categoryRequest, int userId) {
    Optional<Category> categoryOpt = categoryRepo.findUserCategory(userId, categoryId);
    if (!categoryOpt.isPresent()) {
      throw new IllegalArgumentException(categoryNotFoundMsg);
    }
    Category category = categoryOpt.get();
    if (category.getUserId() != userId) {
      throw new IllegalArgumentException("デフォルトカテゴリは編集できません");
    }
    category.setName(categoryRequest.getName());
    return categoryRepo.save(category);
  }

  public void deleteCategory(UUID id, int userId) {
    List<Category> categoryList = categoryRepo.findNotDeleted(List.of(userId, systemUserId));
    if (categoryList.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, categoryNotFoundMsg);
    }
    Category category = categoryList.stream()
        .filter(cat -> cat.getId().equals(id))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, categoryNotFoundMsg));

    if (category.getUserId() != userId) {
      throw new IllegalArgumentException("デフォルトカテゴリは削除できません");
    }
    if (category.getItems().isEmpty()) {
      // アイテムが存在しない場合のみ削除フラグを立てる
      category.setDeletedFlag(true);
      categoryRepo.save(category);
    } else {
      throw new IllegalArgumentException("アイテムが存在するため削除できません");
    }
  }
}
