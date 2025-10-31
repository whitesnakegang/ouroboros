package kr.co.ouroboros.core.rest.tryit.identification;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.ouroboros.core.rest.tryit.context.TryContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
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

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(response);
        }

        @Test
        void noHeader_emptyString() throws Exception {
            when(request.getHeader("X-Ouroboros-Try")).thenReturn("");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(response);
        }

        @Test
        void headerNotOn() throws Exception {
            when(request.getHeader("X-Ouroboros-Try")).thenReturn("off");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(response);
        }
    }

    @Nested
    class ValidTryRequests {
        
        @Test
        void lowercase_on() throws Exception {
            when(request.getHeader("X-Ouroboros-Try")).thenReturn("on");

            try (MockedStatic<TryContext> ctx = mockStatic(TryContext.class)) {
                filter.doFilterInternal(request, response, filterChain);

                // Verify that filterChain.doFilter is called with original response
                verify(filterChain).doFilter(request, response);
                ctx.verify(() -> TryContext.setTryId(any(UUID.class)), times(1));
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
                ctx.verify(TryContext::clear, times(1));
            }
        }
    }
}
