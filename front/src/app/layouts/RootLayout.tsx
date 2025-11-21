import { Outlet } from "react-router-dom";
import { Sidebar } from "@/features/sidebar/components/Sidebar";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";

export function RootLayout() {
  const {
    isDarkMode,
    isOpen,
    toggle,
    setTriggerNewForm,
    toggleDarkMode,
    endpoints,
  } = useSidebarStore();
  const { i18n, t } = useTranslation(); // i18n과 t 모두 가져오기
  
  // 현재 언어를 i18n.language에서 직접 계산
  // t 함수를 실제로 사용하여 언어 변경 시 자동 리렌더링 보장
  const currentLanguage = useMemo(() => {
    const lang = i18n.language || localStorage.getItem("i18nextLng") || "en";
    return lang.startsWith("ko") ? "ko" : "en";
  }, [i18n.language]);

  // 언어 선택 드롭다운 상태
  const [isLanguageMenuOpen, setIsLanguageMenuOpen] = useState(false);
  const languageMenuRef = useRef<HTMLDivElement>(null);

  const changeLanguage = async (newLanguage: "ko" | "en") => {
    // localStorage에 먼저 저장 (중요: changeLanguage 전에 저장)
    localStorage.setItem("i18nextLng", newLanguage);
    
    // useTranslation에서 가져온 i18n 인스턴스 사용
    // changeLanguage는 Promise를 반환하므로 await로 완료 대기
    // t 함수를 사용하는 모든 컴포넌트가 자동으로 리렌더링됨
    await i18n.changeLanguage(newLanguage);
    setIsLanguageMenuOpen(false);
  };

  // 외부 클릭 시 드롭다운 닫기
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        languageMenuRef.current &&
        !languageMenuRef.current.contains(event.target as Node)
      ) {
        setIsLanguageMenuOpen(false);
      }
    }

    if (isLanguageMenuOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isLanguageMenuOpen]);

  const handleNewApiForm = () => {
    // 새 API 폼 트리거
    if (setTriggerNewForm) {
      setTriggerNewForm(true);
    }
  };

  // 전체 진행률 계산 (REST + WS)
  const { totalEndpoints, completedEndpoints, progressPercentage } =
    useMemo(() => {
      let total = 0;
      let completed = 0;

      Object.values(endpoints).forEach((groupEndpoints) => {
        groupEndpoints.forEach((endpoint) => {
          // REST와 WebSocket만 카운트
          if (
            endpoint.protocol === "REST" ||
            endpoint.protocol === "WebSocket"
          ) {
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
    // 전환 애니메이션을 위한 클래스 추가
    document.documentElement.style.transition = "background-color 0.5s ease-in-out, color 0.5s ease-in-out";
    
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
          // 초기 로드 시에는 애니메이션 없이 즉시 적용
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
    
    // 초기 로드 후 약간의 지연을 두고 전환 애니메이션 활성화
    const timer = setTimeout(() => {
      document.documentElement.style.transition = "background-color 0.5s ease-in-out, color 0.5s ease-in-out";
    }, 100);
    
    return () => clearTimeout(timer);
  }, []);


  return (
    <div className="h-screen flex flex-col bg-white dark:bg-[#0D1117] transition-[background-color] duration-500 ease-in-out">
      <header className="border-b border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#0D1117] px-4 py-2 flex items-center justify-between transition-[background-color,border-color] duration-500 ease-in-out">
        <div className="flex items-center gap-3">
          {/* 모바일 메뉴 버튼 */}
          <button
            onClick={toggle}
            className="lg:hidden p-2 rounded-md hover:bg-gray-100 dark:hover:bg-[#161B22] transition-colors"
            aria-label="Toggle Menu"
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
            {t("header.title")}
          </h1>
        </div>
        <div className="flex items-center gap-4">
          {/* 진행률 표시 */}
          <div className="flex items-center gap-3">
            <div className="text-right">
              <div className="text-xs text-gray-600 dark:text-[#8B949E]">
                {completedEndpoints}/{totalEndpoints} {t("header.completed")}
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
          {/* 언어 선택 드롭다운 */}
          <div className="relative" ref={languageMenuRef}>
            <button
              onClick={() => setIsLanguageMenuOpen(!isLanguageMenuOpen)}
              className="p-2 text-xl hover:opacity-70 transition-opacity"
              style={{ 
                appearance: 'none', 
                WebkitAppearance: 'none',
                background: 'transparent',
                border: 'none',
                outline: 'none',
                boxShadow: 'none',
                padding: '0.5rem',
                cursor: 'pointer'
              }}
              onMouseDown={(e) => e.preventDefault()}
              title={currentLanguage === "ko" ? t("header.switchToEnglish") : t("header.switchToKorean")}
            >
              🌐
            </button>
            {isLanguageMenuOpen && (
              <div className="absolute right-0 top-full mt-2 w-32 bg-white dark:bg-[#161B22] border border-gray-200 dark:border-[#2D333B] rounded-md shadow-lg z-50">
                <button
                  onClick={() => changeLanguage("ko")}
                  className="w-full px-4 py-2 text-left text-sm hover:bg-gray-50 dark:hover:bg-[#0D1117] transition-colors focus:outline-none focus-visible:outline-none flex items-center justify-between"
                >
                  <span className={currentLanguage === "ko" ? "text-[#2563EB] dark:text-[#58A6FF]" : "text-gray-700 dark:text-[#E6EDF3]"}>
                    한국어
                  </span>
                  {currentLanguage === "ko" && (
                    <svg
                      className="w-4 h-4 text-[#2563EB] dark:text-[#58A6FF]"
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
                  )}
                </button>
                <button
                  onClick={() => changeLanguage("en")}
                  className="w-full px-4 py-2 text-left text-sm hover:bg-gray-50 dark:hover:bg-[#0D1117] transition-colors focus:outline-none focus-visible:outline-none flex items-center justify-between"
                >
                  <span className={currentLanguage === "en" ? "text-[#2563EB] dark:text-[#58A6FF]" : "text-gray-700 dark:text-[#E6EDF3]"}>
                    English
                  </span>
                  {currentLanguage === "en" && (
                    <svg
                      className="w-4 h-4 text-[#2563EB] dark:text-[#58A6FF]"
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
                  )}
                </button>
              </div>
            )}
          </div>
          <button
            onClick={toggleDarkMode}
            className="p-2 hover:opacity-70 transition-opacity"
            style={{ 
              appearance: 'none', 
              WebkitAppearance: 'none',
              background: 'transparent',
              border: 'none',
              outline: 'none',
              boxShadow: 'none',
              padding: '0.5rem',
              cursor: 'pointer'
            }}
            onMouseDown={(e) => e.preventDefault()}
            title={isDarkMode ? t("header.switchToLightMode") : t("header.switchToDarkMode")}
          >
            {isDarkMode ? (
              <svg
                className="w-5 h-5 text-yellow-500 transition-all duration-500 ease-in-out"
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
                className="w-5 h-5 text-gray-600 dark:text-[#8B949E] transition-all duration-500 ease-in-out"
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
