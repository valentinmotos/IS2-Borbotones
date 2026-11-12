import { useEffect, useState } from "react";
import { LocateFixed, MapPin } from "lucide-react";
import { Alert } from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";
import RutaMap from "./RutaMap.jsx";

const kilometros = new Intl.NumberFormat("es-AR", {
  maximumFractionDigits: 1,
});

/** Posición del GPS del celular; null si el navegador no la da (permiso denegado, sin HTTPS, etc.). */
function ubicacionActual() {
  if (!navigator.geolocation) return Promise.resolve(null);
  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      ({ coords }) =>
        resolve({ latitud: coords.latitude, longitud: coords.longitude }),
      () => resolve(null),
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 60_000 },
    );
  });
}

/**
 * E5-01: ruta del día del preventor. Se recarga cada vez que cambia `version` (al cerrar un
 * pedido) y propone el próximo punto más cercano desde la posición actual. Sin GPS, el servidor
 * parte del último punto visitado (`ultimoPuntoId` sirve para dibujar el tramo desde ahí).
 */
export default function MiRuta({ version, ultimoPuntoId, onElegir }) {
  const [ruta, setRuta] = useState(null);
  const [posicion, setPosicion] = useState(null);
  const [error, setError] = useState(null);
  // Cada consulta tiene una clave; está cargando hasta que responde la consulta vigente.
  const [solicitud, setSolicitud] = useState(0);
  const [respondida, setRespondida] = useState(null);
  const clave = `${version}-${solicitud}`;
  const cargando = respondida !== clave;

  useEffect(() => {
    const controller = new AbortController();
    (async () => {
      const actual = await ubicacionActual();
      if (controller.signal.aborted) return;
      try {
        const { data } = await api.get("/preventa/ruta", {
          params: actual ?? {},
          signal: controller.signal,
        });
        if (controller.signal.aborted) return;
        setPosicion(actual);
        setRuta(data);
        setError(null);
      } catch (failure) {
        if (!controller.signal.aborted) setError(failure.message);
      } finally {
        if (!controller.signal.aborted) setRespondida(clave);
      }
    })();
    return () => controller.abort();
  }, [clave]);

  const visitadas = ruta?.paradas.filter((parada) => parada.visitada).length;
  const ultima = ruta?.paradas.find(
    (parada) => parada.visitada && parada.puntoDeVentaId === ultimoPuntoId,
  );
  const siguiente = ruta?.siguiente;

  return (
    <section id="ruta-preventa" className="preventa-panel">
      <div className="preventa-encabezado">
        <div>
          <h2>Mi ruta de hoy</h2>
          {ruta && ruta.paradas.length > 0 && (
            <p>
              {visitadas} de {ruta.paradas.length} puntos visitados
            </p>
          )}
        </div>
        <button
          className="dm-button dm-button-sm"
          type="button"
          disabled={cargando}
          onClick={() => setSolicitud((previa) => previa + 1)}
        >
          <LocateFixed size={14} aria-hidden="true" />
          {cargando ? "Ubicando…" : "Actualizar ubicación"}
        </button>
      </div>
      {ruta && ruta.paradas.length > 0 && (
        <div className="ruta-progreso" aria-hidden="true">
          <span
            style={{ width: `${(visitadas / ruta.paradas.length) * 100}%` }}
          />
        </div>
      )}
      {error && <Alert tone="danger">{error}</Alert>}
      {!ruta && !error && (
        <p className="muted" role="status">
          Cargando la ruta…
        </p>
      )}
      {ruta && ruta.paradas.length === 0 && (
        <p className="preventa-vacio">
          Todavía no tenés puntos de venta asignados.
        </p>
      )}
      {ruta && ruta.paradas.length > 0 && (
        <div className="ruta-cuerpo">
          <div className="ruta-info">
            {siguiente ? (
              <div className="ruta-siguiente" aria-live="polite">
                <MapPin
                  className="ruta-siguiente-icono"
                  size={22}
                  aria-hidden="true"
                />
                <div>
                  <span>
                    {version > 0 ? "Pedido cerrado. " : ""}Próximo punto
                  </span>
                  <strong>{siguiente.nombreComercio}</strong>
                  <span>
                    {siguiente.calle} {siguiente.numeracion}
                    {siguiente.localidad && ` · ${siguiente.localidad}`}
                    {ruta.distanciaKm != null &&
                      ` · a ${kilometros.format(ruta.distanciaKm)} km`}
                  </span>
                </div>
                <button
                  className="dm-button dm-button-primary dm-button-lg"
                  type="button"
                  onClick={() => onElegir(siguiente.puntoDeVentaId)}
                >
                  Cargar pedido aquí
                </button>
              </div>
            ) : (
              <Alert tone="success">
                Visitaste todos los puntos de la ruta de hoy.
              </Alert>
            )}
            {!posicion && siguiente && (
              <p className="ruta-nota">
                {visitadas > 0
                  ? "No tenemos tu ubicación: el recorrido parte del último punto visitado."
                  : "No tenemos tu ubicación: el recorrido empieza por el primer punto pendiente."}
              </p>
            )}
          </div>
          <RutaMap ruta={ruta} posicion={posicion} origen={ultima} />
        </div>
      )}
    </section>
  );
}
