package kr.co.ouroboros.core.rest.tryit.controller;

import kr.co.ouroboros.OuroborosApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TryController 통합 테스트
 * 
 * Session 생성을 제거하고 헤더 기반 방식으로 변경되었습니다.
 */
@SpringBootTest(classes = OuroborosApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("TryController 통합 테스트")
class TryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("유효하지 않은 tryId 형식")
    void testGetResult_InvalidTryId() throws Exception {
        mockMvc.perform(get("/ouroboros/tries/invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효한 tryId로 결과 조회 - PENDING 상태")
    void testGetResult_ValidTryId() throws Exception {
        String validUuid = UUID.randomUUID().toString();
        
        mockMvc.perform(get("/ouroboros/tries/{tryId}", validUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tryId").value(validUuid))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Try 헤더 포함 시 응답 body에 tryId 반환")
    void testTryHeader_returnsTryIdInResponseBody() throws Exception {
        mockMvc.perform(get("/ouroboros/tries/550e8400-e29b-41d4-a716-446655440000")
                .header("X-Ouroboros-Try", "on"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._ouroborosTryId").exists())
                .andExpect(jsonPath("$._ouroborosTryId").isNotEmpty());
    }

    @Test
    @DisplayName("Try 헤더 없을 때 응답 body에 tryId 없음")
    void testNoTryHeader_noTryIdInResponseBody() throws Exception {
        mockMvc.perform(get("/ouroboros/tries/550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._ouroborosTryId").doesNotExist());
    }

    @Test
    @DisplayName("Try 헤더 대소문자 무시 - ON")
    void testTryHeaderCaseInsensitive_ON() throws Exception {
        mockMvc.perform(get("/ouroboros/tries/550e8400-e29b-41d4-a716-446655440000")
                .header("X-Ouroboros-Try", "ON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._ouroborosTryId").exists())
                .andExpect(jsonPath("$._ouroborosTryId").isNotEmpty());
    }

    @Test
    @DisplayName("Try 헤더 대소문자 무시 - On")
    void testTryHeaderCaseInsensitive_On() throws Exception {
        mockMvc.perform(get("/ouroboros/tries/550e8400-e29b-41d4-a716-446655440000")
                .header("X-Ouroboros-Try", "On"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._ouroborosTryId").exists())
                .andExpect(jsonPath("$._ouroborosTryId").isNotEmpty());
    }

    @Test
    @DisplayName("Try 헤더 잘못된 값 - off")
    void testTryHeaderInvalidValue_off() throws Exception {
        mockMvc.perform(get("/ouroboros/tries/550e8400-e29b-41d4-a716-446655440000")
                .header("X-Ouroboros-Try", "off"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._ouroborosTryId").doesNotExist());
    }

}
