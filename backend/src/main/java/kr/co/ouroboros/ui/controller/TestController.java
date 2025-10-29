package kr.co.ouroboros.ui.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Map;
import kr.co.ouroboros.core.global.annotation.ApiState;
import kr.co.ouroboros.core.global.annotation.ApiState.State;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.rest.handler.OuroRestHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 테스트용 REST API 컨트롤러 예시.
 * <p>Swagger에서 자동 스캔되어 /swagger-ui.html 또는 /v3/api-docs 에 표시됨.</p>
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final OuroRestHandler ouroRestHandler;


    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    static class User {
        private String name;
    }

    /**
     * 사용자 목록을 조회하는 API
     */
    @GetMapping("/users")
    @ApiState(state = State.COMPLETED)
    public ResponseEntity<Map<String, Object>> getUsers() {
        return ResponseEntity.ok(Map.of(
                "message", "사용자 목록 조회 성공",
                "data", new String[]{"방준엽", "홍길동", "이몽룡"}
        ));
    }

    /**
     * 새로운 사용자를 등록하는 API
     */
    @PostMapping("/users")
    @ApiState(state = State.COMPLETED)
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new User("heello"));
    }

    /**
     * 특정 사용자 정보를 수정하는 API
     */
    @PutMapping("/users/{id}")
    @ApiState(state = State.BUG_FIXING)
    @Operation(summary = "사용자 조회", description = "ID로 사용자 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상적으로 조회됨",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> request
    ) {
        return ResponseEntity.ok(Map.of(
                "message", "사용자 정보 수정 성공",
                "userId", id,
                "updatedData", request
        ));
    }

    /**
     * 사용자 검색 API
     * <p>이름(name)이나 나이(age)로 사용자를 필터링합니다.</p>
     */
    @GetMapping("/users/search")
    @ApiState(state = State.COMPLETED)
    @Operation(summary = "사용자 검색", description = "이름(name)과 나이(age)로 사용자를 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer age
    ) {
        return ResponseEntity.ok(Map.of(
                "message", "검색 결과",
                "filter", Map.of("name", name, "age", age),
                "data", new String[]{"방준엽", "홍길동"} // 예시 데이터
        ));
    }
}
