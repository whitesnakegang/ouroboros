package kr.co.ouroboros.core.rest.tryit.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.ouroboros.core.rest.tryit.config.TrySessionProperties;
import kr.co.ouroboros.core.rest.tryit.session.TrySessionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TryFilter.
 */
@ExtendWith(MockitoExtension.class)
class TryFilterTest {

    @Mock
    private TrySessionRegistry registry;

    @Mock
    private TrySessionProperties properties;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private TryFilter tryFilter;

    @Test
    void testFilterWithoutTryHeader() throws Exception {
        // No header
        when(request.getHeader("X-Ouroboros-Try")).thenReturn(null);

        tryFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(registry, never()).isValid(any(UUID.class), anyString(), anyBoolean());
    }

    @Test
    void testFilterWithValidTryHeader() throws Exception {
        UUID tryId = UUID.randomUUID();
        when(properties.isBindClientIp()).thenReturn(true);
        when(properties.isOneShot()).thenReturn(false);
        when(request.getHeader("X-Ouroboros-Try")).thenReturn(tryId.toString());
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(registry.isValid(any(UUID.class), anyString(), anyBoolean())).thenReturn(true);

        tryFilter.doFilterInternal(request, response, filterChain);

        verify(registry).isValid(eq(tryId), eq("127.0.0.1"), eq(true));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testFilterWithInvalidTryHeader() throws Exception {
        String invalidHeader = "not-a-uuid";
        when(request.getHeader("X-Ouroboros-Try")).thenReturn(invalidHeader);

        tryFilter.doFilterInternal(request, response, filterChain);

        verify(registry, never()).isValid(any(UUID.class), anyString(), anyBoolean());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testFilterWithExpiredSession() throws Exception {
        UUID tryId = UUID.randomUUID();
        when(properties.isBindClientIp()).thenReturn(true);
        when(request.getHeader("X-Ouroboros-Try")).thenReturn(tryId.toString());
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(registry.isValid(any(UUID.class), anyString(), anyBoolean())).thenReturn(false);
        // isOneShot() is not called when isValid returns false, so don't mock it

        tryFilter.doFilterInternal(request, response, filterChain);

        verify(registry).isValid(eq(tryId), eq("127.0.0.1"), eq(true));
        verify(filterChain).doFilter(request, response);
        // TryContext.clear() is called in finally block but we can't verify static calls
    }
}
