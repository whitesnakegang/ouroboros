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

    /**
     * Applies the OtelContextTaskDecorator to target ThreadPoolTaskExecutor beans before Spring initialization.
     *
     * <p>If the provided bean's name is one of the configured executor names and the bean is a
     * {@code ThreadPoolTaskExecutor}, its TaskDecorator is set to {@code OtelContextTaskDecorator.INSTANCE}
     * and an informational log entry is emitted.</p>
     *
     * @param bean the bean instance being processed
     * @param beanName the name of the bean in the application context
     * @return the original bean instance (potentially modified)
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (TARGET_EXECUTOR_BEAN_NAMES.contains(beanName) && bean instanceof ThreadPoolTaskExecutor threadPool) {
            threadPool.setTaskDecorator(OtelContextTaskDecorator.INSTANCE);
            log.info("Applied OtelContextTaskDecorator to executor bean '{}'", beanName);
        }
        return bean;
    }

    /**
     * No-op BeanPostProcessor callback invoked after a bean's initialization.
     *
     * @param bean the bean instance created by the container
     * @param beanName the name of the bean in the container
     * @return the original bean instance unchanged
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
