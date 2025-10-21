package inventory.example.inventory_id.controller;

import inventory.example.inventory_id.dto.CategoryDto;
import inventory.example.inventory_id.dto.ItemDto;
import inventory.example.inventory_id.dto.ItemRecordDto;
import inventory.example.inventory_id.enums.TransactionType;
import inventory.example.inventory_id.exception.AuthenticationException;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.repository.ItemRepository;
import inventory.example.inventory_id.request.CategoryRequest;
import inventory.example.inventory_id.request.ItemRecordRequest;
import inventory.example.inventory_id.request.ItemRequest;
import inventory.example.inventory_id.service.CategoryService;
import inventory.example.inventory_id.service.ItemRecordService;
import inventory.example.inventory_id.service.ItemService;
import inventory.example.inventory_id.util.TimeUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Web controller for serving Thymeleaf templates
 * This controller handles the web UI for the inventory management system
 */
@Controller
public class WebController extends BaseController {

  private final ItemRepository itemRepository;

  @Autowired
  private ItemService itemService;

  @Autowired
  private CategoryService categoryService;

  @Autowired
  private ItemRecordService itemRecordService;

  private String apiUrl = "http://localhost:8080/api/";

  @Autowired
  private RestTemplate restTemplate;

  private static final Logger log = LoggerFactory.getLogger(
    WebController.class
  );

  WebController(ItemRepository itemRepository) {
    this.itemRepository = itemRepository;
  }

  @GetMapping("/")
  public String dashboard(Model model) {
    try {
      String userId = fetchUserIdFromToken();

      // データをサービスから取得
      try {
        // カテゴリーを全て取得
        List<CategoryDto> categories = categoryService.getAllCategories(userId);
        model.addAttribute("totalCategories", categories.size());

        // カテゴリーごとのアイテム数を計算
        int totalItems = 0;
        List<LowStockAlert> lowStockAlerts = new ArrayList<>();

        for (CategoryDto category : categories) {
          try {
            List<ItemDto> items = itemService.getItems(
              userId,
              category.getName()
            );
            totalItems += items.size();
            // TODO : 低在庫アラートの計算ロジックを追加
          } catch (Exception e) {
            // Continue with other categories if one fails
            System.err.println(
              "Error getting items for category " +
              category.getName() +
              ": " +
              e.getMessage()
            );
          }
        }

        model.addAttribute("totalItems", totalItems);
        model.addAttribute("lowStockItems", lowStockAlerts.size());

        // 最近のアクティビティを取得
        try {
          List<ItemRecordDto> allRecords = itemRecordService.getUserItemRecords(
            userId
          );

          // 最近5件を抽出
          List<DashboardActivity> recentActivities = new ArrayList<>();
          int count = 0;
          for (ItemRecordDto record : allRecords) {
            if (count >= 5) break;

            recentActivities.add(
              new DashboardActivity(
                record.getCategoryName(),
                record.getItemName(),
                record.getTransactionType(),
                record.getQuantity(),
                record.getCreatedAt()
              )
            );
            count++;
          }
          model.addAttribute("recentActivities", recentActivities);
          model.addAttribute("monthlyTransactions", allRecords.size());
        } catch (Exception e) {
          System.err.println("Error getting records: " + e.getMessage());
          model.addAttribute("recentActivities", new ArrayList<>());
          model.addAttribute("monthlyTransactions", 0);
        }

        model.addAttribute("lowStockAlerts", lowStockAlerts);
      } catch (Exception e) {
        System.err.println("Error getting dashboard data: " + e.getMessage());
        return "redirect:/server-error";
      }
    } catch (AuthenticationException e) {
      return "redirect:/login";
    }

    return "dashboard";
  }

  @PostMapping("/items")
  public String createItem(
    RedirectAttributes redirectAttributes,
    @Valid @ModelAttribute ItemRequest itemRequest,
    BindingResult bindingResult
  ) {
    if (bindingResult.hasErrors()) {
      return "items/form";
    }
    try {
      HttpHeaders headers = getHeaders(request);
      headers.set("Content-Type", "application/json");
      HttpEntity<ItemRequest> entity = new HttpEntity<>(itemRequest, headers);
      ResponseEntity<Object> response = restTemplate.postForEntity(
        apiUrl + "item",
        entity,
        Object.class
      );
      String message = (Map<?, ?>) response.getBody() != null
        ? (String) ((Map<?, ?>) response.getBody()).get("message")
        : null;

      if (response.getStatusCode().is2xxSuccessful()) {
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/categories";
      }

      redirectAttributes.addFlashAttribute("message", message);
      return "redirect:/items/new";
    } catch (AuthenticationException e) {
      return "redirect:/login";
    } catch (Exception e) {
      return "redirect:/server-error";
    }
  }

  @GetMapping("/items")
  public String itemList(
    Model model,
    @RequestParam(required = true) String category,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String search,
    @RequestParam(required = false) String stock,
    @RequestParam(defaultValue = "name") String sort,
    @RequestParam(defaultValue = "asc") String order
  ) {
    try {
      HttpHeaders headers = getHeaders(request);
      HttpEntity<?> entity = new HttpEntity<>(headers);
      ResponseEntity<List<ItemDto>> response = restTemplate.exchange(
        apiUrl +
        "item" +
        "?category_name=" +
        category +
        "&page=" +
        page +
        "&size=" +
        size +
        "&sort=" +
        sort,
        HttpMethod.GET,
        entity,
        new ParameterizedTypeReference<List<ItemDto>>() {}
      );
      List<ItemDto> items = response.getBody();

      // Add data to model
      model.addAttribute("items", items);
      // model.addAttribute("categories", categoryService.getAllCategories(userId));
      model.addAttribute("currentPage", page);
      model.addAttribute(
        "totalPages",
        Math.max(1, (items.size() + size - 1) / size)
      );
      model.addAttribute("pageSize", size);
      model.addAttribute("sortField", sort);
      model.addAttribute("sortOrder", order);
      model.addAttribute("selectedCategory", category);

      return "items/list";
    } catch (AuthenticationException e) {
      return "redirect:/login";
    } catch (Exception e) {
      return "redirect:/server-error";
    }
  }

  @GetMapping("/items/new")
  public String newItem(Model model) {
    HttpHeaders headers = getHeaders(request);
    HttpEntity<?> entity = new HttpEntity<>(headers);

    ResponseEntity<List<CategoryDto>> response = restTemplate.exchange(
      apiUrl + "category",
      HttpMethod.GET,
      entity,
      new ParameterizedTypeReference<List<CategoryDto>>() {}
    );
    List<CategoryDto> categories = response.getBody();
    model.addAttribute("itemRequest", new ItemRequest());
    model.addAttribute("categories", categories);
    return "items/form";
  }

  @GetMapping("/items/{id}")
  public String itemDetail(@PathVariable String id, Model model) {
    HttpHeaders headers = getHeaders(request);
    headers.set("Content-Type", "application/json");
    HttpEntity<ItemRequest> entity = new HttpEntity<>(headers);

    ResponseEntity<List<ItemRecordDto>> response = restTemplate.exchange(
      apiUrl + "item/" + id + "/records",
      HttpMethod.GET,
      entity,
      new ParameterizedTypeReference<List<ItemRecordDto>>() {}
    );

    Optional<Item> item = itemRepository.getActiveItemWithId(
      List.of(fetchUserIdFromToken()),
      UUID.fromString(id)
    );

    if (item.isEmpty()) {
      return "redirect:/dashboard";
    }

    model.addAttribute("item", item.get());

    List<ItemRecordDto> recentRecords = response
      .getBody()
      .stream()
      .limit(5)
      .toList();

    if (response.getStatusCode().is2xxSuccessful()) {
      model.addAttribute("recentRecords", recentRecords);
    } else {
      model.addAttribute("recentRecords", new ArrayList<ItemRecordDto>());
    }
    return "items/detail";
  }

  // @GetMapping("/items/{id}/edit")
  // public String editItem(@PathVariable String id, Model model) {
  // model.addAttribute("item", getSampleItem(id));
  // model.addAttribute("itemRequest", new ItemRequest());
  // model.addAttribute("categories", getSampleCategories());
  // return "items/form";
  // }
  @PostMapping("/categories")
  public String createCategory(
    RedirectAttributes redirectAttributes,
    @ModelAttribute @Valid CategoryRequest categoryRequest,
    BindingResult bindingResult
  ) {
    if (bindingResult.hasErrors()) {
      return "categories/form";
    }
    try {
      HttpHeaders headers = getHeaders(request);
      headers.set("Content-Type", "application/json");
      HttpEntity<CategoryRequest> entity = new HttpEntity<>(
        categoryRequest,
        headers
      );

      ResponseEntity<Object> response = restTemplate.postForEntity(
        apiUrl + "category",
        entity,
        Object.class
      );
      Map<?, ?> map = (Map<?, ?>) response.getBody();
      String message = (Map<?, ?>) response.getBody() != null
        ? (String) map.get("message")
        : null;

      if (response.getStatusCode().is2xxSuccessful()) {
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/categories";
      }
      redirectAttributes.addFlashAttribute("message", message);
      return "redirect:/categories/new";
    } catch (AuthenticationException e) {
      return "redirect:/login";
    } catch (Exception e) {
      return "redirect:/server-error";
    }
  }

  @GetMapping("/categories")
  public String categoryList(
    Model model,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "12") int size,
    @RequestParam(required = false) String search,
    @RequestParam(defaultValue = "name") String sort,
    HttpServletRequest request
  ) {
    try {
      HttpHeaders headers = getHeaders(request);
      HttpEntity<?> entity = new HttpEntity<>(headers);

      ResponseEntity<List<CategoryDto>> response = restTemplate.exchange(
        apiUrl +
        "category" +
        "?page=" +
        page +
        "&size=" +
        size +
        "&sort=" +
        sort,
        HttpMethod.GET,
        entity,
        new ParameterizedTypeReference<List<CategoryDto>>() {}
      );
      List<CategoryDto> categories = response.getBody();

      model.addAttribute("categories", categories);
      model.addAttribute("currentPage", page);
      model.addAttribute("totalPages", 3);
      model.addAttribute("pageSize", size);
      model.addAttribute("totalCategories", categories.size());
      int totalItems = categories
        .stream()
        .mapToInt(CategoryDto::getItemCount)
        .sum();
      model.addAttribute("totalItems", totalItems);
      model.addAttribute(
        "averageItemsPerCategory",
        Math.round((totalItems / (double) categories.size()) * 100.0) / 100.0
      );
      model.addAttribute(
        "emptyCategoriesCount",
        categories.stream().filter(c -> c.getItemCount() == 0).count()
      );
      return "categories/list";
    } catch (AuthenticationException e) {
      return "redirect:/login";
    } catch (Exception e) {
      return "redirect:/server-error";
    }
  }

  @PostMapping("/categories/{id}/delete")
  public String deleteCategory(
    RedirectAttributes redirectAttributes,
    @PathVariable UUID id
  ) {
    try {
      String userId = fetchUserIdFromToken();
      categoryService.deleteCategory(id, userId);
      redirectAttributes.addFlashAttribute(
        "message",
        "カテゴリが正常に削除されました。"
      );
      return "redirect:/categories";
    } catch (ResponseStatusException e) {
      redirectAttributes.addFlashAttribute(
        "error",
        "カテゴリーの削除に失敗しました: " + e.getReason()
      );
      return "redirect:/categories";
    } catch (AuthenticationException e) {
      return "redirect:/login";
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/categories";
    }
  }

  @GetMapping("/categories/new")
  public String newCategory(Model model) {
    model.addAttribute("categoryRequest", new CategoryRequest());
    return "categories/form";
  }

  // @GetMapping("/categories/{id}/edit")
  // public String editCategory(@PathVariable String id, Model model) {
  //   model.addAttribute("category", getSampleCategory(id));
  //   model.addAttribute("categoryRequest", new CategoryRequest());
  //   return "categories/form";
  // }

  @GetMapping("/records")
  public String recordList(
    Model model,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String type,
    @RequestParam(required = false) String itemName,
    @RequestParam(required = false) String dateFrom,
    @RequestParam(required = false) String dateTo,
    @RequestParam(defaultValue = "createdAt") String sort,
    @RequestParam(defaultValue = "desc") String order
  ) {
    // Set active page for navigation highlighting
    try {
      HttpHeaders headers = getHeaders(request);
      HttpEntity<?> entity = new HttpEntity<>(headers);

      ResponseEntity<List<ItemRecordDto>> response = restTemplate.exchange(
        apiUrl +
        "item-record/history" +
        "?page=" +
        page +
        "&size=" +
        size +
        "&sort=" +
        sort,
        HttpMethod.GET,
        entity,
        new ParameterizedTypeReference<List<ItemRecordDto>>() {}
      );
      if (!response.getStatusCode().is2xxSuccessful()) {
        return "redirect:/server-error";
      }
      List<ItemRecordDto> records = response.getBody();
      Map<String, Integer> summary = Map.of(
        "totalIn",
        records
          .stream()
          .filter(r -> r.getTransactionType() == TransactionType.IN)
          .toList()
          .size(),
        "totalOut",
        records
          .stream()
          .filter(r -> r.getTransactionType() == TransactionType.OUT)
          .toList()
          .size(),
        "totalRecords",
        records.size()
      );
      model.addAttribute("records", records);
      model.addAttribute("summary", summary);
      model.addAttribute("currentPage", page);
      model.addAttribute("totalPages", 8);
      model.addAttribute("pageSize", size);
      model.addAttribute("sortField", sort);
      model.addAttribute("sortOrder", order);
      return "records/list";
    } catch (AuthenticationException e) {
      return "redirect:/login";
    } catch (Exception e) {
      return "redirect:/server-error";
    }
  }

  @PostMapping("/records")
  public String createInRecord(
    RedirectAttributes redirectAttributes,
    @Valid @ModelAttribute ItemRecordRequest recordRequest,
    BindingResult bindingResult
  ) {
    HttpHeaders headers = getHeaders(request);
    headers.set("Content-Type", "application/json");
    HttpEntity<ItemRecordRequest> entity = new HttpEntity<>(
      recordRequest,
      headers
    );

    ResponseEntity<Object> response = restTemplate.postForEntity(
      apiUrl + "item-record",
      entity,
      Object.class
    );
    Map<?, ?> map = (Map<?, ?>) response.getBody();
    String message = (Map<?, ?>) response.getBody() != null
      ? (String) map.get("message")
      : null;

    redirectAttributes.addFlashAttribute("message", message);
    return "redirect:/items/" + recordRequest.getItemId();
  }

  // @GetMapping("/records/new")
  // public String newRecord(Model model) {
  // model.addAttribute("recordRequest", new RecordRequest());
  // model.addAttribute("categoriesWithItems", getCategoriesWithItems());
  // model.addAttribute("recentRecords", getSampleRecentRecords());
  // return "records/form";
  // }

  @GetMapping("/login")
  public String loginPage(Model model) {
    return "auth/login";
  }

  @GetMapping("/logout")
  public String logoutPage(Model model) {
    HttpHeaders headers = getHeaders(request);
    headers.set("Content-Type", "application/json");
    HttpEntity<ItemRecordRequest> entity = new HttpEntity<>(headers);
    restTemplate.postForEntity(apiUrl + "/auth/signOut", entity, Object.class);
    return "redirect:/login";
  }

  public static class DashboardActivity {

    public String categoryName;
    public String itemName;
    public TransactionType type;
    public int quantity;
    public LocalDateTime createdAt;
    public String note;
    public final String timestamp;

    public DashboardActivity(
      String categoryName,
      String itemName,
      TransactionType type,
      int quantity,
      LocalDateTime createdAt
    ) {
      this.categoryName = categoryName;
      this.itemName = itemName;
      this.type = type;
      this.quantity = quantity;
      this.createdAt = createdAt;
      this.timestamp = TimeUtils.calculateTimeAgo(createdAt);
    }

    // Getters
    public String getItemName() {
      return itemName;
    }

    public TransactionType getType() {
      return type;
    }

    public int getQuantity() {
      return quantity;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public String getNote() {
      return note;
    }

    public String getTimestamp() {
      return timestamp;
    }
  }

  public static class LowStockAlert {

    public String categoryName;
    public String itemName;
    public int currentStock;
    public int threshold; // Default threshold

    public LowStockAlert(
      String categoryName,
      String itemName,
      int currentStock,
      int threshold
    ) {
      this.categoryName = categoryName;
      this.itemName = itemName;
      this.currentStock = currentStock;
      this.threshold = threshold;
    }

    // Getters
    public String getCategoryName() {
      return categoryName;
    }

    public String getItemName() {
      return itemName;
    }

    public int getCurrentStock() {
      return currentStock;
    }

    public int getThreshold() {
      return threshold;
    }
  }

  public HttpHeaders getHeaders(HttpServletRequest request) {
    // Extract cookies from the incoming request
    String cookieHeader = null;
    if (request.getCookies() != null) {
      StringBuilder sb = new StringBuilder();
      for (Cookie cookie : request.getCookies()) {
        if (sb.length() > 0) sb.append("; ");
        sb.append(cookie.getName()).append("=").append(cookie.getValue());
      }
      cookieHeader = sb.toString();
    }
    HttpHeaders headers = new HttpHeaders();
    if (cookieHeader != null && !cookieHeader.isEmpty()) {
      headers.add("Cookie", cookieHeader);
    }
    return headers;
  }
}
