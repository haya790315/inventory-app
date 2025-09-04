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

class CategoryRequestValidationTest {

  private final Validator validator;

  public CategoryRequestValidationTest() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("カテゴリリクエストのバリデーション成功")
  void testValidCategoryRequest() {
    CategoryRequest request = new CategoryRequest();
    request.setName("ValidCategory");

    Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  @DisplayName("カテゴリリクエストのバリデーション失敗 - 名前が空")
  void testBlankName() {
    CategoryRequest request = new CategoryRequest();
    request.setName("");

    Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  @DisplayName("カテゴリリクエストのバリデーション失敗 - 名前がnull")
  void testNullName() {
    CategoryRequest request = new CategoryRequest();
    request.setName(null);

    Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  @DisplayName("カテゴリリクエストのバリデーション失敗 - 名前が50文字を超える")
  void testNameTooLong() {
    CategoryRequest request = new CategoryRequest();
    request.setName("A".repeat(51)); // 51文字

    Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  @DisplayName("カテゴリリクエストのバリデーション成功 - 名前が50文字")
  void testNameExactly50Characters() {
    CategoryRequest request = new CategoryRequest();
    request.setName("A".repeat(50)); // 50文字

    Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }
}
