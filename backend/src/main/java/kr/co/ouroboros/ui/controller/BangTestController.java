package kr.co.ouroboros.ui.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import kr.co.ouroboros.core.global.annotation.ApiState;
import kr.co.ouroboros.core.global.annotation.ApiState.State;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 테스트용 REST API 컨트롤러 예시.
 * <p>ourorest.yml의 /api/users GET과 다른 요청 파라미터를 가진 API를 정의합니다.</p>
 */
@RestController
@RequestMapping("/ouro")
public class BangTestController {

    /**
     * Finds users filtered by the optional name and email query parameters.
     *
     * @param name  the optional name to filter users by
     * @param email the optional email to filter users by
     * @return a ResponseEntity whose body is a list of matching User objects; the list may be empty
     */
    @GetMapping("/users")
    @Operation(summary = "Find users (request param)", description = "Query parameters로 사용자를 필터링합니다. (파라미터 2개)")
    @ApiState(state = State.BUG_FIXING)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Filtered user list",
                    content = @Content(schema = @Schema(implementation = User[].class)))
    })
    public ResponseEntity<List<User>> findUsers(
            @Parameter(description = "사용자 이름") @RequestParam(required = false) String name,
            @Parameter(description = "이메일") @RequestParam(required = false) String email) {
        List<User> users = List.of(
            new User("1", "John Doe", "john@example.com"),
            new User("2", "Jane Smith", "jane@example.com")
        );
        return ResponseEntity.ok(users);
    }

    /**
     * Retrieve a single user by its ID.
     *
     * @param userId the numeric identifier of the user to retrieve
     * @return a ResponseEntity containing the found User and HTTP 200 OK
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user by ID (path param)", description = "Path parameter로 단일 사용자를 조회합니다. (다른 이름과 타입)")
    @ApiState(state = State.COMPLETED)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Found user",
                    content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<User> getUserById(
            @Parameter(description = "User ID (integer)") @PathVariable Integer userId) {
        User user = new User(String.valueOf(userId), "Test User", "test@example.com");
        return ResponseEntity.ok(user);
    }

    /**
     * Create a user using the path parameter id and the provided request body.
     *
     * @param id   path parameter identifying the user to create
     * @param user request body containing the user's attributes
     * @return the created User object
     */
    @PostMapping("/users/{id}")
    @Operation(summary = "Create user by ID (path param)", description = "Path parameter로 사용자를 생성합니다. (ourorest.yml에는 없음)")
    @ApiState(state = State.COMPLETED)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created",
                    content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<User> createUserById(
            @Parameter(description = "User ID") @PathVariable String id,
            @RequestBody User user) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(user);
    }

    // ==================== DTO 클래스들 ====================
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        private String id;
        private String name;
        private String email;
    }
}