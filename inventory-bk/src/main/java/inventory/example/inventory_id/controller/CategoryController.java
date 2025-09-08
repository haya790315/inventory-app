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
import inventory.example.inventory_id.dto.ItemDto;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.request.CategoryRequest;
import inventory.example.inventory_id.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/category")
@Tag(name = "カテゴリ", description = "カテゴリ操作のためのAPI")
public class CategoryController extends BaseController {
  @Autowired
  private CategoryService categoryService;

  @GetMapping()
  @Operation(summary = "全てのカテゴリを取得", description = "ユーザーのカテゴリ一覧を取得します")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "カテゴリー取得成功", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CategoryDto.class)))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
  })
  public ResponseEntity<Object> getAllCategories() {
    try {
      String userId = fetchUserIdFromToken();
      List<CategoryDto> categories = categoryService.getAllCategories(userId);
      return response(HttpStatus.OK, categories);
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/items")
  @Operation(summary = "カテゴリに属するアイテム一覧を取得", description = "指定したカテゴリに属するアイテム一覧を取得します")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "カテゴリー取得成功", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ItemDto.class)))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
  })
  public ResponseEntity<Object> getCategoryItems(@RequestParam UUID categoryId) {
    try {
      String userId = fetchUserIdFromToken();
      List<Item> items = categoryService.getCategoryItems(userId, categoryId);
      return response(HttpStatus.OK, items);
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping()
  @Operation(summary = "カスタムカテゴリの作成", description = "新しいカスタムカテゴリを作成します")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "カスタムカテゴリの作成が完了しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"カスタムカテゴリの作成が完了しました\" }"))),
      @ApiResponse(responseCode = "409", description = "登録できるカテゴリの上限に達しています", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"登録できるカテゴリの上限に達しています\" }"))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
  })
  public ResponseEntity<Object> createCategory(@RequestBody @Valid CategoryRequest categoryRequest) {
    try {
      String userId = fetchUserIdFromToken();
      categoryService.createCategory(categoryRequest, userId);
      return response(HttpStatus.CREATED, "カスタムカテゴリの作成が完了しました");
    } catch (ResponseStatusException e) {
      return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @PutMapping()
  @Operation(summary = "カスタムカテゴリの更新", description = "指定したカスタムカテゴリを更新します")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "カスタムカテゴリの更新が完了しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"カスタムカテゴリの更新が完了しました\" }"))),
      @ApiResponse(responseCode = "400", description = "デフォルトカテゴリは編集できません", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"デフォルトカテゴリは編集できません\" }"))),
      @ApiResponse(responseCode = "409", description = "カテゴリー名はすでに存在します", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"カテゴリー名はすでに存在します\" }"))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
  })
  public ResponseEntity<Object> putMethodName(
      @RequestParam UUID category_id,
      @RequestBody @Valid CategoryRequest categoryRequest) {
    try {
      String userId = fetchUserIdFromToken();
      categoryService.updateCategory(category_id, categoryRequest, userId);
      return response(HttpStatus.OK, "カスタムカテゴリの更新が完了しました");
    } catch (ResponseStatusException e) {
      return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @DeleteMapping()
  @Operation(summary = "カスタムカテゴリの削除", description = "指定したカスタムカテゴリを削除します")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "カスタムカテゴリの削除が完了しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"カスタムカテゴリの削除が完了しました\" }"))),
      @ApiResponse(responseCode = "400", description = "デフォルトカテゴリは削除できません", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"デフォルトカテゴリは削除できません\" }"))),
      @ApiResponse(responseCode = "404", description = "指定したカテゴリが見つかりません", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"指定したカテゴリが見つかりません\" }"))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
  })
  public ResponseEntity<Object> deleteCategory(@RequestParam UUID category_id) {
    try {
      String userId = fetchUserIdFromToken();
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
