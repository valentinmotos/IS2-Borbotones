import { createHttpClient, createTokenStore } from "@dulce-mayor/http";

export const tokenStore = createTokenStore(
  sessionStorage,
  "dulcemayor.public.token",
);
export const api = createHttpClient({
  baseURL: import.meta.env.VITE_API_URL || "/api",
  tokenStore,
  onUnauthorized: () => window.dispatchEvent(new Event("session-expired")),
});
