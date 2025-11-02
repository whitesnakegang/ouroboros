package kr.co.ouroboros.core.rest.tryit.trace.parser;

import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanMethodInfo;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parses method information (className, methodName, parameters) from TraceSpanInfo.
 */
@Slf4j
@Component
public class SpanMethodParser {
    
    /**
     * Parses method information from a span, preferring OpenTelemetry attributes.
     * 
     * @param span Span information
     * @return Parsed method information
     */
    public SpanMethodInfo parse(TraceSpanInfo span) {
        if (span == null) {
            return SpanMethodInfo.builder()
                    .className(null)
                    .methodName(null)
                    .parameters(new ArrayList<>())
                    .build();
        }
        
        // Prefer OpenTelemetry attributes from interceptor
        String namespace = span.getAttributes() != null ? span.getAttributes().get("code.namespace") : null;
        String function = span.getAttributes() != null ? span.getAttributes().get("code.function") : null;
        
        if (namespace != null || function != null) {
            return parseFromAttributes(span, namespace, function);
        }
        
        // Fallback to span name parsing
        return parseFromSpanName(span);
    }
    
    /**
     * Parses method info from OpenTelemetry attributes.
     */
    private SpanMethodInfo parseFromAttributes(TraceSpanInfo span, String namespace, String function) {
        String className = null;
        String methodName = null;
        
        if (namespace != null) {
            int lastDot = namespace.lastIndexOf('.');
            className = lastDot >= 0 ? namespace.substring(lastDot + 1) : namespace;
        }
        
        if (function != null) {
            methodName = function;
        }
        
        // Parse parameters
        List<SpanMethodInfo.Parameter> parameters = new ArrayList<>();
        int idx = 0;
        while (true) {
            String type = span.getAttributes().get("code.parameter." + idx + ".type");
            String name = span.getAttributes().get("code.parameter." + idx + ".name");
            if (type == null && name == null) {
                break;
            }
            parameters.add(SpanMethodInfo.Parameter.builder()
                    .type(type != null ? type : "")
                    .name(name != null ? name : "")
                    .build());
            idx++;
        }
        
        return SpanMethodInfo.builder()
                .className(className)
                .methodName(methodName)
                .parameters(parameters)
                .build();
    }
    
    /**
     * Parses method info from span name.
     * Handles patterns like:
     * - "ClassName.methodName"
     * - "ClassName.methodName(ParamType)"
     * - "ClassName.methodName(Type1, Type2)"
     * - "http get /api/users/{id}" (HTTP spans)
     */
    private SpanMethodInfo parseFromSpanName(TraceSpanInfo span) {
        String spanName = span.getName();
        
        if (spanName == null || spanName.isEmpty()) {
            return SpanMethodInfo.builder()
                    .className(null)
                    .methodName(null)
                    .parameters(new ArrayList<>())
                    .build();
        }
        
        // Handle HTTP span names (e.g., "http get /api/users/{id}")
        if (spanName.startsWith("http ")) {
            return SpanMethodInfo.builder()
                    .className("HTTP")
                    .methodName(spanName)
                    .parameters(new ArrayList<>())
                    .build();
        }
        
        // Try to parse class.method() pattern
        int lastDotIndex = spanName.lastIndexOf('.');
        
        if (lastDotIndex > 0 && lastDotIndex < spanName.length() - 1) {
            // Extract class name
            String className = spanName.substring(0, lastDotIndex);
            
            // Extract method part
            String methodPart = spanName.substring(lastDotIndex + 1);
            
            // Check if it contains parentheses (has parameters)
            int openParen = methodPart.indexOf('(');
            
            if (openParen > 0) {
                // Has parameters
                String methodName = methodPart.substring(0, openParen);
                String paramsStr = methodPart.substring(openParen + 1, methodPart.lastIndexOf(')'));
                
                // Parse parameters
                List<SpanMethodInfo.Parameter> parameters = new ArrayList<>();
                if (!paramsStr.isEmpty()) {
                    parameters = Arrays.stream(paramsStr.split(","))
                            .map(String::trim)
                            .map(this::parseParameter)
                            .collect(Collectors.toList());
                }
                
                return SpanMethodInfo.builder()
                        .className(className)
                        .methodName(methodName)
                        .parameters(parameters)
                        .build();
            } else {
                // No parameters
                return SpanMethodInfo.builder()
                        .className(className)
                        .methodName(methodPart)
                        .parameters(new ArrayList<>())
                        .build();
            }
        } else {
            // Can't parse, treat entire name as method name
            return SpanMethodInfo.builder()
                    .className(null)
                    .methodName(spanName)
                    .parameters(new ArrayList<>())
                    .build();
        }
    }
    
    /**
     * Parses a parameter string into Parameter object.
     * Supports formats:
     * - "Type" (only type, no name)
     * - "Type name" (type and name with space)
     * 
     * @param paramStr Parameter string
     * @return Parameter object
     */
    private SpanMethodInfo.Parameter parseParameter(String paramStr) {
        if (paramStr == null || paramStr.isEmpty()) {
            return SpanMethodInfo.Parameter.builder()
                    .type("")
                    .name("")
                    .build();
        }
        
        // Try to parse as "Type name" or just "Type"
        String trimmed = paramStr.trim();
        int spaceIndex = trimmed.lastIndexOf(' ');
        
        if (spaceIndex > 0 && spaceIndex < trimmed.length() - 1) {
            // Has both type and name
            String type = trimmed.substring(0, spaceIndex).trim();
            String name = trimmed.substring(spaceIndex + 1).trim();
            return SpanMethodInfo.Parameter.builder()
                    .type(type)
                    .name(name)
                    .build();
        } else {
            // Only type, no name (like "Long" or "String")
            return SpanMethodInfo.Parameter.builder()
                    .type(trimmed)
                    .name("")  // No name available
                    .build();
        }
    }
}

