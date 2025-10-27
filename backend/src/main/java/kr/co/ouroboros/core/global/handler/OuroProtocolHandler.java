package kr.co.ouroboros.core.global.handler;

import kr.co.ouroboros.core.global.spec.OuroApiSpec;

public interface OuroProtocolHandler {
    /**
     * 핸들러가 처리할 프로토콜의 고유 이름(Key)
     * (예: "rest", "grpc")
     */
    String getProtocol();

    /**
     * 코드(@Annotation)를 스캔하여
     * 현재 상태의 스펙 객체(OuroApiSpec)를 생성
     */
    OuroApiSpec scanCurrentState();

    /**
     * YAML 파일을 파싱하여
     * 저장된(Desired) 스펙 객체(OuroApiSpec)를 생성
     * @param yamlContent .yml 파일의 내용
     */
    OuroApiSpec loadFromFile(String yamlContent);

    /**
     * 두 스펙 객체를 비교하여 불일치 결과 반환
     */
    OuroApiSpec validate(OuroApiSpec fileSpec, OuroApiSpec scannedSpec);

    /**
     * 스펙 객체(주로 scannedSpec)를
     * .yml 파일 포맷(String)으로 변환 (저장용)
     */
    String saveToString(OuroApiSpec specToSave);
}
