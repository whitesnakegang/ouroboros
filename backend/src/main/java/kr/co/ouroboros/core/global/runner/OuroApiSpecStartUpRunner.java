package kr.co.ouroboros.core.global.runner;

import java.util.List;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.manager.OuroApiSpecManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class OuroApiSpecStartUpRunner implements ApplicationRunner {
    private final OuroApiSpecManager specManager;
    private final List<OuroProtocolHandler> handlers;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // (Job 3-11)
        // 등록된 모든 프로토콜 핸들러를 순회하며
        // 매니저에게 초기화 작업을 위임
        for (OuroProtocolHandler handler : handlers) {
            try {
                specManager.initializeProtocolOnStartup(handler.getProtocol());
            } catch (Exception e) {
                // 특정 프로토콜 초기화에 실패해도 서버가 죽지 않도록 처리
                log.error("[{}] 프로토콜 스펙 초기화 실패: {}", handler.getProtocol(), e.getMessage(), e);
            }
        }
    }
}
