package inventory.example.inventory_id.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class TimeUtils {

  public static String calculateTimeAgo(LocalDateTime compareDate) {
    try {
      LocalDateTime now = LocalDateTime.now();

      long minutes = ChronoUnit.MINUTES.between(compareDate, now);
      long hours = ChronoUnit.HOURS.between(compareDate, now);
      long days = ChronoUnit.DAYS.between(compareDate, now);
      long weeks = ChronoUnit.WEEKS.between(compareDate, now);
      long months = ChronoUnit.MONTHS.between(compareDate, now);
      long years = ChronoUnit.YEARS.between(compareDate, now);

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
}
