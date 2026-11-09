import { preview } from "vite";
import { fileURLToPath } from "node:url";

const running = [];
const root = fileURLToPath(new URL("../", import.meta.url));

async function close() {
  await Promise.all(
    running.map(
      ({ httpServer }) => new Promise((resolve) => httpServer.close(resolve)),
    ),
  );
}

try {
  for (const [app, port] of [
    ["client-admin", 5173],
    ["client-public", 5174],
  ]) {
    const server = await preview({
      root: `${root}${app}`,
      configFile: `${root}${app}/vite.config.js`,
      preview: { host: "127.0.0.1", port, strictPort: true },
    });
    running.push(server);
    console.log(`${app}: http://localhost:${port}/`);
  }
} catch (error) {
  await close();
  console.error(`No se pudieron iniciar los clientes: ${error.message}`);
  process.exitCode = 1;
}

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => {
    void close().then(() => process.exit(0));
  });
}
