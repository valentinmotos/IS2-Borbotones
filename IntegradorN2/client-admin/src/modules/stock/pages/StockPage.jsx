import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import {
  Alert,
  DataTable,
  FormField,
  Kpi,
  StatusBadge,
} from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";
import "../styles.css";

// E2-02: stock actual por producto, alerta de stock cero, ingresos, ajustes e historial.

const numero = new Intl.NumberFormat("es-AR");

const columnasStock = [
  { key: "codigo", label: "Código" },
  {
    key: "nombre",
    label: "Producto",
    render: (p) => (
      <>
        <strong>{p.nombre}</strong>
        <small className="stock-presentacion">{p.presentacion}</small>
      </>
    ),
  },
  {
    key: "cantidadActual",
    label: "Existencias",
    render: (p) => numero.format(p.cantidadActual),
  },
  {
    key: "estado",
    label: "Estado",
    render: (p) =>
      p.sinStock ? (
        <StatusBadge tone="danger">Sin stock</StatusBadge>
      ) : (
        <StatusBadge tone="success">Disponible</StatusBadge>
      ),
  },
];

const tiposMovimiento = {
  INGRESO: "Ingreso",
  AJUSTE: "Ajuste",
  EGRESO: "Venta",
};

const columnasMovimientos = [
  {
    key: "fechaHora",
    label: "Fecha",
    render: (m) =>
      new Date(m.fechaHora).toLocaleString("es-AR", {
        dateStyle: "short",
        timeStyle: "short",
      }),
  },
  {
    key: "tipo",
    label: "Movimiento",
    render: (m) => tiposMovimiento[m.tipo] ?? m.tipo,
  },
  {
    key: "cantidad",
    label: "Variación",
    render: (m) => `${m.cantidad > 0 ? "+" : ""}${numero.format(m.cantidad)}`,
  },
  {
    key: "observacion",
    label: "Observación",
    render: (m) => m.observacion || "—",
  },
];

export default function StockPage() {
  const [resultado, setResultado] = useState(null);
  const [version, setVersion] = useState(0);
  const [productoId, setProductoId] = useState("");
  const [tipo, setTipo] = useState("ingreso");
  const [cantidad, setCantidad] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [aviso, setAviso] = useState(null);
  const [historial, setHistorial] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    api.get("/stock", { signal: controller.signal }).then(
      ({ data }) => {
        if (controller.signal.aborted) return;
        setResultado(data);
        setProductoId((actual) => actual || data[0]?.productoId || "");
      },
      (failure) => {
        if (!controller.signal.aborted)
          setAviso({ tono: "danger", texto: failure.message });
      },
    );
    return () => controller.abort();
  }, [version]);

  const productos = resultado ?? [];
  const sinStock = productos.filter((p) => p.sinStock);
  const seleccionado = productos.find((p) => p.productoId === productoId);
  const unidades = Number(cantidad) || 0;
  const saldoResultante = seleccionado
    ? seleccionado.cantidadActual + unidades
    : null;
  const ajusteNegativo =
    tipo === "ajuste" && saldoResultante !== null && saldoResultante < 0;

  async function cargarHistorial(id) {
    setHistorial({ productoId: id, cargando: true });
    try {
      const { data } = await api.get(`/stock/${id}/movimientos`);
      setHistorial({ productoId: id, data });
    } catch (failure) {
      setHistorial({ productoId: id, data: [] });
      setAviso({ tono: "danger", texto: failure.message });
    }
  }

  async function guardar(event) {
    event.preventDefault();
    if (guardando || ajusteNegativo) return;
    const formulario = event.currentTarget;
    const observacion = new FormData(formulario).get("observacion") || null;
    const payload =
      tipo === "ingreso"
        ? { productoId, cantidad: unidades, observacion }
        : { productoId, diferencia: unidades, observacion };
    setGuardando(true);
    try {
      const { data: actualizado } = await api.post(
        `/stock/${tipo === "ingreso" ? "ingresos" : "ajustes"}`,
        payload,
      );
      setAviso({
        tono: "success",
        texto: `${actualizado.nombre}: stock actualizado a ${numero.format(actualizado.cantidadActual)} unidades.`,
      });
      setVersion((v) => v + 1);
      setCantidad("");
      formulario.reset();
      if (historial?.productoId === productoId) cargarHistorial(productoId);
    } catch (failure) {
      setAviso({ tono: "danger", texto: failure.message });
    } finally {
      setGuardando(false);
    }
  }

  return (
    <div className="stock-page">
      <header className="page-head">
        <div>
          <span className="eyebrow">Catálogo</span>
          <h1>Stock y movimientos</h1>
          <p className="lead">
            Controlá las existencias y registrá ingresos o correcciones de
            inventario.
          </p>
        </div>
        <div className="page-head-actions">
          <a className="dm-button dm-button-primary" href="#stock-movimiento">
            <Plus size={16} aria-hidden="true" />
            Registrar movimiento
          </a>
        </div>
      </header>
      {aviso && (
        <Alert tone={aviso.tono} onDismiss={() => setAviso(null)}>
          {aviso.texto}
        </Alert>
      )}
      {sinStock.length > 0 && (
        <Alert tone="warning">
          <strong>
            {sinStock.length === 1
              ? "1 producto sin stock:"
              : `${sinStock.length} productos sin stock:`}
          </strong>{" "}
          {sinStock.map((p) => p.nombre).join(", ")}. No se muestran en el
          catálogo hasta reponerlos.
        </Alert>
      )}
      <dl className="kpi-grid">
        <Kpi label="Productos controlados" value={productos.length} />
        <Kpi
          label="Sin stock"
          value={sinStock.length}
          detail={
            sinStock.length
              ? "Requieren reposición"
              : "Todos tienen existencias"
          }
        />
      </dl>
      <section className="panel" aria-labelledby="stock-listado">
        <div className="panel-head">
          <h2 id="stock-listado">Existencias actuales</h2>
          <p>Las cantidades se expresan en unidades.</p>
        </div>
        <DataTable
          caption="Stock actual por producto"
          columns={columnasStock}
          rows={productos}
          rowKey={(p) => p.productoId}
          empty={
            resultado === null ? "Cargando stock…" : "No hay productos activos."
          }
        />
      </section>
      <div className="stock-grid">
        <section className="panel" aria-labelledby="stock-movimiento">
          <div className="panel-head">
            <h2 id="stock-movimiento">Registrar movimiento</h2>
            <p>Sumá mercadería o corregí una diferencia de inventario.</p>
          </div>
          <form className="stock-form" onSubmit={guardar}>
            <fieldset disabled={guardando || !productos.length}>
              <label className="dm-field">
                <span>Producto</span>
                <select
                  className="dm-input"
                  value={productoId}
                  onChange={(e) => setProductoId(e.target.value)}
                  required
                >
                  <option value="">Seleccioná un producto</option>
                  {productos.map((p) => (
                    <option key={p.productoId} value={p.productoId}>
                      {p.codigo} · {p.nombre} (actual: {p.cantidadActual})
                    </option>
                  ))}
                </select>
              </label>
              <div className="form-grid">
                <label className="dm-field">
                  <span>Tipo</span>
                  <select
                    className="dm-input"
                    value={tipo}
                    onChange={(e) => setTipo(e.target.value)}
                  >
                    <option value="ingreso">Ingreso de mercadería</option>
                    <option value="ajuste">Ajuste manual</option>
                  </select>
                </label>
                <FormField
                  name="cantidad"
                  label={
                    tipo === "ingreso"
                      ? "Unidades ingresadas"
                      : "Diferencia de unidades"
                  }
                  type="number"
                  required
                  min={tipo === "ingreso" ? 1 : undefined}
                  step={1}
                  value={cantidad}
                  onChange={(e) => setCantidad(e.target.value)}
                  hint={
                    tipo === "ajuste"
                      ? "Usá un valor positivo para sumar o negativo para descontar."
                      : undefined
                  }
                  error={
                    ajusteNegativo
                      ? "El ajuste no puede dejar el stock en negativo."
                      : undefined
                  }
                />
              </div>
              {saldoResultante !== null && cantidad !== "" && (
                <p className="stock-preview">
                  Saldo resultante:{" "}
                  <strong>{numero.format(saldoResultante)}</strong>
                </p>
              )}
              <FormField
                name="observacion"
                label="Observación"
                maxLength={500}
                hint="Opcional · hasta 500 caracteres"
              />
              <button
                className="dm-button dm-button-primary"
                type="submit"
                disabled={ajusteNegativo}
              >
                {guardando ? "Guardando…" : "Registrar movimiento"}
              </button>
            </fieldset>
          </form>
        </section>
        <section className="panel" aria-labelledby="stock-historial">
          <div className="panel-head">
            <h2 id="stock-historial">Historial de movimientos</h2>
            <p>Ingresos, ajustes y ventas de un producto.</p>
          </div>
          <div className="toolbar">
            <label className="dm-field">
              <span>Producto</span>
              <select
                className="dm-input"
                value={historial?.productoId ?? ""}
                onChange={(e) =>
                  e.target.value && cargarHistorial(e.target.value)
                }
              >
                <option value="">Seleccioná un producto</option>
                {productos.map((p) => (
                  <option key={p.productoId} value={p.productoId}>
                    {p.codigo} · {p.nombre}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <DataTable
            caption="Movimientos de stock"
            columns={columnasMovimientos}
            rows={historial?.data ?? []}
            rowKey={(m) => m.id}
            empty={
              !historial
                ? "Elegí un producto para consultar sus movimientos."
                : historial.cargando
                  ? "Cargando movimientos…"
                  : "Todavía no hay movimientos registrados."
            }
          />
        </section>
      </div>
    </div>
  );
}
