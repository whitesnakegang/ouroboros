import { Outlet } from "react-router-dom";
import { Sidebar } from "@/features/sidebar/components/Sidebar";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import { useEffect, useLayoutEffect, useMemo } from "react";

export function RootLayout() {
  const { isDarkMode, isOpen, toggle, setTriggerNewForm, toggleDarkMode, endpoints } = useSidebarStore();

  const handleNewApiForm = () => {
    // 새 API 폼 트리거
    if (setTriggerNewForm) {
      setTriggerNewForm(true);
    }
  };

  // 전체 진행률 계산 (REST + WS)
  const { totalEndpoints, completedEndpoints, progressPercentage } = useMemo(() => {
    let total = 0;
    let completed = 0;

    Object.values(endpoints).forEach((groupEndpoints) => {
      groupEndpoints.forEach((endpoint) => {
        // REST와 WebSocket만 카운트
        if (endpoint.protocol === "REST" || endpoint.protocol === "WebSocket") {
          total++;
          if (endpoint.progress?.toLowerCase() === "completed") {
            completed++;
          }
        }
      });
    });

    const percentage = total > 0 ? Math.round((completed / total) * 100) : 0;

    return {
      totalEndpoints: total,
      completedEndpoints: completed,
      progressPercentage: percentage,
    };
  }, [endpoints]);

  // 다크 모드 설정 적용 (초기 로드 시 즉시 적용 및 변경 시)
  useLayoutEffect(() => {
    if (isDarkMode) {
      document.documentElement.classList.add("dark");
      document.documentElement.style.colorScheme = "dark";
    } else {
      document.documentElement.classList.remove("dark");
      document.documentElement.style.colorScheme = "light";
    }
  }, [isDarkMode]);

  // 초기 로드 시 다크모드 상태 동기화 (크롬 테마와 무관하게)
  useEffect(() => {
    // localStorage에서 저장된 다크모드 상태 확인
    const stored = localStorage.getItem("sidebar-store");
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        if (parsed.state?.isDarkMode !== undefined) {
          if (parsed.state.isDarkMode) {
            document.documentElement.classList.add("dark");
            document.documentElement.style.colorScheme = "dark";
          } else {
            document.documentElement.classList.remove("dark");
            document.documentElement.style.colorScheme = "light";
          }
        }
      } catch (_e) {
        // localStorage 파싱 실패 시 무시
      }
    }
  }, []);

  return (
    <div className="h-screen flex flex-col bg-white dark:bg-[#0D1117] transition-colors">
      <header className="border-b border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#0D1117] px-4 py-2 flex items-center justify-between">
        <div className="flex items-center gap-3">
          {/* 모바일 메뉴 버튼 */}
          <button
            onClick={toggle}
            className="lg:hidden p-2 rounded-md hover:bg-gray-100 dark:hover:bg-[#161B22] transition-colors"
            aria-label="메뉴 토글"
          >
            <svg
              className="w-6 h-6 text-gray-600 dark:text-[#8B949E]"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 6h16M4 12h16M4 18h16"
              />
            </svg>
          </button>
          <h1 className="text-xl font-bold text-gray-900 dark:text-[#E6EDF3]">
            Ouroboros API
          </h1>
        </div>
        <div className="flex items-center gap-4">
          {/* 진행률 표시 */}
          <div className="flex items-center gap-3">
            <div className="text-right">
              <div className="text-xs text-gray-600 dark:text-[#8B949E]">
                {completedEndpoints}/{totalEndpoints} 완료
              </div>
            </div>
            <div className="w-24 h-2 bg-gray-200 dark:bg-[#161B22] border border-gray-300 dark:border-[#2D333B] rounded-md overflow-hidden">
              <div
                className="h-full bg-[#2563EB] transition-all duration-500 ease-out"
                style={{ width: `${progressPercentage}%` }}
              />
            </div>
            <span className="text-xs font-medium text-gray-900 dark:text-[#E6EDF3] min-w-[2.5rem]">
              {progressPercentage}%
            </span>
          </div>
          <button
            onClick={toggleDarkMode}
            className="p-2 rounded-md hover:bg-gray-100 dark:hover:bg-[#161B22] transition-colors"
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
      </header>
      <div className="flex flex-1 overflow-hidden relative">
        {/* 오버레이 (모바일용만) */}
        {isOpen && (
          <div
            className="lg:hidden fixed inset-0 bg-black bg-opacity-50 z-40"
            onClick={toggle}
          />
        )}

        {/* 사이드바 */}
        <aside
          className={`fixed lg:static inset-y-0 left-0 z-50 lg:z-auto w-80 border-r border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#0D1117] transition-transform duration-300 transform lg:transform-none ${
            isOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"
          }`}
        >
          <Sidebar onAddNew={handleNewApiForm} />
        </aside>

        {/* 메인 콘텐츠 */}
        <main className="flex-1 overflow-auto bg-white dark:bg-[#0D1117] min-w-0 w-full">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
