package kr.co.ouroboros.core.global.spec;

/**
 * 모든 API 스펙(REST, gRPC, Thrift 등)의
 * 최상위 공통 데이터 인터페이스
 */
public interface OuroApiSpec {
    /**
 * Gets the protocol identifier for this API specification.
 *
 * @return the protocol name (for example, "rest" or "grpc")
 */
    String getProtocol();

    /**
 * Gets the API spec version.
 *
 * @return the version string of the spec (for example, "v1" or "1.0")
 */
    String getVersion();
}