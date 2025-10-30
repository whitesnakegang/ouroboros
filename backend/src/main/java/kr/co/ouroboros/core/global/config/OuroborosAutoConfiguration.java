package kr.co.ouroboros.core.global.config;


import kr.co.ouroboros.core.global.properties.OuroborosProperties;
import kr.co.ouroboros.core.rest.filter.ApiStateGlobalMethodFilter;
import kr.co.ouroboros.core.rest.config.OpenApiCustomizerConfig;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Auto-configuration for Ouroboros REST API specification library.
 * <p>
 * Automatically registers all necessary beans for the library to function,
 * including services, controllers, and OpenAPI customizers.
 * Uses component scanning for most beans, but excludes the annotation package
 * to avoid scanning internal annotation classes (e.g., {@code ApiState$State}).
 * <p>
 * This configuration can be disabled by setting {@code ouroboros.enabled=false}
 * in application.properties or application.yml.
 *
 * @since 0.0.1
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "ouroboros", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OuroborosProperties.class)
@ComponentScan(
    basePackages = "kr.co.ouroboros",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "kr\\.co\\.ouroboros\\.core\\.global\\.annotation\\..*"
    )
)
public class OuroborosAutoConfiguration {

    /**
         * Create and register a global method filter that applies {@code @ApiState} annotations to API operation metadata.
         * <p>
         * This bean is declared explicitly because the package containing the {@code @ApiState} annotation is excluded from component scanning.
         *
         * @return an {@link ApiStateGlobalMethodFilter} instance that applies {@code @ApiState} annotations globally
         */
    @Bean
    @ConditionalOnMissingBean
    public ApiStateGlobalMethodFilter apiStateGlobalMethodFilter() {
        return new ApiStateGlobalMethodFilter();
    }

    /**
     * Registers the OpenAPI operation customizer for {@code @ApiState} annotations.
     * <p>
     * This bean is registered explicitly because the annotation package is excluded
     * from component scanning.
     *
     * @return the operation customizer that adds API state metadata
     */
//    @Bean
//    @ConditionalOnMissingBean
//    public OperationCustomizer apiOperationCustomizer() {
//        return new OpenApiCustomizerConfig().apiOperationCustomizer();
//    }
}