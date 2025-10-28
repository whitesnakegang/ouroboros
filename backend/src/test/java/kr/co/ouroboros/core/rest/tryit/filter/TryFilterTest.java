package kr.co.ouroboros.core.rest.tryit.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
                verify(response, never()).setHeader(anyString(), anyString());
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
                verify(response, never()).setHeader(anyString(), anyString());
                ctx.verify(TryContext::clear, times(1));
                ctx.verifyNoMoreInteractions();
            }
        }

        @Test
        void headerNotOn() throws Exception {
            when(request.getHeader("X-Ouroboros-Try")).thenReturn("off");

            try (MockedStatic<TryContext> ctx = mockStatic(TryContext.class)) {
                filter.doFilterInternal(request, response, filterChain);

                verify(filterChain).doFilter(request, response);
                verify(response, never()).setHeader(anyString(), anyString());
                ctx.verify(TryContext::clear, times(1));
                ctx.verifyNoMoreInteractions();
            }
        }
    }

    @Nested
    class ValidTryRequests {
        
        @Test
        void lowercase_on() throws Exception {
            when(request.getHeader("X-Ouroboros-Try")).thenReturn("on");

            try (MockedStatic<TryContext> ctx = mockStatic(TryContext.class)) {
                filter.doFilterInternal(request, response, filterChain);

                verify(filterChain).doFilter(request, response);
                ctx.verify(() -> TryContext.setTryId(any(UUID.class)), times(1));
                verify(response).setHeader(eq("X-Ouroboros-Try-Id"), anyString());
                ctx.verify(TryContext::clear, times(1));
            }
        }

        @Test
        void uppercase_ON() throws Exception {
            when(request.getHeader("X-Ouroboros-Try")).thenReturn("ON");

            try (MockedStatic<TryContext> ctx = mockStatic(TryContext.class)) {
                filter.doFilterInternal(request, response, filterChain);

                verify(filterChain).doFilter(request, response);
                ctx.verify(() -> TryContext.setTryId(any(UUID.class)), times(1));
                verify(response).setHeader(eq("X-Ouroboros-Try-Id"), anyString());
                ctx.verify(TryContext::clear, times(1));
            }
        }

        @Test
        void mixedCase_On() throws Exception {
            when(request.getHeader("X-Ouroboros-Try")).thenReturn("On");

            try (MockedStatic<TryContext> ctx = mockStatic(TryContext.class)) {
                filter.doFilterInternal(request, response, filterChain);

                verify(filterChain).doFilter(request, response);
                ctx.verify(() -> TryContext.setTryId(any(UUID.class)), times(1));
                verify(response).setHeader(eq("X-Ouroboros-Try-Id"), anyString());
                ctx.verify(TryContext::clear, times(1));
            }
        }
    }
}
