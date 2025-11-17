import { useState, useEffect } from "react";
import { useTestingStore } from "../store/testing.store";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import { getRestApiSpec, getSchema } from "@/features/spec/services/api";
import {
  parseOpenAPIRequestBody,
  parseOpenAPISchemaToSchemaField,
} from "@/features/spec/utils/schemaConverter";
import type { RequestBody } from "@/features/spec/types/schema.types";
import { isArraySchema, isRefSchema } from "@/features/spec/types/schema.types";
import { RequestBodyForm } from "./RequestBodyForm";

export function TestRequestPanel() {
  const {
    request,
    setRequest,
    updateRequestHeader,
    addRequestHeader,
    removeRequestHeader,
    updateQueryParam,
    addQueryParam,
    removeQueryParam,
  } = useTestingStore();
  const { selectedEndpoint } = useSidebarStore();
  const [requestBody, setRequestBody] = useState<RequestBody | null>(null);
  const [contentType, setContentType] = useState("application/json");

  const methods = ["GET", "POST", "PUT", "PATCH", "DELETE"];

  // Load API spec when selectedEndpoint changes
  useEffect(() => {
    const loadApiSpec = async () => {
      if (!selectedEndpoint?.id) {
        setRequestBody(null);
        setContentType("application/json");
        setRequest({ body: "" });
        return;
      }

      try {
        const response = await getRestApiSpec(selectedEndpoint.id);
        const spec = response.data;

        // Update method, url, description
        setRequest({
          method: spec.method || "GET",
          url: spec.path || "",
          description: spec.description || spec.summary || "",
          body: "", // 새로운 API 선택 시 body 초기화
        });

        // Load parameters (query params and headers)
        const testHeaders: Array<{ key: string; value: string }> = [];
        const testQueryParams: Array<{ key: string; value: string }> = [];

        if (spec.parameters && Array.isArray(spec.parameters)) {
          spec.parameters.forEach((param: any) => {
            if (param.in === "header") {
              testHeaders.push({
                key: param.name || "",
                value: param.example || param.schema?.default || "",
              });
            } else if (param.in === "query") {
              testQueryParams.push({
                key: param.name || "",
                value: param.example || param.schema?.default || "",
              });
            }
          });
        }

        setRequest({
          queryParams: testQueryParams,
          headers: testHeaders,
        });

        // Load request body
        let detectedContentType = "application/json";
        if (spec.requestBody) {
          const reqBody = spec.requestBody as any;
          if (reqBody.content && Object.keys(reqBody.content).length > 0) {
            detectedContentType = Object.keys(reqBody.content)[0];
            setContentType(detectedContentType);

            const parsed = parseOpenAPIRequestBody(
              reqBody,
              detectedContentType
            );
            if (parsed) {
              // schemaRef가 있으면 스키마를 조회해서 fields 채우기 (모든 contentType에 대해 동일하게 처리)
              if (
                parsed.schemaRef &&
                (!parsed.fields || parsed.fields.length === 0)
              ) {
                try {
                  const schemaResponse = await getSchema(parsed.schemaRef);
                  const schemaData = schemaResponse.data;

                  if (schemaData.properties) {
                    const fields = Object.entries(schemaData.properties).map(
                      ([key, propSchema]: [string, any]) => {
                        return parseOpenAPISchemaToSchemaField(key, propSchema);
                      }
                    );

                    // required 필드 설정
                    if (
                      schemaData.required &&
                      Array.isArray(schemaData.required)
                    ) {
                      fields.forEach((field) => {
                        if (schemaData.required!.includes(field.key)) {
                          field.required = true;
                        }
                      });
                    }

                    parsed.fields = fields;
                  }
                } catch (error) {
                  // 스키마 조회 실패 시 무시
                }
              }

              // rootSchemaType이 array이고 items가 ref인 경우 스키마 조회
              if (parsed.rootSchemaType && isArraySchema(parsed.rootSchemaType)) {
                if (isRefSchema(parsed.rootSchemaType.items)) {
                  try {
                    const schemaResponse = await getSchema(parsed.rootSchemaType.items.schemaName);
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
                      parsed.rootSchemaType = {
                        ...parsed.rootSchemaType,
                        items: {
                          kind: "object",
                          properties,
                        },
                      };
                    }
                  } catch (error) {
                    // 스키마 조회 실패 시 무시
                  }
                }
              }

              setRequestBody(parsed);
            } else {
              setRequestBody(null);
            }
          } else {
            setRequestBody(null);
          }
        } else {
          setRequestBody(null);
        }

        // Update Content-Type header
        const contentTypeHeader = testHeaders.find(
          (h) => h.key.toLowerCase() === "content-type"
        );
        if (contentTypeHeader) {
          contentTypeHeader.value = detectedContentType;
        } else {
          // Content-Type 헤더가 없으면 추가
          setRequest({
            headers: [
              ...testHeaders,
              { key: "Content-Type", value: detectedContentType },
            ],
          });
        }
      } catch (error) {
        setRequestBody(null);
      }
    };

    loadApiSpec();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedEndpoint?.id]);

  const handleBodyChange = (newBody: string) => {
    setRequest({ body: newBody });
  };

  return (
    <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm">
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
            d="M12 4v16m8-8H4"
          />
        </svg>
        <span>Request</span>
      </div>

      {/* Method + URL */}
      <div className="space-y-4 mb-6">
        <div className="grid grid-cols-12 gap-3">
          <div className="col-span-3">
            <label className="block text-xs font-medium text-[#8B949E] mb-2">
              METHOD
            </label>
            <select
              value={request.method}
              onChange={(e) => setRequest({ method: e.target.value })}
              className="w-full px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm font-medium"
            >
              {methods.map((m) => (
                <option key={m} value={m}>
                  {m}
                </option>
              ))}
            </select>
          </div>
          <div className="col-span-9">
            <label className="block text-xs font-medium text-[#8B949E] mb-2">
              URL
            </label>
            <input
              type="text"
              value={request.url}
              onChange={(e) => setRequest({ url: e.target.value })}
              placeholder="/api/endpoint"
              className="w-full px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm font-mono"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-medium text-[#8B949E] mb-2">
            Desc
          </label>
          <input
            type="text"
            value={request.description}
            onChange={(e) => setRequest({ description: e.target.value })}
            placeholder="API 설명"
            className="w-full px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
          />
        </div>
      </div>

      {/* Headers */}
      <div className="mb-6">
        <div className="flex items-center justify-between mb-3">
          <label className="text-xs font-medium text-[#8B949E]">Headers</label>
          <button
            onClick={addRequestHeader}
            className="text-xs px-2 py-1 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none"
          >
            + Add
          </button>
        </div>
        <div className="space-y-2">
          {request.headers.map((header, index) => (
            <div key={index} className="flex gap-2">
              <input
                type="text"
                value={header.key}
                onChange={(e) =>
                  updateRequestHeader(index, e.target.value, header.value)
                }
                placeholder="Key"
                className="flex-1 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
              />
              <input
                type="text"
                value={header.value}
                onChange={(e) =>
                  updateRequestHeader(index, header.key, e.target.value)
                }
                placeholder="Value"
                className="flex-1 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
              />
              <button
                onClick={() => removeRequestHeader(index)}
                className="px-3 py-2 bg-transparent border border-[#2D333B] text-[#8B949E] hover:text-red-500 hover:border-red-500 rounded-md transition-colors"
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
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Query Parameters */}
      {request.queryParams.length > 0 && (
        <div className="mb-6">
          <div className="flex items-center justify-between mb-3">
            <label className="text-xs font-medium text-[#8B949E]">
              Query Parameters
            </label>
            <button
              onClick={addQueryParam}
              className="text-xs px-2 py-1 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none"
            >
              + Add
            </button>
          </div>
          <div className="space-y-2">
            {request.queryParams.map((param, index) => (
              <div key={index} className="flex gap-2">
                <input
                  type="text"
                  value={param.key}
                  onChange={(e) =>
                    updateQueryParam(index, e.target.value, param.value)
                  }
                  placeholder="Key"
                  className="flex-1 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
                />
                <input
                  type="text"
                  value={param.value}
                  onChange={(e) =>
                    updateQueryParam(index, param.key, e.target.value)
                  }
                  placeholder="Value"
                  className="flex-1 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
                />
                <button
                  onClick={() => removeQueryParam(index)}
                  className="px-3 py-2 bg-transparent border border-[#2D333B] text-[#8B949E] hover:text-red-500 hover:border-red-500 rounded-md transition-colors"
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
                      d="M6 18L18 6M6 6l12 12"
                    />
                  </svg>
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Request Body */}
      {request.method !== "GET" && (
        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="block text-xs font-medium text-[#8B949E]">
              Request body
              {requestBody?.required && (
                <span className="text-red-500 ml-1">required</span>
              )}
            </label>
            {requestBody && requestBody.type !== "none" && (
              <span className="text-xs text-[#8B949E]">
                {requestBody.type === "json" && "application/json"}
                {requestBody.type === "form-data" && "multipart/form-data"}
                {requestBody.type === "x-www-form-urlencoded" &&
                  "application/x-www-form-urlencoded"}
                {requestBody.type === "xml" &&
                  (contentType.includes("text/xml")
                    ? "text/xml"
                    : "application/xml")}
              </span>
            )}
            {(!requestBody || requestBody.type === "none") && (
              <span className="text-xs text-[#8B949E]">application/json</span>
            )}
          </div>
          {requestBody?.description && (
            <p className="text-xs text-[#8B949E] mb-2">
              {requestBody.description}
            </p>
          )}
          <RequestBodyForm
            requestBody={
              requestBody || { type: "json", required: false, fields: [] }
            }
            contentType={contentType}
            value={request.body}
            onChange={handleBodyChange}
          />
        </div>
      )}
    </div>
  );
}
