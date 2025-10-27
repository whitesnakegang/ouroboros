package kr.co.ouroboros.core.rest.tryit.session;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Try 세션 정보를 담는 불변 객체.
 * 각 세션은 고유한 tryId와 TTL, IP 바인딩 정보를 가진다.
 */
public class TrySession {
    private final UUID tryId;
    private final String clientIp;
    private final LocalDateTime expiresAt;
    private boolean used;

    /**
     * 새로운 Try 세션을 생성한다.
     *
     * @param tryId 세션 식별자
     * @param clientIp 클라이언트 IP 주소
     * @param expiresAt 세션 만료 시각
     */
    public TrySession(UUID tryId, String clientIp, LocalDateTime expiresAt) {
        this.tryId = tryId;
        this.clientIp = clientIp;
        this.expiresAt = expiresAt;
        this.used = false;
    }

    /**
     * 세션 식별자를 반환한다.
     *
     * @return tryId
     */
    public UUID getTryId() {
        return tryId;
    }

    /**
     * 등록된 클라이언트 IP 주소를 반환한다.
     *
     * @return 클라이언트 IP
     */
    public String getClientIp() {
        return clientIp;
    }

    /**
     * 세션 만료 시각을 반환한다.
     *
     * @return 만료 시각
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * 세션이 이미 사용되었는지 여부를 반환한다.
     *
     * @return 사용 여부 (oneShot 모드에서 사용됨)
     */
    public boolean isUsed() {
        return used;
    }

    /**
     * 세션을 사용됨으로 표시한다.
     * oneShot 모드에서 한 번만 사용 가능하도록 하기 위함.
     */
    public void markUsed() {
        this.used = true;
    }

    /**
     * 현재 시각 기준으로 세션이 만료되었는지 확인한다.
     *
     * @return 만료 여부
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 전달받은 IP 주소가 세션에 등록된 IP와 일치하는지 확인한다.
     *
     * @param ip 확인할 IP 주소
     * @return 일치 여부
     */
    public boolean matchesIp(String ip) {
        return this.clientIp.equals(ip);
    }
}
