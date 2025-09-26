package inventory.example.inventory_id.validation;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class CustomLocalDateDeserializer extends JsonDeserializer<LocalDate> {

  private static final DateTimeFormatter EXPECTED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @Override
  public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    String dateString = parser.getText();

    if (dateString == null) {
      return null;
    }
    if (dateString.isEmpty()) {
      throw new IllegalArgumentException(
          "有効期限の形式が不正です。yyyy-MM-dd形式で入力してください。");
    }

    try {
      return LocalDate.parse(dateString, EXPECTED_FORMAT);
    } catch (DateTimeParseException e) {
      // Throw a more specific error with field information
      throw new IllegalArgumentException(
          "有効期限の形式が不正です。yyyy-MM-dd形式で入力してください。");
    }
  }
}
