package lumeva.profileservice.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;

/**
 * База интеграционных тестов. Контейнеры стартуют один раз, контекст Spring кэшируется
 * и переиспользуется всеми наследниками. Поэтому тесты не зависят от чужих данных:
 * каждый работает со своим случайным userId.
 */
@SpringBootTest
@ActiveProfiles("test")
@ImportTestcontainers(ProfileServiceContainers.class)
public abstract class AbstractIntegrationTest {
}
