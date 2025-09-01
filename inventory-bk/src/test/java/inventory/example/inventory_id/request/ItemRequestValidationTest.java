package inventory.example.inventory_id.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRequestValidationTest {

  private final Validator validator;

  public ItemRequestValidationTest() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("アイテムリクエストのバリデーション成功")
  void testValidItemRequest() {
    ItemRequest request = new ItemRequest("validName", "category", 1);
    Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  @DisplayName("アイテムリクエストのバリデーション失敗 - 名前が空")
  void testBlankName() {
    ItemRequest request = new ItemRequest("", "category", 1);
    Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  @DisplayName("アイテムリクエストのバリデーション失敗 - 数量が負")
  void testNegativeQuantity() {
    ItemRequest request = new ItemRequest("", "category", -1);
    Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  @DisplayName("アイテムリクエストのバリデーション失敗 - カテゴリー名が空")
  void testNullCategoryName() {
    ItemRequest request = new ItemRequest("ValidName", null, 1);
    Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  @DisplayName("アイテムリクエストのバリデーション失敗 - 名前が50文字を超える")
  void testNameTooLong() {
    ItemRequest request = new ItemRequest("A".repeat(51), "category", 1);
    Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }
}
