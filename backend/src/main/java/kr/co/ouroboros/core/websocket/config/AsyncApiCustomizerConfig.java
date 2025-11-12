package kr.co.ouroboros.core.websocket.config;

import io.github.springwolf.asyncapi.v3.model.Tag;
import io.github.springwolf.asyncapi.v3.model.operation.Operation;
import io.github.springwolf.core.asyncapi.scanners.operations.annotations.OperationCustomizer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.co.ouroboros.core.global.annotation.ApiState;
import org.springframework.stereotype.Component;

@Component
public class AsyncApiCustomizerConfig implements OperationCustomizer {

    @Override
    public void customize(Operation operation, Method method) {
        ApiState annotation = method.getAnnotation(ApiState.class);
        if (annotation == null) return;

        String progressValue;
        switch (annotation.state()) {
            case COMPLETED:     progressValue = "completed";     break;
            case BUGFIX:        progressValue = "bugfix";        break;
            case IMPLEMENTING:  progressValue = "implementing";  break;
            default:            return;
        }

        // Operation 수준의 x- 확장 필드에 직접 기록
        putExt(operation, "x-ouroboros-progress",   progressValue);
    }

    private static void putExt(Operation op, String key, Object value) {
        Map<String, Object> ext = op.getExtensionFields();
        if (ext == null) {
            ext = new LinkedHashMap<>();
            op.setExtensionFields(ext);
        }
        // 이미 값이 있으면 덮어쓰지 않으려면 putIfAbsent 사용
        ext.putIfAbsent(key, value);
    }
}