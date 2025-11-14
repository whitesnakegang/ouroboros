import { useState, useEffect, useCallback, useRef } from "react";
import { ApiRequestCard } from "./ApiRequestCard";
import { ApiResponseCard } from "./ApiResponseCard";
import { SchemaCard } from "./SchemaCard";
import { CodeSnippetPanel } from "./CodeSnippetPanel";
import { ImportResultModal } from "./ImportResultModal";
import { TestLayout } from "@/features/testing/components/TestLayout";
import { DiffNotification } from "./DiffNotification";
import { WsEditorForm } from "./WsEditorForm";
import type { RequestBody } from "../types/schema.types";
import {
  useSidebarStore,
  type Endpoint,
} from "@/features/sidebar/store/sidebar.store";
import { useTestingStore } from "@/features/testing/store/testing.store";
import axios from "axios";
import { downloadMarkdown } from "../utils/markdownExporter";
import { downloadYaml } from "../utils/yamlExporter";
import {
  importYaml,
  type ImportYamlResponse,
  exportYaml,
} from "../services/api";
import {
  createRestApiSpec,
  updateRestApiSpec,
  deleteRestApiSpec,
  getRestApiSpec,
  getSchema,
  getWebSocketOperation,
  updateWebSocketOperation,
  getWebSocketChannel,
  type RestApiSpecResponse,
} from "../services/api";
import {
  convertRequestBodyToOpenAPI,
  parseOpenAPIRequestBody,
  parseOpenAPISchemaToSchemaField,
} from "../utils/schemaConverter";
import {
  createPrimitiveField,
  isArraySchema,
  isRefSchema,
} from "../types/schema.types";

interface KeyValuePair {
  key: string;
  value: string;
  required?: boolean;
  description?: string;
  type?: string;
}

interface StatusCode {
  code: string;
  type: "Success" | "Error";
  message: string;
  headers?: Array<{ key: string; value: string }>; // Response headers
  schema?: {
    ref?: string; // 스키마 참조 (예: "User")
    properties?: Record<string, any>; // 인라인 스키마
    type?: string; // Primitive 타입 (string, integer, number, boolean)
    isArray?: boolean; // Array of Schema 여부
    minItems?: number; // Array 최소 개수
    maxItems?: number; // Array 최대 개수
  };
}

export function ApiEditorLayout() {
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
    protocol,
    setProtocol,
  } = useSidebarStore();
  const {
    request,
    setResponse,
    isLoading,
    setIsLoading,
    setTryId,
    authorization,
    setAuthorization,
    setProtocol: setTestingProtocol,
  } = useTestingStore();
  const [activeTab, setActiveTab] = useState<"form" | "test">("form");
  const [isCodeSnippetOpen, setIsCodeSnippetOpen] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [isNewFormMode, setIsNewFormMode] = useState(false);
  const [importResult, setImportResult] = useState<ImportYamlResponse | null>(
    null
  );
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [executionStatus, setExecutionStatus] = useState<
    "idle" | "running" | "completed" | "error"
  >("idle");
  const [isAuthorizationInputOpen, setIsAuthorizationInputOpen] =
    useState(false);

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

      // 네트워크 에러를 사용자 친화적인 메시지로 변환
      const lowerMessage = message.toLowerCase();
      if (
        lowerMessage.includes("failed to fetch") ||
        lowerMessage.includes("networkerror") ||
        lowerMessage.includes("network request failed") ||
        message.trim() === ""
      ) {
        return "서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.";
      }

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
        return "서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.";
      }
      return message;
    }
    // Error 객체가 아닌 경우 문자열로 변환 후 처리
    const errorStr = String(error);
    const lowerErrorStr = errorStr.toLowerCase();

    // 네트워크 에러 체크
    if (
      lowerErrorStr.includes("failed to fetch") ||
      lowerErrorStr.includes("networkerror") ||
      lowerErrorStr.includes("network request failed")
    ) {
      return "서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.";
    }

    const message = errorStr
      .replace(/localhost:\d+/gi, "")
      .replace(/127\.0\.0\.1:\d+/gi, "")
      .replace(/.*내용:\s*/i, "")
      .replace(/.*Content:\s*/i, "")
      .replace(/\s+/g, " ")
      .trim();
    return (
      message || "서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요."
    );
  };

  // endpoints가 업데이트된 후 selectedEndpoint 유효성 검증 및 상태 동기화
  useEffect(() => {
    if (selectedEndpoint && endpoints) {
      // 현재 selectedEndpoint가 실제로 존재하는지 확인하고 최신 상태로 업데이트
      let foundEndpoint: typeof selectedEndpoint | null = null;

      for (const group of Object.values(endpoints)) {
        const ep = group.find((e) => e.id === selectedEndpoint.id);
        if (ep) {
          foundEndpoint = ep;
          break;
        }
      }

      if (!foundEndpoint) {
        // 존재하지 않으면 초기화 (YML에서 삭제된 경우)
        setSelectedEndpoint(null);
      } else {
        // 존재하면 최신 상태로 업데이트 (progress, tag, diff 등)
        // 상태가 변경된 경우에만 업데이트하여 무한 루프 방지
        if (
          foundEndpoint.progress !== selectedEndpoint.progress ||
          foundEndpoint.tag !== selectedEndpoint.tag ||
          foundEndpoint.diff !== selectedEndpoint.diff ||
          foundEndpoint.implementationStatus !==
            selectedEndpoint.implementationStatus ||
          foundEndpoint.hasSpecError !== selectedEndpoint.hasSpecError
        ) {
          setSelectedEndpoint(foundEndpoint);
        }
      }
    }
  }, [endpoints, selectedEndpoint, setSelectedEndpoint]);

  // Load selected endpoint data when endpoint is clicked
  useEffect(() => {
    if (selectedEndpoint && selectedEndpoint.id) {
      setIsEditMode(false); // 항목 선택 시 읽기 전용 모드로 시작
      setIsNewFormMode(false); // 엔드포인트 선택 시 새 폼 모드 해제

      // WebSocket 엔드포인트인 경우 프로토콜 설정 및 operation 데이터 로드
      if (selectedEndpoint.protocol === "WebSocket") {
        setProtocol("WebSocket");
        setTestingProtocol("WebSocket");
        setActiveTab("form"); // 명세서 상세보기를 위해 form 탭으로
        loadWebSocketOperationData(selectedEndpoint.id);
      } else {
        // REST 엔드포인트인 경우 프로토콜 설정 및 데이터 로드
        setProtocol("REST");
        setTestingProtocol("REST");
        loadEndpointData(selectedEndpoint.id);
      }

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
        fields: [],
      });
      setAuth({ type: "none" });
      setStatusCodes([]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedEndpoint?.id]);

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

      // 스펙 정보 저장 (CodeSnippetPanel에서 사용)
      setCurrentSpec(spec);

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

      // RequestBody 처리 (새로운 schemaConverter 사용)
      let loadedRequestBody: RequestBody = { type: "none", fields: [] };

      if (spec.requestBody != null) {
        const reqBody = spec.requestBody as any;

        if (reqBody.content && Object.keys(reqBody.content).length > 0) {
          const contentType = Object.keys(reqBody.content)[0];

          // 새로운 parseOpenAPIRequestBody 사용
          const parsed = parseOpenAPIRequestBody(reqBody, contentType);

          if (parsed) {
            loadedRequestBody = parsed;
          }
        }
      }

      // schemaRef가 있으면 스키마를 조회해서 fields 채우기
      if (
        loadedRequestBody.schemaRef &&
        (!loadedRequestBody.fields || loadedRequestBody.fields.length === 0)
      ) {
        try {
          const schemaResponse = await getSchema(loadedRequestBody.schemaRef);
          const schemaData = schemaResponse.data;

          if (schemaData.properties) {
            const fields = Object.entries(schemaData.properties).map(
              ([key, propSchema]: [string, any]) => {
                return parseOpenAPISchemaToSchemaField(key, propSchema);
              }
            );

            // required 필드 설정
            if (schemaData.required && Array.isArray(schemaData.required)) {
              fields.forEach((field) => {
                if (schemaData.required!.includes(field.key)) {
                  field.required = true;
                }
              });
            }

            loadedRequestBody.fields = fields;
          }
        } catch {
          // 스키마 조회 실패 시 무시
        }
      }

      // rootSchemaType이 array이고 items가 ref인 경우 스키마 조회
      if (
        loadedRequestBody.rootSchemaType &&
        isArraySchema(loadedRequestBody.rootSchemaType)
      ) {
        if (isRefSchema(loadedRequestBody.rootSchemaType.items)) {
          try {
            const schemaResponse = await getSchema(
              loadedRequestBody.rootSchemaType.items.schemaName
            );
            const schemaData = schemaResponse.data;

            // 스키마의 properties를 items의 object schema로 변환
            if (schemaData.properties) {
              const properties = Object.entries(schemaData.properties).map(
                ([key, propSchema]: [string, any]) => {
                  return parseOpenAPISchemaToSchemaField(key, propSchema);
                }
              );

              // required 필드 설정
              if (schemaData.required && Array.isArray(schemaData.required)) {
                properties.forEach((field) => {
                  if (schemaData.required!.includes(field.key)) {
                    field.required = true;
                  }
                });
              }

              // items를 object schema로 변환
              loadedRequestBody.rootSchemaType = {
                ...loadedRequestBody.rootSchemaType,
                items: {
                  kind: "object",
                  properties,
                },
              };
            }
          } catch {
            // 스키마 조회 실패 시 무시
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
                // Array 타입 감지
                if (schema.type === "array" && schema.items) {
                  const itemsSchema = schema.items;
                  const itemsRef = itemsSchema.$ref || itemsSchema.ref;

                  const schemaData: any = {
                    isArray: true,
                    minItems: schema.minItems,
                    maxItems: schema.maxItems,
                  };

                  if (itemsRef) {
                    // Array of Schema Reference
                    let schemaName: string;
                    if (
                      typeof itemsRef === "string" &&
                      itemsRef.includes("#/components/schemas/")
                    ) {
                      const refMatch = itemsRef.match(
                        /#\/components\/schemas\/(.+)/
                      );
                      schemaName = refMatch ? refMatch[1] : itemsRef;
                    } else {
                      schemaName =
                        typeof itemsRef === "string"
                          ? itemsRef
                          : String(itemsRef);
                    }

                    try {
                      // 스키마 조회하여 properties 가져오기
                      const schemaResponse = await getSchema(schemaName);
                      const resolvedSchema = schemaResponse.data;

                      schemaData.ref = schemaName;
                      if (resolvedSchema.properties) {
                        schemaData.properties = resolvedSchema.properties;
                      }
                    } catch (error) {
                      console.error("⚠️ Response 스키마 조회 실패:", error);
                      schemaData.ref = schemaName;
                    }
                  } else if (itemsSchema.properties) {
                    // Array of Inline Schema
                    schemaData.properties = itemsSchema.properties;
                  } else if (itemsSchema.type) {
                    // Array of Primitive Type
                    schemaData.type = itemsSchema.type;
                  }

                  statusCode.schema = schemaData;
                } else {
                  // Non-array 타입
                  const schemaRef = schema.$ref || schema.ref;
                  if (schemaRef) {
                    // Schema Reference
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
                      statusCode.schema = {
                        ref: schemaName,
                      };
                    }
                  } else if (schema.properties) {
                    // Inline Schema
                    statusCode.schema = {
                      properties: schema.properties,
                    };
                  } else if (schema.type) {
                    // Primitive Type
                    statusCode.schema = {
                      type: schema.type,
                    };
                  }
                }
              }
            }
          }

          loadedStatusCodes.push(statusCode);
        }
      }

      // StatusCodes state 업데이트
      setStatusCodes(loadedStatusCodes);

      // 백엔드에서 받은 상태 정보(progress, tag, diff)를 selectedEndpoint에 반영
      if (selectedEndpoint && selectedEndpoint.id === id) {
        const updatedEndpoint = {
          ...selectedEndpoint,
          progress: spec.progress || selectedEndpoint.progress,
          tag: spec.tag || selectedEndpoint.tag,
          diff: spec.diff || selectedEndpoint.diff,
          // implementationStatus와 hasSpecError는 progress, tag, diff로부터 계산됨
          implementationStatus: (() => {
            if (spec.progress?.toLowerCase() === "completed") return undefined;
            switch (spec.tag) {
              case "implementing":
                return "in-progress" as const;
              case "bugfix":
                return "modifying" as const;
              case "none":
              default:
                return "not-implemented" as const;
            }
          })(),
          hasSpecError: spec.diff && spec.diff !== "none" ? true : undefined,
        };
        setSelectedEndpoint(updatedEndpoint);
        updateEndpoint(updatedEndpoint);
      }

      // 테스트 스토어 업데이트는 TestRequestPanel에서 처리하므로 여기서는 제거
      // (TestRequestPanel에서 selectedEndpoint 변경 시 자동으로 로드됨)
    } catch (error) {
      console.error("API 스펙 로드 실패:", error);
      const errorMessage = getErrorMessage(error);

      // 에러 타입에 따라 적절한 메시지 표시
      let alertMessage = "";

      if (error instanceof Error) {
        const errMsg = error.message.toLowerCase();

        // 네트워크 에러 (서버 다운)
        if (
          errMsg.includes("failed to fetch") ||
          errMsg.includes("networkerror") ||
          errMsg.includes("network request failed") ||
          errMsg === "" ||
          !error.message
        ) {
          alertMessage =
            "서버에 연결할 수 없습니다.\n\n서버가 실행 중인지 확인해주세요.";
        }
        // 404 에러 (엔드포인트 없음)
        else if (errMsg.includes("404") || errMsg.includes("not found")) {
          alertMessage =
            "명세에 없는 내용입니다. 선택된 엔드포인트가 존재하지 않습니다.";
        }
        // 기타 서버 에러
        else {
          alertMessage = `API 스펙 로드에 실패했습니다.\n\n${
            errorMessage || error.message
          }`;
        }
      } else {
        // Error 객체가 아닌 경우
        alertMessage =
          "서버에 연결할 수 없습니다.\n\n서버가 실행 중인지 확인해주세요.";
      }

      alert(alertMessage);
      setSelectedEndpoint(null);

      // 상세 에러 정보는 콘솔에 출력
      if (errorMessage) {
        console.error("상세 에러:", errorMessage);
      }
    }
  };

  // Load WebSocket operation data from backend
  const loadWebSocketOperationData = async (operationId: string) => {
    if (!operationId || operationId.trim() === "") {
      alert("유효하지 않은 Operation ID입니다.");
      setSelectedEndpoint(null);
      return;
    }

    try {
      // operationId는 UUID (x-ouroboros-id)
      const response = await getWebSocketOperation(operationId);
      const operationData = response.data;

      console.log("✅ Loaded WebSocket operation:", operationData);

      // WebSocket form state 설정
      setWsEntryPoint(operationData.operation.entrypoint || "/ws");
      setWsSummary(""); // Operation에는 summary가 없음
      setWsDescription(operationData.operationName || ""); // operationName을 description으로
      setWsTags(""); // 필요시 추가

      // Receiver 설정
      if (
        operationData.operation.action === "receive" &&
        operationData.operation.channel
      ) {
        const channelRef = operationData.operation.channel.ref || "";
        const channelName = channelRef.replace("#/channels/", "");

        // Channel 정보 조회하여 실제 address 사용
        let actualAddress = channelName;
        try {
          const channelResponse = await getWebSocketChannel(channelName);
          actualAddress = channelResponse.data.channel?.address || channelName;
        } catch {
          console.warn("Channel 조회 실패, channel name 사용:", channelName);
        }

        setWsReceiver({
          address: actualAddress,
          headers: [
            {
              key: "accept-version",
              value: "1.1",
              required: true,
              description: "STOMP 프로토콜 버전 (필수)",
            },
          ],
          schema: { type: "json", fields: [] },
        });
      } else if (
        operationData.operation.action === "send" &&
        operationData.operation.channel
      ) {
        // Send-only operation의 경우도 channel을 receiver로 설정
        const channelRef = operationData.operation.channel.ref || "";
        const channelName = channelRef.replace("#/channels/", "");

        let actualAddress = channelName;
        try {
          const channelResponse = await getWebSocketChannel(channelName);
          actualAddress = channelResponse.data.channel?.address || channelName;
        } catch {
          console.warn("Channel 조회 실패, channel name 사용:", channelName);
        }

        setWsReceiver({
          address: actualAddress,
          headers: [],
          schema: { type: "json", fields: [] },
        });
      } else {
        setWsReceiver(null);
      }

      // Reply 설정 (reply가 있는 경우)
      if (
        operationData.operation.reply &&
        operationData.operation.reply.channel
      ) {
        const replyChannelRef = operationData.operation.reply.channel.ref || "";
        const replyChannelName = replyChannelRef.replace("#/channels/", "");

        // Channel 정보 조회하여 실제 address 사용
        let actualReplyAddress = replyChannelName;
        try {
          const channelResponse = await getWebSocketChannel(replyChannelName);
          actualReplyAddress =
            channelResponse.data.channel?.address || replyChannelName;
        } catch {
          console.warn(
            "Reply channel 조회 실패, channel name 사용:",
            replyChannelName
          );
        }

        setWsReply({
          address: actualReplyAddress,
          schema: { type: "json", fields: [] },
        });
      } else {
        setWsReply(null);
      }

      console.log("✅ WebSocket form state 설정 완료");
    } catch (error) {
      console.error("WebSocket Operation 로드 실패:", error);
      alert(
        `Operation을 불러오는데 실패했습니다: ${
          error instanceof Error ? error.message : "알 수 없는 오류"
        }`
      );
    }
  };

  // Form state
  const [method, setMethod] = useState("POST");
  const [url, setUrl] = useState("");
  const [tags, setTags] = useState("");
  const [currentSpec, setCurrentSpec] = useState<RestApiSpecResponse | null>(
    null
  );
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
    fields: [],
  });

  // Response state
  const [statusCodes, setStatusCodes] = useState<StatusCode[]>([]);

  // WebSocket state
  const [wsEntryPoint, setWsEntryPoint] = useState("");
  const [wsSummary, setWsSummary] = useState("");
  const [wsDescription, setWsDescription] = useState("");
  const [wsTags, setWsTags] = useState("");
  const [wsReceiver, setWsReceiver] = useState<{
    address: string;
    headers: KeyValuePair[];
    schema: RequestBody;
  } | null>(null);
  const [wsReply, setWsReply] = useState<{
    address: string;
    schema: RequestBody;
  } | null>(null);

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
      let baseSchema: any;
      if (code.schema) {
        if (code.schema.ref) {
          // Reference 모드: ref로 전송 (백엔드에서 $ref로 변환)
          baseSchema = {
            ref: code.schema.ref,
          };
        } else if (code.schema.properties) {
          // Inline 모드: properties 포함
          baseSchema = {
            type: "object",
            properties: code.schema.properties,
          };
        } else if (code.schema.type) {
          // Primitive 타입 (string, integer, number, boolean)
          baseSchema = {
            type: code.schema.type,
          };
        } else {
          // 기본 schema
          baseSchema = {
            type: "object",
            properties: {},
          };
        }
      } else {
        // schema 정보가 없으면 기본 schema
        baseSchema = {
          type: "object",
          properties: {},
        };
      }

      // isArray가 true이면 array로 감싸기
      if (code.schema?.isArray) {
        schema = {
          type: "array",
          items: baseSchema,
        };
        // minItems/maxItems 추가
        if (code.schema.minItems !== undefined) {
          schema.minItems = code.schema.minItems;
        }
        if (code.schema.maxItems !== undefined) {
          schema.maxItems = code.schema.maxItems;
        }
      } else {
        schema = baseSchema;
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
    // WebSocket 저장 로직
    if (protocol === "WebSocket") {
      if (!wsEntryPoint) {
        alert("Entry Point를 입력해주세요.");
        return;
      }
      alert("WebSocket Operation 저장 기능은 준비 중입니다.");
      // TODO: Implement WebSocket Operation save logic
      // - createWebSocketOperation or updateWebSocketOperation 호출
      // - receiver/reply messages 변환
      // - protocol + pathname 변환
      return;
    }

    // REST 저장 로직
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

        // 사이드바 목록 다시 로드 (백그라운드에서)
        await loadEndpoints();

        // 저장 후 다시 로드하여 백엔드에서 최신 데이터 가져오기
        // loadEndpointData에서 백엔드 응답의 progress, tag, diff를 자동으로 반영함
        await loadEndpointData(selectedEndpoint.id);
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

        // 백엔드 응답에서 상태 정보 추출
        const spec = response.data;
        const group = tags ? tags.split(",")[0].trim() : "OTHERS";

        // 백엔드 응답의 상태 정보를 사용하여 엔드포인트 생성
        const newEndpoint = {
          id: spec.id,
          method: spec.method,
          path: spec.path,
          description: spec.description || spec.summary || "",
          implementationStatus: (() => {
            if (spec.progress?.toLowerCase() === "completed") return undefined;
            switch (spec.tag) {
              case "implementing":
                return "in-progress" as const;
              case "bugfix":
                return "modifying" as const;
              case "none":
              default:
                return "not-implemented" as const;
            }
          })(),
          hasSpecError: spec.diff && spec.diff !== "none" ? true : undefined,
          tags: spec.tags || [],
          progress: spec.progress || "mock",
          tag: spec.tag || "none",
          diff: spec.diff || "none",
        };

        addEndpoint(newEndpoint, group);
        alert(`${method} ${url} API가 생성되었습니다.`);

        // 사이드바 목록 다시 로드
        await loadEndpoints();

        // 생성된 엔드포인트를 선택
        setSelectedEndpoint(newEndpoint);
        setIsNewFormMode(false); // 저장 후 새 폼 모드 해제

        // 생성 후 다시 로드하여 백엔드에서 최신 데이터 가져오기
        // loadEndpointData에서 백엔드 응답의 progress, tag, diff를 자동으로 반영함
        await loadEndpointData(spec.id);
      }
    } catch (error: unknown) {
      console.error("API 저장 실패:", error);
      const errorMessage = getErrorMessage(error);
      alert(`API 저장에 실패했습니다: ${errorMessage}`);
    }
  };

  const handleDelete = async () => {
    if (!selectedEndpoint) return;

    // WebSocket 삭제 로직
    if (protocol === "WebSocket") {
      if (confirm("이 WebSocket Operation을 삭제하시겠습니까?")) {
        alert("WebSocket Operation 삭제 기능은 준비 중입니다.");
        // TODO: Implement WebSocket Operation delete logic
        // - deleteWebSocketOperation 호출
      }
      return;
    }

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
        fields: [createPrimitiveField("email", "string")],
      });
      setStatusCodes([]);
    }
  };

  const handleNewForm = useCallback(() => {
    // 새 작성 폼으로 전환
    // 선택된 프로토콜에 따라 다른 폼을 표시할 수 있도록 구조화
    // 현재는 REST만 지원하지만, 나중에 WebSocket/GraphQL 지원 시 확장 가능
    setSelectedEndpoint(null);
    setIsNewFormMode(true);
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
      fields: [],
    });
    setStatusCodes([]);

    // 프로토콜에 따른 추가 초기화
    if (protocol === "WebSocket") {
      setWsEntryPoint("");
      setWsSummary("");
      setWsDescription("");
      setWsTags("");
      setWsReceiver(null);
      setWsReply(null);
    }
    // if (protocol === "GraphQL") { ... }
  }, [setSelectedEndpoint, protocol]);

  // 사이드바 Add 버튼 클릭 시 새 폼 초기화
  useEffect(() => {
    if (triggerNewForm) {
      handleNewForm();
      setActiveTab("form"); // 새 명세서 작성 시 항상 폼 탭으로 전환
      setTriggerNewForm(false);
    }
  }, [triggerNewForm, handleNewForm, setTriggerNewForm]);

  // 프로토콜 변경 시 새 폼 모드 해제
  // Add 버튼을 눌러서 새 폼을 작성 중이어도 프로토콜을 변경하면 새 폼 모드를 해제해야 함
  const prevProtocolRef = useRef(protocol);
  const triggerNewFormRef = useRef(triggerNewForm);

  // triggerNewForm 변경 시 ref 업데이트
  useEffect(() => {
    triggerNewFormRef.current = triggerNewForm;
  }, [triggerNewForm]);

  useEffect(() => {
    // 프로토콜이 변경되었을 때
    if (prevProtocolRef.current !== protocol) {
      // triggerNewForm이 true인 경우는 Add 버튼을 누른 직후이므로 무시
      if (!triggerNewFormRef.current) {
        // 프로토콜 변경 시 새 폼 모드 해제
        setIsNewFormMode(false);
      }
    }
    prevProtocolRef.current = protocol;
  }, [protocol]);

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
        // 현재 엔드포인트의 정보를 백엔드에서 가져옴
        const response = await getRestApiSpec(selectedEndpoint.id);
        const spec = response.data;

        // 현재 명세 정보를 그대로 업데이트 요청으로 전달
        // 백엔드의 updateRestApiSpec 메서드에서 자동으로 x-ouroboros-diff를 "none"으로 설정함
        const updateRequest = {
          path: spec.path,
          method: spec.method,
          summary: spec.summary,
          description: spec.description,
          tags: spec.tags || [],
          parameters: spec.parameters || [],
          requestBody: spec.requestBody || undefined,
          responses: spec.responses || {},
          security: spec.security || [],
        };

        await updateRestApiSpec(selectedEndpoint.id, updateRequest);

        // 엔드포인트 데이터 다시 로드하여 최신 상태 반영
        await loadEndpointData(selectedEndpoint.id);

        // 사이드바 목록도 다시 로드하여 diff 상태 업데이트
        await loadEndpoints();

        alert(" 실제 구현이 명세에 성공적으로 반영되었습니다!");
      } catch (error: unknown) {
        console.error("명세 동기화 실패:", error);
        const errorMessage = getErrorMessage(error);
        alert(`명세 동기화에 실패했습니다: ${errorMessage}`);
      }
    }
  };

  // WebSocket Operation 동기화 (channel diff 해결)
  const handleSyncWebSocketToActual = async () => {
    if (!selectedEndpoint || protocol !== "WebSocket") return;

    // UUID(id)를 사용 (operationName이 아님)
    const operationId = selectedEndpoint.id;
    if (!operationId) {
      alert("Operation ID를 찾을 수 없습니다.");
      return;
    }

    if (
      confirm(
        "실제 구현의 Channel 정보를 명세에 자동으로 반영하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다."
      )
    ) {
      try {
        // UUID로 operation 정보 조회
        const response = await getWebSocketOperation(operationId);
        const operation = response.data.operation;

        // 백엔드가 기대하는 UpdateOperationRequest 형식으로 변환
        const updateRequest: any = {};

        // Receive 변환 (channel + messages)
        if (operation.channel) {
          updateRequest.receive = {
            channelRef:
              operation.channel.ref?.replace("#/channels/", "") || null,
            messages:
              operation.messages
                ?.map((m) =>
                  typeof m === "string" ? m : m.ref?.replace("#/messages/", "")
                )
                .filter(Boolean) || [],
          };
        }

        // Reply 변환
        if (operation.reply?.channel) {
          updateRequest.reply = {
            channelRef:
              operation.reply.channel.ref?.replace("#/channels/", "") || null,
            messages:
              operation.reply.messages
                ?.map((m) =>
                  typeof m === "string" ? m : m.ref?.replace("#/messages/", "")
                )
                .filter(Boolean) || [],
          };
        }

        console.log("🔄 WebSocket Update Request:", updateRequest);
        console.log("📝 operationId (UUID):", operationId);
        console.log("📊 업데이트 전 diff 상태:", selectedEndpoint.diff);

        // UUID로 업데이트
        const updateResponse = await updateWebSocketOperation(
          operationId,
          updateRequest
        );

        // ✅ 업데이트 응답에서 diff 상태 확인
        const updatedDiff = updateResponse.data?.operation?.diff || "none";
        console.log("📊 업데이트 후 응답의 diff 상태:", updatedDiff);

        // UUID로 다시 로드
        await loadWebSocketOperationData(operationId);

        // 사이드바 목록도 다시 로드하여 diff 상태 업데이트
        await loadEndpoints();

        // ✅ 사이드바 새로고침 후 실제 diff 상태 확인
        setTimeout(() => {
          const allEndpoints = useSidebarStore.getState().endpoints;
          let foundEndpoint: Endpoint | null = null;

          for (const group of Object.values(allEndpoints)) {
            const ep = (group as Endpoint[]).find((e) => e?.id === operationId);
            if (ep) {
              foundEndpoint = ep;
              break;
            }
          }

          if (foundEndpoint) {
            console.log(
              "📊 사이드바 새로고침 후 diff 상태:",
              foundEndpoint.diff
            );
            console.log(
              "📊 사이드바 새로고침 후 전체 endpoint:",
              foundEndpoint
            );

            // selectedEndpoint도 최신 상태로 업데이트
            if (foundEndpoint.diff !== selectedEndpoint.diff) {
              setSelectedEndpoint(foundEndpoint);
              console.log(
                "✅ selectedEndpoint diff 상태 업데이트 완료:",
                foundEndpoint.diff
              );
            }
          } else {
            console.warn(
              "⚠️ 사이드바에서 업데이트된 endpoint를 찾을 수 없습니다."
            );
          }
        }, 200);

        alert("✅ 실제 구현이 명세에 성공적으로 반영되었습니다!");
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
      // 실제 API 호출 (Mock 엔드포인트는 백엔드에서 faker data 기반 응답 생성, Completed 엔드포인트는 실제 응답)
      const startTime = performance.now();

      // 헤더 변환
      const headers: Record<string, string> = {};
      request.headers.forEach((h) => {
        if (h.key && h.value) {
          headers[h.key] = h.value;
        }
      });

      // Authorization 헤더 추가 (입력된 경우)
      if (authorization && authorization.trim()) {
        headers["Authorization"] = authorization.trim();
      }

      // X-Ouroboros-Try:on 헤더 추가
      headers["X-Ouroboros-Try"] = "on";

      // Query 파라미터 추가
      let url = request.url;
      if (request.queryParams.length > 0) {
        const queryString = request.queryParams
          .filter((p) => p.key && p.value)
          .map(
            (p) => `${encodeURIComponent(p.key)}=${encodeURIComponent(p.value)}`
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
      let requestData: any = undefined;
      if (request.method !== "GET" && request.body && request.body.trim()) {
        const contentTypeHeader = request.headers.find(
          (h) => h.key.toLowerCase() === "content-type"
        );
        const contentType = contentTypeHeader?.value || "application/json";

        if (contentType.includes("multipart/form-data")) {
          // FormData로 변환
          const formData = new FormData();
          try {
            const bodyObj = JSON.parse(request.body);
            Object.entries(bodyObj).forEach(([key, value]) => {
              if (value !== undefined && value !== null) {
                if (value instanceof File) {
                  formData.append(key, value);
                } else if (Array.isArray(value)) {
                  value.forEach((item) => {
                    if (item instanceof File) {
                      formData.append(key, item);
                    } else {
                      formData.append(key, String(item));
                    }
                  });
                } else {
                  formData.append(key, String(value));
                }
              }
            });
            requestData = formData;
            // FormData는 Content-Type을 자동으로 설정하므로 헤더에서 제거
            delete headers["Content-Type"];
          } catch (e) {
            console.error("FormData 변환 실패:", e);
            throw new Error("FormData 변환에 실패했습니다.");
          }
        } else if (contentType.includes("application/x-www-form-urlencoded")) {
          // URLSearchParams로 변환
          const params = new URLSearchParams();
          try {
            const bodyObj = JSON.parse(request.body);
            Object.entries(bodyObj).forEach(([key, value]) => {
              if (value !== undefined && value !== null) {
                if (Array.isArray(value)) {
                  value.forEach((item) => {
                    params.append(key, String(item));
                  });
                } else {
                  params.append(key, String(value));
                }
              }
            });
            requestData = params.toString();
          } catch (e) {
            console.error("URLSearchParams 변환 실패:", e);
            throw new Error("URL-encoded 변환에 실패했습니다.");
          }
        } else if (
          contentType.includes("application/xml") ||
          contentType.includes("text/xml")
        ) {
          // XML은 문자열 그대로 전송
          requestData = request.body;
        } else {
          // JSON (기본)
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
              onClick={() => {
                // 새 명세서 작성 중일 때는 테스트 폼 접근 불가
                if (!selectedEndpoint) {
                  return;
                }
                setActiveTab("test");
              }}
              disabled={!selectedEndpoint}
              className={`px-4 py-2 text-sm font-medium transition-colors border-b-2 ${
                activeTab === "test"
                  ? "text-gray-900 dark:text-[#E6EDF3] border-[#2563EB]"
                  : "text-gray-500 dark:text-[#8B949E] border-transparent hover:text-gray-900 dark:hover:text-[#E6EDF3]"
              } ${!selectedEndpoint ? "opacity-50 cursor-not-allowed" : ""}`}
              title={
                !selectedEndpoint ? "먼저 API를 생성하거나 선택해주세요" : ""
              }
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
                </button>
                <button
                  onClick={async () => {
                    try {
                      const yaml = await exportYaml();
                      const { convertYamlToMarkdown } = await import(
                        "../utils/markdownExporter"
                      );
                      const md = convertYamlToMarkdown(yaml);
                      downloadMarkdown(
                        md,
                        `API_DOCUMENTATION_${new Date().getTime()}.md`
                      );
                      alert("Markdown 파일이 다운로드되었습니다.");
                    } catch (e) {
                      console.error("Markdown 내보내기 오류:", e);
                      const errorMsg = getErrorMessage(e);
                      alert(
                        `Markdown 내보내기에 실패했습니다.\n오류: ${errorMsg}`
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
                </button>
                <button
                  onClick={async () => {
                    try {
                      const yaml = await exportYaml();
                      downloadYaml(
                        yaml,
                        `ourorest_${new Date().getTime()}.yml`
                      );
                      alert("YAML 파일이 다운로드되었습니다.");
                    } catch (e) {
                      console.error("YAML 내보내기 오류:", e);
                      const errorMsg = getErrorMessage(e);
                      alert(`YAML 내보내기에 실패했습니다.\n오류: ${errorMsg}`);
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
                </button>
              </div>
            </div>
          ) : (
            // 테스트 폼일 때 버튼들
            <div className="flex flex-wrap items-center gap-2">
              {/* Authorization Button & Input */}
              <div className="relative flex items-center gap-2">
                {!isAuthorizationInputOpen ? (
                  <button
                    onClick={() => setIsAuthorizationInputOpen(true)}
                    className={`px-3 py-2 rounded-md border transition-colors text-sm font-medium flex items-center gap-2 ${
                      authorization && authorization.trim()
                        ? "bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800 text-green-700 dark:text-green-300"
                        : "bg-white dark:bg-[#0D1117] border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22]"
                    }`}
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
                        d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
                      />
                    </svg>
                    {authorization && authorization.trim() ? (
                      <span className="flex items-center gap-1">
                        <svg
                          className="w-4 h-4 text-green-500"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M5 13l4 4L19 7"
                          />
                        </svg>
                        Auth
                      </span>
                    ) : (
                      <span>Auth</span>
                    )}
                  </button>
                ) : (
                  <div className="relative flex items-center">
                    <input
                      type="text"
                      value={authorization}
                      onChange={(e) => setAuthorization(e.target.value)}
                      onBlur={() => {
                        // 입력이 완료되면 입력창 숨김
                        if (authorization && authorization.trim()) {
                          setIsAuthorizationInputOpen(false);
                        }
                      }}
                      onKeyDown={(e) => {
                        if (e.key === "Enter") {
                          // Enter 키를 누르면 입력창 숨김
                          if (authorization && authorization.trim()) {
                            setIsAuthorizationInputOpen(false);
                          }
                        } else if (e.key === "Escape") {
                          // Escape 키를 누르면 입력창 숨김
                          setIsAuthorizationInputOpen(false);
                        }
                      }}
                      placeholder="Authorization"
                      autoFocus
                      className="px-3 py-2 pr-10 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm w-64"
                    />
                    {authorization && authorization.trim() && (
                      <div className="absolute right-3 flex items-center">
                        <svg
                          className="w-5 h-5 text-green-500"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M5 13l4 4L19 7"
                          />
                        </svg>
                      </div>
                    )}
                  </div>
                )}
              </div>
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
            {/* Protocol not selected or not supported message */}
            {(protocol === null ||
              (protocol !== null &&
                selectedEndpoint === null &&
                !isNewFormMode)) && (
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
                    프로토콜을 선택해주세요
                  </h3>
                  <p className="text-gray-600 dark:text-[#8B949E]">
                    사이드바에서 프로토콜을 선택한 후 Add 버튼을 클릭하세요.
                  </p>
                </div>
              </div>
            )}
            {protocol === "WebSocket" &&
              (selectedEndpoint !== null || isNewFormMode) && (
                <WsEditorForm
                  entryPoint={wsEntryPoint}
                  setEntryPoint={setWsEntryPoint}
                  summary={wsSummary}
                  setSummary={setWsSummary}
                  description={wsDescription}
                  setDescription={setWsDescription}
                  tags={wsTags}
                  setTags={setWsTags}
                  receiver={wsReceiver}
                  setReceiver={setWsReceiver}
                  reply={wsReply}
                  setReply={setWsReply}
                  isReadOnly={!!(selectedEndpoint && !isEditMode)}
                  diff={selectedEndpoint?.diff}
                  operationInfo={
                    selectedEndpoint
                      ? {
                          operationName: (selectedEndpoint as any)
                            .operationName,
                          tag: selectedEndpoint.method?.toLowerCase(),
                          progress: selectedEndpoint.progress,
                        }
                      : undefined
                  }
                  onSyncToActual={
                    selectedEndpoint &&
                    selectedEndpoint.diff &&
                    selectedEndpoint.diff !== "none"
                      ? handleSyncWebSocketToActual
                      : undefined
                  }
                />
              )}
            {protocol !== null &&
              protocol !== "REST" &&
              protocol !== "WebSocket" && (
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
                      현재는 REST API와 WebSocket만 지원합니다.
                    </p>
                    <p className="text-sm text-gray-500 dark:text-[#8B949E]">
                      프로토콜 탭을 클릭하여 REST 또는 WebSocket으로 전환할 수
                      있습니다.
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
            {protocol === "REST" &&
              (selectedEndpoint !== null || isNewFormMode) && (
                <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm mb-6">
                  <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mb-2 flex items-center justify-between">
                    <div className="flex items-center gap-2">
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
                    {/* Code Snippet 버튼 - 생성 완료된 명세서에서만 활성화 (수정 중일 때는 숨김) */}
                    {selectedEndpoint && !isEditMode && (
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
                        <span className="hidden sm:inline">Code Snippet</span>
                      </button>
                    )}
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
            {protocol === "REST" &&
              (selectedEndpoint !== null || isNewFormMode) && (
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
            {protocol === "REST" &&
              (selectedEndpoint !== null || isNewFormMode) && (
                <div className="mt-6">
                  <ApiResponseCard
                    statusCodes={statusCodes}
                    setStatusCodes={setStatusCodes}
                    isReadOnly={!!(selectedEndpoint && !isEditMode)}
                  />
                </div>
              )}

            {/* Schema Card */}
            {protocol === "REST" &&
              (selectedEndpoint !== null || isNewFormMode) && (
                <div className="mt-6">
                  <SchemaCard
                    isReadOnly={!!(selectedEndpoint && !isEditMode)}
                    protocol="REST"
                  />
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
        spec={currentSpec}
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
