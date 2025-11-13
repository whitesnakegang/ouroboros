package kr.co.ouroboros.core.websocket.config;

import io.github.springwolf.asyncapi.v3.model.operation.Operation;
import io.github.springwolf.core.asyncapi.scanners.operations.annotations.OperationCustomizer;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.ouroboros.core.global.annotation.ApiState;
import org.springframework.stereotype.Component;

@Component
public class AsyncApiCustomizerConfig implements OperationCustomizer {

    /**
     * Adds an `x-ouroboros-progress` operation extension based on the method's ApiState when that state is
     * one of COMPLETED, BUGFIX, or IMPLEMENTING.
     *
     * @param operation the OpenAPI/AsyncAPI operation to update with the progress extension
     * @param method the Java reflection Method to inspect for an @ApiState annotation
     */
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

    /**
     * Ensures the operation has an extension-fields map and adds the given extension only if the key is not already present.
     *
     * Initializes the operation's extension fields map when absent and inserts the specified key/value pair using a non-overwriting put.
     *
     * @param op the Operation to update
     * @param key the extension field name to add (e.g. "x-ouroboros-progress")
     * @param value the extension value to set when the key is not already present
     */
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