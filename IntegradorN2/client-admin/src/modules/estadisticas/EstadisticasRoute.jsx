import { lazy, Suspense } from "react";

const EstadisticasPage = lazy(() => import("./EstadisticasPage.jsx"));

export default function EstadisticasRoute() {
  return (
    <Suspense fallback={<p role="status">Cargando estadísticas…</p>}>
      <EstadisticasPage />
    </Suspense>
  );
}
