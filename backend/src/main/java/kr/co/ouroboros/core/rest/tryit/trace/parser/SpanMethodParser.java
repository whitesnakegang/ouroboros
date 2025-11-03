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
 * <p>
 * This component extracts method information from span data, preferring
 * OpenTelemetry attributes when available, with fallback to span name parsing.
 * <p>
 * <b>Parsing Strategy:</b>
 * <ol>
 *   <li>Prefer OpenTelemetry attributes (code.namespace, code.function, code.parameter.*)</li>
 *   <li>Fallback to span name parsing (ClassName.methodName or ClassName.methodName(Type1, Type2))</li>
 *   <li>Handle HTTP spans with special formatting</li>
 * </ol>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@Component
public class SpanMethodParser {
    
    /**
     * Parses method information from a span, preferring OpenTelemetry attributes.
     * <p>
     * First attempts to extract method information from OpenTelemetry attributes:
     * <ul>
     *   <li>code.namespace → className</li>
     *   <li>code.function → methodName</li>
     *   <li>code.parameter.* → parameters</li>
     * </ul>
     * <p>
     * If attributes are not available, falls back to parsing span name
     * using patterns like "ClassName.methodName" or "ClassName.methodName(Type1, Type2)".
     *
     * @param span Span information containing method data
     * @return Parsed method information with className, methodName, and parameters
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
     * Parses method information from OpenTelemetry attributes.
     * <p>
     * Extracts method information from OpenTelemetry standard attributes:
     * <ul>
     *   <li>code.namespace → extracts className (last segment after dot)</li>
     *   <li>code.function → methodName</li>
     *   <li>code.parameter.{index}.type → parameter types</li>
     *   <li>code.parameter.{index}.name → parameter names</li>
     * </ul>
     *
     * @param span Span information containing attributes
     * @param namespace Namespace attribute (code.namespace)
     * @param function Function attribute (code.function)
     * @return Parsed method information from attributes
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
     * Parses method information from span name.
     * <p>
     * Handles various span name patterns:
     * <ul>
     *   <li>"ClassName.methodName" → className and methodName</li>
     *   <li>"ClassName.methodName(ParamType)" → with single parameter</li>
     *   <li>"ClassName.methodName(Type1, Type2)" → with multiple parameters</li>
     *   <li>"http get /api/users/{id}" → HTTP spans (special handling)</li>
     * </ul>
     *
     * @param span Span information containing name
     * @return Parsed method information from span name
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
     * <p>
     * Supports various parameter string formats:
     * <ul>
     *   <li>"Type" → only type, no name (e.g., "String", "Long")</li>
     *   <li>"Type name" → type and name separated by space (e.g., "String userId")</li>
     * </ul>
     *
     * @param paramStr Parameter string to parse
     * @return Parameter object with type and name
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

