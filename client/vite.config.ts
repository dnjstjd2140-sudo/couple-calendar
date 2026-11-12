import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    proxy: {
      // 백엔드(Spring Boot, :8081)로 프록시 — 배포 시에는 Nginx가 같은 역할을 함
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
