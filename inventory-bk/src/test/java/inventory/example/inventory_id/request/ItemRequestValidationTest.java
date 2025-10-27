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
    ItemRequest request = new ItemRequest("validName", "category");
    Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  @DisplayName("アイテムリクエストのバリデーション失敗 - 名前が空")
  void testBlankName() {
    ItemRequest request = new ItemRequest("", "category");
    Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  @DisplayName("アイテムリクエストのバリデーション失敗 - 名前が全角の空白")
  void testFullWidthBlankName() {
    ItemRequest request = new ItemRequest("　　　　", "category");
    Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }


  @Test
  @DisplayName("アイテムリクエストのバリデーション失敗 - カテゴリー名が空")
  void testBlankCategoryName() {
    ItemRequest request = new ItemRequest("ValidName", "");
    Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  @DisplayName("アイテムリクエストのバリデーション失敗 - カテゴリー名が全角の空白")
  void testFullWidthBlankCategoryName() {
    ItemRequest request = new ItemRequest("ValidName", "　　　　");
    Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  @DisplayName("アイテムリクエストのバリデーション失敗 - 名前が50文字を超える")
  void testNameTooLong() {
    ItemRequest request = new ItemRequest("A".repeat(51), "category");
    Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  @DisplayName("アイテムリクエストのバリデーション成功 - 名前が50文字")
  void testNameExactly50Characters() {
    ItemRequest request = new ItemRequest("A".repeat(50), "category");
    Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }
}
