package kr.co.ouroboros.core.rest.tryit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Try 세션 관리에 대한 설정 프로퍼티.
 * application.properties에서 'ouroboros.try-session.*' 접두사로 설정한다.
 */
@Data
@ConfigurationProperties(prefix = "ouroboros.try-session")
public class TrySessionProperties {
    
    /**
     * 세션 TTL (초). 기본값 120초.
     */
    private int ttlSeconds = 120;
    
    /**
     * 클라이언트 IP 바인딩 활성화 여부. 기본값 true.
     * true이면 발급한 IP와 다른 IP에서 사용 시 무효화된다.
     */
    private boolean bindClientIp = true;
    
    /**
     * 일회성 세션 활성화 여부. 기본값 false.
     * true이면 한 번 사용 후 즉시 무효화된다.
     */
    private boolean oneShot = false;
    
    /**
     * 최대 동시 활성 세션 수. 기본값 50.
     * 이 값을 초과하면 429 Too Many Requests를 반환한다.
     */
    private int maxActive = 50;
}
