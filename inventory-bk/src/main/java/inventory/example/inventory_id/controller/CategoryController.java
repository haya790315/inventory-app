package inventory.example.inventory_id.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import inventory.example.inventory_id.dto.CategoryDto;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.request.CategoryRequest;
import inventory.example.inventory_id.service.CategoryService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/category")
public class CategoryController extends BaseController {
  @Autowired
  private CategoryService categoryService;

  @GetMapping()
  public ResponseEntity<Object> getAllCategories() {
    try {
      Integer userId = fetchUserIdFromToken();
      List<CategoryDto> categories = categoryService.getAllCategories(userId);
      return response(HttpStatus.OK, categories);
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/items")
  public ResponseEntity<Object> getCategoryItems(@RequestParam UUID categoryId) {
    try {
      Integer userId = fetchUserIdFromToken();
      List<Item> items = categoryService.getCategoryItems(userId, categoryId);
      return response(HttpStatus.OK, items);
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping()
  public ResponseEntity<Object> createCategory(@RequestBody @Valid CategoryRequest categoryRequest) {
    try {
      Integer userId = fetchUserIdFromToken();
      categoryService.createCategory(categoryRequest, userId);
      return response(HttpStatus.CREATED, "カスタムカテゴリの作成が完了しました");
    } catch (ResponseStatusException e) {
      return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @PutMapping()
  public ResponseEntity<Object> putMethodName(
      @RequestParam UUID category_id,
      @RequestBody CategoryRequest categoryRequest) {
    try {
      Integer userId = fetchUserIdFromToken();
      categoryService.updateCategory(category_id, categoryRequest, userId);
      return response(HttpStatus.OK, "カスタムカテゴリの更新が完了しました");
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @DeleteMapping()
  public ResponseEntity<Object> deleteCategory(@RequestParam UUID category_id) {
    try {
      Integer userId = fetchUserIdFromToken();
      categoryService.deleteCategory(category_id, userId);
      return response(HttpStatus.ACCEPTED, "カスタムカテゴリの削除が完了しました");
    } catch (ResponseStatusException e) {
      return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }
}
