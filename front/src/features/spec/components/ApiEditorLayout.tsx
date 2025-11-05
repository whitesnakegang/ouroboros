import { useState, useEffect, useCallback } from "react";
import { ApiRequestCard } from "./ApiRequestCard";
import { ApiResponseCard } from "./ApiResponseCard";
import { SchemaCard } from "./SchemaCard";
import { ProtocolTabs } from "./ProtocolTabs";
import { CodeSnippetPanel } from "./CodeSnippetPanel";
import { ImportResultModal } from "./ImportResultModal";
import { TestLayout } from "@/features/testing/components/TestLayout";
import { DiffNotification } from "./DiffNotification";
import { useSpecStore } from "../store/spec.store";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import { useTestingStore } from "@/features/testing/store/testing.store";
import axios from "axios";
import {
  downloadMarkdown,
  exportAllToMarkdown,
} from "../utils/markdownExporter";
import { buildOpenApiYamlFromSpecs, downloadYaml } from "../utils/yamlExporter";
import {
  getAllRestApiSpecs,
  getAllSchemas,
  type GetAllSchemasResponse,
  importYaml,
  type ImportYamlResponse,
} from "../services/api";
import {
  createRestApiSpec,
  updateRestApiSpec,
  deleteRestApiSpec,
  getRestApiSpec,
  getSchema,
} from "../services/api";

interface KeyValuePair {
  key: string;
  value: string;
  required?: boolean;
  description?: string;
  type?: string;
}

// Import RequestBody type from ApiRequestCard
type RequestBody = {
  type: "none" | "form-data" | "x-www-form-urlencoded" | "json" | "xml";
  contentType: string;
  fields: Array<{
    key: string;
    value: string;
    type: string;
    description?: string;
    required?: boolean;
    ref?: string; // 스키마 참조 시 사용 (예: "User")
  }>;
  schemaRef?: string; // 전체 스키마 참조 (예: "User")
};

interface StatusCode {
  code: string;
  type: "Success" | "Error";
  message: string;
  headers?: Array<{ key: string; value: string }>; // Response headers
  schema?: {
    ref?: string; // 스키마 참조 (예: "User")
    properties?: Record<string, any>; // 인라인 스키마
  };
}

export function ApiEditorLayout() {
  const { protocol, setProtocol } = useSpecStore();
  const {
    selectedEndpoint,
    deleteEndpoint,
    addEndpoint,
    setSelectedEndpoint,
    triggerNewForm,
    setTriggerNewForm,
    loadEndpoints,
    updateEndpoint,
    endpoints,
  } = useSidebarStore();
  const {
    protocol: testProtocol,
    setProtocol: setTestProtocol,
    request,
    setRequest,
    setResponse,
    isLoading,
    setIsLoading,
    useDummyResponse,
    setUseDummyResponse,
    setTryId,
  } = useTestingStore();
  const [activeTab, setActiveTab] = useState<"form" | "test">("form");
  const [isCodeSnippetOpen, setIsCodeSnippetOpen] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [importResult, setImportResult] = useState<ImportYamlResponse | null>(
    null
  );
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [executionStatus, setExecutionStatus] = useState<
    "idle" | "running" | "completed" | "error"
  >("idle");

  // Diff가 있는지 확인 (boolean으로 명시적 변환)
  const hasDiff = !!(
    selectedEndpoint?.diff && selectedEndpoint.diff !== "none"
  );

  // Completed 상태인지 확인
  const isCompleted = selectedEndpoint?.progress?.toLowerCase() === "completed";

  // 수정/삭제 불가능한 상태인지 확인 (completed인 경우만, mock 상태는 diff가 있어도 수정/삭제 가능)
  const isReadOnly = isCompleted;

  // 에러 메시지에서 localhost 주소 제거 및 사용자 친화적인 메시지로 변환
  const getErrorMessage = (error: unknown): string => {
    if (error instanceof Error) {
      let message = error.message || String(error);
      // localhost 주소 및 관련 텍스트 제거 (다양한 형식 대응)
      message = message.replace(/localhost:\d+/gi, "");
      message = message.replace(/127\.0\.0\.1:\d+/gi, "");
      message = message.replace(/https?:\/\/localhost:\d+/gi, "");
      message = message.replace(/https?:\/\/127\.0\.0\.1:\d+/gi, "");
      message = message.replace(/localhost:\d+\s*내용:/gi, "");
      message = message.replace(/localhost:\d+\s*Content:/gi, "");
      // "내용:" 또는 "Content:" 뒤의 내용만 남기기
      message = message.replace(/.*내용:\s*/i, "");
      message = message.replace(/.*Content:\s*/i, "");
      // 불필요한 공백 및 줄바꿈 정리
      message = message.replace(/\s+/g, " ").trim();
      // 빈 메시지인 경우 기본 메시지 반환
      if (!message) {
        return "";
      }
      return message;
    }
    // Error 객체가 아닌 경우 문자열로 변환 후 처리
    const errorStr = String(error);
    const message = errorStr
      .replace(/localhost:\d+/gi, "")
      .replace(/127\.0\.0\.1:\d+/gi, "")
      .replace(/.*내용:\s*/i, "")
      .replace(/.*Content:\s*/i, "")
      .replace(/\s+/g, " ")
      .trim();
    return message || "";
  };

  // endpoints가 업데이트된 후 selectedEndpoint 유효성 검증
  useEffect(() => {
    if (selectedEndpoint && endpoints) {
      // 현재 selectedEndpoint가 실제로 존재하는지 확인
      const exists = Object.values(endpoints).some((group) =>
        group.some((ep) => ep.id === selectedEndpoint.id)
      );

      if (!exists) {
        // 존재하지 않으면 초기화 (YML에서 삭제된 경우)
        setSelectedEndpoint(null);
      }
    }
  }, [endpoints, selectedEndpoint, setSelectedEndpoint]);

  // Load selected endpoint data when endpoint is clicked
  useEffect(() => {
    if (selectedEndpoint && selectedEndpoint.id) {
      setIsEditMode(false); // 항목 선택 시 읽기 전용 모드로 시작
      loadEndpointData(selectedEndpoint.id);

      // 폼 부분으로 스크롤 (activeTab에 따라 다른 컨테이너로 스크롤)
      setTimeout(() => {
        if (activeTab === "test") {
          const testContainer = document.getElementById("test-form-container");
          if (testContainer) {
            testContainer.scrollIntoView({
              behavior: "smooth",
              block: "start",
            });
          }
        } else {
          const formContainer = document.getElementById("api-form-container");
          if (formContainer) {
            formContainer.scrollIntoView({
              behavior: "smooth",
              block: "start",
            });
          }
        }
      }, 100);
    } else {
      // selectedEndpoint가 없을 때 폼 초기화 (새로고침 시 하드코딩된 초기값 제거)
      setIsEditMode(false);
      setMethod("POST");
      setUrl("");
      setTags("");
      setDescription("");
      setSummary("");
      setQueryParams([]);
      setRequestHeaders([]);
      setRequestBody({
        type: "none",
        contentType: "application/json",
        fields: [],
      });
      setAuth({ type: "none" });
      setStatusCodes([]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedEndpoint, activeTab]);

  // Load endpoint data from backend
  const loadEndpointData = async (id: string) => {
    // restId가 null이거나 유효하지 않은 경우 처리
    if (!id || id.trim() === "") {
      alert("명세에 없는 내용입니다. 선택된 엔드포인트가 존재하지 않습니다.");
      setSelectedEndpoint(null);
      return;
    }

    try {
      const response = await getRestApiSpec(id);
      const spec = response.data;

      setMethod(spec.method);
      setUrl(spec.path);
      setDescription(spec.description || "");
      setSummary(spec.summary || "");
      setTags(spec.tags ? spec.tags.join(", ") : "");

      // Security를 auth state로 변환
      if (
        spec.security &&
        Array.isArray(spec.security) &&
        spec.security.length > 0
      ) {
        const firstSecurity = spec.security[0] as {
          requirements?: Record<string, unknown>;
        };
        if (firstSecurity && firstSecurity.requirements) {
          const schemeName = Object.keys(firstSecurity.requirements)[0];

          switch (schemeName) {
            case "BearerAuth":
              setAuth({ type: "bearer", bearer: { token: "" } });
              break;
            case "BasicAuth":
              setAuth({
                type: "basicAuth",
                basicAuth: { username: "", password: "" },
              });
              break;
            case "ApiKeyAuth":
              setAuth({
                type: "apiKey",
                apiKey: { key: "X-API-Key", value: "", addTo: "header" },
              });
              break;
            default:
              setAuth({ type: "none" });
          }
        } else {
          setAuth({ type: "none" });
        }
      } else {
        setAuth({ type: "none" });
      }

      // Parameters를 폼 state와 테스트 스토어로 분리
      const formHeaders: KeyValuePair[] = [];
      const formQueryParams: KeyValuePair[] = [];
      const testHeaders: Array<{ key: string; value: string }> = [];
      const testQueryParams: Array<{ key: string; value: string }> = [];
      let testBody = "";

      // Parameters를 헤더와 쿼리 파라미터로 분리
      if (spec.parameters && Array.isArray(spec.parameters)) {
        spec.parameters.forEach((param: any) => {
          if (param.in === "header") {
            // 폼 state (편집용)
            formHeaders.push({
              key: param.name || "",
              value: param.schema?.default || param.example || "",
              required: param.required || false,
              description: param.description || "",
              type: param.schema?.type || "string",
            });
            // 테스트 스토어 (Try It 용)
            testHeaders.push({
              key: param.name || "",
              value: param.example || param.schema?.default || "",
            });
          } else if (param.in === "query") {
            // 폼 state (편집용)
            formQueryParams.push({
              key: param.name || "",
              value: param.schema?.default || param.example || "",
              required: param.required || false,
              description: param.description || "",
              type: param.schema?.type || "string",
            });
            // 테스트 스토어 (Try It 용)
            testQueryParams.push({
              key: param.name || "",
              value: param.example || param.schema?.default || "",
            });
          }
        });
      }

      // 폼 state 업데이트
      setQueryParams(formQueryParams);
      setRequestHeaders(formHeaders);

      // RequestBody 처리
      let loadedRequestBody: RequestBody = {
        type: "none",
        contentType: "application/json",
        fields: [],
      };

      if (spec.requestBody) {
        const reqBody = spec.requestBody as any;

        // content가 없거나 빈 객체인 경우 처리
        if (!reqBody.content || Object.keys(reqBody.content).length === 0) {
          // content 없음
        } else {
          const contentType = Object.keys(reqBody.content)[0];

          // Content-Type에 따라 body type 결정
          let bodyType:
            | "none"
            | "form-data"
            | "x-www-form-urlencoded"
            | "json"
            | "xml" = "json";
          if (contentType.includes("multipart/form-data")) {
            bodyType = "form-data";
          } else if (contentType.includes("x-www-form-urlencoded")) {
            bodyType = "x-www-form-urlencoded";
          } else if (contentType.includes("xml")) {
            bodyType = "xml";
          } else if (contentType.includes("json")) {
            bodyType = "json";
          }

          const content = reqBody.content[contentType];

          if (content) {
            const schema = content.schema;

            // 스키마 참조 처리 ($ref 또는 ref)
            const schemaRef = schema?.$ref || schema?.ref;
            if (schemaRef) {
              // $ref 형식: "#/components/schemas/OrderCreateRequest"
              // ref 형식: "OrderCreateRequest"
              let schemaName: string;
              if (
                typeof schemaRef === "string" &&
                schemaRef.includes("#/components/schemas/")
              ) {
                const refMatch = schemaRef.match(
                  /#\/components\/schemas\/(.+)/
                );
                schemaName = refMatch ? refMatch[1] : schemaRef;
              } else {
                schemaName =
                  typeof schemaRef === "string" ? schemaRef : String(schemaRef);
              }

              try {
                // 스키마 조회하여 properties 가져오기
                const schemaResponse = await getSchema(schemaName);
                const resolvedSchema = schemaResponse.data;

                if (resolvedSchema.properties) {
                  // 스키마의 properties를 fields로 변환
                  const fields: Array<{
                    key: string;
                    value: string;
                    type: string;
                    description?: string;
                    required?: boolean;
                    ref?: string;
                  }> = [];

                  Object.keys(resolvedSchema.properties).forEach((key) => {
                    const prop = resolvedSchema.properties![key];

                    // 프로퍼티가 ref를 참조하는 경우
                    if (prop.ref) {
                      fields.push({
                        key,
                        value: "",
                        type: prop.type || "object",
                        description: prop.description,
                        required: resolvedSchema.required?.includes(key),
                        ref: prop.ref,
                      });
                    } else {
                      // 일반 프로퍼티
                      fields.push({
                        key,
                        value: prop.mockExpression || "",
                        type: prop.type || "string",
                        description: prop.description,
                        required: resolvedSchema.required?.includes(key),
                      });
                    }
                  });

                  loadedRequestBody = {
                    type: bodyType,
                    contentType: contentType,
                    fields: fields,
                    schemaRef: schemaName, // 참조 정보도 유지
                  };
                } else {
                  // properties가 없는 경우
                  loadedRequestBody = {
                    type: bodyType,
                    contentType: contentType,
                    fields: [],
                    schemaRef: schemaName,
                  };
                }
              } catch (error) {
                console.error("⚠️ 스키마 조회 실패:", error);
                // 스키마 조회 실패 시에도 schemaRef는 설정
                loadedRequestBody = {
                  type: bodyType,
                  contentType: contentType,
                  fields: [],
                  schemaRef: schemaName,
                };
              }
            } else if (schema?.properties) {
              // 인라인 스키마 처리
              const fields: Array<{
                key: string;
                value: string;
                type: string;
                description?: string;
                required?: boolean;
                ref?: string;
              }> = [];

              Object.keys(schema.properties).forEach((key) => {
                const prop = schema.properties[key];

                // 프로퍼티가 ref를 참조하는 경우
                if (prop.$ref) {
                  const refMatch = prop.$ref.match(
                    /#\/components\/schemas\/(.+)/
                  );
                  if (refMatch) {
                    fields.push({
                      key,
                      value: "",
                      type: "object",
                      description: prop.description,
                      required: schema.required?.includes(key),
                      ref: refMatch[1],
                    });
                  }
                } else {
                  // 일반 프로퍼티
                  fields.push({
                    key,
                    value: prop.mockExpression || prop.default || "",
                    type: prop.type || "string",
                    description: prop.description,
                    required: schema.required?.includes(key),
                  });
                }
              });

              loadedRequestBody = {
                type: bodyType,
                contentType: contentType,
                fields: fields,
              };
            } else if (schema) {
              // schema는 있지만 $ref나 properties가 없는 경우 (예: type만 있는 경우)
              // 빈 body이지만 type은 설정
              loadedRequestBody = {
                type: bodyType,
                contentType: contentType,
                fields: [],
              };
            }

            // 테스트용 body 생성
            if (schema && schema.properties) {
              const bodyObj: Record<string, any> = {};
              Object.keys(schema.properties).forEach((key) => {
                const prop = schema.properties[key];
                if (prop.example !== undefined) {
                  bodyObj[key] = prop.example;
                } else if (prop.type === "string") {
                  bodyObj[key] = prop.default || "string";
                } else if (prop.type === "number" || prop.type === "integer") {
                  bodyObj[key] = prop.default || 0;
                } else if (prop.type === "boolean") {
                  bodyObj[key] = prop.default || false;
                }
              });
              testBody = JSON.stringify(bodyObj, null, 2);
            } else if (content.example !== undefined) {
              testBody =
                typeof content.example === "string"
                  ? content.example
                  : JSON.stringify(content.example, null, 2);
            }
          }
        }
      }

      // RequestBody state 업데이트
      setRequestBody(loadedRequestBody);

      // Responses 처리
      const loadedStatusCodes: StatusCode[] = [];
      if (spec.responses && typeof spec.responses === "object") {
        // for...of 루프로 변경하여 비동기 처리 가능하게 함
        for (const [code, response] of Object.entries(spec.responses)) {
          if (!code || code.trim() === "") continue;

          const responseData = response as any;

          const statusCode: StatusCode = {
            code: code,
            type:
              parseInt(code) >= 200 && parseInt(code) < 300
                ? "Success"
                : "Error",
            message: responseData.description || "",
          };

          // Response headers 처리
          if (
            responseData.headers &&
            typeof responseData.headers === "object"
          ) {
            statusCode.headers = Object.entries(responseData.headers).map(
              ([key, header]: [string, any]) => ({
                key: key,
                value:
                  (header as any).description ||
                  (header as any).schema?.type ||
                  "",
              })
            );
          }

          // Response schema 처리 - 모든 content type 확인
          if (
            responseData.content &&
            typeof responseData.content === "object"
          ) {
            // 첫 번째 content type 사용 (보통 application/json)
            const contentType = Object.keys(responseData.content)[0];
            if (contentType) {
              const content = responseData.content[contentType];
              const schema = content?.schema;

              if (schema) {
                // 스키마 참조 처리 ($ref 또는 ref)
                const schemaRef = schema.$ref || schema.ref;
                if (schemaRef) {
                  // $ref 형식: "#/components/schemas/OrderCreateRequest"
                  // ref 형식: "OrderCreateRequest"
                  let schemaName: string;
                  if (
                    typeof schemaRef === "string" &&
                    schemaRef.includes("#/components/schemas/")
                  ) {
                    const refMatch = schemaRef.match(
                      /#\/components\/schemas\/(.+)/
                    );
                    schemaName = refMatch ? refMatch[1] : schemaRef;
                  } else {
                    schemaName =
                      typeof schemaRef === "string"
                        ? schemaRef
                        : String(schemaRef);
                  }

                  try {
                    // 스키마 조회하여 properties 가져오기
                    const schemaResponse = await getSchema(schemaName);
                    const resolvedSchema = schemaResponse.data;

                    if (resolvedSchema.properties) {
                      statusCode.schema = {
                        ref: schemaName,
                        properties: resolvedSchema.properties,
                      };
                    } else {
                      statusCode.schema = {
                        ref: schemaName,
                      };
                    }
                  } catch (error) {
                    console.error("⚠️ Response 스키마 조회 실패:", error);
                    // 스키마 조회 실패 시에도 ref는 설정
                    statusCode.schema = {
                      ref: schemaName,
                    };
                  }
                } else if (schema.properties) {
                  // 인라인 스키마
                  statusCode.schema = {
                    properties: schema.properties,
                  };
                }
              }
            }
          }

          loadedStatusCodes.push(statusCode);
        }
      }

      // StatusCodes state 업데이트
      setStatusCodes(loadedStatusCodes);

      // 테스트 스토어 업데이트
      setRequest({
        method: spec.method,
        url: spec.path,
        description: spec.description || spec.summary || "",
        headers: testHeaders,
        queryParams: testQueryParams,
        body: testBody,
      });
    } catch (error) {
      console.error("API 스펙 로드 실패:", error);
      const errorMessage = getErrorMessage(error);

      // 명세에 없는 내용일 경우 selectedEndpoint 초기화
      alert("명세에 없는 내용입니다. 선택된 엔드포인트가 존재하지 않습니다.");
      setSelectedEndpoint(null);

      // 기존 에러 메시지는 콘솔에만 출력
      if (errorMessage) {
        console.error("상세 에러:", errorMessage);
      }
    }
  };

  // Form state
  const [method, setMethod] = useState("POST");
  const [url, setUrl] = useState("");
  const [tags, setTags] = useState("");
  const [description, setDescription] = useState("");
  const [summary, setSummary] = useState("");

  // Auth state
  const [auth, setAuth] = useState<{
    type:
      | "none"
      | "apiKey"
      | "bearer"
      | "jwtBearer"
      | "basicAuth"
      | "digestAuth"
      | "oauth2"
      | "oauth1";
    apiKey?: { key: string; value: string; addTo: "header" | "query" };
    bearer?: { token: string };
    basicAuth?: { username: string; password: string };
    oauth2?: { accessToken: string; tokenType?: string };
  }>({ type: "none" });

  // Request state
  const [queryParams, setQueryParams] = useState<KeyValuePair[]>([]);
  const [requestHeaders, setRequestHeaders] = useState<KeyValuePair[]>([]);
  const [requestBody, setRequestBody] = useState<RequestBody>({
    type: "none",
    contentType: "application/json",
    fields: [],
  });

  // Response state
  const [statusCodes, setStatusCodes] = useState<StatusCode[]>([]);

  const methods = ["GET", "POST", "PUT", "PATCH", "DELETE"];

  // 진행률: 전체 엔드포인트 대비 completed 비율
  const allEndpoints = Object.values(endpoints || {}).flat();
  const totalEndpoints = allEndpoints.length || 0;
  const completedEndpoints = allEndpoints.filter(
    (ep) => ep.progress?.toLowerCase() === "completed"
  ).length;
  const progressPercentage = totalEndpoints
    ? Math.round((completedEndpoints / totalEndpoints) * 100)
    : 0;

  /**
   * 프론트엔드 RequestBody를 OpenAPI RequestBody 구조로 변환
   */
  const convertRequestBodyToOpenAPI = (
    frontendBody: RequestBody | null
  ): {
    description: string;
    required: boolean;
    content: Record<string, any>;
  } | null => {
    // null이거나 type이 "none"이면 null 반환
    if (!frontendBody || frontendBody.type === "none") {
      return null;
    }

    // Content-Type 결정
    let contentType = "application/json";
    if (frontendBody.type === "form-data") {
      contentType = "multipart/form-data";
    } else if (frontendBody.type === "x-www-form-urlencoded") {
      contentType = "application/x-www-form-urlencoded";
    } else if (frontendBody.type === "xml") {
      contentType = "application/xml";
    }

    // 전체 스키마 참조가 있으면 ref만 사용
    if (frontendBody.schemaRef) {
      return {
        description: "Request body",
        required: true,
        content: {
          [contentType]: {
            schema: {
              ref: frontendBody.schemaRef,
            },
          },
        },
      };
    }

    // 인라인 스키마: fields를 OpenAPI properties로 변환
    if (!frontendBody.fields || frontendBody.fields.length === 0) {
      return null;
    }

    const properties: Record<string, any> = {};
    const required: string[] = [];

    frontendBody.fields.forEach((field) => {
      if (field.key) {
        const property: any = {
          type: field.type || "string",
        };

        if (field.description) {
          property.description = field.description;
        }

        if (field.value) {
          property.mockExpression = field.value; // DataFaker 표현식
        }

        properties[field.key] = property;

        if (field.required) {
          required.push(field.key);
        }
      }
    });

    return {
      description: "Request body",
      required: required.length > 0,
      content: {
        [contentType]: {
          schema: {
            type: "object",
            properties: properties,
            required: required.length > 0 ? required : undefined,
          },
        },
      },
    };
  };

  /**
   * queryParams를 OpenAPI parameters 구조로 변환
   */
  const convertQueryParamsToParameters = (params: KeyValuePair[]): any[] => {
    return params
      .filter((param) => param.key)
      .map((param) => ({
        name: param.key,
        in: "query",
        description: param.description || `Query parameter: ${param.key}`,
        required: param.required || false,
        schema: {
          type: param.type || "string",
        },
      }));
  };

  /**
   * requestHeaders를 OpenAPI parameters 구조로 변환
   * (Content-Type 같은 일반 헤더는 제외하고, API 스펙에 필요한 헤더만 parameters로 변환)
   */
  const convertHeadersToParameters = (headers: KeyValuePair[]): any[] => {
    // Content-Type은 requestBody의 content에 포함되므로 parameters에서 제외
    const standardHeaders = ["Content-Type", "Accept"];

    return headers
      .filter((header) => header.key && !standardHeaders.includes(header.key))
      .map((header) => ({
        name: header.key,
        in: "header",
        description: header.description || `Header: ${header.key}`,
        required: header.required || false,
        schema: {
          type: "string",
        },
      }));
  };

  /**
   * Auth 상태를 OpenAPI security 구조로 변환
   * 백엔드 형식: List<SecurityRequirement>
   * SecurityRequirement = { requirements: Map<String, List<String>> }
   */
  const convertAuthToSecurity = (): any[] => {
    if (auth.type === "none") {
      return [];
    }

    let schemeName = "";

    switch (auth.type) {
      case "bearer":
      case "jwtBearer":
        schemeName = "BearerAuth";
        break;
      case "basicAuth":
        schemeName = "BasicAuth";
        break;
      case "apiKey":
        schemeName = "ApiKeyAuth";
        break;
      case "oauth2":
        schemeName = "OAuth2";
        break;
      case "oauth1":
        schemeName = "OAuth1";
        break;
      case "digestAuth":
        schemeName = "DigestAuth";
        break;
    }

    if (!schemeName) {
      return [];
    }

    // 백엔드 SecurityRequirement 형식으로 변환
    const result = [
      {
        requirements: {
          [schemeName]: [],
        },
      },
    ];

    return result;
  };

  /**
   * StatusCode 배열을 OpenAPI responses 구조로 변환
   */
  const convertResponsesToOpenAPI = (
    statusCodes: StatusCode[]
  ): Record<string, any> => {
    return statusCodes.reduce((acc, code) => {
      // 빈 status code는 무시 (YAML에 ? '' 같은 이상한 키 생성 방지)
      if (!code.code || code.code.trim() === "") {
        return acc;
      }
      let schema: any;

      // StatusCode에 schema 정보가 있으면 사용
      if (code.schema) {
        if (code.schema.ref) {
          // Reference 모드: ref만 전송
          schema = {
            ref: code.schema.ref,
          };
        } else if (code.schema.properties) {
          // Inline 모드: properties 포함
          schema = {
            type: "object",
            properties: code.schema.properties,
          };
        } else {
          // 기본 schema
          schema = {
            type: "object",
            properties: {},
          };
        }
      } else {
        // schema 정보가 없으면 기본 schema
        schema = {
          type: "object",
          properties: {},
        };
      }

      const response: any = {
        description: code.message,
        content: {
          "application/json": {
            schema: schema,
          },
        },
      };

      // Response headers를 OpenAPI 형식으로 변환
      // 프론트: [{ key: "Content-Type", value: "application/json" }]
      // 백엔드: { "Content-Type": { description: "", schema: { type: "string" } } }
      if (code.headers && code.headers.length > 0) {
        const headers: Record<string, any> = {};
        code.headers.forEach((header: { key: string; value: string }) => {
          if (header.key) {
            headers[header.key] = {
              description: header.value || `${header.key} header`,
              schema: {
                type: "string",
              },
            };
          }
        });
        response.headers = headers;
      }

      acc[code.code] = response;
      return acc;
    }, {} as Record<string, any>);
  };

  const handleSave = async () => {
    if (!method || !url) {
      alert("Method와 URL을 입력해주세요.");
      return;
    }

    try {
      if (selectedEndpoint && isEditMode) {
        // 수정 로직 - path와 method도 수정 가능 (백엔드 지원)
        const updateRequest = {
          path: url, // path 수정 가능
          method, // method 수정 가능
          summary,
          description,
          tags: tags ? tags.split(",").map((tag) => tag.trim()) : [],
          parameters: [
            ...convertQueryParamsToParameters(queryParams),
            ...convertHeadersToParameters(requestHeaders),
          ],
          requestBody: convertRequestBodyToOpenAPI(requestBody),
          responses: convertResponsesToOpenAPI(statusCodes),
          security: convertAuthToSecurity(),
        };

        await updateRestApiSpec(selectedEndpoint.id, updateRequest);

        alert("API 스펙이 수정되었습니다.");
        setIsEditMode(false);

        // 수정된 엔드포인트를 로컬 상태에 반영
        const updatedEndpoint = {
          id: selectedEndpoint.id,
          method, // 수정된 method 반영
          path: url, // 수정된 path 반영
          description,
          implementationStatus: selectedEndpoint.implementationStatus,
          hasSpecError: selectedEndpoint.hasSpecError,
          tags: tags ? tags.split(",").map((tag) => tag.trim()) : [],
          progress: "mock",
        };
        updateEndpoint(updatedEndpoint);
        setSelectedEndpoint(updatedEndpoint);

        // 사이드바 목록 다시 로드 (백그라운드에서)
        loadEndpoints();
      } else {
        // 새 엔드포인트 생성
        const apiRequest = {
          path: url,
          method,
          summary,
          description,
          tags: tags ? tags.split(",").map((tag) => tag.trim()) : [],
          parameters: [
            ...convertQueryParamsToParameters(queryParams),
            ...convertHeadersToParameters(requestHeaders),
          ],
          requestBody: convertRequestBodyToOpenAPI(requestBody),
          responses: convertResponsesToOpenAPI(statusCodes),
          security: convertAuthToSecurity(),
        };

        const response = await createRestApiSpec(apiRequest);

        const newEndpoint = {
          id: response.data.id,
          method,
          path: url,
          description,
          implementationStatus: "not-implemented" as const,
          tags: tags ? tags.split(",").map((tag) => tag.trim()) : [],
          progress: "mock",
        };

        const group = tags ? tags.split(",")[0].trim() : "OTHERS";
        addEndpoint(newEndpoint, group);
        alert(`${method} ${url} API가 생성되었습니다.`);

        // 사이드바 목록 다시 로드
        await loadEndpoints();

        // 수정된 엔드포인트를 다시 선택
        const updatedEndpoint = {
          id: response.data.id,
          method,
          path: url,
          description,
          implementationStatus: "not-implemented" as const,
          tags: tags ? tags.split(",").map((tag) => tag.trim()) : [],
          progress: "mock",
        };
        setSelectedEndpoint(updatedEndpoint);
      }
    } catch (error: unknown) {
      console.error("API 저장 실패:", error);
      const errorMessage = getErrorMessage(error);
      alert(`API 저장에 실패했습니다: ${errorMessage}`);
    }
  };

  const handleDelete = async () => {
    if (!selectedEndpoint) return;

    // completed 상태만 삭제 불가 (mock 상태는 diff가 있어도 삭제 가능)
    if (isCompleted) {
      alert("이미 완료(completed)된 API는 삭제할 수 없습니다.");
      return;
    }

    if (confirm("이 엔드포인트를 삭제하시겠습니까?")) {
      try {
        await deleteRestApiSpec(selectedEndpoint.id);
        deleteEndpoint(selectedEndpoint.id);
        alert("엔드포인트가 삭제되었습니다.");

        // 폼 초기화
        setSelectedEndpoint(null);
        setMethod("POST");
        setUrl("/api/auth/login");
        setTags("AUTH");
        setDescription("사용자 로그인");
        setIsEditMode(false);
        loadEndpoints();
      } catch (error: unknown) {
        console.error("API 삭제 실패:", error);
        const errorMessage = getErrorMessage(error);
        alert(`API 삭제에 실패했습니다: ${errorMessage}`);
      }
    }
  };

  const handleEdit = () => {
    // completed 상태만 수정 불가 (mock 상태는 diff가 있어도 수정 가능)
    if (isCompleted) {
      alert("이미 완료(completed)된 API는 수정할 수 없습니다.");
      return;
    }
    setIsEditMode(true);
  };

  const handleCancelEdit = () => {
    if (selectedEndpoint) {
      loadEndpointData(selectedEndpoint.id);
    }
    setIsEditMode(false);
  };

  const handleReset = () => {
    if (confirm("작성 중인 내용을 초기화하시겠습니까?")) {
      setMethod("POST");
      setUrl("");
      setTags("");
      setDescription("");
      setSummary("");
      setQueryParams([]);
      setRequestHeaders([]);
      setAuth({ type: "none" });
      setRequestBody({
        type: "json",
        contentType: "application/json",
        fields: [{ key: "email", value: "string", type: "string" }],
      });
      setStatusCodes([]);
    }
  };

  const handleNewForm = useCallback(() => {
    // 새 작성 폼으로 전환
    setSelectedEndpoint(null);
    setMethod("POST");
    // 값은 비워 placeholder가 보이도록 처리
    setUrl("");
    setTags("");
    setDescription("");
    setSummary("");
    setQueryParams([]);
    setAuth({ type: "none" });
    setRequestHeaders([]);
    setRequestBody({
      type: "json",
      contentType: "application/json",
      fields: [],
    });
    setStatusCodes([]);
  }, [setSelectedEndpoint]);

  // 사이드바 Add 버튼 클릭 시 새 폼 초기화
  useEffect(() => {
    if (triggerNewForm) {
      handleNewForm();
      setTriggerNewForm(false);
    }
  }, [triggerNewForm, handleNewForm, setTriggerNewForm]);

  const handleImportYAML = async () => {
    // 파일 선택 input 생성
    const input = document.createElement("input");
    input.type = "file";
    input.accept = ".yml,.yaml";

    input.onchange = async (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (!file) return;

      // 파일 확장자 검증
      const fileName = file.name.toLowerCase();
      if (!fileName.endsWith(".yml") && !fileName.endsWith(".yaml")) {
        alert("YAML 파일(.yml 또는 .yaml)만 업로드 가능합니다.");
        return;
      }

      try {
        // Import 실행
        const result: ImportYamlResponse = await importYaml(file);

        // 모달로 결과 표시
        setImportResult(result);
        setIsImportModalOpen(true);

        // 사이드바 목록 새로고침
        await loadEndpoints();
      } catch (error) {
        console.error("YAML Import 오류:", error);
        const errorMsg = getErrorMessage(error);
        alert(`YAML Import 실패\n\n${errorMsg}`);
      }
    };

    // 파일 선택 다이얼로그 열기
    input.click();
  };

  const handleSyncDiffToSpec = async () => {
    if (!selectedEndpoint) return;

    if (
      confirm(
        "실제 구현의 내용을 명세에 자동으로 반영하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다."
      )
    ) {
      try {
        // TODO: 백엔드 API 엔드포인트 구현 필요
        // 백엔드에서 실제 구현된 스펙을 가져와서 명세를 업데이트하는 API 호출
        alert(
          "기능 개발 중입니다.\n\n백엔드에서 실제 구현 → 명세 동기화 API가 필요합니다."
        );

        // 예시: 향후 구현될 API 호출
        // const response = await syncImplementationToSpec(selectedEndpoint.id);
        // await loadEndpointData(selectedEndpoint.id);
        // await loadEndpoints();
        // alert("✅ 실제 구현이 명세에 성공적으로 반영되었습니다!");
      } catch (error: unknown) {
        console.error("명세 동기화 실패:", error);
        const errorMessage = getErrorMessage(error);
        alert(`명세 동기화에 실패했습니다: ${errorMessage}`);
      }
    }
  };

  const handleRun = async () => {
    setIsLoading(true);
    setExecutionStatus("running");
    setResponse(null);

    try {
      if (useDummyResponse) {
        // Dummy Response 사용
        setTimeout(() => {
          const dummyResponse = {
            status: 200,
            statusText: "OK",
            headers: {
              "Content-Type": "application/json",
              "X-Request-ID": "req-123456",
            },
            body: JSON.stringify(
              {
                token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mock.token",
                user: {
                  id: "123",
                  email: "test@example.com",
                  name: "Test User",
                },
              },
              null,
              2
            ),
            responseTime: Math.floor(Math.random() * 200) + 50, // 50-250ms
          };
          setResponse(dummyResponse);
          setExecutionStatus("completed");
          setIsLoading(false);
        }, 500);
      } else {
        // 실제 API 호출
        const startTime = performance.now();

        // 헤더 변환
        const headers: Record<string, string> = {};
        request.headers.forEach((h) => {
          if (h.key && h.value) {
            headers[h.key] = h.value;
          }
        });

        // X-Ouroboros-Try:on 헤더 추가
        headers["X-Ouroboros-Try"] = "on";

        // Query 파라미터 추가
        let url = request.url;
        if (request.queryParams.length > 0) {
          const queryString = request.queryParams
            .filter((p) => p.key && p.value)
            .map(
              (p) =>
                `${encodeURIComponent(p.key)}=${encodeURIComponent(p.value)}`
            )
            .join("&");
          if (queryString) {
            url += `?${queryString}`;
          }
        }

        // URL이 상대 경로인 경우 그대로 사용 (Vite 프록시 사용)
        // 절대 URL(http://로 시작)인 경우에만 그대로 사용
        const fullUrl = url.startsWith("http") ? url : url;

        // Request body 파싱 (GET 메서드가 아니고 body가 있을 때만)
        let requestData = undefined;
        if (request.method !== "GET" && request.body && request.body.trim()) {
          try {
            requestData = JSON.parse(request.body);
          } catch (e) {
            console.error("Request body 파싱 실패:", e);
            throw new Error(
              `Request body가 유효한 JSON 형식이 아닙니다: ${
                e instanceof Error ? e.message : String(e)
              }`
            );
          }
        }

        const response = await axios({
          method: request.method,
          url: fullUrl,
          headers: headers,
          data: requestData,
        });

        const endTime = performance.now();
        const responseTime = Math.round(endTime - startTime);

        // 응답 헤더에서 X-Ouroboros-Try-Id 추출
        const responseHeaders = response.headers as Record<string, string>;
        const tryIdValue =
          responseHeaders["x-ouroboros-try-id"] ||
          responseHeaders["X-Ouroboros-Try-Id"];
        if (tryIdValue) {
          setTryId(tryIdValue);
        }

        setResponse({
          status: response.status,
          statusText: response.statusText,
          headers: responseHeaders,
          body: JSON.stringify(response.data, null, 2),
          responseTime,
        });
        setExecutionStatus("completed");
      }
    } catch (error) {
      console.error("API 요청 실패:", error);
      const endTime = performance.now();
      const startTime = endTime - 100; // 에러 발생 시간 추정
      const responseTime = Math.round(endTime - startTime);

      if (axios.isAxiosError(error) && error.response) {
        // 에러 응답에서도 X-Ouroboros-Try-Id 추출 시도
        const errorHeaders = error.response.headers as Record<string, string>;
        const tryIdValue =
          errorHeaders["x-ouroboros-try-id"] ||
          errorHeaders["X-Ouroboros-Try-Id"];
        if (tryIdValue) {
          setTryId(tryIdValue);
        }

        setResponse({
          status: error.response.status,
          statusText: error.response.statusText,
          headers: errorHeaders,
          body: JSON.stringify(error.response.data, null, 2),
          responseTime,
        });
      } else {
        setResponse({
          status: 0,
          statusText: "Network Error",
          headers: {},
          body: JSON.stringify(
            { error: error instanceof Error ? error.message : "Unknown error" },
            null,
            2
          ),
          responseTime,
        });
      }
      setExecutionStatus("error");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="h-full flex flex-col bg-white dark:bg-[#0D1117] min-h-0">
      {/* Header Tabs */}
      <div className="border-b border-gray-200 dark:border-[#2D333B] px-6 py-4 bg-white dark:bg-[#0D1117]">
        <div className="flex items-center justify-between mb-4">
          {/* Left: Tabs */}
          <div className="flex gap-1 border-b border-gray-200 dark:border-[#2D333B]">
            <button
              onClick={() => setActiveTab("form")}
              className={`px-4 py-2 text-sm font-medium transition-colors border-b-2 ${
                activeTab === "form"
                  ? "text-gray-900 dark:text-[#E6EDF3] border-[#2563EB]"
                  : "text-gray-500 dark:text-[#8B949E] border-transparent hover:text-gray-900 dark:hover:text-[#E6EDF3]"
              }`}
            >
              API 생성 폼
            </button>
            <button
              onClick={() => setActiveTab("test")}
              className={`px-4 py-2 text-sm font-medium transition-colors border-b-2 ${
                activeTab === "test"
                  ? "text-gray-900 dark:text-[#E6EDF3] border-[#2563EB]"
                  : "text-gray-500 dark:text-[#8B949E] border-transparent hover:text-gray-900 dark:hover:text-[#E6EDF3]"
              }`}
            >
              테스트 폼
            </button>
          </div>

          {/* Right: Progress Bar & Actions - 조건부 표시 */}
          {activeTab === "form" ? (
            <div className="flex flex-col lg:flex-row items-start lg:items-center gap-4 lg:gap-6">
              {/* Progress Bar */}
              <div className="flex items-center gap-3">
                <div className="text-right hidden sm:block">
                  <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
                    진행률
                  </div>
                  <div className="text-xs text-gray-600 dark:text-[#8B949E]">
                    {completedEndpoints}/{totalEndpoints} 완료
                  </div>
                </div>
                <div className="w-24 sm:w-32 h-2 bg-gray-200 dark:bg-[#161B22] border border-gray-300 dark:border-[#2D333B] rounded-md overflow-hidden">
                  <div
                    className="h-full bg-[#2563EB] transition-all duration-500 ease-out"
                    style={{ width: `${progressPercentage}%` }}
                  />
                </div>
                <span className="text-sm font-medium text-gray-900 dark:text-[#E6EDF3] min-w-[3rem]">
                  {progressPercentage}%
                </span>
              </div>

              {/* Action Buttons - Utility만 유지 */}
              <div className="flex flex-wrap items-center gap-2">
                {/* Utility Buttons */}
                <button
                  onClick={handleImportYAML}
                  className="px-3 py-2 border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] rounded-md bg-transparent transition-colors text-sm font-medium flex items-center gap-2"
                  title="YAML 파일 가져오기"
                >
                  <svg
                    className="w-4 h-4"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10"
                    />
                  </svg>
                  <span className="hidden sm:inline">Import</span>
                </button>
                <button
                  onClick={async () => {
                    try {
                      const res = await getAllRestApiSpecs();
                      const md = exportAllToMarkdown(res.data);
                      downloadMarkdown(
                        md,
                        `ALL_APIS_${new Date().getTime()}.md`
                      );
                      alert("Markdown 파일이 다운로드되었습니다.");
                    } catch (e) {
                      console.error("Markdown 내보내기 오류:", e);
                      const errorMsg = getErrorMessage(e);
                      alert(
                        `전체 Markdown 내보내기에 실패했습니다.\n오류: ${errorMsg}`
                      );
                    }
                  }}
                  className="px-3 py-2 border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] rounded-md bg-transparent transition-colors text-sm font-medium flex items-center gap-2"
                  title="Markdown 파일 내보내기"
                >
                  <svg
                    className="w-4 h-4"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                    />
                  </svg>
                  <span className="hidden sm:inline">Export</span>
                </button>
                <button
                  onClick={async () => {
                    try {
                      const [specsRes, schemasRes] = await Promise.all([
                        getAllRestApiSpecs(),
                        getAllSchemas().catch((error) => {
                          console.warn(
                            "Schema 조회 실패, 빈 배열로 계속 진행:",
                            error.message
                          );
                          return {
                            status: 200,
                            data: [],
                            message: "Schema 조회 실패",
                          } as GetAllSchemasResponse;
                        }),
                      ]);
                      const yaml = buildOpenApiYamlFromSpecs(
                        specsRes.data,
                        (schemasRes as GetAllSchemasResponse).data
                      );
                      downloadYaml(
                        yaml,
                        `ALL_APIS_${new Date().getTime()}.yml`
                      );
                      alert("YAML 파일이 다운로드되었습니다.");
                    } catch (e) {
                      console.error("YAML 내보내기 오류:", e);
                      const errorMsg = getErrorMessage(e);
                      alert(
                        `전체 YAML 내보내기에 실패했습니다.\n오류: ${errorMsg}`
                      );
                    }
                  }}
                  className="px-3 py-2 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-colors text-sm font-medium flex items-center gap-2"
                  title="API YAML 파일 생성"
                >
                  <svg
                    className="w-4 h-4"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
                    />
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                    />
                  </svg>
                  <span className="hidden sm:inline">Generate</span>
                </button>
                <button
                  onClick={() => setIsCodeSnippetOpen(true)}
                  className="px-3 py-2 border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] rounded-md bg-transparent transition-colors text-sm font-medium flex items-center gap-2"
                  title="Code Snippet 보기"
                >
                  <svg
                    className="w-4 h-4"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"
                    />
                  </svg>
                  Code Snippet
                </button>
              </div>
            </div>
          ) : (
            // 테스트 폼일 때 버튼들
            <div className="flex flex-wrap items-center gap-2">
              {/* Use Dummy Response Checkbox */}
              <label className="flex items-center gap-2 cursor-pointer px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-transparent hover:bg-gray-50 dark:hover:bg-[#161B22] transition-colors">
                <input
                  type="checkbox"
                  checked={useDummyResponse}
                  onChange={(e) => setUseDummyResponse(e.target.checked)}
                  className="w-4 h-4 text-[#2563EB] bg-white dark:bg-[#0D1117] border-gray-300 dark:border-[#2D333B] rounded focus:ring-[#2563EB] focus:ring-1"
                />
                <span className="text-sm font-medium text-gray-900 dark:text-[#E6EDF3]">
                  Use Dummy Response
                </span>
              </label>

              {/* Run Button */}
              <button
                onClick={handleRun}
                disabled={isLoading}
                className="px-6 py-2 bg-[#2563EB] hover:bg-[#1E40AF] disabled:bg-gray-200 dark:disabled:bg-[#161B22] disabled:text-gray-400 dark:disabled:text-[#8B949E] text-white rounded-md transition-colors text-sm font-medium flex items-center gap-2 disabled:cursor-not-allowed"
              >
                {isLoading ? (
                  <>
                    <svg
                      className="animate-spin h-5 w-5 text-white"
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      />
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      />
                    </svg>
                    <span className="hidden sm:inline">실행 중...</span>
                  </>
                ) : (
                  <>
                    <svg
                      className="w-5 h-5"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fillRule="evenodd"
                        d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z"
                        clipRule="evenodd"
                      />
                    </svg>
                    RUN
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Protocol Tabs - 항상 표시 */}
      <div className="border-b border-gray-200 dark:border-[#2D333B] px-6 bg-white dark:bg-[#0D1117]">
        {activeTab === "form" ? (
          <ProtocolTabs
            selectedProtocol={protocol}
            onProtocolChange={setProtocol}
            onNewForm={handleNewForm}
          />
        ) : (
          <ProtocolTabs
            selectedProtocol={testProtocol}
            onProtocolChange={setTestProtocol}
            onNewForm={handleNewForm}
          />
        )}
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-auto">
        {activeTab === "test" ? (
          <>
            {/* Execution Status - 테스트 폼에서만 표시 */}
            {executionStatus !== "idle" && (
              <div className="border-b border-gray-200 dark:border-[#2D333B] px-6 py-3 bg-white dark:bg-[#0D1117]">
                <div className="flex items-center gap-2">
                  <div
                    className={`px-3 py-1 rounded-md text-sm font-medium ${
                      executionStatus === "running"
                        ? "bg-[#2563EB] text-white"
                        : executionStatus === "completed"
                        ? "bg-emerald-500 text-white"
                        : "bg-red-500 text-white"
                    }`}
                  >
                    {executionStatus === "running"
                      ? "실행 중..."
                      : executionStatus === "completed"
                      ? "완료됨"
                      : "에러 발생"}
                  </div>
                </div>
              </div>
            )}
            <TestLayout />
          </>
        ) : (
          <div
            id="api-form-container"
            className="w-full max-w-6xl mx-auto px-6 py-8"
          >
            {/* Protocol not supported message */}
            {protocol !== "REST" && (
              <div className="h-full flex items-center justify-center py-12">
                <div className="text-center">
                  <div className="w-16 h-16 mx-auto mb-6 rounded-md bg-gray-100 dark:bg-[#161B22] border border-gray-300 dark:border-[#2D333B] flex items-center justify-center">
                    <svg
                      className="w-8 h-8 text-gray-500 dark:text-[#8B949E]"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M12 6v6m0 0v6m0-6h6m-6 0H6"
                      />
                    </svg>
                  </div>
                  <h3 className="text-xl font-semibold text-gray-900 dark:text-[#E6EDF3] mb-2">
                    {protocol} 명세서 준비 중
                  </h3>
                  <p className="text-gray-600 dark:text-[#8B949E] mb-4">
                    현재는 REST API만 지원합니다.
                  </p>
                  <p className="text-sm text-gray-500 dark:text-[#8B949E]">
                    프로토콜 탭을 클릭하여 REST로 전환할 수 있습니다.
                  </p>
                </div>
              </div>
            )}

            {/* Diff Notification - 불일치가 있을 때만 표시 (completed 또는 mock 상태 모두) */}
            {protocol === "REST" && selectedEndpoint && hasDiff && (
              <DiffNotification
                diff={selectedEndpoint.diff || "none"}
                progress={selectedEndpoint.progress}
                onSyncToSpec={handleSyncDiffToSpec}
              />
            )}

            {/* Method + URL Card */}
            {protocol === "REST" && (
              <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm mb-6">
                <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mb-2 flex items-center gap-2">
                  <svg
                    className="h-4 w-4 text-gray-500 dark:text-[#8B949E]"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                    />
                  </svg>
                  <span>Method & URL</span>
                </div>
                <p className="text-xs text-gray-600 dark:text-[#8B949E] mb-4">
                  HTTP 메서드와 엔드포인트 URL을 입력하세요
                </p>

                <div className="space-y-4">
                  <div className="flex flex-col sm:flex-row gap-4">
                    <div className="relative sm:w-auto w-full">
                      <select
                        value={method}
                        onChange={(e) => setMethod(e.target.value)}
                        disabled={!!(selectedEndpoint && !isEditMode)}
                        className={`appearance-none w-full sm:w-auto px-3 py-2 pr-10 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm font-medium min-w-[120px] ${
                          selectedEndpoint && !isEditMode
                            ? "opacity-60 cursor-not-allowed"
                            : ""
                        }`}
                      >
                        {methods.map((m) => (
                          <option key={m} value={m}>
                            {m}
                          </option>
                        ))}
                      </select>
                      <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
                        <svg
                          className="w-4 h-4 text-gray-500 dark:text-[#8B949E]"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M19 9l-7 7-7-7"
                          />
                        </svg>
                      </div>
                    </div>
                    <div className="relative flex-1">
                      <input
                        type="text"
                        value={url}
                        onChange={(e) => setUrl(e.target.value)}
                        placeholder="예: /api/users, /api/auth/login"
                        disabled={!!(selectedEndpoint && !isEditMode)}
                        className={`w-full px-3 py-2 ${
                          hasDiff ? "pr-10" : ""
                        } rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm font-mono ${
                          selectedEndpoint && !isEditMode
                            ? "opacity-60 cursor-not-allowed"
                            : ""
                        }`}
                      />
                      {/* Diff 주의 표시 아이콘 (URL 우측) */}
                      {hasDiff && (
                        <div
                          className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none"
                          title="명세와 실제 구현이 일치하지 않습니다"
                        >
                          <svg
                            className="w-4 h-4 text-amber-500"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={2}
                              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                            />
                          </svg>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Method Badge */}
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-gray-600 dark:text-[#8B949E]">
                      Method:
                    </span>
                    <span
                      className={`inline-flex items-center rounded-[4px] border border-gray-300 dark:border-[#2D333B] bg-white dark:bg-[#0D1117] px-2 py-[2px] text-[10px] font-mono font-semibold ${
                        method === "GET"
                          ? "text-[#10B981]"
                          : method === "POST"
                          ? "text-[#2563EB]"
                          : method === "PUT"
                          ? "text-[#F59E0B]"
                          : method === "PATCH"
                          ? "text-[#F59E0B]"
                          : "text-red-500"
                      }`}
                    >
                      {method}
                    </span>
                  </div>

                  <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
                    <div>
                      <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                        Tags/Category
                      </label>
                      <input
                        type="text"
                        value={tags}
                        onChange={(e) => setTags(e.target.value)}
                        placeholder="예: AUTH, USER, PRODUCT, ORDER"
                        disabled={!!(selectedEndpoint && !isEditMode)}
                        className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                          selectedEndpoint && !isEditMode
                            ? "opacity-60 cursor-not-allowed"
                            : ""
                        }`}
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                        Summary
                      </label>
                      <input
                        type="text"
                        value={summary}
                        onChange={(e) => setSummary(e.target.value)}
                        placeholder="예: 사용자 로그인 생성"
                        disabled={!!(selectedEndpoint && !isEditMode)}
                        className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                          selectedEndpoint && !isEditMode
                            ? "opacity-60 cursor-not-allowed"
                            : ""
                        }`}
                      />
                    </div>
                  </div>

                  {/* Description */}
                  <div>
                    <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                      Description
                    </label>
                    <input
                      type="text"
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="예: 사용자 로그인, 상품 목록 조회, 주문 생성"
                      disabled={!!(selectedEndpoint && !isEditMode)}
                      className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                        selectedEndpoint && !isEditMode
                          ? "opacity-60 cursor-not-allowed"
                          : ""
                      }`}
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Request Card */}
            {protocol === "REST" && (
              <ApiRequestCard
                queryParams={queryParams}
                setQueryParams={setQueryParams}
                requestHeaders={requestHeaders}
                setRequestHeaders={setRequestHeaders}
                requestBody={requestBody}
                setRequestBody={setRequestBody}
                auth={auth}
                setAuth={setAuth}
                isReadOnly={!!(selectedEndpoint && !isEditMode)}
              />
            )}

            {/* Response Card */}
            {protocol === "REST" && (
              <div className="mt-6">
                <ApiResponseCard
                  statusCodes={statusCodes}
                  setStatusCodes={setStatusCodes}
                  isReadOnly={!!(selectedEndpoint && !isEditMode)}
                />
              </div>
            )}

            {/* Schema Card */}
            {protocol === "REST" && (
              <div className="mt-6">
                <SchemaCard isReadOnly={!!(selectedEndpoint && !isEditMode)} />
              </div>
            )}

            {/* Preview 제거: 상세 보기에서는 Code Snippet만 노출 */}
          </div>
        )}
      </div>

      {/* 하단 수정/삭제 버튼 - 선택된 엔드포인트가 있을 때만 표시 (명세서 폼에서만) */}
      {activeTab === "form" && selectedEndpoint && (
        <div className="border-t border-gray-200 dark:border-[#2D333B] px-6 py-4 bg-white dark:bg-[#0D1117]">
          <div className="flex items-center justify-end gap-3">
            {isEditMode ? (
              <>
                <button
                  onClick={handleCancelEdit}
                  className="px-3 py-2 border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] rounded-md bg-transparent transition-colors text-sm font-medium flex items-center gap-2"
                >
                  취소
                </button>
                <button
                  onClick={handleSave}
                  className="px-3 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-md transition-colors text-sm font-medium flex items-center gap-2"
                >
                  저장
                </button>
              </>
            ) : (
              <>
                <button
                  onClick={handleEdit}
                  disabled={isReadOnly}
                  className={`px-3 py-2 rounded-md transition-colors text-sm font-medium flex items-center gap-2 ${
                    isReadOnly
                      ? "bg-gray-200 dark:bg-[#161B22] text-gray-400 dark:text-[#8B949E] cursor-not-allowed"
                      : "bg-[#2563EB] hover:bg-[#1E40AF] text-white"
                  }`}
                  title={isCompleted ? "완료된 API는 수정할 수 없습니다" : ""}
                >
                  수정
                </button>
                <button
                  onClick={handleDelete}
                  disabled={isReadOnly}
                  className={`px-3 py-2 rounded-md transition-colors text-sm font-medium flex items-center gap-2 ${
                    isReadOnly
                      ? "bg-gray-200 dark:bg-[#161B22] text-gray-400 dark:text-[#8B949E] cursor-not-allowed"
                      : "bg-red-500 hover:bg-red-600 text-white"
                  }`}
                  title={isCompleted ? "완료된 API는 삭제할 수 없습니다" : ""}
                >
                  삭제
                </button>
              </>
            )}
          </div>
        </div>
      )}
      {/* 하단 생성/초기화 버튼 - 새 명세 작성 중일 때 표시 (명세서 폼에서만) */}
      {activeTab === "form" && !selectedEndpoint && (
        <div className="border-t border-gray-200 dark:border-[#2D333B] px-6 py-4 bg-white dark:bg-[#0D1117]">
          <div className="flex items-center justify-end gap-3">
            <button
              onClick={handleReset}
              className="px-3 py-2 border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] rounded-md bg-transparent transition-colors text-sm font-medium flex items-center gap-2"
            >
              초기화
            </button>
            <button
              onClick={handleSave}
              className="px-3 py-2 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-colors text-sm font-medium flex items-center gap-2"
            >
              생성
            </button>
          </div>
        </div>
      )}

      {/* Code Snippet Panel */}
      <CodeSnippetPanel
        isOpen={isCodeSnippetOpen}
        onClose={() => setIsCodeSnippetOpen(false)}
        method={method}
        url={url}
        headers={requestHeaders}
        requestBody={requestBody}
      />

      {/* Import Result Modal */}
      {importResult && (
        <ImportResultModal
          isOpen={isImportModalOpen}
          onClose={() => {
            setIsImportModalOpen(false);
            setImportResult(null);
          }}
          result={importResult.data}
        />
      )}
    </div>
  );
}
