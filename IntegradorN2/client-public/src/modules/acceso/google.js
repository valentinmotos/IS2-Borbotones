import { useEffect, useState } from "react";
import { api } from "../../lib/api.js";

export const googleStart = "/api/auth/google/iniciar/google";
const sameOrigin = { baseURL: "/api" };

export function useGoogleAvailable() {
  const [available, setAvailable] = useState(false);
  useEffect(() => {
    const controller = new AbortController();
    api
      .get("/publico/auth/proveedores", {
        ...sameOrigin,
        skipAuth: true,
        signal: controller.signal,
      })
      .then(
        ({ data }) => setAvailable(data.google === true),
        () => {},
      );
    return () => controller.abort();
  }, []);
  return available;
}

export async function googleStatus(signal) {
  const { data } = await api.get("/auth/google/estado", {
    ...sameOrigin,
    signal,
  });
  return data;
}

export async function googlePost(action, status, body, skipAuth = false) {
  const headers = { "X-CSRF-TOKEN": status.csrf };
  if (status.flujo) headers["X-Google-Flow"] = status.flujo;
  const { data } = await api.post(`/auth/google/${action}`, body, {
    ...sameOrigin,
    headers,
    skipAuth,
  });
  return data;
}
