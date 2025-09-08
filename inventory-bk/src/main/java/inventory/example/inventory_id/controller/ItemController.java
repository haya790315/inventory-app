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

import inventory.example.inventory_id.dto.ItemDto;
import inventory.example.inventory_id.request.ItemRequest;
import inventory.example.inventory_id.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/item")
@Tag(name = "アイテム", description = "アイテム操作のためのAPI")
public class ItemController extends BaseController {

  @Autowired
  private ItemService itemService;

  @PostMapping()
  @Operation(summary = "アイテムの作成", description = "新しいアイテムを作成します")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "アイテムの作成が完了しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"アイテムの作成が完了しました\" }"))),
      @ApiResponse(responseCode = "400", description = "カテゴリーが見つかりません", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"カテゴリーが見つかりません\" }"))),
      @ApiResponse(responseCode = "409", description = "アイテム名は既に存在します", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"アイテム名は既に存在します\" }"))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
  })
  public ResponseEntity<Object> createItem(@RequestBody @Valid ItemRequest itemRequest) {
    try {
      String userId = fetchUserIdFromToken();
      itemService.createItem(userId, itemRequest);
      return response(HttpStatus.CREATED, "アイテムの作成が完了しました");
    } catch (ResponseStatusException e) {
      return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping()
  @Operation(summary = "アイテム一覧の取得", description = "指定したカテゴリーに属するアイテム一覧を取得します")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "アイテム一覧の取得に成功しました", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ItemDto.class)))),
      @ApiResponse(responseCode = "400", description = "カテゴリーが見つかりません", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"カテゴリーが見つかりません\" }"))),
      @ApiResponse(responseCode = "404", description = "アイテムが登録されていません", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"アイテムが登録されていません\" }"))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
  })
  public ResponseEntity<Object> getItems(@RequestParam("category_name") String categoryName) {
    try {
      String userId = fetchUserIdFromToken();
      List<ItemDto> items = itemService.getItems(userId, categoryName);
      return response(HttpStatus.OK, items);
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (ResponseStatusException e) {
      return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @PutMapping()
  @Operation(summary = "アイテムの更新", description = "指定したアイテムの情報を更新します")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "アイテムの更新が完了しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"アイテムの更新が完了しました\" }"))),
      @ApiResponse(responseCode = "400", description = "アイテム名は既に登録されています", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"アイテム名は既に登録されています\" }"))),
      @ApiResponse(responseCode = "404", description = "アイテムが見つかりません", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"アイテムが見つかりません\" }"))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
  })
  public ResponseEntity<Object> updateItem(
      @RequestBody @Valid ItemRequest itemRequest,
      @RequestParam("item_id") UUID itemId) {
    try {
      String userId = fetchUserIdFromToken();
      itemService.updateItem(userId, itemId, itemRequest);
      return response(HttpStatus.OK, "アイテムの更新が完了しました");
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (ResponseStatusException e) {
      return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    } catch (Exception e) {
      System.err.println("Error updating item: " + e.getMessage());
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @DeleteMapping()
  @Operation(summary = "アイテムの削除", description = "指定したアイテムを削除します")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "アイテムの削除が完了しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"アイテムの削除が完了しました\" }"))),
      @ApiResponse(responseCode = "404", description = "アイテムが見つかりません", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"アイテムが見つかりません\" }"))),
      @ApiResponse(responseCode = "500", description = "サーバーエラーが発生しました", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"サーバーエラーが発生しました\" }")))
  })
  public ResponseEntity<Object> deleteItem(@RequestParam("item_id") UUID itemId) {
    try {
      String userId = fetchUserIdFromToken();
      itemService.deleteItem(userId, itemId);
      return response(HttpStatus.OK, "アイテムの削除が完了しました");
    } catch (ResponseStatusException e) {
      return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    } catch (Exception e) {
      System.err.println("Error deleting item: " + e.getMessage());
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }
}
