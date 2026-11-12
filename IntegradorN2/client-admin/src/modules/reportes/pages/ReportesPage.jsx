import { useEffect, useMemo, useState } from "react";
import { Alert, DataTable, Kpi, StatusBadge } from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";
import { estadosPedido, moneda } from "../../almacen/formato.js";
import "../styles.css";

const hoy = new Date();
const isoLocal = (fecha) => {
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, "0");
  const dia = String(fecha.getDate()).padStart(2, "0");
  return `${anio}-${mes}-${dia}`;
};
const fechaDesde = `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, "0")}-01`;
const fechaHasta = isoLocal(hoy);

function parametros(filtros) {
  const query = new URLSearchParams({ desde: filtros.desde, hasta: filtros.hasta });
  if (filtros.departamentoId) query.set("departamentoId", filtros.departamentoId);
  if (filtros.preventorId) query.set("preventorId", filtros.preventorId);
  return query.toString();
}

function fecha(valor) {
  if (!valor) return "—";
  const [anio, mes, dia] = valor.slice(0, 10).split("-");
  return `${dia}/${mes}/${anio}`;
}

function fechaHora(valor) {
  return valor ? `${fecha(valor)} ${valor.slice(11, 16)}` : "—";
}

function descargar(respuesta, extension, filtros) {
  const url = URL.createObjectURL(respuesta.data);
  const enlace = document.createElement("a");
  enlace.href = url;
  enlace.download = `reporte-ventas-${filtros.desde}-${filtros.hasta}.${extension}`;
  enlace.click();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

const columnasResumen = [
  { key: "departamento", label: "Departamento" },
  { key: "preventor", label: "Preventor" },
  { key: "cantidadVentas", label: "Ventas" },
  { key: "montoTotal", label: "Total facturado", render: (fila) => moneda(fila.montoTotal) },
];

const columnasDetalle = [
  { key: "numeroFactura", label: "Factura", render: (fila) => String(fila.numeroFactura).padStart(8, "0") },
  { key: "fechaFactura", label: "Fecha", render: (fila) => fecha(fila.fechaFactura) },
  { key: "numeroPedido", label: "Pedido", render: (fila) => String(fila.numeroPedido).padStart(8, "0") },
  { key: "fechaPedido", label: "Fecha del pedido", render: (fila) => fechaHora(fila.fechaPedido) },
  { key: "cliente", label: "Cliente" },
  { key: "puntoDeVenta", label: "Comercio" },
  { key: "departamento", label: "Departamento" },
  { key: "preventor", label: "Preventor" },
  {
    key: "estadoEnvio",
    label: "Estado de envío",
    render: (fila) => {
      const [etiqueta, tono] = estadosPedido[fila.estadoEnvio] ?? [fila.estadoEnvio, "neutral"];
      return <StatusBadge tone={tono}>{etiqueta}</StatusBadge>;
    },
  },
  { key: "monto", label: "Monto", render: (fila) => moneda(fila.monto) },
];

export default function ReportesPage() {
  const [filtros, setFiltros] = useState({
    desde: fechaDesde,
    hasta: fechaHasta,
    departamentoId: "",
    preventorId: "",
  });
  const [aplicados, setAplicados] = useState(filtros);
  const [opciones, setOpciones] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [errorFiltros, setErrorFiltros] = useState("");
  const [errorExportacion, setErrorExportacion] = useState("");
  const [descargando, setDescargando] = useState("");
  const queryReporte = parametros(aplicados);

  useEffect(() => {
    const controller = new AbortController();
    api.get("/reportes/filtros", { signal: controller.signal }).then(
      ({ data }) => setOpciones(data),
      (e) => {
        if (!controller.signal.aborted) setErrorFiltros(e.message || "No se pudieron cargar los filtros.");
      },
    );
    return () => controller.abort();
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    api.get(`/reportes/ventas?${queryReporte}`, { signal: controller.signal }).then(
      ({ data }) => {
        if (!controller.signal.aborted) setResultado({ query: queryReporte, data });
      },
      (e) => {
        if (!controller.signal.aborted) {
          setResultado({ query: queryReporte, error: e.message || "No se pudo cargar el reporte." });
        }
      },
    );
    return () => controller.abort();
  }, [queryReporte]);

  const estadoActual = resultado?.query === queryReporte ? resultado : null;
  const reporte = estadoActual?.data ?? null;
  const cargando = !estadoActual;
  const error = estadoActual?.error || errorFiltros || errorExportacion;

  const validacion = useMemo(() => {
    if (!filtros.desde || !filtros.hasta) return "Indicá las dos fechas del período.";
    if (filtros.desde > filtros.hasta) return "La fecha desde no puede ser posterior a hasta.";
    return "";
  }, [filtros]);

  async function exportar(formato) {
    setDescargando(formato);
    setErrorExportacion("");
    try {
      const respuesta = await api.get(`/reportes/ventas/${formato}?${parametros(aplicados)}`, {
        responseType: "blob",
      });
      descargar(respuesta, formato === "excel" ? "xlsx" : "pdf", aplicados);
    } catch (e) {
      setErrorExportacion(e.message || `No se pudo generar el archivo ${formato.toUpperCase()}.`);
    } finally {
      setDescargando("");
    }
  }

  return (
    <>
      <header className="dm-page-header">
        <div>
          <h1>Reportes de ventas</h1>
          <p>Consultá clientes, pedidos facturados, estados de envío y montos por período.</p>
        </div>
      </header>

      <section className="design-register reportes-panel" aria-labelledby="reportes-filtros">
        <h2 id="reportes-filtros">Período y filtros</h2>
        <form className="reportes-filtros" onSubmit={(evento) => {
          evento.preventDefault();
          if (!validacion) setAplicados({ ...filtros });
        }}>
          <label className="dm-field">
            <span>Desde</span>
            <input type="date" value={filtros.desde} max={filtros.hasta || undefined}
              onChange={(evento) => setFiltros({ ...filtros, desde: evento.target.value })} required />
          </label>
          <label className="dm-field">
            <span>Hasta</span>
            <input type="date" value={filtros.hasta} min={filtros.desde || undefined}
              onChange={(evento) => setFiltros({ ...filtros, hasta: evento.target.value })} required />
          </label>
          <label className="dm-field">
            <span>Departamento</span>
            <select value={filtros.departamentoId}
              onChange={(evento) => setFiltros({ ...filtros, departamentoId: evento.target.value })}>
              <option value="">Todos</option>
              {(opciones?.departamentos ?? []).map((opcion) => (
                <option key={opcion.id} value={opcion.id}>{opcion.nombre}</option>
              ))}
            </select>
          </label>
          <label className="dm-field">
            <span>Preventor</span>
            <select value={filtros.preventorId}
              onChange={(evento) => setFiltros({ ...filtros, preventorId: evento.target.value })}>
              <option value="">Todos</option>
              {(opciones?.preventores ?? []).map((opcion) => (
                <option key={opcion.id} value={opcion.id}>{opcion.nombre}</option>
              ))}
            </select>
          </label>
          <button className="dm-button" type="submit" disabled={Boolean(validacion) || cargando}>
            Aplicar filtros
          </button>
        </form>
        {validacion && <p className="reportes-validacion" role="alert">{validacion}</p>}
        {error && <Alert tone="danger">{error}</Alert>}
      </section>

      {cargando && <p role="status">Cargando reporte…</p>}
      {reporte && !cargando && (
        <>
          <section className="reportes-kpis" aria-label="Totales del período">
            <Kpi label="Ventas facturadas" value={reporte.cantidadVentas} />
            <Kpi label="Total facturado" value={moneda(reporte.montoTotal)} detail={`${fecha(reporte.desde)} al ${fecha(reporte.hasta)}`} />
          </section>

          <section className="design-register reportes-panel" aria-labelledby="reportes-resumen">
            <div className="reportes-encabezado">
              <div>
                <h2 id="reportes-resumen">Ventas por departamento y preventor</h2>
                <p>El total coincide con el detalle y con los archivos exportados.</p>
              </div>
              <div className="reportes-acciones">
                <button className="dm-button" type="button" onClick={() => exportar("pdf")}
                  disabled={Boolean(descargando)}>
                  {descargando === "pdf" ? "Generando PDF…" : "Descargar PDF"}
                </button>
                <button className="dm-button" type="button" onClick={() => exportar("excel")}
                  disabled={Boolean(descargando)}>
                  {descargando === "excel" ? "Generando Excel…" : "Exportar Excel"}
                </button>
              </div>
            </div>
            <DataTable caption="Ventas agrupadas por departamento y preventor"
              columns={columnasResumen} rows={reporte.resumen} rowKey={(fila) => `${fila.departamentoId}-${fila.preventorId}`}
              empty="No hay ventas facturadas para el período y los filtros elegidos." />
          </section>

          <section className="design-register reportes-panel" aria-labelledby="reportes-detalle">
            <h2 id="reportes-detalle">Detalle de pedidos y entregas</h2>
            <DataTable caption="Detalle de ventas facturadas"
              columns={columnasDetalle} rows={reporte.detalle} rowKey={(fila) => fila.facturaId}
              empty="No hay pedidos facturados para mostrar." />
          </section>
        </>
      )}
    </>
  );
}
