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


@Slf4j
@Configuration
@ConditionalOnClass(name = "jakarta.servlet.Filter")
@ConditionalOnProperty(prefix = "ouroboros", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RestMockAutoConfiguration {
    private static final int MOCK_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE;

    /**
     * Register a high-priority OuroborosMockFilter in the servlet filter chain.
     *
     * Creates and configures a FilterRegistrationBean that wraps an OuroborosMockFilter
     * wired with the provided registry, validation service, schema builder, and mappers.
     *
     * @return a FilterRegistrationBean that registers an OuroborosMockFilter ordered at MOCK_FILTER_ORDER
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

    /**
     * Provide an XmlMapper instance as a fallback for MVC environments.
     *
     * @return a new XmlMapper instance
     */
    @Bean
    @ConditionalOnMissingBean(XmlMapper.class)
    public XmlMapper xmlMapper() {
        return new XmlMapper(); // MVC 환경 fallback
    }

}