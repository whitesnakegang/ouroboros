package kr.co.ouroboros.core.rest.tryit.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TrySessionRegistryTest {

    private TrySessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TrySessionRegistry();
    }

    @Test
    void testRegisterAndValidate() {
        UUID tryId = UUID.randomUUID();
        String clientIp = "127.0.0.1";
        
        TrySession session = registry.register(tryId, clientIp, 120, false);
        assertNotNull(session);
        assertEquals(1, registry.getActiveSessionCount());
        
        assertTrue(registry.isValid(tryId, clientIp, true));
    }

    @Test
    void testIpBinding() {
        UUID tryId = UUID.randomUUID();
        
        registry.register(tryId, "127.0.0.1", 120, false);
        
        assertTrue(registry.isValid(tryId, "127.0.0.1", true));
        assertFalse(registry.isValid(tryId, "192.168.1.1", true));
        
        // IP 바인딩 끄면 통과
        assertTrue(registry.isValid(tryId, "192.168.1.1", false));
    }

    @Test
    void testExpiredSession() throws InterruptedException {
        UUID tryId = UUID.randomUUID();
        
        registry.register(tryId, "127.0.0.1", 1, false);
        
        assertTrue(registry.isValid(tryId, "127.0.0.1", true));
        
        Thread.sleep(1100); // 1.1초 대기
        
        assertFalse(registry.isValid(tryId, "127.0.0.1", true));
    }

    @Test
    void testOneShot() {
        UUID tryId = UUID.randomUUID();
        
        registry.register(tryId, "127.0.0.1", 120, true);
        
        assertTrue(registry.isValid(tryId, "127.0.0.1", true));
        
        registry.markUsed(tryId);
        
        assertFalse(registry.isValid(tryId, "127.0.0.1", true));
    }

    @Test
    void testCleanup() {
        for (int i = 0; i < 3; i++) {
            UUID tryId = UUID.randomUUID();
            registry.register(tryId, "127.0.0.1", 1, false);
        }
        
        assertEquals(3, registry.getActiveSessionCount());
        
        registry.cleanup();
        assertEquals(3, registry.getActiveSessionCount()); // 아직 만료 안됨
    }

    @Test
    void testMaxActiveSessions() {
        for (int i = 0; i < 50; i++) {
            UUID tryId = UUID.randomUUID();
            registry.register(tryId, "127.0.0.1", 120, false);
        }
        
        assertEquals(50, registry.getActiveSessionCount());
    }
}
