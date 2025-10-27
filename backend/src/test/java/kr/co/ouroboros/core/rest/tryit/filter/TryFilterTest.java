package kr.co.ouroboros.core.rest.tryit.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.ouroboros.core.rest.tryit.config.TrySessionProperties;
import kr.co.ouroboros.core.rest.tryit.session.TrySessionRegistry;
import kr.co.ouroboros.core.rest.tryit.util.TryContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TryFilterTest {

    @Mock
    TrySessionRegistry registry;

    @Mock
    TrySessionProperties properties;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain filterChain;

    @InjectMocks
    TryFilter filter;

    @Nested
    class HeaderAbsentOrEmpty {

        @Test
        void noHeader_null() throws Exception {
            when(request.getHeader("X-Ouroboros-Try")).thenReturn(null);

            try (MockedStatic<TryContext> ctx = mockStatic(TryContext.class)) {
                filter.doFilterInternal(request, response, filterChain);

                verify(filterChain).doFilter(request, response);
                verifyNoInteractions(registry, properties);
                // finally 블록 검증
                ctx.verify(TryContext::clear, times(1));
                ctx.verifyNoMoreInteractions();
            }
        }

        @Test
        void noHeader_emptyString() throws Exception {
            when(request.getHeader("X-Ouroboros-Try")).thenReturn("");

            try (MockedStatic<TryContext> ctx = mockStatic(TryContext.class)) {
                filter.doFilterInternal(request, response, filterChain);

                verify(filterChain).doFilter(request, response);
                verifyNoInteractions(registry, properties);
                ctx.verify(TryContext::clear, times(1));
                ctx.verifyNoMoreInteractions();
            }
        }
    }

    @Test
    void invalidUuidHeader_gracefullyContinues() throws Exception {
        when(request.getHeader("X-Ouroboros-Try")).thenReturn("not-a-uuid");

        try (MockedStatic<TryContext> ctx = mockStatic(TryContext.class)) {
            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(registry, never()).isValid(any(), anyString(), anyBoolean());
            verify(registry, never()).markUsed(any());
            verifyNoInteractions(properties);
            // setTryId는 호출되지 않아야 함
            ctx.verify(() -> TryContext.setTryId(any()), never());
            ctx.verify(TryContext::clear, times(1));
        }
    }

    @Test
    void validHeader_withRemoteAddr_bindClientIp_true_oneShot_false() throws Exception {
        UUID tryId = UUID.randomUUID();
        when(request.getHeader("X-Ouroboros-Try")).thenReturn(tryId.toString());
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(properties.isBindClientIp()).thenReturn(true);
        when(properties.isOneShot()).thenReturn(false);
        when(registry.isValid(tryEq(tryId), eq("127.0.0.1"), eq(true))).thenReturn(true);

        try (MockedStatic<TryContext> ctx = mockStatic(TryContext.class)) {
            filter.doFilterInternal(request, response, filterChain);

            verify(registry).isValid(tryEq(tryId), eq("127.0.0.1"), eq(true));
            verify(properties).isBindClientIp();
            verify(properties).isOneShot();
            // oneShot=false -> markUsed 호출 안됨
            verify(registry, never()).markUsed(any());

            ctx.verify(() -> TryContext.setTryId(tryId), times(1));
            verify(filterChain).doFilter(request, response);
            ctx.verify(TryContext::clear, times(1));
        }
    }

    @Test
    void validHeader_withXForwardedFor_bindClientIp_false() throws Exception {
        UUID tryId = UUID.randomUUID();
        String forwarded = "203.0.113.10, 70.1.2.3"; // 코드가 첫번째만 추출하지 않고 전체 문자열을 사용함
        when(request.getHeader("X-Ouroboros-Try")).thenReturn(tryId.toString());
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwarded);
        when(properties.isBindClientIp()).thenReturn(false);
        when(properties.isOneShot()).thenReturn(false);
        when(registry.isValid(tryEq(tryId), eq(forwarded), eq(false))).thenReturn(true);

        try (MockedStatic<TryContext> ctx = mockStatic(TryContext.class)) {
            filter.doFilterInternal(request, response, filterChain);

            verify(registry).isValid(tryEq(tryId), eq(forwarded), eq(false));
            verify(registry, never()).markUsed(any());
            ctx.verify(() -> TryContext.setTryId(tryId), times(1));
            verify(filterChain).doFilter(request, response);
            ctx.verify(TryContext::clear, times(1));
        }
    }

    @Test
    void validHeader_sessionInvalid_doesNotSetContextOrMarkUsed() throws Exception {
        UUID tryId = UUID.randomUUID();
        when(request.getHeader("X-Ouroboros-Try")).thenReturn(tryId.toString());
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");
        when(properties.isBindClientIp()).thenReturn(true);
        when(registry.isValid(tryEq(tryId), eq("10.0.0.5"), eq(true))).thenReturn(false);

        try (MockedStatic<TryContext> ctx = mockStatic(TryContext.class)) {
            filter.doFilterInternal(request, response, filterChain);

            verify(registry).isValid(tryEq(tryId), eq("10.0.0.5"), eq(true));
            // isOneShot()은 호출되지 않아야 함 (유효하지 않으므로)
            verify(properties, never()).isOneShot();
            verify(registry, never()).markUsed(any());
            // TryContext.setTryId 호출되지 않음
            ctx.verify(() -> TryContext.setTryId(any()), never());

            verify(filterChain).doFilter(request, response);
            ctx.verify(TryContext::clear, times(1));
        }
    }

    @Test
    void validHeader_oneShot_true_marksUsedOnce() throws Exception {
        UUID tryId = UUID.randomUUID();
        when(request.getHeader("X-Ouroboros-Try")).thenReturn(tryId.toString());
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.0.7");
        when(properties.isBindClientIp()).thenReturn(true);
        when(registry.isValid(tryEq(tryId), eq("192.168.0.7"), eq(true))).thenReturn(true);
        when(properties.isOneShot()).thenReturn(true);

        try (MockedStatic<TryContext> ctx = mockStatic(TryContext.class)) {
            filter.doFilterInternal(request, response, filterChain);

            verify(registry).isValid(tryEq(tryId), eq("192.168.0.7"), eq(true));
            verify(properties).isOneShot();
            verify(registry).markUsed(tryEq(tryId));

            ctx.verify(() -> TryContext.setTryId(tryId), times(1));
            verify(filterChain).doFilter(request, response);
            ctx.verify(TryContext::clear, times(1));
        }
    }

    // 편의: UUID eq helper (Mockito가 toString 비교를 타는 경우를 줄이기 위해)
    private static UUID tryEq(UUID expected) {
        return argThat(actual -> actual != null && actual.equals(expected));
    }
}
