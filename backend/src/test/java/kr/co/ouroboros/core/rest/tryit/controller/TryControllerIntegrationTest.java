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

}
