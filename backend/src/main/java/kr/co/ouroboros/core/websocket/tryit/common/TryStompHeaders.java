package kr.co.ouroboros.core.websocket.tryit.common;

/**
 * STOMP Try 기능에서 사용하는 헤더와 속성 이름을 정의한다.
 */
public final class TryStompHeaders {

    private TryStompHeaders() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Try 요청 여부를 나타내는 헤더 이름.
     */
    public static final String TRY_HEADER = "X-Ouroboros-Try";

    /**
     * Try 식별자를 전달하는 헤더 이름.
     */
    public static final String TRY_ID_HEADER = "X-Ouroboros-Try-Id";

    /**
     * STOMP 세션에 저장하는 tryId 속성 이름.
     */
    public static final String SESSION_TRY_ID_ATTR = "kr.co.ouroboros.tryId";

    /**
     * 인터셉터 내부에서 Scope 관리를 위해 사용하는 헤더 이름.
     */
    public static final String INTERNAL_SCOPE_HEADER = "kr.co.ouroboros.tryScope";

    /**
     * 아웃바운드 인터셉터에서 Scope 관리를 위해 사용하는 헤더 이름.
     */
    public static final String INTERNAL_OUTBOUND_SCOPE_HEADER = "kr.co.ouroboros.tryOutboundScope";

    /**
     * Try 헤더가 활성화되었음을 나타내는 값.
     */
    public static final String TRY_HEADER_ENABLED_VALUE = "on";
}


