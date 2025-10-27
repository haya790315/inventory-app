package inventory.example.inventory_id.config;

import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.repository.CategoryRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

  @Autowired
  private CategoryRepository categoryRepository;

  @Value("${system.userid}")
  private String systemUserId;

  @Override
  public void run(String... args) {
    addDefaultCategories();
  }

  private void addDefaultCategories() {
    List<String> categoryNames = Arrays.asList(
      "食べ物",
      "家電",
      "書籍",
      "衣類",
      "家具"
    );
    for (String name : categoryNames) {
      boolean exists = categoryRepository.existsByUserIdAndName(
        systemUserId,
        name
      );
      if (!exists) {
        Category category = new Category(name);
        category.setUserId(systemUserId);
        categoryRepository.save(category);
      }
    }
  }
}
