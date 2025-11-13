package kr.co.ouroboros.core.websocket.tryit.infrastructure.instrumentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Applies OpenTelemetry TaskDecorator to STOMP channel executors.
 * <p>
 * If an executor already has a TaskDecorator, it composes the decorators
 * instead of replacing the existing one to avoid breaking other functionality.
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
            TaskDecorator existingDecorator = getExistingTaskDecorator(threadPool);
            if (existingDecorator != null) {
                log.warn("Executor bean '{}' already has a TaskDecorator ({}). " +
                        "Composing decorators to preserve existing functionality.",
                        beanName, existingDecorator.getClass().getName());
                // Compose decorators: existing decorator runs first, then OtelContextTaskDecorator
                threadPool.setTaskDecorator(composeDecorators(existingDecorator, OtelContextTaskDecorator.INSTANCE));
                log.info("Composed OtelContextTaskDecorator with existing decorator for executor bean '{}'", beanName);
            } else {
                threadPool.setTaskDecorator(OtelContextTaskDecorator.INSTANCE);
                log.info("Applied OtelContextTaskDecorator to executor bean '{}'", beanName);
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * Attempts to retrieve the existing TaskDecorator from a ThreadPoolTaskExecutor.
     * <p>
     * Uses reflection to safely check if a TaskDecorator is already set, as
     * ThreadPoolTaskExecutor may not expose a public getter method in all Spring versions.
     * <p>
     * This method tries multiple approaches:
     * <ol>
     *   <li>First, attempts to call getTaskDecorator() method if it exists</li>
     *   <li>If method doesn't exist, tries to access the taskDecorator field directly</li>
     *   <li>If field access fails, searches all fields for a TaskDecorator type</li>
     * </ol>
     *
     * @param executor the ThreadPoolTaskExecutor to check
     * @return the existing TaskDecorator if present, or null if not found or not accessible
     */
    private TaskDecorator getExistingTaskDecorator(ThreadPoolTaskExecutor executor) {
        // Try 1: Check if getTaskDecorator() method exists
        try {
            Method getTaskDecorator = ThreadPoolTaskExecutor.class.getMethod("getTaskDecorator");
            TaskDecorator decorator = (TaskDecorator) getTaskDecorator.invoke(executor);
            if (decorator != null) {
                return decorator;
            }
        } catch (NoSuchMethodException e) {
            // Method doesn't exist, continue to field access
        } catch (Exception e) {
            log.trace("Could not invoke getTaskDecorator() method: {}", e.getMessage());
        }

        // Try 2: Direct field access with common field names
        String[] possibleFieldNames = {"taskDecorator", "decorator"};
        for (String fieldName : possibleFieldNames) {
            try {
                var field = ThreadPoolTaskExecutor.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(executor);
                if (value instanceof TaskDecorator decorator) {
                    return decorator;
                }
            } catch (NoSuchFieldException e) {
                // Field doesn't exist with this name, try next
            } catch (Exception e) {
                log.trace("Could not access field '{}': {}", fieldName, e.getMessage());
            }
        }

        // Try 3: Search all fields for TaskDecorator type
        try {
            var fields = ThreadPoolTaskExecutor.class.getDeclaredFields();
            for (var field : fields) {
                if (TaskDecorator.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(executor);
                    if (value instanceof TaskDecorator decorator) {
                        return decorator;
                    }
                }
            }
        } catch (Exception e) {
            log.trace("Could not search fields for TaskDecorator: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Composes two TaskDecorators into a single decorator.
     * <p>
     * The first decorator is applied first, then the second decorator.
     * This ensures both decorators are executed in sequence.
     *
     * @param first  the first decorator to apply
     * @param second the second decorator to apply
     * @return a composed TaskDecorator that applies both decorators in sequence
     */
    private TaskDecorator composeDecorators(TaskDecorator first, TaskDecorator second) {
        return runnable -> second.decorate(first.decorate(runnable));
    }
}

