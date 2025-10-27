package kr.co.ouroboros.core.rest.tryit.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Try 세션 생성 API의 응답 DTO.
 */
@Data
public class CreateSessionResponse {
    /** 세션 식별자 (UUID) */
    private final String tryId;
    
    /** HTTP 헤더 이름 */
    private final String headerName;
    
    /** OpenTelemetry Baggage 키 */
    private final String baggageKey;
    
    /** 세션 만료 시각 */
    private final LocalDateTime expiresAt;
}
