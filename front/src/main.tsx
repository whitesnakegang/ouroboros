import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AppProvider } from "@/app/providers/AppProvider";
import { ExplorerPage } from "@/pages/ExplorerPage";
import { RootLayout } from "@/app/layouts/RootLayout";
import "@/index.css";
import "@/i18n/config";

// 초기 로드 시 저장된 테마 설정을 즉시 적용하여 깜빡임 방지 및 시스템 설정과의 충돌 방지
const applyInitialTheme = () => {
  try {
    const persistedData = localStorage.getItem("sidebar-storage");
    if (persistedData) {
      const parsed = JSON.parse(persistedData);
      const isDarkMode = parsed.state?.isDarkMode ?? false;

      if (isDarkMode) {
        document.documentElement.classList.add("dark");
        document.documentElement.style.colorScheme = "dark";
      } else {
        document.documentElement.classList.remove("dark");
        document.documentElement.style.colorScheme = "light";
      }
    }
  } catch (e) {
    // localStorage 접근 실패 시 기본값(라이트 모드) 사용
    document.documentElement.classList.remove("dark");
    document.documentElement.style.colorScheme = "light";
  }
};

// DOM이 로드되기 전에 실행
applyInitialTheme();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter
      basename={import.meta.env.MODE === "production" ? "/ouroboros" : "/"}
    >
      <AppProvider>
        <Routes>
          <Route path="/" element={<RootLayout />}>
            <Route index element={<ExplorerPage />} />
          </Route>
        </Routes>
      </AppProvider>
    </BrowserRouter>
  </StrictMode>
);
