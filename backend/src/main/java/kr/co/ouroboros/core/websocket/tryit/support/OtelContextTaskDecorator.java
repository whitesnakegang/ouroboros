package kr.co.ouroboros.core.websocket.tryit.support;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.springframework.core.task.TaskDecorator;

/**
 * OpenTelemetry Context를 비동기 작업 실행 시에도 전파하는 TaskDecorator.
 */
public final class OtelContextTaskDecorator implements TaskDecorator {

    public static final OtelContextTaskDecorator INSTANCE = new OtelContextTaskDecorator();

    private OtelContextTaskDecorator() {
    }

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

