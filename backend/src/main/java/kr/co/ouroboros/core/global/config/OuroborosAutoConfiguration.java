package kr.co.ouroboros.core.global.config;

import kr.co.ouroboros.core.rest.spec.service.RestApiSpecService;
import kr.co.ouroboros.core.rest.spec.service.RestApiSpecServiceimpl;
import kr.co.ouroboros.ui.controller.RestApiSpecController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = "kr.co.ouroboros")
public class OuroborosAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestApiSpecService restApiSpecService() {
        return new RestApiSpecServiceimpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestApiSpecController restApiSpecController(RestApiSpecService restApiSpecService) {
        return new RestApiSpecController(restApiSpecService);
    }
}