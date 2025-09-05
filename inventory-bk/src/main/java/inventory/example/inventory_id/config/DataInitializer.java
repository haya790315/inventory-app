package inventory.example.inventory_id.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import inventory.example.inventory_id.model.Category;
import inventory.example.inventory_id.repository.CategoryRepository;

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
    List<String> categoryNames = Arrays.asList("本", "食べ物", "服");
    for (String name : categoryNames) {
      boolean exists = categoryRepository.existsByUserIdAndName(systemUserId, name);
      if (!exists) {
        Category category = new Category(name, systemUserId);
        categoryRepository.save(category);
      }
    }
  }
}
