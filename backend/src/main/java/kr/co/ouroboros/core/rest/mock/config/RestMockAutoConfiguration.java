package kr.co.ouroboros.core.rest.mock.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import kr.co.ouroboros.core.global.mock.service.SchemaMockBuilder;
import kr.co.ouroboros.core.rest.mock.filter.*;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import kr.co.ouroboros.core.rest.mock.registry.RestMockRegistry;
import kr.co.ouroboros.core.rest.mock.service.RestMockLoaderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.util.Map;


@Configuration
@Slf4j
@ConditionalOnClass(name = "jakarta.servlet.Filter")
@ConditionalOnProperty(prefix = "ouroboros", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RestMockAutoConfiguration {
    @Bean
    public FilterRegistrationBean<MockRoutingFilter> routing(RestMockRegistry registry) {
        FilterRegistrationBean<MockRoutingFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new MockRoutingFilter(registry));
        reg.setOrder(1);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<MockValidationFilter> validation() {
        FilterRegistrationBean<MockValidationFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new MockValidationFilter());
        reg.setOrder(2);
        return reg;
    }

    @Bean
    @ConditionalOnMissingBean(MockResponseFilter.class)
    public FilterRegistrationBean<MockResponseFilter> response(
            SchemaMockBuilder schemaMockBuilder,
            ObjectMapper objectMapper,
            XmlMapper xmlMapper
    ) {
        FilterRegistrationBean<MockResponseFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new MockResponseFilter(schemaMockBuilder, objectMapper, xmlMapper));
        reg.setOrder(3);
        return reg;
    }

    @Bean
    @ConditionalOnMissingBean(XmlMapper.class)
    public XmlMapper xmlMapper() {
        return new XmlMapper(); // MVC 환경 fallback
    }

    @Bean
    public CommandLineRunner loadMockRegistry(RestMockLoaderService loaderService, RestMockRegistry registry) {
        return args -> {
            try {
                log.info("Loading mock endpoints from YAML...");
                Map<String, EndpointMeta> endpoints = loaderService.loadFromYaml();
                endpoints.values().forEach(registry::register);
                log.info("Successfully loaded {} mock endpoints into registry", endpoints.size());
            } catch (Exception e) {
                log.error("Failed to load mock registry", e);
            }
        };
    }
}
