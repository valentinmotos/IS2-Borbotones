import { test } from "node:test";
import assert from "node:assert/strict";
import { AxiosError } from "axios";
import { ApiError, createHttpClient, createTokenStore } from "../src/index.js";

function store() {
  const values = new Map();
  const storage = {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  };
  return { storage, tokens: createTokenStore(storage, "token") };
}

const success = (config) =>
  Promise.resolve({
    data: {},
    status: 200,
    statusText: "OK",
    headers: {},
    config,
  });
const failure = (status, data) => (config) =>
  Promise.reject(
    new AxiosError(
      "Error",
      "ERR_BAD_RESPONSE",
      config,
      {},
      { status, data, config },
    ),
  );

test("el interceptor lee el token actual en cada petición y respeta la URL configurada", async () => {
  const { tokens } = store();
  const requests = [];
  const http = createHttpClient({
    baseURL: "https://api.example.test/api",
    tokenStore: tokens,
    adapter: (config) => {
      requests.push(config);
      return success(config);
    },
  });
  await http.get("/productos");
  tokens.set("primero");
  await http.get("/productos");
  tokens.set("renovado");
  await http.get("/productos");
  tokens.clear();
  await http.get("/productos");
  assert.equal(requests[0].baseURL, "https://api.example.test/api");
  assert.equal(requests[0].headers.get("Authorization"), undefined);
  assert.equal(requests[1].headers.get("Authorization"), "Bearer primero");
  assert.equal(requests[2].headers.get("Authorization"), "Bearer renovado");
  assert.equal(requests[3].headers.get("Authorization"), undefined);
});

test("las sesiones de los clientes permanecen separadas", () => {
  const { storage } = store();
  const admin = createTokenStore(storage, "dulcemayor.admin.token");
  const publicStore = createTokenStore(storage, "dulcemayor.public.token");
  admin.set("admin");
  publicStore.set("minorista");
  admin.clear();
  assert.equal(admin.get(), null);
  assert.equal(publicStore.get(), "minorista");
});

test("un 401 elimina el token y notifica una sola vez por respuesta", async () => {
  const { tokens } = store();
  tokens.set("vencido");
  let notifications = 0;
  const http = createHttpClient({
    tokenStore: tokens,
    onUnauthorized: () => notifications++,
    adapter: failure(401, { mensaje: "Sesión vencida.", campo: null }),
  });
  await assert.rejects(
    http.get("/pedidos"),
    (error) =>
      error instanceof ApiError &&
      error.status === 401 &&
      error.message === "Sesión vencida.",
  );
  assert.equal(tokens.get(), null);
  assert.equal(notifications, 1);
});

test("un 403 conserva el token y los errores de formulario mantienen mensaje y campo", async () => {
  const { tokens } = store();
  tokens.set("valido");
  const http = createHttpClient({
    tokenStore: tokens,
    adapter: failure(403, { mensaje: "No tenés permiso.", campo: "rol" }),
  });
  await assert.rejects(http.get("/usuarios"), {
    status: 403,
    message: "No tenés permiso.",
    campo: "rol",
  });
  assert.equal(tokens.get(), "valido");
});

test("skipAuth permite iniciar sesión sin enviar ni borrar una sesión existente", async () => {
  const { tokens } = store();
  tokens.set("existente");
  const http = createHttpClient({
    tokenStore: tokens,
    adapter: (config) => {
      assert.equal(config.headers.get("Authorization"), undefined);
      return failure(401, {})(config);
    },
  });
  await assert.rejects(http.post("/auth/login", {}, { skipAuth: true }), {
    status: 401,
  });
  assert.equal(tokens.get(), "existente");
});

test("un 401 de una solicitud anterior conserva el token renovado", async () => {
  const { tokens } = store();
  tokens.set("anterior");
  let notifications = 0;
  const http = createHttpClient({
    tokenStore: tokens,
    onUnauthorized: () => notifications++,
    adapter: (config) => {
      tokens.set("renovado");
      return failure(401, {})(config);
    },
  });
  await assert.rejects(http.get("/pedidos"), { status: 401 });
  assert.equal(tokens.get(), "renovado");
  assert.equal(notifications, 0);
});

test("errores de red y respuestas sin contrato tienen un mensaje seguro en español", async () => {
  const http = createHttpClient({
    adapter: () => Promise.reject(new Error("detalle interno")),
  });
  await assert.rejects(http.get("/productos"), {
    status: 0,
    message: "No pudimos conectar con el servidor. Intentá nuevamente.",
  });
  const other = createHttpClient({
    adapter: failure(500, "<html>error interno</html>"),
  });
  await assert.rejects(other.get("/productos"), {
    status: 500,
    message: "No pudimos completar la solicitud.",
  });
});

test("peticiones canceladas conservan su tipo y no invalidan la sesión", async () => {
  const { tokens } = store();
  tokens.set("valido");
  const controller = new AbortController();
  controller.abort();
  const http = createHttpClient({ tokenStore: tokens, adapter: success });
  await assert.rejects(http.get("/productos", { signal: controller.signal }), {
    code: "ERR_CANCELED",
  });
  assert.equal(tokens.get(), "valido");
});
