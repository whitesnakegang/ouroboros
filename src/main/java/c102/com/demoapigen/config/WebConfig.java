package c102.com.demoapigen.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Registers a resource handler that serves static resources under the `/demoapigen/**` URL path.
     *
     * Maps requests matching `/demoapigen/**` to resources located at `classpath:/static/demoapigen/`.
     *
     * @param registry the registry used to add resource handlers and locations
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/demoapigen/**")
                .addResourceLocations("classpath:/static/demoapigen/");
    }
}