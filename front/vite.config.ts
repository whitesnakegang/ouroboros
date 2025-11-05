import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");

  // ✅ 백엔드 리소스 폴더 절대경로
  const OUT_DIR = path.resolve(__dirname, "../backend/src/main/resources/static/ouroboros");

  return {
    plugins: [react()],
    base: "/ouroboros/",
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
        "@/app": path.resolve(__dirname, "./src/app"),
        "@/features": path.resolve(__dirname, "./src/features"),
        "@/pages": path.resolve(__dirname, "./src/pages"),
        "@/ui": path.resolve(__dirname, "./src/ui"),
        "@/lib": path.resolve(__dirname, "./src/lib"),
        "@/constants": path.resolve(__dirname, "./src/constants"),
      },
    },
    server: {
      proxy: {
        "/ouro": {
          target: env.VITE_API_BASE_URL || "http://localhost:8080",
          changeOrigin: true,
          secure: false,
        },
        "/api": {
          target: env.VITE_API_BASE_URL || "http://localhost:8080",
          changeOrigin: true,
          secure: false,
        },
      },
    },
    build: {
      outDir: OUT_DIR,          // ✅ 여기로 바로 출력
      assetsDir: "assets",      // /ouroboros/assets/** 로 정적자산 배치
      emptyOutDir: true,        // ✅ root 바깥으로 내보낼 때 필수 (Vite 5)
      // manifest: true,        // 필요하면 켜기 (서버에서 자산 매핑 쓸 때)
    },
  };
});
