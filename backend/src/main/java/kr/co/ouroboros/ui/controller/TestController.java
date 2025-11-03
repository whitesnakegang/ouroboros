package kr.co.ouroboros.ui.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import kr.co.ouroboros.core.global.annotation.ApiState;
import kr.co.ouroboros.core.global.annotation.ApiState.State;
import kr.co.ouroboros.core.rest.handler.OuroRestHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트용 REST API 컨트롤러 예시.
 * <p>Swagger에서 자동 스캔되어 /swagger-ui.html 또는 /v3/api-docs 에 표시됨.</p>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TestController {

    private final OuroRestHandler ouroRestHandler;

    /**
     * Retrieve a list of users.
     *
     * The response body contains a `"message"` with a success description and a `"data"` array of user names.
     *
     * @return a map with keys `"message"` (success message) and `"data"` (array of user names)
     */
    @GetMapping("/users")
    @ApiState(state = State.COMPLETED)
    public ResponseEntity<List<User>> getUsers() {
        return ResponseEntity.ok(List.of(new User("id1", "name1", "email1"), new User("id2", "name2", "email2")));
    }

    /**
     * Create a new user resource.
     *
     * @param user the user data from the request body
     * @return the created User
     */
    @PostMapping("/users")
    @ApiState(state = State.COMPLETED)
    @ApiResponse(responseCode = "201", description = "정상적으로 조회됨",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = User.class)))
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new User("id3", "name3", "email3"));
    }

    /**
     * Update a user's information identified by the given ID.
     *
     * @param id the ID of the user to update
     * @param request a map containing fields to update and their new values
     * @return a map containing "message" (operation result), "userId" (the user ID), and "updatedData" (the provided request)
     */
    @GetMapping("/users/{id}")
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

    /**
     * Provide a plain "성공" response for the /response endpoint.
     *
     * @param name optional query parameter for a user name filter
     * @param age  optional query parameter for a user age filter
     * @return     the literal string "성공"
     */
    @GetMapping("/response")
    @ApiState(state = State.COMPLETED)
    public ResponseEntity<?> response(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer age
    ) {
        return ResponseEntity.ok("성공");
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        private String id;
        private String name;
        private String email;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String street;
        private String city;
        private String zipCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserWithAddress {
        private String id;
        private String name;
        private Address address;
        private List<Address> previousAddresses = new ArrayList<>();
    }
}
