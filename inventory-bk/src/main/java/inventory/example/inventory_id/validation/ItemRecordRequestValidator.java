package inventory.example.inventory_id.validation;

import inventory.example.inventory_id.request.ItemRecordRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ItemRecordRequestValidator implements ConstraintValidator<ValidItemRecordRequest, ItemRecordRequest> {

  @Override
  public boolean isValid(ItemRecordRequest request, ConstraintValidatorContext context) {
    boolean isValid = true;

    // Check if OUT request has itemRecordId
    if (request.getSource() == ItemRecordRequest.Source.OUT && request.getItemRecordId() == null) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("出庫にはレコードIDが必要です。")
          .addPropertyNode("itemRecordId").addConstraintViolation();
      isValid = false;
    }
    return isValid;
  }
}
