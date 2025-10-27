package kr.co.ouroboros.core.rest.tryit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Try 세션 관리를 위한 Spring 설정.
 * TrySessionProperties 활성화 및 스케줄링 활성화.
 */
@Configuration
@EnableConfigurationProperties(kr.co.ouroboros.core.rest.tryit.config.TrySessionProperties.class)
@EnableScheduling
public class TrySessionConfig {
}
