package kr.co.ouroboros.core.rest.spec.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for retrieving all REST API specifications.
 * <p>
 * Used as the response for the GET /ouro/rest-specs endpoint,
 * containing server information and summary data for all API specifications.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetRestApiSpecsResponse {

    /**
     * Server base URL (e.g., https://api.example.com)
     */
    private String baseUrl;

    /**
     * API version (e.g., 1.0.0)
     */
    private String version;

    /**
     * List of all API specification summaries
     */
    private List<RestApiSpecSummary> specs;

    /**
     * Summary information for an individual REST API specification.
     * <p>
     * Contains only the core metadata for each API endpoint.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestApiSpecSummary {

        /**
         * Domain (from YAML tags field, e.g., "Books", "Users")
         */
        private List<String> domain;

        /**
         * HTTP method (e.g., GET, POST, PUT, DELETE)
         */
        private String method;

        /**
         * API path (e.g., /api/users/{id})
         */
        private String path;

        /**
         * Protocol type (fixed value: "rest")
         */
        private String protocol;

        /**
         * API specification unique identifier (UUID)
         */
        private String id;

        /**
         * Development progress status ("mock" or "completed")
         */
        private String progress;

        /**
         * Development tag (x-ouroboros-tag value: "none", "bugfix", "implementing")
         */
        private String tag;
    }
}
