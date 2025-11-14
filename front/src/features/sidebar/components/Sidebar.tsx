import React, { useState, useMemo } from "react";
import { StatusFilter } from "./StatusFilter";
import { EndpointGroup } from "./EndpointGroup";
import { EndpointCard } from "./EndpointCard";
import { ProtocolTabs } from "@/features/spec/components/ProtocolTabs";
import { useSidebarStore } from "../store/sidebar.store";

interface SidebarProps {
  onAddNew?: () => void;
}

export function Sidebar({ onAddNew }: SidebarProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<"mock" | "completed">(
    "mock"
  );

  const {
    isDarkMode,
    toggleDarkMode,
    toggle,
    endpoints,
    loadEndpoints,
    isLoading,
    protocol,
    setProtocol,
    setTriggerNewForm,
  } = useSidebarStore();

  React.useEffect(() => {
    loadEndpoints();
  }, [loadEndpoints]);

  const filteredEndpoints = useMemo(() => {
    const filtered: Record<
      string,
      Array<{
        id: string;
        method: string;
        path: string;
        description: string;
        implementationStatus?: "not-implemented" | "in-progress" | "modifying";
        hasSpecError?: boolean;
      }>
    > = {};

    Object.entries(endpoints).forEach(([group, groupEndpoints]) => {
      const filteredGroupEndpoints = groupEndpoints.filter((endpoint) => {
        // 프로토콜 필터링 (null일 때는 모든 엔드포인트 표시)
        if (protocol !== null) {
          const endpointProtocol = endpoint.protocol || "REST"; // 기본값은 REST
          if (endpointProtocol !== protocol) {
            return false;
          }
        }

        // 검색 필터링
        if (searchQuery) {
          const query = searchQuery.toLowerCase();
          const matchesSearch =
            endpoint.path.toLowerCase().includes(query) ||
            endpoint.description.toLowerCase().includes(query) ||
            endpoint.method.toLowerCase().includes(query);
          if (!matchesSearch) return false;
        }

        // REST 전용: Mock/Completed 필터 적용
        // WebSocket, GraphQL: 필터 적용 안 함 (모두 표시)
        const endpointProtocol = endpoint.protocol || "REST"; // 기본값은 REST
        if (endpointProtocol === "REST") {
          const ep = endpoint as { progress?: string };
          const progressLower = ep.progress?.toLowerCase();
          if (activeFilter === "mock") {
            return progressLower !== "completed";
          } else {
            return progressLower === "completed";
          }
        }

        // WebSocket, GraphQL은 필터 미적용 (모두 통과)
        return true;
      });

      if (filteredGroupEndpoints.length > 0) {
        filtered[group] = filteredGroupEndpoints;
      }
    });

    return filtered;
  }, [searchQuery, activeFilter, endpoints, protocol]);

  return (
    <div className="h-full flex flex-col bg-white dark:bg-[#0D1117] transition-colors">
      <div className="p-4 border-b border-gray-200 dark:border-[#2D333B]">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-[#E6EDF3]">
              API 엔드포인트
            </h2>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={toggle}
              className="lg:hidden p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-[#161B22] transition-colors"
              aria-label="사이드바 닫기"
            >
              <svg
                className="w-5 h-5 text-gray-600 dark:text-[#8B949E]"
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
            <button
              onClick={toggleDarkMode}
              className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-[#161B22] transition-colors"
              title={isDarkMode ? "라이트 모드로 전환" : "다크 모드로 전환"}
            >
              {isDarkMode ? (
                <svg
                  className="w-5 h-5 text-yellow-500"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path
                    fillRule="evenodd"
                    d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z"
                    clipRule="evenodd"
                  />
                </svg>
              ) : (
                <svg
                  className="w-5 h-5 text-gray-600 dark:text-[#8B949E]"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z" />
                </svg>
              )}
            </button>
          </div>
        </div>

        <ProtocolTabs
          selectedProtocol={protocol}
          onProtocolChange={setProtocol}
          onNewForm={() => {
            if (setTriggerNewForm) {
              setTriggerNewForm(true);
            }
          }}
          compact={true}
        />

        <div className="relative mb-3">
          <svg
            className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400 dark:text-[#8B949E]"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
            />
          </svg>
          <input
            type="text"
            placeholder="Search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-8 py-2 border border-gray-300 dark:border-[#2D333B] rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder-gray-400 dark:placeholder-[#8B949E]"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery("")}
              className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-[#8B949E] dark:text-[#8B949E]"
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
          )}
        </div>

        {/* REST일 때만 Mock/Completed 필터 표시 */}
        {protocol === "REST" && (
          <StatusFilter
            activeFilter={activeFilter}
            onFilterChange={setActiveFilter}
          />
        )}
      </div>

      <div className="flex-1 overflow-y-auto">
        {isLoading ? (
          <div className="p-4 text-center text-gray-500 dark:text-[#8B949E] text-sm">
            로딩 중...
          </div>
        ) : Object.keys(filteredEndpoints).length > 0 ? (
          Object.entries(filteredEndpoints).map(([groupName, endpoints]) => (
            <EndpointGroup key={groupName} groupName={groupName} defaultOpen>
              {endpoints.map((endpoint) => (
                <EndpointCard
                  key={endpoint.id}
                  endpoint={endpoint}
                  filterType={activeFilter}
                />
              ))}
            </EndpointGroup>
          ))
        ) : (
          <div className="p-4 text-center text-gray-500 dark:text-[#8B949E] text-sm">
            엔드포인트가 없습니다
          </div>
        )}
      </div>

      <div className="p-4 border-t border-gray-200 dark:border-[#2D333B]">
        <button
          onClick={() => {
            // 선택된 프로토콜 정보와 함께 새 폼 트리거
            if (setTriggerNewForm) {
              setTriggerNewForm(true);
            }
            if (onAddNew) {
              onAddNew();
            }
          }}
          disabled={protocol === null}
          className={`w-full py-2 px-4 rounded-lg transition-colors ${
            protocol === null
              ? "bg-gray-300 dark:bg-[#2D333B] text-gray-500 dark:text-[#6E7681] cursor-not-allowed"
              : "bg-black dark:bg-[#161B22] text-white dark:text-[#E6EDF3] hover:bg-gray-800 dark:hover:bg-[#21262D]"
          }`}
        >
          + Add
        </button>
      </div>
    </div>
  );
}
