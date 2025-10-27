package kr.co.ouroboros.core.rest.tryit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.OuroborosApplication;
import kr.co.ouroboros.core.rest.tryit.config.TrySessionProperties;
import kr.co.ouroboros.core.rest.tryit.session.TrySessionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = OuroborosApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "ouroboros.try-session.max-active=50"
})
class TryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TrySessionRegistry registry;

    @Test
    void testCreateSession() throws Exception {
        // First session should succeed
        mockMvc.perform(post("/ouroboros/tries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tryId").exists())
                .andExpect(jsonPath("$.headerName").value("X-Ouroboros-Try"))
                .andExpect(jsonPath("$.baggageKey").value("ouro.try_id"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void testCreateSessionWithMaxActiveSessions() throws Exception {
        // Fill registry to near max (leave 1 slot)
        for (int i = 0; i < 49; i++) {
            registry.register(java.util.UUID.randomUUID(), "127.0.0.1", 120, false);
        }

        // This should still succeed (49/50)
        mockMvc.perform(post("/ouroboros/tries"))
                .andExpect(status().isOk());
        
        // Now at 50/50, next one should fail
        mockMvc.perform(post("/ouroboros/tries"))
                .andExpect(status().isTooManyRequests());
    }
}
