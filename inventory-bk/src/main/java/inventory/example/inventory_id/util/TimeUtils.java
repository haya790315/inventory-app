package inventory.example.inventory_id.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TimeUtils {

  private static final DateTimeFormatter formatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public static String calculateTimeAgo(String compareDate) {
    try {
      LocalDateTime createdAt = parseDateTime(compareDate);
      LocalDateTime now = LocalDateTime.now();

      long minutes = ChronoUnit.MINUTES.between(createdAt, now);
      long hours = ChronoUnit.HOURS.between(createdAt, now);
      long days = ChronoUnit.DAYS.between(createdAt, now);
      long weeks = ChronoUnit.WEEKS.between(createdAt, now);
      long months = ChronoUnit.MONTHS.between(createdAt, now);
      long years = ChronoUnit.YEARS.between(createdAt, now);

      if (years > 0) {
        return years + "年前";
      } else if (months > 0) {
        return months + "ヶ月前";
      } else if (weeks > 0) {
        return weeks + "週間前";
      } else if (days > 0) {
        return days + "日前";
      } else if (hours > 0) {
        return hours + "時間前";
      } else if (minutes > 0) {
        return minutes + "分前";
      } else {
        return "たった今";
      }
    } catch (Exception e) {
      System.err.println(
        "Failed to parse date: " + compareDate + " - " + e.getMessage()
      );
      return "不明";
    }
  }

  private static LocalDateTime parseDateTime(String dateStr) throws Exception {
    try {
      LocalDateTime result = LocalDateTime.parse(dateStr, formatter);
      System.out.println("Successfully parsed with formatter: " + formatter);
      return result;
    } catch (Exception e) {
      System.out.println(
        "Failed with formatter " + formatter + ": " + e.getMessage()
      );
      throw new Exception("All parsing attempts failed for date: " + dateStr);
    }
  }
}
