"use strict";

// Este cliente del navegador consume los mismos DTOs/endpoints que TinderRestClient en Java.
// Las credenciales viven solo en memoria y se borran al cerrar sesion o recargar la pagina.
const $ = (id) => document.getElementById(id);
const state = { user: null, auth: null, pets: [], demo: null, logs: [], photos: new Set(), pendingUpload: null, afterLogin: null };
let busy = false;

function node(tag, className, text) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text !== undefined) element.textContent = text;
  return element;
}
function redact(value) {
  if (Array.isArray(value)) return value.map(redact);
  if (value && typeof value === "object") return Object.fromEntries(Object.entries(value)
    .map(([key, item]) => [key, /clave|authorization/i.test(key) ? "***" : redact(item)]));
  return value;
}
async function api(path, { method = "GET", body, auth = state.auth, image = false } = {}) {
  const headers = {};
  if (auth) headers.Authorization = auth;
  if (body !== undefined && !(body instanceof FormData)) headers["Content-Type"] = "application/json";
  const start = performance.now();
  const entry = { path: "/api" + path, method, status: 0, headers: redact(headers),
    request: body instanceof FormData ? { archivo: body.get("archivo")?.name, formato: "multipart/form-data" } : redact(body ?? null) };
  try {
    const response = await fetch(entry.path, { method, headers,
      body: body === undefined ? undefined : body instanceof FormData ? body : JSON.stringify(body) });
    entry.status = response.status;
    let result;
    if (image && response.ok) {
      result = await response.blob();
      entry.response = { contenido: "Imagen binaria", mime: result.type, bytes: result.size };
    } else {
      const text = await response.text();
      try { result = text ? JSON.parse(text) : null; }
      catch { result = { detail: text || "Respuesta no válida" }; }
      entry.response = redact(result);
    }
    if (!response.ok) throw new Error([result?.detail || "Error HTTP " + response.status, ...(result?.errores || [])].join(" · "));
    return result;
  } catch (error) {
    if (!entry.status) {
      entry.response = { detail: "No se pudo conectar con el servidor." };
      throw new Error("No se pudo conectar con el servidor. Verificá que Spring Boot siga ejecutándose.");
    }
    throw error;
  } finally {
    entry.ms = Math.round(performance.now() - start);
    state.logs.unshift(entry);
    state.logs = state.logs.slice(0, 30);
    renderLog();
  }
}
function credentials(mail, clave) {
  const bytes = new TextEncoder().encode(mail + ":" + clave);
  return "Basic " + btoa(Array.from(bytes, (byte) => String.fromCharCode(byte)).join(""));
}

