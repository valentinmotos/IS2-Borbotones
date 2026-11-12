import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import {
  Alert,
  DataTable,
} from "../../../../../packages/design/components.jsx";
import { useSession } from "../../../../../packages/auth/session.js";
import { api } from "../../../lib/api.js";
import PuntosVentaMap from "../components/PuntosVentaMap.jsx";
import PuntoVentaForm from "../components/PuntoVentaForm.jsx";
import "../styles.css";

// E2-06: puntos de venta por zona (departamento) en mapa y tabla. Solo el preventor da de alta.

const columnas = [
  { key: "nombreComercio", label: "Comercio" },
  {
    key: "direccion",
    label: "Dirección",
    render: (p) => `${p.calle} ${p.numeracion}, ${p.localidad}`,
  },
  { key: "minorista", label: "Minorista", render: (p) => p.nombreMinorista },
  {
    key: "mapa",
    label: "Ubicación",
    render: (p) => (
      <a
        href={`https://www.openstreetmap.org/?mlat=${p.latitud}&mlon=${p.longitud}#map=17/${p.latitud}/${p.longitud}`}
        target="_blank"
        rel="noreferrer"
      >
        Abrir mapa
      </a>
    ),
  },
];

export default function PuntosVentaPage() {
  const { user } = useSession();
  const esPreventor = user?.rol === "PREVENTOR";
  const [puntos, setPuntos] = useState(null);
  const [departamentos, setDepartamentos] = useState([]);
  const [minoristas, setMinoristas] = useState([]);
  const [zona, setZona] = useState("");
  const [version, setVersion] = useState(0);
  const [aviso, setAviso] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    const signal = controller.signal;
    const pedidos = [api.get("/departamentos", { signal })];
    if (esPreventor)
      pedidos.push(api.get("/puntos-de-venta/minoristas", { signal }));
    Promise.all(pedidos).then(
      ([deps, mins]) => {
        if (signal.aborted) return;
        setDepartamentos(deps.data);
        setMinoristas(mins?.data ?? []);
      },
      (failure) => {
        if (!signal.aborted)
          setAviso({ tono: "danger", texto: failure.message });
      },
    );
    return () => controller.abort();
  }, [esPreventor]);

  useEffect(() => {
    const controller = new AbortController();
    api
      .get("/puntos-de-venta", {
        params: zona ? { departamentoId: zona } : {},
        signal: controller.signal,
      })
      .then(
        ({ data }) => {
          if (!controller.signal.aborted) setPuntos(data);
        },
        (failure) => {
          if (!controller.signal.aborted)
            setAviso({ tono: "danger", texto: failure.message });
        },
      );
    return () => controller.abort();
  }, [zona, version]);

  function puntoCreado(creado) {
    setAviso({
      tono: "success",
      texto: `Se agregó ${creado.nombreComercio} al mapa de ${creado.departamento}.`,
    });
    setZona(creado.idDepartamento);
    setVersion((v) => v + 1);
  }

  const lista = puntos ?? [];

  return (
    <div className="pv-page">
      <header className="page-head">
        <div>
          <p className="eyebrow">Pedidos</p>
          <h1>Puntos de venta</h1>
          <p className="lead">
            {esPreventor
              ? "Registrá comercios y consultá los puntos ubicados en cada zona."
              : "Consultá los comercios registrados por los preventores en cada zona."}
          </p>
        </div>
        {esPreventor && (
          <div className="page-head-actions">
            <a className="dm-button dm-button-primary" href="#alta-punto-venta">
              <Plus size={16} aria-hidden="true" />
              Nuevo punto de venta
            </a>
          </div>
        )}
      </header>
      {aviso && (
        <Alert tone={aviso.tono} onDismiss={() => setAviso(null)}>
          {aviso.texto}
        </Alert>
      )}
      <section
        className={`pv-layout${esPreventor ? " pv-layout-alta" : ""}`}
        aria-label="Puntos de venta por zona"
      >
        <div className="panel pv-list">
          <div className="panel-head">
            <div className="pv-list-title">
              <h2>Comercios por zona</h2>
              <p>
                {puntos === null
                  ? "Cargando…"
                  : lista.length === 1
                    ? "1 comercio en el mapa"
                    : `${lista.length} comercios en el mapa`}
              </p>
            </div>
          </div>
          <div className="toolbar pv-zone">
            <label className="dm-field">
              <span>Zona</span>
              <select
                id="pv-zona"
                className="dm-input"
                value={zona}
                onChange={(e) => setZona(e.target.value)}
              >
                <option value="">Todas las zonas</option>
                {departamentos.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.nombre}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <PuntosVentaMap puntos={lista} />
          <DataTable
            caption="Comercios de la zona seleccionada"
            columns={columnas}
            rows={lista}
            rowKey={(p) => p.id}
            empty={
              puntos === null
                ? "Cargando puntos de venta…"
                : "Todavía no hay comercios en esta zona."
            }
          />
        </div>
        {esPreventor && (
          <div id="alta-punto-venta" className="pv-alta">
            <PuntoVentaForm minoristas={minoristas} onCreado={puntoCreado} />
          </div>
        )}
      </section>
    </div>
  );
}
