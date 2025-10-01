package inventory.example.inventory_id.controller;

import java.util.List;
import java.util.UUID;

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

import inventory.example.inventory_id.dto.ItemRecordDto;
import inventory.example.inventory_id.request.ItemRecordRequest;
import inventory.example.inventory_id.service.ItemRecordService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/item-record")
public class ItemRecordController extends BaseController {

  private final ItemRecordService itemRecordService;

  public ItemRecordController(ItemRecordService itemRecordService) {
    this.itemRecordService = itemRecordService;
  }

  @PostMapping
  public ResponseEntity<Object> createItemRecord(@RequestBody @Valid ItemRecordRequest request) {
    try {
      String userId = fetchUserIdFromToken();
      itemRecordService.createItemRecord(userId, request);
      return response(HttpStatus.CREATED,
          request.getSource().equals(ItemRecordRequest.Source.IN) ? "アイテム入庫しました。" : "アイテム出庫しました。");
    } catch (ResponseStatusException e) {
      return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @DeleteMapping()
  public ResponseEntity<Object> deleteItemRecord(@RequestParam("record_id") UUID recordId) {
    try {
      String userId = fetchUserIdFromToken();
      itemRecordService.deleteItemRecord(recordId, userId);
      return response(HttpStatus.ACCEPTED, "入出庫履歴を削除しました");
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping()
  public ResponseEntity<Object> getItemRecord(@RequestParam("record_id") UUID recordId) {
    try {
      String userId = fetchUserIdFromToken();
      ItemRecordDto itemRecord = itemRecordService.getItemRecord(recordId, userId);
      return response(HttpStatus.OK, itemRecord);
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/history")
  public ResponseEntity<Object> getUserItemRecords() {
    try {
      String userId = fetchUserIdFromToken();
      List<ItemRecordDto> itemRecords = itemRecordService.getUserItemRecords(userId);
      return response(HttpStatus.OK, itemRecords);
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }
}
