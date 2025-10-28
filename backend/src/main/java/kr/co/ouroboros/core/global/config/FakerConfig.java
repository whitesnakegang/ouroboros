package kr.co.ouroboros.core.global.config;

import net.datafaker.Faker;
import org.springframework.context.annotation.Configuration;
import java.util.Locale;

@Configuration
public class FakerConfig {
    private static final ThreadLocal<Faker> threadLocalFaker =
            ThreadLocal.withInitial(() -> new Faker(Locale.KOREA));

    private Faker faker() {
        return threadLocalFaker.get();
    }
}
