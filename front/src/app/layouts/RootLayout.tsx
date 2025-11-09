import { Outlet } from "react-router-dom";
import { Sidebar } from "@/features/sidebar/components/Sidebar";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import { useEffect, useLayoutEffect } from "react";

export function RootLayout() {
  const { isDarkMode, isOpen, toggle, setTriggerNewForm } = useSidebarStore();

  const handleNewApiForm = () => {
    // 새 API 폼 트리거
    if (setTriggerNewForm) {
      setTriggerNewForm(true);
    }
  };

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
