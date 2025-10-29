import { Outlet } from "react-router-dom";
import { Sidebar } from "@/features/sidebar/components/Sidebar";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import { useEffect } from "react";

export function RootLayout() {
  const { isDarkMode, isOpen, toggle, setTriggerNewForm } = useSidebarStore();

  const handleNewApiForm = () => {
    // 새 API 폼 트리거
    if (setTriggerNewForm) {
      setTriggerNewForm(true);
    }
  };

  useEffect(() => {
    if (isDarkMode) {
      document.documentElement.classList.add("dark");
    } else {
      document.documentElement.classList.remove("dark");
    }
  }, [isDarkMode]);

  return (
    <div className="h-screen flex flex-col bg-white dark:bg-gray-900 transition-colors">
      <header className="border-b bg-white dark:bg-gray-800 dark:border-gray-700 px-4 py-2 flex items-center justify-between">
        <div className="flex items-center gap-3">
          {/* 모바일 메뉴 버튼 */}
          <button
            onClick={toggle}
            className="lg:hidden p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            aria-label="메뉴 토글"
          >
            <svg
              className="w-6 h-6 text-gray-600 dark:text-gray-300"
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
          <h1 className="text-xl font-bold text-gray-900 dark:text-white">
            Ouroboros API
          </h1>
        </div>
      </header>
      <div className="flex flex-1 overflow-hidden relative">
        {/* 오버레이 (모바일용) */}
        {isOpen && (
          <div
            className="lg:hidden fixed inset-0 bg-black bg-opacity-50 z-40"
            onClick={toggle}
          />
        )}

        {/* 사이드바 */}
        <aside
          className={`fixed lg:static inset-y-0 left-0 z-50 lg:z-auto w-80 border-r dark:border-gray-700 bg-white dark:bg-gray-800 transition-transform duration-300 transform ${
            isOpen ? "translate-x-0" : "-translate-x-full"
          }`}
        >
          <Sidebar onAddNew={handleNewApiForm} />
        </aside>

        {/* 메인 콘텐츠 */}
        <main className="flex-1 overflow-auto bg-gray-50 dark:bg-gray-900">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
