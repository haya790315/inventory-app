package inventory.example.inventory_id.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = ItemRecordRequestValidator.class)
@Target({ TYPE })
@Retention(RUNTIME)
public @interface ValidItemRecordRequest {
  String message() default "アイテムの入出庫リクエストが不正です。";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
