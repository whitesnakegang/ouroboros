import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AppProvider } from "@/app/providers/AppProvider";
import { ExplorerPage } from "@/pages/ExplorerPage";
import { RootLayout } from "@/app/layouts/RootLayout";
import "@/index.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter basename={import.meta.env.MODE === 'production' ? '/ouroboros' : '/'}>
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
