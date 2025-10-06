package inventory.example.inventory_id.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inventory.example.inventory_id.enums.TransactionType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@DisplayName("ItemRecordRequest バリデーションテスト")
public class ItemRecordRequestValidationTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  @DisplayName("有効な入庫リクエスト - バリデーション成功")
  void validInRequest_shouldPassValidation() {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        10, // quantity (int)
        500, // price (int)
        LocalDate.now(),
        TransactionType.IN);

    Set<ConstraintViolation<ItemRecordRequest>> violations = validator
        .validate(request);

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("有効な出庫リクエスト - バリデーション成功")
  void validOutRequest_shouldPassValidation() {
    // Given
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        5,
        TransactionType.OUT,
        UUID.randomUUID());

    Set<ConstraintViolation<ItemRecordRequest>> violations = validator
        .validate(request);

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("バリデーション失敗- アイテムIDがnull")
  void nullItemId_shouldFailValidation() {
    ItemRecordRequest request = new ItemRecordRequest(
        null,
        10, // quantity (int)
        500, // price (int)
        LocalDate.now(),
        TransactionType.IN);

    Set<ConstraintViolation<ItemRecordRequest>> violations = validator
        .validate(request);

    assertThat(violations).isNotEmpty();
    ConstraintViolation<ItemRecordRequest> violation = violations.iterator()
        .next();
    assertThat(violation.getMessage()).isEqualTo("アイテムIDは必須です。");
  }

  @Test
  @DisplayName("バリデーション失敗- 数量が0（未設定時のデフォルト値）")
  void defaultQuantity_shouldFailValidation() {
    // When quantity is not provided in JSON, it defaults to 0
    // This tests the case where quantity field is missing from request
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        0, // quantity defaults to 0 when missing
        100, // price (int)
        LocalDate.now(),
        TransactionType.IN);

    Set<ConstraintViolation<ItemRecordRequest>> violations = validator
        .validate(request);
    assertThat(violations).isNotEmpty();
    ConstraintViolation<ItemRecordRequest> violation = violations
        .iterator().next();
    assertThat(violation.getMessage()).isEqualTo("数量は1以上である必要があります。");
  }

  @Test
  @DisplayName("バリデーション失敗- 数量が0")
  void zeroQuantity_shouldFailValidation() {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        0, // quantity = 0 (invalid)
        100, // price (int)
        LocalDate.now(),
        TransactionType.IN);

    Set<ConstraintViolation<ItemRecordRequest>> violations = validator
        .validate(request);
    assertThat(violations).isNotEmpty();
    ConstraintViolation<ItemRecordRequest> violation = violations
        .iterator().next();
    assertThat(violation.getMessage()).isEqualTo("数量は1以上である必要があります。");
  }

  @Test
  @DisplayName("バリデーション失敗- 数量が負の値")
  void negativeQuantity_shouldFailValidation() {
    // Given
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        -5, // quantity negative (invalid)
        100, // price (int)
        LocalDate.now(),
        TransactionType.IN);

    Set<ConstraintViolation<ItemRecordRequest>> violations = validator
        .validate(request);
    assertThat(violations).isNotEmpty();
    ConstraintViolation<ItemRecordRequest> violation = violations
        .iterator().next();
    assertThat(violation.getMessage()).isEqualTo("数量は1以上である必要があります。");
  }

  @Test
  @DisplayName("バリデーション失敗- 入出庫区分がnull")
  void nullSource_shouldFailValidation() {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        10, // quantity (int)
        100, // price (int)
        LocalDate.now(),
        null);
    Set<ConstraintViolation<ItemRecordRequest>> violations = validator
        .validate(request);
    assertThat(violations).isNotEmpty();
    ConstraintViolation<ItemRecordRequest> violation = violations
        .iterator().next();
    assertThat(violation.getMessage()).isEqualTo("入出庫種別は必須です。");
  }

  @Test
  @DisplayName("有効なリクエスト - 単価が0（無料アイテム）")
  void zeroPriceItem_shouldBeValid() {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        10, // quantity (int)
        0, // price = 0 (free item, valid)
        LocalDate.now(),
        TransactionType.IN);

    Set<ConstraintViolation<ItemRecordRequest>> violations = validator
        .validate(request);
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("バリデーション失敗 - 単価が負の値")
  void negativePrice_shouldFailValidation() {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        10, // quantity (int)
        -100, // price negative (invalid)
        LocalDate.now(),
        TransactionType.IN);

    Set<ConstraintViolation<ItemRecordRequest>> violations = validator
        .validate(request);
    assertThat(violations).isNotEmpty();
    ConstraintViolation<ItemRecordRequest> violation = violations
        .iterator().next();
    assertThat(violation.getMessage()).isEqualTo("価格は0以上である必要があります。");
  }

  @Test
  @DisplayName("有効なリクエスト - 有効期限がnull")
  void nullExpirationDate_shouldBeValid() {
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        10, // quantity (int)
        100, // price (int)
        null, // 有効期限はオプション
        TransactionType.IN);
    Set<ConstraintViolation<ItemRecordRequest>> violations = validator
        .validate(request);

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("バリデーション失敗 - itemRecordIdが設定しない（出庫時必須）")
  void nullItemRecordIdForOut_shouldFailValidation() {
    // Given
    ItemRecordRequest request = new ItemRecordRequest(
        UUID.randomUUID(),
        10,
        TransactionType.OUT,
        null);

    Set<ConstraintViolation<ItemRecordRequest>> violations = validator
        .validate(request);
    assertThat(violations).isNotEmpty();
    ConstraintViolation<ItemRecordRequest> violation = violations
        .iterator().next();
    assertThat(violation.getMessage()).isEqualTo("出庫にはレコードIDが必要です。");
  }
}
