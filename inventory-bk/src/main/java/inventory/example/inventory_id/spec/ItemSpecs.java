package inventory.example.inventory_id.spec;

import inventory.example.inventory_id.model.Item;
import org.springframework.data.jpa.domain.Specification;

public class ItemSpecs {

  public static Specification<Item> belongsToUser(String userId) {
    return (root, query, cb) -> cb.equal(root.get("userId"), userId);
  }

  public static Specification<Item> hasCategoryName(String categoryName) {
    return (root, query, cb) ->
      cb.equal(root.get("category").get("name"), categoryName);
  }

  public static Specification<Item> isNotDeleted() {
    return (root, query, cb) -> cb.equal(root.get("deletedFlag"), false);
  }
}
