/**
 * STOMP 기반 실시간 통신에서 Try 기능을 제공하기 위한 모듈.
 * <p>
 * HTTP Try 모듈과 동일한 식별·추적 전략을 STOMP 메시지에도 확장하여,
 * 개발자가 실시간 채널에서도 동일한 Try 경험을 누릴 수 있도록 지원한다.
 * <p>
 * <b>핵심 구성 요소</b>
 * <ul>
 *   <li><b>Identification</b> - STOMP 프레임 헤더를 검사하여 Try 요청을 감지하고 식별자(tryId)를 발급</li>
 *   <li><b>Config</b> - 채널 인터셉터를 메시지 브로커에 등록하여 인바운드/아웃바운드 흐름에 Try 컨텍스트를 적용</li>
 *   <li><b>Infrastructure</b> - 향후 STOMP 전용 저장소·추적 로직을 확장하기 위한 기술적 기반</li>
 * </ul>
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.websocket.tryit;


