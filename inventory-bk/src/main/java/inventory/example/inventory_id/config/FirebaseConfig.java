package inventory.example.inventory_id.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class FirebaseConfig {
  private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

  @PostConstruct
  public void init() throws IOException {
    if (FirebaseApp.getApps().isEmpty()) {
      try {
        FileInputStream serviceAccount = new FileInputStream("./src/main/resources/firebase-service-account.json");
        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build();

        FirebaseApp.initializeApp(options);
      } catch (Exception e) {
        logger.error("Firebase initialization error: {}", e.getMessage(), e);
      }
    }
  }
}
