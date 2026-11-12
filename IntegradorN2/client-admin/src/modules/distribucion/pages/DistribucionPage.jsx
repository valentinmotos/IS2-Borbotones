import { useEffect, useState } from "react";
import { Check, LocateFixed, MapPin, PackageCheck } from "lucide-react";
import {
  Alert,
  ConfirmDialog,
  StatusBadge,
} from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";
import { estadosPedido } from "../../almacen/formato.js";
import RutaMap from "../../preventa/components/RutaMap.jsx";
import "../../preventa/styles.css";
import "../styles.css";

const moneda = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
});
const kilometros = new Intl.NumberFormat("es-AR", {
  maximumFractionDigits: 1,
});

/** GPS del celular; null si el navegador no lo da (igual que MiRuta de E5-01). */
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

const direccion = (item) =>
  `${item.calle ?? ""} ${item.numeracion ?? ""}${item.localidad ? ` · ${item.localidad}` : ""}`;

/**
 * E5-02: portal móvil del distribuidor. Toma los pedidos listos (facturados y pagados), muestra la
 * ruta de reparto del día en el mapa y marca cada pedido como entregado; el servidor propone el
 * siguiente punto y le avisa por correo a ese comercio.
 */
export default function DistribucionPage() {
  const [ruta, setRuta] = useState(null);
  const [listos, setListos] = useState([]);
  const [seleccion, setSeleccion] = useState(new Set());
  const [posicion, setPosicion] = useState(null);
  const [error, setError] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [ocupado, setOcupado] = useState(true);
  const [aEntregar, setAEntregar] = useState(null);

  // Cada cambio de `version` vuelve a pedir la ubicación, la ruta y los pedidos listos.
  const [version, setVersion] = useState(0);
  const recargar = () => {
    setOcupado(true);
    setVersion((previa) => previa + 1);
  };

  useEffect(() => {
    const controller = new AbortController();
    (async () => {
      const actual = await ubicacionActual();
      if (controller.signal.aborted) return;
      try {
        const [respuestaRuta, respuestaListos] = await Promise.all([
          api.get("/distribucion/ruta", {
            params: actual ?? {},
            signal: controller.signal,
          }),
          api.get("/distribucion/pedidos-listos", {
            signal: controller.signal,
          }),
        ]);
        setPosicion(actual);
        setRuta(respuestaRuta.data);
        setListos(respuestaListos.data);
        setError(null);
      } catch (failure) {
        if (!controller.signal.aborted) setError(failure.message);
      } finally {
        if (!controller.signal.aborted) setOcupado(false);
      }
    })();
    return () => controller.abort();
  }, [version]);

  const alternar = (id) =>
    setSeleccion((previa) => {
      const nueva = new Set(previa);
      if (nueva.has(id)) nueva.delete(id);
      else nueva.add(id);
      return nueva;
    });

  async function tomar() {
    setOcupado(true);
    try {
      await api.post("/distribucion/reparto", {
        pedidoIds: [...seleccion],
      });
      setAviso(`Tomaste ${seleccion.size} pedido(s) para repartir.`);
      setSeleccion(new Set());
      recargar();
    } catch (failure) {
      setError(failure.message);
      setOcupado(false);
    }
  }

  async function entregar() {
    const parada = aEntregar;
    setAEntregar(null);
    setOcupado(true);
    try {
      const { data } = await api.post(
        `/distribucion/pedidos/${parada.pedidoId}/entrega`,
        posicion ?? {},
      );
      setRuta(data.ruta);
      setAviso(
        data.avisoEnCaminoA
          ? `Pedido N° ${parada.numeroPedido} entregado. Le avisamos a ${data.avisoEnCaminoA} que vas en camino.`
          : `Pedido N° ${parada.numeroPedido} entregado.`,
      );
      setError(null);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setOcupado(false);
    }
  }

  const porId = new Map((ruta?.paradas ?? []).map((p) => [p.id, p]));
  const pendientes = (ruta?.recorridoSugerido ?? [])
    .map((id) => porId.get(id))
    .filter(Boolean);
  const entregados = (ruta?.paradas ?? []).filter((p) => p.visitada).length;
  const siguiente = ruta?.siguiente;

  return (
    <div className="preventa distribucion">
      <header className="page-head">
        <div>
          <h1>Distribución</h1>
          <p className="lead">
            Tomá los pedidos listos, seguí la ruta y marcá cada entrega en el
            comercio.
          </p>
        </div>
        <div className="page-head-actions">
          <button
            className="dm-button"
            type="button"
            disabled={ocupado}
            onClick={recargar}
          >
            <LocateFixed size={16} aria-hidden="true" />
            {ocupado ? "Actualizando…" : "Actualizar ubicación"}
          </button>
        </div>
      </header>

      {error && (
        <Alert tone="danger" onDismiss={() => setError(null)}>
          {error}
        </Alert>
      )}
      {aviso && (
        <Alert tone="success" onDismiss={() => setAviso(null)}>
          {aviso}
        </Alert>
      )}

      <section className="preventa-panel" aria-labelledby="titulo-ruta">
        <div className="preventa-encabezado">
          <div>
            <h2 id="titulo-ruta">Mi ruta de hoy</h2>
            {ruta && ruta.paradas.length > 0 && (
              <p>
                {entregados} de {ruta.paradas.length} pedidos entregados
              </p>
            )}
          </div>
        </div>
        {ruta && ruta.paradas.length > 0 && (
          <div className="ruta-progreso" aria-hidden="true">
            <span
              style={{ width: `${(entregados / ruta.paradas.length) * 100}%` }}
            />
          </div>
        )}
        {!ruta && !error && (
          <p className="muted" role="status">
            Cargando la ruta…
          </p>
        )}
        {ruta && ruta.paradas.length === 0 && (
          <p className="preventa-vacio">
            Todavía no tomaste pedidos para repartir. Elegilos en «Listos para
            reparto».
          </p>
        )}
        {ruta && ruta.paradas.length > 0 && (
          <div className="ruta-cuerpo distribucion-ruta">
            <div className="ruta-info">
              {siguiente ? (
                <div className="ruta-siguiente" aria-live="polite">
                  <MapPin
                    className="ruta-siguiente-icono"
                    size={22}
                    aria-hidden="true"
                  />
                  <div>
                    <span>Próximo punto</span>
                    <strong>{siguiente.nombreComercio}</strong>
                    <span>
                      {direccion(siguiente)}
                      {ruta.distanciaKm != null &&
                        ` · a ${kilometros.format(ruta.distanciaKm)} km`}
                    </span>
                  </div>
                </div>
              ) : (
                <Alert tone="success">
                  Entregaste todos los pedidos de la ruta de hoy.
                </Alert>
              )}
              <ol className="distribucion-lista distribucion-paradas">
                {pendientes.map((parada) => (
                  <li key={parada.id} className="distribucion-item">
                    <div>
                      <strong>{parada.nombreComercio}</strong>
                      <span>
                        Pedido N° {parada.numeroPedido} · {direccion(parada)}
                      </span>
                    </div>
                    <button
                      className="dm-button dm-button-primary"
                      type="button"
                      disabled={ocupado}
                      onClick={() => setAEntregar(parada)}
                    >
                      <Check size={18} aria-hidden="true" />
                      Entregado
                    </button>
                  </li>
                ))}
              </ol>
            </div>
            <RutaMap
              ruta={ruta}
              posicion={posicion}
              etiquetaHecha="Entregado"
            />
          </div>
        )}
      </section>

      <section className="preventa-panel" aria-labelledby="titulo-listos">
        <div className="preventa-encabezado">
          <div>
            <h2 id="titulo-listos">Listos para reparto</h2>
            <p>Marcá los pedidos que vas a llevar y tomalos para tu ruta.</p>
          </div>
        </div>
        {listos.length === 0 ? (
          <p className="preventa-vacio">
            <PackageCheck size={22} aria-hidden="true" />
            No hay pedidos facturados y pagados esperando reparto.
          </p>
        ) : (
          <>
            <ul className="distribucion-lista distribucion-listos">
              {listos.map((pedido) => (
                <li key={pedido.id} className="distribucion-item">
                  <label>
                    <input
                      type="checkbox"
                      checked={seleccion.has(pedido.id)}
                      onChange={() => alternar(pedido.id)}
                    />
                    <span>
                      <strong>{pedido.nombreComercio}</strong>
                      <span>
                        Pedido N° {pedido.numeroPedido} · {direccion(pedido)}
                      </span>
                    </span>
                  </label>
                  <span className="distribucion-monto">
                    {moneda.format(pedido.total)}
                  </span>
                </li>
              ))}
            </ul>
            <div className="dm-actions distribucion-acciones">
              <button
                className="dm-button"
                type="button"
                disabled={ocupado}
                onClick={() =>
                  setSeleccion(new Set(listos.map((pedido) => pedido.id)))
                }
              >
                Elegir todos
              </button>
              <button
                className="dm-button dm-button-primary"
                type="button"
                disabled={ocupado || seleccion.size === 0}
                onClick={tomar}
              >
                Tomar para reparto ({seleccion.size})
              </button>
            </div>
          </>
        )}
      </section>

      <ConfirmDialog
        open={aEntregar != null}
        title="Confirmar entrega"
        confirmLabel="Entregado"
        onConfirm={entregar}
        onCancel={() => setAEntregar(null)}
      >
        {aEntregar && (
          <p>
            ¿Entregaste el pedido N° {aEntregar.numeroPedido} en{" "}
            {aEntregar.nombreComercio}?{" "}
            <StatusBadge tone={estadosPedido.ENTREGADO[1]}>
              {estadosPedido.ENTREGADO[0]}
            </StatusBadge>
          </p>
        )}
      </ConfirmDialog>
    </div>
  );
}
