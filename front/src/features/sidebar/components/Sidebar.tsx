import { useState, useMemo } from "react";
import { StatusFilter } from "./StatusFilter";
import { EndpointGroup } from "./EndpointGroup";
import { EndpointCard } from "./EndpointCard";
import { useSidebarStore } from "../store/sidebar.store";

// 예시 데이터
const mockEndpoints = {
  AUTH: [
    {
      id: 1,
      method: "POST",
      path: "/api/auth/login",
      description: "사용자 로그인",
      implementationStatus: "not-implemented" as const,
    },
    {
      id: 2,
      method: "POST",
      path: "/api/auth/register",
      description: "신규 사용자 등록",
      implementationStatus: "in-progress" as const,
    },
    {
      id: 3,
      method: "POST",
      path: "/api/auth/logout",
      description: "사용자 로그아웃",
      implementationStatus: "modifying" as const,
    },
  ],
  USERS: [
    {
      id: 4,
      method: "GET",
      path: "/api/users/:id",
      description: "ID로 사용자 조회",
      hasSpecError: false,
    },
    {
      id: 5,
      method: "GET",
      path: "/api/users",
      description: "전체 사용자 목록",
      hasSpecError: true,
    },
    {
      id: 6,
      method: "PUT",
      path: "/api/users/:id",
      description: "사용자 정보 수정",
      hasSpecError: false,
    },
  ],
};

export function Sidebar() {
  const [searchQuery, setSearchQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<"mock" | "completed">(
    "mock"
  );
  const { isDarkMode, toggleDarkMode, isOpen, toggle } = useSidebarStore();

  // 필터링된 엔드포인트
  const filteredEndpoints = useMemo(() => {
    const filtered: Record<
      string,
      Array<(typeof mockEndpoints.AUTH)[0] | (typeof mockEndpoints.USERS)[0]>
    > = {};

    Object.entries(mockEndpoints).forEach(([group, endpoints]) => {
      const groupEndpoints = endpoints.filter((endpoint) => {
        // 검색어 필터
        if (searchQuery) {
          const query = searchQuery.toLowerCase();
          const matchesSearch =
            endpoint.path.toLowerCase().includes(query) ||
            endpoint.description.toLowerCase().includes(query) ||
            endpoint.method.toLowerCase().includes(query);
          if (!matchesSearch) return false;
        }

        // Mock/Completed 필터
        if (activeFilter === "mock") {
          return "implementationStatus" in endpoint;
        } else {
          return !("implementationStatus" in endpoint);
        }
      });

      if (groupEndpoints.length > 0) {
        filtered[group] = groupEndpoints;
      }
    });

    return filtered;
  }, [searchQuery, activeFilter]);

  return (
    <div className="h-full flex flex-col bg-white dark:bg-gray-800 transition-colors">
      {/* 헤더 */}
      <div className="p-4 border-b dark:border-gray-700">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              API 엔드포인트
            </h2>
          </div>
          <div className="flex items-center gap-2">
            {/* 모바일 닫기 버튼 */}
            <button
              onClick={toggle}
              className="lg:hidden p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
              aria-label="사이드바 닫기"
            >
              <svg
                className="w-5 h-5 text-gray-600 dark:text-gray-300"
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
              className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
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
                  className="w-5 h-5 text-gray-600"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z" />
                </svg>
              )}
            </button>
          </div>
        </div>

        {/* 검색창 */}
        <div className="relative mb-3">
          <svg
            className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400"
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
            className="w-full pl-10 pr-8 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery("")}
              className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
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

        {/* 필터 탭 */}
        <StatusFilter
          activeFilter={activeFilter}
          onFilterChange={setActiveFilter}
        />
      </div>

      {/* 엔드포인트 목록 */}
      <div className="flex-1 overflow-y-auto">
        {Object.keys(filteredEndpoints).length > 0 ? (
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
          <div className="p-4 text-center text-gray-500 dark:text-gray-400 text-sm">
            엔드포인트가 없습니다
          </div>
        )}
      </div>

      {/* 추가 버튼 */}
      <div className="p-4 border-t dark:border-gray-700">
        <button className="w-full bg-black dark:bg-gray-700 text-white py-2 px-4 rounded-lg hover:bg-gray-800 dark:hover:bg-gray-600 transition-colors">
          + Add
        </button>
      </div>
    </div>
  );
}
