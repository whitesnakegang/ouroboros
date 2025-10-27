import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// https://vite.dev/config/
export default defineConfig({
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
});
