package c102.com.demoapigen.controller;

import c102.com.demoapigen.model.ApiDefinition;
import c102.com.demoapigen.model.Endpoint;
import c102.com.demoapigen.service.ApiDefinitionService;
import c102.com.demoapigen.service.DummyDataGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/demoapigen/api")
@RequiredArgsConstructor
public class EditorApiController {

    private final ApiDefinitionService apiDefinitionService;
    private final DummyDataGenerator dummyDataGenerator;

    @GetMapping("/definition")
    public ResponseEntity<ApiDefinition> getApiDefinition() {
        log.info("Fetching current API definition");
        ApiDefinition definition = apiDefinitionService.loadApiDefinition();
        return ResponseEntity.ok(definition);
    }

    @PostMapping("/definition")
    public ResponseEntity<Map<String, String>> saveApiDefinition(@RequestBody ApiDefinition apiDefinition) {
        log.info("Saving API definition with {} endpoints",
                apiDefinition.getEndpoints() != null ? apiDefinition.getEndpoints().size() : 0);

        try {
            apiDefinitionService.saveApiDefinition(apiDefinition);
            apiDefinitionService.reloadEndpoints();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "API definition saved and endpoints reloaded"
            ));
        } catch (Exception e) {
            log.error("Failed to save API definition", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
                ));
        }
    }

    @PostMapping("/preview")
    public ResponseEntity<Object> previewEndpoint(@RequestBody Endpoint endpoint) {
        log.info("Generating preview for endpoint: {} {}", endpoint.getMethod(), endpoint.getPath());

        try {
            // responses 배열에서 첫 번째 성공 응답(2xx)을 미리보기로 사용
            if (endpoint.getResponses() != null && !endpoint.getResponses().isEmpty()) {
                var successResponse = endpoint.getResponses().stream()
                        .filter(sr -> sr.getStatusCode() != null && sr.getStatusCode() >= 200 && sr.getStatusCode() < 300)
                        .findFirst()
                        .orElse(endpoint.getResponses().get(0)); // 성공 응답 없으면 첫 번째 응답 사용

                if (successResponse.getResponse() != null) {
                    Object previewData = dummyDataGenerator.generateDummyData(successResponse.getResponse());
                    return ResponseEntity.ok(previewData);
                }
            }

            return ResponseEntity.ok(Map.of("message", "No response defined"));
        } catch (Exception e) {
            log.error("Failed to generate preview", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate preview", "message", e.getMessage()));
        }
    }
}
