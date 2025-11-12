package kr.co.ouroboros.core.websocket.tryit.infrastructure.instrumentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Applies OpenTelemetry TaskDecorator to STOMP channel executors.
 */
@Slf4j
@Component
public class TryChannelExecutorCustomizer implements BeanPostProcessor {

    private static final Set<String> TARGET_EXECUTOR_BEAN_NAMES = Set.of(
            "clientInboundChannelExecutor",
            "clientOutboundChannelExecutor",
            "brokerChannelExecutor"
    );

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (TARGET_EXECUTOR_BEAN_NAMES.contains(beanName) && bean instanceof ThreadPoolTaskExecutor threadPool) {
            threadPool.setTaskDecorator(OtelContextTaskDecorator.INSTANCE);
            log.info("Applied OtelContextTaskDecorator to executor bean '{}'", beanName);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}

