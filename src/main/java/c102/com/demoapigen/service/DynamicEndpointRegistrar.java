package c102.com.demoapigen.service;

import c102.com.demoapigen.model.ApiDefinition;
import c102.com.demoapigen.model.Endpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicEndpointRegistrar {

    private final RequestMappingHandlerMapping handlerMapping;
    private final ApiYamlParser apiYamlParser;
    private final DummyDataGenerator dummyDataGenerator;
    private final DynamicEndpointController dynamicEndpointController;

    /**
     * Registers dynamic API endpoints defined in classpath:api.yml during application startup.
     *
     * <p>Parses the API YAML, registers each discovered endpoint with the application's handler mapping,
     * and logs the result. If no endpoints are present, logs an informational message and returns.
     */
    @PostConstruct
    public void registerEndpoints() {
        try {
            ApiDefinition apiDefinition = apiYamlParser.parseApiYaml("classpath:api.yml");

            if (apiDefinition == null || apiDefinition.getEndpoints() == null || apiDefinition.getEndpoints().isEmpty()) {
                log.info("No endpoints to register. You can add endpoints via the web editor at /demoapigen/editor");
                return;
            }

            for (Endpoint endpoint : apiDefinition.getEndpoints()) {
                registerEndpoint(endpoint);
            }

            log.info("Successfully registered {} dynamic endpoints", apiDefinition.getEndpoints().size());
        } catch (Exception e) {
            log.error("Failed to register dynamic endpoints", e);
        }
    }

    /**
     * Registers a single dynamic HTTP endpoint described by the provided Endpoint object with Spring's
     * RequestMappingHandlerMapping and stores its metadata in the DynamicEndpointController.
     *
     * <p>The mapping is created for the endpoint's path and HTTP method and bound to
     * DynamicEndpointController.handleRequest(HttpServletRequest). The endpoint metadata is stored
     * under the key `path:METHOD` (method uppercased). Failures during registration are caught and
     * logged.
     *
     * @param endpoint the endpoint definition containing the path and HTTP method to register
     */
    private void registerEndpoint(Endpoint endpoint) {
        try {
            RequestMethod requestMethod = RequestMethod.valueOf(endpoint.getMethod().toUpperCase());

            RequestMappingInfo mappingInfo = RequestMappingInfo
                    .paths(endpoint.getPath())
                    .methods(requestMethod)
                    .build();

            Method controllerMethod = DynamicEndpointController.class.getMethod("handleRequest", jakarta.servlet.http.HttpServletRequest.class);

            handlerMapping.registerMapping(
                    mappingInfo,
                    dynamicEndpointController,
                    controllerMethod
            );

            // Store endpoint metadata for the controller to use (key: path:method)
            String key = endpoint.getPath() + ":" + endpoint.getMethod().toUpperCase();
            dynamicEndpointController.registerEndpoint(key, endpoint);

            log.info("Registered endpoint: {} {}", endpoint.getMethod(), endpoint.getPath());
        } catch (Exception e) {
            log.error("Failed to register endpoint: {} {}", endpoint.getMethod(), endpoint.getPath(), e);
        }
    }
}