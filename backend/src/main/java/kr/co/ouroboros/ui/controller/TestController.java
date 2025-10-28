package kr.co.ouroboros.ui.controller;

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
}
