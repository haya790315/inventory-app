package inventory.example.inventory_id.controller;

import inventory.example.inventory_id.dto.ItemRecordDto;
import inventory.example.inventory_id.request.ItemRecordRequest;
import inventory.example.inventory_id.request.ItemRecordRequest;
import inventory.example.inventory_id.service.ItemRecordService;
import inventory.example.inventory_id.service.ItemRecordService;
import jakarta.validation.Valid;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/item-record")
public class ItemRecordController extends BaseController {

  private final ItemRecordService itemRecordService;

  public ItemRecordController(ItemRecordService itemRecordService) {
    this.itemRecordService = itemRecordService;
  }

  private final String ITEM_RECORD_DELETED = "入出庫履歴を削除しました";

  @PostMapping
  public ResponseEntity<Object> createItemRecord(
    @RequestBody @Valid ItemRecordRequest request
  ) {
    try {
      String userId = fetchUserIdFromToken();
      String returnMessage = itemRecordService.createItemRecord(
        userId,
        request
      );
      return response(HttpStatus.CREATED, returnMessage);
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

  @DeleteMapping
  public ResponseEntity<Object> deleteItemRecord(
    @RequestParam("record_id") Long recordId
  ) {
    try {
      String userId = fetchUserIdFromToken();
      List<Long> deletedRecordIds = itemRecordService.deleteItemRecord(
        recordId,
        userId
      );
      Map<String, Object> data = Map.of(
        "message",
        ITEM_RECORD_DELETED,
        "deletedRecordIds",
        deletedRecordIds
      );
      return response(HttpStatus.ACCEPTED, data);
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping
  public ResponseEntity<Object> getItemRecord(
    @RequestParam("record_id") UUID recordId
  ) {
    try {
      String userId = fetchUserIdFromToken();
      ItemRecordDto itemRecord = itemRecordService.getItemRecord(
        recordId,
        userId
      );
      return response(HttpStatus.OK, itemRecord);
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }
}
