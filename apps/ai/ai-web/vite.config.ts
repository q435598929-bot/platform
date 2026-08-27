import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5175,
    proxy: {
      '/api': 'http://localhost:8081',
      '/platform-api': { target: 'http://localhost:9090', rewrite: path => path.replace(/^\/platform-api/, '/api') }
    }
  }
})
