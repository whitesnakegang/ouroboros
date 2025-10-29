import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 환경변수 로드
  const env = loadEnv(mode, process.cwd(), "");

  return {
    plugins: [react()],
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
      },
    },
  };
});
