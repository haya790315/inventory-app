package inventory.example.inventory_id.controller;

import inventory.example.inventory_id.dto.CategoryDto;
import inventory.example.inventory_id.dto.ItemDto;
import inventory.example.inventory_id.dto.ItemRecordDto;
import inventory.example.inventory_id.enums.TransactionType;
import inventory.example.inventory_id.exception.AuthenticationException;
import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.model.Item;
import inventory.example.inventory_id.repository.CategoryRepository;
import inventory.example.inventory_id.repository.ItemRepository;
import inventory.example.inventory_id.request.CategoryRequest;
import inventory.example.inventory_id.request.ItemRecordRequest;
import inventory.example.inventory_id.request.ItemRequest;
import inventory.example.inventory_id.response.PageResponse;
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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Web controller for serving Thymeleaf templates
 * This controller handles the web UI for the inventory management system
 */
@Controller
public class WebController extends BaseController {

  private final ItemRepository itemRepository;
  private final CategoryRepository categoryRepository;

  @Autowired
  private ItemService itemService;

  @Autowired
  private CategoryService categoryService;

  @Autowired
  private ItemRecordService itemRecordService;

  private String apiUrl = "http://localhost:8080/api/";

  @Value("${system.userid}")
  private String systemUserId;

  @Autowired
  private RestTemplate restTemplate;

  private static final Logger log = LoggerFactory.getLogger(
    WebController.class
  );

  WebController(
    ItemRepository itemRepository,
    CategoryRepository categoryRepository
  ) {
    this.itemRepository = itemRepository;
    this.categoryRepository = categoryRepository;
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

        Page<ItemDto> items = itemService.getItems(
          Pageable.unpaged(),
          userId,
          null
        );
        int totalItems = items.getContent().size();

        List<StockAlert> lowStockAlerts = new ArrayList<>(
          List.of(
            new StockAlert("衣類", "パジャマ", "在庫が閾値を下回っています"),
            new StockAlert("食べ物", "牛肉", "有効期限が近づいています")
          )
        );
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

  @GetMapping("/items")
  public String itemList(
    Model model,
    @RequestParam(required = false) String category,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(required = false) String search,
    @RequestParam(required = false) String stock,
    @RequestParam(defaultValue = "updatedAt") String sort,
    @RequestParam(defaultValue = "desc") String order
  ) {
    try {
      HttpHeaders headers = getHeaders(request);
      HttpEntity<?> entity = new HttpEntity<>(headers);

      UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiUrl)
        .path("item")
        .queryParam("page", page)
        .queryParam("size", size)
        .queryParam("sort", sort + "," + order);

      if (category != null && !category.isBlank()) {
        builder.queryParam("category_name", category);
      }

      String url = builder.toUriString();

      ResponseEntity<PageResponse<ItemDto>> response = restTemplate.exchange(
        url,
        HttpMethod.GET,
        entity,
        new ParameterizedTypeReference<PageResponse<ItemDto>>() {}
      );
      var items = response.getBody().getContent();

      // Add data to model
      model.addAttribute("items", items);
      model.addAttribute("currentPage", page);
      model.addAttribute("totalPages", response.getBody().getTotalPages());
      model.addAttribute("pageSize", size);
      model.addAttribute("sortField", sort);
      model.addAttribute("sortOrder", order);

      ResponseEntity<PageResponse<CategoryDto>> categoryResponse =
        restTemplate.exchange(
          apiUrl + "category",
          HttpMethod.GET,
          entity,
          new ParameterizedTypeReference<PageResponse<CategoryDto>>() {}
        );
      List<CategoryDto> categories = categoryResponse.getBody().getContent();

      model.addAttribute("categories", categories);
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

    ResponseEntity<PageResponse<CategoryDto>> response = restTemplate.exchange(
      apiUrl + "category",
      HttpMethod.GET,
      entity,
      new ParameterizedTypeReference<PageResponse<CategoryDto>>() {}
    );

    List<CategoryDto> categories = response.getBody().getContent();
    model.addAttribute("itemRequest", new ItemRequest());
    model.addAttribute("categories", categories);
    return "items/form";
  }

  @GetMapping("/items/{id}")
  public String itemDetail(@PathVariable String id, Model model) {
    try {
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
    } catch (AuthenticationException e) {
      return "redirect:/login";
    } catch (Exception e) {
      return "redirect:/server-error";
    }
  }

  @GetMapping("/items/{id}/edit")
  public String editItem(@PathVariable String id, Model model) {
    try {
      String userId = fetchUserIdFromToken();
      Item item = itemRepository
        .getActiveItemWithId(List.of(userId), UUID.fromString(id))
        .orElseThrow(() ->
          new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "アイテムが見つかりません"
          )
        );

      model.addAttribute("item", item);
      model.addAttribute(
        "itemRequest",
        new ItemRequest(item.getName(), item.getCategory().getName())
      );
      model.addAttribute(
        "categories",
        categoryService.getAllCategories(userId)
      );
      return "items/form";
    } catch (AuthenticationException e) {
      return "redirect:/login";
    } catch (Exception e) {
      return "redirect:/server-error";
    }
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

  @PutMapping("/items/{id}")
  public String updateItem(
    @PathVariable UUID id,
    @Valid @ModelAttribute ItemRequest itemRequest,
    BindingResult bindingResult,
    RedirectAttributes redirectAttributes
  ) {
    if (bindingResult.hasErrors()) {
      return "items/form";
    }
    try {
      HttpHeaders headers = getHeaders(request);
      headers.set("Content-Type", "application/json");
      HttpEntity<ItemRequest> entity = new HttpEntity<>(itemRequest, headers);

      ResponseEntity<Object> response = restTemplate.exchange(
        apiUrl + "item?item_id=" + id,
        HttpMethod.PUT,
        entity,
        Object.class
      );
      Map<?, ?> map = (Map<?, ?>) response.getBody();
      String message = (Map<?, ?>) response.getBody() != null
        ? (String) map.get("message")
        : null;

      if (response.getStatusCode().is2xxSuccessful()) {
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/items";
      }
      redirectAttributes.addFlashAttribute("message", message);
      return "redirect:/items/" + id + "/edit";
    } catch (AuthenticationException e) {
      return "redirect:/login";
    } catch (Exception e) {
      return "redirect:/server-error";
    }
  }

  @GetMapping("/items/{id}/delete")
  public String deleteItem(
    RedirectAttributes redirectAttributes,
    @PathVariable UUID id
  ) {
    try {
      String userId = fetchUserIdFromToken();
      itemService.deleteItem(userId, id);
      redirectAttributes.addFlashAttribute(
        "message",
        "アイテムの削除が完了しました"
      );
      return "redirect:/items";
    } catch (ResponseStatusException e) {
      redirectAttributes.addFlashAttribute(
        "error",
        "アイテムの削除に失敗しました: " + e.getReason()
      );
      return "redirect:/items";
    } catch (AuthenticationException e) {
      return "redirect:/login";
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/items";
    }
  }

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

  @PutMapping("/categories/{id}")
  public String updateCategory(
    @PathVariable UUID id,
    @ModelAttribute @Valid CategoryRequest categoryRequest,
    BindingResult bindingResult,
    RedirectAttributes redirectAttributes
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

      ResponseEntity<Object> response = restTemplate.exchange(
        apiUrl + "category?category_id=" + id,
        HttpMethod.PUT,
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
      return "redirect:/categories/" + id + "/edit";
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
    @RequestParam(defaultValue = "name,asc") String sort,
    HttpServletRequest request
  ) {
    try {
      HttpHeaders headers = getHeaders(request);
      HttpEntity<?> entity = new HttpEntity<>(headers);
      UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiUrl)
        .path("category")
        .queryParam("page", page)
        .queryParam("size", size)
        .queryParam("sort", sort);
      String url = builder.toUriString();

      ResponseEntity<PageResponse<CategoryDto>> response =
        restTemplate.exchange(
          url,
          HttpMethod.GET,
          entity,
          new ParameterizedTypeReference<PageResponse<CategoryDto>>() {}
        );
      PageResponse<CategoryDto> res = response.getBody();
      List<CategoryDto> categories = res.getContent();

      model.addAttribute("categories", categories);
      model.addAttribute("currentPage", page);
      model.addAttribute("totalPages", res.getTotalPages());
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
        "カスタムカテゴリの削除が完了しました"
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
    try {
      fetchUserIdFromToken();
      model.addAttribute("categoryRequest", new CategoryRequest());
      return "categories/form";
    } catch (AuthenticationException e) {
      return "redirect:/login";
    } catch (Exception e) {
      return "redirect:/server-error";
    }
  }

  @GetMapping("/categories/{id}/edit")
  public String editCategory(@PathVariable String id, Model model) {
    try {
      String userId = fetchUserIdFromToken();
      Optional<Category> categoryOpt = categoryRepository.findUserCategory(
        List.of(userId, systemUserId),
        UUID.fromString(id)
      );
      if (categoryOpt.isPresent()) {
        Category category = categoryOpt.get();
        model.addAttribute("category", category);
        CategoryRequest categoryRequest = new CategoryRequest();
        categoryRequest.setName(category.getName());
        model.addAttribute("categoryRequest", categoryRequest);
        return "categories/form";
      } else {
        return "redirect:/categories";
      }
    } catch (AuthenticationException e) {
      return "redirect:/login";
    } catch (Exception e) {
      return "redirect:/server-error";
    }
  }

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

  @GetMapping("/records/new")
  public String newRecord(Model model) {
    model.addAttribute("recordRequest", new ItemRecordRequest());
    return "records/form";
  }

  @GetMapping("/records/delete/{id}")
  public String deleteRecord(
    RedirectAttributes redirectAttributes,
    @PathVariable Long id
  ) {
    try {
      String userId = fetchUserIdFromToken();
      List<Long> deletedIds = itemRecordService.deleteItemRecord(id, userId);
      String idsString = deletedIds
        .stream()
        .map(String::valueOf)
        .collect(Collectors.joining(", "));
      String message =
        "カテゴリが正常に削除されました。\n削除されたID: " + idsString;
      redirectAttributes.addFlashAttribute("message", message);
      return "redirect:/records";
    } catch (ResponseStatusException e) {
      redirectAttributes.addFlashAttribute(
        "error",
        "レコードの削除に失敗しました: " + e.getReason()
      );
      return "redirect:/records";
    } catch (AuthenticationException e) {
      return "redirect:/login";
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/records";
    }
  }

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

  public static class StockAlert {

    public String categoryName;
    public String itemName;
    public String warning;

    public StockAlert(String categoryName, String itemName, String warning) {
      this.categoryName = categoryName;
      this.itemName = itemName;
      this.warning = warning;
    }

    // Getters
    public String getCategoryName() {
      return categoryName;
    }

    public String getItemName() {
      return itemName;
    }

    public String getwarning() {
      return warning;
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
