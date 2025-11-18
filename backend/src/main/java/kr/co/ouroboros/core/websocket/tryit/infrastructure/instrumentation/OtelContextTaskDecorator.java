package kr.co.ouroboros.core.websocket.tryit.infrastructure.instrumentation;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.springframework.core.task.TaskDecorator;

/**
 * TaskDecorator for propagating OpenTelemetry Context across asynchronous tasks.
 */
public final class OtelContextTaskDecorator implements TaskDecorator {

    public static final OtelContextTaskDecorator INSTANCE = new OtelContextTaskDecorator();

    /**
     * Prevents external instantiation of this decorator and enforces the in-class singleton.
     */
    private OtelContextTaskDecorator() {
    }

    /**
     * Wraps a Runnable so it executes with the OpenTelemetry Context that was current at decoration time.
     *
     * @param runnable the task to decorate; may be {@code null}
     * @return the decorated Runnable that activates the captured OpenTelemetry Context when executed, or {@code null} if {@code runnable} is {@code null}
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        if (runnable == null) {
            return null;
        }
        Context context = Context.current();
        return () -> {
            try (Scope scope = context.makeCurrent()) {
                runnable.run();
            }
        };
    }
}
