package c102.com.demoapigen.config;

import c102.com.demoapigen.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@AutoConfiguration
@ConditionalOnClass(WebMvcConfigurer.class)
@ConditionalOnProperty(prefix = "demoapigen", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "c102.com.demoapigen")
public class DemoApiAutoConfiguration {

    /**
     * Initializes the auto-configuration and records an initialization message to the application log.
     */
    public DemoApiAutoConfiguration() {
        log.info("DemoApiGen Auto-configuration initialized");
    }

    /**
     * Provides an ApiYamlParser for parsing API YAML definitions.
     *
     * @return an ApiYamlParser instance for parsing API YAML definitions
     */
    @Bean
    @ConditionalOnMissingBean
    public ApiYamlParser apiYamlParser() {
        return new ApiYamlParser();
    }

    /**
     * Create a default DummyDataGenerator bean when no other bean of the same type is present.
     *
     * @return the created DummyDataGenerator instance
     */
    @Bean
    @ConditionalOnMissingBean
    public DummyDataGenerator dummyDataGenerator() {
        return new DummyDataGenerator();
    }

    /**
     * Create and expose a DynamicEndpointController configured with the provided DummyDataGenerator.
     *
     * @param dummyDataGenerator the generator used by the controller to produce dummy responses
     * @return a DynamicEndpointController instance configured with the provided generator
     */
    @Bean
    @ConditionalOnMissingBean
    public DynamicEndpointController dynamicEndpointController(DummyDataGenerator dummyDataGenerator) {
        return new DynamicEndpointController(dummyDataGenerator);
    }
}