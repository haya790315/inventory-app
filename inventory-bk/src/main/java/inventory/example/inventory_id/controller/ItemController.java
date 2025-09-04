package inventory.example.inventory_id.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import inventory.example.inventory_id.dto.ItemDto;
import inventory.example.inventory_id.request.ItemRequest;
import inventory.example.inventory_id.service.ItemService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/item")
public class ItemController extends BaseController {

  @Autowired
  private ItemService itemService;

  @PostMapping()
  public ResponseEntity<Object> createItem(@RequestBody @Valid ItemRequest itemRequest) {
    try {
      Integer userId = fetchUserIdFromToken();
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
  public ResponseEntity<Object> getItems(@RequestParam("category_name") String categoryName) {
    try {
      Integer userId = fetchUserIdFromToken();
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
}
