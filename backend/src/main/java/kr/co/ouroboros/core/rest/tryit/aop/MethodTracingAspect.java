package kr.co.ouroboros.core.rest.tryit.aop;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import kr.co.ouroboros.core.rest.tryit.util.TryContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Spring AOP Aspect for tracing method calls in Service, RestController, and Repository classes.
 * 
 * This aspect automatically creates OpenTelemetry spans for method calls and adds
 * detailed method metadata as span attributes.
 */
@Aspect
@Slf4j
public class MethodTracingAspect {

    private final Tracer tracer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MethodTracingAspect(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Intercepts method calls in Service, RestController, and Repository classes.
     * Only traces methods when a Try request is active (tryId exists in context).
     */
    @Around("(@within(Service) || @within(RestController) || @within(Repository)) && execution(public * *(..))")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // Only trace if this is a Try request
        if (!TryContext.hasTryId()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        String spanName = className + "." + methodName;

        log.debug("MethodTracingAspect: Intercepting method call: {}", spanName);

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(io.opentelemetry.api.trace.SpanKind.INTERNAL)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Add method metadata as span attributes
            addMethodAttributes(span, joinPoint, signature);
            
            Object result = joinPoint.proceed();
            span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            return result;
        } catch (Throwable throwable) {
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, throwable.getMessage());
            span.recordException(throwable);
            throw throwable;
        } finally {
            span.end();
        }
    }

    /**
     * Adds method metadata as span attributes.
     * 
     * @param span Current span
     * @param joinPoint AOP join point
     * @param signature Method signature
     */
    private void addMethodAttributes(Span span, ProceedingJoinPoint joinPoint, MethodSignature signature) {
        try {
            Method method = signature.getMethod();
            Class<?> declaringClass = method.getDeclaringClass();
            
            // Add class name
            span.setAttribute("method.class", declaringClass.getName());
            
            // Add method name
            span.setAttribute("method.name", method.getName());
            
            // Add parameter information
            Parameter[] parameters = method.getParameters();
            Object[] args = joinPoint.getArgs();
            
            ArrayNode paramsArray = objectMapper.createArrayNode();
            for (int i = 0; i < parameters.length; i++) {
                ObjectNode paramNode = objectMapper.createObjectNode();
                paramNode.put("type", parameters[i].getType().getSimpleName());
                paramNode.put("name", parameters[i].getName());
                if (args[i] != null) {
                    paramNode.put("value", args[i].toString());
                } else {
                    paramNode.putNull("value");
                }
                paramsArray.add(paramNode);
            }
            span.setAttribute("method.parameters", paramsArray.toString());
            
        } catch (Exception e) {
            log.warn("Failed to add method attributes to span: {}", e.getMessage());
        }
    }
}
