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

    public DemoApiAutoConfiguration() {
        log.info("DemoApiGen Auto-configuration initialized");
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiYamlParser apiYamlParser() {
        return new ApiYamlParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public DummyDataGenerator dummyDataGenerator() {
        return new DummyDataGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicEndpointController dynamicEndpointController(DummyDataGenerator dummyDataGenerator) {
        return new DynamicEndpointController(dummyDataGenerator);
    }
}
