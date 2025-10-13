import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: '/demoapigen/',  // 중요: assets 경로가 /demoapigen/assets/로 설정됨
  build: {
    outDir: '../src/main/resources/static/demoapigen',
    emptyOutDir: true
  },
  server: {
    proxy: {
      '/demoapigen/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
