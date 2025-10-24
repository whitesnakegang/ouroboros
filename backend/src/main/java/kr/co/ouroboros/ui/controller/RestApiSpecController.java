package kr.co.ouroboros.ui.controller;

import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiResponse;
import kr.co.ouroboros.core.rest.spec.service.RestApiSpecService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rest-specs")
@RequiredArgsConstructor
public class RestApiSpecController {

    private final RestApiSpecService restApiSpecService;

    @PostMapping
    public ResponseEntity<CreateRestApiResponse> createRestApiSpec(
            @RequestBody CreateRestApiRequest request) {
        try {
            CreateRestApiResponse response = restApiSpecService.createRestApiSpec(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            CreateRestApiResponse errorResponse = CreateRestApiResponse.builder()
                    .success(false)
                    .message("Failed to create REST API specification: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
