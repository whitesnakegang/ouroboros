package kr.co.ouroboros.core.rest.tryit.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TryContext.
 */
class TryContextTest {

    @Test
    void testSetAndGetTryId() {
        UUID tryId = UUID.randomUUID();
        
        TryContext.setTryId(tryId);
        assertEquals(tryId, TryContext.getTryId());
        assertTrue(TryContext.hasTryId());
        
        TryContext.clear();
    }

    @Test
    void testHasTryId() {
        assertFalse(TryContext.hasTryId());
        
        UUID tryId = UUID.randomUUID();
        TryContext.setTryId(tryId);
        assertTrue(TryContext.hasTryId());
        
        TryContext.clear();
        assertFalse(TryContext.hasTryId());
    }

    @Test
    void testClear() {
        UUID tryId = UUID.randomUUID();
        TryContext.setTryId(tryId);
        assertTrue(TryContext.hasTryId());
        
        TryContext.clear();
        assertFalse(TryContext.hasTryId());
        assertNull(TryContext.getTryId());
    }

    @Test
    void testSetNullTryId() {
        TryContext.setTryId(null);
        assertFalse(TryContext.hasTryId());
        assertNull(TryContext.getTryId());
    }

    @Test
    void testThreadIsolation() throws InterruptedException {
        UUID tryId1 = UUID.randomUUID();
        UUID tryId2 = UUID.randomUUID();
        
        // Set in main thread
        TryContext.setTryId(tryId1);
        assertEquals(tryId1, TryContext.getTryId());
        
        // Check in another thread
        Thread thread = new Thread(() -> {
            assertFalse(TryContext.hasTryId());
            assertNull(TryContext.getTryId());
            
            TryContext.setTryId(tryId2);
            assertEquals(tryId2, TryContext.getTryId());
            
            TryContext.clear();
        });
        
        thread.start();
        thread.join();
        
        // Main thread still has original tryId
        assertEquals(tryId1, TryContext.getTryId());
        
        TryContext.clear();
    }
}
