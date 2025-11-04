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
     * Parse method metadata from a TraceSpanInfo, preferring OpenTelemetry attributes.
     *
     * <p>When present, uses OpenTelemetry attributes (`code.namespace`, `code.function`, and
     * `code.parameter.*`) to populate className, methodName, and parameters. If those attributes
     * are absent, falls back to parsing the span name using patterns like
     * "ClassName.methodName" or "ClassName.methodName(Type1, Type2)". HTTP-style span names that
     * start with "http " are treated specially.
     *
     * @param span the TraceSpanInfo to extract method information from
     * @return a SpanMethodInfo containing the extracted className, methodName, and parameters
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
     * Create a SpanMethodInfo from OpenTelemetry "code.*" attributes.
     *
     * <p>Derives the className from the last segment of `code.namespace`, uses
     * `code.function` as the methodName, and builds parameters from
     * `code.parameter.{index}.type` and `code.parameter.{index}.name` pairs.
     *
     * @param span the trace span containing attributes to read
     * @param namespace the value of `code.namespace` (may be null)
     * @param function the value of `code.function` (may be null)
     * @return a SpanMethodInfo populated from the provided attributes (className, methodName, and parameters)
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
     * Parse the span's name to extract a class name, method name, and parameter list.
     *
     * <p>Recognizes common patterns such as "ClassName.methodName", "ClassName.methodName(Type...)".
     * Span names that start with "http " are treated as HTTP spans (className set to "HTTP" and
     * methodName set to the full span name). If the span name is null or cannot be parsed into a
     * class/method form, the result will contain nulls for className/methodName and an empty
     * parameter list.
     *
     * @param span span information whose name will be parsed
     * @return a {@code SpanMethodInfo} containing the extracted className, methodName, and parameters;
     *         fields are {@code null} or empty when the corresponding information is not available
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
     * Parse a parameter declaration string into a SpanMethodInfo.Parameter.
     *
     * <p>Accepts either a bare type (e.g., "String") or a type followed by a name
     * separated by whitespace (e.g., "String userId"). Null or empty input yields
     * a Parameter with both type and name set to empty strings.
     *
     * @param paramStr the parameter string to parse; may be null or empty
     * @return a Parameter whose `type` is the parsed type and whose `name` is the parsed name (empty string if absent)
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
