import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, fileURLToPath(new URL(".", import.meta.url)), "");
  const proxy = {
    "/api": {
      target: env.API_PROXY_TARGET || "http://localhost:8080",
      changeOrigin: true,
    },
  };
  return { plugins: [react()], server: { proxy }, preview: { proxy } };
});
