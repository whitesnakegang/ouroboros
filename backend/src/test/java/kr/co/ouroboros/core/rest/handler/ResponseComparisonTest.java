package kr.co.ouroboros.core.rest.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.handler.comparator.ResponseComparator;
import kr.co.ouroboros.core.rest.handler.helper.RequestDiffHelper.HttpMethod;
import kr.co.ouroboros.core.rest.loader.TestResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResponseComparisonTest {

    private ResponseComparator responseComparator;
    private final TestResourceLoader resourceLoader = new TestResourceLoader();

    @BeforeEach
    public void setUp() {
        responseComparator = new ResponseComparator();
    }
    
    /**
     * 스캔 스팩에 파일 스팩에 없는 Response Status 있는 경우(나머지는 Status 동일)
     * Expect: 파일 스펙에 존재하는 Status는 일치하고 스캔 스펙에 추가 Status가 존재하는 경우 일치 판정(파일 스팩에 추가 스팩 반영)
     * diff : none
     * progress : completed
     */
    @Test
    public void 모두_일치하는_경우() throws Exception {
        // YAML 파일에서 테스트 데이터 로드
        OuroRestApiSpec scannedSpec = resourceLoader.loadResponseTest("response-all-match-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadResponseTest("response-all-match-file.yaml");

        // 스키마 매칭 결과 (User 스키마가 일치)
        Map<String, Boolean> schemaMatchResults = new HashMap<>();
        schemaMatchResults.put("User", true);

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/api/test/users/{id}")
                .getPut();
        Operation fileOperation = fileSpec.getPaths()
                .get("/api/test/users/{id}")
                .getPut();

        // 응답 비교 실행
        // 파일 스펙의 초기 상태: diff 없음("none")
        responseComparator.compareResponsesForMethod("/api/test/users/{id}", HttpMethod.PUT, scannedOperation, fileOperation, schemaMatchResults);

        // 검증: 모든 응답 일치이므로 diff는 none 유지, progress=completed 설정
        assertEquals("none", fileOperation.getXOuroborosDiff(), "일치하는 응답이면 diff가 none이어야 합니다.");
        assertEquals("completed", fileOperation.getXOuroborosProgress(), "응답이 모두 일치하고 diff가 none이면 progress는 completed여야 합니다.");
    }
    
    /**
     * $ref가 다른 경우(다른 객체 타입을 사용하는 경우) ex) file : Book / scan : User
     * Expect: 반환 객체가 다르기 때문에 불일치 판정
     * diff : response
     * progress : mock
     * @throws Exception
     */
    @Test
    public void ref가_다른_경우() throws Exception {
        // YAML 파일에서 테스트 데이터 로드
        OuroRestApiSpec scannedSpec = resourceLoader.loadResponseTest("response-mismatch-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadResponseTest("response-mismatch-file.yaml");

        // 스키마 매칭 결과 (User 스키마가 일치하지 않음)
        Map<String, Boolean> schemaMatchResults = new HashMap<>();
        schemaMatchResults.put("User", false);

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/api/test/users")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/api/test/users")
                .getGet();

        // 파일 스펙의 초기 상태는 JSON에 포함됨: diff=none, progress=completed

        // 응답 비교 실행
        responseComparator.compareResponsesForMethod("/api/test/users", HttpMethod.GET, scannedOperation, fileOperation, schemaMatchResults);

        // 검증: 불일치 → diff: none -> response, progress: completed -> mock
        assertEquals("response", fileOperation.getXOuroborosDiff(), "$ref가 다르면 diff는 'response'가 되어야 합니다.");
        assertEquals("mock", fileOperation.getXOuroborosProgress(), "불일치면 completed였던 progress는 mock으로 바뀌어야 합니다.");
    }
    
    /**
     * request도 불일치하는 상황에서 response도 불일치 하는 경우
     * Expect : 불일치 판정 + request와 response 둘 다 불일치 하기 때문에 both로 설정
     * diff : both
     * progress : mock
     * @throws Exception
     */
    @Test
    public void request와_response가_모두_불일치하는_경우() throws Exception {
        // YAML 파일에서 테스트 데이터 로드
        OuroRestApiSpec scannedSpec = resourceLoader.loadResponseTest("response-diff-request-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadResponseTest("response-diff-request-file.yaml");

        Map<String, Boolean> schemaMatchResults = new HashMap<>();

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/api/test/response")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/api/test/response")
                .getGet();

        // 파일 스펙의 초기 상태는 JSON에 포함됨: diff=request, progress=completed

        // 응답 비교 실행 (경로/메서드 일치하게 정정)
        responseComparator.compareResponsesForMethod("/api/test/response", HttpMethod.GET, scannedOperation, fileOperation, schemaMatchResults);

        // 검증: 불일치 → diff: request -> both, progress: completed -> mock
        assertEquals("both", fileOperation.getXOuroborosDiff(), "기존 diff가 request이면 both로 변경되어야 합니다.");
        assertEquals("mock", fileOperation.getXOuroborosProgress(), "불일치면 completed였던 progress는 mock으로 바뀌어야 합니다.");
    }
    
    /**
     * content-type이 다른 경우 (하지만 schema는 같은 경우)
     * expect: content-type은 무시하고 schema만 비교하므로 일치 판정
     * diff : none
     * progress : completed
     */
    @Test
    public void Content_type이_다른_경우() throws Exception {
        // YAML 파일에서 테스트 데이터 로드
        OuroRestApiSpec scannedSpec = resourceLoader.loadResponseTest("response-different-content-type-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadResponseTest("response-different-content-type-file.yaml");

        Map<String, Boolean> schemaMatchResults = new HashMap<>();

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/api/test/content-type")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/api/test/content-type")
                .getGet();

        // 응답 비교 실행
        responseComparator.compareResponsesForMethod("/api/test/content-type", HttpMethod.GET, scannedOperation, fileOperation, schemaMatchResults);

        // 검증: content-type은 무시하고 schema만 비교하므로, schema가 같으면 일치 판정
        assertEquals("none", fileOperation.getXOuroborosDiff(), "content-type은 무시하고 schema만 비교하므로, schema가 같으면 diff는 'none'이어야 합니다.");
        assertEquals("completed", fileOperation.getXOuroborosProgress(), "schema가 같으면 progress는 'completed'여야 합니다.");
    }
    
    /**
     * 파일 스팩의 content-type이 application/json이고 스캔 스펙이 와일드카드인 경우 일치판정
     * expect: 일치판정
     * diff : none
     * progress : completed
     */
    @Test
    public void Content_type이_와일드카드인_경우() throws Exception {
        // YAML 파일에서 테스트 데이터 로드
        OuroRestApiSpec scannedSpec = resourceLoader.loadResponseTest("response-wildcard-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadResponseTest("response-wildcard-file.yaml");

        Map<String, Boolean> schemaMatchResults = new HashMap<>();

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/api/test/wildcard")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/api/test/wildcard")
                .getGet();

        // 응답 비교 실행
        responseComparator.compareResponsesForMethod("/api/test/wildcard", HttpMethod.GET, scannedOperation, fileOperation, schemaMatchResults);

        // 검증: */*는 모든 content-type과 일치하므로 diff는 none 유지
        assertEquals("none", fileOperation.getXOuroborosDiff(), "*/* content-type은 모든 타입과 일치하므로 diff는 none이어야 합니다.");
        assertEquals("completed", fileOperation.getXOuroborosProgress(), "*/* content-type은 모든 타입과 일치하므로 progress는 completed이어야 합니다.");
    }
    
    /**
     *  스캔스펙에_추가적인_status가_존재하고_나머지_status는_모두_일치하는_경우
     *  expect: 스캔 스펙에만 있는 status는 추가하지 않고 불일치 판정
     *  diff: response
     *  progress: mock
     * @throws Exception
     */
    @Test
    public void 스캔스펙에_추가적인_status가_존재하고_나머지_status는_모두_일치하는_경우() throws Exception {
        // YAML 파일에서 테스트 데이터 로드
        OuroRestApiSpec scannedSpec = resourceLoader.loadResponseTest("response-missing-in-file-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadResponseTest("response-missing-in-file-file.yaml");

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/p")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/p")
                .getGet();

        responseComparator.compareResponsesForMethod("/p", HttpMethod.GET, scannedOperation, fileOperation, new HashMap<>());

        // 201이 추가되지 않아야 함
        assertNotNull(fileOperation.getResponses());
        assertNotNull(fileOperation.getResponses().get("200"), "200 status는 존재해야 합니다.");
        assertNull(fileOperation.getResponses().get("201"), "201 status는 추가되지 않아야 합니다.");
        // 스캔 스펙에만 201이 있으므로 불일치 판정
        assertEquals("response", fileOperation.getXOuroborosDiff(), "스캔 스펙에만 있는 status가 있으면 diff는 'response'가 되어야 합니다.");
        assertEquals("mock", fileOperation.getXOuroborosProgress(), "불일치면 progress는 'mock'이어야 합니다.");
    }
    
    /**
     * 파일스팩에 정의된 status가 스캔스펙에 없는경우
     * Expect : 명세에 작성한 게 미구현한 것이므로 불일치 판정
     * diff : response
     * progress : mock
     * @throws Exception
     */
    @Test
    public void 파일스팩에_정의된_status가_스캔스펙에_없는_경우() throws Exception {
        // YAML 파일에서 테스트 데이터 로드
        OuroRestApiSpec scannedSpec = resourceLoader.loadResponseTest("response-file-only-status-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadResponseTest("response-file-only-status-file.yaml");

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/e")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/e")
                .getGet();

        responseComparator.compareResponsesForMethod("/e", HttpMethod.GET, scannedOperation, fileOperation, new HashMap<>());

        assertEquals("response", fileOperation.getXOuroborosDiff());
        assertEquals("mock", fileOperation.getXOuroborosProgress());
    }
    
    /**
     * 파일스팩에 정의된 status가 스캔스펙에 없고 request도 불일치 하는 경우
     * Expect : 명세에 작성한 게 미구현한 것이므로 불일치 판정
     * diff : both
     * progress : mock
     * @throws Exception
     */
    @Test
    public void 파일스팩에_정의된_status가_스캔스펙에_없고_request도_불일치하는_경우() throws Exception {
        // YAML 파일에서 테스트 데이터 로드
        OuroRestApiSpec scannedSpec = resourceLoader.loadResponseTest("response-file-only-status-diff-request-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadResponseTest("response-file-only-status-diff-request-file.yaml");

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/e2")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/e2")
                .getGet();

        responseComparator.compareResponsesForMethod("/e2", HttpMethod.GET, scannedOperation, fileOperation, new HashMap<>());

        assertEquals("both", fileOperation.getXOuroborosDiff());
        assertEquals("mock", fileOperation.getXOuroborosProgress());
    }
    
    /**
     * 파일 스펙에 스캔 스펙에 없는 추가 content-type이 있는 경우
     * Expect: content-type은 무시하고 schema만 비교하므로, scan 스펙에 있는 content-type의 schema가 일치하면 일치 판정
     * diff: none (schema가 일치하는 경우)
     * progress: completed (schema가 일치하는 경우)
     * @throws Exception
     */
    @Test
    public void 파일_스펙에_추가_content_type이_있지만_schema는_일치하는_경우() throws Exception {
        // YAML 파일에서 테스트 데이터 로드
        OuroRestApiSpec scannedSpec = resourceLoader.loadResponseTest("response-all-match-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadResponseTest("response-all-match-file.yaml");

        // 스키마 매칭 결과 (User 스키마가 일치)
        Map<String, Boolean> schemaMatchResults = new HashMap<>();
        schemaMatchResults.put("User", true);

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/api/test/users/{id}")
                .getPut();
        Operation fileOperation = fileSpec.getPaths()
                .get("/api/test/users/{id}")
                .getPut();

        // file 스펙에 추가 content-type 추가 (application/xml)
        // content-type은 무시하고 schema만 비교하므로, application/json의 schema가 일치하면 일치 판정
        if (fileOperation.getResponses() != null && fileOperation.getResponses().get("200") != null) {
            var fileResponse = fileOperation.getResponses().get("200");
            if (fileResponse.getContent() != null) {
                // application/xml content-type 추가
                var xmlMediaType = new kr.co.ouroboros.core.rest.common.dto.MediaType();
                var xmlSchema = new kr.co.ouroboros.core.rest.common.dto.Schema();
                xmlSchema.setType("string");
                xmlMediaType.setSchema(xmlSchema);
                fileResponse.getContent().put("application/xml", xmlMediaType);
            }
        }

        // 파일 스펙의 초기 상태: diff 없음("none")
        fileOperation.setXOuroborosDiff("none");
        fileOperation.setXOuroborosProgress("completed");

        // 응답 비교 실행
        responseComparator.compareResponsesForMethod("/api/test/users/{id}", HttpMethod.PUT, scannedOperation, fileOperation, schemaMatchResults);

        // 검증: content-type은 무시하고 schema만 비교하므로, scan 스펙에 있는 application/json의 schema가 일치하면 일치 판정
        assertEquals("none", fileOperation.getXOuroborosDiff(), "content-type은 무시하고 schema만 비교하므로, schema가 일치하면 diff는 'none'이어야 합니다.");
        assertEquals("completed", fileOperation.getXOuroborosProgress(), "schema가 일치하면 progress는 'completed'여야 합니다.");
    }
    
    /**
     * 파일 스펙에 스캔 스펙에 없는 추가 content-type이 있고, schema도 다른 경우
     * Expect: content-type은 무시하고 schema만 비교하므로, scan 스펙에 있는 content-type의 schema가 불일치하면 불일치 판정
     * diff: response
     * progress: mock
     * @throws Exception
     */
    @Test
    public void 파일_스펙에_추가_content_type이_있고_schema도_다른_경우() throws Exception {
        // YAML 파일에서 테스트 데이터 로드
        OuroRestApiSpec scannedSpec = resourceLoader.loadResponseTest("response-mismatch-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadResponseTest("response-mismatch-file.yaml");

        // 스키마 매칭 결과 (User 스키마가 불일치)
        Map<String, Boolean> schemaMatchResults = new HashMap<>();
        schemaMatchResults.put("User", true);

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/api/test/users")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/api/test/users")
                .getGet();

        // file 스펙에 추가 content-type 추가 (application/xml)
        if (fileOperation.getResponses() != null && fileOperation.getResponses().get("200") != null) {
            var fileResponse = fileOperation.getResponses().get("200");
            if (fileResponse.getContent() != null) {
                // application/xml content-type 추가
                var xmlMediaType = new kr.co.ouroboros.core.rest.common.dto.MediaType();
                var xmlSchema = new kr.co.ouroboros.core.rest.common.dto.Schema();
                xmlSchema.setType("string");
                xmlMediaType.setSchema(xmlSchema);
                fileResponse.getContent().put("application/xml", xmlMediaType);
            }
        }

        // 파일 스펙의 초기 상태: diff 없음("none")
        fileOperation.setXOuroborosDiff("none");
        fileOperation.setXOuroborosProgress("completed");

        // 응답 비교 실행
        responseComparator.compareResponsesForMethod("/api/test/users", HttpMethod.GET, scannedOperation, fileOperation, schemaMatchResults);

        // 검증: content-type은 무시하고 schema만 비교하므로, scan 스펙에 있는 application/json의 schema가 불일치하면 불일치 판정
        assertEquals("response", fileOperation.getXOuroborosDiff(), "content-type은 무시하고 schema만 비교하므로, schema가 불일치하면 diff는 'response'가 되어야 합니다.");
        assertEquals("mock", fileOperation.getXOuroborosProgress(), "schema가 불일치하면 progress는 'mock'이어야 합니다.");
    }
}
