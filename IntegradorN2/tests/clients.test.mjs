import { test } from "node:test";
import assert from "node:assert/strict";
import { createServer as createHttpServer } from "node:http";
import { once } from "node:events";
import { fileURLToPath } from "node:url";
import { createElement } from "react";
import { renderToString } from "react-dom/server";
import { MemoryRouter } from "react-router-dom";
import { createServer, preview } from "vite";
import { createHttpClient } from "../packages/http/src/index.js";

const root = fileURLToPath(new URL("../", import.meta.url));

test("ambos builds sirven HTML, assets, rutas profundas y el proxy de API", async (t) => {
  // API aislada: prueba el transporte sin depender de servicios de otras etapas.
  const backend = createHttpServer((request, response) => {
    response.setHeader("Content-Type", "application/json");
    response.end(
      JSON.stringify({
        url: request.url,
        authorization: request.headers.authorization ?? null,
      }),
    );
  });
  backend.listen(0, "127.0.0.1");
  await once(backend, "listening");
  t.after(() => new Promise((resolve) => backend.close(resolve)));
  const target = `http://127.0.0.1:${backend.address().port}`;

  for (const [app, route] of [
    ["client-admin", "/guia"],
    ["client-public", "/como-comprar"],
  ]) {
    await t.test(app, async (t) => {
      const server = await preview({
        root: `${root}${app}`,
        configFile: `${root}${app}/vite.config.js`,
        preview: {
          host: "127.0.0.1",
          port: 0,
          proxy: { "/api": { target, changeOrigin: true } },
        },
      });
      t.after(() => new Promise((resolve) => server.httpServer.close(resolve)));
      const base = `http://127.0.0.1:${server.httpServer.address().port}`;
      for (const path of ["/", route, "/ruta-inexistente"]) {
        const response = await fetch(`${base}${path}`);
        assert.equal(response.status, 200);
        const html = await response.text();
        assert.match(html, /lang="es-AR"/);
        const asset = html.match(/src="([^"]+\.js)"/)[1];
        const script = await fetch(`${base}${asset}`);
        assert.equal(script.status, 200);
        assert.match(script.headers.get("content-type"), /javascript/);
      }
      const http = createHttpClient({
        baseURL: `${base}/api`,
        tokenStore: { get: () => "token-de-prueba" },
      });
      const { data } = await http.get("/prueba");
      assert.deepEqual(data, {
        url: "/api/prueba",
        authorization: "Bearer token-de-prueba",
      });
    });
  }
});

test("las rutas React renderizan su contenido y una recuperación para rutas desconocidas", async (t) => {
  for (const [app, routes] of [
    [
      "client-admin",
      [
        ["/", "Bienvenido a tu espacio."],
        ["/guia", "Un equipo. Todo un recorrido."],
        ["/inexistente", "Esta página no está en tu recorrido."],
      ],
    ],
    [
      "client-public",
      [
        ["/", "Lo dulce, también."],
        ["/empresa", "Del lado de tu negocio."],
        ["/como-comprar", "Un negocio más dulce."],
        ["/inexistente", "Nos fuimos por otro camino."],
      ],
    ],
  ]) {
    const server = await createServer({
      root: `${root}${app}`,
      configFile: `${root}${app}/vite.config.js`,
      server: { middlewareMode: true },
      appType: "custom",
    });
    t.after(() => server.close());
    const { default: App } = await server.ssrLoadModule("/src/app/App.jsx");
    for (const [path, heading] of routes) {
      const html = renderToString(
        createElement(
          MemoryRouter,
          { initialEntries: [path] },
          createElement(App),
        ),
      );
      assert.ok(
        html.includes(heading),
        `${app}${path} debe incluir ${heading}`,
      );
      assert.match(html, /Saltar al contenido/);
      assert.match(html, /Navegación principal/);
    }
    await server.close();
  }
});
