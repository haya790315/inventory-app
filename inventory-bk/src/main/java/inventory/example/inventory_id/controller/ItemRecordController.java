package inventory.example.inventory_id.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
      String returnMessage = itemRecordService.createItemRecord(userId, request);
      return response(HttpStatus.CREATED, returnMessage);
    } catch (ResponseStatusException e) {
      return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    } catch (IllegalArgumentException e) {
      return response(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }
}
