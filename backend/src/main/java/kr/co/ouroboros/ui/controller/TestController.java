package kr.co.ouroboros.ui.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Map;
import kr.co.ouroboros.core.global.annotation.ApiState;
import kr.co.ouroboros.core.global.annotation.ApiState.State;
import kr.co.ouroboros.core.rest.handler.OuroRestHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        private Integer age;
        private Double height;
        private Address address;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    static class Address {
        String roadname;
        String dong;
        String gu;
    }

    /**
     * Retrieve a list of users.
     *
     * The response body contains a `"message"` with a success description and a `"data"` array of user names.
     *
     * @return a map with keys `"message"` (success message) and `"data"` (array of user names)
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
     * Create a new user resource.
     *
     * @param user the user data from the request body
     * @return the created User
     */
    @PostMapping("/users")
    @ApiState(state = State.COMPLETED)
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new User("name", 13, 180.2, new Address("road", "Adong", "Bgu")));
    }

    /**
     * Update a user's information identified by the given ID.
     *
     * @param id the ID of the user to update
     * @param request a map containing fields to update and their new values
     * @return a map containing "message" (operation result), "userId" (the user ID), and "updatedData" (the provided request)
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
     * Searches for users using optional name and/or age filters.
     *
     * @param name optional name filter; when provided, results are filtered by this name
     * @param age  optional age filter; when provided, results are filtered by this age
     * @return a map containing:
     *         - "message": a status message,
     *         - "filter": the provided filter values,
     *         - "data": an array of matching user names
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

    /**
     * Provide a plain "성공" response for the /response endpoint.
     *
     * @param name optional query parameter for a user name filter
     * @param age  optional query parameter for a user age filter
     * @return     the literal string "성공"
     */
    @GetMapping("/response")
    @ApiState(state = State.COMPLETED)
    public ResponseEntity<String> response(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer age
    ) {
        return ResponseEntity.ok("성공");
    }
}