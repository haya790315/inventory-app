package inventory.example.inventory_id.controller;

import inventory.example.inventory_id.dto.ItemDto;
import inventory.example.inventory_id.dto.ItemRecordDto;
import inventory.example.inventory_id.exception.AuthenticationException;
import inventory.example.inventory_id.request.ItemRequest;
import inventory.example.inventory_id.response.PageResponse;
import inventory.example.inventory_id.service.ItemRecordService;
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
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/item")
@Tag(
  name = "アイテム",
  description = "アイテム管理用APIです。ユーザーはアイテムの新規作成、更新、削除、取得が可能です。各アイテムは指定のカテゴリに属して作成します。"
)
public class ItemController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(
    ItemController.class
  );

  @Autowired
  private ItemService itemService;

  @Autowired
  private ItemRecordService itemRecordService;

  @PostMapping
  @Operation(
    summary = "アイテムの作成",
    description = "新しいアイテムを作成します\n- 各アイテムは指定のカテゴリに属します\n- 同じカテゴリに属するアイテムは、アイテム名が重複して登録できません"
  )
  @ApiResponses(
    {
      @ApiResponse(
        responseCode = "201",
        description = "アイテム作成成功時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
            value = "{ \"message\": \"アイテムの作成が完了しました\" }"
          )
        )
      ),
      @ApiResponse(
        responseCode = "404",
        description = "カテゴリ名が見つからない時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
            value = "{ \"message\": \"カテゴリーが見つかりません\" }"
          )
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "インプットが不正な時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = {
            @ExampleObject(
              name = "カテゴリ名がない",
              value = "{ \"error\": \"カテゴリは必須です\" }",
              description = "カテゴリ名がない場合のレスポンス"
            ),
            @ExampleObject(
              name = "カテゴリ名が長すぎる",
              value = "{ \"error\": \"カテゴリは50文字以内で入力してください\" }",
              description = "カテゴリ名が50文字超える場合のレスポンス"
            ),
            @ExampleObject(
              name = "アイテム名がない",
              value = "{ \"error\": \"アイテム名は必須です\" }",
              description = "アイテム名がない場合のレスポンス"
            ),
            @ExampleObject(
              name = "アイテム名が長すぎる",
              value = "{ \"error\": \"アイテム名は50文字以内で入力してください\" }",
              description = "アイテム名が50文字超える場合のレスポンス"
            ),
          }
        )
      ),
      @ApiResponse(
        responseCode = "409",
        description = "アイテム名は重複時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
            value = "{ \"message\": \"アイテム名は既に存在します\" }"
          )
        )
      ),
      @ApiResponse(
        responseCode = "500",
        description = "サーバーエラーが発生時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
            value = "{ \"message\": \"サーバーエラーが発生しました\" }"
          )
        )
      ),
    }
  )
  public ResponseEntity<Object> createItem(
    @RequestBody @Valid ItemRequest itemRequest
  ) {
    try {
      String userId = fetchUserIdFromToken();
      itemService.createItem(userId, itemRequest);
      return response(HttpStatus.CREATED, "アイテムの作成が完了しました");
    } catch (ResponseStatusException e) {
      return response(
        HttpStatus.valueOf(e.getStatusCode().value()),
        e.getReason()
      );
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping
  @Operation(
    summary = "アイテム一覧の取得",
    description = "指定したカテゴリ名に属する、現在ログイン中のユーザーが所有する全てのアイテムを取得します。\n\n 取得したアイテムは、各アイテムの更新日時（updated_at）を基準に新しい順で並べて返します。"
  )
  @ApiResponses(
    {
      @ApiResponse(
        responseCode = "200",
        description = "アイテム一覧取得成功のレスポンス",
        content = @Content(
          mediaType = "application/json",
          array = @ArraySchema(schema = @Schema(implementation = ItemDto.class))
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "指定のカテゴリがない時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
            value = "{ \"message\": \"カテゴリーが見つかりません\" }"
          )
        )
      ),
      @ApiResponse(
        responseCode = "404",
        description = "指定のカテゴリに属するアイテムがない時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
            value = "{ \"message\": \"アイテムが登録されていません\" }"
          )
        )
      ),
      @ApiResponse(
        responseCode = "500",
        description = "サーバーエラーが発生時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
            value = "{ \"message\": \"サーバーエラーが発生しました\" }"
          )
        )
      ),
    }
  )
  public ResponseEntity<Object> getItems(
    Pageable pageable,
    @RequestParam(value = "category_name", required = false) String categoryName
  ) {
    try {
      String userId = fetchUserIdFromToken();

      Page<ItemDto> items = itemService.getItems(
        pageable,
        userId,
        categoryName
      );
      PageResponse<ItemDto> res = new PageResponse<>(
        items.getContent(),
        items.getNumber(),
        items.getSize(),
        items.getTotalElements(),
        items.getTotalPages()
      );
      return response(HttpStatus.OK, res);
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (AuthenticationException e) {
      return response(HttpStatus.UNAUTHORIZED, e.getMessage());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @PutMapping
  @Operation(
    summary = "アイテムの更新",
    description = "指定したアイテムの情報を更新します。\n\n- アイテム名を変更する場合、変更後の名称が既に登録しているものと重複した場合は更新できません\n\n- アイテムの属するカテゴリ変更はできません"
  )
  @ApiResponses(
    {
      @ApiResponse(
        responseCode = "200",
        description = "アイテム更新成功時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
            value = "{ \"message\": \"アイテムの更新が完了しました\" }"
          )
        )
      ),
      @ApiResponse(
        responseCode = "404",
        description = "アイテムが見つからない時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
            value = "{ \"message\": \"アイテムが見つかりません\" }"
          )
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "インプットが不正な時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = {
            @ExampleObject(
              name = "カテゴリ名がない",
              value = "{ \"error\": \"カテゴリは必須です\" }",
              description = "カテゴリ名がない場合のレスポンス"
            ),
            @ExampleObject(
              name = "カテゴリ名が長すぎる",
              value = "{ \"error\": \"カテゴリは50文字以内で入力してください\" }",
              description = "カテゴリ名が50文字超える場合のレスポンス"
            ),
            @ExampleObject(
              name = "アイテム名がない",
              value = "{ \"error\": \"アイテム名は必須です\" }",
              description = "アイテム名がない場合のレスポンス"
            ),
            @ExampleObject(
              name = "アイテム名が長すぎる",
              value = "{ \"error\": \"アイテム名は50文字以内で入力してください\" }",
              description = "アイテム名が50文字超える場合のレスポンス"
            ),
          }
        )
      ),
      @ApiResponse(
        responseCode = "500",
        description = "サーバーエラーが発生時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
            value = "{ \"message\": \"サーバーエラーが発生しました\" }"
          )
        )
      ),
    }
  )
  public ResponseEntity<Object> updateItem(
    @RequestBody @Valid ItemRequest itemRequest,
    @RequestParam("item_id") UUID itemId
  ) {
    try {
      String userId = fetchUserIdFromToken();
      itemService.updateItem(userId, itemId, itemRequest);
      return response(HttpStatus.OK, "アイテムの更新が完了しました");
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (ResponseStatusException e) {
      return response(
        HttpStatus.valueOf(e.getStatusCode().value()),
        e.getReason()
      );
    } catch (Exception e) {
      System.err.println("Error updating item: " + e.getMessage());
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @DeleteMapping
  @Operation(
    summary = "アイテムの削除",
    description = "指定したアイテムを削除します"
  )
  @ApiResponses(
    {
      @ApiResponse(
        responseCode = "200",
        description = "アイテム削除成功時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
            value = "{ \"message\": \"アイテムの削除が完了しました\" }"
          )
        )
      ),
      @ApiResponse(
        responseCode = "404",
        description = "アイテムが見つかりません時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
            value = "{ \"message\": \"アイテムが見つかりません\" }"
          )
        )
      ),
      @ApiResponse(
        responseCode = "500",
        description = "サーバーエラーが発生時のレスポンス",
        content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
            value = "{ \"message\": \"サーバーエラーが発生しました\" }"
          )
        )
      ),
    }
  )
  public ResponseEntity<Object> deleteItem(
    @RequestParam("item_id") UUID itemId
  ) {
    try {
      String userId = fetchUserIdFromToken();
      itemService.deleteItem(userId, itemId);
      return response(HttpStatus.OK, "アイテムの削除が完了しました");
    } catch (ResponseStatusException e) {
      return response(
        HttpStatus.valueOf(e.getStatusCode().value()),
        e.getReason()
      );
    } catch (Exception e) {
      System.err.println("Error deleting item: " + e.getMessage());
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/{item_id}/records")
  public ResponseEntity<Object> getItemRecords(
    @PathVariable("item_id") UUID itemId
  ) {
    try {
      String userId = fetchUserIdFromToken();
      List<ItemRecordDto> records = itemRecordService.getAllRecordsByItem(
        userId,
        itemId
      );
      return response(HttpStatus.OK, records);
    } catch (ResponseStatusException e) {
      return response(
        HttpStatus.valueOf(e.getStatusCode().value()),
        e.getReason()
      );
    } catch (AuthenticationException e) {
      return response(HttpStatus.UNAUTHORIZED, e.getMessage());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }
}
