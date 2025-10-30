package kr.co.ouroboros.core.global.config;

import net.datafaker.Faker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Locale;

@Configuration
public class FakerConfig {
    private static final ThreadLocal<Faker> threadLocalFaker =
            ThreadLocal.withInitial(() -> new Faker(Locale.US));

    /**
     * Provides a Faker instance unique to the current thread.
     *
     * @return a Faker instance scoped to the current thread, initialized with Locale.US
     */
    @Bean
    public Faker faker() {
        // 각 Thread마다 독립된 Faker 인스턴스를 반환
        return threadLocalFaker.get();
    }
}