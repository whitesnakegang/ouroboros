package kr.co.ouroboros.core.rest.mock.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import kr.co.ouroboros.core.global.mock.service.SchemaMockBuilder;
import kr.co.ouroboros.core.rest.mock.filter.*;
import kr.co.ouroboros.core.rest.mock.registry.RestMockRegistry;
import kr.co.ouroboros.core.rest.mock.service.MockValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.Ordered;

/**
 * Spring Boot auto-configuration for REST mock server functionality.
 * <p>
 * Automatically registers {@link OuroborosMockFilter} at {@link Ordered#HIGHEST_PRECEDENCE}
 * to intercept mock endpoints before Spring Security and other filters.
 * <p>
 * Activation conditions:
 * <ul>
 *   <li>Jakarta Servlet Filter is on classpath</li>
 *   <li>{@code ouroboros.enabled=true} (default: true)</li>
 * </ul>
 * <p>
 * Can be disabled with:
 * <pre>
 * ouroboros.enabled=false
 * </pre>
 *
 * @since 0.0.1
 * @see OuroborosMockFilter
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "jakarta.servlet.Filter")
@ConditionalOnProperty(prefix = "ouroboros", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RestMockAutoConfiguration {
    private static final int MOCK_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE;

    /**
     * Registers the unified mock filter at highest precedence.
     * <p>
     * Filter order: {@link Ordered#HIGHEST_PRECEDENCE} (-2147483648)<br>
     * Runs before: Spring Security (-100), all user filters
     * <p>
     * This ensures mock endpoints are intercepted before authentication/authorization checks,
     * preventing 401/404 errors for mock endpoints without real Controller implementations.
     *
     * @param registry the mock endpoint registry
     * @param validationService the validation service for request validation
     * @param schemaMockBuilder the builder for generating mock responses
     * @param objectMapper the JSON object mapper
     * @param xmlMapper the XML object mapper
     * @return configured filter registration bean
     */
    @Bean
    public FilterRegistrationBean<OuroborosMockFilter> ouroborosMockFilter(
            RestMockRegistry registry,
            MockValidationService validationService,
            SchemaMockBuilder schemaMockBuilder,
            ObjectMapper objectMapper,
            XmlMapper xmlMapper) {

        FilterRegistrationBean<OuroborosMockFilter> reg = new FilterRegistrationBean<>();

        OuroborosMockFilter filter = new OuroborosMockFilter(
                registry,
                validationService,
                schemaMockBuilder,
                objectMapper,
                xmlMapper
        );

        reg.setFilter(filter);
        reg.setOrder(MOCK_FILTER_ORDER);

        return reg;
    }

    @Bean
    @ConditionalOnMissingBean(XmlMapper.class)
    public XmlMapper xmlMapper() {
        return new XmlMapper(); // MVC 환경 fallback
    }

}
