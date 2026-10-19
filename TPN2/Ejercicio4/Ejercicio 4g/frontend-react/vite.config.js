import { defineConfig } from 'vite';
const proxy = { '/api': { target: process.env.CLINICA_API || 'http://localhost:8080', changeOrigin: true } };
export default defineConfig({
  server: { port: 5173, strictPort: true, proxy },
  preview: { port: 4173, strictPort: true, proxy }
});
