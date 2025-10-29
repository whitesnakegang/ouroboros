package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import java.util.Objects;
import kr.co.ouroboros.core.rest.common.dto.MediaType;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.Response;
import kr.co.ouroboros.core.rest.common.dto.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * REST API 응답 스펙을 비교하는 컴포넌트.
 * 
 * 스캔된 문서를 기준으로 파일 기반 스펙과 응답 형식을 비교하여
 * 일치 여부를 검사하고 상세한 불일치 사항을 로깅합니다.
 * 
 * @since 0.0.1
 */
@Slf4j
@Component
public class ResponseComparator {

    /**
     * 특정 HTTP 메서드에 대한 응답을 비교합니다.
     *
     * @param method HTTP 메서드명
     * @param scannedOperation 스캔된 Operation (기준)
     * @param fileOperation 파일 기반 Operation (비교 대상)
     * @param endpoint 엔드포인트 경로
     * @param schemaMatchResults 스키마별 일치 여부 Map
     */
    public void compareResponsesForMethod(String method, Operation scannedOperation, Operation fileOperation, String endpoint, Map<String, Boolean> schemaMatchResults) {
        if (scannedOperation == null || fileOperation == null) {
            return;
        }

        Map<String, Response> scannedResponses = scannedOperation.getResponses();
        Map<String, Response> fileResponses = fileOperation.getResponses();

        if (scannedResponses == null || fileResponses == null) {
            log.debug("Responses가 null입니다.");
            return;
        }

        // 스캔된 문서의 각 응답에 대해 파일 문서와 비교
        for (Map.Entry<String, Response> scannedEntry : scannedResponses.entrySet()) {
            String statusCode = scannedEntry.getKey();
            Response scannedResponse = scannedEntry.getValue();
            Response fileResponse = fileResponses.get(statusCode);

            if (fileResponse == null) {
                System.out.println(String.format("[RESPONSE MISSING] %s %s - Status %s: File spec에 해당 상태코드 응답이 없습니다. 스캔된 응답을 추가합니다.", 
                    method, endpoint, statusCode));
                // 파일 스펙에 스캔된 응답 추가
                fileResponses.put(statusCode, scannedResponse);
                continue;
            }

            // 응답 스키마 비교
            boolean isMatch = compareResponseSchemas(scannedResponse, fileResponse, method, endpoint, statusCode, schemaMatchResults);
            
            if (isMatch) {
                System.out.println(String.format("[RESPONSE MATCH] %s %s - Status %s: 응답 형식이 일치합니다.", 
                    method, endpoint, statusCode));
            } else {
                System.out.println(String.format("[RESPONSE MISMATCH] %s %s - Status %s: 응답 형식이 일치하지 않습니다.", 
                    method, endpoint, statusCode));
                // 불일치 => 파일 Operation에 표기
                fileOperation.setXOuroborosDiff("response");
            }
        }
    }

    /**
     * 두 응답의 스키마를 비교합니다.
     *
     * @param scannedResponse 스캔된 응답 (기준)
     * @param fileResponse 파일 기반 응답 (비교 대상)
     * @param method HTTP 메서드
     * @param endpoint 엔드포인트
     * @param statusCode 상태코드
     * @param schemaMatchResults 스키마별 일치 여부 Map
     * @return 스키마가 일치하면 true, 그렇지 않으면 false
     */
    private boolean compareResponseSchemas(Response scannedResponse, Response fileResponse, String method, String endpoint, String statusCode, Map<String, Boolean> schemaMatchResults) {
        if (scannedResponse == null && fileResponse == null) {
            return true;
        }
        if (scannedResponse == null || fileResponse == null) {
            return false;
        }

        // Content 비교 (상태코드, content-type, schema만)
        if (!compareContent(scannedResponse.getContent(), fileResponse.getContent(), method, endpoint, statusCode, schemaMatchResults)) {
            return false;
        }

        return true;
    }

    /**
     * Content Map을 비교합니다.
     *
     * @param scannedContent 스캔된 Content (기준)
     * @param fileContent 파일 기반 Content (비교 대상)
     * @param method HTTP 메서드
     * @param endpoint 엔드포인트
     * @param statusCode 상태코드
     * @param schemaMatchResults 스키마별 일치 여부 Map
     * @return Content가 일치하면 true, 그렇지 않으면 false
     */
    private boolean compareContent(Map<String, MediaType> scannedContent, Map<String, MediaType> fileContent, String method, String endpoint, String statusCode, Map<String, Boolean> schemaMatchResults) {
        if (scannedContent == null && fileContent == null) {
            return true;
        }
        if (scannedContent == null || fileContent == null) {
            return false;
        }

        // 스캔된 문서의 각 Content Type에 대해 비교
        for (Map.Entry<String, MediaType> scannedEntry : scannedContent.entrySet()) {
            String contentType = scannedEntry.getKey();
            MediaType scannedMediaType = scannedEntry.getValue();
            MediaType fileMediaType = fileContent.get(contentType);

            if (fileMediaType == null) {
                // */*는 모든 content-type과 일치한다고 간주
                if ("*/*".equals(contentType)) {
                    System.out.println(String.format("[CONTENT TYPE WILDCARD] %s %s - Status %s: Content-Type '*/*'는 모든 타입과 일치합니다.", 
                        method, endpoint, statusCode));
                    continue;
                }
                System.out.println(String.format("[CONTENT TYPE MISSING] %s %s - Status %s: Content-Type '%s'가 파일 스펙에 없습니다.", 
                    method, endpoint, statusCode, contentType));
                return false;
            }

            if (!compareMediaTypes(scannedMediaType, fileMediaType, method, endpoint, statusCode, contentType, schemaMatchResults)) {
                return false;
            }
        }

        return true;
    }

    /**
     * MediaType을 비교합니다.
     *
     * @param scannedMediaType 스캔된 MediaType (기준)
     * @param fileMediaType 파일 기반 MediaType (비교 대상)
     * @param method HTTP 메서드
     * @param endpoint 엔드포인트
     * @param statusCode 상태코드
     * @param contentType Content-Type
     * @param schemaMatchResults 스키마별 일치 여부 Map
     * @return MediaType이 일치하면 true, 그렇지 않으면 false
     */
    private boolean compareMediaTypes(MediaType scannedMediaType, MediaType fileMediaType, String method, String endpoint, String statusCode, String contentType, Map<String, Boolean> schemaMatchResults) {
        if (scannedMediaType == null && fileMediaType == null) {
            return true;
        }
        if (scannedMediaType == null || fileMediaType == null) {
            return false;
        }

        return compareSchemas(scannedMediaType.getSchema(), fileMediaType.getSchema(), method, endpoint, statusCode, contentType, schemaMatchResults);
    }

    /**
     * Schema를 비교합니다. ($ref와 type만 비교)
     *
     * @param scannedSchema 스캔된 Schema (기준)
     * @param fileSchema 파일 기반 Schema (비교 대상)
     * @param method HTTP 메서드
     * @param endpoint 엔드포인트
     * @param statusCode 상태코드
     * @param contentType Content-Type
     * @param schemaMatchResults 스키마별 일치 여부 Map
     * @return Schema가 일치하면 true, 그렇지 않으면 false
     */
    private boolean compareSchemas(Schema scannedSchema, Schema fileSchema, String method, String endpoint, String statusCode, String contentType, Map<String, Boolean> schemaMatchResults) {
        if (scannedSchema == null && fileSchema == null) {
            return true;
        }
        if (scannedSchema == null || fileSchema == null) {
            System.out.println(String.format("[SCHEMA NULL MISMATCH] %s %s - Status %s, Content-Type '%s': 한쪽 스키마가 null입니다.", 
                method, endpoint, statusCode, contentType));
            return false;
        }

        // $ref 비교 (객체 참조인 경우)
        if (scannedSchema.getRef() != null || fileSchema.getRef() != null) {
            if (!Objects.equals(scannedSchema.getRef(), fileSchema.getRef())) {
                System.out.println(String.format("[SCHEMA REF MISMATCH] %s %s - Status %s, Content-Type '%s': $ref가 다릅니다. (스캔: %s, 파일: %s)", 
                    method, endpoint, statusCode, contentType, scannedSchema.getRef(), fileSchema.getRef()));
                return false;
            }
            
            // $ref가 같으면 schemaMatchResults에서 확인
            if (scannedSchema.getRef() != null) {
                String schemaName = extractSchemaNameFromRef(scannedSchema.getRef());
                if (schemaName != null && schemaMatchResults.containsKey(schemaName)) {
                    boolean schemaMatch = schemaMatchResults.get(schemaName);
                    if (!schemaMatch) {
                        System.out.println(String.format("[SCHEMA REF MISMATCH] %s %s - Status %s, Content-Type '%s': 참조하는 스키마 '%s'가 일치하지 않습니다.", 
                            method, endpoint, statusCode, contentType, schemaName));
                        return false;
                    }
                }
            }
        }
        // type 비교 (기본 타입인 경우)
        else {
            if (!Objects.equals(scannedSchema.getType(), fileSchema.getType())) {
                System.out.println(String.format("[SCHEMA TYPE MISMATCH] %s %s - Status %s, Content-Type '%s': 타입이 다릅니다. (스캔: %s, 파일: %s)", 
                    method, endpoint, statusCode, contentType, scannedSchema.getType(), fileSchema.getType()));
                return false;
            }
        }

        return true;
    }
    
    /**
     * $ref에서 스키마명을 추출합니다.
     * 예: "#/components/schemas/User" -> "User"
     *
     * @param ref $ref 값
     * @return 스키마명, 추출할 수 없으면 null
     */
    private String extractSchemaNameFromRef(String ref) {
        if (ref == null || !ref.startsWith("#/components/schemas/")) {
            return null;
        }
        return ref.substring("#/components/schemas/".length());
    }

}
