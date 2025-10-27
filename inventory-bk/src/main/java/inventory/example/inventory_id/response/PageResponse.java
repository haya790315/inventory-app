package inventory.example.inventory_id.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class PageResponse<T> {

  private List<T> content = new ArrayList<>();
  private int number;
  private int size;
  private long totalElements;
  private int totalPages;

  @JsonCreator
  public PageResponse(
    @JsonProperty("content") List<T> content,
    @JsonProperty("number") int number,
    @JsonProperty("size") int size,
    @JsonProperty("totalElements") long totalElements,
    @JsonProperty("totalPages") int totalPages
  ) {
    this.content = content == null ? new ArrayList<>() : content;
    this.number = number;
    this.size = size;
    this.totalElements = totalElements;
    this.totalPages = totalPages;
  }

  // Convenience constructor used by controllers when creating response from Page
  public PageResponse(
    List<T> content,
    int number,
    int size,
    long totalElements
  ) {
    this(
      content,
      number,
      size,
      totalElements,
      (int) ((size == 0) ? 0 : ((totalElements + size - 1) / size))
    );
  }
}
