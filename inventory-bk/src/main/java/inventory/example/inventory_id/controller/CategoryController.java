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
@Tag(name = "カテゴリ", description = "カテゴリ管理用APIです。ユーザーはカテゴリの新規作成、更新、削除、取得が可能です。システムには複数のデフォルトカテゴリが用意されており、これらは全ユーザーが利用できますが、デフォルトカテゴリは編集・削除できません。ユーザー自身が作成したカスタムカテゴリについては、自由に編集・削除が可能です。カテゴリ取得時にはデフォルトカテゴリとユーザー作成カテゴリが辞書順一覧で返されます。")
public class CategoryController extends BaseController {
  @Autowired
  private CategoryService categoryService;

  @GetMapping()
  @Operation(summary = "カテゴリの取得", description = "システムのデフォルトカテゴリも含めてユーザ自分のカテゴリ一覧を取得、辞書順に表示する")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "カテゴリ一覧取得、辞書順に表示する", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CategoryDto.class)))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生時のレスポンス", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
  })
  public ResponseEntity<Object> getAllCategories() {
    try {
      String userId = fetchUserIdFromToken();
      List<CategoryDto> categories = categoryService.getAllCategories(userId);
      return response(HttpStatus.OK, categories);
    } catch (ResponseStatusException e) {
      return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/items")
  @Operation(summary = "カテゴリに属するアイテム一覧を取得", description = "指定したカテゴリに属するアイテム一覧を取得します、更新日時の降順で表示します")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "カテゴリー取得成功時のレスポンス", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ItemDto.class)))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生時のレスポンス", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
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
  @Operation(summary = "カスタムカテゴリの作成", description = "新しいカスタムカテゴリを作成します\n- 各ユーザは最大50個のカスタムカテゴリを作成できます\n- カテゴリ名は重複できません。")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "カスタムカテゴリ作成成功時のレスポンス", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"カスタムカテゴリの作成が完了しました\" }"))),
      @ApiResponse(responseCode = "400", description = "インプットが不正な時のレスポンス", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "カテゴリ名がない", value = "{ \"error\": \"カテゴリ名は必須\" }", description = "カテゴリ名がない場合のレスポンス"),
          @ExampleObject(name = "カテゴリ名が長すぎる", value = "{ \"error\": \"カテゴリ名は50文字以内\" }", description = "カテゴリ名が50文字を超える場合のレスポンス")
      })),
      @ApiResponse(responseCode = "409", description = "カテゴリ作成失敗時のレスポンス", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"登録できるカテゴリの上限に達しています\" }"))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生時のレスポンス", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
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
  @Operation(summary = "カスタムカテゴリの更新", description = "指定のカスタムカテゴリを更新します\n- デフォルトカテゴリは編集できません\n- 変更するカテゴリ名は重複できません")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "カテゴリ更新成功時のレスポンス", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"カスタムカテゴリの更新が完了しました\" }"))),
      @ApiResponse(responseCode = "400", description = "デフォルトカテゴリは更新不可", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"デフォルトカテゴリは編集できません\" }"))),
      @ApiResponse(responseCode = "400", description = "インプットが不正な時のレスポンス", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "カテゴリ名がない", value = "{ \"error\": \"カテゴリ名は必須\" }", description = "カテゴリ名がない場合のレスポンス"),
          @ExampleObject(name = "カテゴリ名が長すぎる", value = "{ \"error\": \"カテゴリ名は50文字以内\" }", description = "カテゴリ名が50文字を超える場合のレスポンス")
      })),
      @ApiResponse(responseCode = "409", description = "カテゴリ更新失敗時のレスポンス", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"カテゴリー名はすでに存在します\" }"))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生時のレスポンス", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
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
  @Operation(summary = "カスタムカテゴリの削除", description = "指定のカスタムカテゴリを削除します\n- デフォルトカテゴリは削除できません\n- 指定のカテゴリにアイテムが存在する場合は削除できません")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "カスタムカテゴリ削除時のレスポンス", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"カスタムカテゴリの削除が完了しました\" }"))),
      @ApiResponse(responseCode = "400", description = "カテゴリを削除不可のレスポンス", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "デフォルトカテゴリを削除", value = "{ \"message\": \"デフォルトカテゴリは削除できません\" }", description = "デフォルトカテゴリを削除しようとした場合のレスポンス"),
          @ExampleObject(name = "アイテムが存在のカテゴリを削除", value = "{ \"message\": \"アイテムが存在するため削除できません\" }", description = "指定のカテゴリにアイテムが存在する場合のレスポンス") })),
      @ApiResponse(responseCode = "404", description = "指定のカテゴリがない時のレスポンス", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"指定したカテゴリが見つかりません\" }"))),
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
