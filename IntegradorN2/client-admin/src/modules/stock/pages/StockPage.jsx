import { useCallback, useEffect, useState } from "react";
import { DataTable, FormField, Kpi, StatusBadge } from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";

function StockToast({ toast, onDismiss }) {
  const [saliendo, setSaliendo] = useState(false);

  useEffect(() => {
    const salida = window.setTimeout(() => setSaliendo(true), 5200);
    const cierre = window.setTimeout(onDismiss, 5600);
    return () => {
      window.clearTimeout(salida);
      window.clearTimeout(cierre);
    };
  }, [toast.id, onDismiss]);

  return (
    <div
      className={`stock-toast${saliendo ? " stock-toast--leaving" : ""}`}
      data-tone={toast.tone}
      role={toast.tone === "danger" ? "alert" : "status"}
      aria-live={toast.tone === "danger" ? "assertive" : "polite"}
    >
      <p>{toast.message}</p>
      <button type="button" onClick={onDismiss} aria-label="Cerrar aviso">
        <svg aria-hidden="true" viewBox="0 0 20 20" width="16" height="16" fill="none">
          <path d="m5 5 10 10M15 5 5 15" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
        </svg>
      </button>
    </div>
  );
}

const numero = new Intl.NumberFormat("es-AR");
const columnasBase = [
  { key: "codigo", label: "Código" },
  { key: "nombre", label: "Producto", render: (p) => <><strong>{p.nombre}</strong><small className="stock-presentacion">{p.presentacion}</small></> },
  { key: "cantidadActual", label: "Existencias", render: (p) => numero.format(p.cantidadActual) },
  { key: "estado", label: "Estado", render: (p) => p.sinStock ? <StatusBadge tone="danger">Sin stock</StatusBadge> : <StatusBadge tone="success">Disponible</StatusBadge> },
];

export default function StockPage() {
  const [resultado, setResultado] = useState(null);
  const [version, setVersion] = useState(0);
  const [productoId, setProductoId] = useState("");
  const [tipo, setTipo] = useState("ingreso");
  const [guardando, setGuardando] = useState(false);
  const [toast, setToast] = useState(null);
  const [historial, setHistorial] = useState(null);
  const [cantidad, setCantidad] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    api.get("/stock", { signal: controller.signal }).then(({ data }) => {
      if (!controller.signal.aborted) {
        setResultado(data);
        setProductoId((actual) => actual || data[0]?.productoId || "");
      }
    }, (failure) => {
      if (!controller.signal.aborted) setToast({ id: Date.now(), tone: "danger", message: failure.message });
    });
    return () => controller.abort();
  }, [version]);

  const productos = resultado ?? [];
  const sinStock = productos.filter((p) => p.sinStock).length;
  const seleccionado = productos.find((p) => p.productoId === productoId);
  const descartarToast = useCallback(() => setToast(null), []);

  async function guardar(event) {
    event.preventDefault();
    if (guardando) return;
    const formulario = event.currentTarget;
    const data = new FormData(formulario);
      const unidades = Number(data.get("cantidad"));
    setGuardando(true);
    try {
      const payload = { productoId, observacion: data.get("observacion") || null };
      if (tipo === "ingreso") payload.cantidad = unidades;
      else payload.diferencia = unidades;
      const { data: actualizado } = await api.post(`/stock/${tipo === "ingreso" ? "ingresos" : "ajustes"}`, payload);
      setToast({ id: Date.now(), tone: "success", message: `${actualizado.nombre}: stock actualizado a ${numero.format(actualizado.cantidadActual)} unidades.` });
      setVersion((v) => v + 1);
      setCantidad("");
      if (historial?.productoId === productoId) cargarHistorial(productoId);
      formulario.reset();
    } catch (failure) { setToast({ id: Date.now(), tone: "danger", message: failure.message }); }
    finally { setGuardando(false); }
  }

  async function cargarHistorial(id) {
    setHistorial({ productoId: id, cargando: true });
    try {
      const { data } = await api.get(`/stock/${id}/movimientos`);
      setHistorial({ productoId: id, data });
    } catch (failure) {
      setHistorial({ productoId: id, error: failure.message });
      setToast({ id: Date.now(), tone: "danger", message: failure.message });
    }
  }

  const movimientos = historial?.data ?? [];
  const columnasMovimientos = [
    { key: "fechaHora", label: "Fecha", render: (m) => new Date(m.fechaHora).toLocaleString("es-AR", { dateStyle: "short", timeStyle: "short" }) },
    { key: "tipo", label: "Movimiento", render: (m) => m.tipo === "INGRESO" ? "Ingreso" : "Ajuste" },
    { key: "cantidad", label: "Variación", render: (m) => `${m.cantidad > 0 ? "+" : ""}${numero.format(m.cantidad)}` },
    { key: "observacion", label: "Observación", render: (m) => m.observacion || "—" },
  ];

  return <>
    {toast && <StockToast toast={toast} onDismiss={descartarToast} />}
    <header className="dm-page-header"><div><h1>Stock y movimientos</h1><p>Controlá las existencias y registrá ingresos o correcciones de inventario.</p></div></header>
    <dl className="stock-kpis"><Kpi label="Productos controlados" value={productos.length} /><Kpi label="Sin stock" value={sinStock} detail={sinStock ? "Requieren reposición" : "Todos tienen existencias"} /></dl>
    <section className="design-register" aria-labelledby="stock-listado">
      <h2 id="stock-listado">Existencias actuales</h2>
      <DataTable caption="Stock actual por producto" columns={columnasBase} rows={productos} rowKey={(p) => p.productoId} empty={resultado === null ? "Cargando stock…" : "No hay productos activos."} />
    </section>
    <div className="stock-grid">
      <section className="design-register" aria-labelledby="stock-movimiento">
        <h2 id="stock-movimiento">Registrar movimiento</h2>
        <form className="stock-form" onSubmit={guardar}>
          <fieldset disabled={guardando || !productos.length}>
            <label className="dm-field"><span>Producto</span><select className="dm-input" value={productoId} onChange={(e) => setProductoId(e.target.value)} required><option value="">Seleccioná un producto</option>{productos.map((p) => <option key={p.productoId} value={p.productoId}>{p.codigo} · {p.nombre} (actual: {p.cantidadActual})</option>)}</select></label>
            <label className="dm-field"><span>Tipo</span><select className="dm-input" value={tipo} onChange={(e) => setTipo(e.target.value)}><option value="ingreso">Ingreso de mercadería</option><option value="ajuste">Ajuste manual</option></select></label>
            <FormField name="cantidad" label={tipo === "ingreso" ? "Unidades ingresadas" : "Diferencia de unidades"} type="number" required min={tipo === "ingreso" ? 1 : undefined} step={1} value={cantidad} onChange={(e) => setCantidad(e.target.value)} hint={tipo === "ajuste" ? "Usá un valor positivo para sumar o negativo para descontar." : undefined} />
            {tipo === "ajuste" && seleccionado && <p className="stock-preview">Saldo resultante: <strong>{numero.format(seleccionado.cantidadActual + (Number(cantidad) || 0))}</strong></p>}
            <FormField name="observacion" label="Observación" maxLength={500} hint="Opcional · hasta 500 caracteres" />
            <button className="dm-button dm-button-primary" type="submit">{guardando ? "Guardando…" : "Registrar movimiento"}</button>
          </fieldset>
        </form>
      </section>
      <section className="design-register" aria-labelledby="stock-historial">
        <h2 id="stock-historial">Historial de movimientos</h2>
        <label className="dm-field"><span>Producto</span><select className="dm-input" value={historial?.productoId ?? ""} onChange={(e) => e.target.value && cargarHistorial(e.target.value)}><option value="">Seleccioná un producto</option>{productos.map((p) => <option key={p.productoId} value={p.productoId}>{p.codigo} · {p.nombre}</option>)}</select></label>
        <DataTable caption="Movimientos de stock" columns={columnasMovimientos} rows={movimientos} rowKey={(m) => m.id} empty={!historial ? "Elegí un producto para consultar sus movimientos." : historial.cargando ? "Cargando movimientos…" : "Todavía no hay movimientos registrados."} />
      </section>
    </div>
  </>;
}
