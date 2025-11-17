package kr.co.ouroboros.core.rest.handler.helper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.co.ouroboros.core.rest.common.dto.Components;
import kr.co.ouroboros.core.rest.common.dto.MediaType;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.Parameter;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import kr.co.ouroboros.core.rest.common.dto.RequestBody;
import kr.co.ouroboros.core.rest.common.dto.Response;
import kr.co.ouroboros.core.rest.common.dto.Schema;
import kr.co.ouroboros.core.rest.handler.comparator.SchemaComparator;
import lombok.extern.slf4j.Slf4j;

import static kr.co.ouroboros.core.rest.handler.helper.RequestDiffHelper.*;

/**
 * Helper class for comparing and synchronizing endpoint differences between file and scanned API
 * specifications.
 */
@Slf4j
public final class EndpointDiffHelper {

    /**
     * Prevents instantiation of this utility class.
     */
    private EndpointDiffHelper() {
    }


    /**
     * Detects whether a URL is missing from the file-based API spec and, if missing, inserts and tags the scanned path item.
     *
     * @param url the endpoint path to check (e.g., "/pets")
     * @param pathsFile map of path strings to PathItem from the file-based specification; may be modified to add the scanned path
     * @param pathsScanned map of path strings to PathItem from the scanned specification used to populate missing entries
     * @return {@code true} if the URL was not present in {@code pathsFile}, was added from {@code pathsScanned}, and its operations were marked with the diff tag; {@code false} if the URL already existed in {@code pathsFile}
     */
    public static boolean isDiffUrl(String url, Map<String, PathItem> pathsFile, Map<String, PathItem> pathsScanned, Map<String, SchemaComparator.TypeCnts> fileSchemaMap, OuroRestApiSpec fileSpec, OuroRestApiSpec scanSpec) {
        // url에 해당하는 ENDPOINT가 명세에 있는 경우
        if (pathsFile.get(url) != null) {
            log.info("URL : {} 존재", url);
            return false;
        }

        PathItem scannedPath = pathsScanned.get(url);
        pathsFile.put(url, scannedPath);

        PathItem addedPath = pathsFile.get(url);
        log.info("URL : {} 존재하지 않는 경우", url);

        // 메서드를 기준으로 순회
        for (RequestDiffHelper.HttpMethod method : RequestDiffHelper.HttpMethod.values()) {
            Operation op = getOperationByMethod(addedPath, method);
            if (op != null) {
                // Generate x-ouroboros-id if not present
                if (op.getXOuroborosId() == null) {
                    op.setXOuroborosId(UUID.randomUUID().toString());
                    log.debug("Generated x-ouroboros-id for {} {}: {}", method, url, op.getXOuroborosId());
                }
                // Normalize tags to uppercase
                if (op.getTags() != null) {
                    op.setTags(kr.co.ouroboros.core.global.spec.SpecValidationUtil.normalizeRestTags(op.getTags()));
                }
                op.setXOuroborosDiff("endpoint");
                op.setXOuroborosTag("none");
                
                // Operation과 연관된 모든 Schema를 파일 명세에 추가
                addMissingSchemasFromOperation(op, fileSpec, scanSpec, fileSchemaMap);

                // Note: security is preserved from scannedPath (will be empty from annotation scan)
            }
        }

        return true;
    }

    /**
     * Operation에서 사용하는 모든 $ref Schema를 재귀적으로 추출하여 파일 명세에 추가합니다.
     * 
     * @param operation 스캔된 Operation
     * @param fileSpec 파일 명세 (추가 대상)
     * @param scanSpec 스캔된 명세 (Schema 소스)
     * @param fileSchemaMap 파일에 이미 존재하는 Schema 맵 (key가 Schema 이름)
     */
    private static void addMissingSchemasFromOperation(
            Operation operation,
            OuroRestApiSpec fileSpec,
            OuroRestApiSpec scanSpec,
            Map<String, SchemaComparator.TypeCnts> fileSchemaMap) {
        
        if (scanSpec == null || scanSpec.getComponents() == null || 
            scanSpec.getComponents().getSchemas() == null) {
            return;
        }
        
        // 파일 명세의 components가 없으면 생성
        if (fileSpec.getComponents() == null) {
            fileSpec.setComponents(new Components());
        }
        if (fileSpec.getComponents().getSchemas() == null) {
            fileSpec.getComponents().setSchemas(new java.util.LinkedHashMap<>());
        }
        
        // Operation에서 사용하는 모든 Schema 참조를 수집
        Set<String> referencedSchemaNames = new HashSet<>();
        collectSchemaReferencesFromOperation(operation, scanSpec.getComponents(), referencedSchemaNames);
        
        // 파일에 없는 Schema만 추가
        for (String schemaName : referencedSchemaNames) {
            if (!fileSchemaMap.containsKey(schemaName)) {
                Schema schemaToAdd = scanSpec.getComponents().getSchemas().get(schemaName);
                if (schemaToAdd != null) {
                    // 재귀적으로 참조하는 모든 Schema도 함께 추가
                    addSchemaRecursively(schemaName, schemaToAdd, fileSpec, scanSpec, fileSchemaMap, new HashSet<>());
                    log.debug("Added missing schema '{}' to file spec from scanned operation", schemaName);
                }
            }
        }
    }

    /**
     * Operation에서 사용하는 모든 Schema 참조를 재귀적으로 수집합니다.
     * 
     * @param operation Operation 객체
     * @param scanComponents 스캔된 Components (Schema 해석용)
     * @param collectedNames 수집된 Schema 이름 Set
     */
    private static void collectSchemaReferencesFromOperation(
            Operation operation,
            Components scanComponents,
            Set<String> collectedNames) {
        
        // Parameters에서 Schema 참조 수집
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                if (param.getSchema() != null) {
                    collectSchemaReferencesFromSchema(param.getSchema(), scanComponents, collectedNames, new HashSet<>());
                }
            }
        }
        
        // RequestBody에서 Schema 참조 수집
        if (operation.getRequestBody() != null) {
            collectSchemaReferencesFromRequestBody(operation.getRequestBody(), scanComponents, collectedNames);
        }
        
        // Responses에서 Schema 참조 수집
        if (operation.getResponses() != null) {
            for (Response response : operation.getResponses().values()) {
                collectSchemaReferencesFromResponse(response, scanComponents, collectedNames);
            }
        }
    }

    /**
     * RequestBody에서 Schema 참조를 수집합니다.
     */
    private static void collectSchemaReferencesFromRequestBody(
            RequestBody requestBody,
            Components scanComponents,
            Set<String> collectedNames) {
        
        if (requestBody.getContent() == null) {
            return;
        }
        
        for (MediaType mediaType : requestBody.getContent().values()) {
            if (mediaType.getSchema() != null) {
                collectSchemaReferencesFromSchema(mediaType.getSchema(), scanComponents, collectedNames, new HashSet<>());
            }
        }
    }

    /**
     * Response에서 Schema 참조를 수집합니다.
     */
    private static void collectSchemaReferencesFromResponse(
            Response response,
            Components scanComponents,
            Set<String> collectedNames) {
        
        if (response.getContent() == null) {
            return;
        }
        
        for (MediaType mediaType : response.getContent().values()) {
            if (mediaType.getSchema() != null) {
                collectSchemaReferencesFromSchema(mediaType.getSchema(), scanComponents, collectedNames, new HashSet<>());
            }
        }
    }

    /**
     * Schema에서 $ref를 재귀적으로 추출합니다.
     * 
     * @param schema Schema 객체
     * @param scanComponents 스캔된 Components (참조 해석용)
     * @param collectedNames 수집된 Schema 이름 Set
     * @param visited 순환 참조 방지를 위한 방문한 Schema 이름 Set
     */
    private static void collectSchemaReferencesFromSchema(
            Schema schema,
            Components scanComponents,
            Set<String> collectedNames,
            Set<String> visited) {
        
        if (schema == null) {
            return;
        }
        
        // $ref 처리
        if (schema.getRef() != null) {
            String schemaName = extractSchemaNameFromRef(schema.getRef());
            if (schemaName != null && !visited.contains(schemaName)) {
                collectedNames.add(schemaName);
                visited.add(schemaName);
                
                // 참조된 Schema의 내부도 재귀적으로 탐색
                Schema referencedSchema = getSchemaByName(schemaName, scanComponents);
                if (referencedSchema != null) {
                    collectSchemaReferencesFromSchema(referencedSchema, scanComponents, collectedNames, visited);
                }
                visited.remove(schemaName);
            }
            return; // $ref가 있으면 다른 필드는 무시
        }
        
        // Properties에서 재귀적으로 수집
        if (schema.getProperties() != null) {
            for (Schema propertySchema : schema.getProperties().values()) {
                collectSchemaReferencesFromSchema(propertySchema, scanComponents, collectedNames, visited);
            }
        }
        
        // Items에서 재귀적으로 수집 (배열 타입)
        if (schema.getItems() != null) {
            collectSchemaReferencesFromSchema(schema.getItems(), scanComponents, collectedNames, visited);
        }
    }

    /**
     * Schema를 재귀적으로 파일 명세에 추가합니다.
     * 
     * @param schemaName 추가할 Schema 이름
     * @param schema 추가할 Schema 객체
     * @param fileSpec 파일 명세
     * @param scanSpec 스캔된 명세
     * @param fileSchemaMap 파일에 이미 존재하는 Schema 맵
     * @param addedNames 이미 추가한 Schema 이름 Set (순환 참조 방지)
     */
    private static void addSchemaRecursively(
            String schemaName,
            Schema schema,
            OuroRestApiSpec fileSpec,
            OuroRestApiSpec scanSpec,
            Map<String, SchemaComparator.TypeCnts> fileSchemaMap,
            Set<String> addedNames) {
        
        if (schemaName == null || schema == null || addedNames.contains(schemaName)) {
            return; // 순환 참조 방지
        }
        
        // 이미 파일에 존재하면 추가하지 않음
        if (fileSchemaMap.containsKey(schemaName)) {
            return;
        }
        
        // 이미 추가한 경우 스킵
        if (fileSpec.getComponents().getSchemas().containsKey(schemaName)) {
            return;
        }
        
        addedNames.add(schemaName);
        
        // Schema 복사 (깊은 복사 필요)
        Schema schemaCopy = copySchema(schema);
        
        // $ref가 있으면 참조된 Schema도 추가
        if (schemaCopy.getRef() != null) {
            String referencedSchemaName = extractSchemaNameFromRef(schemaCopy.getRef());
            if (referencedSchemaName != null && scanSpec.getComponents() != null &&
                scanSpec.getComponents().getSchemas() != null) {
                Schema referencedSchema = scanSpec.getComponents().getSchemas().get(referencedSchemaName);
                if (referencedSchema != null) {
                    addSchemaRecursively(referencedSchemaName, referencedSchema, fileSpec, scanSpec, 
                                       fileSchemaMap, addedNames);
                }
            }
        }
        
        // Properties에서 재귀적으로 추가
        if (schemaCopy.getProperties() != null) {
            for (Map.Entry<String, Schema> entry : schemaCopy.getProperties().entrySet()) {
                Schema propertySchema = entry.getValue();
                if (propertySchema.getRef() != null) {
                    String refSchemaName = extractSchemaNameFromRef(propertySchema.getRef());
                    if (refSchemaName != null && scanSpec.getComponents() != null &&
                        scanSpec.getComponents().getSchemas() != null) {
                        Schema refSchema = scanSpec.getComponents().getSchemas().get(refSchemaName);
                        if (refSchema != null) {
                            addSchemaRecursively(refSchemaName, refSchema, fileSpec, scanSpec, 
                                               fileSchemaMap, addedNames);
                        }
                    }
                }
            }
        }
        
        // Items에서 재귀적으로 추가
        if (schemaCopy.getItems() != null) {
            Schema itemsSchema = schemaCopy.getItems();
            if (itemsSchema.getRef() != null) {
                String refSchemaName = extractSchemaNameFromRef(itemsSchema.getRef());
                if (refSchemaName != null && scanSpec.getComponents() != null &&
                    scanSpec.getComponents().getSchemas() != null) {
                    Schema refSchema = scanSpec.getComponents().getSchemas().get(refSchemaName);
                    if (refSchema != null) {
                        addSchemaRecursively(refSchemaName, refSchema, fileSpec, scanSpec, 
                                           fileSchemaMap, addedNames);
                    }
                }
            }
        }
        
        // Schema를 파일 명세에 추가
        fileSpec.getComponents().getSchemas().put(schemaName, schemaCopy);
        log.debug("Added schema '{}' to file spec components", schemaName);
        
        addedNames.remove(schemaName);
    }

    /**
     * Schema를 깊은 복사합니다.
     */
    private static Schema copySchema(Schema source) {
        if (source == null) {
            return null;
        }
        
        Schema copy = new Schema();
        copy.setType(source.getType());
        copy.setDescription(source.getDescription());
        copy.setRef(source.getRef());
        copy.setFormat(source.getFormat());
        copy.setRequired(source.getRequired());
        copy.setMinItems(source.getMinItems());
        copy.setMaxItems(source.getMaxItems());
        copy.setXOuroborosMock(source.getXOuroborosMock());
        copy.setXOuroborosOrders(source.getXOuroborosOrders());
        
        // Properties 복사
        if (source.getProperties() != null) {
            Map<String, Schema> propertiesCopy = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, Schema> entry : source.getProperties().entrySet()) {
                propertiesCopy.put(entry.getKey(), copySchema(entry.getValue()));
            }
            copy.setProperties(propertiesCopy);
        }
        
        // Items 복사
        if (source.getItems() != null) {
            copy.setItems(copySchema(source.getItems()));
        }
        
        // Xml 복사
        if (source.getXml() != null) {
            copy.setXml(source.getXml());
        }
        
        return copy;
    }

    /**
     * $ref 문자열에서 Schema 이름을 추출합니다.
     * 
     * @param ref $ref 문자열 (예: "#/components/schemas/User")
     * @return Schema 이름 (예: "User"), 추출 실패 시 null
     */
    private static String extractSchemaNameFromRef(String ref) {
        if (ref == null || !ref.startsWith("#/components/schemas/")) {
            return null;
        }
        return ref.substring("#/components/schemas/".length());
    }

    /**
     * Components에서 Schema 이름으로 Schema를 가져옵니다.
     */
    private static Schema getSchemaByName(String schemaName, Components components) {
        if (schemaName == null || components == null || components.getSchemas() == null) {
            return null;
        }
        return components.getSchemas().get(schemaName);
    }

    /**
     * Determine whether the given file-based operation is marked as an endpoint diff.
     *
     * @param fileOp the operation from the file-based specification to check
     * @return `true` if the operation is marked as an endpoint diff, `false` otherwise
     */
    public static boolean isDiffStatusEndpoint(Operation fileOp) {
        return fileOp.getXOuroborosDiff()
                .equals("endpoint");
    }


    /**
     * Replace the operation for the given HTTP method at the specified URL in the file-based spec and mark it as an endpoint difference.
     *
     * Sets the provided scanned operation into the supplied file spec's PathItem for the given HTTP method and sets its `XOuroborosDiff` to "endpoint".
     *
     * @param url          the URL path whose operation will be replaced
     * @param scanOp       the operation from the scanned spec to apply
     * @param restFileSpec the file-based paths map to modify
     * @param method       the HTTP method whose operation should be replaced and marked
     */
    public static void markDiffEndpoint(String url, Operation scanOp, Map<String, PathItem> restFileSpec, HttpMethod method) {
        log.info("METHOD: [{}], URL: [{}]은 같지만 METHOD는 다름", method, url);
        PathItem pathItem = restFileSpec.get(url);

        // Preserve security from existing operation (if exists)
        Operation existingOp = getOperationByMethod(pathItem, method);
        if (existingOp != null && existingOp.getSecurity() != null && !existingOp.getSecurity().isEmpty()) {
            scanOp.setSecurity(existingOp.getSecurity());
            log.debug("Preserved {} security requirement(s) from existing operation", existingOp.getSecurity().size());
        }

        // Generate x-ouroboros-id if not present
        if (scanOp.getXOuroborosId() == null) {
            scanOp.setXOuroborosId(UUID.randomUUID().toString());
            log.debug("Generated x-ouroboros-id for {} {}: {}", method, url, scanOp.getXOuroborosId());
        }

        // Normalize tags to uppercase
        if (scanOp.getTags() != null) {
            scanOp.setTags(kr.co.ouroboros.core.global.spec.SpecValidationUtil.normalizeRestTags(scanOp.getTags()));
        }

        setOperationByMethod(pathItem, method, scanOp);
        Operation operationByMethod = getOperationByMethod(pathItem, method);
        operationByMethod.setXOuroborosDiff("endpoint");
        operationByMethod.setXOuroborosTag("none");
    }

    /**
     * Retrieve the Operation from a PathItem corresponding to the specified HTTP method.
     *
     * @param item       the PathItem containing operations for different HTTP methods
     * @param httpMethod the HTTP method whose Operation should be returned
     * @return the Operation for the specified HTTP method, or `null` if none is defined
     */
    private static Operation getOperationByMethod(PathItem item, HttpMethod httpMethod) {
        return switch (httpMethod) {
            case GET -> item.getGet();
            case POST -> item.getPost();
            case PUT -> item.getPut();
            case PATCH -> item.getPatch();
            case DELETE -> item.getDelete();
        };
    }

    /**
     * Sets the given Operation on the PathItem corresponding to the specified HTTP method.
     *
     * @param item       the PathItem to modify
     * @param httpMethod the HTTP method whose operation will be replaced
     * @param scanOp     the Operation to assign for the specified method
     */
    private static void setOperationByMethod(PathItem item, HttpMethod httpMethod, Operation scanOp) {
        switch (httpMethod) {
            case GET: item.setGet(scanOp); break;
            case POST: item.setPost(scanOp); break;
            case PUT: item.setPut(scanOp); break;
            case PATCH: item.setPatch(scanOp); break;
            case DELETE: item.setDelete(scanOp); break;
        }
    }
}
