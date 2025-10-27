package inventory.example.inventory_id.spec;

import inventory.example.inventory_id.model.Category;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class CategorySpecs {

  /**
   * Matches categories whose userId is in the provided list (e.g. user + system default).
   */
  public static Specification<Category> belongsToUser(List<String> userIds) {
    return (root, query, cb) -> root.get("userId").in(userIds);
  }

  public static Specification<Category> isNotDeleted() {
    return (root, query, cb) -> cb.equal(root.get("deletedFlag"), false);
  }
}
