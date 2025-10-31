package kr.co.ouroboros.core.rest.tryit.context;

import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TryContext 단위 테스트
 * 
 * TryContext는 요청 스레드에서 tryId를 관리하는 유틸리티 클래스입니다.
 * ThreadLocal을 사용하여 요청 단위로 tryId를 저장하고 조회합니다.
 * 
 * Session 제거로 인해 TryContext는 Filter에서 자동 생성된 tryId를 관리합니다.
 */
@DisplayName("TryContext 단위 테스트")
class TryContextTest {

    private UUID testTryId;

    @BeforeEach
    void setUp() {
        testTryId = UUID.randomUUID();
    }

    @Test
    @DisplayName("TryContext 설정 및 조회 테스트")
    void testSetAndGetTryId() {
        // Given: 요청 컨텍스트 설정
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // When: TryContext에 tryId 설정
        TryContext.setTryId(testTryId);

        // Then: tryId가 올바르게 조회되는지 확인
        assertTrue(TryContext.hasTryId(), "TryContext에 tryId가 설정되어야 합니다");
        assertEquals(testTryId, TryContext.getTryId(), "설정한 tryId와 조회한 tryId가 일치해야 합니다");

        // Cleanup
        TryContext.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("tryId가 없을 때 조회 테스트")
    void testGetTryId_WhenNoTryId() {
        // Given: 요청 컨텍스트 설정
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Then: tryId가 없어야 함
        assertFalse(TryContext.hasTryId(), "초기 상태에서는 tryId가 없어야 합니다");

        // Cleanup
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("tryId 설정 후 clear 테스트")
    void testClear() {
        // Given: 요청 컨텍스트 설정 및 tryId 설정
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        TryContext.setTryId(testTryId);

        // When: TryContext clear
        TryContext.clear();

        // Then: tryId가 제거되어야 함
        assertFalse(TryContext.hasTryId(), "clear 후에는 tryId가 없어야 합니다");

        // Cleanup
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("여러 번 setTryId 호출 시 마지막 값 유지 테스트")
    void testSetTryId_MultipleTimes() {
        // Given: 요청 컨텍스트 설정
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UUID firstTryId = UUID.randomUUID();
        UUID secondTryId = UUID.randomUUID();

        // When: tryId를 두 번 설정
        TryContext.setTryId(firstTryId);
        TryContext.setTryId(secondTryId);

        // Then: 마지막 설정한 tryId가 저장되어야 함
        assertEquals(secondTryId, TryContext.getTryId(), "마지막으로 설정한 tryId가 반환되어야 합니다");

        // Cleanup
        TryContext.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("setTryId를 null로 설정 시 동작 테스트")
    void testSetTryId_Null() {
        // Given: 요청 컨텍스트 설정 및 tryId 설정
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        TryContext.setTryId(testTryId);

        // When: null로 설정
        TryContext.setTryId(null);

        // Then: tryId가 제거되어야 함
        assertFalse(TryContext.hasTryId(), "null로 설정하면 tryId가 없어야 합니다");

        // Cleanup
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("스레드 격리 테스트")
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