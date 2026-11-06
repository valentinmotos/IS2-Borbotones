import axios from "axios";

/** La instancia de cada cliente mantiene su propia sesión. Nunca guarda credenciales. */
export function createTokenStore(storage, key) {
  return {
    get: () => storage.getItem(key),
    set: (token) => storage.setItem(key, token),
    clear: () => storage.removeItem(key),
  };
}

export class ApiError extends Error {
  constructor(message, { status = 0, campo = null, cause } = {}) {
    super(message, { cause });
    this.name = "ApiError";
    this.status = status;
    this.campo = campo;
  }
}

/** Comparte transporte e interceptores, sin acoplarlos al router ni a React. */
export function createHttpClient({
  baseURL = "/api",
  tokenStore,
  onUnauthorized,
  adapter,
} = {}) {
  const http = axios.create({
    baseURL,
    timeout: 10000,
    ...(adapter && { adapter }),
  });
  http.interceptors.request.use((config) => {
    const token = tokenStore?.get();
    if (token && !config.skipAuth)
      config.headers.set("Authorization", `Bearer ${token}`);
    return config;
  });
  http.interceptors.response.use(
    (response) => response,
    (error) => {
      if (axios.isCancel(error)) return Promise.reject(error);
      const status = error.response?.status ?? 0;
      // Un 403 no cierra una sesión válida. Un login rechazado tampoco borra otra sesión.
      if (status === 401 && !error.config?.skipAuth) {
        tokenStore?.clear();
        onUnauthorized?.();
      }
      const data = error.response?.data;
      const fallback = status
        ? "No pudimos completar la solicitud."
        : "No pudimos conectar con el servidor. Intentá nuevamente.";
      return Promise.reject(
        new ApiError(
          typeof data?.mensaje === "string" ? data.mensaje : fallback,
          { status, campo: data?.campo ?? null, cause: error },
        ),
      );
    },
  );
  return http;
}
