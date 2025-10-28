package kr.co.ouroboros.core.global.config;

import net.datafaker.Faker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Locale;

@Configuration
public class FakerConfig {
    @Bean
    public Faker faker() {
        return new Faker(Locale.KOREA);
    }
}
