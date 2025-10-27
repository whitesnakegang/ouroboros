package kr.co.ouroboros.core.global.spec;

/**
 * 모든 API 스펙(REST, gRPC, Thrift 등)의
 * 최상위 공통 데이터 인터페이스
 */
public interface OuroApiSpec {
    /**
     * 이 스펙이 어떤 프로토콜인지 반환 (예: "rest", "grpc")
     */
    String getProtocol();

    /**
     * 스펙의 버전을 반환
     */
    String getVersion();
}
