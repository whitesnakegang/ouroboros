package kr.co.ouroboros.core.websocket.tryit.infrastructure.instrumentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Applies OpenTelemetry TaskDecorator to STOMP channel executors.
 * <p>
 * If an executor already has a TaskDecorator, it composes the decorators
 * instead of replacing the existing one to avoid breaking other functionality.
 * <p>
 * This implementation:
 * <ul>
 *   <li>Prefers the public {@code getTaskDecorator()} method (Spring Framework 4.3+)</li>
 *   <li>Falls back to field access for older Spring versions (prior to 4.3)</li>
 *   <li>Handles security restrictions gracefully (SecurityManager/module system)</li>
 * </ul>
 * <p>
 * Security considerations:
 * <ul>
 *   <li>Public API is always preferred when available</li>
 *   <li>Field access is only used as a fallback for backward compatibility</li>
 *   <li>Security exceptions are caught and handled gracefully</li>
 *   <li>In restricted environments where field access fails, the decorator is set anyway
 *       (preserving functionality while potentially overwriting existing decorators)</li>
 * </ul>
 */
@Slf4j
@Component
public class TryChannelExecutorCustomizer implements BeanPostProcessor {

    private static final Set<String> TARGET_EXECUTOR_BEAN_NAMES = Set.of(
            "clientInboundChannelExecutor",
            "clientOutboundChannelExecutor",
            "brokerChannelExecutor"
    );

    // Cache the getTaskDecorator method if available (Spring Framework 4.3+)
    private static final Method GET_TASK_DECORATOR_METHOD = findGetTaskDecoratorMethod();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (TARGET_EXECUTOR_BEAN_NAMES.contains(beanName) && bean instanceof ThreadPoolTaskExecutor threadPool) {
            TaskDecorator existingDecorator = getExistingTaskDecorator(threadPool);
            if (existingDecorator != null) {
                log.warn("Executor bean '{}' already has a TaskDecorator ({}). " +
                        "Composing decorators to preserve existing functionality.",
                        beanName, existingDecorator.getClass().getName());
                // Compose decorators: OtelContextTaskDecorator wraps the existing decorator
                // The composed decorator sets OpenTelemetry context first, then executes the existing decorator's wrapped runnable
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
     * Finds the public getTaskDecorator() method if available in the Spring Framework version.
     * <p>
     * This method is available in Spring Framework 4.3+ as part of the public API.
     * If not available, returns null and we'll use field access as a fallback.
     *
     * @return the Method object for getTaskDecorator(), or null if not available
     */
    private static Method findGetTaskDecoratorMethod() {
        try {
            return ThreadPoolTaskExecutor.class.getMethod("getTaskDecorator");
        } catch (NoSuchMethodException e) {
            // Method not available in this Spring Framework version
            return null;
        }
    }

    /**
     * Retrieves the existing TaskDecorator from a ThreadPoolTaskExecutor.
     * <p>
     * This method tries multiple approaches in order of preference:
     * <ol>
     *   <li>Uses the public {@code getTaskDecorator()} method (Spring Framework 4.3+)</li>
     *   <li>Falls back to accessing the {@code taskDecorator} field directly (for older Spring versions)</li>
     * </ol>
     * <p>
     * The field access fallback is necessary for Spring Framework versions prior to 4.3
     * where the public getter method is not available. Field access is attempted only when
     * the public API is unavailable, and exceptions are caught and logged at trace level.
     * <p>
     * Security considerations:
     * <ul>
     *   <li>Public API is preferred and always used when available (Spring 4.3+)</li>
     *   <li>Field access is only used as a fallback for older Spring versions</li>
     *   <li>Field access failures are gracefully handled without breaking functionality</li>
     *   <li>In restricted environments (SecurityManager/modules), field access may fail,
     *       but this is acceptable as the primary goal is to add OpenTelemetry context propagation</li>
     * </ul>
     *
     * @param executor the ThreadPoolTaskExecutor to check
     * @return the existing TaskDecorator if present and accessible, or null if not found or not accessible
     */
    private TaskDecorator getExistingTaskDecorator(ThreadPoolTaskExecutor executor) {
        // Try 1: Use public API (Spring Framework 4.3+)
        if (GET_TASK_DECORATOR_METHOD != null) {
            try {
                TaskDecorator decorator = (TaskDecorator) GET_TASK_DECORATOR_METHOD.invoke(executor);
                return decorator;
            } catch (Exception e) {
                log.trace("Could not retrieve existing TaskDecorator using public API: {}", e.getMessage());
                // Fall through to field access
            }
        }

        // Try 2: Fallback to field access (for Spring Framework < 4.3)
        // This is necessary to support older Spring versions where getTaskDecorator() doesn't exist
        Field field = findTaskDecoratorField(ThreadPoolTaskExecutor.class);
        if (field == null) {
            log.trace("taskDecorator field not found on {} or its superclasses", ThreadPoolTaskExecutor.class.getName());
            return null;
        }
        // Attempt to make accessible - may fail in SecurityManager/module environments
        try {
            field.setAccessible(true);
        } catch (SecurityException e) {
            // In restricted environments, we can't access the field
            // This is acceptable - we'll set the decorator anyway
            log.trace("Cannot access taskDecorator field due to security restrictions: {}", e.getMessage());
            return null;
        }
        try {
            Object value = field.get(executor);
            if (value instanceof TaskDecorator decorator) {
                return decorator;
            }
        } catch (Exception e) {
            // Any other exception (IllegalAccessException, etc.)
            log.trace("Could not access taskDecorator field: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Finds the taskDecorator field by traversing the class hierarchy.
     * <p>
     * The taskDecorator field is declared in ExecutorConfigurationSupport,
     * which is a superclass of ThreadPoolTaskExecutor. This method searches
     * up the class hierarchy to find the field.
     *
     * @param type the class to start searching from
     * @return the Field object for taskDecorator, or null if not found
     */
    private Field findTaskDecoratorField(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField("taskDecorator");
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Composes two TaskDecorators into a single decorator.
     * <p>
     * The composition applies decorators in the following order:
     * <ol>
     *   <li>The first decorator decorates the original runnable</li>
     *   <li>The second decorator decorates the result from the first decorator</li>
     * </ol>
     * <p>
     * When the composed Runnable is executed, the second decorator's wrapper runs first
     * (as the outer wrapper), then the first decorator's wrapped runnable runs inside it.
     * <p>
     * Example: When composing (existingDecorator, OtelContextTaskDecorator):
     * <ul>
     *   <li>OtelContextTaskDecorator becomes the outer wrapper (sets OpenTelemetry context)</li>
     *   <li>existingDecorator's wrapped runnable runs inside the context</li>
     * </ul>
     *
     * @param first  the first decorator to apply (runs inside the second decorator's wrapper)
     * @param second the second decorator to apply (becomes the outer wrapper)
     * @return a composed TaskDecorator where second wraps first
     */
    private TaskDecorator composeDecorators(TaskDecorator first, TaskDecorator second) {
        return runnable -> second.decorate(first.decorate(runnable));
    }
}

